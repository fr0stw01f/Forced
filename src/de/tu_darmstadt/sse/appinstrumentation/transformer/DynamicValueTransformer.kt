package de.tu_darmstadt.sse.appinstrumentation.transformer

import de.tu_darmstadt.sse.appinstrumentation.UtilInstrumenter
import de.tu_darmstadt.sse.dynamiccfg.utils.FileUtils
import soot.*
import soot.javaToJimple.LocalGenerator
import soot.jimple.*
import java.io.File
import java.util.*


class DynamicValueTransformer(instrumentOnlyComparisons: Boolean) : AbstractInstrumentationTransformer() {

    private var instrumentOnlyComparisons = false

    lateinit private var refString: SootMethodRef
    lateinit private var refInt: SootMethodRef

    init {
        this.instrumentOnlyComparisons = instrumentOnlyComparisons

        // Make references to the tracker methods
        val stringType = RefType.v("java.lang.String")
        run {
            val paramTypeList = ArrayList<Type>()
            paramTypeList.add(stringType)
            paramTypeList.add(IntType.v())
            refString = Scene.v().makeMethodRef(
                    Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_CODE_POSITIONS),
                    "reportDynamicValue",
                    paramTypeList,
                    VoidType.v(),
                    true)
        }
        run {
            val paramTypeList = ArrayList<Type>()
            paramTypeList.add(IntType.v())
            paramTypeList.add(IntType.v())
            refInt = Scene.v().makeMethodRef(
                    Scene.v().getSootClass(UtilInstrumenter.JAVA_CLASS_FOR_CODE_POSITIONS),
                    "reportDynamicValue",
                    paramTypeList,
                    VoidType.v(),
                    true)
        }
    }

    override fun internalTransform(b: Body, phaseName: String,
                                   options: Map<String, String>) {
        // Do not instrument methods in framework classes
        if (!canInstrumentMethod(b.method))
            return

        // Iterate over all statements. For each definition statement that
        // defines a string, report the string to the server.
        val unitIt = b.units.snapshotIterator()
        while (unitIt.hasNext()) {
            val curUnit = unitIt.next()

            // If we're still inside the IdentityStmt block, there's nothing to
            // instrument
            if (curUnit is IdentityStmt ||
                    // If this unit was instrumented by another transformer, there's nothing to instrument
                    curUnit.hasTag(InstrumentedCodeTag.name))
                continue

            if (instrumentOnlyComparisons) {
                // Is this a comparison?
                val curStmt = curUnit as Stmt
                if (!curStmt.containsInvokeExpr())
                    continue
                val invExpr = curStmt.invokeExpr
                if (comparisonSignatures.contains(invExpr.method.signature)) {
                    if (invExpr is InstanceInvokeExpr)
                        checkAndReport(b, curStmt, invExpr.base, -1)
                    for (i in 0..invExpr.argCount - 1)
                        checkAndReport(b, curStmt, invExpr.getArg(i), i)
                }

                // Do not look for anything else
                continue
            }

            // We only care about statements that define strings
            if (curUnit !is AssignStmt)
                continue
            val assignStmt = curUnit
            checkAndReport(b, assignStmt, assignStmt.leftOp, -1)
        }

    }

    private fun checkAndReport(b: Body, curStmt: Stmt, value: Value, paramIdx: Int) {
        val localGenerator = LocalGenerator(b)
        val stringType = RefType.v("java.lang.String")
        var lhs = value

        if (lhs is StringConstant)
            return
        else if (lhs is IntConstant)
            return

        // If this is a CharSequence, we need to convert it into a string
        if (lhs.type === RefType.v("java.lang.CharSequence") || lhs.type === RefType.v("java.lang.StringBuilder") && lhs is Local) {
            val toStringRef = Scene.v().getMethod("<java.lang.Object: " + "java.lang.String toString()>").makeRef()
            val stringLocal = localGenerator.generateLocal(stringType)
            val stringAssignStmt = Jimple.v().newAssignStmt(stringLocal,
                    Jimple.v().newVirtualInvokeExpr(lhs as Local, toStringRef))
            stringAssignStmt.addTag(InstrumentedCodeTag)

            b.units.insertBefore(stringAssignStmt, curStmt)
            lhs = stringLocal
        } else if (lhs.type !== IntType.v() && lhs.type !== stringType)
            return

        //new String() case
        if (value is NewExpr)
            return

        // Depending on the type of the value, we might need an intermediate local
        if (lhs !is Local) {
            val newLhs = localGenerator.generateLocal(lhs.type)
            val assignLocalStmt = Jimple.v().newAssignStmt(newLhs, lhs)
            assignLocalStmt.addTag(InstrumentedCodeTag)
            b.units.insertBefore(assignLocalStmt, curStmt)
            lhs = newLhs
        }

        // Report the value
        val reportValueStmt: Stmt
        if (lhs.type === stringType) {
            reportValueStmt = Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(refString, lhs, IntConstant.v(paramIdx)))
        } else if (lhs.type === IntType.v()) {
            reportValueStmt = Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(refInt, lhs, IntConstant.v(paramIdx)))
        } else
            return
        reportValueStmt.addTag(InstrumentedCodeTag)

        b.units.insertBefore(reportValueStmt, curStmt)
    }

    companion object {
        private val DYNAMIC_VALUES_FILENAME = "." + File.separator + "files" + File.separator + "dynamicValueMethods.txt"
        private val comparisonSignatures = FileUtils.textFileToLineSet(DYNAMIC_VALUES_FILENAME)
    }

}
