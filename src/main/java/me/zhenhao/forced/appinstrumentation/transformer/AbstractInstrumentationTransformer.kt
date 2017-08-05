package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import soot.BodyTransformer
import soot.Scene
import soot.SootMethod


abstract class AbstractInstrumentationTransformer : BodyTransformer() {

    protected fun isInstrumentTarget(sm: SootMethod): Boolean {
        // Check whether this is actually user code
        val sClass = sm.declaringClass
        if (!UtilInstrumenter.isAppDeveloperCode(sClass))
            return false

        // We do not instrument the dummy main method
        return !Scene.v().entryPoints.contains(sm)
    }

}
