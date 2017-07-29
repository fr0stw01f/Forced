package me.zhenhao.forced.decisionmaker.server.history

import java.util.ArrayList
import java.util.HashSet

import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.FrameworkOptions.TraceConstructionMode
import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.decisionmaker.DeterministicRandom
import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.shared.networkconnection.DecisionRequest


object GeneticCombination {

    private fun combine(history1: ClientHistory, history2: ClientHistory,
                        mutationHistories: Set<ClientHistory>): ClientHistory {
        val requests = HashSet<DecisionRequest>()
        for ((first) in history1.decisionAndResponse)
            requests.add(first)
        for ((first) in history2.decisionAndResponse)
            requests.add(first)

        // Create the combined history
        val newHistory = ClientHistory()

        for (request in requests) {
            // For each request, randomly get a value from one of the two histories
            var response: AnalysisDecision? = null
            val mutate = DeterministicRandom.theRandom.nextInt(MULTIPLIER) < MULTIPLIER * MUTATION_PROBABILITY
            if (mutate) {
                val remaining = HashSet(mutationHistories)
                remaining.remove(history1)
                remaining.remove(history2)
                response = pickRandomResponse(request, remaining)
            } else {
                if (DeterministicRandom.theRandom.nextBoolean()) {
                    response = history1.getResponseForRequest(request)
                    if (response == null || !response.serverResponse.doesResponseExist())
                        response = history2.getResponseForRequest(request)
                } else {
                    response = history2.getResponseForRequest(request)
                    if (response == null || !response.serverResponse.doesResponseExist())
                        response = history1.getResponseForRequest(request)
                }
            }

            // If we have a response, we record it in the new history
            if (response != null && response.serverResponse.doesResponseExist())
                newHistory.addDecisionRequestAndResponse(request, response)
        }

        return newHistory
    }


    private fun pickRandomResponse(request: DecisionRequest,
                                   histories: MutableSet<ClientHistory>): AnalysisDecision? {
        while (!histories.isEmpty()) {
            // Randomly pick a new history
            val index = DeterministicRandom.theRandom.nextInt(histories.size)
            var history: ClientHistory? = null
            run {
                var curIdx = 0
                for (hist in histories) {
                    if (curIdx == index) {
                        history = hist
                        break
                    }
                    curIdx++
                }
            }
            histories.remove(history)

            // Does this history define a response for the given request?
            val response = history!!.getResponseForRequest(request)
            if (response != null)
                return response
        }

        // No value found
        return null
    }


    fun combineGenetically(histories: MutableCollection<ClientHistory>): ClientHistory? {
        // Clean up the set of histories
        val hists = HashSet(histories)
        val pickCrashed = DeterministicRandom.theRandom.nextInt(MULTIPLIER) < CRASH_PICK_PROBABILITY * MULTIPLIER
        val histIt = histories.iterator()
        while (histIt.hasNext()) {
            val curHist = histIt.next()

            // Do we allow picking crashed traces?
            if (!pickCrashed && curHist.crashException != null)
                histIt.remove()
            else if (curHist.hasOnlyEmptyDecisions())
                histIt.remove()// Remove empty traces
        }

        // If we only have one history, there is nothing to combine
        LogHelper.logInfo("Genetically combining " + hists.size + " histories...")
        if (hists.size < 2)
            return null

        while (!hists.isEmpty()) {
            // Do we do random pick?
            var bestHistory: ClientHistory? = null
            var secondBestHistory: ClientHistory? = null
            if (FrameworkOptions.traceConstructionMode === TraceConstructionMode.RandomCombine || DeterministicRandom.theRandom.nextInt(MULTIPLIER) < LESSER_COMBINATION_PROBABILITY * MULTIPLIER) {
                val histList = ArrayList(hists)
                bestHistory = histList[DeterministicRandom.theRandom.nextInt(
                        histList.size)]
            } else {
                // Get the best histories
                var bestValue = 0
                for (curHistory in hists) {
                    val curValue = curHistory.getProgressValue("ApproachLevel")
                    if (curValue > bestValue) {
                        bestValue = curValue
                    }
                }
                val tempList = ArrayList<ClientHistory>(hists.size)
                getHistoriesByApproachLevel(tempList, hists, bestValue)

                // If we have shadow traces, we only pick from those with the highest
                // confidence in the last (not yet tried) decision
                val bestHistories = ArrayList<ClientHistory>(hists.size)
                filterBestShadowHistories(bestHistories, tempList)

                // Randomly pick one of the best histories
                bestHistory = bestHistories[DeterministicRandom.theRandom.nextInt(bestHistories.size)]

                // If we have a second history of the same quality, we take that
                if (bestHistories.size > 1) {
                    while (secondBestHistory == null || secondBestHistory === bestHistory)
                        secondBestHistory = bestHistories[DeterministicRandom.theRandom.nextInt(bestHistories.size)]
                }
            }

            // Once again, allow for random pick
            if (FrameworkOptions.traceConstructionMode === TraceConstructionMode.RandomCombine || DeterministicRandom.theRandom.nextInt(MULTIPLIER) < LESSER_COMBINATION_PROBABILITY * MULTIPLIER) {
                val histList = ArrayList(hists)
                while (secondBestHistory == null || secondBestHistory === bestHistory) {
                    secondBestHistory = histList.removeAt(DeterministicRandom.theRandom.nextInt(
                            histList.size))
                }
            } else {
                // If we don't have a history of the same quality, we need to get a
                // lesser one
                if (secondBestHistory == null) {
                    var secondBestValue = 0
                    for (curHistory in hists) {
                        val curValue = curHistory.getProgressValue("ApproachLevel")
                        if (curHistory !== bestHistory && curValue > secondBestValue) {
                            secondBestValue = curValue
                        }
                    }

                    val tempList = ArrayList<ClientHistory>(hists.size)
                    getHistoriesByApproachLevel(tempList, hists, secondBestValue)

                    // If we have shadow traces, we only pick from those with the highest
                    // confidence in the last (not yet tried) decision
                    val secondBestHistories = ArrayList<ClientHistory>(hists.size)
                    filterBestShadowHistories(secondBestHistories, tempList)

                    // Randomly pick one of the second-best histories
                    while ((secondBestHistory == null || secondBestHistory === bestHistory) && !secondBestHistories.isEmpty()) {
                        val idx = DeterministicRandom.theRandom.nextInt(secondBestHistories.size)
                        secondBestHistory = secondBestHistories.removeAt(idx)
                    }
                }
            }

            // If we did not find two histories to combine, try again
            if (bestHistory == null || secondBestHistory == null)
                continue

            // Combine the two histories
            val hist = combine(bestHistory, secondBestHistory, hists)
            if (hist.hasOnlyEmptyDecisions()) {
                System.err.println("Result of genetic combination has only empty decisions")
                hists.remove(bestHistory)
                continue
            }

            // If the genetic recombination gives us a new history that we have
            // already tried, we remove the best candidate and give the weaker
            // ones a chance
            if (hists.contains(hist))
                hists.remove(bestHistory)
            else
                return hist
        }

        // nothing left to combine
        return null
    }


    private fun filterBestShadowHistories(
            secondBestHistories: MutableList<ClientHistory>,
            tempList: List<ClientHistory>) {
        // First pass: Copy over all non-shadow traces and find the highest confidence
        // value for the shadow traces
        var highestConfidence = 0
        for (history in tempList) {
            if (history.isShadowTrace) {
                val (_, second) = history.decisionAndResponse[history.decisionAndResponse.size - 1]
                highestConfidence = Math.max(highestConfidence,
                        second.decisionWeight)
            } else
                secondBestHistories.add(history)
        }

        // Second pass: Copy over all shadow traces with the confidence value found in
        // step 1
        for (history in tempList) {
            if (history.isShadowTrace) {
                val (_, second) = history.decisionAndResponse[history.decisionAndResponse.size - 1]
                if (second.decisionWeight == highestConfidence)
                    secondBestHistories.add(history)
            }
        }
    }


    private fun getHistoriesByApproachLevel(bestHistories: MutableCollection<ClientHistory>,
                                            hists: Collection<ClientHistory>, bestValue: Int) {
        for (curHistory in hists) {
            val curValue = curHistory.getProgressValue("ApproachLevel")
            if (curValue == bestValue) {
                bestHistories.add(curHistory)
            }
        }
    }


    private val MUTATION_PROBABILITY = 0.1f

    private val MULTIPLIER = 10000

    private val LESSER_COMBINATION_PROBABILITY = 0.1f

    private val CRASH_PICK_PROBABILITY = 0.01f
}
