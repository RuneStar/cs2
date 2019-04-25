package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.Type.*
import org.runestar.cs2.namesReverse

internal interface Op {

    val id: Int

    fun translate(state: Interpreter.State): Instruction

    companion object {

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
                addAll(BranchCompare.values().asList())
                addAll(Assign.values().asList())
                addAll(Basic.values().asList())
                addAll(SetOn.values().asList())
                addAll(ParamKey.values().asList())
                associateBy { it.id }
            }
        }

        fun translate(state: Interpreter.State): Instruction = map.getValue(state.opcode).translate(state)
    }

    private object Switch : Op {

        override val id = Opcodes.SWITCH

        override fun translate(state: Interpreter.State): Instruction {
            return Instruction.Switch(state.pop(INT), state.switch.mapValues { Instruction.Label(it.value + 1 + state.pc) })
        }
    }

    private object Branch : Op {

        override val id = Opcodes.BRANCH

        override fun translate(state: Interpreter.State): Instruction {
            return Instruction.Goto(Instruction.Label(state.pc + state.intOperand + 1))
        }
    }

    private object Invoke : Op {

        override val id = Opcodes.INVOKE

        override fun translate(state: Interpreter.State): Instruction {
            val invokeId = state.intOperand
            val invoked = checkNotNull(state.interpreter.scriptLoader.load(invokeId))
            val args = ArrayList<Element>()
            args.add(Element.Constant(invokeId))
            args.addAll(state.take(List(invoked.intArgumentCount) { INT } + List(invoked.stringArgumentCount) { STRING }))
            val returns = invoked.returnTypes.map { state.push(it) }
            return Instruction.Assignment(Expression(returns), Expression.Operation(returns.map { it.type }, id, Expression(args)))
        }
    }

    private object Return : Op {

        override val id = Opcodes.RETURN

        override fun translate(state: Interpreter.State): Instruction {
            return Instruction.Return(Expression(state.takeAll()))
        }
    }

    private object GetEnum : Op {

        override val id = Opcodes.ENUM

        override fun translate(state: Interpreter.State): Instruction {
            val key = state.pop(INT)
            val enumId = state.pop(ENUM)
            val valueType = Type.of(state.stack.peek().value as Int)
            val valueTypeVar = state.pop(TYPE)
            val keyType = Type.of(state.stack.peek().value as Int)
            val keyTypeVar = state.pop(TYPE)
            val args = Expression(keyTypeVar, valueTypeVar, enumId, key)
            key.type = keyType
            return Instruction.Assignment(state.push(valueType), Expression.Operation(listOf(valueType), id, args))
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

        override fun translate(state: Interpreter.State): Instruction {
            val r = state.pop(INT)
            val l = state.pop(INT)
            val expr = Expression.Operation(emptyList(), id, Expression(l, r))
            return Instruction.Branch(expr, Instruction.Label(state.pc + state.intOperand + 1))
        }
    }

    private object DefineArray : Op {

        override val id = Opcodes.DEFINE_ARRAY

        override fun translate(state: Interpreter.State): Instruction {
            val length = state.pop(INT)
            val arrayId = state.intOperand shr 16
            val arrayIdVar = Element.Constant(arrayId)
            val typeDesc = state.intOperand and 0xFFFF
            val type = Element.Constant(typeDesc, TYPE)
            state.arrayTypes[arrayId] = Type.of(typeDesc)
            return Instruction.Assignment(Expression(), Expression.Operation(emptyList(), id, Expression(arrayIdVar, type, length)))
        }
    }

    private object GetArrayInt : Op {

        override val id = Opcodes.GET_ARRAY_INT

        override fun translate(state: Interpreter.State): Instruction {
            val arrayId = state.intOperand
            val arrayIdVar = Element.Constant(arrayId)
            val arrayIndex = state.pop(INT)
            val arrayType = state.arrayTypes[arrayId] ?: INT
            return Instruction.Assignment(state.push(arrayType), Expression.Operation(listOf(arrayType), id, Expression(arrayIdVar, arrayIndex)))
        }
    }

    private object SetArrayInt : Op {

        override val id = Opcodes.SET_ARRAY_INT

        override fun translate(state: Interpreter.State): Instruction {
            val arrayId = state.intOperand
            val arrayType = state.arrayTypes[arrayId] ?: INT
            val arrayIdVar = Element.Constant(arrayId)
            val value = state.pop(arrayType)
            val arrayIndex = state.pop(INT)
            return Instruction.Assignment(Expression(), Expression.Operation(emptyList(), id, Expression(arrayIdVar, arrayIndex, value)))
        }
    }

    private enum class Assign : Op {

        PUSH_CONSTANT_INT,
        PUSH_CONSTANT_STRING,
        GET_VAR,
        SET_VAR,
        GET_VARBIT,
        SET_VARBIT,
        PUSH_INT_LOCAL,
        POP_INT_LOCAL,
        PUSH_STRING_LOCAL,
        POP_STRING_LOCAL,
        POP_INT_DISCARD,
        POP_STRING_DISCARD,
        GET_VARC_INT,
        SET_VARC_INT,
        GET_VARC_STRING,
        SET_VARC_STRING,
        ;

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Instruction {
            return when (this) {
                PUSH_CONSTANT_INT -> Instruction.Assignment(state.push(INT, state.intOperand), state.operand(INT))
                PUSH_CONSTANT_STRING -> Instruction.Assignment(state.push(STRING, state.stringOperand), state.operand(STRING))
                GET_VAR -> Instruction.Assignment(state.push(INT), Element.Variable.Varp(state.intOperand, INT))
                SET_VAR -> Instruction.Assignment(Element.Variable.Varp(state.intOperand, INT), state.pop(INT))
                GET_VARBIT -> Instruction.Assignment(state.push(INT), Element.Variable.Varbit(state.intOperand, INT))
                SET_VARBIT -> Instruction.Assignment(Element.Variable.Varbit(state.intOperand, INT), state.pop(INT))
                PUSH_INT_LOCAL -> Instruction.Assignment(state.push(INT), Element.Variable.Local(state.intOperand, INT))
                POP_INT_LOCAL -> Instruction.Assignment(Element.Variable.Local(state.intOperand, INT), state.pop(INT))
                PUSH_STRING_LOCAL -> Instruction.Assignment(state.push(STRING), Element.Variable.Local(state.intOperand, STRING))
                POP_STRING_LOCAL -> Instruction.Assignment(Element.Variable.Local(state.intOperand, STRING), state.pop(STRING))
                POP_INT_DISCARD -> Instruction.Assignment(Expression(), state.pop(INT))
                POP_STRING_DISCARD -> Instruction.Assignment(Expression(), state.pop(STRING))
                GET_VARC_INT -> Instruction.Assignment(state.push(INT), Element.Variable.Varc(state.intOperand, INT))
                SET_VARC_INT -> Instruction.Assignment(Element.Variable.Varc(state.intOperand, INT), state.pop(INT))
                GET_VARC_STRING -> Instruction.Assignment(state.push(STRING), Element.Variable.Varc(state.intOperand, STRING))
                SET_VARC_STRING -> Instruction.Assignment(Element.Variable.Varc(state.intOperand, STRING), state.pop(STRING))
            }
        }
    }

    private enum class Basic(
            val args: Array<Type>,
            val defs: Array<Type>,
            val o: Type? = null
    ) : Op {
        GET_VARC_STRING_OLD(arrayOf(), arrayOf(STRING), INT),
        SET_VARC_STRING_OLD(arrayOf(STRING), arrayOf(), INT),
        CC_CREATE(arrayOf(COMPONENT, IFTYPE, INT), arrayOf(), BOOLEAN),
        CC_DELETE(arrayOf(), arrayOf(), BOOLEAN),
        CC_DELETEALL(arrayOf(COMPONENT), arrayOf()),
        CC_FIND(arrayOf(COMPONENT, INT), arrayOf(BIT), BOOLEAN),
        IF_FIND(arrayOf(COMPONENT), arrayOf(BOOLEAN), BOOLEAN),

        CC_SETPOSITION(arrayOf(INT, INT, SETPOSH, SETPOSV), arrayOf(), BOOLEAN),
        CC_SETSIZE(arrayOf(INT, INT, SETSIZE, SETSIZE), arrayOf(), BOOLEAN),
        CC_SETHIDE(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        CC_SETNOCLICKTHROUGH(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        _1006(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),

        CC_SETSCROLLPOS(arrayOf(INT, INT), arrayOf(), BOOLEAN),
        CC_SETCOLOUR(arrayOf(COLOUR), arrayOf(), BOOLEAN),
        CC_SETFILL(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        CC_SETTRANS(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETLINEWID(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETGRAPHIC(arrayOf(GRAPHIC), arrayOf(), BOOLEAN),
        CC_SET2DANGLE(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETTILING(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        CC_SETMODEL(arrayOf(MODEL), arrayOf(), BOOLEAN),
        CC_SETMODELANGLE(arrayOf(INT, INT, INT, INT, INT, INT), arrayOf(), BOOLEAN),
        CC_SETMODELANIM(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETMODELORTHOG(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        CC_SETTEXT(arrayOf(STRING), arrayOf(), BOOLEAN),
        CC_SETTEXTFONT(arrayOf(FONTMETRICS), arrayOf(), BOOLEAN),
        CC_SETTEXTALIGN(arrayOf(SETTEXTALIGNH, SETTEXTALIGNV, INT), arrayOf(), BOOLEAN),
        CC_SETTEXTSHADOW(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        CC_SETOUTLINE(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETGRAPHICSHADOW(arrayOf(COLOUR), arrayOf(), BOOLEAN),
        CC_SETVFLIP(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        CC_SETHFLIP(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        CC_SETSCROLLSIZE(arrayOf(INT, INT), arrayOf(), BOOLEAN),
        CC_RESUME_PAUSEBUTTON(arrayOf(), arrayOf(), BOOLEAN),
        _1122(arrayOf(GRAPHIC), arrayOf(), BOOLEAN),
        CC_SETFILLCOLOUR(arrayOf(COLOUR), arrayOf(), BOOLEAN),
        _1124(arrayOf(INT), arrayOf(), BOOLEAN),
        _1125(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETLINEDIRECTION(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),
        _1127(arrayOf(BOOLEAN), arrayOf(), BOOLEAN),

        CC_SETOBJECT(arrayOf(OBJ, INT), arrayOf(), BOOLEAN),
        CC_SETNPCHEAD(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETPLAYERHEAD_SELF(arrayOf(), arrayOf(), BOOLEAN),
        CC_SETOBJECT_NONUM(arrayOf(OBJ, INT), arrayOf(), BOOLEAN),
        CC_SETOBJECT_ALWAYS_NUM(arrayOf(OBJ, INT), arrayOf(), BOOLEAN),

        CC_SETOP(arrayOf(INT, STRING), arrayOf(), BOOLEAN),
        CC_SETDRAGGABLE(arrayOf(INT, INT), arrayOf(), BOOLEAN),
        CC_SETDRAGGABLEBEHAVIOR(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETDRAGDEADZONE(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETDRAGDEADTIME(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETOPBASE(arrayOf(STRING), arrayOf(), BOOLEAN),
        CC_SETTARGETVERB(arrayOf(STRING), arrayOf(), BOOLEAN),
        CC_CLEAROPS(arrayOf(), arrayOf(), BOOLEAN),
        CC_SETOPKEY(arrayOf(INT, INT, INT, INT, INT, INT, INT, INT, INT, INT, INT), arrayOf(), BOOLEAN),
        CC_SETOPTKEY(arrayOf(INT, INT), arrayOf(), BOOLEAN),
        CC_SETOPKEYRATE(arrayOf(INT, INT, INT), arrayOf(), BOOLEAN),
        CC_SETOPTKEYRATE(arrayOf(INT, INT), arrayOf(), BOOLEAN),
        CC_SETOPKEYIGNOREHELD(arrayOf(INT), arrayOf(), BOOLEAN),
        CC_SETOPTKEYIGNOREHELD(arrayOf(), arrayOf(), BOOLEAN),

        CC_GETX(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETY(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETWIDTH(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETHEIGHT(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETHIDE(arrayOf(), arrayOf(BOOLEAN), BOOLEAN),
        CC_GETLAYER(arrayOf(), arrayOf(COMPONENT), BOOLEAN),

        CC_GETSCROLLX(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETSCROLLY(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETTEXT(arrayOf(), arrayOf(STRING), BOOLEAN),
        CC_GETSCROLLWIDTH(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETSCROLLHEIGHT(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETMODELZOOM(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETMODELANGLE_X(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETMODELANGLE_Z(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETMODELANGLE_Y(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETTRANS(arrayOf(), arrayOf(INT), BOOLEAN),
        _1610(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETCOLOUR(arrayOf(), arrayOf(COLOUR), BOOLEAN),
        CC_GETFILLCOLOUR(arrayOf(), arrayOf(COLOUR), BOOLEAN),
        _1613(arrayOf(), arrayOf(INT), BOOLEAN),
        _1614(arrayOf(), arrayOf(BOOLEAN), BOOLEAN),

        CC_GETINVOBJECT(arrayOf(), arrayOf(OBJ), BOOLEAN),
        CC_GETINVCOUNT(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETID(arrayOf(), arrayOf(INT), BOOLEAN),

        CC_GETTARGETMASK(arrayOf(), arrayOf(INT), BOOLEAN),
        CC_GETOP(arrayOf(INT), arrayOf(STRING), BOOLEAN),
        CC_GETOPBASE(arrayOf(), arrayOf(STRING), BOOLEAN),

        CC_CALLONRESIZE(arrayOf(BOOLEAN), arrayOf()),

        IF_SETPOSITION(arrayOf(INT, INT, SETPOSH, SETPOSV, COMPONENT), arrayOf()),
        IF_SETSIZE(arrayOf(INT, INT, SETSIZE, SETSIZE, COMPONENT), arrayOf()),
        IF_SETHIDE(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        IF_SETNOCLICKTHROUGH(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        _2006(arrayOf(BOOLEAN, COMPONENT), arrayOf()),

        IF_SETSCROLLPOS(arrayOf(INT, INT, COMPONENT), arrayOf()),
        IF_SETCOLOUR(arrayOf(COLOUR, COMPONENT), arrayOf()),
        IF_SETFILL(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        IF_SETTRANS(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETLINEWID(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETGRAPHIC(arrayOf(GRAPHIC, COMPONENT), arrayOf()),
        IF_SET2DANGLE(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETTILING(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        IF_SETMODEL(arrayOf(MODEL, COMPONENT), arrayOf()),
        IF_SETMODELANGLE(arrayOf(INT, INT, INT, INT, INT, INT, COMPONENT), arrayOf()),
        IF_SETMODELANIM(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETMODELORTHOG(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        IF_SETTEXT(arrayOf(STRING, COMPONENT), arrayOf()),
        IF_SETTEXTFONT(arrayOf(FONTMETRICS, COMPONENT), arrayOf()),
        IF_SETTEXTALIGN(arrayOf(SETTEXTALIGNH, SETTEXTALIGNV, INT, COMPONENT), arrayOf()),
        IF_SETTEXTSHADOW(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        IF_SETOUTLINE(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETGRAPHICSHADOW(arrayOf(COLOUR, COMPONENT), arrayOf()),
        IF_SETVFLIP(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        IF_SETHFLIP(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        IF_SETSCROLLSIZE(arrayOf(INT, INT, COMPONENT), arrayOf()),
        IF_RESUME_PAUSEBUTTON(arrayOf(COMPONENT), arrayOf()),
        _2122(arrayOf(GRAPHIC, COMPONENT), arrayOf()),
        IF_SETFILLCOLOUR(arrayOf(COLOUR, COMPONENT), arrayOf()),
        _2124(arrayOf(INT, COMPONENT), arrayOf()),
        _2125(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETLINEDIRECTION(arrayOf(BOOLEAN, COMPONENT), arrayOf()),
        _2127(arrayOf(BOOLEAN, COMPONENT), arrayOf()),

        IF_SETOBJECT(arrayOf(OBJ, INT, COMPONENT), arrayOf()),
        IF_SETNPCHEAD(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETPLAYERHEAD_SELF(arrayOf(COMPONENT), arrayOf()),
        IF_SETOBJECT_NONUM(arrayOf(OBJ, INT, COMPONENT), arrayOf()),
        IF_SETOBJECT_ALWAYS_NUM(arrayOf(OBJ, INT, COMPONENT), arrayOf()),

        IF_SETOP(arrayOf(INT, STRING, COMPONENT), arrayOf()),
        IF_SETDRAGGABLE(arrayOf(INT, INT, COMPONENT), arrayOf()),
        IF_SETDRAGGABLEBEHAVIOR(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETDRAGDEADZONE(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETDRAGDEADTIME(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETOPBASE(arrayOf(STRING, COMPONENT), arrayOf()),
        IF_SETTARGETVERB(arrayOf(STRING, COMPONENT), arrayOf()),
        IF_CLEAROPS(arrayOf(COMPONENT), arrayOf()),
        IF_SETOPKEY(arrayOf(INT, INT, INT, COMPONENT), arrayOf()),
        IF_SETOPTKEY(arrayOf(INT, INT, COMPONENT), arrayOf()),
        IF_SETOPKEYRATE(arrayOf(INT, INT, INT, COMPONENT), arrayOf()),
        IF_SETOPTKEYRATE(arrayOf(INT, INT, COMPONENT), arrayOf()),
        IF_SETOPKEYIGNOREHELD(arrayOf(INT, COMPONENT), arrayOf()),
        IF_SETOPTKEYIGNOREHELD(arrayOf(COMPONENT), arrayOf()),

        IF_GETX(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETY(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETWIDTH(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETHEIGHT(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETHIDE(arrayOf(COMPONENT), arrayOf(BOOLEAN)),
        IF_GETLAYER(arrayOf(COMPONENT), arrayOf(COMPONENT)),

        IF_GETSCROLLX(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETSCROLLY(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETTEXT(arrayOf(COMPONENT), arrayOf(STRING)),
        IF_GETSCROLLWIDTH(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETSCROLLHEIGHT(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETMODELZOOM(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETMODELANGLE_X(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETMODELANGLE_Z(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETMODELANGLE_Y(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETTRANS(arrayOf(COMPONENT), arrayOf(INT)),
        _2610(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETCOLOUR(arrayOf(COMPONENT), arrayOf(COLOUR)),
        IF_GETFILLCOLOUR(arrayOf(COMPONENT), arrayOf(COLOUR)),
        _2613(arrayOf(COMPONENT), arrayOf(INT)),
        _2614(arrayOf(COMPONENT), arrayOf(BOOLEAN)),

        IF_GETINVOBJECT(arrayOf(COMPONENT), arrayOf(OBJ)),
        IF_GETINVCOUNT(arrayOf(COMPONENT), arrayOf(INT)),
        IF_HASSUB(arrayOf(COMPONENT), arrayOf(BOOLEAN)),
        IF_GETTOP(arrayOf(), arrayOf(INT)),

        IF_GETTARGETMASK(arrayOf(COMPONENT), arrayOf(INT)),
        IF_GETOP(arrayOf(INT, COMPONENT), arrayOf(STRING)),
        IF_GETOPBASE(arrayOf(COMPONENT), arrayOf(STRING)),

        IF_CALLONRESIZE(arrayOf(COMPONENT), arrayOf(), BOOLEAN),

        MES(arrayOf(STRING), arrayOf()),
        ANIM(arrayOf(INT, INT), arrayOf()),
        IF_CLOSE(arrayOf(), arrayOf()),
        RESUME_COUNTDIALOG(arrayOf(STRING), arrayOf()),
        RESUME_NAMEDIALOG(arrayOf(STRING), arrayOf()),
        RESUME_STRINGDIALOG(arrayOf(STRING), arrayOf()),
        OPPLAYER(arrayOf(INT, STRING), arrayOf()),
        IF_DRAGPICKUP(arrayOf(COMPONENT, INT, INT), arrayOf()),
        CC_DRAGPICKUP(arrayOf(INT, INT), arrayOf(), BOOLEAN),
        MOUSECAM(arrayOf(BOOLEAN), arrayOf()),
        GETREMOVEROOFS(arrayOf(), arrayOf(BOOLEAN)),
        SETREMOVEROOFS(arrayOf(BOOLEAN), arrayOf()),
        OPENURL(arrayOf(STRING, BOOLEAN), arrayOf()),
        RESUME_OBJDIALOG(arrayOf(INT), arrayOf()),
        BUG_REPORT(arrayOf(INT, STRING, STRING), arrayOf()),
        SETSHIFTCLICKDROP(arrayOf(BOOLEAN), arrayOf()),
        SETSHOWMOUSEOVERTEXT(arrayOf(BOOLEAN), arrayOf()),
        RENDERSELF(arrayOf(BOOLEAN), arrayOf()),
        _3120(arrayOf(BOOLEAN), arrayOf()),
        _3121(arrayOf(BOOLEAN), arrayOf()),
        _3122(arrayOf(BOOLEAN), arrayOf()),
        _3123(arrayOf(BOOLEAN), arrayOf()),
        _3124(arrayOf(), arrayOf()),
        SETSHOWMOUSECROSS(arrayOf(BOOLEAN), arrayOf()),
        SETSHOWLOADINGMESSAGES(arrayOf(BOOLEAN), arrayOf()),
        SETTAPTODROP(arrayOf(BOOLEAN), arrayOf()),
        GETTAPTODROP(arrayOf(), arrayOf(BOOLEAN)),
        _3129(arrayOf(INT, INT), arrayOf()),
        _3130(arrayOf(INT, INT), arrayOf()),
        _3131(arrayOf(INT), arrayOf()),
        GETCANVASSIZE(arrayOf(), arrayOf(INT, INT)),
        _3133(arrayOf(INT), arrayOf()),
        _3134(arrayOf(), arrayOf()),
        _3135(arrayOf(INT, INT), arrayOf()),
        _3136(arrayOf(BOOLEAN), arrayOf()),
        _3137(arrayOf(BOOLEAN), arrayOf()),
        _3138(arrayOf(), arrayOf()),
        _3139(arrayOf(), arrayOf()),
        _3140(arrayOf(), arrayOf(), BOOLEAN),
        SETHIDEUSERNAME(arrayOf(BOOLEAN), arrayOf()),
        GETHIDEUSERNAME(arrayOf(), arrayOf(BOOLEAN)),
        SETREMEMBERUSERNAME(arrayOf(BOOLEAN), arrayOf()),
        GETREMEMBERUSERNAME(arrayOf(), arrayOf(BOOLEAN)),
        _3145(arrayOf(), arrayOf()),

        SOUND_SYNTH(arrayOf(SYNTH, INT, INT), arrayOf()),
        SOUND_SONG(arrayOf(INT), arrayOf()),
        SOUND_JINGLE(arrayOf(INT, INT), arrayOf()),

        CLIENTCLOCK(arrayOf(), arrayOf(INT)),
        INV_GETOBJ(arrayOf(INV, INT), arrayOf(OBJ)),
        INV_GETNUM(arrayOf(INV, INT), arrayOf(INT)),
        INV_TOTAL(arrayOf(INV, OBJ), arrayOf(INT)),
        INV_SIZE(arrayOf(INV), arrayOf(INT)),
        STAT(arrayOf(Type.STAT), arrayOf(INT)),
        STAT_BASE(arrayOf(Type.STAT), arrayOf(INT)),
        STAT_XP(arrayOf(Type.STAT), arrayOf(INT)),
        COORD(arrayOf(), arrayOf(Type.COORD)),
        COORDX(arrayOf(Type.COORD), arrayOf(INT)),
        COORDZ(arrayOf(Type.COORD), arrayOf(INT)),
        COORDY(arrayOf(Type.COORD), arrayOf(INT)),
        MAP_MEMBERS(arrayOf(), arrayOf(BIT)),
        INVOTHER_GETOBJ(arrayOf(INV, INT), arrayOf(OBJ)),
        INVOTHER_GETNUM(arrayOf(INV, INT), arrayOf(INT)),
        INVOTHER_TOTAL(arrayOf(INV, OBJ), arrayOf(INT)),
        STAFFMODLEVEL(arrayOf(), arrayOf(INT)),
        REBOOTTIMER(arrayOf(), arrayOf(INT)),
        MAP_WORLD(arrayOf(), arrayOf(INT)),
        RUNENERGY_VISIBLE(arrayOf(), arrayOf(INT)),
        RUNWEIGHT_VISIBLE(arrayOf(), arrayOf(INT)),
        PLAYERMOD(arrayOf(), arrayOf(BOOLEAN)),
        WORLDFLAGS(arrayOf(), arrayOf(INT)),
        MOVECOORD(arrayOf(Type.COORD, INT, INT, INT), arrayOf(Type.COORD)),

        ENUM_STRING(arrayOf(ENUM, INT), arrayOf(STRING)),
        ENUM_GETOUTPUTCOUNT(arrayOf(ENUM), arrayOf(INT)),

        FRIEND_COUNT(arrayOf(), arrayOf(INT)),
        FRIEND_GETNAME(arrayOf(INT), arrayOf(STRING, STRING)),
        FRIEND_GETWORLD(arrayOf(INT), arrayOf(INT)),
        FRIEND_GETRANK(arrayOf(INT), arrayOf(INT)),
        FRIEND_SETRANK(arrayOf(STRING, INT), arrayOf()),
        FRIEND_ADD(arrayOf(STRING), arrayOf()),
        FRIEND_DEL(arrayOf(STRING), arrayOf()),
        IGNORE_ADD(arrayOf(STRING), arrayOf()),
        IGNORE_DEL(arrayOf(STRING), arrayOf()),
        FRIEND_TEST(arrayOf(STRING), arrayOf(BOOLEAN)),
        CLAN_GETCHATDISPLAYNAME(arrayOf(), arrayOf(STRING)),
        CLAN_GETCHATCOUNT(arrayOf(), arrayOf(INT)),
        CLAN_GETCHATUSERNAME(arrayOf(INT), arrayOf(STRING)),
        CLAN_GETCHATUSERWORLD(arrayOf(INT), arrayOf(INT)),
        CLAN_GETCHATUSERRANK(arrayOf(INT), arrayOf(INT)),
        CLAN_GETCHATMINKICK(arrayOf(), arrayOf(INT)),
        CLAN_KICKUSER(arrayOf(STRING), arrayOf()),
        CLAN_GETCHATRANK(arrayOf(), arrayOf(INT)),
        CLAN_JOINCHAT(arrayOf(STRING), arrayOf()),
        CLAN_LEAVECHAT(arrayOf(), arrayOf()),
        IGNORE_COUNT(arrayOf(), arrayOf(INT)),
        IGNORE_GETNAME(arrayOf(INT), arrayOf(STRING, STRING)),
        IGNORE_TEST(arrayOf(STRING), arrayOf(BOOLEAN)),
        CLAN_ISSELF(arrayOf(INT), arrayOf(BOOLEAN)),
        CLAN_GETCHATOWNERNAME(arrayOf(), arrayOf(STRING)),
        CLAN_ISFRIEND(arrayOf(INT), arrayOf(BOOLEAN)),
        CLAN_ISIGNORE(arrayOf(INT), arrayOf(BOOLEAN)),
        _3628(arrayOf(), arrayOf()),
        _3629(arrayOf(BOOLEAN), arrayOf()),
        _3630(arrayOf(BOOLEAN), arrayOf()),
        _3631(arrayOf(BOOLEAN), arrayOf()),
        _3632(arrayOf(BOOLEAN), arrayOf()),
        _3633(arrayOf(BOOLEAN), arrayOf()),
        _3634(arrayOf(BOOLEAN), arrayOf()),
        _3635(arrayOf(BOOLEAN), arrayOf()),
        _3636(arrayOf(BOOLEAN), arrayOf()),
        _3637(arrayOf(BOOLEAN), arrayOf()),
        _3638(arrayOf(BOOLEAN), arrayOf()),
        _3639(arrayOf(), arrayOf()),
        _3640(arrayOf(), arrayOf()),
        _3641(arrayOf(BOOLEAN), arrayOf()),
        _3642(arrayOf(BOOLEAN), arrayOf()),
        _3643(arrayOf(), arrayOf()),
        _3644(arrayOf(), arrayOf()),
        _3645(arrayOf(BOOLEAN), arrayOf()),
        _3646(arrayOf(BOOLEAN), arrayOf()),
        _3647(arrayOf(BOOLEAN), arrayOf()),
        _3648(arrayOf(BOOLEAN), arrayOf()),
        _3649(arrayOf(BOOLEAN), arrayOf()),
        _3650(arrayOf(BOOLEAN), arrayOf()),
        _3651(arrayOf(BOOLEAN), arrayOf()),
        _3652(arrayOf(BOOLEAN), arrayOf()),
        _3653(arrayOf(BOOLEAN), arrayOf()),
        _3654(arrayOf(BOOLEAN), arrayOf()),
        _3655(arrayOf(), arrayOf()),
        _3656(arrayOf(BOOLEAN), arrayOf()),
        _3657(arrayOf(BOOLEAN), arrayOf()),

        STOCKMARKET_GETOFFERTYPE(arrayOf(INT), arrayOf(INT)),
        STOCKMARKET_GETOFFERITEM(arrayOf(INT), arrayOf(OBJ)),
        STOCKMARKET_GETOFFERPRICE(arrayOf(INT), arrayOf(INT)),
        STOCKMARKET_GETOFFERCOUNT(arrayOf(INT), arrayOf(INT)),
        STOCKMARKET_GETOFFERCOMPLETEDCOUNT(arrayOf(INT), arrayOf(INT)),
        STOCKMARKET_GETOFFERCOMPLETEDGOLD(arrayOf(INT), arrayOf(INT)),
        STOCKMARKET_ISOFFEREMPTY(arrayOf(INT), arrayOf(BOOLEAN)),
        STOCKMARKET_ISOFFERSTABLE(arrayOf(INT), arrayOf(BOOLEAN)),
        STOCKMARKET_ISOFFERFINISHED(arrayOf(INT), arrayOf(BOOLEAN)),
        STOCKMARKET_ISOFFERADDING(arrayOf(INT), arrayOf(BOOLEAN)),

        TRADINGPOST_SORTBY_NAME(arrayOf(BOOLEAN), arrayOf()),
        TRADINGPOST_SORTBY_PRICE(arrayOf(BOOLEAN), arrayOf()),
        TRADINGPOST_SORTFILTERBY_WORLD(arrayOf(BOOLEAN, BOOLEAN), arrayOf()),
        TRADINGPOST_SORTBY_AGE(arrayOf(BOOLEAN), arrayOf()),
        TRADINGPOST_SORTBY_COUNT(arrayOf(BOOLEAN), arrayOf()),
        TRADINGPOST_GETTOTALOFFERS(arrayOf(), arrayOf(INT)),
        TRADINGPOST_GETOFFERWORLD(arrayOf(INT), arrayOf(INT)),
        TRADINGPOST_GETOFFERNAME(arrayOf(INT), arrayOf(STRING)),
        TRADINGPOST_GETOFFERPREVIOUSNAME(arrayOf(INT), arrayOf(STRING)),
        TRADINGPOST_GETOFFERAGE(arrayOf(INT), arrayOf(STRING)),
        TRADINGPOST_GETOFFERCOUNT(arrayOf(INT), arrayOf(INT)),
        TRADINGPOST_GETOFFERPRICE(arrayOf(INT), arrayOf(INT)),
        TRADINGPOST_GETOFFERITEM(arrayOf(INT), arrayOf(INT)),

        ADD(arrayOf(INT, INT), arrayOf(INT)),
        SUB(arrayOf(INT, INT), arrayOf(INT)),
        MULTIPLY(arrayOf(INT, INT), arrayOf(INT)),
        DIV(arrayOf(INT, INT), arrayOf(INT)),
        RANDOM(arrayOf(INT), arrayOf(INT)),
        RANDOMINC(arrayOf(INT), arrayOf(INT)),
        INTERPOLATE(arrayOf(INT, INT, INT, INT, INT), arrayOf(INT)),
        ADDPERCENT(arrayOf(INT, INT), arrayOf(INT)),
        SETBIT(arrayOf(INT, INT), arrayOf(INT)),
        CLEARBIT(arrayOf(INT, INT), arrayOf(INT)),
        TESTBIT(arrayOf(INT, INT), arrayOf(BIT)),
        MOD(arrayOf(INT, INT), arrayOf(INT)),
        POW(arrayOf(INT, INT), arrayOf(INT)),
        INVPOW(arrayOf(INT, INT), arrayOf(INT)),
        AND(arrayOf(INT, INT), arrayOf(INT)),
        OR(arrayOf(INT, INT), arrayOf(INT)),
        SCALE(arrayOf(INT, INT, INT), arrayOf(INT)),
        APPEND_NUM(arrayOf(STRING, INT), arrayOf(STRING)),
        APPEND(arrayOf(STRING, STRING), arrayOf(STRING)),
        APPEND_SIGNNUM(arrayOf(STRING, INT), arrayOf(STRING)),
        LOWERCASE(arrayOf(STRING), arrayOf(STRING)),
        FROMDATE(arrayOf(INT), arrayOf(STRING)),
        TEXT_GENDER(arrayOf(STRING, STRING), arrayOf(STRING)),
        TOSTRING(arrayOf(INT), arrayOf(STRING)),
        COMPARE(arrayOf(STRING, STRING), arrayOf(INT)),
        PARAHEIGHT(arrayOf(STRING, INT, FONTMETRICS), arrayOf(INT)),
        PARAWIDTH(arrayOf(STRING, INT, FONTMETRICS), arrayOf(INT)),
        TEXT_SWITCH(arrayOf(INT, STRING, STRING), arrayOf(STRING)),
        ESCAPE(arrayOf(STRING), arrayOf(STRING)),
        APPEND_CHAR(arrayOf(STRING, CHAR), arrayOf(STRING)),
        CHAR_ISPRINTABLE(arrayOf(CHAR), arrayOf(BOOLEAN)),
        CHAR_ISALPHANUMERIC(arrayOf(CHAR), arrayOf(BOOLEAN)),
        CHAR_ISALPHA(arrayOf(CHAR), arrayOf(BOOLEAN)),
        CHAR_ISNUMERIC(arrayOf(CHAR), arrayOf(BOOLEAN)),
        STRING_LENGTH(arrayOf(STRING), arrayOf(INT)),
        SUBSTRING(arrayOf(STRING, INT, INT), arrayOf(STRING)),
        REMOVETAGS(arrayOf(STRING), arrayOf(STRING)),
        STRING_INDEXOF_CHAR(arrayOf(STRING, CHAR), arrayOf(INT)),
        STRING_INDEXOF_STRING(arrayOf(STRING, STRING, INT), arrayOf(INT)),

        OC_NAME(arrayOf(OBJ), arrayOf(STRING)),
        OC_OP(arrayOf(OBJ, INT), arrayOf(STRING)),
        OC_IOP(arrayOf(OBJ, INT), arrayOf(STRING)),
        OC_COST(arrayOf(OBJ), arrayOf(INT)),
        OC_STACKABLE(arrayOf(OBJ), arrayOf(BOOLEAN)),
        OC_CERT(arrayOf(OBJ), arrayOf(OBJ)),
        OC_UNCERT(arrayOf(OBJ), arrayOf(OBJ)),
        OC_MEMBERS(arrayOf(OBJ), arrayOf(BIT)),
        OC_PLACEHOLDER(arrayOf(OBJ), arrayOf(OBJ)),
        OC_UNPLACEHOLDER(arrayOf(OBJ), arrayOf(OBJ)),
        OC_FIND(arrayOf(STRING, BOOLEAN), arrayOf(INT)),
        OC_FINDNEXT(arrayOf(), arrayOf(OBJ)),
        OC_FINDRESET(arrayOf(), arrayOf()),

        CHAT_GETFILTER_PUBLIC(arrayOf(), arrayOf(INT)),
        CHAT_SETFILTER(arrayOf(INT, INT, INT), arrayOf()),
        CHAT_SENDABUSEREPORT(arrayOf(STRING, INT, INT), arrayOf()),
        CHAT_GETHISTORY_BYTYPEANDLINE(arrayOf(CHATTYPE, INT), arrayOf(INT, INT, STRING, STRING, STRING, INT)),
        CHAT_GETHISTORY_BYUID(arrayOf(INT), arrayOf(CHATTYPE, INT, STRING, STRING, STRING, INT)),
        CHAT_GETFILTER_PRIVATE(arrayOf(), arrayOf(INT)),
        CHAT_SENDPUBLIC(arrayOf(STRING, INT), arrayOf()),
        CHAT_SENDPRIVATE(arrayOf(STRING, STRING), arrayOf()),
        CHAT_PLAYERNAME(arrayOf(), arrayOf(STRING)),
        CHAT_GETFILTER_TRADE(arrayOf(), arrayOf(INT)),
        CHAT_GETHISTORYLENGTH(arrayOf(CHATTYPE), arrayOf(INT)),
        CHAT_GETNEXTUID(arrayOf(INT), arrayOf(INT)),
        CHAT_GETPREVUID(arrayOf(INT), arrayOf(INT)),
        DOCHEAT(arrayOf(STRING), arrayOf()),
        CHAT_SETMESSAGEFILTER(arrayOf(STRING), arrayOf()),
        CHAT_GETMESSAGEFILTER(arrayOf(), arrayOf(STRING)),

        GETWINDOWMODE(arrayOf(), arrayOf(INT)),
        SETWINDOWMODE(arrayOf(INT), arrayOf()),
        GETDEFAULTWINDOWMODE(arrayOf(), arrayOf(INT)),
        SETDEFAULTWINDOWMODE(arrayOf(INT), arrayOf()),

        CAM_FORCEANGLE(arrayOf(INT, INT), arrayOf()),
        CAM_GETANGLE_XA(arrayOf(), arrayOf(INT)),
        CAM_GETANGLE_YA(arrayOf(), arrayOf(INT)),
        CAM_SETFOLLOWHEIGHT(arrayOf(INT), arrayOf()),
        CAM_GETFOLLOWHEIGHT(arrayOf(), arrayOf(INT)),

        LOGOUT(arrayOf(), arrayOf()),

        VIEWPORT_SETFOV(arrayOf(INT, INT), arrayOf()),
        VIEWPORT_SETZOOM(arrayOf(INT, INT), arrayOf()),
        VIEWPORT_CLAMPFOV(arrayOf(INT, INT, INT, INT), arrayOf()),
        VIEWPORT_GETEFFECTIVESIZE(arrayOf(), arrayOf(INT, INT)),
        VIEWPORT_GETZOOM(arrayOf(), arrayOf(INT, INT)),
        VIEWPORT_GETFOV(arrayOf(), arrayOf(INT, INT)),

        WORLDLIST_FETCH(arrayOf(), arrayOf(BOOLEAN)),
        WORLDLIST_START(arrayOf(), arrayOf(INT, INT, STRING, INT, INT, STRING)),
        WORLDLIST_NEXT(arrayOf(), arrayOf(INT, INT, STRING, INT, INT, STRING)),
        WORLDLIST_SPECIFIC(arrayOf(INT), arrayOf(INT, INT, STRING, INT, INT, STRING)),
        WORLDLIST_SORT(arrayOf(INT, BOOLEAN, INT, BOOLEAN), arrayOf()),
        _6511(arrayOf(INT), arrayOf(INT, INT, STRING, INT, INT, STRING)),
        SETFOLLOWEROPSLOWPRIORITY(arrayOf(BOOLEAN), arrayOf()),

        ON_MOBILE(arrayOf(), arrayOf(BOOLEAN)),
        CLIENTTYPE(arrayOf(), arrayOf(INT)),
        _6520(arrayOf(), arrayOf()),
        _6521(arrayOf(), arrayOf()),
        _6522(arrayOf(STRING, INT), arrayOf()),
        _6523(arrayOf(STRING, INT), arrayOf()),
        BATTERYLEVEL(arrayOf(), arrayOf(INT)),
        BATTERYCHARGING(arrayOf(), arrayOf(BOOLEAN)),
        WIFIAVAILABLE(arrayOf(), arrayOf(BOOLEAN)),

        _6600(arrayOf(), arrayOf()),
        WORLDMAP_GETMAPNAME(arrayOf(MAPAREA), arrayOf(STRING)),
        WORLDMAP_SETMAP(arrayOf(MAPAREA), arrayOf()),
        WORLDMAP_GETZOOM(arrayOf(), arrayOf(INT)),
        WORLDMAP_SETZOOM(arrayOf(INT), arrayOf()),
        WORLDMAP_ISLOADED(arrayOf(), arrayOf(BOOLEAN)),
        WORLDMAP_JUMPTODISPLAYCOORD(arrayOf(Type.COORD), arrayOf()),
        WORLDMAP_JUMPTODISPLAYCOORD_INSTANT(arrayOf(Type.COORD), arrayOf()),
        WORLDMAP_JUMPTOSOURCECOORD(arrayOf(Type.COORD), arrayOf()),
        WORLDMAP_JUMPTOSOURCECOORD_INSTANT(arrayOf(Type.COORD), arrayOf()),
        WORLDMAP_GETDISPLAYPOSITION(arrayOf(), arrayOf(INT, INT)),
        WORLDMAP_GETCONFIGORIGIN(arrayOf(MAPAREA), arrayOf(INT)),
        WORLDMAP_GETCONFIGSIZE(arrayOf(MAPAREA), arrayOf(INT, INT)),
        WORLDMAP_GETCONFIGBOUNDS(arrayOf(MAPAREA), arrayOf(INT, INT, INT, INT)),
        WORLDMAP_GETCONFIGZOOM(arrayOf(MAPAREA), arrayOf(INT)),
        _6615(arrayOf(), arrayOf(INT, INT)),
        WORLDMAP_GETCURRENTMAP(arrayOf(), arrayOf(MAPAREA)),
        WORLDMAP_GETDISPLAYCOORD(arrayOf(Type.COORD), arrayOf(INT, INT)),
        _6618(arrayOf(Type.COORD), arrayOf(INT, INT)),
        _6619(arrayOf(INT, Type.COORD), arrayOf()),
        _6620(arrayOf(INT, Type.COORD), arrayOf()),
        WORLDMAP_COORDINMAP(arrayOf(MAPAREA, Type.COORD), arrayOf(BOOLEAN)),
        WORLDMAP_GETSIZE(arrayOf(), arrayOf(INT, INT)),
        _6623(arrayOf(Type.COORD), arrayOf(INT)),
        _6624(arrayOf(INT), arrayOf()),
        _6625(arrayOf(), arrayOf()),
        _6626(arrayOf(INT), arrayOf()),
        _6627(arrayOf(), arrayOf()),
        WORLDMAP_PERPETUALFLASH(arrayOf(INT), arrayOf()),
        WORLDMAP_FLASHELEMENT(arrayOf(INT), arrayOf()),
        WORLDMAP_FLASHELEMENTCATEGORY(arrayOf(CATEGORY), arrayOf()),
        WORLDMAP_STOPCURRENTFLASHES(arrayOf(), arrayOf()),
        WORLDMAP_DISABLEELEMENTS(arrayOf(BOOLEAN), arrayOf()),
        WORLDMAP_DISABLEELEMENT(arrayOf(INT, BOOLEAN), arrayOf()),
        WORLDMAP_DISABLEELEMENTCATEGORY(arrayOf(INT, BOOLEAN), arrayOf()),
        WORLDMAP_GETDISABLEELEMENTS(arrayOf(), arrayOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENT(arrayOf(INT), arrayOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENTCATEGORY(arrayOf(INT), arrayOf(BOOLEAN)),
        _6638(arrayOf(INT, Type.COORD), arrayOf(INT)),
        WORLDMAP_LISTELEMENT_START(arrayOf(), arrayOf(INT, INT)),
        WORLDMAP_LISTELEMENT_NEXT(arrayOf(), arrayOf(INT, INT)),
        MEC_TEXT(arrayOf(INT), arrayOf(STRING)),
        MEC_TEXTSIZE(arrayOf(INT), arrayOf(INT)),
        MEC_CATEGORY(arrayOf(INT), arrayOf(CATEGORY)),
        MEC_SPRITE(arrayOf(INT), arrayOf(INT)),
        _6697(arrayOf(), arrayOf(INT)),
        _6698(arrayOf(), arrayOf(Type.COORD)),
        _6699(arrayOf(), arrayOf(Type.COORD)),
        ;

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Instruction {
            val opArgs = args.indices.reversed().mapTo(ArrayList<Element>()) { state.pop(args[it]) }
            opArgs.reverse()
            if (o != null) opArgs.add(state.operand(o))
            val opDefs = defs.map { state.push(it) }
            return Instruction.Assignment(Expression(opDefs), Expression.Operation(defs.asList(), id, Expression(opArgs)))
        }
    }

    private object JoinString : Op {

        override val id = Opcodes.JOIN_STRING

        override fun translate(state: Interpreter.State): Instruction {
            val args = MutableList<Element>(state.intOperand) { state.pop(STRING) }
            args.reverse()
            return Instruction.Assignment(state.push(STRING), Expression.Operation(listOf(STRING), id, Expression(args)))
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

        override fun translate(state: Interpreter.State): Instruction {
            val args = ArrayList<Element>()
            if (id >= 2000) {
                args.add(state.pop(COMPONENT))
            } else {
                args.add(state.operand(BOOLEAN))
            }
            var s = checkNotNull(state.stack.pop().value as String)
            if (s.isNotEmpty() && s.last() == 'Y') {
                val triggerType = when (id) {
                    Opcodes.IF_SETONSTATTRANSMIT, Opcodes.CC_SETONSTATTRANSMIT -> STAT
                    Opcodes.IF_SETONINVTRANSMIT, Opcodes.CC_SETONINVTRANSMIT -> INV
                    Opcodes.IF_SETONVARTRANSMIT, Opcodes.CC_SETONVARTRANSMIT -> VAR
                    else -> error(this)
                }
                val n = checkNotNull(state.stack.pop().value as Int)
                args.add(Element.Constant(n))
                repeat(n) {
                    args.add(state.pop(triggerType))
                }
                s = s.dropLast(1)
            } else {
                args.add(Element.Constant(0))
            }
            for (i in s.lastIndex downTo 0) {
                args.add(state.pop(Type.of(s[i])))
            }
            args.add(state.pop(INT))
            args.reverse()
            return Instruction.Assignment(Expression(), Expression.Operation(emptyList(), id, Expression(args)))
        }
    }

    enum class ParamKey(val type: Type) : Op {
        NC_PARAM(INT),
        LC_PARAM(LOC),
        OC_PARAM(OBJ),
        STRUCT_PARAM(STRUCT),
        ;

        override val id = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Instruction {
            val paramKeyId = state.stack.peek().value as Int
            val param = state.pop(PARAM)
            val rec = state.pop(type)
            val paramType = checkNotNull(state.interpreter.paramTypeLoader.load(paramKeyId))
            return Instruction.Assignment(state.push(paramType), Expression.Operation(listOf(paramType), id, Expression(rec, param)))
        }
    }
}