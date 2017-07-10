package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import soot.*
import soot.jimple.IdentityStmt
import soot.jimple.IntConstant
import soot.jimple.Jimple
import soot.jimple.Stmt


class CodePositionTracking(private val codePositionManager: CodePositionManager) : AbstractInstrumentationTransformer() {

    override fun internalTransform(b: Body, phaseName: String, options: Map<String, String>) {
        // Do not instrument methods in framework classes
        if (!canInstrumentMethod(b.method))
            return

        // Make a reference to the tracker method
        val ref = Scene.v().makeMethodRef(
                Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_CODE_POSITIONS),
                "setLastExecutedStatement",
                listOf<Type>(IntType.v()),
                VoidType.v(),
                true)
        val methodSig = b.method.signature

        // Iterate over all the units and add a unit that sets the current
        // execution pointer
        var curLineNum = 0
        val unitIt = b.units.snapshotIterator()
        while (unitIt.hasNext()) {
            val curUnit = unitIt.next()

            // If we're still inside the IdentityStmt block, there's nothing to
            // instrument
            if (curUnit is IdentityStmt ||
                    // If this unit was instrumented by another transformer, there's nothing to instrument
                    curUnit.hasTag(InstrumentedCodeTag.name))
                continue

            // Get the current code positions
            val codePos = codePositionManager.getCodePositionForUnit(curUnit,
                    methodSig, curLineNum++, (curUnit as Stmt).javaSourceStartLineNumber)

            val setCodePosStmt = Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(ref, IntConstant.v(codePos.id)))
            setCodePosStmt.addTag(InstrumentedCodeTag)

            b.units.insertAfter(setCodePosStmt, curUnit)
        }
    }

}
