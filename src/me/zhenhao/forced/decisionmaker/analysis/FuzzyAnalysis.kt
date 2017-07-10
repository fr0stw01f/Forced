package me.zhenhao.forced.decisionmaker.analysis

import soot.Unit
import me.zhenhao.forced.decisionmaker.server.ThreadTraceManager
import me.zhenhao.forced.decisionmaker.server.TraceManager
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest


abstract class FuzzyAnalysis {
    var penaltyRank = 0

    abstract fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager)

    abstract fun resolveRequest(clientRequest: DecisionRequest,
                                completeHistory: ThreadTraceManager): List<AnalysisDecision>

    abstract fun reset()

    abstract fun getAnalysisName(): String

}
