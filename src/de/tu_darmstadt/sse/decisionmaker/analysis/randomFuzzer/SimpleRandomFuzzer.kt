package de.tu_darmstadt.sse.decisionmaker.analysis.randomFuzzer

import de.tu_darmstadt.sse.decisionmaker.DeterministicRandom
import de.tu_darmstadt.sse.decisionmaker.analysis.AnalysisDecision
import de.tu_darmstadt.sse.decisionmaker.analysis.FuzzyAnalysis
import de.tu_darmstadt.sse.decisionmaker.server.ThreadTraceManager
import de.tu_darmstadt.sse.decisionmaker.server.TraceManager
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest
import de.tu_darmstadt.sse.sharedclasses.networkconnection.ServerResponse
import soot.Unit


class SimpleRandomFuzzer : FuzzyAnalysis() {

    private val primitives = RandomPrimitives()

    override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {}

    override fun resolveRequest(clientRequest: DecisionRequest,
                                completeHistory: ThreadTraceManager): List<AnalysisDecision> {
        val finalDecistion = AnalysisDecision()
        finalDecistion.decisionWeight = 3
        finalDecistion.analysisName = getAnalysisName()
        val response = ServerResponse()
        response.analysisName = getAnalysisName()

        if (clientRequest.isHookAfter) {
            val hookSignature = clientRequest.loggingPointSignature
            val returnType = extractReturnType(hookSignature)
            if (returnType != "void") {
                if (primitives.isSupportedType(returnType)) {
                    if (DeterministicRandom.theRandom.nextBoolean()) {
                        val value = primitives.next(returnType)
                        response.returnValue = value
                        response.setResponseExist(true)
                    }
                } else {
                    if (DeterministicRandom.theRandom.nextBoolean()) {
                        response.returnValue = "null"
                        response.setResponseExist(true)
                    }
                }
            }
        }

        finalDecistion.serverResponse = response
        return listOf(finalDecistion)
    }

    private fun extractReturnType(methodSignature: String): String {
        return methodSignature.split(": ")[1].split(" ")[0]
    }

    override fun reset() {
        // nothing to do here
    }

    override fun getAnalysisName(): String {
        return "SimpleRandomFuzzer"
    }

}
