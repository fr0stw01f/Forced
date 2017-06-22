package de.tu_darmstadt.sse.decisionmaker.server

import java.util.ArrayList
import java.util.HashSet

import de.tu_darmstadt.sse.decisionmaker.analysis.AnalysisDecision
import de.tu_darmstadt.sse.decisionmaker.server.history.ClientHistory
import de.tu_darmstadt.sse.decisionmaker.server.history.ClientHistoryCreatedHandler
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest


class ThreadTraceManager(val threadID: Long) {
    private val clientHistories = ArrayList<ClientHistory>()
    private val shadowHistories = ArrayList<ClientHistory>()
    private val onCreateHandlers = HashSet<ClientHistoryCreatedHandler>()


    fun ensureHistorySize(size: Int): Boolean {
        if (clientHistories.size >= size)
            return false

        for (i in clientHistories.size..size - 1) {
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


    fun getHistories(): List<ClientHistory> {
        return this.clientHistories
    }


    fun getNewestClientHistory(): ClientHistory?{
        if (this.clientHistories.isEmpty())
            return null
        return this.clientHistories[this.clientHistories.size - 1]
    }


    fun getLastClientHistory(): ClientHistory? {
        if (this.clientHistories.size < 2)
            return null
        return this.clientHistories[this.clientHistories.size - 2]
    }


    fun addShadowHistory(history: ClientHistory) {
        // Do not add a shadow history that is a prefix of an existing history or
        // shadow history
        for (existingHistory in clientHistories)
            if (history.isPrefixOf(existingHistory))
                return
        for (existingHistory in shadowHistories)
            if (history.isPrefixOf(existingHistory))
                return

        // Add the new shadow history
        shadowHistories.add(history)

        // Notify our handlers
        for (handler in onCreateHandlers)
            handler.onClientHistoryCreated(history)
    }


    fun getShadowHistories(): List<ClientHistory> {
        return this.shadowHistories
    }


    fun addOnCreateHandler(handler: ClientHistoryCreatedHandler) {
        this.onCreateHandlers.add(handler)
    }


    val historyAndShadowCount: Int
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
                if (decision != null && decision.serverResponse!!.doesResponseExist()) {
                    bestDecision = decision
                    bestScore = score
                }
            }
        }
        return bestDecision
    }

}
