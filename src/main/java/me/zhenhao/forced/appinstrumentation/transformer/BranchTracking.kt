package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import soot.*
import soot.jimple.IfStmt
import soot.jimple.IntConstant
import soot.jimple.Jimple
import java.util.*


class BranchTracking : AbstractInstrumentationTransformer() {

    private val branchTargetStmt = HashSet<String>()

    private var branchId: Int = 0

    override fun internalTransform(body: Body, phaseName: String, options: Map<String, String>) {
        if (!isInstrumentTarget(body.method))
            return

        //important to use snapshotIterator here
        val iterator = body.units.snapshotIterator()

        while (iterator.hasNext()) {
            val unit = iterator.next()

            if (unit is IfStmt && !unit.hasTag(InstrumentedCodeTag.name)) {
                instrumentEachBranchAccess(body, unit)
            }
        }

    }

    private fun instrumentEachBranchAccess(body: Body, ifStmt: IfStmt) {
        val sootClass = Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_INSTRUMENTATION)

        val reportCondition = sootClass.getMethod("reportConditionSynchronous", listOf<Type>(IntType.v()))
        val sieIfLogExpr = Jimple.v().newStaticInvokeExpr(reportCondition.makeRef(), IntConstant.v(branchId))
        val sieIfLogStmt = Jimple.v().newInvokeStmt(sieIfLogExpr)
        sieIfLogStmt.addTag(InstrumentedCodeTag)

        body.units.insertBefore(sieIfLogStmt, ifStmt)

        val reportConditionOutcome = sootClass.getMethod("reportConditionOutcomeSynchronous",
                arrayListOf<Type>(IntType.v(), BooleanType.v()))

        // treatment of target statement ("true"-branch)
        val sieThenExpr = Jimple.v().newStaticInvokeExpr(reportConditionOutcome.makeRef(),
                IntConstant.v(branchId), IntConstant.v(1)) //then -> true
        val sieThenStmt = Jimple.v().newInvokeStmt(sieThenExpr)
        sieThenStmt.addTag(InstrumentedCodeTag)

        val targetStmt = ifStmt.target
        if (!branchTargetStmt.contains(targetStmt.toString())) {
            branchTargetStmt.add(sieThenStmt.toString())
            body.units.insertBefore(sieThenStmt, targetStmt)

            val nop = Jimple.v().newNopStmt()
            val gotoNop = Jimple.v().newGotoStmt(nop)
            body.units.insertBeforeNoRedirect(nop, targetStmt)
            body.units.insertBeforeNoRedirect(gotoNop, sieThenStmt)
        }


        // treatment of "else"-branch
        val sieElseExpr = Jimple.v().newStaticInvokeExpr(reportConditionOutcome.makeRef(),
                IntConstant.v(branchId), IntConstant.v(0)) //else -> false
        val sieElseStmt = Jimple.v().newInvokeStmt(sieElseExpr)
        sieElseStmt.addTag(InstrumentedCodeTag)

        body.units.insertAfter(sieElseStmt, ifStmt)

        branchId += 1
    }

     fun getBranchId(): Int {
        return branchId
    }

}
