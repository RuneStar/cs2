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
            val map = state.script.switches[state.script.intOperands[state.pc]]
            return Insn.Switch(state.pop(Type.INT), map.mapValues { Insn.Label(it.value + 1 + state.pc) })
        }
    }

    private object Branch : Op {

        override val id = Opcodes.BRANCH

        override fun translate(state: Interpreter.State): Insn {
            return Insn.Goto(Insn.Label(state.pc + state.script.intOperands[state.pc] + 1))
        }
    }

    private object Invoke : Op {

        override val id = Opcodes.GOSUB_WITH_PARAMS

        override fun translate(state: Interpreter.State): Insn {
            val invokeId = state.script.intOperands[state.pc]
            val invoked = Interpreter(state.loader).interpret(invokeId)

            // todo : script invoking itself

            val args = ArrayList<Expr>()
            args.add(Expr.Cst(INT, state.script.intOperands[state.pc]))
            invoked.args.forEach {
                args.add(state.pop(it.type))
            }

            val returns = ArrayList<Expr.Var>()
            invoked.returns.forEach {
                returns.add(state.push(it))
            }

            return Insn.Assignment(returns, Expr.Operation(invoked.returns, id, args))
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
            val a = state.pop(INT)
            val b = state.pop(INT)
            val ctype = state.peekCst(INT)
            val c = state.pop(INT)
            val d = state.pop(INT)
            val args = mutableListOf<Expr>(d, c, b, a)
            val ctypeDesc = ctype.cst as Int
            val target = if (ctypeDesc == STRING.desc.toInt()) {
                state.push(STRING)
            } else {
                state.push(INT)
            }
            return Insn.Assignment(listOf(target), Expr.Operation(listOf(target.type), id, args))
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
            val pc = state.pc
            val r = state.pop(INT)
            val l = state.pop(INT)
            val expr = Expr.Operation(emptyList(), id, mutableListOf(l, r))
            return Insn.Branch(expr, Insn.Label(pc + state.script.intOperands[pc] + 1))
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
            val operand: Any? = when (type.topType) {
                TopType.INT -> state.script.intOperands[state.pc]
                TopType.STRING -> state.script.stringOperands[state.pc]
            }
            val cst = Expr.Cst(type, operand)
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
        CC_GETHIDE(defs = arrayOf(INT u S)),
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
        _1614(defs = arrayOf(INT u S)),

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
        IF_GETHIDE(arrayOf(COMPONENT u S), arrayOf(INT u S)),
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
        _2614(arrayOf(COMPONENT u S), arrayOf(INT u S)),

        IF_GETINVOBJECT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETINVCOUNT(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETID(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        _2706(defs = arrayOf(INT u S)),

        IF_GETTARGETMASK(arrayOf(COMPONENT u S), arrayOf(INT u S)),
        IF_GETOP(arrayOf(INT u S, COMPONENT u S), arrayOf(STRING u S)),
        IF_GETOPBASE(arrayOf(COMPONENT u S), arrayOf(STRING u S)),

        _3115(arrayOf(INT u S)),
        _3116(arrayOf(INT u S, STRING u S, STRING u S)),
        _3117(arrayOf(INT u S)),
        _3118(arrayOf(INT u S)),
        _3119(arrayOf(INT u S)),
        _3120(arrayOf(INT u S)),
        _3121(arrayOf(INT u S)),
        _3122(arrayOf(INT u S)),
        _3123(arrayOf(INT u S)),
        _3124(),
        _3125(arrayOf(INT u S)),
        _3126(arrayOf(INT u S)),
        _3127(arrayOf(INT u S)),
        _3128(defs = arrayOf(INT u S)),
        _3129(arrayOf(INT u S, INT u S)),
        _3130(arrayOf(INT u S, INT u S)),
        _3131(arrayOf(INT u S)),
        _3132(defs = arrayOf(INT u S, INT u S)),
        _3133(arrayOf(INT u S)),
        _3134(),
        _3135(arrayOf(INT u S, INT u S)),
        _3136(arrayOf(INT u S)),
        _3137(defs = arrayOf(INT u S)),
        _3138(arrayOf(INT u S)),
        _3139(defs = arrayOf(INT u S)),

        _3200(arrayOf(INT u S, INT u S, INT u S)),
        _3201(arrayOf(INT u S)),
        _3202(arrayOf(INT u S, INT u S)),

        _3300(defs = arrayOf(INT u S)),
        _3301(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3303(arrayOf(INT u S, INT u S), arrayOf(INT u S)),
        _3305(arrayOf(INT u S), arrayOf(INT u S)),
        _3306(arrayOf(INT u S), arrayOf(INT u S)),
        _3312(defs = arrayOf(INT u S)),
        _3324(defs = arrayOf(INT u S)),
        _3411(arrayOf(INT u S), arrayOf(INT u S)),

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
            val intOperand = state.script.intOperands[state.pc]
            val strOperand = state.script.stringOperands[state.pc]
            val stackArgs = ListStack<Expr.Var>(ArrayList())
            for (i in args.lastIndex downTo 0) {
                val arg = args[i]
                if (arg.src == S) stackArgs.push(state.pop(arg.type))
            }
            val exprArgs = ArrayList<Expr>(args.size)
            for (arg in args) {
                when (arg.src) {
                    S -> exprArgs.add(stackArgs.pop())
                    L -> exprArgs.add(Expr.Var.l(intOperand, arg.type))
                    O -> {
                        val operand: Any? = when (arg.type.topType) {
                            TopType.STRING -> strOperand
                            TopType.INT -> intOperand
                        }
                        exprArgs.add(Expr.Cst(arg.type, operand))
                    }
                }
            }
            val exprDefs = ArrayList<Expr.Var>(defs.size)
            for (def in defs) {
                when (def.src) {
                    S -> exprDefs.add(state.push(def.type))
                    L -> exprDefs.add(Expr.Var.l(intOperand, def.type))
                    O -> throw IllegalStateException()
                }
            }
            return Insn.Assignment(exprDefs, Expr.Operation(defs.map { it.type }, id, exprArgs))
        }
    }

    private object JoinString : Op {

        override val id = Opcodes.JOIN_STRING

        override fun translate(state: Interpreter.State): Insn {
            val intOperand = state.script.intOperands[state.pc]
            val args = ArrayList<Expr>(intOperand)
            repeat(intOperand) {
                args.add(state.pop(Type.STRING))
            }
            args.reverse()
            return Insn.Assignment(listOf(state.push(Type.STRING)), Expr.Operation(listOf(Type.STRING), id, args))
        }
    }

    private enum class CcIfSetOn(override val id: Int) : Op {

        CC_SETONCLICK(Opcodes.CC_SETONCLICK),
        CC_SETONHOLD(Opcodes.CC_SETONHOLD),
        CC_SETONRELEASE(Opcodes.CC_SETONRELEASE),
        CC_SETONMOUSEOVER(Opcodes.CC_SETONMOUSEOVER),
        CC_SETONMOUSELEAVE(Opcodes.CC_SETONMOUSELEAVE),
        CC_SETONDRAG(Opcodes.CC_SETONDRAG),
        CC_SETONTARGETLEAVE(Opcodes.CC_SETONTARGETLEAVE),
        CC_SETONVARTRANSMIT(Opcodes.CC_SETONVARTRANSMIT),
        CC_SETONTIME(Opcodes.CC_SETONTIME),
        CC_SETONTOP(Opcodes.CC_SETONTOP),
        CC_SETONDRAGCOMPLETE(Opcodes.CC_SETONDRAGCOMPLETE),
        CC_SETONCLICKREPEAT(Opcodes.CC_SETONCLICKREPEAT),
        CC_SETONMOUSEREPEAT(Opcodes.CC_SETONMOUSEREPEAT),
        CC_SETONINVTRANSMIT(Opcodes.CC_SETONINVTRANSMIT),
        CC_SETONSTATTRANSMIT(Opcodes.CC_SETONSTATTRANSMIT),
        CC_SETONTARGETENTER(Opcodes.CC_SETONTARGETENTER),
        CC_SETONSCROLLWHEEL(Opcodes.CC_SETONSCROLLWHEEL),
        CC_SETONCHATTRANSMIT(Opcodes.CC_SETONCHATTRANSMIT),
        CC_SETONKEY(Opcodes.CC_SETONKEY),
        _1420(Opcodes._1420),
        _1421(Opcodes._1421),
        _1422(Opcodes._1422),
        _1423(Opcodes._1423),
        _1424(Opcodes._1424),
        _1425(Opcodes._1425),
        _1426(Opcodes._1426),
        _1427(Opcodes._1427),
        IF_SETONCLICK(Opcodes.IF_SETONCLICK),
        IF_SETONHOLD(Opcodes.IF_SETONHOLD),
        IF_SETONRELEASE(Opcodes.IF_SETONRELEASE),
        IF_SETONMOUSEOVER(Opcodes.IF_SETONMOUSEOVER),
        IF_SETONMOUSELEAVE(Opcodes.IF_SETONMOUSELEAVE),
        IF_SETONDRAG(Opcodes.IF_SETONDRAG),
        IF_SETONTARGETLEAVE(Opcodes.IF_SETONTARGETLEAVE),
        IF_SETONVARTRANSMIT(Opcodes.IF_SETONVARTRANSMIT),
        IF_SETONTIME(Opcodes.IF_SETONTIME),
        IF_SETONTOP(Opcodes.IF_SETONTOP),
        IF_SETONDRAGCOMPLETE(Opcodes.IF_SETONDRAGCOMPLETE),
        IF_SETONCLICKREPEAT(Opcodes.IF_SETONCLICKREPEAT),
        IF_SETONMOUSEREPEAT(Opcodes.IF_SETONMOUSEREPEAT),
        IF_SETONINVTRANSMIT(Opcodes.IF_SETONINVTRANSMIT),
        IF_SETONSTATTRANSMIT(Opcodes.IF_SETONSTATTRANSMIT),
        IF_SETONTARGETENTER(Opcodes.IF_SETONTARGETENTER),
        IF_SETONSCROLLWHEEL(Opcodes.IF_SETONSCROLLWHEEL),
        IF_SETONCHATTRANSMIT(Opcodes.IF_SETONCHATTRANSMIT),
        IF_SETONKEY(Opcodes.IF_SETONKEY),
        _2420(Opcodes._2420),
        _2421(Opcodes._2421),
        _2422(Opcodes._2422),
        _2423(Opcodes._2423),
        _2424(Opcodes._2424),
        _2425(Opcodes._2425),
        _2426(Opcodes._2426),
        _2427(Opcodes._2427);

        override fun translate(state: Interpreter.State): Insn {
            val args = ArrayList<Expr>()
            if (id >= 2000) {
                args.add(state.pop(Type.INT))
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
            var intArgs = 0
            var strArgs = 0
            for (c in s) {
                if (c == 's') {
                    strArgs++
                } else {
                    intArgs++
                }
            }
            repeat(intArgs) {
                args.add(state.pop(Type.INT))
            }
            repeat(strArgs) {
                args.add(state.pop(Type.STRING))
            }
            args.add(state.pop(Type.INT))
            return Insn.Assignment(emptyList(), Expr.Operation(emptyList(), id, args))
        }
    }
}