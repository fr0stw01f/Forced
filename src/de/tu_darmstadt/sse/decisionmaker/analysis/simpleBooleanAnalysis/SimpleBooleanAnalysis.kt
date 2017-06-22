package de.tu_darmstadt.sse.decisionmaker.analysis.simpleBooleanAnalysis

import java.util.ArrayList
import java.util.HashSet

import soot.Unit
import de.tu_darmstadt.sse.decisionmaker.analysis.AnalysisDecision
import de.tu_darmstadt.sse.decisionmaker.analysis.FuzzyAnalysis
import de.tu_darmstadt.sse.decisionmaker.server.ThreadTraceManager
import de.tu_darmstadt.sse.decisionmaker.server.TraceManager
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest
import de.tu_darmstadt.sse.sharedclasses.networkconnection.ServerResponse


class SimpleBooleanAnalysis : FuzzyAnalysis() {

    private val seenCodePositions = HashSet<Int>()

    override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {
        //No pre-analysis required
    }

    override fun resolveRequest(clientRequest: DecisionRequest,
                                completeHistory: ThreadTraceManager): List<AnalysisDecision> {
        val decisions = ArrayList<AnalysisDecision>()

        if (seenCodePositions.contains(clientRequest.codePosition))
            return ArrayList()

        if (clientRequest.loggingPointSignature == "<android.app.SharedPreferencesImpl: boolean getBoolean(java.lang.String, boolean)>"
                || clientRequest.loggingPointSignature == "<android.app.admin.DevicePolicyManager: boolean isAdminActive(android.content.ComponentName)>") {
            seenCodePositions.add(clientRequest.codePosition)
            val responseTrue = ServerResponse()
            responseTrue.analysisName = getAnalysisName()
            responseTrue.setResponseExist(true)
            responseTrue.returnValue = true
            val finalDecisionTrue = AnalysisDecision()
            finalDecisionTrue.analysisName = getAnalysisName()
            finalDecisionTrue.decisionWeight = 5
            finalDecisionTrue.serverResponse = responseTrue
            val responseFalse = ServerResponse()
            responseFalse.analysisName = getAnalysisName()
            responseFalse.setResponseExist(true)
            responseFalse.returnValue = false
            val finalDecisionFalse = AnalysisDecision()
            finalDecisionFalse.analysisName = getAnalysisName()
            finalDecisionFalse.decisionWeight = 5
            finalDecisionFalse.serverResponse = responseFalse

            decisions.add(finalDecisionTrue)
            decisions.add(finalDecisionFalse)
            return decisions
        }

        return ArrayList()
    }

    override fun reset() {
        // no reset requried
    }

    override fun getAnalysisName(): String {
        return "SimpleBooleanAnalysis"
    }

}
