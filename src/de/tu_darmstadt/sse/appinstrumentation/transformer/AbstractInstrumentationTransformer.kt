package de.tu_darmstadt.sse.appinstrumentation.transformer

import de.tu_darmstadt.sse.appinstrumentation.UtilInstrumenter
import soot.BodyTransformer
import soot.Scene
import soot.SootMethod


abstract class AbstractInstrumentationTransformer : BodyTransformer() {

    protected fun canInstrumentMethod(sm: SootMethod): Boolean {
        // Check whether this is actually user code
        val sClass = sm.declaringClass
        if (!UtilInstrumenter.isAppDeveloperCode(sClass))
            return false

        // We do not instrument the dummy main method
        return !Scene.v().entryPoints.contains(sm)
    }

}
