package me.zhenhao.forced.decisionmaker.analysis.smartconstantdataextractor

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.decisionmaker.analysis.FuzzyAnalysis
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.*
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.SMTConverter
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.SMTExecutor
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTAssertStatement
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTConstantValue
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTProgram
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTSimpleAssignment
import me.zhenhao.forced.decisionmaker.server.ThreadTraceManager
import me.zhenhao.forced.decisionmaker.server.TraceManager
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory
import me.zhenhao.forced.shared.networkconnection.DecisionRequest
import me.zhenhao.forced.shared.networkconnection.ServerResponse
import org.apache.commons.codec.binary.Hex
import soot.Scene
import soot.Unit
import soot.jimple.ArrayRef
import soot.jimple.AssignStmt
import soot.jimple.IntConstant
import soot.jimple.Stmt
import soot.jimple.infoflow.Infoflow
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder
import soot.jimple.infoflow.handlers.ResultsAvailableHandler
import soot.jimple.infoflow.results.InfoflowResults
import soot.jimple.infoflow.results.ResultSourceInfo
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG
import soot.jimple.infoflow.source.ISourceSinkManager
import soot.jimple.infoflow.source.data.SourceSinkDefinition
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*


class SmartConstantDataExtractorFuzzyAnalysis : FuzzyAnalysis() {

    internal var codePositionManager = CodePositionManager.codePositionManagerInstance
    internal var constantBasedValuesToFuzz: MutableMap<Int, MutableSet<Any>> = HashMap()
    internal var dynamicValueBasedValuesToFuzz: MutableMap<Int, MutableSet<Any>> = HashMap()

    internal var dataFlowsToSMTPrograms: MutableMap<DataFlowObject, Set<SMTProgram>> = HashMap()
    private val dynamicValueInfos = HashMap<SMTProgram, Set<DynamicValueInformation>>()
    private val dynValuesOfRuns = HashSet<Set<DynamicValue>>()
    private val staticValuesSent = HashSet<Int>()

    private inner class InplaceInfoflow : Infoflow() {
        public override fun runAnalysis(sourcesSinks: ISourceSinkManager) {
            super.runAnalysis(sourcesSinks)
        }
    }

    override fun getAnalysisName(): String {
        return "SymbolicDataExtractor"
    }

    private inner class FuzzerResultsAvailableHandler(private val sources: MutableSet<SourceSinkDefinition>,
                                                      private val targetUnits: Set<Unit>) : ResultsAvailableHandler {

        override fun onResultsAvailable(cfg: IInfoflowCFG, results: InfoflowResults) {
            println("############################# RESULTS: " + results.results.keySet().size)
            val smtPreparation = SMTPreparationPhase(cfg, results)
            val preparedDataFlowsForSMT = smtPreparation.prepareDataFlowPathsForSMTConverter()

            //pre-run for split methods
            val splitInfos = HashBasedTable.create<Stmt, Int, MutableSet<String>>()
            for (dataFlow in preparedDataFlowsForSMT) {
                //pre-analysis especially for the split api call
                if (dataFlow.path[0].containsInvokeExpr()) {
                    val inv = dataFlow.path[0].invokeExpr
                    //special treatment in case of a dataflow starting with a split method
                    if (inv.method.signature == "<java.lang.String: java.lang.String[] split(java.lang.String)>") {

                        //we remove the split-API method from the source list
                        val iterator = this.sources.iterator()
                        while (iterator.hasNext()) {
                            val source = iterator.next()
                            if (source.method.signature == "<java.lang.String: java.lang.String[] split(java.lang.String)>")
                                iterator.remove()
                        }

                        splitAPI_DataFlowtoSMTConvertion(dataFlow, cfg, preparedDataFlowsForSMT, splitInfos)
                    }
                }
            }

            //actual run:
            for (dataFlow in preparedDataFlowsForSMT) {
                if (dataFlow.path[0].containsInvokeExpr()) {
                    val inv = dataFlow.path[0].invokeExpr
                    //standard case
                    if (inv.method.signature != "<java.lang.String: java.lang.String[] split(java.lang.String)>") {
                        standardDataFlowToSMTConvertion(dataFlow, cfg, preparedDataFlowsForSMT, splitInfos)
                    }
                }
            }

        }


        private fun standardDataFlowToSMTConvertion(dataFlow: ResultSourceInfo, cfg: IInfoflowCFG, preparedDataFlowsForSMT: Set<ResultSourceInfo>, splitInfos: Table<Stmt, Int, MutableSet<String>>) {
            val converter = SMTConverter(sources)
            for (i in 0..dataFlow.path.size - 1) {
                println("\t" + dataFlow.path[i])
                println("\t\t" + dataFlow.pathAccessPaths[i])
            }

            converter.convertJimpleToSMT(dataFlow.path,
                    dataFlow.pathAccessPaths, targetUnits, cfg, splitInfos)

            dataFlowsToSMTPrograms.put(DataFlowObject(dataFlow.path), converter.getSmtPrograms())

            //dynamic value information
            dynamicValueInfos.putAll(converter.getDynamicValueInfos())

            converter.printProgramToCmdLine()

            val z3str2Script = File(FrameworkOptions.z3scriptLocation)
            if (!z3str2Script.exists())
                throw RuntimeException("There is no z3-script available")
            val smtExecutor = SMTExecutor(converter.getSmtPrograms(), z3str2Script)
            val smtFiles = smtExecutor.createSMTFile()

            val values = HashSet<Any>()
            for (smtFile in smtFiles) {
                if (smtFile == null)
                    continue

                var loggingPointValue = smtExecutor.executeZ3str2ScriptAndExtractLoggingPointValue(smtFile)
                if (loggingPointValue != null) {
                    loggingPointValue = fixSMTSolverIntegerOutput(loggingPointValue, dataFlow.path[0])

                    //SMT solver only returns hex-based UTF-8 values in some cases; we fixed this with our own hexToUnicode converter
                    if (loggingPointValue.contains("\\x"))
                        addAdditionalUnicodeValue(loggingPointValue, values)

                    values.add(loggingPointValue)
                    println(String.format("Extracted logging point value: %s", loggingPointValue))
                }
            }

            println("####################################")

            //add values to fuzzy-seed
            val stmt = dataFlow.source
            val position = codePositionManager.getCodePositionForUnit(stmt)
            if (constantBasedValuesToFuzz.containsKey(position.id))
                constantBasedValuesToFuzz[position.id]!!.addAll(values)
            else
                constantBasedValuesToFuzz.put(position.id, values)
        }


        private fun splitAPI_DataFlowtoSMTConvertion(dataFlow: ResultSourceInfo, cfg: IInfoflowCFG, preparedDataFlowsForSMT: Set<ResultSourceInfo>, splitInfos: Table<Stmt, Int, MutableSet<String>>) {
            val converter = SMTConverter(sources)
            for (i in 0..dataFlow.path.size - 1) {
                println("\t" + dataFlow.path[i])
                println("\t\t" + dataFlow.pathAccessPaths[i])
            }

            //we remove the first statement (split-API method)
            val n = dataFlow.path.size - 1
            val reducedDataFlow = arrayOfNulls<Stmt>(n)
            System.arraycopy(dataFlow.path, 1, reducedDataFlow, 0, n)

            //currently only possible if there is a constant index for the array
            if (hasConstantIndexAtArrayForSplitDataFlow(reducedDataFlow as Array<Stmt>)) {
                val valueOfInterest = getValueOfInterestForSplitDataflow(reducedDataFlow)

                converter.convertJimpleToSMT(reducedDataFlow,
                        dataFlow.pathAccessPaths, targetUnits, cfg, null)

                converter.printProgramToCmdLine()

                val z3str2Script = File(FrameworkOptions.z3scriptLocation)
                if (!z3str2Script.exists())
                    throw RuntimeException("There is no z3-script available")
                val smtExecutor = SMTExecutor(converter.getSmtPrograms(), z3str2Script)
                val smtFiles = smtExecutor.createSMTFile()

                for (smtFile in smtFiles) {
                    if (smtFile == null)
                        continue

                    val loggingPointValue = smtExecutor.executeZ3str2ScriptAndExtractValue(smtFile, valueOfInterest)
                    if (loggingPointValue != null) {
                        val splitStmt = dataFlow.path[0]
                        val index = getConstantArrayIndexForSplitDataFlow(reducedDataFlow)

                        if (splitInfos.contains(splitStmt, index))
                            splitInfos.get(splitStmt, index).add(loggingPointValue)
                        else {
                            val values = HashSet<String>()
                            values.add(loggingPointValue)
                            splitInfos.put(splitStmt, index, values)
                        }
                    }
                    println(loggingPointValue)
                }

                println("####################################")
            }
        }
    }


    private fun getValueOfInterestForSplitDataflow(dataflow: Array<Stmt>): String {
        val firstAssign = dataflow[0]
        if (firstAssign is AssignStmt) {
            return firstAssign.leftOp.toString()
        } else
            throw RuntimeException("this should not happen - wrong assumption")
    }


    private fun hasConstantIndexAtArrayForSplitDataFlow(dataflow: Array<Stmt>): Boolean {
        val firstAssign = dataflow[0]
        if (firstAssign is AssignStmt) {
            val value = firstAssign.rightOp
            if (value is ArrayRef) {
                val index = value.index

                if (index is IntConstant)
                    return true
            }
        } else
            throw RuntimeException("this should not happen - wrong assumption")

        return false
    }

    private fun getConstantArrayIndexForSplitDataFlow(dataflow: Array<Stmt>): Int {
        val firstAssign = dataflow[0]
        if (firstAssign is AssignStmt) {
            val value = firstAssign.rightOp
            if (value is ArrayRef) {
                val index = value.index

                if (index is IntConstant)
                    return index.value
            }
        } else
            throw RuntimeException("this should not happen - wrong assumption")

        return -1
    }

    override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {
        //necessary for update information once a new dynamic value is available
        //        traceManager.addThreadTraceCreateHandler(new ThreadTraceManagerCreatedHandler() {
        //
        //            @Override
        //            public void onThreadTraceManagerCreated(
        //                    final ThreadTraceManager threadTraceManager) {
        //                threadTraceManager.addOnCreateHandler(new ClientHistoryCreatedHandler() {
        //
        //                    @Override
        //                    public void onClientHistoryCreated(ClientHistory history) {
        //                        DynamicValueWorker worker = new DynamicValueWorker(threadTraceManager);
        //                        history.getDynamicValues().addDynamicValueUpdateHandler(worker);
        //                    }
        //                });
        //            }
        //        });

        runAnalysis(targetUnits)
    }

    override fun resolveRequest(clientRequest: DecisionRequest, threadTraceManager: ThreadTraceManager): List<AnalysisDecision> {
        val decisions = ArrayList<AnalysisDecision>()
        var codePosition = clientRequest.codePosition
        //Todo: why is there an 1 offset?
        codePosition += 1

        val history = threadTraceManager.getLastClientHistory()
        val dynValueCheckNecessary = areNewDynValuesOfHistory(history)

        //are there dynamic values available? static values are sent for the first request (so decision maker has all static values already)
        if (dynValueCheckNecessary && history != null) {
            dynValuesOfRuns.add(history.dynamicValues.getValues())
            val SMT_ADDITIONAL_COMPUTATION_TIME = 3 * 60
            val dynamicValues = history.dynamicValues
            FrameworkOptions.forceTimeout = FrameworkOptions.forceTimeout + SMT_ADDITIONAL_COMPUTATION_TIME
            //depending on the argPos or the baseObject, we need to create a new value
            val updateInfo = getSMTProgramUpdateInfos(dynamicValues)
            if (!updateInfo.isEmpty) {
                for (smtProgram in updateInfo.rowKeySet()) {
                    val allAssertCombinations = getAllAssertCombinations(updateInfo.row(smtProgram))

                    for (assertCombi in allAssertCombinations) {
                        var sourceOfDataflow: Stmt? = null
                        for (assertInfo in assertCombi) {
                            smtProgram.addAssertStatement(assertInfo.assertionStmt)
                            if (sourceOfDataflow == null)
                                sourceOfDataflow = assertInfo.sourceOfDataflow
                            if (sourceOfDataflow != null) {
                                if (sourceOfDataflow.toString() != assertInfo.sourceOfDataflow.toString())
                                    LoggerHelper.logWarning("sourceOfDataflow have to be the same all the time!")
                            }
                        }

                        val z3str2Script = File(FrameworkOptions.z3scriptLocation)
                        if (!z3str2Script.exists())
                            throw RuntimeException("There is no z3-script available")
                        val smtExecutor = SMTExecutor(setOf(smtProgram), z3str2Script)
                        val smtFiles = smtExecutor.createSMTFile()

                        //we need to remove it. if there are more dynamic values available, we need to get the clean
                        //old program for the solver
                        for (assertInfo in assertCombi) {
                            smtProgram.removeAssertStatement(assertInfo.assertionStmt)
                        }

                        for (smtFile in smtFiles) {
                            if (smtFile == null)
                                continue

                            val loggingPointValue = smtExecutor.executeZ3str2ScriptAndExtractLoggingPointValue(smtFile)
                            if (loggingPointValue != null) {
                                if (isSemanticallyCorrect(loggingPointValue, sourceOfDataflow!!)) {
                                    println(loggingPointValue)
                                    assertCombi
                                            .map {
                                                //add values to fuzzy-seed
                                                it.sourceOfDataflow
                                            }
                                            .map { codePositionManager.getCodePositionForUnit(it) }
                                            .forEach {
                                                if (dynamicValueBasedValuesToFuzz.containsKey(it.id))
                                                    dynamicValueBasedValuesToFuzz[it.id]!!.add(loggingPointValue)
                                                else {
                                                    val values = HashSet<Any>()
                                                    values.add(loggingPointValue)
                                                    dynamicValueBasedValuesToFuzz.put(it.id, values)
                                                }
                                            }

//                                  //SMT solver only returns hex-based UTF-8 values in some cases; we fixed this with our own hexToUnicode converter
//                                    if(loggingPointValue != null && loggingPointValue.contains("\\x"))
//                                        addAdditionalUnicodeValue(loggingPointValue, values);
//                                    if(loggingPointValue != null)
//                                        values.add(loggingPointValue);
//                                    System.out.println(String.format("Extracted NEW DYNAMIC-BASED loggingpoint-value: %s", loggingPointValue));

                                    assertCombi
                                            .map {
                                                //add values to fuzzy-seed
                                                it.sourceOfDataflow
                                            }
                                            .map { codePositionManager.getCodePositionForUnit(it) }
                                            .forEach {
                                                if (dynamicValueBasedValuesToFuzz.containsKey(it.id))
                                                    dynamicValueBasedValuesToFuzz[it.id]!!.add(loggingPointValue)
                                                else {
                                                    val values = HashSet<Any>()
                                                    values.add(loggingPointValue)
                                                    dynamicValueBasedValuesToFuzz.put(it.id, values)
                                                }
                                            }
                                }
                            }

                        }
                    }
                }
            }

            if (dynamicValueBasedValuesToFuzz.containsKey(codePosition)) {
                //we return all extracted values at once!
                val valueIt = dynamicValueBasedValuesToFuzz[codePosition]!!.iterator()
                while (valueIt.hasNext()) {
                    val valueToFuzz = valueIt.next()
                    LoggerHelper.logEvent(MyLevel.SMT_SOLVER_VALUE, "<---- dyn-values (first run) : " + valueToFuzz)
                    val sResponse = ServerResponse()
                    sResponse.analysisName = getAnalysisName()
                    sResponse.setResponseExist(true)
                    sResponse.returnValue = valueToFuzz
                    val finalDecision = AnalysisDecision()
                    finalDecision.analysisName = getAnalysisName()
                    finalDecision.decisionWeight = 12
                    finalDecision.serverResponse = sResponse
                    decisions.add(finalDecision)
                }
            }

            FrameworkOptions.forceTimeout = FrameworkOptions.forceTimeout - SMT_ADDITIONAL_COMPUTATION_TIME

        } else if (dynamicValueBasedValuesToFuzz.containsKey(codePosition)) {
            //we return all extracted values at once!
            val valueIt = dynamicValueBasedValuesToFuzz[codePosition]!!.iterator()
            while (valueIt.hasNext()) {
                val valueToFuzz = valueIt.next()
                LoggerHelper.logEvent(MyLevel.SMT_SOLVER_VALUE, "<---- dyn-values: " + valueToFuzz)
                val sResponse = ServerResponse()
                sResponse.setResponseExist(true)
                sResponse.analysisName = getAnalysisName()
                sResponse.returnValue = valueToFuzz
                val finalDecision = AnalysisDecision()
                finalDecision.analysisName = getAnalysisName()
                finalDecision.decisionWeight = 12
                finalDecision.serverResponse = sResponse
                decisions.add(finalDecision)
            }
        } else if (constantBasedValuesToFuzz.containsKey(codePosition) && !staticValuesAlreadySend(codePosition)) {
            staticValuesSent.add(codePosition)
            //we return all extracted values at once!
            val valueIt = constantBasedValuesToFuzz[codePosition]!!.iterator()
            while (valueIt.hasNext()) {
                val valueToFuzz = valueIt.next()
                LoggerHelper.logEvent(MyLevel.SMT_SOLVER_VALUE, "<---- static-values: " + valueToFuzz)
                val sResponse = ServerResponse()
                sResponse.setResponseExist(true)
                sResponse.analysisName = getAnalysisName()
                sResponse.returnValue = valueToFuzz
                val finalDecision = AnalysisDecision()
                finalDecision.analysisName = getAnalysisName()
                finalDecision.decisionWeight = 8
                finalDecision.serverResponse = sResponse
                decisions.add(finalDecision)
            }
        }//second all constant-based values

        //no decision found
        if (decisions.isEmpty()) {
            val sResponse = ServerResponse()
            sResponse.setResponseExist(false)
            sResponse.analysisName = getAnalysisName()
            val noDecision = AnalysisDecision()
            noDecision.analysisName = getAnalysisName()
            noDecision.decisionWeight = 8
            noDecision.serverResponse = sResponse
            return listOf(noDecision)
        }

        return decisions
    }


    private fun areNewDynValuesOfHistory(history: ClientHistory?): Boolean {
        var dynValueCheckNecessary = true
        if (history != null) {
            val currValues = history.dynamicValues.getValues()
            for (values in dynValuesOfRuns) {
                for (value in values) {
                    if (!currValues.contains(value))
                        break
                }
                dynValueCheckNecessary = false
            }
        }
        return dynValueCheckNecessary
    }


    private fun staticValuesAlreadySend(codePosition: Int): Boolean {
        return staticValuesSent.contains(codePosition)
    }


    private fun addAdditionalUnicodeValue(loggingPointValue: String, values: MutableSet<Any>) {
        val delim = "#################################################################################################################"
        val allStrings = ArrayList<String>()
        val hexValues = ArrayList<String>()

        var currentHexValue = ""
        var currentNormalString = ""

        var i = 0
        while (i < loggingPointValue.length) {
            val c = loggingPointValue[i]
            if (c == '\\') {
                if (loggingPointValue[i + 1] == 'x') {
                    if (currentNormalString.isNotEmpty()) {
                        allStrings.add(currentNormalString)
                        currentNormalString = ""
                    }
                    i += 2
                    //look ahead
                    currentHexValue += loggingPointValue[i]
                    ++i
                    currentHexValue += loggingPointValue[i]
                } else {
                    if (currentHexValue.isNotEmpty()) {
                        hexValues.add(currentHexValue)
                        allStrings.add(delim)
                        currentHexValue = ""
                    }
                    currentNormalString += c

                }
            } else {
                if (currentHexValue.isNotEmpty()) {
                    hexValues.add(currentHexValue)
                    allStrings.add(delim)
                    currentHexValue = ""
                }
                currentNormalString += c
            }

            //last values
            if (i + 1 == loggingPointValue.length) {
                if (currentHexValue.isNotEmpty()) {
                    hexValues.add(currentHexValue)
                    allStrings.add(delim)
                    currentHexValue = ""
                }
                if (currentNormalString.isNotEmpty()) {
                    allStrings.add(currentNormalString)
                    currentNormalString = ""
                }
            }
            i++

        }


        for (hexValue in hexValues) {
            val tmp1: ByteArray
            try {
                tmp1 = Hex.decodeHex(hexValue.toCharArray())
                val newValue = tmp1.toString(StandardCharsets.UTF_8)
                //newValue = String(tmp1, "UTF-8")
                val replaceIndex = allStrings.indexOf(delim)
                allStrings[replaceIndex] = newValue
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        var newValue = ""
        for (string in allStrings)
            newValue += string


        if (newValue != "") {
            values.add(newValue)
            println(String.format("Extracted logging point value: %s", newValue))
        }
    }


    private fun runAnalysis(targetUnits: Set<Unit>) {
        try {
            Scene.v().orMakeFastHierarchy

            val infoflow = InplaceInfoflow()
            //            InfoflowConfiguration.setAccessPathLength(2);
            infoflow.setPathBuilderFactory(DefaultPathBuilderFactory(
                    PathBuilder.ContextSensitive, true))
            infoflow.taintWrapper = EasyTaintWrapper(TAINT_WRAPPER_PATH)
            infoflow.config.enableExceptionTracking = false
            infoflow.config.enableArraySizeTainting = false
            //            infoflow.getConfig().setCallgraphAlgorithm(CallgraphAlgorithm.CHA);

            println("Running data flow analysis...")
            val pmp = PermissionMethodParser.fromFile(SOURCES_SINKS_FILE)
            val srcSinkManager = AccessPathBasedSourceSinkManager(pmp.sources, pmp.sinks)

            infoflow.addResultsAvailableHandler(FuzzerResultsAvailableHandler(pmp.sources,
                    targetUnits))
            infoflow.runAnalysis(srcSinkManager)
        } catch (ex: IOException) {
            throw RuntimeException("Could not read source/sink file", ex)
        }

    }

    override fun reset() {

    }


    private fun fixSMTSolverIntegerOutput(loggingPoint: String, stmt: Stmt): String {
        if (stmt.containsInvokeExpr()) {
            val inv = stmt.invokeExpr
            val metSig = inv.method.signature
            if (metSig == "<android.telephony.TelephonyManager: java.lang.String getSimOperator()>" || metSig == "<android.telephony.TelephonyManager: java.lang.String getNetworkOperator()>") {
                var newLoggingPoint = ""
                for (c in loggingPoint.toCharArray()) {
                    if (c < '0' || c > '9') {
                        val rand = Random()
                        val num = rand.nextInt(10)
                        newLoggingPoint += num
                    } else
                        newLoggingPoint += c
                }
                return newLoggingPoint
            }
        }
        return loggingPoint
    }


    private fun isSemanticallyCorrect(loggingPoint: String?, stmt: Stmt): Boolean {
        if (loggingPoint == null)
            return false
        if (stmt.containsInvokeExpr()) {
            val inv = stmt.invokeExpr
            val metSig = inv.method.signature
            if (metSig == "<android.telephony.TelephonyManager: java.lang.String getSimOperator()>" || metSig == "<android.telephony.TelephonyManager: java.lang.String getNetworkOperator()>") {
                loggingPoint.toCharArray()
                        .filter { it < '0' || it > '9' }
                        .forEach { return false }
            }
        }
        return true
    }


    private fun getSMTProgramUpdateInfos(dynamicValues: DynamicValueContainer): Table<SMTProgram, Stmt, MutableList<Pair<DynamicValueInformation, DynamicValue>>> {
        val updateInfoTable = HashBasedTable.create<SMTProgram, Stmt, MutableList<Pair<DynamicValueInformation, DynamicValue>>>()

        for (value in dynamicValues.getValues()) {
            val unit = codePositionManager.getUnitForCodePosition(value.codePosition + 1)
            val paramIdx = value.paramIdx

            for ((key, value1) in dynamicValueInfos) {
                value1
                        .filter { it.statement == unit }
                        .forEach {
                            //base object
                            if (paramIdx == -1) {
                                if (it.isBaseObject) {
                                    if (!updateInfoTable.contains(key, it.statement))
                                        updateInfoTable.put(key, it.statement, ArrayList<Pair<DynamicValueInformation, DynamicValue>>())
                                    updateInfoTable.get(key, it.statement).add(Pair(it, value))

                                }
                            } else {
                                if (it.argPos == paramIdx) {
                                    if (!updateInfoTable.contains(key, it.statement))
                                        updateInfoTable.put(key, it.statement, ArrayList<Pair<DynamicValueInformation, DynamicValue>>())
                                    updateInfoTable.get(key, it.statement).add(Pair(it, value))
                                }
                            }//method arguments
                        }
            }
        }

        return updateInfoTable
    }


    private fun getAllAssertCombinations(map: Map<Stmt, List<Pair<DynamicValueInformation, DynamicValue>>>): Set<Set<SMTUpdateInfo>> {
        val allAssertions = HashSet<Set<SMTUpdateInfo>>()
        val currentPos = IntArray(map.keys.size)
        val keys = ArrayList(map.keys)
        val maxSize = keys.map { map[it]!!.size }

        val allPermutations = ArrayList<IntArray>()
        generateAllPermutations(maxSize, currentPos, currentPos.size - 1, allPermutations)


        for (combinations in allPermutations) {
            val currentAssertions = HashSet<SMTUpdateInfo>()
            for (i in combinations.indices) {
                val stmt = keys[i]
                val valueInfo = map[stmt]!![combinations[i]]

                var assignment: SMTSimpleAssignment? = null
                val dynValue = valueInfo.second
                val bindingToUpdate = valueInfo.first.binding
                if (dynValue is DynamicStringValue) {
                    val stringValue = dynValue.stringValue
                    assignment = SMTSimpleAssignment(bindingToUpdate, SMTConstantValue(stringValue))
                } else if (dynValue is DynamicIntValue) {
                    val intValue = dynValue.intValue
                    assignment = SMTSimpleAssignment(bindingToUpdate, SMTConstantValue(intValue))
                }

                val assignAssert = SMTAssertStatement(assignment)
                currentAssertions.add(SMTUpdateInfo(assignAssert, stmt, valueInfo.first.sourceOfDataflow!!))
            }
            allAssertions.add(currentAssertions)
        }

        return allAssertions
    }


    fun generateAllPermutations(maxSize: List<Int>, currArray: IntArray, currIndex: Int, allPermutations: MutableList<IntArray>) {
        var currIndexVar = currIndex
        if (currIndexVar == -1)
            return
        val startPos = currArray.size - 1
        var currValue = currArray[startPos]
        if (currValue + 1 < maxSize[startPos]) {
            currArray[startPos] = currArray[startPos] + 1
            allPermutations.add(currArray.clone())
        } else {
            currValue = currArray[currIndexVar]
            //increment index
            if (currValue + 1 < maxSize[currIndexVar]) {
                currArray[currIndexVar] = currArray[currIndexVar] + 1
                for (i in currIndexVar + 1..currArray.size - 1)
                    currArray[i] = 0
                allPermutations.add(currArray.clone())
            } else {
                //find next index to update
                while (currIndexVar >= 0) {
                    currValue = currArray[currIndexVar]
                    if (currValue + 1 < maxSize[currIndexVar])
                        break
                    currIndexVar--
                }
                if (currIndexVar == -1)
                    return
                currArray[currIndexVar] = currArray[currIndexVar] + 1
                for (i in currIndexVar + 1..currArray.size - 1)
                    currArray[i] = 0
                currIndexVar = currArray.size - 1
                allPermutations.add(currArray.clone())
            }
        }

        generateAllPermutations(maxSize, currArray, currIndexVar, allPermutations)

    }

    companion object {

        private val TAINT_WRAPPER_PATH = FrameworkOptions.frameworkDir + "src/me/zhenhao/forced/decisionmaker/analysis/EasyTaintWrapperSource.txt"
        private val SOURCES_SINKS_FILE = FrameworkOptions.frameworkDir + "src/me/zhenhao/forced/decisionmaker/analysis/smartconstantdataextractor/SourcesAndSinks.txt"
    }
}
