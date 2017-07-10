package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table

import soot.BooleanType
import soot.Unit
import soot.jimple.AssignStmt
import soot.jimple.IntConstant
import soot.jimple.InvokeExpr
import soot.jimple.Jimple
import soot.jimple.Stmt
import soot.jimple.infoflow.data.AccessPath
import soot.jimple.infoflow.results.ResultSourceInfo
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG
import soot.jimple.infoflow.source.data.SourceSinkDefinition
import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.appinstrumentation.transformer.InstrumentedCodeTag
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.DynamicValueInformation
import me.zhenhao.forced.decisionmaker.analysis.smartconstantdataextractor.NotYetSupportedException
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTProgram


class SMTConverter(private val sources: Set<SourceSinkDefinition>) {
    private var smtPrograms: MutableSet<SMTProgram> = HashSet()
    private val preparedDataFlowsForSMT: Set<ResultSourceInfo> = HashSet()
    private val dynamicValueInfo = HashMap<SMTProgram, Set<DynamicValueInformation>>()

    fun convertJimpleToSMT(jimpleDataFlowStatements: Array<Stmt>,
                           accessPathPath: Array<AccessPath>, targetUnits: Set<Unit>, cfg: IInfoflowCFG, splitInfos: Table<Stmt, Int, MutableSet<String>>?) {

        //if the path contains a condition (e.g. a = b.equals("aa")), we over-approximate here and add a new statement which
        //assigns once true (a = true) and once false (a = false) to the left hand side of the assignment
        //this is an over-approximation and is necessary for the SMT solver
        val indexesForConditions = ArrayList<Int>()
        for (i in jimpleDataFlowStatements.indices) {
            val stmt = jimpleDataFlowStatements[i]

            if (stmt is AssignStmt) {
                val assignment = stmt
                if (assignment.rightOp is InvokeExpr) {
                    val invoke = assignment.rightOp as InvokeExpr
                    if (!UtilInstrumenter.isAppDeveloperCode(invoke.method.declaringClass)) {
                        if (assignment.rightOp.type === BooleanType.v()) {
                            indexesForConditions.add(i)
                        }
                    }
                }
            }
        }

        val amountConditions = indexesForConditions.size

        val truthTable = generateTruthTable(amountConditions)

        val allDataFlows = ArrayList<List<Stmt>>()
        val allAccessPath = ArrayList<List<AccessPath>>()
        if (amountConditions == 0) {
            allDataFlows.add(Arrays.asList(*jimpleDataFlowStatements))
            allAccessPath.add(Arrays.asList(*accessPathPath))
        } else {
            for ((_, value) in truthTable) {
                val currentDataflow = ArrayList(Arrays.asList(*jimpleDataFlowStatements))
                val currentAccessPath = ArrayList(Arrays.asList(*accessPathPath))
                allDataFlows.add(currentDataflow)
                allAccessPath.add(currentAccessPath)

                for (i in 0..value.size - 1) {
                    val indexToAdd = indexesForConditions[i]

                    val conditionalStmt = jimpleDataFlowStatements[indexToAdd]
                    assert(conditionalStmt is AssignStmt)
                    val assignment = conditionalStmt as AssignStmt
                    val lhs = assignment.leftOp

                    val booleanValue = value[i]
                    val boolValueJimple = IntConstant.v(booleanValue)

                    val stmtToAdd = Jimple.v().newAssignStmt(lhs, boolValueJimple)

                    currentDataflow.add(indexToAdd + 1, stmtToAdd)
                    currentAccessPath.add(indexToAdd + 1, currentAccessPath[indexToAdd])
                }
            }
        }

        //###### end condition treatment #######

        //##### begin treatment of flows containing the split API method #####
        val splitAPIElementInfo = HashBasedTable.create<List<Stmt>, Stmt, List<List<String>>>()

        for (stmt in jimpleDataFlowStatements) {
            if (stmt.containsInvokeExpr()) {
                val inv = stmt.invokeExpr

                if (inv.method.signature == "<java.lang.String: java.lang.String[] split(java.lang.String)>") {
                    if (splitInfos != null && splitInfos.containsRow(stmt)) {
                        //count all combinations, e.g. key{0,1}, value{foo, bar} = 2 x 2 = 4
                        //this is important for generating different SMT programs. In this case 4 equal dataflows, but once
                        //the split-API call get converted into an SMT formula, we return one key-value combination, e.g. {0, foo}

                        val allElements = ArrayList<List<String>>()
                        val initPos = ArrayList<Int>()
                        for (index in splitInfos.row(stmt).keys) {
                            //add init value for position (will be used later)
                            initPos.add(0)

                            val splitValues = splitInfos.get(stmt, index).toList()
                            allElements.add(splitValues)
                        }
                        val resultElementCombinations = ArrayList<List<String>>()

                        generateAllSplitAPICombinations(allElements, initPos, resultElementCombinations)
                        val countCominations = resultElementCombinations.size

                        //prepare the dataflows:

                        //duplicate amount of countCombinations for dataflows. The correct values will be added to the dataflow once
                        //in the {@generateSMTSplitStmt} method.
                        for (i in 0..countCominations - 1 - 1) {
                            val currentDataflow = ArrayList(Arrays.asList(*jimpleDataFlowStatements))
                            val currentAccessPath = ArrayList(Arrays.asList(*accessPathPath))
                            allDataFlows.add(currentDataflow)
                            allAccessPath.add(currentAccessPath)

                            //add all necessary split-information for this particular dataflow
                            splitAPIElementInfo.put(currentDataflow, stmt, resultElementCombinations)
                        }
                    }
                }

            }
        }

        //##### end treatment of flows containing the split API method #####
        for (i in allDataFlows.indices) {
            val dataflow = allDataFlows[i]
            val accessPath = allAccessPath[i]
            val stmtVisitor = JimpleStmtVisitorImpl(sources, dataflow, accessPath, targetUnits, cfg, splitAPIElementInfo)
            try {
                for (stmt in dataflow) {
                    //does not make any sense to apply our own instrumented code
                    if (!stmt.hasTag(InstrumentedCodeTag.name)) {
                        stmt.apply(stmtVisitor)

                        //in case we do not support a specific statement yet, we will not produce any SMT program!
                        if (stmtVisitor.notSupported)
                            break
                    }
                }
                //in case we do not support a specific statement yet, we will not produce any SMT program!
                if (stmtVisitor.notSupported) {
                    LoggerHelper.logWarning("SMT formular is not generated!")
                    continue
                }
                smtPrograms.addAll(stmtVisitor.getSMTPrograms())

                //add the dataflow source info to the DynamicValueInformation
                for ((_, value) in stmtVisitor.getDynamicValueInfos()) {
                    for (valueInfo in value)
                        valueInfo.sourceOfDataflow = dataflow[0]
                }

                dynamicValueInfo.putAll(stmtVisitor.getDynamicValueInfos())

            } catch (ex: NotYetSupportedException) {
                LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                ex.printStackTrace()
            } catch (ex: Exception) {
                LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                ex.printStackTrace()
            }

        }
    }


    private fun generateAllSplitAPICombinations(allElements: List<List<String>>, currentPos: MutableList<Int>, result: MutableList<List<String>>) {
        val allValues = ArrayList<String>()
        for (i in currentPos.indices) {
            val posForListAtI = currentPos[i]
            val value = allElements[i][posForListAtI]
            allValues.add(value)
        }
        result.add(allValues)


        for (index in currentPos.indices.reversed()) {
            val currentValuePos = currentPos[index]

            val valuesAtPos = allElements[index]
            val sizeOfValuesAtPos = valuesAtPos.size

            if (currentValuePos < sizeOfValuesAtPos - 1) {
                currentPos[index] = currentValuePos + 1
                for (i in index + 1..currentPos.size - 1)
                    currentPos[i] = 0
                generateAllSplitAPICombinations(allElements, currentPos, result)
            }
        }
        return
    }


    private fun generateTruthTable(n: Int): Map<Int, List<Int>> {
        val truthTable = HashMap<Int, MutableList<Int>>()
        val rows = Math.pow(2.0, n.toDouble()).toInt()
        for (i in 0..rows - 1) {
            (n - 1 downTo 0)
                    .map { i / Math.pow(2.0, it.toDouble()).toInt() % 2 }
                    .forEach {
                        if (truthTable.keys.contains(i)) {
                            truthTable[i]!!.add(it)
                        } else {
                            val newList = ArrayList<Int>()
                            newList.add(it)
                            truthTable.put(i, newList)
                        }
                    }
        }
        return truthTable
    }

    fun printProgramToCmdLine() {
        for (smtProgram in smtPrograms)
            println(smtProgram.toString() + "\n")
    }

    fun getSmtPrograms(): Set<SMTProgram> {
        return smtPrograms
    }

    fun getDynamicValueInfos(): Map<SMTProgram, Set<DynamicValueInformation>> {
        return dynamicValueInfo
    }
}
