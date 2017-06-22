package de.tu_darmstadt.sse.decisionmaker.analysis

import soot.Unit
import de.tu_darmstadt.sse.decisionmaker.server.ThreadTraceManager
import de.tu_darmstadt.sse.decisionmaker.server.TraceManager
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest


abstract class FuzzyAnalysis {
    var penaltyRank = 0

    abstract fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager)

    abstract fun resolveRequest(clientRequest: DecisionRequest,
                                completeHistory: ThreadTraceManager): List<AnalysisDecision>

    abstract fun reset()

    abstract fun getAnalysisName(): String

}
