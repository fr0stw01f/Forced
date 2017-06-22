package de.tu_darmstadt.sse.decisionmaker.analysis.symbolicexecution

import de.tu_darmstadt.sse.appinstrumentation.UtilInstrumenter
import de.tu_darmstadt.sse.appinstrumentation.transformer.InstrumentedCodeTag
import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import soot.Unit
import soot.jimple.*
import soot.jimple.infoflow.results.ResultSourceInfo
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.graph.MHGPostDominatorsFinder
import soot.toolkits.graph.UnitGraph
import soot.toolkits.scalar.SimpleLocalDefs
import soot.toolkits.scalar.SimpleLocalUses
import java.util.*


object UtilSMT {

    fun extractConditionForReachingAUnit(cfg: IInfoflowCFG, conditionStmt: Stmt, targetUnits: Set<Unit>): Set<Boolean> {
        var firstConditionVisited = false

        var currentBranchTaken = -1
        var branchCondition: BinopExpr? = null
        var elseBranchStmt: Unit? = null
        val reachedUnits = HashSet<Unit>()
        val worklist = Stack<Unit>()
        worklist.add(conditionStmt)
        //		worklist.addAll(cfg.getSuccsOf(conditionStmt));
        val conditionsToReturn = HashSet<Boolean>()

        while (!worklist.isEmpty()) {
            val currentUnit = worklist.pop()

            //we always take the then branch first, therefore, there else branch comes last
            if (currentUnit === elseBranchStmt)
                currentBranchTaken = 0

            if (targetUnits.contains(currentUnit)) {
                //figure out what branch is taken first
                if (branchCondition == null)
                    throw RuntimeException("there has to be a condition")
                else {

                    if (branchCondition.op2 == null) {
                        LoggerHelper.logWarning("There is a \"null\" check in a condition; we do not consider this")
                        if (currentBranchTaken == 0)
                            conditionsToReturn.add(true)
                        else if (currentBranchTaken == 1)
                            conditionsToReturn.add(false)
                        else
                            throw RuntimeException("should not happen...")
                    } else {
                        val intConst = branchCondition.op2 as IntConstant


                        var intConstValue = -1
                        if (branchCondition is NeExpr) {
                            if (intConst.value == 0)
                                intConstValue = 1
                            else if (intConst.value == 1)
                                intConstValue = 0
                        } else if (branchCondition is EqExpr) {
                            intConstValue = intConst.value
                        } else if (branchCondition is GeExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is GtExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is LeExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is LtExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is NeExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is AddExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is AndExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is CmpExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is CmpgExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is CmplExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is ConditionExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is DivExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is MulExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is OrExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is RemExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is ShlExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is ShrExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is SubExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is UshrExpr) {
                            throw RuntimeException("todo")
                        } else if (branchCondition is XorExpr) {
                            throw RuntimeException("todo")
                        }


                        //this branch taken
                        if (intConstValue == 0) {
                            //which branch is actually taken by the CFG depth search?
                            if (currentBranchTaken == 0)
                                conditionsToReturn.add(true)
                            else if (currentBranchTaken == 1)
                                conditionsToReturn.add(false)
                            else
                                throw RuntimeException("should not happen...")
                        } else {
                            //which branch is actually taken by the CFG depth search?
                            if (currentBranchTaken == 0)
                                conditionsToReturn.add(false)
                            else if (currentBranchTaken == 1)
                                conditionsToReturn.add(true)
                            else
                                throw RuntimeException("should not happen...")
                        }//else branch taken
                    }
                }

                //in case we already discovered that the this and the else branch leads
                // to the logging point, we can stop our analysis
                if (conditionsToReturn.size == 2) {
                    worklist.clear()
                    //reset reached units
                    reachedUnits.clear()

                    continue
                } else
                    continue
            }

            //statement already passed?
            if (reachedUnits.contains(currentUnit))
                continue
            else {
                reachedUnits.add(currentUnit)
                //				System.out.println(String.format("(%d) Reached: %s", reachedUnits.size(), currentUnit));
            }

            val currentMethod = cfg.getMethodOf(currentUnit)
            //there is no need to look into the dummy main method
            if (currentMethod.declaringClass.toString() == "dummyMainClass")
                continue

            if (cfg.isCallStmt(currentUnit)) {
                var invokeExpr: InvokeExpr? = null
                if (currentUnit is AssignStmt) {
                    invokeExpr = currentUnit.invokeExpr
                } else if (currentUnit is InvokeStmt) {
                    invokeExpr = currentUnit.invokeExpr
                }

                //special handling for non-api calls
                val sm = invokeExpr!!.method
                if (UtilInstrumenter.isAppDeveloperCode(sm.declaringClass)) {
                    val callees = cfg.getCalleesOfCallAt(currentUnit)
                    callees
                            .map { cfg.getStartPointsOf(it) }
                            .flatMap { it }
                            .forEach {
                                //since we are context-sensitive, it does not make sense to check for already reached methods here
                                //if(!reachedUnits.contains(unit))
                                worklist.push(it)
                            }
                } else {
                    val successor = cfg.getSuccsOf(currentUnit)
                    for (unit in successor) {
                        //if(!reachedUnits.contains(unit))
                        worklist.push(unit)
                    }
                }//find successor for api calls

            } else if (currentUnit is ReturnStmt || currentUnit is ReturnVoidStmt) {
                val sm = cfg.getMethodOf(currentUnit)
                cfg.getCallersOf(sm)
                        .flatMap { cfg.getSuccsOf(it) }
                        .forEach {
                            //if(!reachedUnits.contains(unit))
                            worklist.push(it)
                        }
                continue
            } else if (currentUnit is IfStmt) {
                val ifStmt = currentUnit
                val bothBranches = cfg.getSuccsOf(currentUnit)
                val branchOne = bothBranches[0]
                val branchTwo = bothBranches[1]

                if (!firstConditionVisited) {
                    firstConditionVisited = true
                    assert(ifStmt.condition is BinopExpr)
                    branchCondition = ifStmt.condition as BinopExpr

                    //we always take the then-branch first
                    currentBranchTaken = 1
                    //therefore, we push the else branch first to the worklist
                    if (cfg.isFallThroughSuccessor(currentUnit, branchOne)) {
                        if (!reachedUnits.contains(branchOne))
                            worklist.push(branchOne)
                        if (!reachedUnits.contains(branchTwo))
                            worklist.push(branchTwo)
                        elseBranchStmt = branchOne
                    } else {
                        if (!reachedUnits.contains(branchTwo))
                            worklist.push(branchTwo)
                        if (!reachedUnits.contains(branchOne))
                            worklist.push(branchOne)
                        elseBranchStmt = branchTwo
                    }

                    continue
                } else {
                    if (!reachedUnits.contains(branchOne))
                        worklist.add(branchOne)
                    if (!reachedUnits.contains(branchTwo))
                        worklist.add(branchTwo)
                    continue
                }

            } else {
                val successor = cfg.getSuccsOf(currentUnit)
                for (unit in successor) {
                    //					if(!reachedUnits.contains(unit))
                    worklist.push(unit)
                }
            }//just take the successors
        }

        return conditionsToReturn
    }


    fun removeDuplicatedFlows(allDataFlows: Set<ResultSourceInfo>): Set<ResultSourceInfo> {
        val copy = HashSet(allDataFlows)

        for (dataFlow1 in allDataFlows) {
            val dataFlowPath1 = dataFlow1.path
            allDataFlows
                    .map { it.path }
                    .filter { dataFlowPath1 != it && Arrays.asList(*it).containsAll(Arrays.asList(*dataFlowPath1)) }
                    .forEach { copy.remove(dataFlow1) }
        }

        return copy
    }


    fun getControlFlowPaths(dataFlowPath: Array<Stmt>, cfg: IInfoflowCFG): Set<FragmentedControlFlowPath> {
        val fragmentedControlFlowPaths = HashSet<FragmentedControlFlowPath>()

        (0..dataFlowPath.size - 1 - 1)
                .map { getControlFlowPathsBetweenTwoDataFlowStmts(dataFlowPath[it], dataFlowPath[it + 1], cfg) }
                .forEach {
                    if (fragmentedControlFlowPaths.isEmpty()) {
                        for (cfp in it) {
                            val fcfp = FragmentedControlFlowPath()
                            fcfp.addNewControlFlowGraphFragment(cfp.deepCopy())
                            fragmentedControlFlowPaths.add(fcfp)
                        }
                    } else {
                        //we have to add more sub-paths to the current controlFlowPaths
                        if (it.size > 1) {
                            for (fcfp in fragmentedControlFlowPaths) {
                                for (subControlFlowPath in it) {
                                    val clonedFcfp = fcfp.deepCopy()
                                    clonedFcfp.addNewControlFlowGraphFragment(subControlFlowPath)
                                    fragmentedControlFlowPaths.add(clonedFcfp)
                                }

                                //we already added it, so we can remove this one
                                fragmentedControlFlowPaths.remove(fcfp)
                            }
                        } else {
                            val subControlFlowPath = it.iterator().next()
                            //remove first element of path since it is already included in the controlFlowPaths
                            //					subControlFlowPath.getPath().remove(0);

                            for (fcfp in fragmentedControlFlowPaths)
                                fcfp.addNewControlFlowGraphFragment(subControlFlowPath)
                        }//there is only one path to add
                    }
                }

        return fragmentedControlFlowPaths
    }


    fun getControlFlowPathsBetweenTwoDataFlowStmts(from: Stmt, to: Stmt, cfg: IInfoflowCFG): Set<ControlFlowPath> {
        //is necessary at conditions where two branches are taken. The control-flow path until the condition is stored
        //and re-used for the then- and else-branch
        val controlFlowPathsAtUnit = HashMap<Unit, MutableSet<ControlFlowPath>>()
        val controlFlowPaths = HashSet<ControlFlowPath>()
        var currentPaths: MutableSet<ControlFlowPath> = HashSet()
        val worklist = Stack<Unit>()
        worklist.add(from)

        while (!worklist.isEmpty()) {
            val currentUnit = worklist.pop()
            currentPaths = getCurrentControlFlowGraphsForUnit(currentUnit, controlFlowPathsAtUnit, currentPaths)

            //we only add cfg-statements of the original code
            //our own instrumented statements will not be part of the path
            if (!currentUnit.hasTag(InstrumentedCodeTag.name))
                addCurrentStmtToPaths(currentPaths, currentUnit)

            //we reached the to-statement
            if (currentUnit === to) {
                return currentPaths
            }

            //we can stop the path traversal in case we reached the dummyMain class
            val currentMethod = cfg.getMethodOf(currentUnit)
            //there is no need to look into the dummy main method
            if (currentMethod.declaringClass.toString() == "dummyMainClass")
                continue

            if (cfg.isCallStmt(currentUnit)) {
                var invokeExpr: InvokeExpr? = null
                if (currentUnit is AssignStmt) {
                    invokeExpr = currentUnit.invokeExpr
                } else if (currentUnit is InvokeStmt) {
                    invokeExpr = currentUnit.invokeExpr
                }

                //special handling for non-api calls
                val sm = invokeExpr!!.method
                if (UtilInstrumenter.isAppDeveloperCode(sm.declaringClass)) {
                    val callees = cfg.getCalleesOfCallAt(currentUnit)
                    callees
                            .map { cfg.getStartPointsOf(it) }
                            .flatMap { it }
                            .filter { proceedWithNextUnit(currentUnit, it, currentPaths, controlFlowPathsAtUnit) }
                            .forEach { worklist.push(it) }
                } else {
                    val successor = cfg.getSuccsOf(currentUnit)
                    successor
                            .filter { proceedWithNextUnit(currentUnit, it, currentPaths, controlFlowPathsAtUnit) }
                            .forEach { worklist.push(it) }
                }//find successor for api calls
            } else if (currentUnit is ReturnStmt || currentUnit is ReturnVoidStmt) {
                val sm = cfg.getMethodOf(currentUnit)
                cfg.getCallersOf(sm)
                        .flatMap { cfg.getSuccsOf(it) }
                        .filter { proceedWithNextUnit(currentUnit, it, currentPaths, controlFlowPathsAtUnit) }
                        .forEach { worklist.push(it) }
                continue
            } else if (currentUnit is IfStmt) {
                val bothBranches = cfg.getSuccsOf(currentUnit)
                val branchOne = bothBranches[0]
                val branchTwo = bothBranches[1]

                //special treatment of successors for conditions
                //we have to save the current-path(s) for both successors; otherwise this information will be lost
                //in our worklist-iteration-process
                worklist.push(branchOne)
                saveControlFlowGraphForUnit(branchOne, currentPaths, controlFlowPathsAtUnit)
                worklist.push(branchTwo)
                saveControlFlowGraphForUnit(branchTwo, currentPaths, controlFlowPathsAtUnit)

                continue
            } else {
                var saveControlFlowPath = false
                val successors = cfg.getSuccsOf(currentUnit)
                //in case there are more than one successors, we have to save the control flow path
                //at the successor statements since the CFG splits (similar to IfStmt)
                if (successors.size > 1)
                    saveControlFlowPath = true
                for (nextUnit in successors) {
                    if (proceedWithNextUnit(currentUnit, nextUnit, currentPaths, controlFlowPathsAtUnit)) {
                        worklist.push(nextUnit)
                        if (saveControlFlowPath)
                            saveControlFlowGraphForUnit(nextUnit, currentPaths, controlFlowPathsAtUnit)
                    }
                }
            }//just take the successors
        }

        return controlFlowPaths
    }


    fun saveControlFlowGraphForUnit(unitToProceedNext: Unit, currentControlFlowPaths: Set<ControlFlowPath>, controlFlowPathsAtUnit: MutableMap<Unit, MutableSet<ControlFlowPath>>) {
        if (controlFlowPathsAtUnit.containsKey(unitToProceedNext)) {
            for (cfp in currentControlFlowPaths) {
                if (controlFlowPathsAtUnit[unitToProceedNext] === currentControlFlowPaths)
                else
                    controlFlowPathsAtUnit[unitToProceedNext]!!.add(cfp.deepCopy())//do nothing
            }
        } else {
            val newControlFlowPathSet = currentControlFlowPaths
                    .map { it.deepCopy() }
                    .toMutableSet()
            controlFlowPathsAtUnit.put(unitToProceedNext, newControlFlowPathSet)
        }
    }


    fun getCurrentControlFlowGraphsForUnit(currentUnit: Unit, controlFlowPathsAtUnit: Map<Unit, MutableSet<ControlFlowPath>>, currentPaths: MutableSet<ControlFlowPath>): MutableSet<ControlFlowPath> {
        if (controlFlowPathsAtUnit.containsKey(currentUnit))
            return controlFlowPathsAtUnit[currentUnit]!!
        else
            return currentPaths
    }


    fun addCurrentStmtToPaths(currentPaths: MutableSet<ControlFlowPath>, currentUnit: Unit) {
        if (currentPaths.isEmpty())
            currentPaths.add(ControlFlowPath())
        for (cfp in currentPaths)
            cfp.addStmt(currentUnit)
    }


    private fun proceedWithNextUnit(currentUnit: Unit, nextUnit: Unit, currentControlFlowPaths: Set<ControlFlowPath>, controlFlowPathsAtUnit: Map<Unit, Set<ControlFlowPath>>): Boolean {
        if (controlFlowPathsAtUnit.containsKey(currentUnit)) {
            val allControlFlowPathsOfUnit = controlFlowPathsAtUnit[currentUnit]

            return allControlFlowPathsOfUnit!!.none { it.containsUnit(nextUnit) }
        } else {
            return currentControlFlowPaths.none { it.containsUnit(nextUnit) }
        }
    }


    fun getPostDominatorOfUnit(cfg: IInfoflowCFG, dataFlowStatement: Unit): Unit? {
        val controlFlowPathsAtUnit = HashMap<Unit, Set<ControlFlowPath>>()
        val currentPaths = HashSet<ControlFlowPath>()
        val worklist = Stack<Unit>()
        worklist.add(dataFlowStatement)

        while (!worklist.isEmpty()) {
            val currentUnit = worklist.pop()

            if (currentUnit.hasTag(InstrumentedCodeTag.name)) {
                val successors = cfg.getSuccsOf(currentUnit)

                successors
                        .filter { proceedWithNextUnit(currentUnit, it, currentPaths, controlFlowPathsAtUnit) }
                        .forEach { worklist.push(it) }
                continue
            }

            val currentMethod = cfg.getMethodOf(currentUnit)

            //this is a kind of hack: We excluded exception-edges here and also keep in mind that ALL dominator-algorithms are intra-procedural
            val postdominatorFinder = MHGPostDominatorsFinder(BriefUnitGraph(currentMethod.retrieveActiveBody()))
            var immediatePostDominator = postdominatorFinder.getImmediateDominator(currentUnit)
            while (immediatePostDominator.hasTag(InstrumentedCodeTag.name)) {
                immediatePostDominator = postdominatorFinder.getImmediateDominator(immediatePostDominator)
            }
            return immediatePostDominator
        }
        return null
    }


    fun needToAddConditionalUnit(cfg: IInfoflowCFG, to: Unit, booleanUnit: Unit): Boolean {
        //1st: We have to check if there is a condition for the boolean-unit
        val conditionOfBooleanUnit = findConditionalStatementForBooleanUnit(cfg, booleanUnit)
        if (conditionOfBooleanUnit == null)
            return false
        else {

        }

        //2nd: Get the post-dominator of the IfStmt from step 1.
        val postDominator = cfg.getPostdominatorOf(conditionOfBooleanUnit).unit ?: return true

        //since we know that the booleanUnit (param) occurs "before" the to unit (param), we can
        //assume that the conditionOfBooleanUnit directly affects the to unit in case of a null of the
        //post-dominator

        //3rd: Starting from the conditionOfBooleanUnit, we take the then and else branch and check
        //whether the "to"-unit is part of the path starting from the conditionOfBooleanUnit till the postDominator.
        //if this is the case, we know that we have to consider the booleanUnit (param) into the contraint,
        //if not, we do not consider it in the contraint.

        val worklist = Stack<Unit>()
        val processedUnits = HashSet<Unit>()
        worklist.add(conditionOfBooleanUnit)

        while (!worklist.isEmpty()) {
            val currentUnit = worklist.pop()
            //in case of a loop or recursion
            if (processedUnits.contains(currentUnit))
                continue

            processedUnits.add(currentUnit)

            //in case we hit the booleanUnit (param), we know that the condition influences the to unit (param)
            if (currentUnit === booleanUnit)
                return true

            //we reached the post-dominator, we have to continue until the second branch is completeley traversed
            if (currentUnit === postDominator) {
                continue
            }

            //we can stop the path traversal in case we reached the dummyMain class
            val currentMethod = cfg.getMethodOf(currentUnit)
            //there is no need to look into the dummy main method
            if (currentMethod.declaringClass.toString() == "dummyMainClass")
                continue

            if (cfg.isCallStmt(currentUnit)) {
                var invokeExpr: InvokeExpr? = null
                if (currentUnit is AssignStmt) {
                    invokeExpr = currentUnit.invokeExpr
                } else if (currentUnit is InvokeStmt) {
                    invokeExpr = currentUnit.invokeExpr
                }

                //special handling for non-api calls
                val sm = invokeExpr!!.method
                if (UtilInstrumenter.isAppDeveloperCode(sm.declaringClass)) {
                    val callees = cfg.getCalleesOfCallAt(currentUnit)
                    callees
                            .map { cfg.getStartPointsOf(it) }
                            .forEach { worklist.addAll(it) }
                } else {
                    val successors = cfg.getSuccsOf(currentUnit)
                    worklist.addAll(successors)
                }//find successor for api calls
            } else if (currentUnit is ReturnStmt || currentUnit is ReturnVoidStmt) {
                val sm = cfg.getMethodOf(currentUnit)
                cfg.getCallersOf(sm)
                        .map { cfg.getSuccsOf(it) }
                        .forEach { worklist.addAll(it) }
                continue
            } else {
                val successors = cfg.getSuccsOf(currentUnit)
                worklist.addAll(successors)
            }//just take the successors
        }
        return false
    }


    private fun findConditionalStatementForBooleanUnit(cfg: IInfoflowCFG, booleanUnit: Unit): IfStmt? {
        val worklist = Stack<Unit>()
        val processedUnits = HashSet<Unit>()
        worklist.add(booleanUnit)

        while (!worklist.isEmpty()) {
            val currentUnit = worklist.pop()
            //in case of a loop or recursion
            if (processedUnits.contains(currentUnit))
                continue
            processedUnits.add(currentUnit)

            //skip our own instrumented code
            if (currentUnit.hasTag(InstrumentedCodeTag.name))
                continue


            //we reached the condition
            if (currentUnit is IfStmt) {
                return currentUnit
            }

            val methodOfBooleanUnit = cfg.getMethodOf(booleanUnit)
            val graph = cfg.getOrCreateUnitGraph(methodOfBooleanUnit)
            //Comment: Steven said it should always be a UnitGraph + he will implement a more convenient way in the near future :-)
            val unitGraph = graph as UnitGraph

            val defs = SimpleLocalDefs(unitGraph)
            val uses = SimpleLocalUses(unitGraph, defs)
            val usesOfCurrentUnit = uses.getUsesOf(booleanUnit)
            usesOfCurrentUnit.mapTo(worklist) { it.getUnit() }

        }
        return null
    }
}
