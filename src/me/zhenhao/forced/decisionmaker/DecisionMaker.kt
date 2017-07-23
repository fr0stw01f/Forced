package me.zhenhao.forced.decisionmaker

import me.zhenhao.forced.EnvironmentResult
import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.FrameworkOptions.TraceConstructionMode
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.apkspecific.CodeModel.StaticCodeIndexer
import me.zhenhao.forced.apkspecific.UtilApk
import me.zhenhao.forced.bootstrap.AnalysisTaskManager
import me.zhenhao.forced.bootstrap.DexFileManager
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.decisionmaker.analysis.filefuzzer.FileFuzzer
import me.zhenhao.forced.decisionmaker.server.SocketServer
import me.zhenhao.forced.decisionmaker.server.ThreadTraceManager
import me.zhenhao.forced.decisionmaker.server.TraceManager
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory
import me.zhenhao.forced.decisionmaker.server.history.GeneticCombination
import me.zhenhao.forced.dynamiccfg.DynamicCallgraphBuilder
import me.zhenhao.forced.dynamiccfg.utils.MapUtils
import me.zhenhao.forced.frameworkevents.FrameworkEvent
import me.zhenhao.forced.frameworkevents.manager.FrameworkEventManager
import me.zhenhao.forced.sharedclasses.SharedClassesSettings
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest
import me.zhenhao.forced.sharedclasses.networkconnection.ServerResponse
import soot.jimple.infoflow.android.manifest.ProcessManifest
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.Map.Entry


class DecisionMaker(val config: DecisionMakerConfig, val dexFileManager: DexFileManager,
                    val analysisTaskManager: AnalysisTaskManager) {

    lateinit private var result: EnvironmentResult

    private var manifest: ProcessManifest? = null

    private var socketServer: SocketServer? = null

    lateinit private var eventManager: FrameworkEventManager

    lateinit private var codeIndexer: StaticCodeIndexer

    private val traceManager = TraceManager()

    lateinit private var logFileProgressName: String

    private var geneticOnlyMode = false

    var dynamicCallgraph: DynamicCallgraphBuilder? = null
        private set

    lateinit var codePositionManager: CodePositionManager
        private set

    fun runPreAnalysisPhase() {
        logProgressMetricsInit()
        startAllPreAnalysis()
    }

    private fun logProgressMetricsInit() {
        val date = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
        val logFileProgress: FileWriter
        try {
            logFileProgressName = "plot" + File.separator + "logProgress-" + dateFormat.format(date) + ".data"
            logFileProgress = FileWriter(logFileProgressName, true)
            config.progressMetrics
                    .map { it.javaClass.name }
                    .forEach {
                        logFileProgress.write(it.substring(it.lastIndexOf('.') + 1) + '\t')
                    }
            logFileProgress.write(System.getProperty("line.separator"))
            logFileProgress.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    private fun startAllPreAnalysis() {
        for (analysis in config.allAnalyses) {
            LoggerHelper.logEvent(MyLevel.PRE_ANALYSIS_START, analysis.getAnalysisName())
            analysis.doPreAnalysis(config.allTargetLocations, traceManager)
            LoggerHelper.logEvent(MyLevel.PRE_ANALYSIS_STOP, analysis.getAnalysisName())
        }
    }


    private fun computeResponse(request: DecisionRequest, currentManager: ThreadTraceManager): ServerResponse {
        // If we already have a decision for that request in the current history, we take it
        run {
            val response = currentManager.getNewestClientHistory()?.getResponseForRequest(request)
            if (response != null && response.serverResponse.doesResponseExist()) {
                response.isDecisionUsed = true
                LoggerHelper.logEvent(MyLevel.ANALYSIS_NAME, response.analysisName)
                return response.serverResponse
            }
        }

        // Compute the analyses for the current request
        val allDecisions = config.allAnalyses
                .map {
                    it.resolveRequest(request, currentManager)  // We only add decisions that actually value values
                }
                .flatMap { it }
                .filter { it.serverResponse.doesResponseExist() }
                .toMutableList()

        // If we are in genetic-only mode and don't have a response in the
        // current trace, we try to get something from an older trace
        if (geneticOnlyMode && allDecisions.isEmpty()) {
            val decision = currentManager.getBestResponse(request)
            if (decision != null && decision.serverResponse.doesResponseExist())
                allDecisions.add(decision)
        }

        // If no analysis returned anything, but we asked, we create an empty response
        // so that we can at least keep track of the hook
        if (allDecisions.isEmpty()) {
            val resp = ServerResponse()
            resp.setResponseExist(false)
            resp.analysisName = "EMPTY_ANALYSIS"

            val decision = AnalysisDecision()
            decision.serverResponse = resp
            decision.analysisName = "EMPTY_ANALYSIS"
            allDecisions.add(decision)

            if (geneticOnlyMode)
                System.err.println("We're in genetic-only mode, but don't have a value for the request.")
        }

        // Apply penalties (if any) to the decisions
        for (decision in allDecisions) {
            val analysis = config.getAnalysisByName(decision.analysisName)
            if (analysis != null) {
                val penalty = analysis.penaltyRank
                if (penalty > 0) {
                    val newWeight = (decision.decisionWeight.toFloat() / (0.1 * penalty.toFloat() + 1.0f)).toFloat()
                    decision.decisionWeight = Math.round(newWeight)
                }
            }
        }

        // Get one of the decisions with the highest confidence
        val finalDecision = getFinalDecision(allDecisions)

        // If the analysis gave us lots of choices, we need to feed them into
        // the trace set to make them available to the genetic algorithm in future runs
        val currentHistory = currentManager.getNewestClientHistory()
        if (currentHistory != null && allDecisions.size > 1) {
            for (nonPickedDecision in allDecisions)
                if (nonPickedDecision !== finalDecision && nonPickedDecision.serverResponse.doesResponseExist()) {
                    val shadow = currentHistory.clone()
                    shadow.addDecisionRequestAndResponse(request, nonPickedDecision)
                    shadow.isShadowTrace = true
                    currentManager.addShadowHistory(shadow)
                }
        }

        // Check that we have a decision
        if (finalDecision == null)
            return ServerResponse.getEmptyResponse()
        else
            finalDecision.isDecisionUsed = true

        // Extract the server response to send back to the app and add it to the current trace
        currentHistory?.addDecisionRequestAndResponse(request, finalDecision)

        // If we have a shadow that is a prefix of the decision we have taken anyway,
        // there is no need to keep the shadow around for further testing.
        var removedCount = 0
        val shadowIt = currentManager.shadowHistories.iterator()
        while (shadowIt.hasNext()) {
            val shadow = shadowIt.next()
            if (currentHistory != null && shadow.isPrefixOf(currentHistory)) {
                shadowIt.remove()
                removedCount++
            }
        }
        if (removedCount > 0)
            LoggerHelper.logInfo("Removed " + removedCount + " shadow histories, because they "
                    + "were prefixes of the decision we are trying now.")

        val serverResponse = finalDecision.serverResponse
        serverResponse.analysisName = finalDecision.analysisName
        return serverResponse
    }


    fun resolveRequest(request: DecisionRequest): ServerResponse {
        println("Incoming decision request: " + request)

        // Get the current trace we're working on
        val currentManager = initializeHistory() ?: return ServerResponse.getEmptyResponse()

        // If we need a decision at a certain statement, we have reached that statement
        currentManager.getNewestClientHistory()?.addCodePosition(request.codePosition, codePositionManager)

        // Make sure that we have updated the dynamic callgraph
        dynamicCallgraph?.updateCFG()

        // Make sure that our metrics are up to date
        for (metric in config.progressMetrics) {
            val clientHistory = currentManager.getNewestClientHistory()
            if (clientHistory != null)
                metric.update(clientHistory)
        }

        // Compute the decision
        var response: ServerResponse? = computeResponse(request, currentManager)
        if (response == null)
            response = ServerResponse.getEmptyResponse()

        //updating the Analysis Progress Metric
        //logging the new data to file
        val logFileProgress: FileWriter
        try {
            logFileProgress = FileWriter(logFileProgressName, true)
            for (metric in config.progressMetrics) {
                val clientHistory = currentManager.getNewestClientHistory()
                if (clientHistory != null) {
                    val newlyCovered = metric.update(clientHistory)
                    println("Metric for " + metric.getMetricName() + ":" + newlyCovered)
                    logFileProgress.write(Integer.toString(newlyCovered) + '\t')
                }
            }
            logFileProgress.write(System.getProperty("line.separator"))
            logFileProgress.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return response!!
    }


    private fun getFinalDecision(decisions: List<AnalysisDecision>): AnalysisDecision? {
        val finalDecisions = ArrayList<AnalysisDecision>()
        if (decisions.isEmpty())
            return null

        // Pick among those decisions with the highest confidence
        Collections.sort(decisions)
        val highestWeight = decisions[0].decisionWeight
        for (decision in decisions) {
            if (decision.decisionWeight == highestWeight)
                finalDecisions.add(decision)
            else if (DeterministicRandom.theRandom.nextInt(GENETIC_RANDOM_OFFSET) <
                    GENETIC_RANDOM_OFFSET * GENETIC_PICK_BAD_DECISION_PROBABILITY)
                // with a certain (low) probability, we also pick a decision with lower
                // confidence
                finalDecisions.add(decision)
        }

        //random pick
        val amountOfDecisions = finalDecisions.size
        if (amountOfDecisions > 1) {
            val randomPick = DeterministicRandom.theRandom.nextInt(amountOfDecisions)
            return finalDecisions[randomPick]
        } else
            return finalDecisions[0]
    }

    fun initialize() {
        this.manifest = UtilApk.getManifest()

        // Get a code model
        codePositionManager = CodePositionManager.codePositionManagerInstance
        codeIndexer = StaticCodeIndexer()

        //start server...
        socketServer = SocketServer.getInstance(this)
        val r1 = { socketServer!!.startSocketServerObjectTransfer() }
        val backgroundThreadForObjectTransfer = Thread(r1)
        backgroundThreadForObjectTransfer.start()

        // set up event manager
        eventManager = FrameworkEventManager.eventManager
        eventManager.connectToAndroidDevice()

        //monitor the logcat for VM crashes
        if (FrameworkOptions.enableLogcatViewer)
            eventManager.startLogcatCrashViewer()
    }


    private fun reset() {
        // Create a new result object
        result = EnvironmentResult()

        // Reset all analyses
        config.allAnalyses.forEach { it.reset() }
    }

    fun executeDecisionMaker(event: FrameworkEvent?): EnvironmentResult {
        reset()
        var startingTime = System.currentTimeMillis()

        //client handling...
        if (!FrameworkOptions.testServer) {
            //pull files onto device
            eventManager.pushFuzzyFiles(FileFuzzer.FUZZY_FILES_DIR)
            eventManager.installApp(manifest!!.packageName)

            //add contacts onto device
            eventManager.addContacts(manifest!!.packageName)

            tryStartingApp()
            try {
                Thread.sleep(FrameworkOptions.tryStartAppWaitingTime.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            if (event != null)
                eventManager.sendEvent(event)

            // Make sure that we don't have any old state lying around
            socketServer!!.resetForNewRun()

            // We only reset the genetic-only mode per app installation
            geneticOnlyMode = false

            var trying = true
            while (trying && !result.isTargetReached) {
                // Compute the time since the last client request
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - socketServer!!.lastRequestProcessed

                // we do a complete (clean) re-install of the app
                if (timeDiff > FrameworkOptions.inactivityTimeout * 1000
                        || currentTime - startingTime > FrameworkOptions.forceTimeout * 1000) {

                    if (result.restartCount < FrameworkOptions.maxRestarts || FrameworkOptions.maxRestarts == -1) {
                        LoggerHelper.logEvent(MyLevel.RESTART,
                                String.format("Restarted app due to timeout: %d", result.restartCount + 1))
                        LoggerHelper.logEvent(MyLevel.RESTART,
                                String.format("timeDiff: %d\ncurr - starting: %d", timeDiff, currentTime - startingTime))

                        eventManager.killAppProcess(manifest!!.packageName)
                        eventManager.uninstallAppProcess(manifest!!.packageName)

                        //wait a couple of seconds...
                        try {
                            Thread.sleep(FrameworkOptions.uninstallWaitingTime.toLong())
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                        // Reset our internal state
                        dynamicCallgraph = null
                        result.restartCount = result.restartCount + 1
                        socketServer!!.notifyAppRunDone()

                        // We need to clean up the trace and remove all decision we haven't used
                        cleanUpUnusedDecisions()

                        // Check if one analysis performed poorly
                        penalizePoorAnalyses()

                        eventManager.installApp(manifest!!.packageName)
                        startingTime = System.currentTimeMillis()
                        tryStartingApp()

                        try {
                            Thread.sleep(FrameworkOptions.tryStartAppWaitingTime.toLong())
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                        //send events
                        if (event != null)
                            eventManager.sendEvent(event)
                    } else {
                        LoggerHelper.logEvent(MyLevel.RUNTIME, "Maximum number of restarts reached -- giving up.")
                        trying = false
                    }
                }
            }

            // Make sure to clean up after ourselves
            eventManager.killAppProcess(manifest!!.packageName)
            eventManager.uninstallAppProcess(manifest!!.packageName)
        } else {
            System.err.println("TESTING SERVER ONLY")
            while (true) {
            }
        }//test server
        return result
    }


    private fun cleanUpUnusedDecisions() {
        traceManager.getAllThreadTraceManagers().forEach { it.getNewestClientHistory()?.removeUnusedDecisions() }
    }


    private fun penalizePoorAnalyses() {
        var historyCount = 0
        val analysisToBestScore = HashMap<String, Int>()
        for (tm in traceManager.getAllThreadTraceManagers()) {
            for (hist in tm.clientHistories) {
                historyCount++
                val progressVal = hist.getProgressValue("ApproachLevel")
                for (pair in hist.allDecisionRequestsAndResponses) {
                    val name = pair.second.analysisName
                    val oldVal = analysisToBestScore[name]
                    if (oldVal == null || oldVal < progressVal)
                        analysisToBestScore.put(name, progressVal)
                }
            }
        }

        // We only judge analyses if have some data
        if (historyCount < PENALIZE_ANALYSES_MIN_HISTORY_COUNT || analysisToBestScore.size < 2)
            return

        // Check if we have an analysis that is  10 times worse than the next
        // better one
        val sortedMap = MapUtils.sortByValue(analysisToBestScore)
        var lastEntry: Entry<String, Int>? = null
        var penaltyRank = 1
        for (entry in sortedMap.entries) {
            // Skip the first entry
            if (lastEntry == null) {
                lastEntry = entry
                continue
            }

            // Is this entry 10 times worse than the previous one?
            if (entry.value * PENALIZE_ANALYSES_FACTOR < lastEntry.value) {
                val analysis = config.getAnalysisByName(entry.key)
                analysis?.penaltyRank = penaltyRank++
            }

            lastEntry = entry
        }
    }

    fun tearDown() {
        socketServer!!.stop()
    }


    fun getManagerForThreadId(threadId: Long): ThreadTraceManager {
        return traceManager.getThreadTraceManager(threadId)
    }


    @Synchronized fun initializeHistory(): ThreadTraceManager? {
        val manager = traceManager.getOrCreateThreadTraceManager(-1)

        // Only perform genetic recombination when actually generating new traces
        var forceGenetic: Boolean
        if (manager.clientHistories.size <= result.restartCount) {
            // Are we in genetic-only mode?
            forceGenetic = geneticOnlyMode

            // If we did not get any new values in the last run, the analyses have
            // run out of values. In that case, we can only rely on genetic recombination.
            if (!forceGenetic) {
                // We can only make this decision if we have already had one complete run
                if (manager.clientHistories.size > 1 && manager.getLastClientHistory() != null
                        && manager.getLastClientHistory()!!.hasOnlyEmptyDecisions()) {
                    if (manager.historyPlusShadowCount >= 2) {
                        forceGenetic = true
                        geneticOnlyMode = true
                        LoggerHelper.logEvent(MyLevel.GENTETIC_ONLY_MODE, "genetic only mode on")
                    } else {
                        System.err.println("It's all empty now, but we don't have enough histories to combine. " +
                                "Looks like we're seriously out of luck.")
                        return null
                    }
                }
            }

            // If we have a couple of histories, we do genetic recombination
            if (!forceGenetic) {
                if (manager.clientHistories.size > GENETIC_MIN_GENE_POOL_SIZE) {
                    if (DeterministicRandom.theRandom.nextInt(GENETIC_RANDOM_OFFSET) <
                            GENETIC_GENE_POOL_EXTENSION_PROBABILITY * GENETIC_RANDOM_OFFSET) {
                        forceGenetic = true
                        LoggerHelper.logEvent(MyLevel.GENTETIC_ONLY_MODE, "genetic only mode on")
                    }
                }
            }

            // When we do genetic recombination, we pre-create a history object
            if (forceGenetic && FrameworkOptions.traceConstructionMode !== TraceConstructionMode.AnalysesOnly) {
                LoggerHelper.logInfo("Using genetic recombination for generating a trace...")

                // We also need to take the shadow histories into account. We take histories
                // from all threads in case we are not on the main thread
                val histories = HashSet<ClientHistory>()

                traceManager.getAllThreadTraceManagers().forEach {
                    histories.addAll(it.clientHistories)
                    histories.addAll(it.shadowHistories)
                }

                // Do the genetic combination
                val combinedHistory = GeneticCombination.combineGenetically(histories)
                if (combinedHistory == null) {
                    LoggerHelper.logWarning("Genetic recombination failed.")
                    return null
                }
                combinedHistory.isShadowTrace = false
                manager.ensureHistorySize(result.restartCount + 1, combinedHistory)

                // Create the dynamic callgraph
                this.dynamicCallgraph = DynamicCallgraphBuilder(manager.getNewestClientHistory()?.callgraph!!,
                        codePositionManager, codeIndexer)
                return manager
            } else if (manager.ensureHistorySize(result.restartCount + 1)) {
                // Check it
                if (geneticOnlyMode)
                    System.err.println("In genetic only mode, but didn't recombine anything. Life ain't good, man :(")

                // Create the new trace
                LoggerHelper.logInfo("Creating a new empty trace...")
                this.dynamicCallgraph = DynamicCallgraphBuilder(manager.getNewestClientHistory()?.callgraph!!,
                        codePositionManager, codeIndexer)
            }// If we actually created a new trace, we must re-initialize the
            // factories

            // We need a dynamic callgraph
            if (this.dynamicCallgraph == null)
                throw RuntimeException("This should never happen. There is no such exception. " +
                        "It's all just an illusion. Move along.")
        }

        return manager
    }


    fun setTargetReached(targetReached: Boolean) {
        result.isTargetReached = targetReached
    }


    private fun tryStartingApp() {
        val hasLaunchableActivity = manifest!!.launchableActivities.size > 0
        val packageName = manifest!!.packageName
        if (hasLaunchableActivity) {
            eventManager.startApp(packageName)
        } else if (manifest!!.activities.size > 0) {
            val node = manifest!!.activities.iterator().next()
            val activityName = node.getAttribute("name").value as String

            eventManager.startActivity(packageName, activityName)
        } else if (manifest!!.services.size > 0) {
            val node = manifest!!.services.iterator().next()
            val serviceName = node.getAttribute("name").value as String

            eventManager.startService(packageName, serviceName)
        } else
            //if there is no launchable activity and no activity at all, we try calling the first service in manifest
            throw RuntimeException("we are not able to start the application")
            //if there is no launchable activity, we try calling the first activity in manifest
    }


    fun SIMPLE_START_APP_OR_START_APP_AND_INIT_EVENT_EVALUATION_CASE(event: FrameworkEvent?) {
        var startingTime = System.currentTimeMillis()
        // Create a new result object
        result = EnvironmentResult()
        //pull files onto device
        eventManager.installApp(manifest!!.packageName)
        tryStartingApp()
        try {
            Thread.sleep(FrameworkOptions.tryStartAppWaitingTime.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        if (event != null)
            eventManager.sendEvent(event)
        var trying = true
        while (trying && !result.isTargetReached) {
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - socketServer!!.lastRequestProcessed

            // we do a complete (clean) re-install of the app
            if (timeDiff > FrameworkOptions.inactivityTimeout * 1000 ||
                    currentTime - startingTime > FrameworkOptions.forceTimeout * 1000) {

                if (result.restartCount < FrameworkOptions.maxRestarts || FrameworkOptions.maxRestarts == -1) {
                    LoggerHelper.logEvent(MyLevel.RESTART,
                            String.format("Restarted app due to timeout: %d", result.restartCount + 1))
                    LoggerHelper.logEvent(MyLevel.RESTART,
                            String.format("timeDiff: %d\ncurr - starting: %d", timeDiff, currentTime - startingTime))

                    eventManager.killAppProcess(manifest!!.packageName)
                    eventManager.uninstallAppProcess(manifest!!.packageName)

                    //wait a couple of seconds...
                    try {
                        Thread.sleep(FrameworkOptions.uninstallWaitingTime.toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }


                    // Reset our internal state
                    dynamicCallgraph = null
                    geneticOnlyMode = false
                    result.restartCount = result.restartCount + 1
                    socketServer!!.notifyAppRunDone()

                    eventManager.installApp(manifest!!.packageName)
                    startingTime = System.currentTimeMillis()
                    tryStartingApp()

                    try {
                        Thread.sleep(FrameworkOptions.tryStartAppWaitingTime.toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    //send events
                    if (event != null)
                        eventManager.sendEvent(event)
                } else {
                    LoggerHelper.logEvent(MyLevel.RUNTIME, "Maximum number of restarts reached -- giving up.")
                    trying = false
                }
            }
        }

        // Make sure to clean up after ourselves
        eventManager.killAppProcess(manifest!!.packageName)
        eventManager.uninstallAppProcess(manifest!!.packageName)
    }

    companion object {
        private val GENETIC_MIN_GENE_POOL_SIZE = 5
        private val GENETIC_RANDOM_OFFSET = 10000
        private val GENETIC_GENE_POOL_EXTENSION_PROBABILITY = 0.25f
        private val GENETIC_PICK_BAD_DECISION_PROBABILITY = 0.10f
        private val PENALIZE_ANALYSES_MIN_HISTORY_COUNT = 5
        private val PENALIZE_ANALYSES_FACTOR = 2.0f
    }

}
