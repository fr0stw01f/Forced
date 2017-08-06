package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.InstrumentUtil
import soot.Body
import soot.LongType
import soot.Scene
import soot.SootMethodRef
import soot.jimple.*


class TimingBombTransformer : AbstractInstrumentationTransformer() {

    override fun internalTransform(body: Body, phaseName: String, options: Map<String, String>) {
        if (!isInstrumentTarget(body.method))
            return

        // Get a reference to the reporter method
        val reporterRef = Scene.v().getMethod("<me.zhenhao.forced.android.tracing.BytecodeLogger: " +
                "void reportTimingBomb(long,long)>").makeRef()

        val unitIt = body.units.snapshotIterator()
        while (unitIt.hasNext()) {
            val curUnit = unitIt.next()

            if (curUnit is InvokeStmt) {
                val invokeStmt = curUnit
                val expr = invokeStmt.invokeExpr
                val methodSig = expr.method.signature

                if (methodSig == "<android.app.AlarmManager: void set(int,long,android.app.PendingIntent)>")
                    prepareAlarmManagerSet(body, invokeStmt, reporterRef)
                else if (methodSig == "<android.os.Handler: boolean postDelayed(java.lang.Runnable,long)>")
                    prepareHandlerPostDelayed(body, invokeStmt, reporterRef)
            }
        }

    }

    private fun prepareAlarmManagerSet(body: Body, setStmt: InvokeStmt, reportRef: SootMethodRef) {
        val oldVal = setStmt.invokeExpr.getArg(1)

        val longLocal = InstrumentUtil.generateFreshLocal(body, LongType.v())
        val currentTimeMillis = Scene.v().getMethod("<java.lang.System: long currentTimeMillis()>")
        val timeInvoke = Jimple.v().newStaticInvokeExpr(currentTimeMillis.makeRef())
        val timeInitialize = Jimple.v().newAssignStmt(longLocal, timeInvoke)

        val addTime = Jimple.v().newAddExpr(longLocal, LongConstant.v(2000L))
        val timeAssign = Jimple.v().newAssignStmt(longLocal, addTime)

        body.units.insertBefore(timeInitialize, setStmt)
        body.units.insertBefore(timeAssign, setStmt)

        val expr = setStmt.invokeExpr
        expr.setArg(0, IntConstant.v(0))
        expr.setArg(1, longLocal)

        // Report the change
        val reportStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(reportRef, oldVal, longLocal))
        reportStmt.addTag(InstrumentedCodeTag)
        body.units.insertAfter(reportStmt, setStmt)
    }

    private fun prepareHandlerPostDelayed(body: Body, invokeStmt: Stmt, reportRef: SootMethodRef) {
        val expr = invokeStmt.invokeExpr

        val oldValue = expr.getArg(1)
        val newValue = LongConstant.v(2000L)

        expr.setArg(1, newValue)

        // Report the change
        val reportStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(reportRef, oldValue, newValue))
        reportStmt.addTag(InstrumentedCodeTag)
        body.units.insertAfter(reportStmt, invokeStmt)
    }

}
