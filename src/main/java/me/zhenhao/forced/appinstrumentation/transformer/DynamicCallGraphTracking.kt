package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.apkspecific.CodeModel.CodePosition
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import soot.*
import soot.jimple.*


class DynamicCallGraphTracking(private val codePositionManager: CodePositionManager) : AbstractInstrumentationTransformer() {

    override fun internalTransform(b: Body, phaseName: String, options: Map<String, String>) {
        if (!isInstrumentTarget(b.method))
            return

        // Create method references
        val callMethodRef = Scene.v().makeMethodRef(
                Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_INSTRUMENTATION),
                "reportMethodCallSynchronous",
                listOf<Type>(IntType.v()),
                VoidType.v(),
                true)
        val returnMethodRef = Scene.v().makeMethodRef(
                Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_INSTRUMENTATION),
                "reportMethodReturnSynchronous",
                listOf<Type>(IntType.v()),
                VoidType.v(),
                true)
        val enterMethodRef = Scene.v().makeMethodRef(
                Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_INSTRUMENTATION),
                "reportMethodEnterSynchronous",
                listOf<Type>(IntType.v()),
                VoidType.v(),
                true)
        val leaveMethodRef = Scene.v().makeMethodRef(
                Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_INSTRUMENTATION),
                "reportMethodLeaveSynchronous",
                listOf<Type>(IntType.v()),
                VoidType.v(),
                true)

        var lineNum = 0
        var started = true
        var firstNonIdentity = false
        val unitIt = b.units.snapshotIterator()
        while (unitIt.hasNext()) {
            val stmt = unitIt.next() as Stmt
            var codePos: CodePosition? = null

            // Do not record trace data on system-generated code
            if (stmt.hasTag(InstrumentedCodeTag.name))
                continue

            // Is this the first non-identity statement in the method
            if (stmt !is IdentityStmt) {
                firstNonIdentity = started
                started = false
            }

            // Does the control flow leave the current method at the current statement?
            val stmtLeavesMethod = stmt is ReturnStmt
                    || stmt is ReturnVoidStmt
                    || stmt is ThrowStmt

            // Get the current code position
            if (stmt.containsInvokeExpr()
                    || firstNonIdentity
                    || stmtLeavesMethod) {
                codePos = codePositionManager.getCodePositionForUnit(stmt, b.method.signature, lineNum,
                        stmt.javaSourceStartLineNumber)
            }

            // Record method enters
            if (firstNonIdentity) {
                val onEnterStmt = Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(enterMethodRef, IntConstant.v(codePos!!.id)))
                onEnterStmt.addTag(InstrumentedCodeTag)
                b.units.insertBefore(onEnterStmt, stmt)
            }

            // Check for method calls
            if (stmt.containsInvokeExpr()) {
                val onCallStmt = Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(callMethodRef, IntConstant.v(codePos!!.id)))
                onCallStmt.addTag(InstrumentedCodeTag)
                b.units.insertBefore(onCallStmt, stmt)

                val onReturnStmt = Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(returnMethodRef, IntConstant.v(codePos.id)))
                onReturnStmt.addTag(InstrumentedCodeTag)
                b.units.insertAfter(onReturnStmt, stmt)
            }

            // Record method leaves
            if (stmtLeavesMethod) {
                val onLeaveStmt = Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(leaveMethodRef, IntConstant.v(codePos!!.id)))
                onLeaveStmt.addTag(InstrumentedCodeTag)
                b.units.insertBefore(onLeaveStmt, stmt)
            }

            lineNum++
        }
    }

}
