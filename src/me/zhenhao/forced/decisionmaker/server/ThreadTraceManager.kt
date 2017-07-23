package me.zhenhao.forced.decisionmaker.server

import java.util.ArrayList
import java.util.HashSet

import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory
import me.zhenhao.forced.decisionmaker.server.history.ClientHistoryCreatedHandler
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest


class ThreadTraceManager(val threadID: Long) {
    val clientHistories = ArrayList<ClientHistory>()
    val shadowHistories = ArrayList<ClientHistory>()
    private val onCreateHandlers = HashSet<ClientHistoryCreatedHandler>()


    fun ensureHistorySize(size: Int): Boolean {
        if (clientHistories.size >= size)
            return false

        for (i in 0..size-clientHistories.size-1) {
            val dummyHistory = ClientHistory()
            clientHistories.add(dummyHistory)
            for (handler in onCreateHandlers)
                handler.onClientHistoryCreated(dummyHistory)
        }
        return true
    }


    fun ensureHistorySize(size: Int, history: ClientHistory) {
        while (this.clientHistories.size < size) {
            this.clientHistories.add(history)
            for (handler in onCreateHandlers)
                handler.onClientHistoryCreated(history)
        }
    }


    fun getNewestClientHistory(): ClientHistory?{
        if (this.clientHistories.isEmpty())
            return null
        return this.clientHistories[this.clientHistories.size-1]
    }


    fun getLastClientHistory(): ClientHistory? {
        if (this.clientHistories.size < 2)
            return null
        return this.clientHistories[this.clientHistories.size-2]
    }


    fun addShadowHistory(history: ClientHistory) {
        // Do not add a shadow history that is a prefix of an existing history or
        // shadow history
        if (clientHistories.any { history.isPrefixOf(it) } || shadowHistories.any { history.isPrefixOf(it) })
            return

        // Add the new shadow history
        shadowHistories.add(history)

        onCreateHandlers.forEach { it.onClientHistoryCreated(history) }
    }


    fun addOnCreateHandler(handler: ClientHistoryCreatedHandler) {
        onCreateHandlers.add(handler)
    }


    val historyPlusShadowCount: Int
        get() = clientHistories.size + shadowHistories.size


    val isEmpty: Boolean
        get() = clientHistories.isEmpty()


    fun getBestResponse(request: DecisionRequest): AnalysisDecision? {
        // Accumulate the decisions we have so far for the given request
        var bestDecision: AnalysisDecision? = null
        var bestScore = 0
        for (history in clientHistories) {
            val score = history.getProgressValue("ApproachLevel")
            if (score > bestScore) {
                val decision = history.getResponseForRequest(request)
                if (decision != null && decision.serverResponse.doesResponseExist()) {
                    bestDecision = decision
                    bestScore = score
                }
            }
        }
        return bestDecision
    }

}
