package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.InstrumenterUtil
import soot.Scene
import soot.SceneTransformer
import soot.Type
import soot.VoidType
import soot.jimple.IdentityStmt
import soot.jimple.Jimple


class CrashReporterInjection(private val methodsToInstrument: Set<String>) : SceneTransformer() {

    override fun internalTransform(phaseName: String, options: Map<String, String>) {
        val ref = Scene.v().makeMethodRef(
                Scene.v().getSootClass(InstrumenterUtil.JAVA_CLASS_FOR_CRASH_REPORTING),
                "registerExceptionHandler",
                emptyList<Type>(),
                VoidType.v(),
                true)

        for (sig in methodsToInstrument) {
            try {
                val sm = Scene.v().grabMethod(sig) ?: continue

                val unitIt = sm.activeBody.units.snapshotIterator()
                while (unitIt.hasNext()) {
                    val curUnit = unitIt.next()

                    // If we're still inside the IdentityStmt block, there's nothing to instrument
                    if (curUnit is IdentityStmt)
                        continue

                    // Put the registration in
                    val stmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(ref))
                    stmt.addTag(InstrumentedCodeTag)
                    sm.activeBody.units.insertAfter(stmt, curUnit)
                    break
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

}
