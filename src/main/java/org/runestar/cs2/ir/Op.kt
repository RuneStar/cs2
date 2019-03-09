package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.Type.*
import org.runestar.cs2.ir.Op.Src.*
import org.runestar.cs2.namesReverse
import org.runestar.cs2.util.ListStack

internal interface Op {

    val id: Int

    fun translate(state: Interpreter.State): Insn

    companion object {
        
        private val Type.s get() = Arg(this, S)

        private val Type.l get() = Arg(this, L)

        private val Type.o get() = Arg(this, O)

        private val map: Map<Int, Op> by lazy {
            ArrayList<Op>().run {
                add(Switch)
                add(Branch)
                add(GetEnum)
                add(Invoke)
                add(Return)
                add(JoinString)
                add(DefineArray)
                add(GetArrayInt)
                add(SetArrayInt)
                addAll(PushCst.values().asList())
                addAll(BranchCompare.values().asList())
                addAll(Basic.values().asList())
                addAll(SetOn.values().asList())
                addAll(ParamKey.values().asList())
                associateBy { it.id }
            }
        }

        fun translate(state: Interpreter.State): Insn = map.getValue(state.opcode).translate(state)
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
            val stackArgs = ArrayList<Expr>()
            if (invokeId == state.id) {
                // todo
                repeat(state.script.stringArgumentCount) { stackArgs.add(state.pop(Type.STRING)) }
                repeat(state.script.intArgumentCount) { stackArgs.add(state.pop(Type.INT)) }
            } else {
                val invoked = state.interpreter.interpret(invokeId)
                invoked.args.asReversed().forEach {
                    stackArgs.add(state.pop(it.type))
                }
                invoked.returns.forEach {
                    returns.add(state.push(it))
                }
            }
            args.addAll(stackArgs.asReversed())
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
            val enumId = state.pop(ENUM)
            val valueType = Type.of(checkNotNull(state.intStack.peek().cst))
            val valueTypeVar = state.pop(TYPE)
            val keyType = Type.of(checkNotNull(state.intStack.peek().cst))
            val keyTypeVar = state.pop(TYPE)
            val args = mutableListOf<Expr>(keyTypeVar, valueTypeVar, enumId, key)
            key.type = keyType
            val value = state.push(valueType)
            return Insn.Assignment(listOf(value), Expr.Operation(listOf(valueType), id, args))
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

    private object DefineArray : Op {

        override val id = Opcodes.DEFINE_ARRAY

        override fun translate(state: Interpreter.State): Insn {
            val length = state.pop(Type.INT)
            val intOperand = state.intOperand
            val arrayId = intOperand shr 16
            val arrayIdVar = Expr.Cst(Type.INT, arrayId)
            val typeDesc = intOperand and 0xFFFF
            val type = Expr.Cst(Type.TYPE, typeDesc)
            state.arrayTypes[arrayId] = Type.of(typeDesc)
            return Insn.Assignment(emptyList(), Expr.Operation(emptyList(), id, mutableListOf(arrayIdVar, type, length)))
        }
    }

    private object GetArrayInt : Op {

        override val id = Opcodes.GET_ARRAY_INT

        override fun translate(state: Interpreter.State): Insn {
            val arrayId = state.intOperand
            val arrayIdVar = Expr.Cst(Type.INT, arrayId)
            val arrayIndex = state.pop(Type.INT)
            val arrayType = state.arrayTypes[arrayId] ?: Type.INT
            val def = state.push(arrayType)
            return Insn.Assignment(listOf(def), Expr.Operation(listOf(arrayType), id, mutableListOf(arrayIdVar, arrayIndex)))
        }
    }

    private object SetArrayInt : Op {

        override val id = Opcodes.SET_ARRAY_INT

        override fun translate(state: Interpreter.State): Insn {
            val arrayId = state.intOperand
            val arrayType = state.arrayTypes[arrayId] ?: Type.INT
            val arrayIdVar = Expr.Cst(Type.INT, arrayId)
            val value = state.pop(arrayType)
            val arrayIndex = state.pop(Type.INT)
            return Insn.Assignment(emptyList(), Expr.Operation(emptyList(), id, mutableListOf(arrayIdVar, arrayIndex, value)))
        }
    }

    private data class Arg(val type: Type, val src: Src)

    private enum class Src {
        L, S, O
    }

    private enum class Basic(val args: Array<Arg> = emptyArray(), val defs: Array<Arg> = emptyArray()) : Op {
        GET_VAR(arrayOf(INT.o), arrayOf(INT.s)),
        SET_VAR(arrayOf(INT.o, INT.s)),
        GET_VARBIT(arrayOf(INT.o), arrayOf(INT.s)),
        SET_VARBIT(arrayOf(INT.o, INT.s)),
        PUSH_INT_LOCAL(arrayOf(INT.l), arrayOf(INT.s)),
        POP_INT_LOCAL(arrayOf(INT.s), arrayOf(INT.l)),
        PUSH_STRING_LOCAL(arrayOf(STRING.l), arrayOf(STRING.s)),
        POP_STRING_LOCAL(arrayOf(STRING.s), arrayOf(STRING.l)),
        POP_INT_DISCARD(arrayOf(INT.s)),
        POP_STRING_DISCARD(arrayOf(STRING.s)),
        GET_VARC_INT(arrayOf(INT.o), arrayOf(INT.s)),
        SET_VARC_INT(arrayOf(INT.o, INT.s)),
        GET_VARC_STRING_OLD(arrayOf(INT.o), arrayOf(STRING.s)),
        SET_VARC_STRING_OLD(arrayOf(INT.o, STRING.s)),
        GET_VARC_STRING(arrayOf(INT.o), arrayOf(STRING.s)),
        SET_VARC_STRING(arrayOf(INT.o, STRING.s)),
        CC_CREATE(arrayOf(COMPONENT.s, IFTYPE.s, INT.s, BOOLEAN.o)),
        CC_DELETE(arrayOf(BOOLEAN.o)),
        CC_DELETEALL(arrayOf(COMPONENT.s)),
        CC_FIND(arrayOf(COMPONENT.s, INT.s, BOOLEAN.o), arrayOf(BOOLEAN.s)),
        IF_FIND(arrayOf(COMPONENT.s, BOOLEAN.o), arrayOf(BOOLEAN.s)),

        CC_SETPOSITION(arrayOf(INT.s, INT.s, SETPOSH.s, SETPOSV.s, BOOLEAN.o)),
        CC_SETSIZE(arrayOf(INT.s, INT.s, SETSIZE.s, SETSIZE.s, BOOLEAN.o)),
        CC_SETHIDE(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        CC_SETNOCLICKTHROUGH(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        _1006(arrayOf(BOOLEAN.s, BOOLEAN.o)),

        CC_SETSCROLLPOS(arrayOf(INT.s, INT.s, BOOLEAN.o)),
        CC_SETCOLOUR(arrayOf(COLOUR.s, BOOLEAN.o)),
        CC_SETFILL(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        CC_SETTRANS(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETLINEWID(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETGRAPHIC(arrayOf(GRAPHIC.s, BOOLEAN.o)),
        CC_SET2DANGLE(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETTILING(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        CC_SETMODEL(arrayOf(MODEL.s, BOOLEAN.o)),
        CC_SETMODELANGLE(arrayOf(INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, BOOLEAN.o)),
        CC_SETMODELANIM(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETMODELORTHOG(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        CC_SETTEXT(arrayOf(STRING.s, BOOLEAN.o)),
        CC_SETTEXTFONT(arrayOf(FONTMETRICS.s, BOOLEAN.o)),
        CC_SETTEXTALIGN(arrayOf(SETTEXTALIGNH.s, SETTEXTALIGNV.s, INT.s, BOOLEAN.o)),
        CC_SETTEXTSHADOW(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        CC_SETOUTLINE(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETGRAPHICSHADOW(arrayOf(COLOUR.s, BOOLEAN.o)),
        CC_SETVFLIP(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        CC_SETHFLIP(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        CC_SETSCROLLSIZE(arrayOf(INT.s, INT.s, BOOLEAN.o)),
        CC_RESUME_PAUSEBUTTON(arrayOf(BOOLEAN.o)),
        _1122(arrayOf(GRAPHIC.s, BOOLEAN.o)),
        CC_SETFILLCOLOUR(arrayOf(COLOUR.s, BOOLEAN.o)),
        _1124(arrayOf(INT.s, BOOLEAN.o)),
        _1125(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETLINEDIRECTION(arrayOf(BOOLEAN.s, BOOLEAN.o)),
        _1127(arrayOf(BOOLEAN.s, BOOLEAN.o)),

        CC_SETOBJECT(arrayOf(OBJ.s, INT.s, BOOLEAN.o)),
        CC_SETNPCHEAD(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETPLAYERHEAD_SELF(arrayOf(BOOLEAN.o)),
        CC_SETOBJECT_NONUM(arrayOf(OBJ.s, INT.s, BOOLEAN.o)),
        CC_SETOBJECT_ALWAYS_NUM(arrayOf(OBJ.s, INT.s, BOOLEAN.o)),

        CC_SETOP(arrayOf(INT.s, STRING.s, BOOLEAN.o)),
        CC_SETDRAGGABLE(arrayOf(INT.s, INT.s, BOOLEAN.o)),
        CC_SETDRAGGABLEBEHAVIOR(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETDRAGDEADZONE(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETDRAGDEADTIME(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETOPBASE(arrayOf(STRING.s, BOOLEAN.o)),
        CC_SETTARGETVERB(arrayOf(STRING.s, BOOLEAN.o)),
        CC_CLEAROPS(arrayOf(BOOLEAN.o)),
        CC_SETOPKEY(arrayOf(INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, BOOLEAN.o)),
        CC_SETOPTKEY(arrayOf(INT.s, INT.s, BOOLEAN.o)),
        CC_SETOPKEYRATE(arrayOf(INT.s, INT.s, INT.s, BOOLEAN.o)),
        CC_SETOPTKEYRATE(arrayOf(INT.s, INT.s, BOOLEAN.o)),
        CC_SETOPKEYIGNOREHELD(arrayOf(INT.s, BOOLEAN.o)),
        CC_SETOPTKEYIGNOREHELD(arrayOf(BOOLEAN.o)),

        CC_GETX(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETY(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETWIDTH(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETHEIGHT(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETHIDE(arrayOf(BOOLEAN.o), arrayOf(BOOLEAN.s)),
        CC_GETLAYER(arrayOf(BOOLEAN.o), arrayOf(COMPONENT.s)),

        CC_GETSCROLLX(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETSCROLLY(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETTEXT(arrayOf(BOOLEAN.o), arrayOf(STRING.s)),
        CC_GETSCROLLWIDTH(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETSCROLLHEIGHT(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETMODELZOOM(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETMODELANGLE_X(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETMODELANGLE_Z(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETMODELANGLE_Y(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETTRANS(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        _1610(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETCOLOUR(arrayOf(BOOLEAN.o), arrayOf(COLOUR.s)),
        CC_GETFILLCOLOUR(arrayOf(BOOLEAN.o), arrayOf(COLOUR.s)),
        _1613(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        _1614(arrayOf(BOOLEAN.o), arrayOf(BOOLEAN.s)),

        CC_GETINVOBJECT(arrayOf(BOOLEAN.o), arrayOf(OBJ.s)),
        CC_GETINVCOUNT(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETID(arrayOf(BOOLEAN.o), arrayOf(INT.s)),

        CC_GETTARGETMASK(arrayOf(BOOLEAN.o), arrayOf(INT.s)),
        CC_GETOP(arrayOf(INT.s, BOOLEAN.o), arrayOf(STRING.s)),
        CC_GETOPBASE(arrayOf(BOOLEAN.o), arrayOf(STRING.s)),

        CC_CALLONRESIZE(arrayOf(BOOLEAN.s)),

        IF_SETPOSITION(arrayOf(INT.s, INT.s, SETPOSH.s, SETPOSV.s, COMPONENT.s)),
        IF_SETSIZE(arrayOf(INT.s, INT.s, SETSIZE.s, SETSIZE.s, COMPONENT.s)),
        IF_SETHIDE(arrayOf(BOOLEAN.s, COMPONENT.s)),
        IF_SETNOCLICKTHROUGH(arrayOf(BOOLEAN.s, COMPONENT.s)),
        _2006(arrayOf(BOOLEAN.s, COMPONENT.s)),

        IF_SETSCROLLPOS(arrayOf(INT.s, INT.s, COMPONENT.s)),
        IF_SETCOLOUR(arrayOf(COLOUR.s, COMPONENT.s)),
        IF_SETFILL(arrayOf(BOOLEAN.s, COMPONENT.s)),
        IF_SETTRANS(arrayOf(INT.s, COMPONENT.s)),
        IF_SETLINEWID(arrayOf(INT.s, COMPONENT.s)),
        IF_SETGRAPHIC(arrayOf(GRAPHIC.s, COMPONENT.s)),
        IF_SET2DANGLE(arrayOf(INT.s, COMPONENT.s)),
        IF_SETTILING(arrayOf(BOOLEAN.s, COMPONENT.s)),
        IF_SETMODEL(arrayOf(MODEL.s, COMPONENT.s)),
        IF_SETMODELANGLE(arrayOf(INT.s, INT.s, INT.s, INT.s, INT.s, INT.s, COMPONENT.s)),
        IF_SETMODELANIM(arrayOf(INT.s, COMPONENT.s)),
        IF_SETMODELORTHOG(arrayOf(BOOLEAN.s, COMPONENT.s)),
        IF_SETTEXT(arrayOf(STRING.s, COMPONENT.s)),
        IF_SETTEXTFONT(arrayOf(FONTMETRICS.s, COMPONENT.s)),
        IF_SETTEXTALIGN(arrayOf(SETTEXTALIGNH.s, SETTEXTALIGNV.s, INT.s, COMPONENT.s)),
        IF_SETTEXTSHADOW(arrayOf(BOOLEAN.s, COMPONENT.s)),
        IF_SETOUTLINE(arrayOf(INT.s, COMPONENT.s)),
        IF_SETGRAPHICSHADOW(arrayOf(COLOUR.s, COMPONENT.s)),
        IF_SETVFLIP(arrayOf(BOOLEAN.s, COMPONENT.s)),
        IF_SETHFLIP(arrayOf(BOOLEAN.s, COMPONENT.s)),
        IF_SETSCROLLSIZE(arrayOf(INT.s, INT.s, COMPONENT.s)),
        IF_RESUME_PAUSEBUTTON(arrayOf(COMPONENT.s)),
        _2122(arrayOf(GRAPHIC.s, COMPONENT.s)),
        IF_SETFILLCOLOUR(arrayOf(COLOUR.s, COMPONENT.s)),
        _2124(arrayOf(INT.s, COMPONENT.s)),
        _2125(arrayOf(INT.s, COMPONENT.s)),
        IF_SETLINEDIRECTION(arrayOf(BOOLEAN.s, COMPONENT.s)),
        _2127(arrayOf(BOOLEAN.s, COMPONENT.s)),

        IF_SETOBJECT(arrayOf(OBJ.s, INT.s, COMPONENT.s)),
        IF_SETNPCHEAD(arrayOf(INT.s, COMPONENT.s)),
        IF_SETPLAYERHEAD_SELF(arrayOf(COMPONENT.s)),
        IF_SETOBJECT_NONUM(arrayOf(OBJ.s, INT.s, COMPONENT.s)),
        IF_SETOBJECT_ALWAYS_NUM(arrayOf(OBJ.s, INT.s, COMPONENT.s)),

        IF_SETOP(arrayOf(INT.s, STRING.s, COMPONENT.s)),
        IF_SETDRAGGABLE(arrayOf(INT.s, INT.s, COMPONENT.s)),
        IF_SETDRAGGABLEBEHAVIOR(arrayOf(INT.s, COMPONENT.s)),
        IF_SETDRAGDEADZONE(arrayOf(INT.s, COMPONENT.s)),
        IF_SETDRAGDEADTIME(arrayOf(INT.s, COMPONENT.s)),
        IF_SETOPBASE(arrayOf(STRING.s, COMPONENT.s)),
        IF_SETTARGETVERB(arrayOf(STRING.s, COMPONENT.s)),
        IF_CLEAROPS(arrayOf(COMPONENT.s)),
        IF_SETOPKEY(arrayOf(INT.s, INT.s, INT.s, COMPONENT.s)),
        IF_SETOPTKEY(arrayOf(INT.s, INT.s, COMPONENT.s)),
        IF_SETOPKEYRATE(arrayOf(INT.s, INT.s, INT.s, COMPONENT.s)),
        IF_SETOPTKEYRATE(arrayOf(INT.s, INT.s, COMPONENT.s)),
        IF_SETOPKEYIGNOREHELD(arrayOf(INT.s, COMPONENT.s)),
        IF_SETOPTKEYIGNOREHELD(arrayOf(COMPONENT.s)),

        IF_GETX(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETY(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETWIDTH(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETHEIGHT(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETHIDE(arrayOf(COMPONENT.s), arrayOf(BOOLEAN.s)),
        IF_GETLAYER(arrayOf(COMPONENT.s), arrayOf(COMPONENT.s)),

        IF_GETSCROLLX(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETSCROLLY(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETTEXT(arrayOf(COMPONENT.s), arrayOf(STRING.s)),
        IF_GETSCROLLWIDTH(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETSCROLLHEIGHT(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETMODELZOOM(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETMODELANGLE_X(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETMODELANGLE_Z(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETMODELANGLE_Y(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETTRANS(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        _2610(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETCOLOUR(arrayOf(COMPONENT.s), arrayOf(COLOUR.s)),
        IF_GETFILLCOLOUR(arrayOf(COMPONENT.s), arrayOf(COLOUR.s)),
        _2613(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        _2614(arrayOf(COMPONENT.s), arrayOf(BOOLEAN.s)),

        IF_GETINVOBJECT(arrayOf(COMPONENT.s), arrayOf(OBJ.s)),
        IF_GETINVCOUNT(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_HASSUB(arrayOf(COMPONENT.s), arrayOf(BOOLEAN.s)),
        IF_GETTOP(defs = arrayOf(INT.s)),

        IF_GETTARGETMASK(arrayOf(COMPONENT.s), arrayOf(INT.s)),
        IF_GETOP(arrayOf(INT.s, COMPONENT.s), arrayOf(STRING.s)),
        IF_GETOPBASE(arrayOf(COMPONENT.s), arrayOf(STRING.s)),

        IF_CALLONRESIZE(arrayOf(COMPONENT.s, BOOLEAN.o)),

        MES(arrayOf(STRING.s)),
        ANIM(arrayOf(INT.s, INT.s)),
        IF_CLOSE,
        RESUME_COUNTDIALOG(arrayOf(STRING.s)),
        RESUME_NAMEDIALOG(arrayOf(STRING.s)),
        RESUME_STRINGDIALOG(arrayOf(STRING.s)),
        OPPLAYER(arrayOf(INT.s, STRING.s)),
        IF_DRAGPICKUP(arrayOf(COMPONENT.s, INT.s, INT.s)),
        CC_DRAGPICKUP(arrayOf(INT.s, INT.s, BOOLEAN.o)),
        MOUSECAM(arrayOf(BOOLEAN.s)),
        GETREMOVEROOFS(defs = arrayOf(BOOLEAN.s)),
        SETREMOVEROOFS(arrayOf(BOOLEAN.s)),
        OPENURL(arrayOf(STRING.s, BOOLEAN.s)),
        RESUME_OBJDIALOG(arrayOf(INT.s)),
        BUG_REPORT(arrayOf(INT.s, STRING.s, STRING.s)),
        SETSHIFTCLICKDROP(arrayOf(BOOLEAN.s)),
        SETSHOWMOUSEOVERTEXT(arrayOf(BOOLEAN.s)),
        RENDERSELF(arrayOf(BOOLEAN.s)),
        _3120(arrayOf(BOOLEAN.s)),
        _3121(arrayOf(BOOLEAN.s)),
        _3122(arrayOf(BOOLEAN.s)),
        _3123(arrayOf(BOOLEAN.s)),
        _3124,
        SETSHOWMOUSECROSS(arrayOf(BOOLEAN.s)),
        SETSHOWLOADINGMESSAGES(arrayOf(BOOLEAN.s)),
        SETTAPTODROP(arrayOf(BOOLEAN.s)),
        GETTAPTODROP(defs = arrayOf(BOOLEAN.s)),
        _3129(arrayOf(INT.s, INT.s)),
        _3130(arrayOf(INT.s, INT.s)),
        _3131(arrayOf(INT.s)),
        GETCANVASSIZE(defs = arrayOf(INT.s, INT.s)),
        _3133(arrayOf(INT.s)),
        _3134,
        _3135(arrayOf(INT.s, INT.s)),
        _3136(arrayOf(BOOLEAN.s)),
        _3137(arrayOf(BOOLEAN.s)),
        _3138,
        _3139,
        _3140(arrayOf(BOOLEAN.o)),
        SETHIDEUSERNAME(arrayOf(BOOLEAN.s)),
        GETHIDEUSERNAME(defs = arrayOf(BOOLEAN.s)),
        SETREMEMBERUSERNAME(arrayOf(BOOLEAN.s)),
        GETREMEMBERUSERNAME(defs = arrayOf(BOOLEAN.s)),
        _3145,

        SOUND_SYNTH(arrayOf(SYNTH.s, INT.s, INT.s)),
        SOUND_SONG(arrayOf(INT.s)),
        SOUND_JINGLE(arrayOf(INT.s, INT.s)),

        CLIENTCLOCK(defs = arrayOf(INT.s)),
        INV_GETOBJ(arrayOf(INV.s, INT.s), arrayOf(OBJ.s)),
        INV_GETNUM(arrayOf(INV.s, INT.s), arrayOf(INT.s)),
        INV_TOTAL(arrayOf(INV.s, OBJ.s), arrayOf(INT.s)),
        INV_SIZE(arrayOf(INV.s), arrayOf(INT.s)),
        STAT(arrayOf(Type.STAT.s), arrayOf(INT.s)),
        STAT_BASE(arrayOf(Type.STAT.s), arrayOf(INT.s)),
        STAT_XP(arrayOf(Type.STAT.s), arrayOf(INT.s)),
        COORD(defs = arrayOf(Type.COORD.s)),
        COORDX(arrayOf(Type.COORD.s), arrayOf(INT.s)),
        COORDZ(arrayOf(Type.COORD.s), arrayOf(INT.s)),
        COORDY(arrayOf(Type.COORD.s), arrayOf(INT.s)),
        MAP_MEMBERS(defs = arrayOf(BOOLEAN.s)),
        INVOTHER_GETOBJ(arrayOf(INV.s, INT.s), arrayOf(OBJ.s)),
        INVOTHER_GETNUM(arrayOf(INV.s, INT.s), arrayOf(INT.s)),
        INVOTHER_TOTAL(arrayOf(INV.s, OBJ.s), arrayOf(INT.s)),
        STAFFMODLEVEL(defs = arrayOf(INT.s)),
        REBOOTTIMER(defs = arrayOf(INT.s)),
        MAP_WORLD(defs = arrayOf(INT.s)),
        RUNENERGY_VISIBLE(defs = arrayOf(INT.s)),
        RUNWEIGHT_VISIBLE(defs = arrayOf(INT.s)),
        PLAYERMOD(defs = arrayOf(BOOLEAN.s)),
        WORLDFLAGS(defs = arrayOf(INT.s)),
        MOVECOORD(arrayOf(Type.COORD.s, INT.s, INT.s, INT.s), arrayOf(Type.COORD.s)),

        ENUM_STRING(arrayOf(ENUM.s, INT.s), arrayOf(STRING.s)),
        ENUM_GETOUTPUTCOUNT(arrayOf(ENUM.s), arrayOf(INT.s)),

        FRIEND_COUNT(defs = arrayOf(INT.s)),
        FRIEND_GETNAME(arrayOf(INT.s), arrayOf(STRING.s, STRING.s)),
        FRIEND_GETWORLD(arrayOf(INT.s), arrayOf(INT.s)),
        FRIEND_GETRANK(arrayOf(INT.s), arrayOf(INT.s)),
        FRIEND_SETRANK(arrayOf(STRING.s, INT.s)),
        FRIEND_ADD(arrayOf(STRING.s)),
        FRIEND_DEL(arrayOf(STRING.s)),
        IGNORE_ADD(arrayOf(STRING.s)),
        IGNORE_DEL(arrayOf(STRING.s)),
        FRIEND_TEST(arrayOf(STRING.s), arrayOf(BOOLEAN.s)),
        CLAN_GETCHATDISPLAYNAME(defs = arrayOf(STRING.s)),
        CLAN_GETCHATCOUNT(defs = arrayOf(INT.s)),
        CLAN_GETCHATUSERNAME(arrayOf(INT.s), arrayOf(STRING.s)),
        CLAN_GETCHATUSERWORLD(arrayOf(INT.s), arrayOf(INT.s)),
        CLAN_GETCHATUSERRANK(arrayOf(INT.s), arrayOf(INT.s)),
        CLAN_GETCHATMINKICK(defs = arrayOf(INT.s)),
        CLAN_KICKUSER(arrayOf(STRING.s)),
        CLAN_GETCHATRANK(defs = arrayOf(INT.s)),
        CLAN_JOINCHAT(arrayOf(STRING.s)),
        CLAN_LEAVECHAT,
        IGNORE_COUNT(defs = arrayOf(INT.s)),
        IGNORE_GETNAME(arrayOf(INT.s), arrayOf(STRING.s, STRING.s)),
        IGNORE_TEST(arrayOf(STRING.s), arrayOf(BOOLEAN.s)),
        CLAN_ISSELF(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        CLAN_GETCHATOWNERNAME(defs = arrayOf(STRING.s)),
        CLAN_ISFRIEND(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        CLAN_ISIGNORE(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        _3628,
        _3629(arrayOf(BOOLEAN.s)),
        _3630(arrayOf(BOOLEAN.s)),
        _3631(arrayOf(BOOLEAN.s)),
        _3632(arrayOf(BOOLEAN.s)),
        _3633(arrayOf(BOOLEAN.s)),
        _3634(arrayOf(BOOLEAN.s)),
        _3635(arrayOf(BOOLEAN.s)),
        _3636(arrayOf(BOOLEAN.s)),
        _3637(arrayOf(BOOLEAN.s)),
        _3638(arrayOf(BOOLEAN.s)),
        _3639,
        _3640,
        _3641(arrayOf(BOOLEAN.s)),
        _3642(arrayOf(BOOLEAN.s)),
        _3643,
        _3644,
        _3645(arrayOf(BOOLEAN.s)),
        _3646(arrayOf(BOOLEAN.s)),
        _3647(arrayOf(BOOLEAN.s)),
        _3648(arrayOf(BOOLEAN.s)),
        _3649(arrayOf(BOOLEAN.s)),
        _3650(arrayOf(BOOLEAN.s)),
        _3651(arrayOf(BOOLEAN.s)),
        _3652(arrayOf(BOOLEAN.s)),
        _3653(arrayOf(BOOLEAN.s)),
        _3654(arrayOf(BOOLEAN.s)),
        _3655,
        _3656(arrayOf(BOOLEAN.s)),
        _3657(arrayOf(BOOLEAN.s)),

        STOCKMARKET_GETOFFERTYPE(arrayOf(INT.s), arrayOf(INT.s)),
        STOCKMARKET_GETOFFERITEM(arrayOf(INT.s), arrayOf(OBJ.s)),
        STOCKMARKET_GETOFFERPRICE(arrayOf(INT.s), arrayOf(INT.s)),
        STOCKMARKET_GETOFFERCOUNT(arrayOf(INT.s), arrayOf(INT.s)),
        STOCKMARKET_GETOFFERCOMPLETEDCOUNT(arrayOf(INT.s), arrayOf(INT.s)),
        STOCKMARKET_GETOFFERCOMPLETEDGOLD(arrayOf(INT.s), arrayOf(INT.s)),
        STOCKMARKET_ISOFFEREMPTY(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        STOCKMARKET_ISOFFERSTABLE(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        STOCKMARKET_ISOFFERFINISHED(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        STOCKMARKET_ISOFFERADDING(arrayOf(INT.s), arrayOf(BOOLEAN.s)),

        TRADINGPOST_SORTBY_NAME(arrayOf(BOOLEAN.s)),
        TRADINGPOST_SORTBY_PRICE(arrayOf(BOOLEAN.s)),
        TRADINGPOST_SORTFILTERBY_WORLD(arrayOf(BOOLEAN.s, BOOLEAN.s)),
        TRADINGPOST_SORTBY_AGE(arrayOf(BOOLEAN.s)),
        TRADINGPOST_SORTBY_COUNT(arrayOf(BOOLEAN.s)),
        TRADINGPOST_GETTOTALOFFERS(defs = arrayOf(INT.s)),
        TRADINGPOST_GETOFFERWORLD(arrayOf(INT.s), arrayOf(INT.s)),
        TRADINGPOST_GETOFFERNAME(arrayOf(INT.s), arrayOf(STRING.s)),
        TRADINGPOST_GETOFFERPREVIOUSNAME(arrayOf(INT.s), arrayOf(STRING.s)),
        TRADINGPOST_GETOFFERAGE(arrayOf(INT.s), arrayOf(STRING.s)),
        TRADINGPOST_GETOFFERCOUNT(arrayOf(INT.s), arrayOf(INT.s)),
        TRADINGPOST_GETOFFERPRICE(arrayOf(INT.s), arrayOf(INT.s)),
        TRADINGPOST_GETOFFERITEM(arrayOf(INT.s), arrayOf(INT.s)),

        ADD(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        SUB(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        MULTIPLY(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        DIV(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        RANDOM(arrayOf(INT.s), arrayOf(INT.s)),
        RANDOMINC(arrayOf(INT.s), arrayOf(INT.s)),
        INTERPOLATE(arrayOf(INT.s, INT.s, INT.s, INT.s, INT.s), arrayOf(INT.s)),
        ADDPERCENT(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        SETBIT(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        CLEARBIT(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        TESTBIT(arrayOf(INT.s, INT.s), arrayOf(BOOLEAN.s)),
        MOD(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        POW(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        INVPOW(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        AND(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        OR(arrayOf(INT.s, INT.s), arrayOf(INT.s)),
        SCALE(arrayOf(INT.s, INT.s, INT.s), arrayOf(INT.s)),
        APPEND_NUM(arrayOf(STRING.s, INT.s), arrayOf(STRING.s)),
        APPEND(arrayOf(STRING.s, STRING.s), arrayOf(STRING.s)),
        APPEND_SIGNNUM(arrayOf(STRING.s, INT.s), arrayOf(STRING.s)),
        LOWERCASE(arrayOf(STRING.s), arrayOf(STRING.s)),
        FROMDATE(arrayOf(INT.s), arrayOf(STRING.s)),
        TEXT_GENDER(arrayOf(STRING.s, STRING.s), arrayOf(STRING.s)),
        TOSTRING(arrayOf(INT.s), arrayOf(STRING.s)),
        COMPARE(arrayOf(STRING.s, STRING.s), arrayOf(INT.s)),
        PARAHEIGHT(arrayOf(INT.s, FONTMETRICS.s, STRING.s), arrayOf(INT.s)),
        PARAWIDTH(arrayOf(INT.s, FONTMETRICS.s, STRING.s), arrayOf(INT.s)),
        TEXT_SWITCH(arrayOf(INT.s, STRING.s, STRING.s), arrayOf(STRING.s)),
        ESCAPE(arrayOf(STRING.s), arrayOf(STRING.s)),
        APPEND_CHAR(arrayOf(STRING.s, CHAR.s), arrayOf(STRING.s)),
        CHAR_ISPRINTABLE(arrayOf(CHAR.s), arrayOf(BOOLEAN.s)),
        CHAR_ISALPHANUMERIC(arrayOf(CHAR.s), arrayOf(BOOLEAN.s)),
        CHAR_ISALPHA(arrayOf(CHAR.s), arrayOf(BOOLEAN.s)),
        CHAR_ISNUMERIC(arrayOf(CHAR.s), arrayOf(BOOLEAN.s)),
        STRING_LENGTH(arrayOf(STRING.s), arrayOf(INT.s)),
        SUBSTRING(arrayOf(STRING.s, INT.s, INT.s), arrayOf(STRING.s)),
        REMOVETAGS(arrayOf(STRING.s), arrayOf(STRING.s)),
        STRING_INDEXOF_CHAR(arrayOf(STRING.s, CHAR.s), arrayOf(INT.s)),
        STRING_INDEXOF_STRING(arrayOf(STRING.s, STRING.s, INT.s), arrayOf(INT.s)),

        OC_NAME(arrayOf(OBJ.s), arrayOf(STRING.s)),
        OC_OP(arrayOf(OBJ.s, INT.s), arrayOf(STRING.s)),
        OC_IOP(arrayOf(OBJ.s, INT.s), arrayOf(STRING.s)),
        OC_COST(arrayOf(OBJ.s), arrayOf(INT.s)),
        OC_STACKABLE(arrayOf(OBJ.s), arrayOf(BOOLEAN.s)),
        OC_CERT(arrayOf(OBJ.s), arrayOf(OBJ.s)),
        OC_UNCERT(arrayOf(OBJ.s), arrayOf(OBJ.s)),
        OC_MEMBERS(arrayOf(OBJ.s), arrayOf(BOOLEAN.s)),
        OC_PLACEHOLDER(arrayOf(OBJ.s), arrayOf(OBJ.s)),
        OC_UNPLACEHOLDER(arrayOf(OBJ.s), arrayOf(OBJ.s)),
        OC_FIND(arrayOf(BOOLEAN.s, STRING.s), arrayOf(INT.s)),
        OC_FINDNEXT(defs = arrayOf(OBJ.s)),
        OC_FINDRESET,

        CHAT_GETFILTER_PUBLIC(defs = arrayOf(INT.s)),
        CHAT_SETFILTER(arrayOf(INT.s, INT.s, INT.s)),
        CHAT_SENDABUSEREPORT(arrayOf(STRING.s, INT.s, INT.s)),
        CHAT_GETHISTORY_BYTYPEANDLINE(arrayOf(CHATTYPE.s, INT.s), arrayOf(INT.s, INT.s, INT.s, STRING.s, STRING.s, STRING.s)),
        CHAT_GETHISTORY_BYUID(arrayOf(INT.s), arrayOf(CHATTYPE.s, INT.s, INT.s, STRING.s, STRING.s, STRING.s)),
        CHAT_GETFILTER_PRIVATE(defs = arrayOf(INT.s)),
        CHAT_SENDPUBLIC(arrayOf(STRING.s, INT.s)),
        CHAT_SENDPRIVATE(arrayOf(STRING.s, STRING.s)),
        CHAT_PLAYERNAME(defs = arrayOf(STRING.s)),
        CHAT_GETFILTER_TRADE(defs = arrayOf(INT.s)),
        CHAT_GETHISTORYLENGTH(arrayOf(CHATTYPE.s), arrayOf(INT.s)),
        CHAT_GETNEXTUID(arrayOf(INT.s), arrayOf(INT.s)),
        CHAT_GETPREVUID(arrayOf(INT.s), arrayOf(INT.s)),
        DOCHEAT(arrayOf(STRING.s)),
        CHAT_SETMESSAGEFILTER(arrayOf(STRING.s)),
        CHAT_GETMESSAGEFILTER(defs = arrayOf(STRING.s)),

        GETWINDOWMODE(defs = arrayOf(INT.s)),
        SETWINDOWMODE(arrayOf(INT.s)),
        GETDEFAULTWINDOWMODE(defs = arrayOf(INT.s)),
        SETDEFAULTWINDOWMODE(arrayOf(INT.s)),

        CAM_FORCEANGLE(arrayOf(INT.s, INT.s)),
        CAM_GETANGLE_XA(defs = arrayOf(INT.s)),
        CAM_GETANGLE_YA(defs = arrayOf(INT.s)),
        CAM_SETFOLLOWHEIGHT(arrayOf(INT.s)),
        CAM_GETFOLLOWHEIGHT(defs = arrayOf(INT.s)),

        LOGOUT,

        VIEWPORT_SETFOV(arrayOf(INT.s, INT.s)),
        VIEWPORT_SETZOOM(arrayOf(INT.s, INT.s)),
        VIEWPORT_CLAMPFOV(arrayOf(INT.s, INT.s, INT.s, INT.s)),
        VIEWPORT_GETEFFECTIVESIZE(defs = arrayOf(INT.s, INT.s)),
        VIEWPORT_GETZOOM(defs = arrayOf(INT.s, INT.s)),
        VIEWPORT_GETFOV(defs = arrayOf(INT.s, INT.s)),

        WORLDLIST_FETCH(defs = arrayOf(BOOLEAN.s)),
        WORLDLIST_START(defs = arrayOf(INT.s, INT.s, STRING.s, INT.s, INT.s, STRING.s)),
        WORLDLIST_NEXT(defs = arrayOf(INT.s, INT.s, STRING.s, INT.s, INT.s, STRING.s)),
        WORLDLIST_SPECIFIC(arrayOf(INT.s), arrayOf(INT.s, INT.s, STRING.s, INT.s, INT.s, STRING.s)),
        WORLDLIST_SORT(arrayOf(INT.s, BOOLEAN.s, INT.s, BOOLEAN.s)),
        _6511(arrayOf(INT.s), arrayOf(INT.s, INT.s, STRING.s, INT.s, INT.s, STRING.s)),
        SETFOLLOWEROPSLOWPRIORITY(arrayOf(BOOLEAN.s)),

        ON_MOBILE(defs = arrayOf(BOOLEAN.s)),
        CLIENTTYPE(defs = arrayOf(INT.s)),
        _6520,
        _6521,
        _6522(arrayOf(INT.s, STRING.s)),
        _6523(arrayOf(INT.s, STRING.s)),
        BATTERYLEVEL(defs = arrayOf(INT.s)),
        BATTERYCHARGING(defs = arrayOf(BOOLEAN.s)),
        WIFIAVAILABLE(defs = arrayOf(BOOLEAN.s)),

        _6600,
        WORLDMAP_GETMAPNAME(arrayOf(MAPAREA.s), arrayOf(STRING.s)),
        WORLDMAP_SETMAP(arrayOf(MAPAREA.s)),
        WORLDMAP_GETZOOM(defs = arrayOf(INT.s)),
        WORLDMAP_SETZOOM(arrayOf(INT.s)),
        WORLDMAP_ISLOADED(defs = arrayOf(BOOLEAN.s)),
        WORLDMAP_JUMPTODISPLAYCOORD(arrayOf(Type.COORD.s)),
        WORLDMAP_JUMPTODISPLAYCOORD_INSTANT(arrayOf(Type.COORD.s)),
        WORLDMAP_JUMPTOSOURCECOORD(arrayOf(Type.COORD.s)),
        WORLDMAP_JUMPTOSOURCECOORD_INSTANT(arrayOf(Type.COORD.s)),
        WORLDMAP_GETDISPLAYPOSITION(defs = arrayOf(INT.s, INT.s)),
        WORLDMAP_GETCONFIGORIGIN(arrayOf(MAPAREA.s), arrayOf(INT.s)),
        WORLDMAP_GETCONFIGSIZE(arrayOf(MAPAREA.s), arrayOf(INT.s, INT.s)),
        WORLDMAP_GETCONFIGBOUNDS(arrayOf(MAPAREA.s), arrayOf(INT.s, INT.s, INT.s, INT.s)),
        WORLDMAP_GETCONFIGZOOM(arrayOf(MAPAREA.s), arrayOf(INT.s)),
        _6615(defs = arrayOf(INT.s, INT.s)),
        WORLDMAP_GETCURRENTMAP(defs = arrayOf(MAPAREA.s)),
        WORLDMAP_GETDISPLAYCOORD(arrayOf(Type.COORD.s), arrayOf(INT.s, INT.s)),
        _6618(arrayOf(Type.COORD.s), arrayOf(INT.s, INT.s)),
        _6619(arrayOf(INT.s, Type.COORD.s)),
        _6620(arrayOf(INT.s, Type.COORD.s)),
        WORLDMAP_COORDINMAP(arrayOf(MAPAREA.s, Type.COORD.s), arrayOf(BOOLEAN.s)),
        WORLDMAP_GETSIZE(defs = arrayOf(INT.s, INT.s)),
        _6623(arrayOf(Type.COORD.s), arrayOf(INT.s)),
        _6624(arrayOf(INT.s)),
        _6625,
        _6626(arrayOf(INT.s)),
        _6627,
        WORLDMAP_PERPETUALFLASH(arrayOf(INT.s)),
        WORLDMAP_FLASHELEMENT(arrayOf(INT.s)),
        WORLDMAP_FLASHELEMENTCATEGORY(arrayOf(CATEGORY.s)),
        WORLDMAP_STOPCURRENTFLASHES,
        WORLDMAP_DISABLEELEMENTS(arrayOf(BOOLEAN.s)),
        WORLDMAP_DISABLEELEMENT(arrayOf(INT.s, BOOLEAN.s)),
        WORLDMAP_DISABLEELEMENTCATEGORY(arrayOf(INT.s, BOOLEAN.s)),
        WORLDMAP_GETDISABLEELEMENTS(defs = arrayOf(BOOLEAN.s)),
        WORLDMAP_GETDISABLEELEMENT(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        WORLDMAP_GETDISABLEELEMENTCATEGORY(arrayOf(INT.s), arrayOf(BOOLEAN.s)),
        _6638(arrayOf(INT.s, Type.COORD.s), arrayOf(INT.s)),
        WORLDMAP_LISTELEMENT_START(defs = arrayOf(INT.s, INT.s)),
        WORLDMAP_LISTELEMENT_NEXT(defs = arrayOf(INT.s, INT.s)),
        MEC_TEXT(arrayOf(INT.s), arrayOf(STRING.s)),
        MEC_TEXTSIZE(arrayOf(INT.s), arrayOf(INT.s)),
        MEC_CATEGORY(arrayOf(INT.s), arrayOf(CATEGORY.s)),
        MEC_SPRITE(arrayOf(INT.s), arrayOf(INT.s)),
        _6697(defs = arrayOf(INT.s)),
        _6698(defs = arrayOf(Type.COORD.s)),
        _6699(defs = arrayOf(Type.COORD.s)),
        ;

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val stackArgs = ListStack<Expr.Var>(args.size)
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
            val args = MutableList<Expr>(state.intOperand) { state.pop(Type.STRING) }
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
        CC_SETONTIMER,
        CC_SETONOP,
        CC_SETONDRAGCOMPLETE,
        CC_SETONCLICKREPEAT,
        CC_SETONMOUSEREPEAT,
        CC_SETONINVTRANSMIT,
        CC_SETONSTATTRANSMIT,
        CC_SETONTARGETENTER,
        CC_SETONSCROLLWHEEL,
        CC_SETONCHATTRANSMIT,
        CC_SETONKEY,
        CC_SETONFRIENDTRANSMIT,
        CC_SETONCLANTRANSMIT,
        CC_SETONMISCTRANSMIT,
        CC_SETONDIALOGABORT,
        CC_SETONSUBCHANGE,
        CC_SETONSTOCKTRANSMIT,
        _1426,
        CC_SETONRESIZE,
        IF_SETONCLICK,
        IF_SETONHOLD,
        IF_SETONRELEASE,
        IF_SETONMOUSEOVER,
        IF_SETONMOUSELEAVE,
        IF_SETONDRAG,
        IF_SETONTARGETLEAVE,
        IF_SETONVARTRANSMIT,
        IF_SETONTIMER,
        IF_SETONOP,
        IF_SETONDRAGCOMPLETE,
        IF_SETONCLICKREPEAT,
        IF_SETONMOUSEREPEAT,
        IF_SETONINVTRANSMIT,
        IF_SETONSTATTRANSMIT,
        IF_SETONTARGETENTER,
        IF_SETONSCROLLWHEEL,
        IF_SETONCHATTRANSMIT,
        IF_SETONKEY,
        IF_SETONFRIENDTRANSMIT,
        IF_SETONCLANTRANSMIT,
        IF_SETONMISCTRANSMIT,
        IF_SETONDIALOGABORT,
        IF_SETONSUBCHANGE,
        IF_SETONSTOCKTRANSMIT,
        _2426,
        IF_SETONRESIZE;

        override val id = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val args = ArrayList<Expr>()
            if (id >= 2000) {
                args.add(state.pop(Type.COMPONENT))
            } else {
                args.add(state.operand(Type.BOOLEAN))
            }
            var s = checkNotNull(state.strStack.pop().cst)
            if (s.isNotEmpty() && s.last() == 'Y') {
                val triggerType = when (id) {
                    Opcodes.IF_SETONSTATTRANSMIT, Opcodes.CC_SETONSTATTRANSMIT -> Type.STAT
                    Opcodes.IF_SETONINVTRANSMIT, Opcodes.CC_SETONINVTRANSMIT -> Type.INV
                    Opcodes.IF_SETONVARTRANSMIT, Opcodes.CC_SETONVARTRANSMIT -> Type.VAR
                    else -> error(this)
                }
                val n = checkNotNull(state.intStack.pop().cst)
                args.add(Expr.Cst(Type.INT, n))
                repeat(n) {
                    args.add(state.pop(triggerType))
                }
                s = s.dropLast(1)
            } else {
                args.add(Expr.Cst(Type.INT, 0))
            }
            for (i in s.lastIndex downTo 0) {
                args.add(state.pop(Type.of(s[i])))
            }
            args.add(state.pop(Type.INT))
            args.reverse()
            return Insn.Assignment(emptyList(), Expr.Operation(emptyList(), id, args))
        }
    }

    enum class ParamKey(val type: Type) : Op {
        NC_PARAM(Type.INT),
        LC_PARAM(Type.LOC),
        OC_PARAM(Type.OBJ),
        STRUCT_PARAM(Type.STRUCT),
        ;

        override val id = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val paramKeyId = checkNotNull(state.intStack.peek().cst)
            val param = state.pop(Type.PARAM)
            val rec = state.pop(type)
            val paramType = checkNotNull(state.interpreter.paramTypeLoader.load(paramKeyId))
            return Insn.Assignment(listOf(state.push(paramType)), Expr.Operation(listOf(paramType), id, mutableListOf(rec, param)))
        }
    }
}