package de.tu_darmstadt.sse.appinstrumentation.transformer

import de.tu_darmstadt.sse.appinstrumentation.UtilInstrumenter
import soot.*
import soot.Unit
import soot.jimple.IfStmt
import soot.jimple.IntConstant
import soot.jimple.Jimple
import java.util.*


class ConditionTracking : AbstractInstrumentationTransformer() {

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
        val sootClass = Scene.v().getSootClass(
                UtilInstrumenter.JAVA_CLASS_FOR_PATH_INSTRUMENTATION)

        // Create the method invocation
        val createAndAdd = sootClass.getMethod("reportConditionOutcomeSynchronous",
                listOf<Type>(BooleanType.v()))
        val sieThen = Jimple.v().newStaticInvokeExpr(
                createAndAdd.makeRef(), IntConstant.v(1))
        val sieElse = Jimple.v().newStaticInvokeExpr(
                createAndAdd.makeRef(), IntConstant.v(0))
        val sieThenUnit = Jimple.v().newInvokeStmt(sieThen)
        sieThenUnit.addTag(InstrumentedCodeTag)
        val sieElseUnit = Jimple.v().newInvokeStmt(sieElse)
        sieElseUnit.addTag(InstrumentedCodeTag)

        //treatment of target statement ("true"-branch)
        val ifStmt = unit as IfStmt
        val targetStmt = ifStmt.target
        if (!branchTargetStmt.contains(targetStmt.toString())) {
            branchTargetStmt.add(sieThenUnit.toString())
            body.units.insertBefore(sieThenUnit, targetStmt)

            val nop = Jimple.v().newNopStmt()
            val gotoNop = Jimple.v().newGotoStmt(nop)
            body.units.insertBeforeNoRedirect(nop, targetStmt)
            body.units.insertBeforeNoRedirect(gotoNop, sieThenUnit)
        }


        //treatment of "else"-branch
        body.units.insertAfter(sieElseUnit, unit)
    }

}
