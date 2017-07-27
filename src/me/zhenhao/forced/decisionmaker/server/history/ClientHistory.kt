package me.zhenhao.forced.decisionmaker.server.history

import java.util.ArrayList
import java.util.HashMap

import soot.Unit
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.DynamicValueContainer
import me.zhenhao.forced.dynamiccfg.Callgraph
import me.zhenhao.forced.shared.networkconnection.DecisionRequest


class ClientHistory : Cloneable {

    val codePositions = ArrayList<Unit>()

    val pathTrace = ArrayList<Pair<Unit, Boolean>>()

    val decisionAndResponse = ArrayList<Pair<DecisionRequest, AnalysisDecision>>()

    var callgraph = Callgraph()

    var isShadowTrace = false

    var crashException: String? = null

    var dynamicValues = DynamicValueContainer()
        get

    private val progressMetrics = HashMap<String, Int>()

    constructor()

    private constructor(original: ClientHistory) {
        this.codePositions.addAll(original.codePositions)
        this.pathTrace.addAll(original.pathTrace)
        for ((first, second) in original.decisionAndResponse)
            this.decisionAndResponse.add(Pair(first.clone(), second.clone()))
        this.callgraph = original.callgraph.clone()
        this.progressMetrics.putAll(original.progressMetrics)
        this.crashException = original.crashException
        this.dynamicValues = dynamicValues.clone()
    }


    fun addCodePosition(codePosition: Unit) {
        // The client might notify us of the same code position several times
        if (codePositions.isEmpty() || codePositions[codePositions.size - 1] !== codePosition)
            codePositions.add(codePosition)
    }


    fun addCodePosition(codePosition: Int, manager: CodePositionManager) {
        val unit = manager.getUnitForCodePosition(codePosition)
        if (unit != null)
            addCodePosition(unit)
    }


    fun addPathTrace(ifStmt: Unit, decision: Boolean) {
        pathTrace.add(Pair(ifStmt, decision))
    }


    fun addDecisionRequestAndResponse(request: DecisionRequest, response: AnalysisDecision) {
        decisionAndResponse.add(Pair(request, response))
    }


    fun setProgressValue(metric: String, value: Int) {
        // We always take the best value we have seen so far
        val oldValue = this.progressMetrics[metric]
        var newValue = value
        if (oldValue != null)
            newValue = Math.min(value, oldValue)
        this.progressMetrics.put(metric, newValue)
    }


    fun getProgressValue(metric: String): Int {
        val value = progressMetrics[metric]
        return value ?: Integer.MAX_VALUE
    }


    fun getResponseForRequest(request: DecisionRequest): AnalysisDecision? {
        return decisionAndResponse
                .firstOrNull { it.first == request && it.second.serverResponse.doesResponseExist() }
                ?.second
    }


    fun hasOnlyEmptyDecisions(): Boolean {
        if (decisionAndResponse.isEmpty())
            return false
        return decisionAndResponse.none { it.second.serverResponse.doesResponseExist() }
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + codePositions.hashCode()
        result = prime * result + decisionAndResponse.hashCode()
        result = prime * result + callgraph.hashCode()
        result = prime * result + pathTrace.hashCode()
        result = prime * result + progressMetrics.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as ClientHistory?

        if (codePositions != other!!.codePositions)
            return false
        if (decisionAndResponse != other.decisionAndResponse)
            return false

        if (callgraph != other.callgraph)
            return false
        if (pathTrace != other.pathTrace)
            return false
        if (progressMetrics != other.progressMetrics)
            return false
        return true
    }


    fun length(): Int {
        return decisionAndResponse.size
    }


    fun isPrefixOf(existingHistory: ClientHistory): Boolean {
        // The given history must be at least as long as the current one
        if (existingHistory.length() < this.length())
            return false

        // Check for incompatibilities
        for (i in decisionAndResponse.indices) {
            val pairThis = decisionAndResponse[i]
            val pairEx = existingHistory.decisionAndResponse[i]
            if (pairThis.first != pairEx.first || pairThis.second != pairEx.second)
                return false
        }
        return true
    }


    public override fun clone(): ClientHistory {
        return ClientHistory(this)
    }


    val allDecisionRequestsAndResponses: List<Pair<DecisionRequest, AnalysisDecision>>
        get() = this.decisionAndResponse


    fun removeUnusedDecisions() {
        val pairIt = decisionAndResponse.iterator()
        while (pairIt.hasNext()) {
            val pair = pairIt.next()
            if (!pair.second.isDecisionUsed)
                pairIt.remove()
        }
    }
}
