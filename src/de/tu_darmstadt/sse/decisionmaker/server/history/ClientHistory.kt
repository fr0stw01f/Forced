package de.tu_darmstadt.sse.decisionmaker.server.history

import java.util.ArrayList
import java.util.HashMap

import soot.Unit
import de.tu_darmstadt.sse.apkspecific.CodeModel.CodePositionManager
import de.tu_darmstadt.sse.decisionmaker.analysis.AnalysisDecision
import de.tu_darmstadt.sse.decisionmaker.analysis.dynamicValues.DynamicValueContainer
import de.tu_darmstadt.sse.dynamiccfg.Callgraph
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest


class ClientHistory : Cloneable {

    private val codePositions = ArrayList<Unit>()

    private val pathTrace = ArrayList<Pair<Unit, Boolean>>()

    private val decisionAndResponse = ArrayList<Pair<DecisionRequest, AnalysisDecision>>()

    var callgraph = Callgraph()

    private val progressMetrics = HashMap<String, Int>()

    var isShadowTrace = false

    var crashException: String? = null

    var dynamicValues = DynamicValueContainer()
        get

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


    fun addCodePosition(codePostion: Unit) {
        // The client might notify us of the same code position several times
        if (codePositions.isEmpty() || codePositions[codePositions.size - 1] !== codePostion)
            codePositions.add(codePostion)
    }


    fun addCodePosition(codePostion: Int, manager: CodePositionManager) {
        val unit = manager.getUnitForCodePosition(codePostion)
        if (unit != null)
            addCodePosition(unit)
    }


    fun addPathTrace(ifStmt: Unit, decision: Boolean) {
        pathTrace.add(Pair(ifStmt, decision))
    }


    fun addDecisionRequestAndResponse(request: DecisionRequest, response: AnalysisDecision) {
        decisionAndResponse.add(Pair(request, response))
    }

    val codePostions: List<Unit>
        get() = codePositions

    fun getPathTrace(): List<Pair<Unit, Boolean>> {
        return pathTrace
    }

    fun getDecisionAndResponse(): List<Pair<DecisionRequest, AnalysisDecision>> {
        return decisionAndResponse
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
        val `val` = progressMetrics[metric]
        return `val` ?: Integer.MAX_VALUE
    }


    fun getResponseForRequest(request: DecisionRequest): AnalysisDecision? {
        return getDecisionAndResponse()
                .firstOrNull { it.first == request && it.second.serverResponse!!.doesResponseExist() }
                ?.second
    }


    fun hasOnlyEmptyDecisions(): Boolean {
        if (decisionAndResponse.isEmpty())
            return false
        return decisionAndResponse.none { it.second.serverResponse!!.doesResponseExist() }
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
