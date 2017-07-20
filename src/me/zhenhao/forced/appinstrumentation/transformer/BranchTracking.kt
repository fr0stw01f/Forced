package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import soot.*
import soot.Unit
import soot.jimple.IfStmt
import soot.jimple.IntConstant
import soot.jimple.Jimple
import java.util.*


class BranchTracking : AbstractInstrumentationTransformer() {

    private val branchTargetStmt = HashSet<String>()

    override fun internalTransform(body: Body, phaseName: String, options: Map<String, String>) {
        // Do not instrument methods in framework classes
        if (!canInstrumentMethod(body.method))
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

    private fun instrumentEachBranchAccess(body: Body, unit: Unit) {
        val sootClass = Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_PATH_INSTRUMENTATION)

        // Create the method invocation
        //val reportMethod = sootClass.getMethod("reportConditionOutcomeSynchronous", listOf<Type>(BooleanType.v()))
        val reportMethod = sootClass.getMethod("reportConditionOutcome", listOf<Type>(BooleanType.v()))
        val sieThenExpr = Jimple.v().newStaticInvokeExpr(reportMethod.makeRef(), IntConstant.v(1)) //then -> true
        val sieElseExpr = Jimple.v().newStaticInvokeExpr(reportMethod.makeRef(), IntConstant.v(0)) //else -> false
        val sieThenStmt = Jimple.v().newInvokeStmt(sieThenExpr)
        sieThenStmt.addTag(InstrumentedCodeTag)
        val sieElseStmt = Jimple.v().newInvokeStmt(sieElseExpr)
        sieElseStmt.addTag(InstrumentedCodeTag)

        //treatment of target statement ("true"-branch)
        val ifStmt = unit as IfStmt
        val targetStmt = ifStmt.target
        if (!branchTargetStmt.contains(targetStmt.toString())) {
            branchTargetStmt.add(sieThenStmt.toString())
            body.units.insertBefore(sieThenStmt, targetStmt)

            val nop = Jimple.v().newNopStmt()
            val gotoNop = Jimple.v().newGotoStmt(nop)
            body.units.insertBeforeNoRedirect(nop, targetStmt)
            body.units.insertBeforeNoRedirect(gotoNop, sieThenStmt)
        }

        //treatment of "else"-branch
        body.units.insertAfter(sieElseStmt, unit)
    }

}
