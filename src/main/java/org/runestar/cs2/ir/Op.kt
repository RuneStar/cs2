package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type
import org.runestar.cs2.Type.*
import org.runestar.cs2.loadNotNull
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

        override val id = Opcodes.GOSUB_WITH_PARAMS

        override fun translate(state: Interpreter.State): Instruction {
            val invokeId = state.intOperand
            val invoked = state.scripts.loadNotNull(invokeId)
            val args = state.pop(invoked.intArgumentCount + invoked.stringArgumentCount)
            val defs = state.push(invoked.returnTypes)
            return Instruction.Assignment(Expression(defs), Expression.Operation.Invoke(invoked.returnTypes, invokeId, Expression(args)))
        }
    }

    private object Return : Op {

        override val id = Opcodes.RETURN

        override fun translate(state: Interpreter.State): Instruction {
            return Instruction.Return(Expression(state.popAll()))
        }
    }

    private object GetEnum : Op {

        override val id = Opcodes.ENUM

        override fun translate(state: Interpreter.State): Instruction {
            val key = state.pop(INT)
            val enumId = state.pop(ENUM)
            val valueType = Type.of(state.peekValue() as Int)
            val valueTypeVar = state.pop(TYPE)
            val keyType = Type.of(state.peekValue() as Int)
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

        override val id = Opcodes.PUSH_ARRAY_INT

        override fun translate(state: Interpreter.State): Instruction {
            val arrayId = state.intOperand
            val arrayIdVar = Element.Constant(arrayId)
            val arrayIndex = state.pop(INT)
            val arrayType = state.arrayTypes[arrayId] ?: INT
            return Instruction.Assignment(state.push(arrayType), Expression.Operation(listOf(arrayType), id, Expression(arrayIdVar, arrayIndex)))
        }
    }

    private object SetArrayInt : Op {

        override val id = Opcodes.POP_ARRAY_INT

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
        PUSH_VAR,
        POP_VAR,
        PUSH_VARBIT,
        POP_VARBIT,
        PUSH_INT_LOCAL,
        POP_INT_LOCAL,
        PUSH_STRING_LOCAL,
        POP_STRING_LOCAL,
        POP_INT_DISCARD,
        POP_STRING_DISCARD,
        PUSH_VARC_INT,
        POP_VARC_INT,
        PUSH_VARC_STRING,
        POP_VARC_STRING,
        ;

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Instruction {
            return when (this) {
                PUSH_CONSTANT_INT -> Instruction.Assignment(state.push(INT, state.intOperand), state.operand(INT))
                PUSH_CONSTANT_STRING -> Instruction.Assignment(state.push(STRING, state.stringOperand), state.operand(STRING))
                PUSH_VAR -> Instruction.Assignment(state.push(INT), Element.Variable.Varp(state.intOperand, INT))
                POP_VAR -> Instruction.Assignment(Element.Variable.Varp(state.intOperand, INT), state.pop(INT))
                PUSH_VARBIT -> Instruction.Assignment(state.push(INT), Element.Variable.Varbit(state.intOperand, INT))
                POP_VARBIT -> Instruction.Assignment(Element.Variable.Varbit(state.intOperand, INT), state.pop(INT))
                PUSH_INT_LOCAL -> Instruction.Assignment(state.push(INT), Element.Variable.Local(state.intOperand, INT))
                POP_INT_LOCAL -> Instruction.Assignment(Element.Variable.Local(state.intOperand, INT), state.pop(INT))
                PUSH_STRING_LOCAL -> Instruction.Assignment(state.push(STRING), Element.Variable.Local(state.intOperand, STRING))
                POP_STRING_LOCAL -> Instruction.Assignment(Element.Variable.Local(state.intOperand, STRING), state.pop(STRING))
                POP_INT_DISCARD -> Instruction.Assignment(Expression(), state.pop(INT))
                POP_STRING_DISCARD -> Instruction.Assignment(Expression(), state.pop(STRING))
                PUSH_VARC_INT -> Instruction.Assignment(state.push(INT), Element.Variable.Varc(state.intOperand, INT))
                POP_VARC_INT -> Instruction.Assignment(Element.Variable.Varc(state.intOperand, INT), state.pop(INT))
                PUSH_VARC_STRING -> Instruction.Assignment(state.push(STRING), Element.Variable.Varc(state.intOperand, STRING))
                POP_VARC_STRING -> Instruction.Assignment(Element.Variable.Varc(state.intOperand, STRING), state.pop(STRING))
            }
        }
    }

    private enum class Basic(
            val args: List<Type>,
            val defs: List<Type>,
            val o: Type? = null
    ) : Op {
        PUSH_VARC_STRING_OLD(listOf(), listOf(STRING), INT),
        POP_VARC_STRING_OLD(listOf(STRING), listOf(), INT),
        CC_CREATE(listOf(COMPONENT, IFTYPE, INT), listOf(), BOOLEAN),
        CC_DELETE(listOf(), listOf(), BOOLEAN),
        CC_DELETEALL(listOf(COMPONENT), listOf()),
        CC_FIND(listOf(COMPONENT, INT), listOf(BIT), BOOLEAN),
        IF_FIND(listOf(COMPONENT), listOf(BOOLEAN), BOOLEAN),

        CC_SETPOSITION(listOf(INT, INT, SETPOSH, SETPOSV), listOf(), BOOLEAN),
        CC_SETSIZE(listOf(INT, INT, SETSIZE, SETSIZE), listOf(), BOOLEAN),
        CC_SETHIDE(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETNOCLICKTHROUGH(listOf(BOOLEAN), listOf(), BOOLEAN),
        _1006(listOf(BOOLEAN), listOf(), BOOLEAN),

        CC_SETSCROLLPOS(listOf(INT, INT), listOf(), BOOLEAN),
        CC_SETCOLOUR(listOf(COLOUR), listOf(), BOOLEAN),
        CC_SETFILL(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETTRANS(listOf(INT), listOf(), BOOLEAN),
        CC_SETLINEWID(listOf(INT), listOf(), BOOLEAN),
        CC_SETGRAPHIC(listOf(GRAPHIC), listOf(), BOOLEAN),
        CC_SET2DANGLE(listOf(INT), listOf(), BOOLEAN),
        CC_SETTILING(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETMODEL(listOf(MODEL), listOf(), BOOLEAN),
        CC_SETMODELANGLE(listOf(INT, INT, INT, INT, INT, INT), listOf(), BOOLEAN),
        CC_SETMODELANIM(listOf(SEQ), listOf(), BOOLEAN),
        CC_SETMODELORTHOG(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETTEXT(listOf(STRING), listOf(), BOOLEAN),
        CC_SETTEXTFONT(listOf(FONTMETRICS), listOf(), BOOLEAN),
        CC_SETTEXTALIGN(listOf(SETTEXTALIGNH, SETTEXTALIGNV, INT), listOf(), BOOLEAN),
        CC_SETTEXTSHADOW(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETOUTLINE(listOf(INT), listOf(), BOOLEAN),
        CC_SETGRAPHICSHADOW(listOf(COLOUR), listOf(), BOOLEAN),
        CC_SETVFLIP(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETHFLIP(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETSCROLLSIZE(listOf(INT, INT), listOf(), BOOLEAN),
        CC_RESUME_PAUSEBUTTON(listOf(), listOf(), BOOLEAN),
        _1122(listOf(GRAPHIC), listOf(), BOOLEAN),
        CC_SETFILLCOLOUR(listOf(COLOUR), listOf(), BOOLEAN),
        _1124(listOf(INT), listOf(), BOOLEAN),
        _1125(listOf(INT), listOf(), BOOLEAN),
        CC_SETLINEDIRECTION(listOf(BOOLEAN), listOf(), BOOLEAN),
        CC_SETMODELTRANSPARENT(listOf(BOOLEAN), listOf(), BOOLEAN),

        CC_SETOBJECT(listOf(OBJ, INT), listOf(), BOOLEAN),
        CC_SETNPCHEAD(listOf(NPC), listOf(), BOOLEAN),
        CC_SETPLAYERHEAD_SELF(listOf(), listOf(), BOOLEAN),
        CC_SETOBJECT_NONUM(listOf(OBJ, INT), listOf(), BOOLEAN),
        CC_SETOBJECT_ALWAYS_NUM(listOf(OBJ, INT), listOf(), BOOLEAN),

        CC_SETOP(listOf(INT, STRING), listOf(), BOOLEAN),
        CC_SETDRAGGABLE(listOf(INT, INT), listOf(), BOOLEAN),
        CC_SETDRAGGABLEBEHAVIOR(listOf(INT), listOf(), BOOLEAN),
        CC_SETDRAGDEADZONE(listOf(INT), listOf(), BOOLEAN),
        CC_SETDRAGDEADTIME(listOf(INT), listOf(), BOOLEAN),
        CC_SETOPBASE(listOf(STRING), listOf(), BOOLEAN),
        CC_SETTARGETVERB(listOf(STRING), listOf(), BOOLEAN),
        CC_CLEAROPS(listOf(), listOf(), BOOLEAN),
        CC_SETOPKEY(listOf(INT, INT, INT, INT, INT, INT, INT, INT, INT, INT, INT), listOf(), BOOLEAN),
        CC_SETOPTKEY(listOf(INT, INT), listOf(), BOOLEAN),
        CC_SETOPKEYRATE(listOf(INT, INT, INT), listOf(), BOOLEAN),
        CC_SETOPTKEYRATE(listOf(INT, INT), listOf(), BOOLEAN),
        CC_SETOPKEYIGNOREHELD(listOf(INT), listOf(), BOOLEAN),
        CC_SETOPTKEYIGNOREHELD(listOf(), listOf(), BOOLEAN),

        CC_GETX(listOf(), listOf(INT), BOOLEAN),
        CC_GETY(listOf(), listOf(INT), BOOLEAN),
        CC_GETWIDTH(listOf(), listOf(INT), BOOLEAN),
        CC_GETHEIGHT(listOf(), listOf(INT), BOOLEAN),
        CC_GETHIDE(listOf(), listOf(BOOLEAN), BOOLEAN),
        CC_GETLAYER(listOf(), listOf(COMPONENT), BOOLEAN),

        CC_GETSCROLLX(listOf(), listOf(INT), BOOLEAN),
        CC_GETSCROLLY(listOf(), listOf(INT), BOOLEAN),
        CC_GETTEXT(listOf(), listOf(STRING), BOOLEAN),
        CC_GETSCROLLWIDTH(listOf(), listOf(INT), BOOLEAN),
        CC_GETSCROLLHEIGHT(listOf(), listOf(INT), BOOLEAN),
        CC_GETMODELZOOM(listOf(), listOf(INT), BOOLEAN),
        CC_GETMODELANGLE_X(listOf(), listOf(INT), BOOLEAN),
        CC_GETMODELANGLE_Z(listOf(), listOf(INT), BOOLEAN),
        CC_GETMODELANGLE_Y(listOf(), listOf(INT), BOOLEAN),
        CC_GETTRANS(listOf(), listOf(INT), BOOLEAN),
        _1610(listOf(), listOf(INT), BOOLEAN),
        CC_GETCOLOUR(listOf(), listOf(COLOUR), BOOLEAN),
        CC_GETFILLCOLOUR(listOf(), listOf(COLOUR), BOOLEAN),
        _1613(listOf(), listOf(INT), BOOLEAN),
        CC_GETMODELTRANSPARENT(listOf(), listOf(BOOLEAN), BOOLEAN),

        CC_GETINVOBJECT(listOf(), listOf(OBJ), BOOLEAN),
        CC_GETINVCOUNT(listOf(), listOf(INT), BOOLEAN),
        CC_GETID(listOf(), listOf(INT), BOOLEAN),

        CC_GETTARGETMASK(listOf(), listOf(INT), BOOLEAN),
        CC_GETOP(listOf(INT), listOf(STRING), BOOLEAN),
        CC_GETOPBASE(listOf(), listOf(STRING), BOOLEAN),

        CC_CALLONRESIZE(listOf(BOOLEAN), listOf()),

        IF_SETPOSITION(listOf(INT, INT, SETPOSH, SETPOSV, COMPONENT), listOf()),
        IF_SETSIZE(listOf(INT, INT, SETSIZE, SETSIZE, COMPONENT), listOf()),
        IF_SETHIDE(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETNOCLICKTHROUGH(listOf(BOOLEAN, COMPONENT), listOf()),
        _2006(listOf(BOOLEAN, COMPONENT), listOf()),

        IF_SETSCROLLPOS(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETCOLOUR(listOf(COLOUR, COMPONENT), listOf()),
        IF_SETFILL(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETTRANS(listOf(INT, COMPONENT), listOf()),
        IF_SETLINEWID(listOf(INT, COMPONENT), listOf()),
        IF_SETGRAPHIC(listOf(GRAPHIC, COMPONENT), listOf()),
        IF_SET2DANGLE(listOf(INT, COMPONENT), listOf()),
        IF_SETTILING(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETMODEL(listOf(MODEL, COMPONENT), listOf()),
        IF_SETMODELANGLE(listOf(INT, INT, INT, INT, INT, INT, COMPONENT), listOf()),
        IF_SETMODELANIM(listOf(SEQ, COMPONENT), listOf()),
        IF_SETMODELORTHOG(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETTEXT(listOf(STRING, COMPONENT), listOf()),
        IF_SETTEXTFONT(listOf(FONTMETRICS, COMPONENT), listOf()),
        IF_SETTEXTALIGN(listOf(SETTEXTALIGNH, SETTEXTALIGNV, INT, COMPONENT), listOf()),
        IF_SETTEXTSHADOW(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETOUTLINE(listOf(INT, COMPONENT), listOf()),
        IF_SETGRAPHICSHADOW(listOf(COLOUR, COMPONENT), listOf()),
        IF_SETVFLIP(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETHFLIP(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETSCROLLSIZE(listOf(INT, INT, COMPONENT), listOf()),
        IF_RESUME_PAUSEBUTTON(listOf(COMPONENT), listOf()),
        _2122(listOf(GRAPHIC, COMPONENT), listOf()),
        IF_SETFILLCOLOUR(listOf(COLOUR, COMPONENT), listOf()),
        _2124(listOf(INT, COMPONENT), listOf()),
        _2125(listOf(INT, COMPONENT), listOf()),
        IF_SETLINEDIRECTION(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETMODELTRANSPARENT(listOf(BOOLEAN, COMPONENT), listOf()),

        IF_SETOBJECT(listOf(OBJ, INT, COMPONENT), listOf()),
        IF_SETNPCHEAD(listOf(NPC, COMPONENT), listOf()),
        IF_SETPLAYERHEAD_SELF(listOf(COMPONENT), listOf()),
        IF_SETOBJECT_NONUM(listOf(OBJ, INT, COMPONENT), listOf()),
        IF_SETOBJECT_ALWAYS_NUM(listOf(OBJ, INT, COMPONENT), listOf()),

        IF_SETOP(listOf(INT, STRING, COMPONENT), listOf()),
        IF_SETDRAGGABLE(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETDRAGGABLEBEHAVIOR(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADZONE(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADTIME(listOf(INT, COMPONENT), listOf()),
        IF_SETOPBASE(listOf(STRING, COMPONENT), listOf()),
        IF_SETTARGETVERB(listOf(STRING, COMPONENT), listOf()),
        IF_CLEAROPS(listOf(COMPONENT), listOf()),
        IF_SETOPKEY(listOf(INT, INT, INT, COMPONENT), listOf()),
        IF_SETOPTKEY(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETOPKEYRATE(listOf(INT, INT, INT, COMPONENT), listOf()),
        IF_SETOPTKEYRATE(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETOPKEYIGNOREHELD(listOf(INT, COMPONENT), listOf()),
        IF_SETOPTKEYIGNOREHELD(listOf(COMPONENT), listOf()),

        IF_GETX(listOf(COMPONENT), listOf(INT)),
        IF_GETY(listOf(COMPONENT), listOf(INT)),
        IF_GETWIDTH(listOf(COMPONENT), listOf(INT)),
        IF_GETHEIGHT(listOf(COMPONENT), listOf(INT)),
        IF_GETHIDE(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETLAYER(listOf(COMPONENT), listOf(COMPONENT)),

        IF_GETSCROLLX(listOf(COMPONENT), listOf(INT)),
        IF_GETSCROLLY(listOf(COMPONENT), listOf(INT)),
        IF_GETTEXT(listOf(COMPONENT), listOf(STRING)),
        IF_GETSCROLLWIDTH(listOf(COMPONENT), listOf(INT)),
        IF_GETSCROLLHEIGHT(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELZOOM(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_X(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_Z(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_Y(listOf(COMPONENT), listOf(INT)),
        IF_GETTRANS(listOf(COMPONENT), listOf(INT)),
        _2610(listOf(COMPONENT), listOf(INT)),
        IF_GETCOLOUR(listOf(COMPONENT), listOf(COLOUR)),
        IF_GETFILLCOLOUR(listOf(COMPONENT), listOf(COLOUR)),
        _2613(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELTRANSPARENT(listOf(COMPONENT), listOf(BOOLEAN)),

        IF_GETINVOBJECT(listOf(COMPONENT), listOf(OBJ)),
        IF_GETINVCOUNT(listOf(COMPONENT), listOf(INT)),
        IF_HASSUB(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETTOP(listOf(), listOf(INT)),

        IF_GETTARGETMASK(listOf(COMPONENT), listOf(INT)),
        IF_GETOP(listOf(INT, COMPONENT), listOf(STRING)),
        IF_GETOPBASE(listOf(COMPONENT), listOf(STRING)),

        IF_CALLONRESIZE(listOf(COMPONENT), listOf(), BOOLEAN),

        MES(listOf(STRING), listOf()),
        ANIM(listOf(SEQ, INT), listOf()),
        IF_CLOSE(listOf(), listOf()),
        RESUME_COUNTDIALOG(listOf(STRING), listOf()),
        RESUME_NAMEDIALOG(listOf(STRING), listOf()),
        RESUME_STRINGDIALOG(listOf(STRING), listOf()),
        OPPLAYER(listOf(INT, STRING), listOf()),
        IF_DRAGPICKUP(listOf(COMPONENT, INT, INT), listOf()),
        CC_DRAGPICKUP(listOf(INT, INT), listOf(), BOOLEAN),
        MOUSECAM(listOf(BOOLEAN), listOf()),
        GETREMOVEROOFS(listOf(), listOf(BOOLEAN)),
        SETREMOVEROOFS(listOf(BOOLEAN), listOf()),
        OPENURL(listOf(STRING, BOOLEAN), listOf()),
        RESUME_OBJDIALOG(listOf(OBJ), listOf()),
        BUG_REPORT(listOf(INT, STRING, STRING), listOf()),
        SETSHIFTCLICKDROP(listOf(BOOLEAN), listOf()),
        SETSHOWMOUSEOVERTEXT(listOf(BOOLEAN), listOf()),
        RENDERSELF(listOf(BOOLEAN), listOf()),
        _3120(listOf(BOOLEAN), listOf()),
        _3121(listOf(BOOLEAN), listOf()),
        _3122(listOf(BOOLEAN), listOf()),
        _3123(listOf(BOOLEAN), listOf()),
        _3124(listOf(), listOf()),
        SETSHOWMOUSECROSS(listOf(BOOLEAN), listOf()),
        SETSHOWLOADINGMESSAGES(listOf(BOOLEAN), listOf()),
        SETTAPTODROP(listOf(BOOLEAN), listOf()),
        GETTAPTODROP(listOf(), listOf(BOOLEAN)),
        _3129(listOf(INT, INT), listOf()),
        _3130(listOf(INT, INT), listOf()),
        _3131(listOf(INT), listOf()),
        GETCANVASSIZE(listOf(), listOf(INT, INT)),
        MOBILE_SETFPS(listOf(INT), listOf()),
        MOBILE_OPENSTORE(listOf(), listOf()),
        MOBILE_OPENSTORECATEGORY(listOf(INT, BOOLEAN), listOf()),
        _3136(listOf(BOOLEAN), listOf()),
        _3137(listOf(BOOLEAN), listOf()),
        _3138(listOf(), listOf()),
        _3139(listOf(), listOf()),
        _3140(listOf(), listOf(), BOOLEAN),
        SETHIDEUSERNAME(listOf(BOOLEAN), listOf()),
        GETHIDEUSERNAME(listOf(), listOf(BOOLEAN)),
        SETREMEMBERUSERNAME(listOf(BOOLEAN), listOf()),
        GETREMEMBERUSERNAME(listOf(), listOf(BOOLEAN)),
        _3145(listOf(), listOf()),

        SOUND_SYNTH(listOf(SYNTH, INT, INT), listOf()),
        SOUND_SONG(listOf(INT), listOf()),
        SOUND_JINGLE(listOf(INT, INT), listOf()),

        CLIENTCLOCK(listOf(), listOf(INT)),
        INV_GETOBJ(listOf(INV, INT), listOf(OBJ)),
        INV_GETNUM(listOf(INV, INT), listOf(INT)),
        INV_TOTAL(listOf(INV, OBJ), listOf(INT)),
        INV_SIZE(listOf(INV), listOf(INT)),
        STAT(listOf(Type.STAT), listOf(INT)),
        STAT_BASE(listOf(Type.STAT), listOf(INT)),
        STAT_XP(listOf(Type.STAT), listOf(INT)),
        COORD(listOf(), listOf(Type.COORD)),
        COORDX(listOf(Type.COORD), listOf(INT)),
        COORDY(listOf(Type.COORD), listOf(INT)),
        COORDZ(listOf(Type.COORD), listOf(INT)),
        MAP_MEMBERS(listOf(), listOf(BIT)),
        INVOTHER_GETOBJ(listOf(INV, INT), listOf(OBJ)),
        INVOTHER_GETNUM(listOf(INV, INT), listOf(INT)),
        INVOTHER_TOTAL(listOf(INV, OBJ), listOf(INT)),
        STAFFMODLEVEL(listOf(), listOf(INT)),
        REBOOTTIMER(listOf(), listOf(INT)),
        MAP_WORLD(listOf(), listOf(INT)),
        RUNENERGY_VISIBLE(listOf(), listOf(INT)),
        RUNWEIGHT_VISIBLE(listOf(), listOf(INT)),
        PLAYERMOD(listOf(), listOf(BOOLEAN)),
        WORLDFLAGS(listOf(), listOf(INT)),
        MOVECOORD(listOf(Type.COORD, INT, INT, INT), listOf(Type.COORD)),

        ENUM_STRING(listOf(ENUM, INT), listOf(STRING)),
        ENUM_GETOUTPUTCOUNT(listOf(ENUM), listOf(INT)),

        FRIEND_COUNT(listOf(), listOf(INT)),
        FRIEND_GETNAME(listOf(INT), listOf(STRING, STRING)),
        FRIEND_GETWORLD(listOf(INT), listOf(INT)),
        FRIEND_GETRANK(listOf(INT), listOf(INT)),
        FRIEND_SETRANK(listOf(STRING, INT), listOf()),
        FRIEND_ADD(listOf(STRING), listOf()),
        FRIEND_DEL(listOf(STRING), listOf()),
        IGNORE_ADD(listOf(STRING), listOf()),
        IGNORE_DEL(listOf(STRING), listOf()),
        FRIEND_TEST(listOf(STRING), listOf(BOOLEAN)),
        CLAN_GETCHATDISPLAYNAME(listOf(), listOf(STRING)),
        CLAN_GETCHATCOUNT(listOf(), listOf(INT)),
        CLAN_GETCHATUSERNAME(listOf(INT), listOf(STRING)),
        CLAN_GETCHATUSERWORLD(listOf(INT), listOf(INT)),
        CLAN_GETCHATUSERRANK(listOf(INT), listOf(INT)),
        CLAN_GETCHATMINKICK(listOf(), listOf(INT)),
        CLAN_KICKUSER(listOf(STRING), listOf()),
        CLAN_GETCHATRANK(listOf(), listOf(INT)),
        CLAN_JOINCHAT(listOf(STRING), listOf()),
        CLAN_LEAVECHAT(listOf(), listOf()),
        IGNORE_COUNT(listOf(), listOf(INT)),
        IGNORE_GETNAME(listOf(INT), listOf(STRING, STRING)),
        IGNORE_TEST(listOf(STRING), listOf(BOOLEAN)),
        CLAN_ISSELF(listOf(INT), listOf(BOOLEAN)),
        CLAN_GETCHATOWNERNAME(listOf(), listOf(STRING)),
        CLAN_ISFRIEND(listOf(INT), listOf(BOOLEAN)),
        CLAN_ISIGNORE(listOf(INT), listOf(BOOLEAN)),
        _3628(listOf(), listOf()),
        _3629(listOf(BOOLEAN), listOf()),
        _3630(listOf(BOOLEAN), listOf()),
        _3631(listOf(BOOLEAN), listOf()),
        _3632(listOf(BOOLEAN), listOf()),
        _3633(listOf(BOOLEAN), listOf()),
        _3634(listOf(BOOLEAN), listOf()),
        _3635(listOf(BOOLEAN), listOf()),
        _3636(listOf(BOOLEAN), listOf()),
        _3637(listOf(BOOLEAN), listOf()),
        _3638(listOf(BOOLEAN), listOf()),
        _3639(listOf(), listOf()),
        _3640(listOf(), listOf()),
        _3641(listOf(BOOLEAN), listOf()),
        _3642(listOf(BOOLEAN), listOf()),
        _3643(listOf(), listOf()),
        _3644(listOf(), listOf()),
        _3645(listOf(BOOLEAN), listOf()),
        _3646(listOf(BOOLEAN), listOf()),
        _3647(listOf(BOOLEAN), listOf()),
        _3648(listOf(BOOLEAN), listOf()),
        _3649(listOf(BOOLEAN), listOf()),
        _3650(listOf(BOOLEAN), listOf()),
        _3651(listOf(BOOLEAN), listOf()),
        _3652(listOf(BOOLEAN), listOf()),
        _3653(listOf(BOOLEAN), listOf()),
        _3654(listOf(BOOLEAN), listOf()),
        _3655(listOf(), listOf()),
        _3656(listOf(BOOLEAN), listOf()),
        _3657(listOf(BOOLEAN), listOf()),

        STOCKMARKET_GETOFFERTYPE(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERITEM(listOf(INT), listOf(OBJ)),
        STOCKMARKET_GETOFFERPRICE(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERCOUNT(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERCOMPLETEDCOUNT(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERCOMPLETEDGOLD(listOf(INT), listOf(INT)),
        STOCKMARKET_ISOFFEREMPTY(listOf(INT), listOf(BOOLEAN)),
        STOCKMARKET_ISOFFERSTABLE(listOf(INT), listOf(BOOLEAN)),
        STOCKMARKET_ISOFFERFINISHED(listOf(INT), listOf(BOOLEAN)),
        STOCKMARKET_ISOFFERADDING(listOf(INT), listOf(BOOLEAN)),

        TRADINGPOST_SORTBY_NAME(listOf(BOOLEAN), listOf()),
        TRADINGPOST_SORTBY_PRICE(listOf(BOOLEAN), listOf()),
        TRADINGPOST_SORTFILTERBY_WORLD(listOf(BOOLEAN, BOOLEAN), listOf()),
        TRADINGPOST_SORTBY_AGE(listOf(BOOLEAN), listOf()),
        TRADINGPOST_SORTBY_COUNT(listOf(BOOLEAN), listOf()),
        TRADINGPOST_GETTOTALOFFERS(listOf(), listOf(INT)),
        TRADINGPOST_GETOFFERWORLD(listOf(INT), listOf(INT)),
        TRADINGPOST_GETOFFERNAME(listOf(INT), listOf(STRING)),
        TRADINGPOST_GETOFFERPREVIOUSNAME(listOf(INT), listOf(STRING)),
        TRADINGPOST_GETOFFERAGE(listOf(INT), listOf(STRING)),
        TRADINGPOST_GETOFFERCOUNT(listOf(INT), listOf(INT)),
        TRADINGPOST_GETOFFERPRICE(listOf(INT), listOf(INT)),
        TRADINGPOST_GETOFFERITEM(listOf(INT), listOf(INT)),

        ADD(listOf(INT, INT), listOf(INT)),
        SUB(listOf(INT, INT), listOf(INT)),
        MULTIPLY(listOf(INT, INT), listOf(INT)),
        DIV(listOf(INT, INT), listOf(INT)),
        RANDOM(listOf(INT), listOf(INT)),
        RANDOMINC(listOf(INT), listOf(INT)),
        INTERPOLATE(listOf(INT, INT, INT, INT, INT), listOf(INT)),
        ADDPERCENT(listOf(INT, INT), listOf(INT)),
        SETBIT(listOf(INT, INT), listOf(INT)),
        CLEARBIT(listOf(INT, INT), listOf(INT)),
        TESTBIT(listOf(INT, INT), listOf(BIT)),
        MOD(listOf(INT, INT), listOf(INT)),
        POW(listOf(INT, INT), listOf(INT)),
        INVPOW(listOf(INT, INT), listOf(INT)),
        AND(listOf(INT, INT), listOf(INT)),
        OR(listOf(INT, INT), listOf(INT)),
        SCALE(listOf(INT, INT, INT), listOf(INT)),
        APPEND_NUM(listOf(STRING, INT), listOf(STRING)),
        APPEND(listOf(STRING, STRING), listOf(STRING)),
        APPEND_SIGNNUM(listOf(STRING, INT), listOf(STRING)),
        LOWERCASE(listOf(STRING), listOf(STRING)),
        FROMDATE(listOf(INT), listOf(STRING)),
        TEXT_GENDER(listOf(STRING, STRING), listOf(STRING)),
        TOSTRING(listOf(INT), listOf(STRING)),
        COMPARE(listOf(STRING, STRING), listOf(INT)),
        PARAHEIGHT(listOf(STRING, INT, FONTMETRICS), listOf(INT)),
        PARAWIDTH(listOf(STRING, INT, FONTMETRICS), listOf(INT)),
        TEXT_SWITCH(listOf(INT, STRING, STRING), listOf(STRING)),
        ESCAPE(listOf(STRING), listOf(STRING)),
        APPEND_CHAR(listOf(STRING, CHAR), listOf(STRING)),
        CHAR_ISPRINTABLE(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHANUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHA(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISNUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        STRING_LENGTH(listOf(STRING), listOf(INT)),
        SUBSTRING(listOf(STRING, INT, INT), listOf(STRING)),
        REMOVETAGS(listOf(STRING), listOf(STRING)),
        STRING_INDEXOF_CHAR(listOf(STRING, CHAR), listOf(INT)),
        STRING_INDEXOF_STRING(listOf(STRING, STRING, INT), listOf(INT)),

        OC_NAME(listOf(OBJ), listOf(STRING)),
        OC_OP(listOf(OBJ, INT), listOf(STRING)),
        OC_IOP(listOf(OBJ, INT), listOf(STRING)),
        OC_COST(listOf(OBJ), listOf(INT)),
        OC_STACKABLE(listOf(OBJ), listOf(BOOLEAN)),
        OC_CERT(listOf(OBJ), listOf(OBJ)),
        OC_UNCERT(listOf(OBJ), listOf(OBJ)),
        OC_MEMBERS(listOf(OBJ), listOf(BIT)),
        OC_PLACEHOLDER(listOf(OBJ), listOf(OBJ)),
        OC_UNPLACEHOLDER(listOf(OBJ), listOf(OBJ)),
        OC_FIND(listOf(STRING, BOOLEAN), listOf(INT)),
        OC_FINDNEXT(listOf(), listOf(OBJ)),
        OC_FINDRESET(listOf(), listOf()),

        CHAT_GETFILTER_PUBLIC(listOf(), listOf(INT)),
        CHAT_SETFILTER(listOf(INT, INT, INT), listOf()),
        CHAT_SENDABUSEREPORT(listOf(STRING, INT, INT), listOf()),
        CHAT_GETHISTORY_BYTYPEANDLINE(listOf(CHATTYPE, INT), listOf(INT, INT, STRING, STRING, STRING, INT)),
        CHAT_GETHISTORY_BYUID(listOf(INT), listOf(CHATTYPE, INT, STRING, STRING, STRING, INT)),
        CHAT_GETFILTER_PRIVATE(listOf(), listOf(INT)),
        CHAT_SENDPUBLIC(listOf(STRING, INT), listOf()),
        CHAT_SENDPRIVATE(listOf(STRING, STRING), listOf()),
        CHAT_PLAYERNAME(listOf(), listOf(STRING)),
        CHAT_GETFILTER_TRADE(listOf(), listOf(INT)),
        CHAT_GETHISTORYLENGTH(listOf(CHATTYPE), listOf(INT)),
        CHAT_GETNEXTUID(listOf(INT), listOf(INT)),
        CHAT_GETPREVUID(listOf(INT), listOf(INT)),
        DOCHEAT(listOf(STRING), listOf()),
        CHAT_SETMESSAGEFILTER(listOf(STRING), listOf()),
        CHAT_GETMESSAGEFILTER(listOf(), listOf(STRING)),

        GETWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETWINDOWMODE(listOf(WINDOWMODE), listOf()),
        GETDEFAULTWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETDEFAULTWINDOWMODE(listOf(WINDOWMODE), listOf()),

        CAM_FORCEANGLE(listOf(INT, INT), listOf()),
        CAM_GETANGLE_XA(listOf(), listOf(INT)),
        CAM_GETANGLE_YA(listOf(), listOf(INT)),
        CAM_SETFOLLOWHEIGHT(listOf(INT), listOf()),
        CAM_GETFOLLOWHEIGHT(listOf(), listOf(INT)),

        LOGOUT(listOf(), listOf()),

        VIEWPORT_SETFOV(listOf(INT, INT), listOf()),
        VIEWPORT_SETZOOM(listOf(INT, INT), listOf()),
        VIEWPORT_CLAMPFOV(listOf(INT, INT, INT, INT), listOf()),
        VIEWPORT_GETEFFECTIVESIZE(listOf(), listOf(INT, INT)),
        VIEWPORT_GETZOOM(listOf(), listOf(INT, INT)),
        VIEWPORT_GETFOV(listOf(), listOf(INT, INT)),

        WORLDLIST_FETCH(listOf(), listOf(BOOLEAN)),
        WORLDLIST_START(listOf(), listOf(INT, INT, STRING, INT, INT, STRING)),
        WORLDLIST_NEXT(listOf(), listOf(INT, INT, STRING, INT, INT, STRING)),
        WORLDLIST_SPECIFIC(listOf(INT), listOf(INT, INT, STRING, INT, INT, STRING)),
        WORLDLIST_SORT(listOf(INT, BOOLEAN, INT, BOOLEAN), listOf()),
        _6511(listOf(INT), listOf(INT, INT, STRING, INT, INT, STRING)),
        SETFOLLOWEROPSLOWPRIORITY(listOf(BOOLEAN), listOf()),

        ON_MOBILE(listOf(), listOf(BOOLEAN)),
        CLIENTTYPE(listOf(), listOf(INT)),
        _6520(listOf(), listOf()),
        MOBILE_KEYBOARDHIDE(listOf(), listOf()),
        _6522(listOf(STRING, INT), listOf()),
        _6523(listOf(STRING, INT), listOf()),
        MOBILE_BATTERYLEVEL(listOf(), listOf(INT)),
        MOBILE_BATTERYCHARGING(listOf(), listOf(BOOLEAN)),
        MOBILE_WIFIAVAILABLE(listOf(), listOf(BOOLEAN)),

        _6600(listOf(), listOf()),
        WORLDMAP_GETMAPNAME(listOf(MAPAREA), listOf(STRING)),
        WORLDMAP_SETMAP(listOf(MAPAREA), listOf()),
        WORLDMAP_GETZOOM(listOf(), listOf(INT)),
        WORLDMAP_SETZOOM(listOf(INT), listOf()),
        WORLDMAP_ISLOADED(listOf(), listOf(BOOLEAN)),
        WORLDMAP_JUMPTODISPLAYCOORD(listOf(Type.COORD), listOf()),
        WORLDMAP_JUMPTODISPLAYCOORD_INSTANT(listOf(Type.COORD), listOf()),
        WORLDMAP_JUMPTOSOURCECOORD(listOf(Type.COORD), listOf()),
        WORLDMAP_JUMPTOSOURCECOORD_INSTANT(listOf(Type.COORD), listOf()),
        WORLDMAP_GETDISPLAYPOSITION(listOf(), listOf(INT, INT)),
        WORLDMAP_GETCONFIGORIGIN(listOf(MAPAREA), listOf(INT)),
        WORLDMAP_GETCONFIGSIZE(listOf(MAPAREA), listOf(INT, INT)),
        WORLDMAP_GETCONFIGBOUNDS(listOf(MAPAREA), listOf(INT, INT, INT, INT)),
        WORLDMAP_GETCONFIGZOOM(listOf(MAPAREA), listOf(INT)),
        _6615(listOf(), listOf(INT, INT)),
        WORLDMAP_GETCURRENTMAP(listOf(), listOf(MAPAREA)),
        WORLDMAP_GETDISPLAYCOORD(listOf(Type.COORD), listOf(INT, INT)),
        _6618(listOf(Type.COORD), listOf(INT, INT)),
        _6619(listOf(INT, Type.COORD), listOf()),
        _6620(listOf(INT, Type.COORD), listOf()),
        WORLDMAP_COORDINMAP(listOf(MAPAREA, Type.COORD), listOf(BOOLEAN)),
        WORLDMAP_GETSIZE(listOf(), listOf(INT, INT)),
        _6623(listOf(Type.COORD), listOf(INT)),
        _6624(listOf(INT), listOf()),
        _6625(listOf(), listOf()),
        _6626(listOf(INT), listOf()),
        _6627(listOf(), listOf()),
        WORLDMAP_PERPETUALFLASH(listOf(BOOLEAN), listOf()),
        WORLDMAP_FLASHELEMENT(listOf(INT), listOf()),
        WORLDMAP_FLASHELEMENTCATEGORY(listOf(CATEGORY), listOf()),
        WORLDMAP_STOPCURRENTFLASHES(listOf(), listOf()),
        WORLDMAP_DISABLEELEMENTS(listOf(BOOLEAN), listOf()),
        WORLDMAP_DISABLEELEMENT(listOf(INT, BOOLEAN), listOf()),
        WORLDMAP_DISABLEELEMENTCATEGORY(listOf(INT, BOOLEAN), listOf()),
        WORLDMAP_GETDISABLEELEMENTS(listOf(), listOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENT(listOf(INT), listOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENTCATEGORY(listOf(INT), listOf(BOOLEAN)),
        _6638(listOf(INT, Type.COORD), listOf(INT)),
        WORLDMAP_LISTELEMENT_START(listOf(), listOf(MAPELEMENT, Type.COORD)),
        WORLDMAP_LISTELEMENT_NEXT(listOf(), listOf(MAPELEMENT, Type.COORD)),
        MEC_TEXT(listOf(MAPELEMENT), listOf(STRING)),
        MEC_TEXTSIZE(listOf(MAPELEMENT), listOf(INT)),
        MEC_CATEGORY(listOf(MAPELEMENT), listOf(CATEGORY)),
        MEC_SPRITE(listOf(MAPELEMENT), listOf(INT)),
        WORLDMAP_ELEMENT(listOf(), listOf(MAPELEMENT)),
        _6698(listOf(), listOf(Type.COORD)),
        WORLDMAP_ELEMENTCOORD(listOf(), listOf(Type.COORD)),
        ;

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Instruction {
            var opArgs: List<Element> = state.pop(args)
            if (o != null) opArgs = opArgs.plus(state.operand(o))
            val opDefs = state.push(defs)
            return Instruction.Assignment(Expression(opDefs), Expression.Operation(defs, id, Expression(opArgs)))
        }
    }

    private object JoinString : Op {

        override val id = Opcodes.JOIN_STRING

        override fun translate(state: Interpreter.State): Instruction {
            val args = state.pop(state.intOperand)
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
            var s = state.popValue() as String
            if (s.isNotEmpty() && s.last() == 'Y') {
                val triggerType = when (id) {
                    Opcodes.IF_SETONSTATTRANSMIT, Opcodes.CC_SETONSTATTRANSMIT -> STAT
                    Opcodes.IF_SETONINVTRANSMIT, Opcodes.CC_SETONINVTRANSMIT -> INV
                    Opcodes.IF_SETONVARTRANSMIT, Opcodes.CC_SETONVARTRANSMIT -> VAR
                    else -> error(this)
                }
                val n = state.popValue() as Int
                args.add(Element.Constant(n))
                repeat(n) {
                    args.add(state.pop(triggerType))
                }
                s = s.dropLast(1)
            } else {
                args.add(Element.Constant(0))
            }
            for (i in s.lastIndex downTo 0) {
                val ep = state.peekValue()?.let { EventProperty.of(it) }
                val pop = state.pop(Type.of(s[i]))
                args.add(ep ?: pop)
            }
            val scriptId = state.popValue() as Int
            args.reverse()
            return Instruction.Assignment(Expression(), Expression.Operation.AddHook(id, scriptId, Expression(args)))
        }
    }

    enum class ParamKey(val type: Type) : Op {
        NC_PARAM(NPC),
        LC_PARAM(LOC),
        OC_PARAM(OBJ),
        STRUCT_PARAM(STRUCT),
        ;

        override val id = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Instruction {
            val paramKeyId = state.peekValue() as Int
            val param = state.pop(PARAM)
            val rec = state.pop(type)
            val paramType = state.paramTypes.loadNotNull(paramKeyId)
            return Instruction.Assignment(state.push(paramType), Expression.Operation(listOf(paramType), id, Expression(rec, param)))
        }
    }
}