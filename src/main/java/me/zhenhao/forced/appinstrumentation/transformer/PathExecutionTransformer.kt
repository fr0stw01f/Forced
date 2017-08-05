package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.InstrumenterUtil
import soot.Body
import soot.IdentityUnit
import soot.RefType
import soot.Unit
import soot.jimple.*
import java.util.*


class PathExecutionTransformer : AbstractInstrumentationTransformer() {

    private val branchTargetStmt = HashSet<String>()

    override fun internalTransform(body: Body, phaseName: String, options: Map<String, String>) {
        if (!isInstrumentTarget(body.method))
            return

        instrumentInfoAboutNonAPICall(body)

        // important to use snapshotIterator here
        val iterator = body.units.snapshotIterator()
        while (iterator.hasNext()) {
            val unit = iterator.next()
            if (unit is ReturnStmt || unit is ReturnVoidStmt)
                instrumentInfoAboutReturnStmt(body, unit)
            else if (unit is DefinitionStmt || unit is InvokeStmt)
                instrumentInfoAboutNonApiCaller(body, unit)
            else if (unit is IfStmt)
                instrumentEachBranchAccess(body, unit)
        }
    }


    private fun instrumentInfoAboutNonAPICall(body: Body) {
        val methodSignature = body.method.signature
        val generatedJimpleCode = InstrumenterUtil.makeJimpleStaticCallForPathExecution("logInfoAboutNonApiMethodAccess", RefType.v("java.lang.String"), StringConstant.v(methodSignature))
        generatedJimpleCode.addTag(InstrumentedCodeTag)
        // super-method call has to be the first statement
        if (methodSignature.contains("<init>(") || methodSignature.contains("<clinit>"))
            body.units.insertAfter(generatedJimpleCode, getUnitAfterIdentities(body))
        else
            body.units.insertBefore(generatedJimpleCode, getUnitAfterIdentities(body))
    }


    private fun instrumentInfoAboutReturnStmt(body: Body, unit: Unit) {
        if (unit is ReturnStmt) {
            val returnStmt = unit
            val generated = ArrayList<Unit>()

            val methodSignature = body.method.signature
            val generatedJimpleCode = InstrumenterUtil.makeJimpleStaticCallForPathExecution("logInfoAboutReturnStatement",
                    RefType.v("java.lang.String"), StringConstant.v(methodSignature),
                    RefType.v("java.lang.Object"), InstrumenterUtil.generateCorrectObject(body, returnStmt.op, generated))
            generatedJimpleCode.addTag(InstrumentedCodeTag)
            generated.add(generatedJimpleCode)
            body.units.insertBefore(generated, returnStmt)
        } else if (unit is ReturnVoidStmt) {
            val methodSignature = body.method.signature
            val generatedJimpleCode = InstrumenterUtil.makeJimpleStaticCallForPathExecution("logInfoAboutReturnStatement", RefType.v("java.lang.String"), StringConstant.v(methodSignature))
            generatedJimpleCode.addTag(InstrumentedCodeTag)
            body.units.insertBefore(generatedJimpleCode, unit)
        }
    }


    private fun instrumentInfoAboutNonApiCaller(body: Body, unit: Unit) {
        // our instrumented code
        if (unit.hasTag(InstrumentedCodeTag.name))
            return

        var invokeExpr: InvokeExpr? = null
        if (unit is DefinitionStmt) {
            val defStmt = unit
            if (defStmt.containsInvokeExpr()) {
                invokeExpr = defStmt.invokeExpr
            }
        } else if (unit is InvokeStmt) {
            invokeExpr = unit.invokeExpr
        }

        if (invokeExpr != null) {
            if (!InstrumenterUtil.isApiCall(invokeExpr)) {
                val invokeExprMethodSignature = invokeExpr.method.signature

                val parameter = invokeExpr.args
                val generated = ArrayList<Unit>()
                val arrayRefAndInstrumentation = InstrumenterUtil.generateParameterArray(parameter, body)

                val generatedArrayInstrumentation = arrayRefAndInstrumentation.second
                val arrayRef = arrayRefAndInstrumentation.first

                val generatedInvokeStmt = InstrumenterUtil.makeJimpleStaticCallForPathExecution("logInfoAboutNonApiMethodCaller",
                        RefType.v("java.lang.String"), StringConstant.v(body.method.signature),
                        RefType.v("java.lang.String"), StringConstant.v(invokeExprMethodSignature),
                        InstrumenterUtil.parameterArrayType, if (parameter.isEmpty()) NullConstant.v() else arrayRef)
                generatedInvokeStmt.addTag(InstrumentedCodeTag)
                generated.addAll(generatedArrayInstrumentation)
                generated.add(generatedInvokeStmt)

                body.units.insertBefore(generated, unit)
            }
        }
    }


    private fun instrumentEachBranchAccess(body: Body, ifStmt: IfStmt) {
        val methodSignature = body.method.signature
        val condition = ifStmt.condition.toString()
        val generatedJimpleCodeForBranch = InstrumenterUtil.makeJimpleStaticCallForPathExecution("logInfoAboutBranchAccess",
                RefType.v("java.lang.String"), StringConstant.v(methodSignature),
                RefType.v("java.lang.String"), StringConstant.v(condition),
                RefType.v("java.lang.String"), NullConstant.v()
        )
        generatedJimpleCodeForBranch.addTag(InstrumentedCodeTag)

        val generatedJimpleCodeThenBranch = InstrumenterUtil.makeJimpleStaticCallForPathExecution("logInfoAboutBranchAccess",
                RefType.v("java.lang.String"), StringConstant.v(methodSignature),
                RefType.v("java.lang.String"), NullConstant.v(),
                RefType.v("java.lang.String"), StringConstant.v("then branch")
        )
        generatedJimpleCodeThenBranch.addTag(InstrumentedCodeTag)

        val generatedJimpleCodeElseBranch = InstrumenterUtil.makeJimpleStaticCallForPathExecution("logInfoAboutBranchAccess",
                RefType.v("java.lang.String"), StringConstant.v(methodSignature),
                RefType.v("java.lang.String"), NullConstant.v(),
                RefType.v("java.lang.String"), StringConstant.v("else branch")
        )
        generatedJimpleCodeElseBranch.addTag(InstrumentedCodeTag)

        body.units.insertBefore(generatedJimpleCodeForBranch, ifStmt)

        //treatment of target statement ("true"-branch)
        val targetStmt = ifStmt.target
        if (!branchTargetStmt.contains(targetStmt.toString())) {
            branchTargetStmt.add(generatedJimpleCodeThenBranch.toString())
            body.units.insertBefore(generatedJimpleCodeThenBranch, targetStmt)
        }

        //treatment of "else"-branch
        body.units.insertAfter(generatedJimpleCodeElseBranch, ifStmt)
    }


    private fun getUnitAfterIdentities(body: Body): Unit {
        var u = body.units.first
        while (u is IdentityUnit)
            u = body.units.getSuccOf(u)

        return u
    }

}
