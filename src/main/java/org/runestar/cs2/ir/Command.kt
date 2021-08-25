package org.runestar.cs2.ir

import org.runestar.cs2.bin.ClientScriptDesc
import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Type
import org.runestar.cs2.bin.*
import org.runestar.cs2.ir.COORD as _COORD
import org.runestar.cs2.ir.CLIENTTYPE as _CLIENTTYPE
import org.runestar.cs2.ir.ENUM as _ENUM
import org.runestar.cs2.ir.MES as _MES
import org.runestar.cs2.ir.STAT as _STAT
import org.runestar.cs2.bin.string
import org.runestar.cs2.util.Loader
import org.runestar.cs2.util.loadNotNull

interface Command {

    val id: Int

    fun translate(state: InterpreterState): Instruction

    companion object {

        val COMMANDS: List<Command> get() = ArrayList<Command>().apply {
            add(Switch)
            add(Branch)
            add(Enum)
            add(Proc)
            add(Return)
            add(JoinString)
            add(DefineArray)
            add(PushArrayInt)
            add(PopArrayInt)
            addAll(BranchCompare.values().asList())
            addAll(Discard.values().asList())
            addAll(Assign.values().asList())
            addAll(Basic.values().asList())
            addAll(ClientScript.values().asList())
            addAll(Param.values().asList())
        }

        fun loader(commands: Iterable<Command>): Loader<Command> = Loader(commands.associateBy { it.id })

        val LOADER: Loader<Command> = loader(COMMANDS)
    }

    object Switch : Command {

        override val id = SWITCH

        override fun translate(state: InterpreterState): Instruction {
            return Instruction.Switch(state.pop(StackType.INT), state.switch.mapValues { Instruction.Label(it.value + 1 + state.pc) })
        }
    }

    object Branch : Command {

        override val id = BRANCH

        override fun translate(state: InterpreterState): Instruction {
            return Instruction.Goto(Instruction.Label(state.pc + state.operand.int + 1))
        }
    }

    object Proc : Command {

        override val id = GOSUB_WITH_PARAMS

        override fun translate(state: InterpreterState): Instruction {
            val invokeId = state.operand.int
            state.callGraph.call(state.scriptId, invokeId, Trigger.proc)
            val invoked = state.scripts.loadNotNull(invokeId)
            val args = Expression(state.pop(invoked.intArgumentCount + invoked.stringArgumentCount))
            assign(state.typings.of(args), state.typings.args(invokeId, args.stackTypes))
            val proc = Expression.Proc(invoked.returnTypes, invokeId, args)
            val defs = Expression(state.push(invoked.returnTypes))
            assign(state.typings.of(proc), state.typings.of(defs))
            return Instruction.Assignment(defs, proc)
        }
    }

    object Return : Command {

        override val id = RETURN

        override fun translate(state: InterpreterState): Instruction {
            val e = Expression(state.popAll())
            assign(state.typings.of(e), state.typings.returns(state.scriptId, e.stackTypes))
            return Instruction.Return(e)
        }
    }

    object Enum : Command {

        override val id = ENUM

        override fun translate(state: InterpreterState): Instruction {
            val key = state.pop(StackType.INT)
            val enumId = state.pop(StackType.INT)
            assign(state.typings.of(enumId), state.typings.of(_ENUM))
            val valueType = Type.of((checkNotNull(state.peekValue()).int).toByte())
            val valueTypeVar = state.pop(StackType.INT)
            assign(state.typings.of(valueTypeVar), state.typings.of(TYPE))
            val keyType = Type.of((checkNotNull(state.peekValue()).int).toByte())
            assign(state.typings.of(key), state.typings.of(Prototype(keyType)))
            val keyTypeVar = state.pop(StackType.INT)
            assign(state.typings.of(keyTypeVar), state.typings.of(TYPE))
            val args = Expression(keyTypeVar, valueTypeVar, enumId, key)
            val value = state.push(valueType.stackType)
            val operation = Expression.Operation(listOf(valueType.stackType), id, args)
            val operationTyping = state.typings.of(operation).single()
            operationTyping.freeze(valueType)
            assign(operationTyping, state.typings.of(value))
            return Instruction.Assignment(value, operation)
        }
    }

    enum class BranchCompare : Command {

        BRANCH_NOT,
        BRANCH_EQUALS,
        BRANCH_LESS_THAN,
        BRANCH_GREATER_THAN,
        BRANCH_LESS_THAN_OR_EQUALS,
        BRANCH_GREATER_THAN_OR_EQUALS;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            val right = state.pop(StackType.INT)
            val left = state.pop(StackType.INT)
            compare(state.typings.of(left), state.typings.of(right))
            val expr = Expression.Operation(emptyList(), id, Expression(left, right))
            return Instruction.Branch(expr, Instruction.Label(state.pc + state.operand.int + 1))
        }
    }

    object DefineArray : Command {

        override val id = DEFINE_ARRAY

        override fun translate(state: InterpreterState): Instruction {
            val length = state.pop(StackType.INT)
            val array = Element.Access(Variable.array(state.scriptId, state.operand.int shr 16))
            val arrayType = Type.of(state.operand.int.toByte())
            assign(state.typings.of(length), state.typings.of(LENGTH))
            state.typings.of(array).freeze(arrayType)
            return Instruction.Assignment(Expression.Operation(emptyList(), id, Expression(array, length)))
        }
    }

    object PushArrayInt : Command {

        override val id = PUSH_ARRAY_INT

        override fun translate(state: InterpreterState): Instruction {
            val arrayIndex = state.pop(StackType.INT)
            val array = Element.Access(Variable.array(state.scriptId, state.operand.int))
            val operation = Expression.Operation(listOf(StackType.INT), id, Expression(array, arrayIndex))
            val def = state.push(StackType.INT)
            assign(state.typings.of(arrayIndex), state.typings.of(INDEX))
            assign(state.typings.of(array), state.typings.of(operation).single())
            assign(state.typings.of(operation).single(), state.typings.of(def))
            return Instruction.Assignment(def, operation)
        }
    }

    object PopArrayInt : Command {

        override val id = POP_ARRAY_INT

        override fun translate(state: InterpreterState): Instruction {
            val value = state.pop(StackType.INT)
            val arrayIndex = state.pop(StackType.INT)
            val array = Element.Access(Variable.array(state.scriptId, state.operand.int))
            assign(state.typings.of(arrayIndex), state.typings.of(INDEX))
            assign(state.typings.of(value), state.typings.of(array))
            return Instruction.Assignment(Expression.Operation(emptyList(), id, Expression(array, arrayIndex, value)))
        }
    }

    enum class Discard(val stackType: StackType) : Command {

        POP_INT_DISCARD(StackType.INT),
        POP_STRING_DISCARD(StackType.STRING),
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            return Instruction.Assignment(state.pop(stackType))
        }
    }

    enum class Assign : Command {

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
        PUSH_VARC_INT,
        POP_VARC_INT,
        PUSH_VARC_STRING,
        POP_VARC_STRING,
        PUSH_VARCLANSETTING,
        PUSH_VARCLAN,
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            val a = when (this) {
                PUSH_CONSTANT_INT -> Instruction.Assignment(state.push(StackType.INT, state.operand), Element.Constant(state.operand))
                PUSH_CONSTANT_STRING -> Instruction.Assignment(state.push(StackType.STRING, state.operand), Element.Constant(state.operand))
                PUSH_VAR -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varp(state.operand.int)))
                POP_VAR -> Instruction.Assignment(Element.Access(Variable.varp(state.operand.int)), state.pop(StackType.INT))
                PUSH_VARBIT -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varbit(state.operand.int)))
                POP_VARBIT -> Instruction.Assignment(Element.Access(Variable.varbit(state.operand.int)), state.pop(StackType.INT))
                PUSH_INT_LOCAL -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.int(state.scriptId, state.operand.int)))
                POP_INT_LOCAL -> Instruction.Assignment(Element.Access(Variable.int(state.scriptId, state.operand.int)), state.pop(StackType.INT))
                PUSH_STRING_LOCAL -> Instruction.Assignment(state.push(StackType.STRING), Element.Access(Variable.string(state.scriptId, state.operand.int)))
                POP_STRING_LOCAL -> Instruction.Assignment(Element.Access(Variable.string(state.scriptId, state.operand.int)), state.pop(StackType.STRING))
                PUSH_VARC_INT -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varcint(state.operand.int)))
                POP_VARC_INT -> Instruction.Assignment(Element.Access(Variable.varcint(state.operand.int)), state.pop(StackType.INT))
                PUSH_VARC_STRING -> Instruction.Assignment(state.push(StackType.STRING), Element.Access(Variable.varcstring(state.operand.int)))
                POP_VARC_STRING -> Instruction.Assignment(Element.Access(Variable.varcstring(state.operand.int)), state.pop(StackType.STRING))
                PUSH_VARCLANSETTING -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varclansetting(state.operand.int)))
                PUSH_VARCLAN -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varclan(state.operand.int)))
            }
            assign(state.typings.of(a.expression), state.typings.of(a.definitions))
            return a
        }
    }

    enum class Basic(
            val args: List<Prototype>,
            val defs: List<Prototype>,
            val o: Boolean = false,
    ) : Command {
        CC_CREATE(listOf(COMPONENT, IFTYPE, COMSUBID), listOf(), true),
        CC_DELETE(listOf(), listOf(), true),
        _103(listOf(NEWVAR, INT, INT), listOf()),
        _104(listOf(NEWVAR), listOf()),
        CC_DELETEALL(listOf(COMPONENT), listOf()),
        CC_FIND(listOf(COMPONENT, COMSUBID), listOf(BOOL), true),
        IF_FIND(listOf(COMPONENT), listOf(BOOLEAN), true),
        _202(listOf(NEWVAR), listOf(INT)),
        _203(listOf(NEWVAR), listOf(INT)),

        CC_SETPOSITION(listOf(X, Y, SETPOSH, SETPOSV), listOf(), true),
        CC_SETSIZE(listOf(WIDTH, HEIGHT, SETSIZE, SETSIZE), listOf(), true),
        CC_SETHIDE(listOf(BOOLEAN), listOf(), true),
        CC_SETNOCLICKTHROUGH(listOf(BOOLEAN), listOf(), true),
        CC_SETNOSCROLLTHROUGH(listOf(BOOLEAN), listOf(), true),

        CC_SETSCROLLPOS(listOf(X, Y), listOf(), true),
        CC_SETCOLOUR(listOf(COLOUR), listOf(), true),
        CC_SETFILL(listOf(BOOLEAN), listOf(), true),
        CC_SETTRANS(listOf(TRANS), listOf(), true),
        CC_SETLINEWID(listOf(INT), listOf(), true),
        CC_SETGRAPHIC(listOf(GRAPHIC), listOf(), true),
        CC_SET2DANGLE(listOf(ANGLE), listOf(), true),
        CC_SETTILING(listOf(BOOLEAN), listOf(), true),
        CC_SETMODEL(listOf(MODEL), listOf(), true),
        CC_SETMODELANGLE(listOf(INT, INT, INT, INT, INT, INT), listOf(), true),
        CC_SETMODELANIM(listOf(SEQ), listOf(), true),
        CC_SETMODELORTHOG(listOf(BOOLEAN), listOf(), true),
        CC_SETTEXT(listOf(TEXT), listOf(), true),
        CC_SETTEXTFONT(listOf(FONTMETRICS), listOf(), true),
        CC_SETTEXTALIGN(listOf(SETTEXTALIGNH, SETTEXTALIGNV, INT), listOf(), true),
        CC_SETTEXTSHADOW(listOf(BOOLEAN), listOf(), true),
        CC_SETOUTLINE(listOf(INT), listOf(), true),
        CC_SETGRAPHICSHADOW(listOf(COLOUR), listOf(), true),
        CC_SETVFLIP(listOf(BOOLEAN), listOf(), true),
        CC_SETHFLIP(listOf(BOOLEAN), listOf(), true),
        CC_SETSCROLLSIZE(listOf(WIDTH, HEIGHT), listOf(), true),
        CC_RESUME_PAUSEBUTTON(listOf(), listOf(), true),
        _1122(listOf(GRAPHIC), listOf(), true),
        CC_SETFILLCOLOUR(listOf(COLOUR), listOf(), true),
        _1124(listOf(INT), listOf(), true),
        _1125(listOf(INT), listOf(), true),
        CC_SETLINEDIRECTION(listOf(BOOLEAN), listOf(), true),
        CC_SETMODELTRANSPARENT(listOf(BOOLEAN), listOf(), true),
        _1128(listOf(INT, INT), listOf()),

        CC_SETOBJECT(listOf(OBJ, NUM), listOf(), true),
        CC_SETNPCHEAD(listOf(NPC), listOf(), true),
        CC_SETPLAYERHEAD_SELF(listOf(), listOf(), true),
        CC_SETOBJECT_NONUM(listOf(OBJ, NUM), listOf(), true),
        CC_SETOBJECT_ALWAYS_NUM(listOf(OBJ, NUM), listOf(), true),

        CC_SETOP(listOf(OPINDEX, OP), listOf(), true),
        CC_SETDRAGGABLE(listOf(COMPONENT, INT), listOf(), true),
        CC_SETDRAGGABLEBEHAVIOR(listOf(INT), listOf(), true),
        CC_SETDRAGDEADZONE(listOf(INT), listOf(), true),
        CC_SETDRAGDEADTIME(listOf(INT), listOf(), true),
        CC_SETOPBASE(listOf(OPBASE), listOf(), true),
        CC_SETTARGETVERB(listOf(STRING), listOf(), true),
        CC_CLEAROPS(listOf(), listOf(), true),
        _1308(listOf(BOOLEAN), listOf(), true),
        _1309(listOf(INT), listOf(), true),
        CC_SETOPKEY(listOf(INT, INT, INT, INT, INT, INT, INT, INT, INT, INT, INT), listOf(), true),
        CC_SETOPTKEY(listOf(INT, INT), listOf(), true),
        CC_SETOPKEYRATE(listOf(INT, INT, INT), listOf(), true),
        CC_SETOPTKEYRATE(listOf(INT, INT), listOf(), true),
        CC_SETOPKEYIGNOREHELD(listOf(INT), listOf(), true),
        CC_SETOPTKEYIGNOREHELD(listOf(), listOf(), true),

        CC_GETX(listOf(), listOf(X), true),
        CC_GETY(listOf(), listOf(Y), true),
        CC_GETWIDTH(listOf(), listOf(WIDTH), true),
        CC_GETHEIGHT(listOf(), listOf(HEIGHT), true),
        CC_GETHIDE(listOf(), listOf(BOOLEAN), true),
        CC_GETLAYER(listOf(), listOf(LAYER), true),

        CC_GETSCROLLX(listOf(), listOf(X), true),
        CC_GETSCROLLY(listOf(), listOf(Y), true),
        CC_GETTEXT(listOf(), listOf(TEXT), true),
        CC_GETSCROLLWIDTH(listOf(), listOf(WIDTH), true),
        CC_GETSCROLLHEIGHT(listOf(), listOf(HEIGHT), true),
        CC_GETMODELZOOM(listOf(), listOf(INT), true),
        CC_GETMODELANGLE_X(listOf(), listOf(INT), true),
        CC_GETMODELANGLE_Z(listOf(), listOf(INT), true),
        CC_GETMODELANGLE_Y(listOf(), listOf(INT), true),
        CC_GETTRANS(listOf(), listOf(TRANS), true),
        _1610(listOf(), listOf(INT), true),
        CC_GETCOLOUR(listOf(), listOf(COLOUR), true),
        CC_GETFILLCOLOUR(listOf(), listOf(COLOUR), true),
        _1613(listOf(), listOf(INT), true),
        CC_GETMODELTRANSPARENT(listOf(), listOf(BOOLEAN), true),
        _1615(listOf(), listOf(INT)),
        _1616(listOf(), listOf(INT)),

        CC_GETINVOBJECT(listOf(), listOf(OBJ), true),
        CC_GETINVCOUNT(listOf(), listOf(COUNT), true),
        CC_GETID(listOf(), listOf(COMSUBID), true),

        CC_GETTARGETMASK(listOf(), listOf(INT), true),
        CC_GETOP(listOf(INT), listOf(OP), true),
        CC_GETOPBASE(listOf(), listOf(OPBASE), true),

        CC_CALLONRESIZE(listOf(BOOLEAN), listOf()),

        IF_SETPOSITION(listOf(X, Y, SETPOSH, SETPOSV, COMPONENT), listOf()),
        IF_SETSIZE(listOf(WIDTH, HEIGHT, SETSIZE, SETSIZE, COMPONENT), listOf()),
        IF_SETHIDE(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETNOCLICKTHROUGH(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETNOSCROLLTHROUGH(listOf(BOOLEAN, COMPONENT), listOf()),

        IF_SETSCROLLPOS(listOf(X, Y, COMPONENT), listOf()),
        IF_SETCOLOUR(listOf(COLOUR, COMPONENT), listOf()),
        IF_SETFILL(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETTRANS(listOf(TRANS, COMPONENT), listOf()),
        IF_SETLINEWID(listOf(INT, COMPONENT), listOf()),
        IF_SETGRAPHIC(listOf(GRAPHIC, COMPONENT), listOf()),
        IF_SET2DANGLE(listOf(ANGLE, COMPONENT), listOf()),
        IF_SETTILING(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETMODEL(listOf(MODEL, COMPONENT), listOf()),
        IF_SETMODELANGLE(listOf(INT, INT, INT, INT, INT, INT, COMPONENT), listOf()),
        IF_SETMODELANIM(listOf(SEQ, COMPONENT), listOf()),
        IF_SETMODELORTHOG(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETTEXT(listOf(TEXT, COMPONENT), listOf()),
        IF_SETTEXTFONT(listOf(FONTMETRICS, COMPONENT), listOf()),
        IF_SETTEXTALIGN(listOf(SETTEXTALIGNH, SETTEXTALIGNV, INT, COMPONENT), listOf()),
        IF_SETTEXTSHADOW(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETOUTLINE(listOf(INT, COMPONENT), listOf()),
        IF_SETGRAPHICSHADOW(listOf(COLOUR, COMPONENT), listOf()),
        IF_SETVFLIP(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETHFLIP(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETSCROLLSIZE(listOf(WIDTH, HEIGHT, COMPONENT), listOf()),
        IF_RESUME_PAUSEBUTTON(listOf(COMPONENT), listOf()),
        _2122(listOf(GRAPHIC, COMPONENT), listOf()),
        IF_SETFILLCOLOUR(listOf(COLOUR, COMPONENT), listOf()),
        _2124(listOf(INT, COMPONENT), listOf()),
        _2125(listOf(INT, COMPONENT), listOf()),
        IF_SETLINEDIRECTION(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETMODELTRANSPARENT(listOf(BOOLEAN, COMPONENT), listOf()),
        _2128(listOf(INT, INT, COMPONENT), listOf()),

        IF_SETOBJECT(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETNPCHEAD(listOf(NPC, COMPONENT), listOf()),
        IF_SETPLAYERHEAD_SELF(listOf(COMPONENT), listOf()),
        IF_SETOBJECT_NONUM(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETOBJECT_ALWAYS_NUM(listOf(OBJ, NUM, COMPONENT), listOf()),

        IF_SETOP(listOf(OPINDEX, OP, COMPONENT), listOf()),
        IF_SETDRAGGABLE(listOf(COMPONENT, INT, COMPONENT), listOf()),
        IF_SETDRAGGABLEBEHAVIOR(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADZONE(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADTIME(listOf(INT, COMPONENT), listOf()),
        IF_SETOPBASE(listOf(OPBASE, COMPONENT), listOf()),
        IF_SETTARGETVERB(listOf(STRING, COMPONENT), listOf()),
        IF_CLEAROPS(listOf(COMPONENT), listOf()),
        _2308(listOf(BOOLEAN, COMPONENT), listOf()),
        _2309(listOf(INT, COMPONENT), listOf()),
        IF_SETOPKEY(listOf(OPINDEX, KEY, FLAGS, COMPONENT), listOf()),
        IF_SETOPTKEY(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETOPKEYRATE(listOf(INT, INT, INT, COMPONENT), listOf()),
        IF_SETOPTKEYRATE(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETOPKEYIGNOREHELD(listOf(INT, COMPONENT), listOf()),
        IF_SETOPTKEYIGNOREHELD(listOf(COMPONENT), listOf()),

        IF_GETX(listOf(COMPONENT), listOf(X)),
        IF_GETY(listOf(COMPONENT), listOf(Y)),
        IF_GETWIDTH(listOf(COMPONENT), listOf(WIDTH)),
        IF_GETHEIGHT(listOf(COMPONENT), listOf(HEIGHT)),
        IF_GETHIDE(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETLAYER(listOf(COMPONENT), listOf(LAYER)),

        IF_GETSCROLLX(listOf(COMPONENT), listOf(X)),
        IF_GETSCROLLY(listOf(COMPONENT), listOf(Y)),
        IF_GETTEXT(listOf(COMPONENT), listOf(TEXT)),
        IF_GETSCROLLWIDTH(listOf(COMPONENT), listOf(WIDTH)),
        IF_GETSCROLLHEIGHT(listOf(COMPONENT), listOf(HEIGHT)),
        IF_GETMODELZOOM(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_X(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_Z(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_Y(listOf(COMPONENT), listOf(INT)),
        IF_GETTRANS(listOf(COMPONENT), listOf(TRANS)),
        _2610(listOf(COMPONENT), listOf(INT)),
        IF_GETCOLOUR(listOf(COMPONENT), listOf(COLOUR)),
        IF_GETFILLCOLOUR(listOf(COMPONENT), listOf(COLOUR)),
        _2613(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELTRANSPARENT(listOf(COMPONENT), listOf(BOOLEAN)),
        _2615(listOf(COMPONENT), listOf(INT)),
        _2616(listOf(COMPONENT), listOf(INT)),

        IF_GETINVOBJECT(listOf(COMPONENT), listOf(OBJ)),
        IF_GETINVCOUNT(listOf(COMPONENT), listOf(COUNT)),
        IF_HASSUB(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETTOP(listOf(), listOf(INTERFACE)),

        IF_GETTARGETMASK(listOf(COMPONENT), listOf(INT)),
        IF_GETOP(listOf(INT, COMPONENT), listOf(OP)),
        IF_GETOPBASE(listOf(COMPONENT), listOf(OPBASE)),

        IF_CALLONRESIZE(listOf(COMPONENT), listOf(), true),
        IF_TRIGGEROP(listOf(COMPONENT, COMSUBID, OPINDEX), listOf()),

        MES(listOf(_MES), listOf()),
        ANIM(listOf(SEQ, INT), listOf()),
        IF_CLOSE(listOf(), listOf()),
        RESUME_COUNTDIALOG(listOf(STRING), listOf()),
        RESUME_NAMEDIALOG(listOf(STRING), listOf()),
        RESUME_STRINGDIALOG(listOf(STRING), listOf()),
        OPPLAYER(listOf(INT, STRING), listOf()),
        IF_DRAGPICKUP(listOf(COMPONENT, INT, INT), listOf()),
        CC_DRAGPICKUP(listOf(INT, INT), listOf(), true),
        SETMOUSECAM(listOf(BOOLEAN), listOf()),
        GETREMOVEROOFS(listOf(), listOf(BOOLEAN)),
        SETREMOVEROOFS(listOf(BOOLEAN), listOf()),
        OPENURL(listOf(URL, BOOLEAN), listOf()),
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
        _3140(listOf(), listOf(), true),
        SETHIDEUSERNAME(listOf(BOOLEAN), listOf()),
        GETHIDEUSERNAME(listOf(), listOf(BOOLEAN)),
        SETREMEMBERUSERNAME(listOf(BOOLEAN), listOf()),
        GETREMEMBERUSERNAME(listOf(), listOf(BOOLEAN)),
        SHOW_IOS_REVIEW(listOf(), listOf()),
        _3157(listOf(INT, INT), listOf()),
        SETBRIGHTNESS(listOf(INT), listOf()),
        GETBRIGHTNESS(listOf(), listOf(INT)),
        SETANTIDRAG(listOf(BOOLEAN), listOf()),
        _3184(listOf(INT), listOf()),

        SOUND_SYNTH(listOf(SYNTH, INT, INT), listOf()),
        SOUND_SONG(listOf(INT), listOf()),
        SOUND_JINGLE(listOf(INT, INT), listOf()),
        SETVOLUMEMUSIC(listOf(INT), listOf()),
        GETVOLUMEMUSIC(listOf(), listOf(INT)),
        SETVOLUMESOUNDS(listOf(INT), listOf()),
        GETVOLUMESOUNDS(listOf(), listOf(INT)),
        SETVOLUMEAREASOUNDS(listOf(INT), listOf()),
        GETVOLUMEAREASOUNDS(listOf(), listOf(INT)),
        _3209(listOf(INT, INT), listOf()),
        _3210(listOf(INT), listOf(INT)),

        CLIENTCLOCK(listOf(), listOf(CLOCK)),
        INV_GETOBJ(listOf(INV, SLOT), listOf(OBJ)),
        INV_GETNUM(listOf(INV, SLOT), listOf(NUM)),
        INV_TOTAL(listOf(INV, OBJ), listOf(TOTAL)),
        INV_SIZE(listOf(INV), listOf(SIZE)),
        STAT(listOf(_STAT), listOf(LVL)),
        STAT_BASE(listOf(_STAT), listOf(LVL)),
        STAT_XP(listOf(_STAT), listOf(XP)),
        COORD(listOf(), listOf(_COORD)),
        COORDX(listOf(_COORD), listOf(X)),
        COORDY(listOf(_COORD), listOf(Y)),
        COORDZ(listOf(_COORD), listOf(Z)),
        MAP_MEMBERS(listOf(), listOf(BOOL)),
        INVOTHER_GETOBJ(listOf(INV, SLOT), listOf(OBJ)),
        INVOTHER_GETNUM(listOf(INV, SLOT), listOf(NUM)),
        INVOTHER_TOTAL(listOf(INV, OBJ), listOf(TOTAL)),
        STAFFMODLEVEL(listOf(), listOf(INT)),
        REBOOTTIMER(listOf(), listOf(INT)),
        MAP_WORLD(listOf(), listOf(WORLD)),
        RUNENERGY_VISIBLE(listOf(), listOf(INT)),
        RUNWEIGHT_VISIBLE(listOf(), listOf(INT)),
        PLAYERMOD(listOf(), listOf(BOOLEAN)),
        WORLDFLAGS(listOf(), listOf(FLAGS)),
        MOVECOORD(listOf(_COORD, X, Y, Z), listOf(_COORD)),
        MOUSE_GETX(listOf(), listOf(X)),
        MOUSE_GETY(listOf(), listOf(Y)),
        _3330(listOf(), listOf(_COORD)),

        ENUM_STRING(listOf(_ENUM, INT), listOf(STRING)),
        ENUM_GETOUTPUTCOUNT(listOf(_ENUM), listOf(COUNT)),

        FRIEND_COUNT(listOf(), listOf(COUNT)),
        FRIEND_GETNAME(listOf(INDEX), listOf(USERNAME, USERNAME)),
        FRIEND_GETWORLD(listOf(INDEX), listOf(WORLD)),
        FRIEND_GETRANK(listOf(INDEX), listOf(RANK)),
        FRIEND_SETRANK(listOf(USERNAME, RANK), listOf()),
        FRIEND_ADD(listOf(USERNAME), listOf()),
        FRIEND_DEL(listOf(USERNAME), listOf()),
        IGNORE_ADD(listOf(USERNAME), listOf()),
        IGNORE_DEL(listOf(USERNAME), listOf()),
        FRIEND_TEST(listOf(USERNAME), listOf(BOOLEAN)),
        CLAN_GETCHATDISPLAYNAME(listOf(), listOf(STRING)),
        CLAN_GETCHATCOUNT(listOf(), listOf(COUNT)),
        CLAN_GETCHATUSERNAME(listOf(INDEX), listOf(USERNAME)),
        CLAN_GETCHATUSERWORLD(listOf(INDEX), listOf(WORLD)),
        CLAN_GETCHATUSERRANK(listOf(INDEX), listOf(RANK)),
        CLAN_GETCHATMINKICK(listOf(), listOf(RANK)),
        CLAN_KICKUSER(listOf(USERNAME), listOf()),
        CLAN_GETCHATRANK(listOf(), listOf(RANK)),
        CLAN_JOINCHAT(listOf(USERNAME), listOf()),
        CLAN_LEAVECHAT(listOf(), listOf()),
        IGNORE_COUNT(listOf(), listOf(COUNT)),
        IGNORE_GETNAME(listOf(INDEX), listOf(USERNAME, USERNAME)),
        IGNORE_TEST(listOf(USERNAME), listOf(BOOLEAN)),
        CLAN_ISSELF(listOf(INDEX), listOf(BOOLEAN)),
        CLAN_GETCHATOWNERNAME(listOf(), listOf(USERNAME)),
        CLAN_ISFRIEND(listOf(INDEX), listOf(BOOLEAN)),
        CLAN_ISIGNORE(listOf(INDEX), listOf(BOOLEAN)),
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

        _3700(listOf(STRING, INT, INT), listOf(INT)),
        _3701(listOf(STRING, INT, INT), listOf(INT)),
        _3702(listOf(), listOf(INT)),

        ACTIVECLANSETTINGS_FIND_LISTENED(listOf(), listOf(BOOLEAN)),
        ACTIVECLANSETTINGS_FIND_AFFINED(listOf(CLANSLOT), listOf(BOOLEAN)),
        ACTIVECLANSETTINGS_GETCLANNAME(listOf(), listOf(STRING)),
        ACTIVECLANSETTINGS_GETALLOWUNAFFINED(listOf(), listOf(BOOLEAN)),
        ACTIVECLANSETTINGS_GETRANKTALK(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETRANKKICK(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETRANKLOOTSHARE(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETCOINSHARE(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETAFFINEDCOUNT(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME(listOf(CLANSLOT), listOf(STRING)),
        ACTIVECLANSETTINGS_GETAFFINEDRANK(listOf(CLANSLOT), listOf(INT)),
        ACTIVECLANSETTINGS_GETBANNEDCOUNT(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME(listOf(CLANSLOT), listOf(STRING)),
        ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO(listOf(CLANSLOT, INT, INT), listOf(INT)),
        ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT(listOf(), listOf(CLANSLOT)),
        ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT(listOf(), listOf(CLANSLOT)),
        ACTIVECLANSETTINGS_GETAFFINEDSLOT(listOf(STRING), listOf(CLANSLOT)),
        ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT(listOf(INT), listOf(CLANSLOT)),
        AFFINEDCLANSETTINGS_ADDBANNED_FROMCHANNEL(listOf(CLANSLOT, INT), listOf()),
        ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY(listOf(CLANSLOT), listOf(INT)),
        AFFINEDCLANSETTINGS_SETMUTED_FROMCHANNEL(listOf(CLANSLOT, BOOLEAN, INT), listOf()),
        ACTIVECLANSETTINGS_GETAFFINEDMUTED(listOf(CLANSLOT), listOf(BOOLEAN)),

        ACTIVECLANCHANNEL_FIND_LISTENED(listOf(), listOf(BOOLEAN)),
        ACTIVECLANCHANNEL_FIND_AFFINED(listOf(CLANSLOT), listOf(BOOLEAN)),
        ACTIVECLANCHANNEL_GETCLANNAME(listOf(), listOf(STRING)),
        ACTIVECLANCHANNEL_GETRANKKICK(listOf(), listOf(INT)),
        ACTIVECLANCHANNEL_GETRANKTALK(listOf(), listOf(INT)),
        ACTIVECLANCHANNEL_GETUSERCOUNT(listOf(), listOf(INT)),
        ACTIVECLANCHANNEL_GETUSERDISPLAYNAME(listOf(CLANSLOT), listOf(STRING)),
        ACTIVECLANCHANNEL_GETUSERRANK(listOf(CLANSLOT), listOf(INT)),
        ACTIVECLANCHANNEL_GETUSERWORLD(listOf(CLANSLOT), listOf(INT)),
        ACTIVECLANCHANNEL_KICKUSER(listOf(CLANSLOT), listOf()),
        ACTIVECLANCHANNEL_GETUSERSLOT(listOf(STRING), listOf(CLANSLOT)),
        ACTIVECLANCHANNEL_GETSORTEDUSERSLOT(listOf(INT), listOf(CLANSLOT)),

        CLANPROFILE_FIND(listOf(), listOf(BOOLEAN)),

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
        SETBIT(listOf(FLAGS, INDEX), listOf(FLAGS)),
        CLEARBIT(listOf(FLAGS, INDEX), listOf(FLAGS)),
        TESTBIT(listOf(FLAGS, INDEX), listOf(BOOL)),
        MOD(listOf(INT, INT), listOf(INT)),
        POW(listOf(INT, INT), listOf(INT)),
        INVPOW(listOf(INT, INT), listOf(INT)),
        AND(listOf(INT, INT), listOf(INT)),
        OR(listOf(INT, INT), listOf(INT)),
        SCALE(listOf(INT, INT, INT), listOf(INT)),
        BITCOUNT(listOf(INT), listOf(INT)),
        TOGGLEBIT(listOf(INT, INT), listOf(INT)),
        SETBIT_RANGE(listOf(INT, INT, INT), listOf(INT)),
        CLEARBIT_RANGE(listOf(INT, INT, INT), listOf(INT)),
        GETBIT_RANGE(listOf(INT, INT, INT), listOf(INT)),
        SETBIT_RANGE_TOINT(listOf(INT, INT, INT, INT), listOf(INT)),
        SIN_DEG(listOf(INT), listOf(INT)),
        COS_DEG(listOf(INT), listOf(INT)),
        _4034(listOf(INT, INT), listOf(INT)),
        ABS(listOf(INT), listOf(INT)),
        APPEND_NUM(listOf(STRING, INT), listOf(STRING)),
        APPEND(listOf(STRING, STRING), listOf(STRING)),
        APPEND_SIGNNUM(listOf(STRING, INT), listOf(STRING)),
        LOWERCASE(listOf(STRING), listOf(STRING)),
        FROMDATE(listOf(INT), listOf(STRING)),
        TEXT_GENDER(listOf(STRING, STRING), listOf(STRING)),
        TOSTRING(listOf(INT), listOf(STRING)),
        COMPARE(listOf(STRING, STRING), listOf(INT)),
        PARAHEIGHT(listOf(STRING, WIDTH, FONTMETRICS), listOf(HEIGHT)),
        PARAWIDTH(listOf(STRING, WIDTH, FONTMETRICS), listOf(WIDTH)),
        TEXT_SWITCH(listOf(INT, STRING, STRING), listOf(STRING)),
        ESCAPE(listOf(STRING), listOf(STRING)),
        APPEND_CHAR(listOf(STRING, CHAR), listOf(STRING)),
        CHAR_ISPRINTABLE(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHANUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHA(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISNUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        STRING_LENGTH(listOf(STRING), listOf(LENGTH)),
        SUBSTRING(listOf(STRING, INDEX, INDEX), listOf(STRING)),
        REMOVETAGS(listOf(STRING), listOf(STRING)),
        STRING_INDEXOF_CHAR(listOf(STRING, CHAR), listOf(INDEX)),
        STRING_INDEXOF_STRING(listOf(STRING, STRING, INDEX), listOf(INDEX)),

        OC_NAME(listOf(OBJ), listOf(STRING)),
        OC_OP(listOf(OBJ, OPINDEX), listOf(OP)),
        OC_IOP(listOf(OBJ, OPINDEX), listOf(OP)),
        OC_COST(listOf(OBJ), listOf(INT)),
        OC_STACKABLE(listOf(OBJ), listOf(BOOLEAN)),
        OC_CERT(listOf(OBJ), listOf(OBJ)),
        OC_UNCERT(listOf(OBJ), listOf(OBJ)),
        OC_MEMBERS(listOf(OBJ), listOf(BOOL)),
        OC_PLACEHOLDER(listOf(OBJ), listOf(OBJ)),
        OC_UNPLACEHOLDER(listOf(OBJ), listOf(OBJ)),
        OC_FIND(listOf(STRING, BOOLEAN), listOf(INT)),
        OC_FINDNEXT(listOf(), listOf(OBJ)),
        OC_FINDRESET(listOf(), listOf()),

        CHAT_GETFILTER_PUBLIC(listOf(), listOf(CHATFILTER)),
        CHAT_SETFILTER(listOf(CHATFILTER, CHATFILTER, CHATFILTER), listOf()),
        CHAT_SENDABUSEREPORT(listOf(STRING, INT, INT), listOf()),
        CHAT_GETHISTORY_BYTYPEANDLINE(listOf(CHATTYPE, INT), listOf(MESUID, CLOCK, USERNAME, STRING, _MES, INT)),
        CHAT_GETHISTORY_BYUID(listOf(MESUID), listOf(CHATTYPE, CLOCK, USERNAME, STRING, _MES, INT)),
        CHAT_GETFILTER_PRIVATE(listOf(), listOf(CHATFILTER)),
        CHAT_SENDPUBLIC(listOf(_MES, INT), listOf()),
        CHAT_SENDPRIVATE(listOf(USERNAME, _MES), listOf()),
        CHAT_SENDCLAN(listOf(_MES, INT, INT), listOf()),
        CHAT_PLAYERNAME(listOf(), listOf(USERNAME)),
        CHAT_GETFILTER_TRADE(listOf(), listOf(CHATFILTER)),
        CHAT_GETHISTORYLENGTH(listOf(CHATTYPE), listOf(LENGTH)),
        CHAT_GETNEXTUID(listOf(MESUID), listOf(MESUID)),
        CHAT_GETPREVUID(listOf(MESUID), listOf(MESUID)),
        DOCHEAT(listOf(STRING), listOf()),
        CHAT_SETMESSAGEFILTER(listOf(STRING), listOf()),
        CHAT_GETMESSAGEFILTER(listOf(), listOf(STRING)),
        WRITECONSOLE(listOf(STRING), listOf()),
        CHAT_SETTIMESTAMPS(listOf(INT), listOf()),
        CHAT_GETTIMESTAMPS(listOf(), listOf(INT)),
        CHAT_GETHISTORYEX_BYTYPEANDLINE(listOf(CHATTYPE, INT), listOf(MESUID, CLOCK, USERNAME, STRING, _MES, INT, STRING, INT)),
        CHAT_GETHISTORYEX_BYUID(listOf(MESUID), listOf(CHATTYPE, CLOCK, USERNAME, STRING, _MES, INT, STRING, INT)),

        GETWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETWINDOWMODE(listOf(WINDOWMODE), listOf()),
        GETDEFAULTWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETDEFAULTWINDOWMODE(listOf(WINDOWMODE), listOf()),
        _5310(listOf(INT), listOf()),
        _5311(listOf(INT, INT), listOf()),
        _5312(listOf(INT), listOf()),
        
        _5350(listOf(INT, STRING, STRING), listOf()),
        _5351(listOf(STRING), listOf()),

        CAM_FORCEANGLE(listOf(INT, INT), listOf()),
        CAM_GETANGLE_XA(listOf(), listOf(INT)),
        CAM_GETANGLE_YA(listOf(), listOf(INT)),
        CAM_SETFOLLOWHEIGHT(listOf(INT), listOf()),
        CAM_GETFOLLOWHEIGHT(listOf(), listOf(INT)),

        LOGOUT(listOf(), listOf()),
        FEDERATED_LOGIN(listOf(STRING, STRING), listOf()),
        _5632(listOf(), listOf(INT)),
        _5633(listOf(STRING, STRING), listOf()),

        VIEWPORT_SETFOV(listOf(INT, INT), listOf()),
        VIEWPORT_SETZOOM(listOf(INT, INT), listOf()),
        VIEWPORT_CLAMPFOV(listOf(INT, INT, INT, INT), listOf()),
        VIEWPORT_GETEFFECTIVESIZE(listOf(), listOf(WIDTH, HEIGHT)),
        VIEWPORT_GETZOOM(listOf(), listOf(INT, INT)),
        VIEWPORT_GETFOV(listOf(), listOf(INT, INT)),
        _6210(listOf(INT), listOf()),
        _6212(listOf(), listOf()),
        _6220(listOf(), listOf(INT)),
        _6221(listOf(), listOf(INT)),
        _6222(listOf(), listOf(INT)),
        _6223(listOf(), listOf(INT)),

        WORLDLIST_FETCH(listOf(), listOf(BOOLEAN)),
        WORLDLIST_START(listOf(), listOf(WORLD, FLAGS, STRING, INT, COUNT, STRING)),
        WORLDLIST_NEXT(listOf(), listOf(WORLD, FLAGS, STRING, INT, COUNT, STRING)),
        WORLDLIST_SPECIFIC(listOf(WORLD), listOf(WORLD, FLAGS, STRING, INT, COUNT, STRING)),
        WORLDLIST_SORT(listOf(INT, BOOLEAN, INT, BOOLEAN), listOf()),
        _6511(listOf(INT), listOf(INT, INT, STRING, INT, INT, STRING)),
        SETFOLLOWEROPSLOWPRIORITY(listOf(BOOLEAN), listOf()),

        ON_MOBILE(listOf(), listOf(BOOLEAN)),
        CLIENTTYPE(listOf(), listOf(_CLIENTTYPE)),
        _6520(listOf(), listOf()),
        MOBILE_KEYBOARDHIDE(listOf(), listOf()),
        _6522(listOf(STRING, INT), listOf()),
        _6523(listOf(STRING, INT), listOf()),
        MOBILE_BATTERYLEVEL(listOf(), listOf(INT)),
        MOBILE_BATTERYCHARGING(listOf(), listOf(BOOLEAN)),
        MOBILE_WIFIAVAILABLE(listOf(), listOf(BOOLEAN)),
        _6527(listOf(), listOf(INT)),

        _6600(listOf(), listOf()),
        WORLDMAP_GETMAPNAME(listOf(MAPAREA), listOf(STRING)),
        WORLDMAP_SETMAP(listOf(MAPAREA), listOf()),
        WORLDMAP_GETZOOM(listOf(), listOf(INT)),
        WORLDMAP_SETZOOM(listOf(INT), listOf()),
        WORLDMAP_ISLOADED(listOf(), listOf(BOOLEAN)),
        WORLDMAP_JUMPTODISPLAYCOORD(listOf(_COORD), listOf()),
        WORLDMAP_JUMPTODISPLAYCOORD_INSTANT(listOf(_COORD), listOf()),
        WORLDMAP_JUMPTOSOURCECOORD(listOf(_COORD), listOf()),
        WORLDMAP_JUMPTOSOURCECOORD_INSTANT(listOf(_COORD), listOf()),
        WORLDMAP_GETDISPLAYPOSITION(listOf(), listOf(INT, INT)),
        WORLDMAP_GETCONFIGORIGIN(listOf(MAPAREA), listOf(INT)),
        WORLDMAP_GETCONFIGSIZE(listOf(MAPAREA), listOf(INT, INT)),
        WORLDMAP_GETCONFIGBOUNDS(listOf(MAPAREA), listOf(INT, INT, INT, INT)),
        WORLDMAP_GETCONFIGZOOM(listOf(MAPAREA), listOf(INT)),
        _6615(listOf(), listOf(INT, INT)),
        WORLDMAP_GETCURRENTMAP(listOf(), listOf(MAPAREA)),
        WORLDMAP_GETDISPLAYCOORD(listOf(_COORD), listOf(INT, INT)),
        _6618(listOf(_COORD), listOf(INT, INT)),
        _6619(listOf(INT, _COORD), listOf()),
        _6620(listOf(INT, _COORD), listOf()),
        WORLDMAP_COORDINMAP(listOf(MAPAREA, _COORD), listOf(BOOLEAN)),
        WORLDMAP_GETSIZE(listOf(), listOf(INT, INT)),
        _6623(listOf(_COORD), listOf(MAPAREA)),
        _6624(listOf(INT), listOf()),
        _6625(listOf(), listOf()),
        _6626(listOf(INT), listOf()),
        _6627(listOf(), listOf()),
        WORLDMAP_PERPETUALFLASH(listOf(BOOLEAN), listOf()),
        WORLDMAP_FLASHELEMENT(listOf(MAPELEMENT), listOf()),
        WORLDMAP_FLASHELEMENTCATEGORY(listOf(CATEGORY), listOf()),
        WORLDMAP_STOPCURRENTFLASHES(listOf(), listOf()),
        WORLDMAP_DISABLEELEMENTS(listOf(BOOLEAN), listOf()),
        WORLDMAP_DISABLEELEMENT(listOf(INT, BOOLEAN), listOf()),
        WORLDMAP_DISABLEELEMENTCATEGORY(listOf(CATEGORY, BOOLEAN), listOf()),
        WORLDMAP_GETDISABLEELEMENTS(listOf(), listOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENT(listOf(INT), listOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENTCATEGORY(listOf(CATEGORY), listOf(BOOLEAN)),
        _6638(listOf(INT, _COORD), listOf(INT)),
        WORLDMAP_LISTELEMENT_START(listOf(), listOf(MAPELEMENT, _COORD)),
        WORLDMAP_LISTELEMENT_NEXT(listOf(), listOf(MAPELEMENT, _COORD)),
        MEC_TEXT(listOf(MAPELEMENT), listOf(TEXT)),
        MEC_TEXTSIZE(listOf(MAPELEMENT), listOf(INT)),
        MEC_CATEGORY(listOf(MAPELEMENT), listOf(CATEGORY)),
        MEC_SPRITE(listOf(MAPELEMENT), listOf(INT)),
        WORLDMAP_ELEMENT(listOf(), listOf(MAPELEMENT)),
        _6698(listOf(), listOf(_COORD)),
        WORLDMAP_ELEMENTCOORD(listOf(), listOf(_COORD)),

        _6700(listOf(INT, STRING, INT), listOf()),
        _6701(listOf(INT), listOf()),
        _6702(listOf(INT, STRING, INT), listOf()),
        _6703(listOf(INT), listOf()),
        _6704(listOf(INT, STRING, INT), listOf()),
        _6705(listOf(INT), listOf()),
        _6706(listOf(INT, STRING, INT), listOf()),
        _6707(listOf(INT), listOf()),
        _6708(listOf(INT, STRING, INT), listOf()),
        _6709(listOf(INT), listOf()),
        
        _6750(listOf(), listOf(STRING)),
        _6751(listOf(), listOf(NPC_UID)),
        _6752(listOf(), listOf(INT)),
        _6753(listOf(), listOf(NPC)),
        NC_NAME(listOf(NPC), listOf(STRING)),

        _6800(listOf(), listOf(STRING)),
        _6801(listOf(), listOf(_COORD)),
        _6802(listOf(), listOf(LOC)),

        _6850(listOf(), listOf(STRING)),
        _6851(listOf(), listOf(_COORD)),
        _6852(listOf(), listOf(OBJ)),
        _6853(listOf(), listOf(INT)),
        
        _6900(listOf(), listOf(STRING)),
        _6902(listOf(), listOf(INT)),
        _6903(listOf(INT), listOf(_COORD)),
        _6904(listOf(), listOf(PLAYER_UID)),
        _6905(listOf(), listOf(PLAYER_UID)),

        _6950(listOf(), listOf(_COORD)),
        
        _7000(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_NPC_ON(listOf(NPC_UID, _COORD, INT), listOf()),
        HIGHLIGHT_NPC_OFF(listOf(NPC_UID, _COORD, INT), listOf()),
        _7003(listOf(NPC, _COORD, INT), listOf(BOOLEAN)),
        _7004(listOf(INT), listOf()),
        _7005(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_NPCTYPE_ON(listOf(NPC, INT), listOf()),
        HIGHLIGHT_NPCTYPE_OFF(listOf(NPC, INT), listOf()),
        _7008(listOf(NPC, INT), listOf(BOOLEAN)),
        _7009(listOf(INT), listOf()),
        _7010(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_LOC_ON(listOf(LOC, _COORD, INT, INT), listOf()),
        HIGHLIGHT_LOC_OFF(listOf(LOC, _COORD, INT, INT), listOf()),
        _7013(listOf(LOC, INT, INT, INT), listOf(BOOLEAN)),
        _7014(listOf(INT), listOf()),
        _7015(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_LOCTYPE_ON(listOf(LOC, INT), listOf()),
        HIGHLIGHT_LOCTYPE_OFF(listOf(LOC, INT), listOf()),
        _7018(listOf(LOC, INT), listOf(BOOLEAN)),
        _7019(listOf(INT), listOf()),
        _7020(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_OBJ_ON(listOf(OBJ, _COORD, INT, INT), listOf()),
        HIGHLIGHT_OBJ_OFF(listOf(OBJ, _COORD, INT, INT), listOf()),
        _7023(listOf(OBJ, _COORD, INT, INT), listOf(BOOLEAN)),
        _7024(listOf(INT), listOf()),
        _7025(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_OBJTYPE_ON(listOf(OBJ, INT), listOf()),
        HIGHLIGHT_OBJTYPE_OFF(listOf(OBJ, INT), listOf()),
        _7028(listOf(OBJ, INT), listOf(BOOLEAN)),
        _7029(listOf(INT), listOf()),
        _7030(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_PLAYER_ON(listOf(STRING, INT), listOf()),
        HIGHLIGHT_PLAYER_OFF(listOf(STRING, INT), listOf()),
        _7033(listOf(STRING, INT), listOf(INT)),
        _7034(listOf(INT), listOf()),
        _7035(listOf(INT, INT, INT, INT, INT), listOf()),
        HIGHLIGHT_TILE_ON(listOf(_COORD, INT, INT), listOf()),
        HIGHLIGHT_TILE_OFF(listOf(_COORD, INT, INT), listOf()),
        _7038(listOf(_COORD, INT, INT), listOf(BOOLEAN)),
        _7039(listOf(INT), listOf()),
        
        _7100(listOf(), listOf(INT)),
        _7101(listOf(), listOf(STRING, STRING)),
        _7102(listOf(), listOf(INT)),
        _7103(listOf(), listOf(INT)),
        _7104(listOf(), listOf(INT)),
        _7105(listOf(), listOf(INT)),
        _7106(listOf(), listOf(INT)),
        _7107(listOf(), listOf(INT)),
        _7108(listOf(), listOf(BOOLEAN)),
        _7109(listOf(), listOf(INT)),
        _7110(listOf(), listOf(INT)),
        _7120(listOf(INT), listOf(INT)),
        _7121(listOf(INT, INT), listOf(INT)),
        _7122(listOf(INT, INT), listOf(INT)),
        
        _7200(listOf(INT, INT, INT, INT, INT), listOf(NEWVAR)),
        _7201(listOf(INT, INT, INT, INT, INT), listOf(INT)),
        _7202(listOf(INT, INT, INT, INT, INT), listOf(INT)),
        _7203(listOf(INT, INT, INT, INT, INT), listOf(INT)),
        _7204(listOf(INT, INT, INT, INT, INT, INT), listOf(INT)),
        _7205(listOf(), listOf(INT)),
        _7206(listOf(), listOf(INT)),
        _7207(listOf(), listOf(INT)),
        _7208(listOf(), listOf(INT)),
        _7209(listOf(), listOf(INT)),
        _7210(listOf(INT), listOf()),
        _7211(listOf(INT), listOf()),
        _7212(listOf(INT), listOf()),
        _7213(listOf(INT), listOf()),
        _7214(listOf(INT, INT), listOf()),
        SETMINIMAPLOCK(listOf(BOOLEAN), listOf()),
        _7252(listOf(INT), listOf()),
        ;

        override val id = opcodes.getValue(name)

        private val defStackTypes = defs.map { it.stackType }

        override fun translate(state: InterpreterState): Instruction {
            val dot = o && state.operand.boolean
            val opArgs = Expression(state.pop(args.size))
            assign(state.typings.of(opArgs), state.typings.of(args))
            val operation = Expression.Operation(defStackTypes, id, opArgs, dot)
            val operationTyping = state.typings.of(operation)
            for (i in defs.indices) {
                operationTyping[i].freeze(defs[i])
            }
            val opDefs = Expression(state.push(defStackTypes))
            assign(state.typings.of(operation), state.typings.of(opDefs))
            return Instruction.Assignment(opDefs, operation)
        }
    }

    object JoinString : Command {

        override val id = JOIN_STRING

        override fun translate(state: InterpreterState): Instruction {
            val args = state.pop(state.operand.int)
            val operation = Expression.Operation(listOf(StackType.STRING), id, Expression(args))
            val def = state.push(StackType.STRING)
            return Instruction.Assignment(def, operation)
        }
    }

    enum class ClientScript : Command {

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
        CC_SETONCLANSETTINGSTRANSMIT,
        CC_SETONCLANCHANNELTRANSMIT,
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
        IF_SETONRESIZE,
        IF_SETONCLANSETTINGSTRANSMIT,
        IF_SETONCLANCHANNELTRANSMIT,
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            var component: Element? = null
            var dot = false
            if (id >= 2000) {
                val c = state.pop(StackType.INT)
                assign(state.typings.of(c), state.typings.of(COMPONENT))
                component = c
            } else {
                dot = state.operand.boolean
            }
            val desc = ClientScriptDesc(state.popValue().string)
            val triggers = ArrayList<Element>()
            if (desc.triggers) {
                val triggerCount = state.popValue().int
                repeat(triggerCount) {
                    val e = when (this) {
                        IF_SETONSTATTRANSMIT, CC_SETONSTATTRANSMIT -> {
                            state.pop(StackType.INT).also { assign(state.typings.of(it), state.typings.of(_STAT)) }
                        }
                        IF_SETONINVTRANSMIT, CC_SETONINVTRANSMIT -> {
                            state.pop(StackType.INT).also { assign(state.typings.of(it), state.typings.of(INV)) }
                        }
                        IF_SETONVARTRANSMIT, CC_SETONVARTRANSMIT -> {
                            Element.Pointer(Variable.varp(state.popValue().int))
                        }
                        else -> error(this)
                    }
                    triggers.add(e)
                }
                triggers.reverse()
            }
            val args = ArrayList<Element>(desc.argumentTypes.size)
            for (t in desc.argumentTypes.asReversed()) {
                val ep = state.peekValue()?.let { EventProperty.of(it) }
                val p = state.pop(t.stackType)
                args.add(ep ?: p)
            }
            val scriptId = state.popValue().int
            state.callGraph.call(state.scriptId, scriptId, Trigger.clientscript)
            args.reverse()
            val argsExpr = Expression(args)
            val argsTyping = state.typings.args(scriptId, args.map { it.stackType })
            for (i in desc.argumentTypes.indices) {
                argsTyping[i].freeze(desc.argumentTypes[i])
            }
            assign(state.typings.of(argsExpr), argsTyping)
            return Instruction.Assignment(Expression.ClientScript(id, scriptId, argsExpr, Expression(triggers), dot, component))
        }
    }

    enum class Param(val prototype: Prototype) : Command {

        NC_PARAM(NPC),
        LC_PARAM(LOC),
        OC_PARAM(OBJ),
        STRUCT_PARAM(STRUCT),
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            val paramId = checkNotNull(state.peekValue()).int
            val paramType = state.paramTypes.loadNotNull(paramId)
            val param = state.pop(StackType.INT)
            assign(state.typings.of(param), state.typings.of(PARAM))
            val recv = state.pop(StackType.INT)
            assign(state.typings.of(recv), state.typings.of((prototype)))
            val operation = Expression.Operation(listOf(paramType.stackType), id, Expression(recv, param))
            val operationTyping = state.typings.of(operation).single()
            operationTyping.freeze(paramType)
            val def = state.push(paramType.stackType)
            assign(operationTyping, state.typings.of(def))
            return Instruction.Assignment(def, operation)
        }
    }
}