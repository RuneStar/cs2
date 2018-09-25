package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type.*
import org.runestar.cs2.Type
import org.runestar.cs2.ir.Op.Src.*
import org.runestar.cs2.namesReverse
import org.runestar.cs2.util.ListStack

interface Op {

    val id: Int

    fun translate(state: Interpreter.State): Insn

    companion object {

        private infix fun Type.u(src: Src) = Arg(this, src)

        private val map: Map<Int, Op> by lazy {
            val list = ArrayList<Op>()
            list.add(Switch)
            list.add(Branch)
            list.add(GetEnum)
            list.add(Invoke)
            list.add(Return)
            list.add(JoinString)
            list.addAll(PushCst.values().asList())
            list.addAll(BranchCompare.values().asList())
            list.addAll(Basic.values().asList())
            list.addAll(SetOn.values().asList())
            list.addAll(ParamKey.values().asList())
            list.associateBy { it.id }
        }

        fun of(id: Int): Op = map.getValue(id)
    }

    private object Switch : Op {

        override val id = Opcodes.SWITCH

        override fun translate(state: Interpreter.State): Insn {
            return Insn.Switch(state.pop(Type.INT), state.switch.mapValues { Insn.Label(it.value + 1 + state.pc) })
        }
    }

    private object Branch : Op {

        override val id = Opcodes.BRANCH

        override fun translate(state: Interpreter.State): Insn {
            return Insn.Goto(Insn.Label(state.pc + state.intOperand + 1))
        }
    }

    private object Invoke : Op {

        override val id = Opcodes.INVOKE

        override fun translate(state: Interpreter.State): Insn {
            val invokeId = state.intOperand

            val args = ArrayList<Expr>()
            args.add(Expr.Cst(INT, invokeId))
            val returns = ArrayList<Expr.Var>()
            if (invokeId == state.id) {
                // todo
                repeat(state.script.intArgumentCount) { args.add(state.pop(Type.INT)) }
                repeat(state.script.stringArgumentCount) { args.add(state.pop(Type.STRING)) }
            } else {
                val invoked = state.interpreter.interpret(invokeId)
                val stackArgs = ArrayList<Expr>()
                invoked.args.forEach {
                    stackArgs.add(state.pop(it.type))
                }
                args.addAll(stackArgs.asReversed())

                invoked.returns.forEach {
                    returns.add(state.push(it))
                }
            }

            return Insn.Assignment(returns, Expr.Operation(returns.map { it.type }, id, args))
        }
    }

    private object Return : Op {

        override val id = Opcodes.RETURN

        override fun translate(state: Interpreter.State): Insn {
            val args = ArrayList<Expr>()
            repeat(state.intStack.size) {
                args.add(state.pop(INT))
            }
            repeat(state.strStack.size) {
                args.add(state.pop(STRING))
            }
            args.reverse()
            return Insn.Return(Expr.Operation(emptyList(), id, args))
        }
    }

    private object GetEnum : Op {

        override val id = Opcodes.ENUM

        override fun translate(state: Interpreter.State): Insn {
            val key = state.pop(INT)
            val enumId = state.pop(INT)
            val valueType = Type.of(checkNotNull(state.intStack.peek().cst))
            val valueTypeVar = state.pop(TYPE)
            val keyType = Type.of(checkNotNull(state.intStack.peek().cst))
            val keyTypeVar = state.pop(TYPE)
            val args = mutableListOf<Expr>(keyTypeVar, valueTypeVar, enumId, key)
            key.type = keyType
            val value = state.push(valueType)
            return Insn.Assignment(listOf(value), Expr.Operation(listOf(value.type), id, args))
        }
    }

    private enum class BranchCompare : Op {

        BRANCH_NOT,
        BRANCH_EQUALS,
        BRANCH_LESS_THAN,
        BRANCH_GREATER_THAN,
        BRANCH_LESS_THAN_OR_EQUALS,
        BRANCH_GREATER_THAN_OR_EQUALS;

        override val id = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val r = state.pop(INT)
            val l = state.pop(INT)
            val expr = Expr.Operation(emptyList(), id, mutableListOf(l, r))
            return Insn.Branch(expr, Insn.Label(state.pc + state.intOperand + 1))
        }
    }

    private enum class PushCst(val type: Type) : Op {
        PUSH_CONSTANT_INT(Type.INT),
        PUSH_CONSTANT_STRING(Type.STRING);

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val cst = state.operand(type)
            return Insn.Assignment(listOf(state.push(type, cst.cst)), cst)
        }
    }

    private data class Arg(val type: Type, val src: Src)

    private enum class Src {
        L, S, O
    }

    private enum class Basic(val args: Array<Arg> = emptyArray(), val defs: Array<Arg> = emptyArray()) : Op {
        PUSH_VAR(arrayOf(INT u O), arrayOf(INT u S)),
        POP_VAR(arrayOf(INT u O, INT u S)),
        PUSH_VARBIT(arrayOf(INT u O), arrayOf(INT u S)),
        POP_VARBIT(arrayOf(INT u O, INT u S)),
        PUSH_INT_LOCAL(arrayOf(INT u L), arrayOf(INT u S)),
        POP_INT_LOCAL(arrayOf(INT u S), arrayOf(INT u L)),
        PUSH_STRING_LOCAL(arrayOf(STRING u L), arrayOf(STRING u S)),
        POP_STRING_LOCAL(arrayOf(STRING u S), arrayOf(STRING u L)),
        POP_INT_DISCARD(arrayOf(INT u S)),
        POP_STRING_DISCARD(arrayOf(STRING u S)),
        GET_VARC_INT(arrayOf(INT u O), arrayOf(INT u S)),
        SET_VARC_INT(arrayOf(INT u O, INT u S)),
        DEFINE_ARRAY(arrayOf(INT u S, INT u O)),
        PUSH_ARRAY_INT(arrayOf(INT u S, INT u O), arrayOf(INT u S)),
        POP_ARRAY_INT(arrayOf(INT u S, INT u S, INT u O)),
        GET_VARC_STRING(arrayOf(INT u O), arrayOf(STRING u S)),
        SET_VARC_STRING(arrayOf(INT u O, STRING u S)),
        CC_CREATE(arrayOf(COMPONENT u S, INT u S, INT u S, BOOLEAN u O)),
        CC_DELETE(arrayOf(BOOLEAN u O)),
        CC_DELETEALL(arrayOf(COMPONENT u S)),
        CC_FIND(arrayOf(INT u S, INT u S, BOOLEAN u O), arrayOf(INT u S)),
        IF_FIND(arrayOf(INT u S, BOOLEAN u O), arrayOf(INT u S)),

        CC_SETPOSITION(arrayOf(INT u S, INT u S, INT u S, INT u S, BOOLEAN u O)),
        CC_SETSIZE(arrayOf(INT u S, INT u S, INT u S, INT u S, BOOLEAN u O)),
        CC_SETHIDE(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        _1005(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        _1006(arrayOf(BOOLEAN u S, BOOLEAN u O)),

        CC_SETSCROLLPOS(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        CC_SETCOLOUR(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETFILL(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        CC_SETTRANS(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETLINEWID(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETGRAPHIC(arrayOf(INT u S, BOOLEAN u O)),
        CC_SET2DANGLE(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETTILING(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        CC_SETMODEL(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETMODELANGLE(arrayOf(INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, BOOLEAN u O)),
        CC_SETMODELANIM(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETMODELORTHOG(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        CC_SETTEXT(arrayOf(STRING u S, BOOLEAN u O)),
        CC_SETTEXTFONT(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETTEXTALIGN(arrayOf(INT u S, INT u S, INT u S, BOOLEAN u O)),
        CC_SETTEXTANTIMACRO(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        CC_SETOUTLINE(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETGRAPHICSHADOW(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETVFLIP(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        CC_SETHFLIP(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        CC_SETSCROLLSIZE(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        CC_RESUME_PAUSEBUTTON(arrayOf(BOOLEAN u O)),
        _1122(arrayOf(INT u S, BOOLEAN u O)),
        _1123(arrayOf(INT u S, BOOLEAN u O)),
        _1124(arrayOf(INT u S, BOOLEAN u O)),
        _1125(arrayOf(INT u S, BOOLEAN u O)),
        _1126(arrayOf(BOOLEAN u S, BOOLEAN u O)),
        _1127(arrayOf(BOOLEAN u S, BOOLEAN u O)),

        CC_SETOBJECT(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        CC_SETNPCHEAD(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETPLAYERHEAD_SELF(arrayOf(BOOLEAN u O)),
        CC_SETOBJECT_NONUM(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        CC_SETOBJECT_ALWAYS_NUM(arrayOf(INT u S, INT u S, BOOLEAN u O)),

        CC_SETOP(arrayOf(INT u S, STRING u S, BOOLEAN u O)),
        CC_SETDRAGGABLE(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        CC_SETDRAGGABLEBEHAVIOR(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETDRAGDEADZONE(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETDRAGDEADTIME(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETOPBASE(arrayOf(STRING u S, BOOLEAN u O)),
        CC_SETTARGETVERB(arrayOf(STRING u S, BOOLEAN u O)),
        CC_CLEAROPS(arrayOf(BOOLEAN u O)),
        CC_SETOPKEY(arrayOf(INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, BOOLEAN u O)),
        CC_SETOPTKEY(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        CC_SETOPKEYRATE(arrayOf(INT u S, INT u S, INT u S, BOOLEAN u O)),
        CC_SETOPTKEYRATE(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        CC_SETOPKEYIGNOREHELD(arrayOf(INT u S, BOOLEAN u O)),
        CC_SETOPTKEYIGNOREHELD(arrayOf(BOOLEAN u O)),

        CC_GETX(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETY(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETWIDTH(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETHEIGHT(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETHIDE(arrayOf(BOOLEAN u O), arrayOf(BOOLEAN u S)),
        CC_GETLAYER(arrayOf(BOOLEAN u O), arrayOf(INT u S)),

        CC_GETSCROLLX(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETSCROLLY(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETTEXT(arrayOf(BOOLEAN u O), arrayOf(STRING u S)),
        CC_GETSCROLLWIDTH(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETSCROLLHEIGHT(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETMODELZOOM(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETMODELANGLE_X(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETMODELANGLE_Z(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETMODELANGLE_Y(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETTRANS(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETMODELXOF(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETMODELYOF(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        _1612(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        _1613(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        _1614(arrayOf(BOOLEAN u O), arrayOf(BOOLEAN u S)),

        CC_GETINVOBJECT(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETINVCOUNT(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETID(arrayOf(BOOLEAN u O), arrayOf(INT u S)),

        CC_GETTARGETMASK(arrayOf(BOOLEAN u O), arrayOf(INT u S)),
        CC_GETOP(arrayOf(INT u S, BOOLEAN u O), arrayOf(STRING u S)),
        CC_GETOPBASE(arrayOf(BOOLEAN u O), arrayOf(STRING u S)),

        _1927(arrayOf(BOOLEAN u S)),

        IF_SETPOSITION(arrayOf(INT u S, INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETSIZE(arrayOf(INT u S, INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETHIDE(arrayOf(BOOLEAN u S, COMPONENT u S)),
        _2005(arrayOf(BOOLEAN u S, COMPONENT u S)),
        _2006(arrayOf(BOOLEAN u S, COMPONENT u S)),

        IF_SETSCROLLPOS(arrayOf(INT u S, INT u S, COMPONENT u S)),
        IF_SETCOLOUR(arrayOf(INT u S, COMPONENT u S)),
        IF_SETFILL(arrayOf(BOOLEAN u S, COMPONENT u S)),
        IF_SETTRANS(arrayOf(INT u S, COMPONENT u S)),
        IF_SETLINEWID(arrayOf(INT u S, COMPONENT u S)),
        IF_SETGRAPHIC(arrayOf(INT u S, COMPONENT u S)),
        IF_SET2DANGLE(arrayOf(INT u S, COMPONENT u S)),
        IF_SETTILING(arrayOf(BOOLEAN u S, COMPONENT u S)),
        IF_SETMODEL(arrayOf(INT u S, COMPONENT u S)),
        IF_SETMODELANGLE(arrayOf(INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETMODELANIM(arrayOf(INT u S, COMPONENT u S)),
        IF_SETMODELORTHOG(arrayOf(BOOLEAN u S, COMPONENT u S)),
        IF_SETTEXT(arrayOf(STRING u S, COMPONENT u S)),
        IF_SETTEXTFONT(arrayOf(INT u S, COMPONENT u S)),
        IF_SETTEXTALIGN(arrayOf(INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETTEXTANTIMACRO(arrayOf(BOOLEAN u S, COMPONENT u S)),
        IF_SETOUTLINE(arrayOf(INT u S, COMPONENT u S)),
        IF_SETGRAPHICSHADOW(arrayOf(INT u S, COMPONENT u S)),
        IF_SETVFLIP(arrayOf(BOOLEAN u S, COMPONENT u S)),
        IF_SETHFLIP(arrayOf(BOOLEAN u S, COMPONENT u S)),
        IF_SETSCROLLSIZE(arrayOf(INT u S, INT u S, COMPONENT u S)),
        _2121(arrayOf(COMPONENT u S)),
        _2122(arrayOf(INT u S, COMPONENT u S)),
        _2123(arrayOf(INT u S, COMPONENT u S)),
        _2124(arrayOf(INT u S, COMPONENT u S)),
        _2125(arrayOf(INT u S, COMPONENT u S)),
        _2126(arrayOf(BOOLEAN u S, COMPONENT u S)),
        _2127(arrayOf(BOOLEAN u S, COMPONENT u S)),

        IF_SETOBJECT(arrayOf(INT u S, INT u S, COMPONENT u S)),
        IF_SETNPCHEAD(arrayOf(INT u S, COMPONENT u S)),
        IF_SETPLAYERHEAD_SELF(arrayOf(COMPONENT u S)),
        IF_SETOBJECT_NONUM(arrayOf(INT u S, INT u S, COMPONENT u S)),
        IF_SETOBJECT_ALWAYS_NUM(arrayOf(INT u S, INT u S, COMPONENT u S)),

        IF_SETOP(arrayOf(INT u S, STRING u S, COMPONENT u S)),
        IF_SETDRAGGABLE(arrayOf(INT u S, INT u S, COMPONENT u S)),
        IF_SETDRAGGABLEBEHAVIOR(arrayOf(INT u S, COMPONENT u S)),
        IF_SETDRAGDEADZONE(arrayOf(INT u S, COMPONENT u S)),
        IF_SETDRAGDEADTIME(arrayOf(INT u S, COMPONENT u S)),
        IF_SETOPBASE(arrayOf(STRING u S, COMPONENT u S)),
        IF_SETTARGETVERB(arrayOf(STRING u S, COMPONENT u S)),
        IF_CLEAROPS(arrayOf(COMPONENT u S)),
        _2350(arrayOf(INT u S, INT u S, INT u S, COMPONENT u S)),
        _2351(arrayOf(INT u S, INT u S, COMPONENT u S)),
        _2352(arrayOf(INT u S, INT u S, INT u S, COMPONENT u S)),
        _2353(arrayOf(INT u S, INT u S, COMPONENT u S)),
        _2354(arrayOf(INT u S, COMPONENT u S)),
        _2355(arrayOf(COMPONENT u S)),

        IF_GETX(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETY(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETWIDTH(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETHEIGHT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETHIDE(arrayOf(COMPONENT u S), arrayOf(BOOLEAN u S)),
        _2505(arrayOf(COMPONENT u S), arrayOf(INT u S)),

        IF_GETSCROLLX(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETSCROLLY(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETTEXT(arrayOf(COMPONENT u S), arrayOf(STRING u S)),
        IF_GETSCROLLWIDTH(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETSCROLLHEIGHT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETMODELZOOM(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETMODELANGLE_X(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETMODELANGLE_Z(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETMODELANGLE_Y(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETTRANS(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETMODELXOF(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETMODELYOF(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2612(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2613(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2614(arrayOf(COMPONENT u S), arrayOf(BOOLEAN u S)),

        IF_GETINVOBJECT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETINVCOUNT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETID(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETTOP(defs = arrayOf(INT u S)),

        IF_GETTARGETMASK(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETOP(arrayOf(INT u S, COMPONENT u S), arrayOf(STRING u S)),
        IF_GETOPBASE(arrayOf(COMPONENT u S), arrayOf(STRING u S)),

        _2927(arrayOf(COMPONENT u S, BOOLEAN u O)),

        MES(arrayOf(STRING u S)),
        _3101(arrayOf(INT u S, INT u S)),
        IF_CLOSE(),
        RESUME_COUNTDIALOG(arrayOf(STRING u S)),
        RESUME_NAMEDIALOG(arrayOf(STRING u S)),
        RESUME_STRINGDIALOG(arrayOf(STRING u S)),
        OPPLAYER(arrayOf(INT u S, STRING u S)),
        IF_DRAGPICKUP(arrayOf(COMPONENT u S, INT u S, INT u S)),
        CC_DRAGPICKUP(arrayOf(INT u S, INT u S, BOOLEAN u O)),
        _3110(arrayOf(INT u S)),
        _3111(defs = arrayOf(BOOLEAN u S)),
        _3112(arrayOf(BOOLEAN u S)),
        OPENURL(arrayOf(STRING u S, BOOLEAN u S)),
        RESUME_OBJDIALOG(arrayOf(INT u S)),
        BUG_REPORT(arrayOf(INT u S, STRING u S, STRING u S)),
        _3117(arrayOf(BOOLEAN u S)),
        _3118(arrayOf(BOOLEAN u S)),
        _3119(arrayOf(BOOLEAN u S)),
        _3120(arrayOf(BOOLEAN u S)),
        _3121(arrayOf(BOOLEAN u S)),
        _3122(arrayOf(BOOLEAN u S)),
        _3123(arrayOf(BOOLEAN u S)),
        _3124(),
        _3125(arrayOf(BOOLEAN u S)),
        _3126(arrayOf(BOOLEAN u S)),
        _3127(arrayOf(BOOLEAN u S)),
        _3128(defs = arrayOf(INT u S)),
        _3129(arrayOf(INT u S, INT u S)),
        _3130(arrayOf(INT u S, INT u S)),
        _3131(arrayOf(INT u S)),
        _3132(defs = arrayOf(INT u S, INT u S)),
        _3133(arrayOf(INT u S)),
        _3134(),
        _3135(arrayOf(INT u S, INT u S)),
        _3136(arrayOf(BOOLEAN u S)),
        _3137(arrayOf(BOOLEAN u S)),
        _3138(),
        _3139(),
        _3140(arrayOf(BOOLEAN u O)),
        _3141(arrayOf(BOOLEAN u S)),
        _3142(defs = arrayOf(BOOLEAN u S)),
        _3143(arrayOf(BOOLEAN u S)),
        _3144(defs = arrayOf(BOOLEAN u S)),

        SOUND_SYNTH(arrayOf(INT u S, INT u S, INT u S)),
        SOUND_SONG(arrayOf(INT u S)),
        SOUND_JINGLE(arrayOf(INT u S, INT u S)),

        CLIENTCLOCK(defs = arrayOf(INT u S)),
        INV_GETOBJ(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        INV_GETNUM(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        INV_TOTAL(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        INV_SIZE(arrayOf(INT u S), arrayOf(INT u S)),
        STAT(arrayOf(INT u S), arrayOf(INT u S)),
        STAT_BASE(arrayOf(INT u S), arrayOf(INT u S)),
        STAT_XP(arrayOf(INT u S), arrayOf(INT u S)),
        COORD(defs = arrayOf(INT u S)),
        COORDX(arrayOf(INT u S), arrayOf(INT u S)),
        COORDZ(arrayOf(INT u S), arrayOf(INT u S)),
        COORDY(arrayOf(INT u S), arrayOf(INT u S)),
        MAP_MEMBERS(defs = arrayOf(BOOLEAN u S)),
        INVOTHER_GETOBJ(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        INVOTHER_GETNUM(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        INVOTHER_TOTAL(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        STAFFMODLEVEL(defs = arrayOf(INT u S)),
        REBOOTTIMER(defs = arrayOf(INT u S)),
        MAP_WORLD(defs = arrayOf(INT u S)),
        RUNENERGY_VISIBLE(defs = arrayOf(INT u S)),
        RUNWEIGHT_VISIBLE(defs = arrayOf(INT u S)),
        PLAYERMOD(defs = arrayOf(BOOLEAN u S)),
        _3324(defs = arrayOf(INT u S)),
        PLAYERMEMBER(arrayOf(INT u S, INT u S, INT u S, INT u S), arrayOf(INT u S)),

        _3400(arrayOf(INT u S, INT u S), arrayOf(STRING u S)),
        ENUM_GETOUTPUTCOUNT(arrayOf(INT u S), arrayOf(INT u S)),

        FRIEND_COUNT(defs = arrayOf(INT u S)),
        FRIEND_GETNAME(arrayOf(INT u S), arrayOf(STRING u S, STRING u S)),
        FRIEND_GETWORLD(arrayOf(INT u S), arrayOf(INT u S)),
        FRIEND_GETRANK(arrayOf(INT u S), arrayOf(INT u S)),
        FRIEND_SETRANK(arrayOf(STRING u S), arrayOf(INT u S)),
        FRIEND_ADD(arrayOf(STRING u S)),
        FRIEND_DEL(arrayOf(STRING u S)),
        IGNORE_ADD(arrayOf(STRING u S)),
        IGNORE_DEL(arrayOf(STRING u S)),
        FRIEND_TEST(arrayOf(STRING u S), arrayOf(INT u S)),
        FRIEND_GETWORLDNAME(arrayOf(INT u S), arrayOf(STRING u S)),
        CLAN_GETCHATDISPLAYNAME(defs = arrayOf(STRING u S)),
        CLAN_GETCHATCOUNT(defs = arrayOf(INT u S)),
        CLAN_GETCHATUSERNAME(arrayOf(INT u S), arrayOf(STRING u S)),
        CLAN_GETCHATUSERWORLD(arrayOf(INT u S), arrayOf(INT u S)),
        CLAN_GETCHATUSERRANK(arrayOf(INT u S), arrayOf(INT u S)),
        CLAN_GETCHATMINKICK(defs = arrayOf(INT u S)),
        CLAN_KICKUSER(arrayOf(STRING u S)),
        CLAN_GETCHATRANK(defs = arrayOf(INT u S)),
        CLAN_JOINCHAT(arrayOf(STRING u S)),
        CLAN_LEAVECHAT(),
        IGNORE_COUNT(defs = arrayOf(INT u S)),
        IGNORE_GETNAME(arrayOf(INT u S), arrayOf(STRING u S, STRING u S)),
        IGNORE_TEST(arrayOf(STRING u S), arrayOf(INT u S)),
        CLAN_ISSELF(arrayOf(INT u S), arrayOf(INT u S)),
        CLAN_GETCHATOWNERNAME(defs = arrayOf(STRING u S)),
        CLAN_GETCHATUSERWORLDNAME(arrayOf(INT u S), arrayOf(INT u S)),
        _3627(arrayOf(INT u S), arrayOf(BOOLEAN u S)),
        _3628(),
        _3629(arrayOf(BOOLEAN u S)),
        _3630(arrayOf(BOOLEAN u S)),
        _3631(arrayOf(BOOLEAN u S)),
        _3632(arrayOf(BOOLEAN u S)),
        _3633(arrayOf(BOOLEAN u S)),
        _3634(arrayOf(BOOLEAN u S)),
        _3635(arrayOf(BOOLEAN u S)),
        _3636(arrayOf(BOOLEAN u S)),
        _3637(arrayOf(BOOLEAN u S)),
        _3638(arrayOf(BOOLEAN u S)),
        _3639(),
        _3640(),
        _3641(arrayOf(BOOLEAN u S)),
        _3642(arrayOf(BOOLEAN u S)),
        _3643(),
        _3644(),
        _3645(arrayOf(BOOLEAN u S)),
        _3646(arrayOf(BOOLEAN u S)),
        _3647(arrayOf(BOOLEAN u S)),
        _3648(arrayOf(BOOLEAN u S)),
        _3649(arrayOf(BOOLEAN u S)),
        _3650(arrayOf(BOOLEAN u S)),
        _3651(arrayOf(BOOLEAN u S)),
        _3652(arrayOf(BOOLEAN u S)),
        _3653(arrayOf(BOOLEAN u S)),
        _3654(arrayOf(BOOLEAN u S)),
        _3655(),
        _3656(arrayOf(BOOLEAN u S)),
        _3657(arrayOf(BOOLEAN u S)),

        STOCKMARKET_GETOFFERTYPE(arrayOf(INT u S), arrayOf(INT u S)),
        STOCKMARKET_GETOFFERITEM(arrayOf(INT u S), arrayOf(INT u S)),
        STOCKMARKET_GETOFFERPRICE(arrayOf(INT u S), arrayOf(INT u S)),
        STOCKMARKET_GETOFFERCOUNT(arrayOf(INT u S), arrayOf(INT u S)),
        STOCKMARKET_GETOFFERCOMPLETEDCOUNT(arrayOf(INT u S), arrayOf(INT u S)),
        STOCKMARKET_GETOFFERCOMPLETEDGOLD(arrayOf(INT u S), arrayOf(INT u S)),
        STOCKMARKET_ISOFFEREMPTY(arrayOf(INT u S), arrayOf(BOOLEAN u S)),
        STOCKMARKET_ISOFFERSTABLE(arrayOf(INT u S), arrayOf(BOOLEAN u S)),
        STOCKMARKET_ISOFFERFINISHED(arrayOf(INT u S), arrayOf(BOOLEAN u S)),
        STOCKMARKET_ISOFFERADDING(arrayOf(INT u S), arrayOf(BOOLEAN u S)),
        _3914(arrayOf(BOOLEAN u S)),
        _3915(arrayOf(BOOLEAN u S)),
        _3916(arrayOf(BOOLEAN u S, BOOLEAN u S)),
        _3917(arrayOf(BOOLEAN u S)),
        _3918(arrayOf(BOOLEAN u S)),
        _3919(defs = arrayOf(BOOLEAN u S)),
        _3920(arrayOf(INT u S), arrayOf(INT u S)),
        _3921(arrayOf(INT u S), arrayOf(STRING u S)),
        _3922(arrayOf(INT u S), arrayOf(STRING u S)),
        _3923(arrayOf(INT u S), arrayOf(STRING u S)),
        _3924(arrayOf(INT u S), arrayOf(INT u S)),
        _3925(arrayOf(INT u S), arrayOf(INT u S)),
        _3926(arrayOf(INT u S), arrayOf(INT u S)),

        ADD(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        SUB(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        MULTIPLY(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        DIV(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        RANDOM(arrayOf(INT u S), arrayOf(INT u S)),
        RANDOMINC(arrayOf(INT u S), arrayOf(INT u S)),
        INTERPOLATE(arrayOf(INT u S, INT u S, INT u S, INT u S, INT u S), arrayOf(INT u S)),
        ADDPERCENT(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        SETBIT(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        CLEARBIT(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        TESTBIT(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        MOD(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        POW(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        INVPOW(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        AND(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        OR(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        SCALE(arrayOf(INT u S, INT u S, INT u S), arrayOf(INT u S)),
        APPEND_NUM(arrayOf(INT u S, STRING u S), arrayOf(STRING u S)),
        APPEND(arrayOf(STRING u S, STRING u S), arrayOf(STRING u S)),
        APPEND_SIGNNUM(arrayOf(INT u S, STRING u S), arrayOf(STRING u S)),
        LOWERCASE(arrayOf(STRING u S), arrayOf(STRING u S)),
        FROMDATE(arrayOf(INT u S), arrayOf(STRING u S)),
        TEXT_GENDER(arrayOf(STRING u S, STRING u S), arrayOf(STRING u S)),
        TOSTRING(arrayOf(INT u S), arrayOf(STRING u S)),
        COMPARE(arrayOf(STRING u S, STRING u S), arrayOf(INT u S)),
        PARAHEIGHT(arrayOf(INT u S, INT u S, STRING u S), arrayOf(INT u S)),
        PARAWIDTH(arrayOf(INT u S, INT u S, STRING u S), arrayOf(INT u S)),
        TEXT_SWITCH(arrayOf(INT u S, STRING u S, STRING u S), arrayOf(STRING u S)),
        ESCAPE(arrayOf(STRING u S), arrayOf(STRING u S)),
        APPEND_CHAR(arrayOf(INT u S, STRING u S), arrayOf(STRING u S)),
        CHAR_ISPRINTABLE(arrayOf(INT u S), arrayOf(INT u S)),
        CHAR_ISALPHANUMERIC(arrayOf(INT u S), arrayOf(INT u S)),
        CHAR_ISALPHA(arrayOf(INT u S), arrayOf(INT u S)),
        CHAR_ISNUMERIC(arrayOf(INT u S), arrayOf(INT u S)),
        STRING_LENGTH(arrayOf(STRING u S), arrayOf(INT u S)),
        SUBSTRING(arrayOf(STRING u S, INT u S, INT u S), arrayOf(STRING u S)),
        REMOVETAGS(arrayOf(STRING u S), arrayOf(STRING u S)),
        STRING_INDEXOF_CHAR(arrayOf(STRING u S, INT u S), arrayOf(INT u S)),
        STRING_INDEXOF_STRING(arrayOf(STRING u S, STRING u S, INT u S), arrayOf(INT u S)),

        OC_NAME(arrayOf(INT u S), arrayOf(STRING u S)),
        OC_OP(arrayOf(INT u S, INT u S), arrayOf(STRING u S)),
        OC_IOP(arrayOf(INT u S, INT u S), arrayOf(STRING u S)),
        OC_COST(arrayOf(INT u S), arrayOf(INT u S)),
        OC_STACKABLE(arrayOf(INT u S), arrayOf(INT u S)),
        OC_CERT(arrayOf(INT u S), arrayOf(INT u S)),
        OC_UNCERT(arrayOf(INT u S), arrayOf(INT u S)),
        OC_MEMBERS(arrayOf(INT u S), arrayOf(INT u S)),
        OC_PLACEHOLDER(arrayOf(INT u S), arrayOf(INT u S)),
        OC_UNPLACEHOLDER(arrayOf(INT u S), arrayOf(INT u S)),
        OC_FIND(arrayOf(INT u S, STRING u S), arrayOf(INT u S)),
        OC_FINDNEXT(defs = arrayOf(INT u S)),
        OC_FINDRESET(),

        CHAT_GETFILTER_PUBLIC(defs = arrayOf(INT u S)),
        CHAT_SETFILTER(arrayOf(INT u S, INT u S, INT u S)),
        CHAT_SENDABUSEREPORT(arrayOf(STRING u S, INT u S, INT u S)),
        CHAT_GETHISTORY_BYTYPEANDLINE(arrayOf(INT u S, INT u S), arrayOf(INT u S, INT u S, INT u S, STRING u S, STRING u S, STRING u S)),
        CHAT_GETHISTORY_BYUID(arrayOf(INT u S), arrayOf(INT u S, INT u S, INT u S, STRING u S, STRING u S, STRING u S)),
        CHAT_GETFILTER_PRIVATE(defs = arrayOf(INT u S)),
        CHAT_SENDPUBLIC(arrayOf(STRING u S, INT u S)),
        CHAT_SENDPRIVATE(arrayOf(STRING u S, STRING u S), arrayOf(STRING u S)),
        CHAT_PLAYERNAME(defs = arrayOf(STRING u S)),
        CHAT_GETFILTER_TRADE(defs = arrayOf(INT u S)),
        CHAT_GETHISTORYLENGTH(arrayOf(INT u S), arrayOf(INT u S)),
        CHAT_GETNEXTUID(arrayOf(INT u S), arrayOf(INT u S)),
        CHAT_GETPREVUID(arrayOf(INT u S), arrayOf(INT u S)),
        DOCHEAT(arrayOf(STRING u S)),
        CHAT_SETMESSAGEFILTER(arrayOf(STRING u S)),
        CHAT_GETMESSAGEFILTER(defs = arrayOf(STRING u S)),

        _5306(defs = arrayOf(INT u S)),
        _5307(arrayOf(INT u S)),
        _5308(defs = arrayOf(INT u S)),
        _5309(arrayOf(INT u S)),

        _5504(arrayOf(INT u S, INT u S)),
        _5505(defs = arrayOf(INT u S)),
        _5506(defs = arrayOf(INT u S)),
        _5530(arrayOf(INT u S)),
        _5531(defs = arrayOf(INT u S)),

        _5630(),

        _6200(arrayOf(INT u S, INT u S)),
        _6201(arrayOf(INT u S, INT u S)),
        _6202(arrayOf(INT u S, INT u S, INT u S, INT u S)),
        _6203(defs = arrayOf(INT u S, INT u S)),
        _6204(defs = arrayOf(INT u S, INT u S)),
        _6205(defs = arrayOf(INT u S, INT u S)),

        _6500(defs = arrayOf(BOOLEAN u S)),
        _6501(defs = arrayOf(INT u S, INT u S, STRING u S, INT u S, INT u S, STRING u S)),
        _6502(defs = arrayOf(INT u S, INT u S, STRING u S, INT u S, INT u S, STRING u S)),
        _6506(arrayOf(INT u S), arrayOf(INT u S, INT u S, STRING u S, INT u S, INT u S, STRING u S)),
        _6507(arrayOf(INT u S, BOOLEAN u S, INT u S, BOOLEAN u S)),
        _6511(arrayOf(INT u S), arrayOf(INT u S, INT u S, STRING u S, INT u S, INT u S, STRING u S)),
        _6512(arrayOf(BOOLEAN u S)),

        _6518(defs = arrayOf(INT u S)),
        _6519(defs = arrayOf(INT u S)),
        _6520(),
        _6521(),
        _6522(arrayOf(INT u S, STRING u S)),
        _6523(arrayOf(INT u S, STRING u S)),
        _6524(defs = arrayOf(INT u S)),
        _6525(defs = arrayOf(INT u S)),
        _6526(defs = arrayOf(INT u S)),

        _6600(),
        _6601(arrayOf(INT u S), arrayOf(STRING u S)),
        _6602(arrayOf(INT u S)),
        _6603(defs = arrayOf(INT u S)),
        _6604(arrayOf(INT u S)),
        _6605(defs = arrayOf(BOOLEAN u S)),
        _6606(arrayOf(INT u S)),
        _6607(arrayOf(INT u S)),
        _6608(arrayOf(INT u S)),
        _6609(arrayOf(INT u S)),
        _6610(defs = arrayOf(INT u S, INT u S)),
        _6611(arrayOf(INT u S), arrayOf(INT u S)),
        _6612(arrayOf(INT u S), arrayOf(INT u S, INT u S)),
        _6613(arrayOf(INT u S), arrayOf(INT u S, INT u S, INT u S, INT u S)),
        _6614(arrayOf(INT u S), arrayOf(INT u S)),
        _6615(defs = arrayOf(INT u S, INT u S)),
        _6616(defs = arrayOf(INT u S)),
        _6617(arrayOf(INT u S), arrayOf(INT u S, INT u S)),
        _6618(arrayOf(INT u S), arrayOf(INT u S, INT u S)),
        _6619(arrayOf(INT u S, INT u S)),
        _6620(arrayOf(INT u S, INT u S)),
        _6621(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _6622(defs = arrayOf(INT u S, INT u S)),
        _6623(arrayOf(INT u S), arrayOf(INT u S)),
        _6624(arrayOf(INT u S)),
        _6625(),
        _6626(arrayOf(INT u S)),
        _6627(),
        _6628(arrayOf(INT u S)),
        _6629(arrayOf(INT u S)),
        _6630(arrayOf(INT u S)),
        _6631(),
        _6632(arrayOf(BOOLEAN u S)),
        _6633(arrayOf(INT u S, BOOLEAN u S)),
        _6634(arrayOf(INT u S, INT u S)),
        _6635(defs = arrayOf(BOOLEAN u S)),
        _6636(arrayOf(INT u S), arrayOf(BOOLEAN u S)),
        _6637(arrayOf(INT u S), arrayOf(BOOLEAN u S)),
        _6638(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _6639(defs = arrayOf(INT u S, INT u S)),
        _6640(defs = arrayOf(INT u S, INT u S)),
        _6693(arrayOf(INT u S), arrayOf(STRING u S)),
        _6694(arrayOf(INT u S), arrayOf(INT u S)),
        _6695(arrayOf(INT u S), arrayOf(INT u S)),
        _6696(arrayOf(INT u S), arrayOf(INT u S)),
        _6697(defs = arrayOf(INT u S)),
        _6698(defs = arrayOf(INT u S)),
        _6699(defs = arrayOf(INT u S)),
        ;

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val stackArgs = ListStack<Expr.Var>(ArrayList())
            for (i in args.lastIndex downTo 0) {
                val arg = args[i]
                if (arg.src == S) stackArgs.push(state.pop(arg.type))
            }
            val exprArgs = ArrayList<Expr>(args.size)
            for (arg in args) {
                when (arg.src) {
                    S -> exprArgs.add(stackArgs.pop())
                    L -> exprArgs.add(Expr.Var(state.intOperand, arg.type))
                    O -> exprArgs.add(state.operand(arg.type))
                }
            }
            val exprDefs = ArrayList<Expr.Var>(defs.size)
            for (def in defs) {
                when (def.src) {
                    S -> exprDefs.add(state.push(def.type))
                    L -> exprDefs.add(Expr.Var(state.intOperand, def.type))
                    O -> throw IllegalStateException()
                }
            }
            return Insn.Assignment(exprDefs, Expr.Operation(defs.map { it.type }, id, exprArgs))
        }
    }

    private object JoinString : Op {

        override val id = Opcodes.JOIN_STRING

        override fun translate(state: Interpreter.State): Insn {
            val intOperand = state.intOperand
            val args = ArrayList<Expr>(intOperand)
            repeat(intOperand) {
                args.add(state.pop(Type.STRING))
            }
            args.reverse()
            return Insn.Assignment(listOf(state.push(Type.STRING)), Expr.Operation(listOf(Type.STRING), id, args))
        }
    }

    private enum class SetOn : Op {

        CC_SETONCLICK,
        CC_SETONHOLD,
        CC_SETONRELEASE,
        CC_SETONMOUSEOVER,
        CC_SETONMOUSELEAVE,
        CC_SETONDRAG,
        CC_SETONTARGETLEAVE,
        CC_SETONVARTRANSMIT,
        CC_SETONTIME,
        CC_SETONTOP,
        CC_SETONDRAGCOMPLETE,
        CC_SETONCLICKREPEAT,
        CC_SETONMOUSEREPEAT,
        CC_SETONINVTRANSMIT,
        CC_SETONSTATTRANSMIT,
        CC_SETONTARGETENTER,
        CC_SETONSCROLLWHEEL,
        CC_SETONCHATTRANSMIT,
        CC_SETONKEY,
        _1420,
        _1421,
        _1422,
        _1423,
        _1424,
        _1425,
        _1426,
        _1427,
        IF_SETONCLICK,
        IF_SETONHOLD,
        IF_SETONRELEASE,
        IF_SETONMOUSEOVER,
        IF_SETONMOUSELEAVE,
        IF_SETONDRAG,
        IF_SETONTARGETLEAVE,
        IF_SETONVARTRANSMIT,
        IF_SETONTIME,
        IF_SETONTOP,
        IF_SETONDRAGCOMPLETE,
        IF_SETONCLICKREPEAT,
        IF_SETONMOUSEREPEAT,
        IF_SETONINVTRANSMIT,
        IF_SETONSTATTRANSMIT,
        IF_SETONTARGETENTER,
        IF_SETONSCROLLWHEEL,
        IF_SETONCHATTRANSMIT,
        IF_SETONKEY,
        _2420,
        _2421,
        _2422,
        _2423,
        _2424,
        _2425,
        _2426,
        _2427;

        override val id = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val args = ArrayList<Expr>()
            if (id >= 2000) {
                args.add(state.pop(Type.COMPONENT))
            }
            var s = checkNotNull(state.strStack.pop().cst)
            if (s.isNotEmpty() && s.last() == 'Y') {
                val n = checkNotNull(state.intStack.pop().cst)
                repeat(n) {
                    args.add(state.pop(Type.INT))
                }
                s = s.dropLast(1)
            }
            for (c in s) {
                args.add(state.pop(Type.of(c)))
            }
            args.add(state.pop(Type.INT))
            args.reverse()
            if (id < 2000) {
                args.add(Expr.Cst(Type.BOOLEAN, state.intOperand))
            }
            return Insn.Assignment(emptyList(), Expr.Operation(emptyList(), id, args))
        }
    }

    enum class ParamKey : Op {
        NC_PARAM,
        LC_PARAM,
        OC_PARAM,
        STRUCT_PARAM,
        ;

        override val id = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val paramKeyId = checkNotNull(state.intStack.peek().cst)
            val args = mutableListOf<Expr>(state.pop(Type.INT), state.pop(INT))
            val paramType = state.interpreter.paramTypeLoader.load(paramKeyId)
            return Insn.Assignment(listOf(state.push(paramType)), Expr.Operation(listOf(paramType), id, args))
        }
    }
}