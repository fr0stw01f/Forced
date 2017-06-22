package de.tu_darmstadt.sse.decisionmaker.analysis.dynamicValues

import java.util.ArrayList
import java.util.Collections
import java.util.HashSet

import soot.IntType
import soot.RefType
import soot.Type
import soot.Unit
import soot.jimple.Stmt
import de.tu_darmstadt.sse.apkspecific.CodeModel.CodePositionManager
import de.tu_darmstadt.sse.decisionmaker.analysis.AnalysisDecision
import de.tu_darmstadt.sse.decisionmaker.analysis.FuzzyAnalysis
import de.tu_darmstadt.sse.decisionmaker.server.ThreadTraceManager
import de.tu_darmstadt.sse.decisionmaker.server.TraceManager
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest
import de.tu_darmstadt.sse.sharedclasses.networkconnection.ServerResponse
import de.tu_darmstadt.sse.sharedclasses.util.Pair


class DynamicValueAnalysis : FuzzyAnalysis() {

    private val codePositionManager = CodePositionManager.codePositionManagerInstance

    override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {
        // nothing to do here
    }

    override fun resolveRequest(clientRequest: DecisionRequest,
                                completeHistory: ThreadTraceManager): List<AnalysisDecision> {
        val s = codePositionManager.getUnitForCodePosition(clientRequest.codePosition + 1) as Stmt
        if (!s.containsInvokeExpr())
            return emptyList()

        val stringType = RefType.v("java.lang.String")

        // Return the dynamically-obtained values
        val runtimeValues = completeHistory.getNewestClientHistory()?.dynamicValues?.getValues() ?: return emptyList()
        val decisions = ArrayList<AnalysisDecision>(runtimeValues.size)
        for (value in runtimeValues) {
            val serverResponse = ServerResponse()
            serverResponse.analysisName = getAnalysisName()
            serverResponse.setResponseExist(true)

            val returnType = s.invokeExpr.method.returnType
            if (clientRequest.isHookAfter && isSupported(returnType)) {
                serverResponse.returnValue = checkAndGet(returnType, value)
            } else {
                val paramValues = HashSet<Pair<Int, Any>>()
                for (i in 0..s.invokeExpr.argCount - 1) {
                    val paramType = s.invokeExpr.method.getParameterType(i)
                    if (paramType === stringType) {
                        val newParamVal = checkAndGet(paramType, value)
                        if (newParamVal != null)
                            paramValues.add(Pair(i, newParamVal))
                    }
                }
                serverResponse.paramValues = paramValues
            }

            val decision = AnalysisDecision()
            decision.analysisName = getAnalysisName()
            decision.serverResponse = serverResponse
            decision.decisionWeight = 5
            decisions.add(decision)
        }
        return decisions
    }


    private fun checkAndGet(tp: Type, value: DynamicValue): Any? {
        if (tp === IntType.v() && value is DynamicIntValue)
            return value.intValue
        else if (tp === RefType.v("java.lang.String") && value is DynamicStringValue)
            return value.stringValue
        else
            return null
    }


    private fun isSupported(returnType: Type): Boolean {
        return returnType === RefType.v("java.lang.String") || returnType === IntType.v()
    }

    override fun reset() {
        // nothing to do here
    }

    override fun getAnalysisName(): String {
        return "DynamicValues"
    }

}
