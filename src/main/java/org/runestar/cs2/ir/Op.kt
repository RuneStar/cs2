package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.TopType
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
            list.addAll(CcIfSetOn.values().asList())
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

        override val id = Opcodes.GOSUB_WITH_PARAMS

        override fun translate(state: Interpreter.State): Insn {
            val invokeId = state.intOperand
            check(invokeId != state.id) // todo

            val invoked = Interpreter(state.loader).interpret(invokeId)

            val args = ArrayList<Expr>()
            args.add(Expr.Cst(INT, invokeId))
            invoked.args.forEach {
                args.add(state.pop(it.type))
            }

            val returns = ArrayList<Expr.Var>()
            invoked.returns.forEach {
                returns.add(state.push(it))
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
            val valueTypeCst = state.peekCst(INT)
            val valueType = state.pop(TYPE)
            val keyType = state.pop(TYPE)
            val args = mutableListOf<Expr>(keyType, valueType, enumId, key)
            val valueTypeType = Type.of((valueTypeCst.cst as Int).toChar())
            val value = state.push(valueTypeType)
            return Insn.Assignment(listOf(value), Expr.Operation(listOf(value.type), id, args))
        }
    }

    private enum class BranchCompare(override val id: Int) : Op {

        BRANCH_NOT(Opcodes.BRANCH_NOT),
        BRANCH_EQUALS(Opcodes.BRANCH_EQUALS),
        BRANCH_LESS_THAN(Opcodes.BRANCH_LESS_THAN),
        BRANCH_GREATER_THAN(Opcodes.BRANCH_GREATER_THAN),
        BRANCH_LESS_THAN_OR_EQUALS(Opcodes.BRANCH_LESS_THAN_OR_EQUALS),
        BRANCH_GREATER_THAN_OR_EQUALS(Opcodes.BRANCH_GREATER_THAN_OR_EQUALS);

        override fun translate(state: Interpreter.State): Insn {
            val r = state.pop(INT)
            val l = state.pop(INT)
            val expr = Expr.Operation(emptyList(), id, mutableListOf(l, r))
            return Insn.Branch(expr, Insn.Label(state.pc + state.intOperand + 1))
        }
    }

    private data class Arg(val type: Type, val src: Src)

    private enum class Src {
        L, S, O
    }

    private enum class PushCst(val type: Type) : Op {
        PUSH_CONSTANT_INT(Type.INT),
        PUSH_CONSTANT_STRING(Type.STRING);

        override val id: Int = namesReverse.getValue(name)

        override fun translate(state: Interpreter.State): Insn {
            val cst = state.operand(type)
            return Insn.Assignment(listOf(state.push(cst)), cst)
        }
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
        _42(arrayOf(INT u O), arrayOf(INT u S)),
        _43(arrayOf(INT u O, INT u S)),
        DEFINE_ARRAY(arrayOf(INT u S, INT u O)),
        PUSH_ARRAY_INT(arrayOf(INT u S, INT u O), arrayOf(INT u S)),
        POP_ARRAY_INT(arrayOf(INT u S, INT u S, INT u O)),
        _47(arrayOf(INT u O), arrayOf(STRING u S)),
        _48(arrayOf(INT u O, STRING u S)),
        CC_CREATE(arrayOf(INT u S, INT u S, INT u S)),
        CC_DELETE(),
        CC_DELETEALL(arrayOf(INT u S)),
        _200(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _201(arrayOf(INT u S), arrayOf(INT u S)),

        CC_SETPOSITION(arrayOf(INT u S, INT u S, INT u S, INT u S)),
        CC_SETSIZE(arrayOf(INT u S, INT u S, INT u S, INT u S)),
        CC_SETHIDE(arrayOf(INT u S)),
        _1005(arrayOf(INT u S)),
        _1006(arrayOf(INT u S)),

        CC_SETSCROLLPOS(arrayOf(INT u S, INT u S)),
        CC_SETCOLOUR(arrayOf(INT u S)),
        CC_SETFILL(arrayOf(INT u S)),
        CC_SETTRANS(arrayOf(INT u S)),
        CC_SETLINEWID(arrayOf(INT u S)),
        CC_SETGRAPHIC(arrayOf(INT u S)),
        CC_SET2DANGLE(arrayOf(INT u S)),
        CC_SETTILING(arrayOf(INT u S)),
        CC_SETMODEL(arrayOf(INT u S)),
        CC_SETMODELANGLE(arrayOf(INT u S, INT u S, INT u S, INT u S, INT u S, INT u S)),
        CC_SETMODELANIM(arrayOf(INT u S)),
        CC_SETMODELORTHOG(arrayOf(INT u S)),
        CC_SETTEXT(arrayOf(STRING u S)),
        CC_SETTEXTFONT(arrayOf(INT u S)),
        CC_SETTEXTALIGN(arrayOf(INT u S, INT u S, INT u S)),
        CC_SETTEXTANTIMACRO(arrayOf(INT u S)),
        CC_SETOUTLINE(arrayOf(INT u S)),
        CC_SETGRAPHICSHADOW(arrayOf(INT u S)),
        CC_SETVFLIP(arrayOf(INT u S)),
        CC_SETHFLIP(arrayOf(INT u S)),
        CC_SETSCROLLSIZE(arrayOf(INT u S, INT u S)),
        _1121(),
        _1122(arrayOf(INT u S)),
        _1123(arrayOf(INT u S)),
        _1124(arrayOf(INT u S)),
        _1125(arrayOf(INT u S)),
        _1126(arrayOf(INT u S)),
        _1127(arrayOf(INT u S)),

        CC_SETOBJECT(arrayOf(INT u S, INT u S)),
        CC_SETNPCHEAD(arrayOf(INT u S)),
        CC_SETPLAYERHEAD_SELF(),
        CC_SETOBJECT_NONUM(arrayOf(INT u S, INT u S)),
        CC_SETOBJECT_ALWAYS_NUM(arrayOf(INT u S, INT u S)),

        CC_SETOP(arrayOf(INT u S, STRING u S)),
        CC_SETDRAGGABLE(arrayOf(INT u S, INT u S)),
        CC_SETDRAGGABLEBEHAVIOR(arrayOf(INT u S)),
        CC_SETDRAGDEADZONE(arrayOf(INT u S)),
        CC_SETDRAGDEADTIME(arrayOf(INT u S)),
        CC_SETOPBASE(arrayOf(STRING u S)),
        CC_SETTARGETVERB(arrayOf(STRING u S)),
        CC_CLEAROPS(),

        CC_GETX(defs = arrayOf(INT u S)),
        CC_GETY(defs = arrayOf(INT u S)),
        CC_GETWIDTH(defs = arrayOf(INT u S)),
        CC_GETHEIGHT(defs = arrayOf(INT u S)),
        CC_GETHIDE(defs = arrayOf(BOOLEAN u S)),
        _1505(defs = arrayOf(INT u S)),

        CC_GETSCROLLX(defs = arrayOf(INT u S)),
        CC_GETSCROLLY(defs = arrayOf(INT u S)),
        CC_GETTEXT(defs = arrayOf(STRING u S)),
        CC_GETSCROLLWIDTH(defs = arrayOf(INT u S)),
        CC_GETSCROLLHEIGHT(defs = arrayOf(INT u S)),
        CC_GETMODELZOOM(defs = arrayOf(INT u S)),
        CC_GETMODELANGLE_X(defs = arrayOf(INT u S)),
        CC_GETMODELANGLE_Z(defs = arrayOf(INT u S)),
        CC_GETMODELANGLE_Y(defs = arrayOf(INT u S)),
        CC_GETTRANS(defs = arrayOf(INT u S)),
        _1610(defs = arrayOf(INT u S)),
        _1611(defs = arrayOf(INT u S)),
        _1612(defs = arrayOf(INT u S)),
        _1613(defs = arrayOf(INT u S)),
        _1614(defs = arrayOf(BOOLEAN u S)),

        CC_GETINVOBJECT(defs = arrayOf(INT u S)),
        CC_GETINVCOUNT(defs = arrayOf(INT u S)),
        CC_GETID(defs = arrayOf(INT u S)),

        CC_GETTARGETMASK(defs = arrayOf(INT u S)),
        CC_GETOP(arrayOf(INT u S), arrayOf(STRING u S)),
        CC_GETOPBASE(defs = arrayOf(STRING u S)),

        IF_SETPOSITION(arrayOf(INT u S, INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETSIZE(arrayOf(INT u S, INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETHIDE(arrayOf(INT u S, COMPONENT u S)),
        _2005(arrayOf(INT u S, COMPONENT u S)),
        _2006(arrayOf(INT u S, COMPONENT u S)),

        IF_SETSCROLLPOS(arrayOf(INT u S, INT u S, COMPONENT u S)),
        IF_SETCOLOUR(arrayOf(INT u S, COMPONENT u S)),
        IF_SETTRANS(arrayOf(INT u S, COMPONENT u S)),
        IF_SETLINEWID(arrayOf(INT u S, COMPONENT u S)),
        IF_SETGRAPHIC(arrayOf(INT u S, COMPONENT u S)),
        IF_SET2DANGLE(arrayOf(INT u S, COMPONENT u S)),
        IF_SETTILING(arrayOf(INT u S, COMPONENT u S)),
        IF_SETMODEL(arrayOf(INT u S, COMPONENT u S)),
        IF_SETMODELANGLE(arrayOf(INT u S, INT u S, INT u S, INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETMODELANIM(arrayOf(INT u S, COMPONENT u S)),
        IF_SETMODELORTHOG(arrayOf(INT u S, COMPONENT u S)),
        IF_SETTEXT(arrayOf(STRING u S, COMPONENT u S)),
        IF_SETTEXTFONT(arrayOf(INT u S, COMPONENT u S)),
        IF_SETTEXTALIGN(arrayOf(INT u S, INT u S, INT u S, COMPONENT u S)),
        IF_SETTEXTANTIMACRO(arrayOf(INT u S, COMPONENT u S)),
        IF_SETOUTLINE(arrayOf(INT u S, COMPONENT u S)),
        IF_SETGRAPHICSHADOW(arrayOf(INT u S, COMPONENT u S)),
        IF_SETVFLIP(arrayOf(INT u S, COMPONENT u S)),
        IF_SETHFLIP(arrayOf(INT u S, COMPONENT u S)),
        IF_SETSCROLLSIZE(arrayOf(INT u S, INT u S, COMPONENT u S)),
        _2121(arrayOf(COMPONENT u S)),
        _2122(arrayOf(INT u S, COMPONENT u S)),
        _2123(arrayOf(INT u S, COMPONENT u S)),
        _2124(arrayOf(INT u S, COMPONENT u S)),
        _2125(arrayOf(INT u S, COMPONENT u S)),
        _2126(arrayOf(INT u S, COMPONENT u S)),
        _2127(arrayOf(INT u S, COMPONENT u S)),

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
        _2610(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2611(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2612(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2613(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2614(arrayOf(COMPONENT u S), arrayOf(BOOLEAN u S)),

        IF_GETINVOBJECT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETINVCOUNT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETID(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2706(defs = arrayOf(INT u S)),

        IF_GETTARGETMASK(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETOP(arrayOf(INT u S, COMPONENT u S), arrayOf(STRING u S)),
        IF_GETOPBASE(arrayOf(COMPONENT u S), arrayOf(STRING u S)),

        _3100(arrayOf(INT u S)),
        _3101(arrayOf(INT u S, INT u S)),
        _3103(),
        _3104(arrayOf(STRING u S)),
        _3105(arrayOf(STRING u S)),
        _3106(arrayOf(STRING u S)),
        _3107(arrayOf(INT u S, STRING u S)),
        _3108(arrayOf(COMPONENT u S, INT u S, INT u S)),
        _3109(arrayOf(INT u S, INT u S)),
        _3110(arrayOf(INT u S)),
        _3111(defs = arrayOf(BOOLEAN u S)),
        _3112(arrayOf(BOOLEAN u S)),
        _3113(arrayOf(STRING u S, BOOLEAN u S)),
        _3115(arrayOf(INT u S)),
        _3116(arrayOf(INT u S, STRING u S, STRING u S)),
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
        _3137(defs = arrayOf(BOOLEAN u S)),
        _3138(arrayOf(BOOLEAN u S)),
        _3139(defs = arrayOf(BOOLEAN u S)),

        _3200(arrayOf(INT u S, INT u S, INT u S)),
        _3201(arrayOf(INT u S)),
        _3202(arrayOf(INT u S, INT u S)),

        _3300(defs = arrayOf(INT u S)),
        _3301(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3302(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3303(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3304(arrayOf(INT u S), arrayOf(INT u S)),
        _3305(arrayOf(INT u S), arrayOf(INT u S)),
        _3306(arrayOf(INT u S), arrayOf(INT u S)),
        _3307(arrayOf(INT u S), arrayOf(INT u S)),
        _3308(defs = arrayOf(INT u S)),
        _3309(arrayOf(INT u S), arrayOf(INT u S)),
        _3310(arrayOf(INT u S), arrayOf(INT u S)),
        _3311(arrayOf(INT u S), arrayOf(INT u S)),
        _3312(defs = arrayOf(BOOLEAN u S)),
        _3313(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3314(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3315(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3316(defs = arrayOf(INT u S)),
        _3317(defs = arrayOf(INT u S)),
        _3318(defs = arrayOf(INT u S)),
        _3321(defs = arrayOf(INT u S)),
        _3322(defs = arrayOf(INT u S)),
        _3323(defs = arrayOf(BOOLEAN u S)),
        _3324(defs = arrayOf(INT u S)),
        _3325(arrayOf(INT u S, INT u S, INT u S, INT u S), arrayOf(INT u S)),

        _3400(arrayOf(INT u S, INT u S), arrayOf(STRING u S)),
        _3411(arrayOf(INT u S), arrayOf(INT u S)),

        _3600(defs = arrayOf(INT u S)),
        _3601(arrayOf(INT u S), arrayOf(STRING u S, STRING u S)),
        _3602(arrayOf(INT u S), arrayOf(INT u S)),
        _3603(arrayOf(INT u S), arrayOf(INT u S)),
        _3604(arrayOf(STRING u S), arrayOf(INT u S)),
        _3605(arrayOf(STRING u S)),
        _3606(arrayOf(STRING u S)),
        _3607(arrayOf(STRING u S)),
        _3608(arrayOf(STRING u S)),
        _3609(arrayOf(STRING u S), arrayOf(INT u S)),
        _3611(defs = arrayOf(STRING u S)),
        _3612(defs = arrayOf(INT u S)),
        _3614(arrayOf(INT u S), arrayOf(STRING u S)),
        _3615(arrayOf(INT u S), arrayOf(INT u S)),
        _3616(defs = arrayOf(INT u S)),
        _3617(arrayOf(STRING u S)),
        _3618(defs = arrayOf(INT u S)),
        _3619(arrayOf(STRING u S)),
        _3620(),
        _3621(defs = arrayOf(INT u S)),
        _3622(arrayOf(INT u S), arrayOf(STRING u S, STRING u S)),
        _3623(arrayOf(STRING u S), arrayOf(INT u S)),
        _3624(arrayOf(INT u S), arrayOf(INT u S)),
        _3625(defs = arrayOf(STRING u S)),
        _3626(arrayOf(INT u S), arrayOf(INT u S)),
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
        APPEND_SIGNUM(arrayOf(INT u S, STRING u S), arrayOf(STRING u S)),
        LOWERCASE(arrayOf(STRING u S), arrayOf(STRING u S)),
        FROMDATE(arrayOf(INT u S), arrayOf(STRING u S)),
        TEXT_GENDER(arrayOf(STRING u S, STRING u S), arrayOf(STRING u S)),
        TOSTRING(arrayOf(INT u S), arrayOf(STRING u S)),
        COMPARE(arrayOf(INT u S, STRING u S, STRING u S), arrayOf(INT u S)),
        PARAHEIGHT(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        PARAWIDTH(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
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
        STRING_INDEXOF_STRING(arrayOf(STRING u S, STRING u S), arrayOf(INT u S)),

        OC_NAME(arrayOf(INT u S), arrayOf(STRING u S)),
        OC_OP(arrayOf(INT u S, INT u S), arrayOf(STRING u S)),
        OC_IOP(arrayOf(INT u S, INT u S), arrayOf(STRING u S)),
        OC_COST(arrayOf(INT u S), arrayOf(INT u S)),
        OC_STACKABLE(arrayOf(INT u S), arrayOf(INT u S)),
        OC_CERT(arrayOf(INT u S), arrayOf(INT u S)),
        OC_UNCERT(arrayOf(INT u S), arrayOf(INT u S)),
        _4207(arrayOf(INT u S), arrayOf(INT u S)),

        _5000(defs = arrayOf(INT u S)),
        _5003(arrayOf(INT u S, INT u S), arrayOf(INT u S, INT u S, INT u S, STRING u S, STRING u S, STRING u S)),
        _5004(arrayOf(INT u S), arrayOf(INT u S, INT u S, INT u S, STRING u S, STRING u S, STRING u S)),
        _5005(defs = arrayOf(INT u S)),
        _5015(defs = arrayOf(STRING u S)),
        _5016(defs = arrayOf(INT u S)),
        _5017(arrayOf(INT u S), arrayOf(INT u S)),
        _5019(arrayOf(INT u S), arrayOf(INT u S)),
        _5022(defs = arrayOf(STRING u S)),

        _5306(defs = arrayOf(INT u S)),
        _5307(arrayOf(INT u S)),
        _5308(defs = arrayOf(INT u S)),
        _5309(arrayOf(INT u S)),

        _6518(defs = arrayOf(INT u S)),
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
                    L -> exprArgs.add(Expr.Var.l(state.intOperand, arg.type))
                    O -> exprArgs.add(state.operand(arg.type))
                }
            }
            val exprDefs = ArrayList<Expr.Var>(defs.size)
            for (def in defs) {
                when (def.src) {
                    S -> exprDefs.add(state.push(def.type))
                    L -> exprDefs.add(Expr.Var.l(state.intOperand, def.type))
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

    private enum class CcIfSetOn() : Op {

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
            var s = state.peekCst(Type.STRING).cst as String
            state.pop(Type.STRING)
            if (s.isNotEmpty() && s.last() == 'Y') {
                val n = state.peekCst(Type.INT).cst as Int
                state.pop(Type.INT)
                repeat(n) {
                    args.add(state.pop(Type.INT))
                }
                s = s.dropLast(1)
            }
            for (c in s) {
                args.add(state.pop(Type.of(c)))
            }
            args.add(state.pop(Type.INT))
            return Insn.Assignment(emptyList(), Expr.Operation(emptyList(), id, args))
        }
    }
}