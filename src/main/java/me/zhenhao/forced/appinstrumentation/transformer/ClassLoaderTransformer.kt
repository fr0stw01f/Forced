package me.zhenhao.forced.appinstrumentation.transformer

import soot.Body
import soot.Scene
import soot.Value
import soot.jimple.AssignStmt
import soot.jimple.InstanceInvokeExpr
import soot.jimple.Jimple
import soot.jimple.Stmt
import java.util.*


class ClassLoaderTransformer : AbstractInstrumentationTransformer() {

    private val methodDexFileLoadClass = Scene.v().getMethod(
            "<dalvik.system.DexFile: java.lang.Class loadClass(java.lang.String,java.lang.ClassLoader)>")
    private val methodOwnLoader = Scene.v().getMethod(
            "<me.zhenhao.forced.android.classloading.InterceptingClassLoader: " +
                    "java.lang.Class loadClass(dalvik.system.DexFile,java.lang.String,java.lang.ClassLoader)>")

    override fun internalTransform(b: Body, phaseName: String, options: Map<String, String>) {
        if (!isInstrumentTarget(b.method))
            return

        // Check for calls to DexFile.loadClass
        val unitIt = b.units.snapshotIterator()
        while (unitIt.hasNext()) {
            val stmt = unitIt.next() as Stmt
            if (stmt.hasTag(InstrumentedCodeTag.name))
                continue
            if (stmt !is AssignStmt)
                continue

            if (stmt.containsInvokeExpr()) {
                val iExpr = stmt.getInvokeExpr()
                if (iExpr.method === methodDexFileLoadClass) {
                    val args = ArrayList<Value>()
                    args.add((iExpr as InstanceInvokeExpr).base)
                    args.addAll(iExpr.getArgs())
                    val newLoadExpr = Jimple.v().newStaticInvokeExpr(methodOwnLoader.makeRef(), args)
                    b.units.swapWith(stmt, Jimple.v().newAssignStmt(stmt.leftOp, newLoadExpr))
                }
            }
        }
    }

}
