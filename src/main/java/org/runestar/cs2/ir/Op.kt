package org.runestar.cs2.ir

import org.runestar.cs2.Opcodes
import org.runestar.cs2.Type

interface Op {

    val id: Int

    fun translate(state: Interpreter.State): Insn

    companion object {

        private val map: Map<Int, Op> = run {
            val list = ArrayList<Op>()
            list.add(Switch)
            list.add(Branch)
            list.add(GetEnum)
            list.add(Invoke)
            list.add(Return)
            list.addAll(BranchCompare.values().asList())
            list.addAll(Stack.values().asList())
            list.addAll(CcIfSetOn.values().asList())
            list.associateBy { it.id }
        }

        fun of(id: Int): Op = map.getValue(id)
    }

    object Switch : Op {

        override val id = Opcodes.SWITCH

        override fun translate(state: Interpreter.State): Insn {
            val map = state.script.switches[state.script.intOperands[state.pc]]
            return Insn.Switch(state.pop(Type.INT), map.mapValues { Insn.Label(it.value + 1 + state.pc) })
        }
    }

    object Branch : Op {

        override val id = Opcodes.BRANCH

        override fun translate(state: Interpreter.State): Insn {
            return Insn.Goto(Insn.Label(state.pc + state.script.intOperands[state.pc] + 1))
        }
    }

    object Invoke : Op {

        override val id = Opcodes.GOSUB_WITH_PARAMS

        override fun translate(state: Interpreter.State): Insn {
            val invokeId = state.script.intOperands[state.pc]
            val invoked = Interpreter(state.loader).interpret(invokeId)

            // todo : script invoking itself

            val args = ArrayList<Expr>()
            args.add(Expr.Cst(Type.INT, state.script.intOperands[state.pc]))
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

    object Return : Op {

        override val id = Opcodes.RETURN

        override fun translate(state: Interpreter.State): Insn {
            val args = ArrayList<Expr>()
            repeat(state.intStack.size) {
                args.add(state.pop(Type.INT))
            }
            repeat(state.strStack.size) {
                args.add(state.pop(Type.STRING))
            }
            args.reverse()
            return Insn.Return(Expr.Operation(emptyList(), id, args))
        }
    }

    object GetEnum : Op {

        override val id = Opcodes.ENUM

        override fun translate(state: Interpreter.State): Insn {
            val a = state.pop(Type.INT)
            val b = state.pop(Type.INT)
            val ctype = state.peekCst(Type.INT)
            val c = state.pop(Type.INT)
            val d = state.pop(Type.INT)
            val args = mutableListOf<Expr>(d, c, b, a)
            val ctypeDesc = ctype.cst as Int
            val target = if (ctypeDesc == Type.STRING.desc.toInt()) {
                state.push(Type.STRING)
            } else {
                state.push(Type.INT)
            }
            return Insn.Assignment(listOf(target), Expr.Operation(listOf(target.type), id, args))
        }
    }

    enum class BranchCompare(override val id: Int) : Op {

        BRANCH_NOT(Opcodes.BRANCH_NOT),
        BRANCH_EQUALS(Opcodes.BRANCH_EQUALS),
        BRANCH_LESS_THAN(Opcodes.BRANCH_LESS_THAN),
        BRANCH_GREATER_THAN(Opcodes.BRANCH_GREATER_THAN),
        BRANCH_LESS_THAN_OR_EQUALS(Opcodes.BRANCH_LESS_THAN_OR_EQUALS),
        BRANCH_GREATER_THAN_OR_EQUALS(Opcodes.BRANCH_GREATER_THAN_OR_EQUALS);

        override fun translate(state: Interpreter.State): Insn {
            val pc = state.pc
            val r = state.pop(Type.INT)
            val l = state.pop(Type.INT)
            val expr = Expr.Operation(emptyList(), id, mutableListOf(l, r))
            return Insn.Branch(expr, Insn.Label(pc + state.script.intOperands[pc] + 1))
        }
    }

    enum class Stack(
            override val id: Int,
            val ipop: Int = 0,
            val ipush: Int = 0,
            val spop: Int = 0,
            val spush: Int = 0,
            val iop: IOP = IOP.NONE,
            val sop: SOP = SOP.NONE
    ) : Op {

        PUSH_CONSTANT_INT(Opcodes.PUSH_CONSTANT_INT, ipush = 1, iop = IOP.PUSH),
        PUSH_VAR(Opcodes.PUSH_VAR, ipush = 1, iop = IOP.CONST),
        POP_VAR(Opcodes.POP_VAR, ipop = 1, iop = IOP.CONST),
        PUSH_CONSTANT_STRING(Opcodes.PUSH_CONSTANT_STRING, spush = 1, sop = SOP.PUSH),
        PUSH_VARBIT(Opcodes.PUSH_VARBIT, ipush = 1, iop = IOP.CONST),
        POP_VARBIT(Opcodes.POP_VARBIT, ipop = 1, iop = IOP.CONST),
        PUSH_INT_LOCAL(Opcodes.PUSH_INT_LOCAL, ipush = 1, iop = IOP.GET_INT),
        POP_INT_LOCAL(Opcodes.POP_INT_LOCAL, ipop = 1, iop = IOP.SET_INT),
        PUSH_STRING_LOCAL(Opcodes.PUSH_STRING_LOCAL, spush = 1, iop = IOP.GET_STR),
        POP_STRING_LOCAL(Opcodes.POP_STRING_LOCAL, spop = 1, iop = IOP.SET_STR),
        JOIN_STRING(Opcodes.JOIN_STRING, spush = 1, iop = IOP.SPOPS),
        POP_INT_DISCARD(Opcodes.POP_INT_DISCARD, ipop = 1),
        POP_STRING_DISCARD(Opcodes.POP_STRING_DISCARD, spop = 1),
        _42(Opcodes._42, ipush = 1, iop = IOP.CONST),
        _43(Opcodes._43, ipop = 1, iop = IOP.CONST),
        DEFINE_ARRAY(Opcodes.DEFINE_ARRAY, ipop = 1, iop = IOP.CONST),
        PUSH_ARRAY_INT(Opcodes.PUSH_ARRAY_INT, ipop = 1, ipush = 1, iop = IOP.CONST),
        POP_ARRAY_INT(Opcodes.POP_ARRAY_INT, ipop = 2, iop = IOP.CONST),
        _47(Opcodes._47, spush = 1, iop = IOP.CONST),
        _48(Opcodes._48, spop = 1, iop = IOP.CONST),

        CC_CREATE(Opcodes.CC_CREATE, ipop = 3),
        CC_DELETE(Opcodes.CC_DELETE),
        CC_DELETEALL(Opcodes.CC_DELETEALL, ipop = 1),
        _200(Opcodes._200, ipop = 2, ipush = 1),
        _201(Opcodes._201, ipop = 1, ipush = 1),

        CC_SETPOSITION(Opcodes.CC_SETPOSITION, ipop = 4),
        CC_SETSIZE(Opcodes.CC_SETSIZE, ipop = 4),
        CC_SETHIDE(Opcodes.CC_SETHIDE, ipop = 1),
        _1005(Opcodes._1005, ipop = 1),
        _1006(Opcodes._1006, ipop = 1),

        CC_SETSCROLLPOS(Opcodes.CC_SETSCROLLPOS, ipop = 2),
        CC_SETCOLOUR(Opcodes.CC_SETCOLOUR, ipop = 1),
        CC_SETFILL(Opcodes.CC_SETFILL, ipop = 1),
        CC_SETTRANS(Opcodes.CC_SETTRANS, ipop = 1),
        CC_SETLINEWID(Opcodes.CC_SETLINEWID, ipop = 1),
        CC_SETGRAPHIC(Opcodes.CC_SETGRAPHIC, ipop = 1),
        CC_SET2DANGLE(Opcodes.CC_SET2DANGLE, ipop = 1),
        CC_SETTILING(Opcodes.CC_SETTILING, ipop = 1),
        CC_SETMODEL(Opcodes.CC_SETMODEL, ipop = 1),
        CC_SETMODELANGLE(Opcodes.CC_SETMODELANGLE, ipop = 6),
        CC_SETMODELANIM(Opcodes.CC_SETMODELANIM, ipop = 1),
        CC_SETMODELORTHOG(Opcodes.CC_SETMODELORTHOG, ipop = 1),
        CC_SETTEXT(Opcodes.CC_SETTEXT, spop = 1),
        CC_SETTEXTFONT(Opcodes.CC_SETTEXTFONT, ipop = 1),
        CC_SETTEXTALIGN(Opcodes.CC_SETTEXTALIGN, ipop = 3),
        CC_SETTEXTANTIMACRO(Opcodes.CC_SETTEXTANTIMACRO, ipop = 1),
        CC_SETOUTLINE(Opcodes.CC_SETOUTLINE, ipop = 1),
        CC_SETGRAPHICSHADOW(Opcodes.CC_SETGRAPHICSHADOW, ipop = 1),
        CC_SETVFLIP(Opcodes.CC_SETVFLIP, ipop = 1),
        CC_SETHFLIP(Opcodes.CC_SETHFLIP, ipop = 1),
        CC_SETSCROLLSIZE(Opcodes.CC_SETSCROLLSIZE, ipop = 2),
        _1121(Opcodes._1121),
        _1122(Opcodes._1122, ipop = 1),
        _1123(Opcodes._1123, ipop = 1),
        _1124(Opcodes._1124, ipop = 1),
        _1125(Opcodes._1125, ipop = 1),
        _1126(Opcodes._1126, ipop = 1),
        _1127(Opcodes._1127, ipop = 1),

        CC_SETOBJECT(Opcodes.CC_SETOBJECT, ipop = 2),
        CC_SETNPCHEAD(Opcodes.CC_SETNPCHEAD, ipop = 1),
        CC_SETPLAYERHEAD_SELF(Opcodes.CC_SETPLAYERHEAD_SELF),
        CC_SETOBJECT_NONUM(Opcodes.CC_SETOBJECT_NONUM, ipop = 2),
        CC_SETOBJECT_ALWAYS_NUM(Opcodes.CC_SETOBJECT_ALWAYS_NUM, ipop = 2),

        CC_SETOP(Opcodes.CC_SETOP, ipop = 1, spop = 1),
        CC_SETDRAGGABLE(Opcodes.CC_SETDRAGGABLE, ipop = 2),
        CC_SETDRAGGABLEBEHAVIOR(Opcodes.CC_SETDRAGGABLEBEHAVIOR, ipop = 1),
        CC_SETDRAGDEADZONE(Opcodes.CC_SETDRAGDEADZONE, ipop = 1),
        CC_SETDRAGDEADTIME(Opcodes.CC_SETDRAGDEADTIME, ipop = 1),
        CC_SETOPBASE(Opcodes.CC_SETOPBASE, spop = 1),
        CC_SETTARGETVERB(Opcodes.CC_SETTARGETVERB, spop = 1),
        CC_CLEAROPS(Opcodes.CC_CLEAROPS),

        CC_GETX(Opcodes.CC_GETX, ipush = 1),
        CC_GETY(Opcodes.CC_GETY, ipush = 1),
        CC_GETWIDTH(Opcodes.CC_GETWIDTH, ipush = 1),
        CC_GETHEIGHT(Opcodes.CC_GETHEIGHT, ipush = 1),
        CC_GETHIDE(Opcodes.CC_GETHIDE, ipush = 1),
        _1505(Opcodes._1505, ipush = 1),

        CC_GETSCROLLX(Opcodes.CC_GETSCROLLX, ipush = 1),
        CC_GETSCROLLY(Opcodes.CC_GETSCROLLY, ipush = 1),
        CC_GETTEXT(Opcodes.CC_GETTEXT, spush = 1),
        CC_GETSCROLLWIDTH(Opcodes.CC_GETSCROLLWIDTH, ipush = 1),
        CC_GETSCROLLHEIGHT(Opcodes.CC_GETSCROLLHEIGHT, ipush = 1),
        CC_GETMODELZOOM(Opcodes.CC_GETMODELZOOM, ipush = 1),
        CC_GETMODELANGLE_X(Opcodes.CC_GETMODELANGLE_X, ipush = 1),
        CC_GETMODELANGLE_Z(Opcodes.CC_GETMODELANGLE_Z, ipush = 1),
        CC_GETMODELANGLE_Y(Opcodes.CC_GETMODELANGLE_Y, ipush = 1),
        CC_GETTRANS(Opcodes.CC_GETTRANS, ipush = 1),
        _1610(Opcodes._1610, ipush = 1),
        _1611(Opcodes._1611, ipush = 1),
        _1612(Opcodes._1612, ipush = 1),
        _1613(Opcodes._1613, ipush = 1),
        _1614(Opcodes._1614, ipush = 1),

        CC_GETINVOBJECT(Opcodes.CC_GETINVOBJECT, ipush = 1),
        CC_GETINVCOUNT(Opcodes.CC_GETINVCOUNT, ipush = 1),
        CC_GETID(Opcodes.CC_GETID, ipush = 1),

        CC_GETTARGETMASK(Opcodes.CC_GETTARGETMASK, ipush = 1),
        CC_GETOP(Opcodes.CC_GETOP, ipop = 1, spush = 1),
        CC_GETOPBASE(Opcodes.CC_GETOPBASE, spush = 1),

        IF_SETPOSITION(Opcodes.IF_SETPOSITION, ipop = 5),
        IF_SETSIZE(Opcodes.IF_SETSIZE, ipop = 5),
        IF_SETHIDE(Opcodes.IF_SETHIDE, ipop = 2),
        _2005(Opcodes._2005, ipop = 2),
        _2006(Opcodes._2006, ipop = 2),

        IF_SETSCROLLPOS(Opcodes.IF_SETSCROLLPOS, ipop = 3),
        IF_SETCOLOUR(Opcodes.IF_SETCOLOUR, ipop = 2),
        IF_SETFILL(Opcodes.IF_SETFILL, ipop = 2),
        IF_SETTRANS(Opcodes.IF_SETTRANS, ipop = 2),
        IF_SETLINEWID(Opcodes.IF_SETLINEWID, ipop = 2),
        IF_SETGRAPHIC(Opcodes.IF_SETGRAPHIC, ipop = 2),
        IF_SET2DANGLE(Opcodes.IF_SET2DANGLE, ipop = 2),
        IF_SETTILING(Opcodes.IF_SETTILING, ipop = 2),
        IF_SETMODEL(Opcodes.IF_SETMODEL, ipop = 2),
        IF_SETMODELANGLE(Opcodes.IF_SETMODELANGLE, ipop = 7),
        IF_SETMODELANIM(Opcodes.IF_SETMODELANIM, ipop = 2),
        IF_SETMODELORTHOG(Opcodes.IF_SETMODELORTHOG, ipop = 2),
        IF_SETTEXT(Opcodes.IF_SETTEXT, spop = 1, ipop = 1),
        IF_SETTEXTFONT(Opcodes.IF_SETTEXTFONT, ipop = 2),
        IF_SETTEXTALIGN(Opcodes.IF_SETTEXTALIGN, ipop = 4),
        IF_SETTEXTANTIMACRO(Opcodes.IF_SETTEXTANTIMACRO, ipop = 2),
        IF_SETOUTLINE(Opcodes.IF_SETOUTLINE, ipop = 2),
        IF_SETGRAPHICSHADOW(Opcodes.IF_SETGRAPHICSHADOW, ipop = 2),
        IF_SETVFLIP(Opcodes.IF_SETVFLIP, ipop = 2),
        IF_SETHFLIP(Opcodes.IF_SETHFLIP, ipop = 2),
        IF_SETSCROLLSIZE(Opcodes.IF_SETSCROLLSIZE, ipop = 3),
        _2121(Opcodes._2121, ipop = 1),
        _2122(Opcodes._2122, ipop = 2),
        _2123(Opcodes._2123, ipop = 2),
        _2124(Opcodes._2124, ipop = 2),
        _2125(Opcodes._2125, ipop = 2),
        _2126(Opcodes._2126, ipop = 2),
        _2127(Opcodes._2127, ipop = 2),

        IF_SETOBJECT(Opcodes.IF_SETOBJECT, ipop = 3),
        IF_SETNPCHEAD(Opcodes.IF_SETNPCHEAD, ipop = 2),
        IF_SETPLAYERHEAD_SELF(Opcodes.IF_SETPLAYERHEAD_SELF, ipop = 1),
        IF_SETOBJECT_NONUM(Opcodes.IF_SETOBJECT_NONUM, ipop = 3),
        IF_SETOBJECT_ALWAYS_NUM(Opcodes.IF_SETOBJECT_ALWAYS_NUM, ipop = 3),

        IF_SETOP(Opcodes.IF_SETOP, ipop = 2, spop = 1),
        IF_SETDRAGGABLE(Opcodes.IF_SETDRAGGABLE, ipop = 3),
        IF_SETDRAGGABLEBEHAVIOR(Opcodes.IF_SETDRAGGABLEBEHAVIOR, ipop = 2),
        IF_SETDRAGDEADZONE(Opcodes.IF_SETDRAGDEADZONE, ipop = 2),
        IF_SETDRAGDEADTIME(Opcodes.IF_SETDRAGDEADTIME, ipop = 2),
        IF_SETOPBASE(Opcodes.IF_SETOPBASE, spop = 1, ipop = 1),
        IF_SETTARGETVERB(Opcodes.IF_SETTARGETVERB, spop = 1, ipop = 1),
        IF_CLEAROPS(Opcodes.IF_CLEAROPS, ipop = 1),

        IF_GETX(Opcodes.IF_GETX, ipop = 1, ipush = 1),
        IF_GETY(Opcodes.IF_GETY, ipop = 1, ipush = 1),
        IF_GETWIDTH(Opcodes.IF_GETWIDTH, ipop = 1, ipush = 1),
        IF_GETHEIGHT(Opcodes.IF_GETHEIGHT, ipop = 1, ipush = 1),
        IF_GETHIDE(Opcodes.IF_GETHIDE, ipop = 1, ipush = 1),
        _2505(Opcodes._2505, ipop = 1, ipush = 1),

        IF_GETSCROLLX(Opcodes.IF_GETSCROLLX, ipop = 1, ipush = 1),
        IF_GETSCROLLY(Opcodes.IF_GETSCROLLY, ipop = 1, ipush = 1),
        IF_GETTEXT(Opcodes.IF_GETTEXT, ipop = 1, spush = 1),
        IF_GETSCROLLWIDTH(Opcodes.IF_GETSCROLLWIDTH, ipop = 1, ipush = 1),
        IF_GETSCROLLHEIGHT(Opcodes.IF_GETSCROLLHEIGHT, ipop = 1, ipush = 1),
        IF_GETMODELZOOM(Opcodes.IF_GETMODELZOOM, ipop = 1, ipush = 1),
        IF_GETMODELANGLE_X(Opcodes.IF_GETMODELANGLE_X, ipop = 1, ipush = 1),
        IF_GETMODELANGLE_Z(Opcodes.IF_GETMODELANGLE_Z, ipop = 1, ipush = 1),
        IF_GETMODELANGLE_Y(Opcodes.IF_GETMODELANGLE_Y, ipop = 1, ipush = 1),
        IF_GETTRANS(Opcodes.IF_GETTRANS, ipop = 1, ipush = 1),
        _2610(Opcodes._2610, ipop = 1, ipush = 1),
        _2611(Opcodes._2611, ipop = 1, ipush = 1),
        _2612(Opcodes._2612, ipop = 1, ipush = 1),
        _2613(Opcodes._2613, ipop = 1, ipush = 1),
        _2614(Opcodes._2614, ipop = 1, ipush = 1),

        IF_GETINVOBJECT(Opcodes.IF_GETINVOBJECT, ipop = 1, ipush = 1),
        IF_GETINVCOUNT(Opcodes.IF_GETINVCOUNT, ipop = 1, ipush = 1),
        IF_GETID(Opcodes.IF_GETID, ipop = 1, ipush = 1),
        _2706(Opcodes._2706, ipush = 1),

        IF_GETTARGETMASK(Opcodes.IF_GETTARGETMASK, ipush = 1, ipop = 1),
        IF_GETOP(Opcodes.IF_GETOP, ipop = 2, spush = 1),
        IF_GETOPBASE(Opcodes.IF_GETOPBASE, spush = 1, ipop = 1),

        _3115(Opcodes._3115, ipop = 1),
        _3116(Opcodes._3116, ipop = 1, spop = 2),
        _3117(Opcodes._3117, ipop = 1),
        _3118(Opcodes._3118, ipop = 1),
        _3119(Opcodes._3119, ipop = 1),
        _3120(Opcodes._3120, ipop = 1),
        _3121(Opcodes._3121, ipop = 1),
        _3122(Opcodes._3122, ipop = 1),
        _3123(Opcodes._3123, ipop = 1),
        _3124(Opcodes._3124),
        _3125(Opcodes._3125, ipop = 1),
        _3126(Opcodes._3126, ipop = 1),
        _3127(Opcodes._3127, ipop = 1),
        _3128(Opcodes._3128, ipush = 1),
        _3129(Opcodes._3129, ipop = 2),
        _3130(Opcodes._3130, ipop = 2),
        _3131(Opcodes._3131, ipop = 1),
        _3132(Opcodes._3132, ipush = 2),
        _3133(Opcodes._3133, ipop = 1),
        _3134(Opcodes._3134),
        _3135(Opcodes._3135, ipop = 2),
        _3136(Opcodes._3136, ipop = 1),
        _3137(Opcodes._3137, ipush = 1),
        _3138(Opcodes._3138, ipop = 1),
        _3139(Opcodes._3139, ipush = 1),

        _3200(Opcodes._3200, ipop = 3),
        _3201(Opcodes._3201, ipop = 1),
        _3202(Opcodes._3202, ipop = 2),

        _3300(Opcodes._3300, ipush = 1),
        _3301(Opcodes._3301, ipush = 1, ipop = 2),
        _3303(Opcodes._3303, ipop = 2, ipush = 1),
        _3305(Opcodes._3305, ipop = 1, ipush = 1),
        _3306(Opcodes._3306, ipop = 1, ipush = 1),
        _3312(Opcodes._3312, ipush = 1),
        _3324(Opcodes._3324, ipush = 1),
        _3411(Opcodes._3411, ipop = 1, ipush = 1),

        ADD(Opcodes.ADD, ipop = 2, ipush = 1),
        SUB(Opcodes.SUB, ipop = 2, ipush = 1),
        MULTIPLY(Opcodes.MULTIPLY, ipop = 2, ipush = 1),
        DIV(Opcodes.DIV, ipop = 2, ipush = 1),
        RANDOM(Opcodes.RANDOM, ipop = 1, ipush = 1),
        RANDOMINC(Opcodes.RANDOMINC, ipop = 1, ipush = 1),
        INTERPOLATE(Opcodes.INTERPOLATE, ipop = 5, ipush = 1),
        ADDPERCENT(Opcodes.ADDPERCENT, ipop = 2, ipush = 1),
        SETBIT(Opcodes.SETBIT, ipop = 2, ipush = 1),
        CLEARBIT(Opcodes.CLEARBIT, ipop = 2, ipush = 1),
        TESTBIT(Opcodes.TESTBIT, ipop = 2, ipush = 1),
        MOD(Opcodes.MOD, ipop = 2, ipush = 1),
        POW(Opcodes.POW, ipop = 2, ipush = 1),
        INVPOW(Opcodes.INVPOW, ipop = 2, ipush = 1),
        AND(Opcodes.AND, ipop = 2, ipush = 1),
        OR(Opcodes.OR, ipop = 2, ipush = 1),
        SCALE(Opcodes.SCALE, ipop = 3, ipush = 1),
        APPEND_NUM(Opcodes.APPEND_NUM, ipop = 1, spop = 1, spush = 1),
        APPEND(Opcodes.APPEND, spop = 2, spush = 1),
        APPEND_SIGNUM(Opcodes.APPEND_SIGNUM, ipop = 1, spop = 1, spush = 1),
        LOWERCASE(Opcodes.LOWERCASE, spop = 1, spush = 1),
        FROMDATE(Opcodes.FROMDATE, ipop = 1, spush = 1),
        TEXT_GENDER(Opcodes.TEXT_GENDER, spop = 2, spush = 1),
        TOSTRING(Opcodes.TOSTRING, ipop = 1, spush = 1),
        COMPARE(Opcodes.COMPARE, ipop = 1, spop = 2, ipush = 1),
        PARAHEIGHT(Opcodes.PARAHEIGHT, ipop = 2, ipush = 1),
        PARAWIDTH(Opcodes.PARAWIDTH, ipop = 2, ipush = 1),
        TEXT_SWITCH(Opcodes.TEXT_SWITCH, ipop = 1, spop = 2, spush = 1),
        ESCAPE(Opcodes.ESCAPE, spop = 1, spush = 1),
        APPEND_CHAR(Opcodes.APPEND_CHAR, ipop = 1, spop = 1, spush = 1),
        CHAR_ISPRINTABLE(Opcodes.CHAR_ISPRINTABLE, ipop = 1, ipush = 1),
        CHAR_ISALPHANUMERIC(Opcodes.CHAR_ISALPHANUMERIC, ipop = 1, ipush = 1),
        CHAR_ISALPHA(Opcodes.CHAR_ISALPHA, ipop = 1, ipush = 1),
        CHAR_ISNUMERIC(Opcodes.CHAR_ISNUMERIC, ipop = 1, ipush = 1),
        STRING_LENGTH(Opcodes.STRING_LENGTH, spop = 1, ipush = 1),
        SUBSTRING(Opcodes.SUBSTRING, spop = 1, ipop = 2, spush = 1),
        REMOVETAGS(Opcodes.REMOVETAGS, spop = 1, spush = 1),
        STRING_INDEXOF_CHAR(Opcodes.STRING_INDEXOF_CHAR, spop = 1, ipop = 1, ipush = 1),
        STRING_INDEXOF_STRING(Opcodes.STRING_INDEXOF_STRING, spop = 2, ipush = 1),
        OC_NAME(Opcodes.OC_NAME, ipop = 1, spush = 1),
        OC_OP(Opcodes.OC_OP, ipop = 2, spush = 1),
        OC_IOP(Opcodes.OC_IOP, ipop = 2, spush = 1),
        OC_COST(Opcodes.OC_COST, ipop = 1, ipush = 1),
        OC_STACKABLE(Opcodes.OC_STACKABLE, ipop = 1, ipush = 1),
        OC_CERT(Opcodes.OC_CERT, ipop = 1, ipush = 1),
        OC_UNCERT(Opcodes.OC_UNCERT, ipop = 1, ipush = 1),
        _4207(Opcodes._4207, ipop = 1, ipush = 1),

        _5000(Opcodes._5000, ipush = 1),
        _5003(Opcodes._5003, ipop = 2, ipush = 3, spush = 3),
        _5004(Opcodes._5004, ipop = 1, ipush = 3, spush = 3),
        _5005(Opcodes._5005, ipush = 1),
        _5015(Opcodes._5015, spush = 1),
        _5016(Opcodes._5016, ipush = 1),
        _5017(Opcodes._5017, ipop = 1, ipush = 1),
        _5019(Opcodes._5019, ipop = 1, ipush = 1),
        _5022(Opcodes._5022, spush = 1),

        _5306(Opcodes._5306, ipush = 1),
        _5307(Opcodes._5307, ipop = 1),
        _5308(Opcodes._5308, ipush = 1),
        _5309(Opcodes._5309, ipop = 1),

        _6518(Opcodes._6518, ipush = 1);

        enum class IOP {
            NONE, CONST, GET_INT, GET_STR, SET_INT, SET_STR, SPOPS, PUSH
        }

        enum class SOP {
            NONE, PUSH
        }

        override fun translate(state: Interpreter.State): Insn {
            val script = state.script
            val intOperand = script.intOperands[state.pc]
            val strOperand = script.stringOperands[state.pc]
            val args = ArrayList<Expr>()
            repeat(ipop) {
                args.add(state.pop(Type.INT))
            }
            repeat(spop) {
                args.add(state.pop(Type.STRING))
            }
            when (iop) {
                IOP.GET_INT -> args.add(Expr.Var.li(intOperand))
                IOP.GET_STR -> args.add(Expr.Var.ls(intOperand))
                IOP.CONST, IOP.PUSH -> args.add(Expr.Cst(Type.INT, intOperand))
                IOP.SPOPS -> {
                    repeat(intOperand) {
                        args.add(state.pop(Type.STRING))
                    }
                }
                else -> {}
            }
            if (sop == SOP.PUSH) {
                args.add(Expr.Cst(Type.STRING, strOperand))
            }

            val targets = ArrayList<Expr.Var>()
            repeat(ipush) {
                targets.add(state.push(Type.INT))
            }
            repeat(spush) {
                targets.add(state.push(Type.STRING))
            }
            when (iop) {
                IOP.SET_INT -> targets.add(Expr.Var.li(intOperand))
                IOP.SET_STR -> targets.add(Expr.Var.ls(intOperand))
                IOP.PUSH -> {
                    state.intStack.pop()
                    state.intStack.push(Expr.Cst(Type.INT, intOperand))
                }
                else -> {}
            }
            if (sop == SOP.PUSH) {
                state.strStack.pop()
                state.strStack.push(Expr.Cst(Type.STRING, strOperand))
            }
            args.reverse()
            return Insn.Assignment(targets, Expr.Operation(targets.map { it.type }, id, args))
        }
    }

    enum class CcIfSetOn(override val id: Int) : Op {

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