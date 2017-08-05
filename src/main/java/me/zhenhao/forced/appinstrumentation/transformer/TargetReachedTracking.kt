package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.InstrumenterUtil
import soot.*
import soot.Unit
import soot.jimple.Jimple
import soot.jimple.Stmt

class TargetReachedTracking(private val targetSignatures: Set<Unit>) : AbstractInstrumentationTransformer() {

    override fun internalTransform(b: Body, phaseName: String, options: Map<String, String>) {
        if (!isInstrumentTarget(b.method))
            return

        // Create method references
        val targetReachedRef = Scene.v().makeMethodRef(
                Scene.v().getSootClass(InstrumenterUtil.JAVA_CLASS_FOR_INSTRUMENTATION),
                "reportTargetReachedSynchronous",
                emptyList<Type>(),
                VoidType.v(),
                true)

        // Iterate over the method and find calls to the target methods
        val unitIt = b.units.snapshotIterator()
        while (unitIt.hasNext()) {
            val stmt = unitIt.next() as Stmt

            if (targetSignatures.contains(stmt)) {
                // Notify the server that the target was reached
                val reachedStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(targetReachedRef))
                reachedStmt.addTag(InstrumentedCodeTag)
                b.units.insertBefore(reachedStmt, stmt)
            }
        }
    }

}
