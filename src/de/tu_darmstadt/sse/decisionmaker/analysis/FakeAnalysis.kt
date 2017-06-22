package de.tu_darmstadt.sse.decisionmaker.analysis

import de.tu_darmstadt.sse.decisionmaker.server.ThreadTraceManager
import de.tu_darmstadt.sse.decisionmaker.server.TraceManager
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest
import de.tu_darmstadt.sse.sharedclasses.networkconnection.ServerResponse
import soot.Unit
import java.util.*

class FakeAnalysis : FuzzyAnalysis() {

    override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {
    }

    private val requests = HashSet<DecisionRequest>()

    override fun resolveRequest(clientRequest: DecisionRequest,
                                completeHistory: ThreadTraceManager): List<AnalysisDecision> {
        if (!requests.add(clientRequest))
            return emptyList()

        val decisions = ArrayList<AnalysisDecision>()

        // Fake 3 values
        decisions.add(fakeDecision("aaaag"))
        decisions.add(fakeDecision("bmtsg"))
        decisions.add(fakeDecision("bmegafong"))

        return decisions
    }

    private fun fakeDecision(string: String): AnalysisDecision {
        val resp = ServerResponse()
        resp.setResponseExist(true)
        resp.returnValue = string

        val decision = AnalysisDecision()
        decision.analysisName = "FAKE"
        decision.decisionWeight = 100
        decision.serverResponse = resp

        return decision
    }

    override fun reset() {
        requests.clear()
    }

    override fun getAnalysisName(): String {
        return "FAKE"
    }

}
