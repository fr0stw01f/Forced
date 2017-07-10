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
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.DecisionMaker
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.DynamicIntValue
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.DynamicStringValue
import me.zhenhao.forced.sharedclasses.crashreporter.CrashReportItem
import me.zhenhao.forced.sharedclasses.dynamiccfg.AbstractDynamicCFGItem
import me.zhenhao.forced.sharedclasses.networkconnection.CloseConnectionRequest
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest
import me.zhenhao.forced.sharedclasses.networkconnection.serializables.BinarySerializableObject
import me.zhenhao.forced.sharedclasses.tracing.DexFileTransferTraceItem
import me.zhenhao.forced.sharedclasses.tracing.DynamicIntValueTraceItem
import me.zhenhao.forced.sharedclasses.tracing.DynamicStringValueTraceItem
import me.zhenhao.forced.sharedclasses.tracing.DynamicValueTraceItem
import me.zhenhao.forced.sharedclasses.tracing.PathTrackingTraceItem
import me.zhenhao.forced.sharedclasses.tracing.TargetReachedTraceItem
import me.zhenhao.forced.sharedclasses.tracing.TimingBombTraceItem
import me.zhenhao.forced.sharedclasses.tracing.TraceItem
import me.zhenhao.forced.sharedclasses.util.NetworkSettings


class SocketServer private constructor(private val decisionMaker: DecisionMaker) {
    private var executor: CountingThreadPoolExecutor? = null


    var lastRequestProcessed = System.currentTimeMillis()
        private set

    @Volatile private var stopped = false
    @Volatile private var objectListener: ServerSocket? = null

    private inner class ClientHandlerObjectThread(private val socket: Socket) : Runnable {

        override fun run() {
            val ois: ObjectInputStream
            val oos: ObjectOutputStream

            try {
                var numAcks = 0

                // Only create the streams once for the full lifetime of the socket
                ois = ObjectInputStream(this.socket.getInputStream())
                oos = ObjectOutputStream(this.socket.getOutputStream())

                while (!socket.isClosed) {
                    val clientRequest = ois.readObject()

                    // For every trace item, register the last position
                    if (clientRequest is TraceItem) {
                        val manager = decisionMaker.initializeHistory()
                        if (manager != null) {
                            val currentClientHistory = manager.getNewestClientHistory()
                            if (currentClientHistory != null) {
                                currentClientHistory.addCodePosition(clientRequest.lastExecutedStatement,
                                        decisionMaker.codePositionManager)

                                // Make sure that our metrics are up to date
                                for (metric in decisionMaker.config.getProgressMetrics()) {
                                    metric.update(currentClientHistory)
                                }
                            }
                        }
                    }

                    numAcks += handleClientRequests(oos, clientRequest)

                    // Acknowledge that we have processed the items
                    if (numAcks > 0) {
                        while (numAcks > 0) {
                            numAcks--
                            oos.writeObject("ACK")
                        }
                        oos.flush()
                    }

                    // Make sure we send out all our data
                    oos.flush()

                    // Terminate the connection if requested
                    if (clientRequest is CloseConnectionRequest)
                        break
                }
            } catch (ex: Exception) {
                LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "There is a problem in the client-server communication " + ex.message)

                ex.printStackTrace()
            } finally {
                try {
                    if (!socket.isOutputShutdown)
                        socket.shutdownOutput()
                    if (!socket.isInputShutdown)
                        socket.shutdownInput()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    System.err.println("Network communication died: " + ex.message)
                }

            }
        }


        @Throws(IOException::class)
        private fun handleClientRequests(oos: ObjectOutputStream,
                                         clientRequest: Any): Int {
            var numAcks = 0
            if (clientRequest is PathTrackingTraceItem) {
                //						System.out.println("Received a PathTrackingTraceItem");
                handlePathTracking(clientRequest)
                numAcks++
            } else if (clientRequest is DecisionRequest) {
//there will be a hook in the dalvik part
                if (clientRequest.codePosition != -1) {
                    println("Received a DecisionRequest")
                    handleDecisionRequest(
                            clientRequest, oos)
                }
            } else if (clientRequest is CloseConnectionRequest) {
                println("Received a CloseConnectionRequest")
            } else if (clientRequest is AbstractDynamicCFGItem) {
                handleDynamicCallgraph(clientRequest)
                numAcks++
            } else if (clientRequest is TargetReachedTraceItem) {
                handleGoalReached(clientRequest)
                numAcks++
            } else if (clientRequest is CrashReportItem) {
                val crash = clientRequest
                handleCrash(crash)
                LoggerHelper.logEvent(MyLevel.EXCEPTION_RUNTIME, String.format("%s | %s", crash.lastExecutedStatement, crash.exceptionMessage))
                numAcks++
            } else if (clientRequest is DexFileTransferTraceItem) {
                LoggerHelper.logInfo("received DexFileTransferTraceItem")
                handleDexFileReceived(clientRequest)
                numAcks++
            } else if (clientRequest is DynamicValueTraceItem) {
                handleDynamicValueReceived(clientRequest)
                numAcks++
            } else if (clientRequest is TimingBombTraceItem) {
                handleTimingBombReceived(clientRequest)
                numAcks++
            } else if (clientRequest is BinarySerializableObject) {
                // Deserialize the contents of the binary object and recursively
                // process the request within.
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
                    handleClientRequests(oos, innerRequest)
                numAcks++
            } else
                throw RuntimeException("Received an unknown data item from the app")
            return numAcks
        }
    }


    fun startSocketServerObjectTransfer() {
        objectListener = null
        try {
            // Create the server socket
            objectListener = ServerSocket(NetworkSettings.SERVERPORT_OBJECT_TRANSFER)
            executor = CountingThreadPoolExecutor(1,
                    Runtime.getRuntime().availableProcessors(),
                    30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

            // Wait for new incoming client connections
            while (!stopped) {
                println("waiting for client-app request...")
                try {
                    val socket = objectListener!!.accept()
                    lastRequestProcessed = System.currentTimeMillis()
                    println("got client-app request...")
                    executor!!.execute(ClientHandlerObjectThread(socket))
                } catch (e: SocketException) {
                    // expected: another thread has called listener.close() to stop the server
                    println()
                }

            }
        } catch (e: Exception) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "There is a problem in startSocketServerObjectTransfer: " + e.message)
            e.printStackTrace()
        } finally {
            try {
                if (objectListener != null && !objectListener!!.isClosed)
                    objectListener!!.close()
                if (executor != null)
                    executor!!.shutdown()
            } catch (ex: IOException) {
                ex.printStackTrace()
                System.err.println("Server socket died: " + ex.message)
            }

        }
    }


    @Throws(IOException::class)
    private fun sendResponse(out: ObjectOutputStream, response: Any) {
        out.writeObject(response)
        out.flush()
        println("sending response to client-app...")
        println(response)
    }


    @Throws(IOException::class)
    private fun handleDecisionRequest(decisionRequest: DecisionRequest,
                                      oos: ObjectOutputStream) {
        // Run the analyses
        val response = decisionMaker.resolveRequest(decisionRequest)
        val logMessage = String.format("[DECISION_REQUEST] %s \n [DECISION_RESPONSE] %s", decisionRequest, response)
        LoggerHelper.logEvent(MyLevel.DECISION_REQUEST_AND_RESPONSE, logMessage)

        // send the response to the client
        sendResponse(oos, response)
    }


    private fun handlePathTracking(pathTrackigTrace: PathTrackingTraceItem) {
        val codePostionUnit = decisionMaker.codePositionManager
                .getUnitForCodePosition(pathTrackigTrace.lastExecutedStatement)
        val decision = pathTrackigTrace.lastConditionalResult
        val mgr = decisionMaker.initializeHistory()
        if (mgr != null) {
            val currentClientHistory = mgr.getNewestClientHistory()
            if (codePostionUnit != null)
                currentClientHistory?.addPathTrace(codePostionUnit, decision)
        }
    }


    private fun handleCrash(crash: CrashReportItem) {
        val mgr = decisionMaker.initializeHistory()
        if (mgr != null) {
            val currentClientHistory = mgr.getNewestClientHistory()
            currentClientHistory?.crashException = crash.exceptionMessage
            System.err.println("Application crashed after " + crash.lastExecutedStatement)
        }
    }


    private fun handleDynamicCallgraph(cgItem: AbstractDynamicCFGItem) {
        val currentManager = decisionMaker.initializeHistory()
        if (currentManager != null && decisionMaker.dynamicCallgraph != null) {
            decisionMaker.dynamicCallgraph!!.enqueueItem(cgItem)
        }
    }


    private fun handleDexFileReceived(dexFileRequest: DexFileTransferTraceItem) {
        val dexFile = dexFileRequest.dexFile
        try {
            // Write the received dex file to disk for debugging
            val timestamp = System.currentTimeMillis()
            val dirPath = String.format("%s/dexFiles/", UtilInstrumenter.SOOT_OUTPUT)
            val dir = File(dirPath)
            if (!dir.exists())
                dir.mkdir()
            val filePath = String.format("%s/dexFiles/%d_dexfile.dex", UtilInstrumenter.SOOT_OUTPUT, timestamp)
            println(String.format("Dex-File: %s (code position: %d)", filePath,
                    dexFileRequest.lastExecutedStatement))
            LoggerHelper.logEvent(MyLevel.DEXFILE, String.format("Received dex-file %s/dexFiles/%d_dexfile.dex", UtilInstrumenter.SOOT_OUTPUT, timestamp))
            Files.write(Paths.get(filePath), dexFile)

            // We need to remove the statements that load the external code,
            // because we merge it into a single app. We must not take the
            // last executed statement, but the current one -> +1.
            val codePos = decisionMaker.codePositionManager
                    .getCodePositionByID(dexFileRequest
                            .lastExecutedStatement)
            val codePosUnit = decisionMaker.codePositionManager.getUnitForCodePosition(codePos)

            val statementsToRemove = HashSet<InstanceIndependentCodePosition>()
            statementsToRemove.add(InstanceIndependentCodePosition(codePos.enclosingMethod,
                    codePos.lineNumber, codePosUnit.toString()))

            // Register the new dex file and spawn an analysis task for it
            val dexFileObj = decisionMaker.dexFileManager.add(DexFile(
                    dexFileRequest.fileName, filePath, dexFile))
            val taskManager = decisionMaker.analysisTaskManager
            val currentTask = taskManager.currentTask
            if (currentTask != null)
                taskManager.enqueueAnalysisTask(currentTask.deriveNewTask(dexFileObj, statementsToRemove))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        println("received dex file")
    }


    private fun handleDynamicValueReceived(dynamicValue: DynamicValueTraceItem) {
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
    }


    private fun handleTimingBombReceived(timingBomb: TimingBombTraceItem) {
        LoggerHelper.logEvent(MyLevel.TIMING_BOMB, "Timing bomb, originally " + timingBomb.originalValue)
    }


    private fun handleGoalReached(grItem: TargetReachedTraceItem) {
        decisionMaker.setTargetReached(true)
        LoggerHelper.logEvent(MyLevel.LOGGING_POINT_REACHED, "REACHED: " + grItem.lastExecutedStatement)
    }

    fun notifyAppRunDone() {
        lastRequestProcessed = System.currentTimeMillis()
    }

    fun stop() {
        LoggerHelper.logInfo("Stopping socket server")
        stopped = true
        try {
            if (objectListener != null) objectListener!!.close()
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
