package me.zhenhao.forced.decisionmaker.server

import heros.solver.CountingThreadPoolExecutor

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashSet
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.bootstrap.DexFile
import me.zhenhao.forced.bootstrap.InstanceIndependentCodePosition
import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.DecisionMaker
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.DynamicIntValue
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.DynamicStringValue
import me.zhenhao.forced.shared.crashreporter.CrashReportItem
import me.zhenhao.forced.shared.dynamiccfg.AbstractDynamicCFGItem
import me.zhenhao.forced.shared.networkconnection.CloseConnectionRequest
import me.zhenhao.forced.shared.networkconnection.DecisionRequest
import me.zhenhao.forced.shared.networkconnection.ServerResponse
import me.zhenhao.forced.shared.networkconnection.serializables.BinarySerializableObject
import me.zhenhao.forced.shared.tracing.DexFileTransferTraceItem
import me.zhenhao.forced.shared.tracing.DynamicIntValueTraceItem
import me.zhenhao.forced.shared.tracing.DynamicStringValueTraceItem
import me.zhenhao.forced.shared.tracing.DynamicValueTraceItem
import me.zhenhao.forced.shared.tracing.PathTrackingTraceItem
import me.zhenhao.forced.shared.tracing.TargetReachedTraceItem
import me.zhenhao.forced.shared.tracing.TimingBombTraceItem
import me.zhenhao.forced.shared.tracing.TraceItem
import me.zhenhao.forced.shared.util.NetworkSettings


class SocketServer private constructor(private val decisionMaker: DecisionMaker) {

    private var executor: CountingThreadPoolExecutor? = null

    @Volatile private var serverSocket: ServerSocket? = null

    @Volatile private var stopped = false

    var lastRequestProcessed = System.currentTimeMillis()
        private set

    private inner class ClientRequestHandlerTask(private val socket: Socket) : Runnable {

        override fun run() {
            try {
                // Only create the streams once for the full lifetime of the socket
                val ois = ObjectInputStream(this.socket.getInputStream())
                val oos = ObjectOutputStream(this.socket.getOutputStream())

                while (!socket.isClosed) {
                    val clientRequest = ois.readObject()

                    // Terminate the connection if requested
                    if (clientRequest is CloseConnectionRequest) {
                        println("Received a CloseConnectionRequest")
                        oos.writeObject("CloseConnection Ack")
                        oos.flush()
                        break
                    }

                    // For every trace item, register the last position
                    if (clientRequest is TraceItem)
                        handleTraceItem(clientRequest)

                    handleClientRequests(clientRequest, oos)

                    // Make sure we send out all our data
                    //oos.flush()
                }
            } catch (ex: Exception) {
                LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS,
                        "There is a problem in the client-server communication " + ex.message)
                ex.printStackTrace()
            } finally {
                try {
                    if (!socket.isOutputShutdown)
                        socket.shutdownOutput()
                    if (!socket.isInputShutdown)
                        socket.shutdownInput()
                    socket.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    System.err.println("Network communication died: " + ex.message)
                }

            }
        }

        fun handleTraceItem(clientRequest: TraceItem) {
            val manager = decisionMaker.initializeHistory()
            if (manager != null) {
                val currentClientHistory = manager.getNewestClientHistory()
                if (currentClientHistory != null) {
                    currentClientHistory.addCodePosition(clientRequest.lastExecutedStatement,
                            decisionMaker.codePositionManager)

                    // Make sure that our metrics are up to date
                    for (metric in decisionMaker.config.progressMetrics) {
                        metric.update(currentClientHistory)
                    }
                }
            }
        }

        private fun handleClientRequests(clientRequest: Any, oos: ObjectOutputStream) {
            when (clientRequest) {
                is DecisionRequest -> handleDecisionRequest(clientRequest, oos)
                is PathTrackingTraceItem -> handlePathTracking(clientRequest, oos)
                is AbstractDynamicCFGItem -> handleDynamicCallgraph(clientRequest, oos)
                is TargetReachedTraceItem -> handleGoalReached(clientRequest, oos)
                is DynamicValueTraceItem -> handleDynamicValueReceived(clientRequest, oos)
                is TimingBombTraceItem -> handleTimingBombReceived(clientRequest, oos)
                is DexFileTransferTraceItem -> handleDexFileReceived(clientRequest, oos)
                is CrashReportItem -> handleCrash(clientRequest, oos)
                is BinarySerializableObject -> handleBinary(clientRequest, oos)
                else -> throw RuntimeException("Received an unknown data item from the app")
            }
        }

        private fun handleBinary(clientRequest: BinarySerializableObject, oos: ObjectOutputStream) {
            var bais: ByteArrayInputStream? = null
            var ois: ObjectInputStream? = null
            var innerRequest: Any? = null
            try {
                bais = ByteArrayInputStream(clientRequest.binaryData)
                ois = ObjectInputStream(bais)
                innerRequest = ois.readObject()
            } catch (e: ClassNotFoundException) {
                System.err.println("Could not de-serialize inner request object")
                e.printStackTrace()
            } finally {
                if (bais != null)
                    bais.close()
                if (ois != null)
                    ois.close()
            }
            if (innerRequest != null)
                handleClientRequests(innerRequest, oos)
        }
    }


    fun startSocketServerObjectTransfer() {
        serverSocket = null
        try {
            // Create the server socket
            serverSocket = ServerSocket(NetworkSettings.SERVER_OBJECT_TRANSFER_PORT)
            executor = CountingThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(),
                    30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

            // Wait for new incoming client connections
            while (!stopped) {
                println("Waiting for client-app request...")
                try {
                    val socket = serverSocket!!.accept()
                    lastRequestProcessed = System.currentTimeMillis()
                    println("Got client-app request...")
                    executor!!.execute(ClientRequestHandlerTask(socket))
                } catch (e: SocketException) {
                    // expected: another thread has called listener.close() to stop the server
                    println()
                }

            }
        } catch (e: Exception) {
            LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS,
                    "There is a problem in startSocketServerObjectTransfer: " + e.message)
            e.printStackTrace()
        } finally {
            try {
                if (serverSocket != null && !serverSocket!!.isClosed)
                    serverSocket!!.close()

                executor?.shutdown()
            } catch (ex: IOException) {
                ex.printStackTrace()
                System.err.println("Server socket died: " + ex.message)
            }

        }
    }


    private fun handleDecisionRequest(decisionRequest: DecisionRequest, oos: ObjectOutputStream) {
        println("Received a DecisionRequest")
        if (decisionRequest.codePosition == -1) {
            oos.writeObject(ServerResponse.getEmptyResponse())
            oos.flush()
            return
        }

        // Run the analyses
        val response = decisionMaker.resolveRequest(decisionRequest)
        val logMessage = String.format("[DECISION_REQUEST] %s \n [DECISION_RESPONSE] %s", decisionRequest, response)
        LogHelper.logEvent(MyLevel.DECISION_REQUEST_AND_RESPONSE, logMessage)

        println("Sending response to client-app...")
        println(response)

        // send the response to the client
        oos.writeObject(response)
        oos.flush()
    }


    private fun handlePathTracking(traceItem: PathTrackingTraceItem, oos: ObjectOutputStream) {
        println("Received a PathTrackingTraceItem")
        val codePositionUnit = decisionMaker.codePositionManager
                .getUnitForCodePosition(traceItem.lastExecutedStatement)
        val decision = traceItem.lastConditionalResult
        val mgr = decisionMaker.initializeHistory()
        if (mgr != null) {
            val currentClientHistory = mgr.getNewestClientHistory()
            if (codePositionUnit != null)
                currentClientHistory?.addPathTrace(codePositionUnit, decision)
        }
        oos.writeObject("PathTracking Ack")
        oos.flush()
    }


    private fun handleDynamicCallgraph(cgItem: AbstractDynamicCFGItem, oos: ObjectOutputStream) {
        LogHelper.logInfo("Received DynamicCallgraph")
        val currentManager = decisionMaker.initializeHistory()
        if (currentManager != null && decisionMaker.dynamicCallgraph != null) {
            decisionMaker.dynamicCallgraph!!.enqueueItem(cgItem)
        }

        oos.writeObject("DynamicCallGraph Ack")
        oos.flush()
    }


    private fun handleDexFileReceived(dexFileRequest: DexFileTransferTraceItem, oos: ObjectOutputStream) {
        LogHelper.logInfo("Received DexFileTransferTraceItem")
        val dexFile = dexFileRequest.dexFile
        try {
            // Write the received dex file to disk for debugging
            val timestamp = System.currentTimeMillis()
            val dirPath = String.format("%s/dexFiles/", UtilInstrumenter.SOOT_OUTPUT)
            val dir = File(dirPath)
            if (!dir.exists())
                dir.mkdir()
            val filePath = String.format("%s/dexFiles/%d_dexfile.dex", UtilInstrumenter.SOOT_OUTPUT, timestamp)
            println(String.format("DexFile: %s (code position: %d)", filePath,
                    dexFileRequest.lastExecutedStatement))
            LogHelper.logEvent(MyLevel.DEXFILE, String.format("Received dex file %s/dexFiles/%d_dexfile.dex",
                    UtilInstrumenter.SOOT_OUTPUT, timestamp))
            Files.write(Paths.get(filePath), dexFile)

            // We need to remove the statements that load the external code,
            // because we merge it into a single app. We must not take the
            // last executed statement, but the current one -> +1.
            val codePos = decisionMaker.codePositionManager.getCodePositionByID(dexFileRequest.lastExecutedStatement)
            val codePosUnit = decisionMaker.codePositionManager.getUnitForCodePosition(codePos)

            val statementsToRemove = HashSet<InstanceIndependentCodePosition>()
            statementsToRemove.add(InstanceIndependentCodePosition(codePos.enclosingMethod, codePos.lineNumber,
                    codePosUnit.toString()))

            // Register the new dex file and spawn an analysis task for it
            val dexFileObj = decisionMaker.dexFileManager.add(DexFile(dexFileRequest.fileName, filePath, dexFile))
            val taskManager = decisionMaker.analysisTaskManager
            val currentTask = taskManager.currentTask
            if (currentTask != null)
                taskManager.enqueueAnalysisTask(currentTask.deriveNewTask(dexFileObj, statementsToRemove))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        println("received dex file")

        oos.writeObject("DexFile Ack")
        oos.flush()
    }


    private fun handleDynamicValueReceived(dynamicValue: DynamicValueTraceItem, oos: ObjectOutputStream) {
        // Get the current trace
        val mgr = decisionMaker.initializeHistory()
        if (mgr != null) {
            val currentClientHistory = mgr.getNewestClientHistory()

            // Depending on the type of object received, we need to create a
            // different data object
            if (dynamicValue is DynamicStringValueTraceItem) {
                val stringValue = dynamicValue.stringValue
                if (stringValue != null && stringValue.isNotEmpty()) {
                    val `val` = DynamicStringValue(dynamicValue.getLastExecutedStatement(),
                            dynamicValue.getParamIdx(), stringValue)
                    currentClientHistory?.dynamicValues?.add(dynamicValue.getLastExecutedStatement(), `val`)
                }
            } else if (dynamicValue is DynamicIntValueTraceItem) {
                val `val` = DynamicIntValue(dynamicValue.getLastExecutedStatement(),
                        dynamicValue.getParamIdx(), dynamicValue.intValue)
                currentClientHistory?.dynamicValues?.add(dynamicValue.getLastExecutedStatement(), `val`)
            } else
                throw RuntimeException("Unknown trace item received from app")
        }

        oos.writeObject("DynamicValue Ack")
        oos.flush()
    }


    private fun handleTimingBombReceived(timingBomb: TimingBombTraceItem, oos: ObjectOutputStream) {
        LogHelper.logEvent(MyLevel.TIMING_BOMB, "Timing bomb, originally " + timingBomb.originalValue)

        oos.writeObject("TimingBomb Ack")
        oos.flush()
    }


    private fun handleGoalReached(grItem: TargetReachedTraceItem, oos: ObjectOutputStream) {
        decisionMaker.setTargetReached(true)
        LogHelper.logEvent(MyLevel.LOGGING_POINT_REACHED, "REACHED: " + grItem.lastExecutedStatement)

        oos.writeObject("GoalReached Ack")
        oos.flush()
    }


    private fun handleCrash(crash: CrashReportItem, oos: ObjectOutputStream) {
        LogHelper.logEvent(MyLevel.EXCEPTION_RUNTIME,
                String.format("%s | %s", crash.lastExecutedStatement, crash.exceptionMessage))
        val mgr = decisionMaker.initializeHistory()
        if (mgr != null) {
            val currentClientHistory = mgr.getNewestClientHistory()
            currentClientHistory?.crashException = crash.exceptionMessage
            System.err.println("Application crashed after " + crash.lastExecutedStatement)
        }

        oos.writeObject("Crash Ack")
        oos.flush()
    }

    fun notifyAppRunDone() {
        lastRequestProcessed = System.currentTimeMillis()
    }

    fun stop() {
        LogHelper.logInfo("Stopping socket server")
        stopped = true
        try {
            if (serverSocket != null) serverSocket!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    fun resetForNewRun() {
        lastRequestProcessed = System.currentTimeMillis()
    }

    companion object {

        private var socketServerInstance: SocketServer? = null

        fun getInstance(decisionMaker: DecisionMaker): SocketServer? {
            if (socketServerInstance == null)
                socketServerInstance = SocketServer(decisionMaker)

            // re-initialize status
            socketServerInstance?.stopped = false
            return socketServerInstance
        }
    }
}
