package me.zhenhao.forced.decisionmaker.analysis.smartconstantdataextractor

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

import soot.BooleanType
import soot.Local
import soot.jimple.AssignStmt
import soot.jimple.Stmt
import soot.jimple.infoflow.InfoflowConfiguration
import soot.jimple.infoflow.data.AccessPath
import soot.jimple.infoflow.results.InfoflowResults
import soot.jimple.infoflow.results.ResultSourceInfo
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG
import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.UtilSMT
import me.zhenhao.forced.sharedclasses.util.Pair


class SMTPreparationPhase(private val cfg: IInfoflowCFG, private val results: InfoflowResults) {


	fun prepareDataFlowPathsForSMTConverter(): Set<ResultSourceInfo> {
		//This step is necessary for storing the ResultSourceInfo elements into a set
		//The result ResultSourceInfo object does only represent a source and not the dataflow.
		//But with the PathAgnosticResults flag, one can force the ResultSourceInfo object
		//to consider the path (see equals method)
		InfoflowConfiguration.setPathAgnosticResults(false)

		//control flow involved
		return prepareDataFlowsDependingOnControlFlow(results, FrameworkOptions.mergeDataFlows)
	}


	private fun prepareDataFlowsDependingOnControlFlow(results: InfoflowResults, mergeDataFlows: Boolean): Set<ResultSourceInfo> {
		val dataFlows = HashSet<ResultSourceInfo>()

		for (sink in results.results.keySet()) {
			for (source in results.results.get(sink)) {
				for (stmt in source.path) {
					println("######" + stmt + "\n")
				}
				dataFlows.add(source)
			}
			println("###################")
		}

		//Remove dataflows where all the elements in a particular dataflow are included in another dataflow
		val minimalDataFlowSet = HashSet<ResultSourceInfo>()
		for (dataflow1 in dataFlows) {
			val flow1 = ArrayList(Arrays.asList(*dataflow1.path))
			var addFlow1 = true
			for (dataflow2 in dataFlows) {
				val flow2 = ArrayList(Arrays.asList(*dataflow2.path))

				if (dataflow1 !== dataflow2 && flow2.containsAll(flow1))
					addFlow1 = false
			}
			if (addFlow1)
				minimalDataFlowSet.add(dataflow1)
		}


		val allStmtsFromAllDataFlowPaths = HashMap<Stmt, AccessPath>()
		//Merge all dataflow-elements together for a faster search
		for (dataflow in minimalDataFlowSet) {
			val statementsDataFlow = ArrayList(Arrays.asList(*dataflow.path))
			val accessPathDataFlow = ArrayList(Arrays.asList(*dataflow.pathAccessPaths))

			assert(statementsDataFlow.size == accessPathDataFlow.size)

			for (i in statementsDataFlow.indices) {
				val key = statementsDataFlow[i]
				val value = accessPathDataFlow[i]
				allStmtsFromAllDataFlowPaths.put(key, value)
			}
		}


		var allDataFlows: MutableSet<ResultSourceInfo> = HashSet()
		for ((tmp_counter_removeMe, dataflow1) in minimalDataFlowSet.withIndex()) {
			var completeTimePath = System.currentTimeMillis()

			val statementsDataFlow1 = ArrayList(Arrays.asList(*dataflow1.path))
			val accessPathDataFlow1 = ArrayList(Arrays.asList(*dataflow1.pathAccessPaths))

			//if one wants to be more precise, he needs to merge the dataflows for a more precise contraint
			if (mergeDataFlows) {
				for (indexDataflow1Statements in 0..statementsDataFlow1.size - 1 - 1) {
					val fromStmt_Dataflow1 = statementsDataFlow1[indexDataflow1Statements]
					val toStmt_Dataflow1 = statementsDataFlow1[indexDataflow1Statements + 1]

					val allCFGPathsBetweenTwoUnitsOfDataflow1 = UtilSMT.getControlFlowPathsBetweenTwoDataFlowStmts(fromStmt_Dataflow1, toStmt_Dataflow1, cfg)

					for (singlePath in allCFGPathsBetweenTwoUnitsOfDataflow1) {
						val singleCfgPathDataflow1BetweenTwoStmts = singlePath.getPath()

						var additionalOffset = 0
						for ((betweenStmt, betweenStmt_AccessPath) in allStmtsFromAllDataFlowPaths) {


							if (singleCfgPathDataflow1BetweenTwoStmts.contains(betweenStmt) && !statementsDataFlow1.contains(betweenStmt)) {
								//special treatment in case of a condition
								if (betweenStmt.containsInvokeExpr() &&
										betweenStmt is AssignStmt &&
										betweenStmt.getInvokeExpr().type is BooleanType) {

									//betweenStmt_Dataflow2 = conditional statement
									val mergeCondition = UtilSMT.needToAddConditionalUnit(cfg, toStmt_Dataflow1, betweenStmt)

									if (mergeCondition) {
										statementsDataFlow1.add(indexDataflow1Statements + 1 + additionalOffset, betweenStmt)
										accessPathDataFlow1.add(indexDataflow1Statements + 1 + additionalOffset, betweenStmt_AccessPath)
										++additionalOffset
									}
								} else {
									statementsDataFlow1.add(indexDataflow1Statements + 1 + additionalOffset, betweenStmt)
									accessPathDataFlow1.add(indexDataflow1Statements + 1 + additionalOffset, betweenStmt_AccessPath)
									++additionalOffset
								}//in case of a linear (no condition) path, just add the new statement to dataflow1
							}
						}
					}
				}
			}
			val tmp = ResultSourceInfo(accessPathDataFlow1[0], statementsDataFlow1[0], null, statementsDataFlow1, accessPathDataFlow1)
			allDataFlows.add(tmp)
			completeTimePath = System.currentTimeMillis() - completeTimePath
			println(String.format("[%s / %s] (%s path elements)\n\t time: %s ms", tmp_counter_removeMe, minimalDataFlowSet.size, statementsDataFlow1.size, completeTimePath))
		}


		/*
		for (ResultSourceInfo dataflow1 : dataFlows) {
			List<Stmt> statementsDataFlow1 = new ArrayList<Stmt>(Arrays.asList(dataflow1.getPath()));
			List<AccessPath> accessPathDataFlow1 = new ArrayList<AccessPath>(Arrays.asList(dataflow1.getPathAccessPaths()));

			for(int indexDataflow1Statements = 0; indexDataflow1Statements < statementsDataFlow1.size() - 1; indexDataflow1Statements++) {
				Stmt fromStmt_Dataflow1 = statementsDataFlow1.get(indexDataflow1Statements);
				Stmt toStmt_Dataflow1 = statementsDataFlow1.get(indexDataflow1Statements+1);

				Set<ControlFlowPath> allCFGPathsBetweenTwoUnitsOfDataflow1 = UtilSMT.getControlFlowPathsBetweenTwoDataFlowStmts(fromStmt_Dataflow1, toStmt_Dataflow1, cfg);

//				if(allCFGPathsBetweenTwoUnitsOfDataflow1.size() > 1)
//					throw new RuntimeException("todo");

				int countCFGsBetweenTwUnits = 0;
				for(ControlFlowPath singlePath : allCFGPathsBetweenTwoUnitsOfDataflow1) {
					List<Unit> singleCfgPathDataflow1BetweenTwoStmts = singlePath.getPath();

					for (ResultSourceInfo dataflow2 : dataFlows) {

						if(dataflow1 != dataflow2) {
							Stmt[] statementsDataFlow2 = dataflow2.getPath();
							AccessPath[] accessPathDataFlow2 = dataflow2.getPathAccessPaths();

							//in case there are more than 1 statement to add to dataflow1, we also have to update the
							//index of dataflow-statemetents1. We do this with an additional offset variable
							int additionalOffset = 0;
							for(int indexDataflow2Statements = 0; indexDataflow2Statements < statementsDataFlow2.length; indexDataflow2Statements++) {
								Stmt betweenStmt_Dataflow2 = statementsDataFlow2[indexDataflow2Statements];
								AccessPath betweenStmt_Dataflow2_AccessPath = accessPathDataFlow2[indexDataflow2Statements];

								if(singleCfgPathDataflow1BetweenTwoStmts.contains(betweenStmt_Dataflow2) &&
										!statementsDataFlow1.contains(betweenStmt_Dataflow2)) {
									//special treatment in case of a condition
									if(betweenStmt_Dataflow2.containsInvokeExpr() &&
											betweenStmt_Dataflow2 instanceof AssignStmt &&
											betweenStmt_Dataflow2.getInvokeExpr().getType() instanceof BooleanType) {
										//betweenStmt_Dataflow2 = conditional statement
										boolean mergeCondition = UtilSMT.needToAddConditionalUnit(cfg, toStmt_Dataflow1, betweenStmt_Dataflow2);

										if(mergeCondition) {
											statementsDataFlow1.add(indexDataflow1Statements+1+additionalOffset, betweenStmt_Dataflow2);
											accessPathDataFlow1.add(indexDataflow1Statements+1+additionalOffset, betweenStmt_Dataflow2_AccessPath);
											++additionalOffset;
//											//This step is the weakness in this analysis
//											//It does ALWAYS assume that the conditional-stmt is directly used by the condition
//											//We either improve this with a slicing approach or we may produce wrong contraints
//											LoggerHelper.logInfo("We might produce a wrong constraint here...");
//											if(conditionsForReachingTarget.size() == 1) {
//												AssignStmt conditionAssignment = (AssignStmt)betweenStmt_Dataflow2;
//												int condition = (conditionsForReachingTarget.iterator().next() == true) ? 1 : 0;
//												Value booleanLocalVariable = conditionAssignment.getLeftOp();
//
//												Stmt newAssignStmt = Jimple.v().newAssignStmt(booleanLocalVariable, IntConstant.v(condition));
//
//												statementsDataFlow1.add(indexDataflow1Statements+2, newAssignStmt);
//												accessPathDataFlow1.add(indexDataflow1Statements+2, null);
//											}
//											else
//												throw new RuntimeException("todo");
										}
									}
									//in case of a linear (no condition) path, just add the new statement to dataflow1
									else {
										statementsDataFlow1.add(indexDataflow1Statements+1+additionalOffset, betweenStmt_Dataflow2);
										accessPathDataFlow1.add(indexDataflow1Statements+1+additionalOffset, betweenStmt_Dataflow2_AccessPath);
										++additionalOffset;
									}
								}
							}
						}
					}
					++countCFGsBetweenTwUnits;
				}
			}

			ResultSourceInfo tmp = new ResultSourceInfo(accessPathDataFlow1.get(0), statementsDataFlow1.get(0), null, statementsDataFlow1, accessPathDataFlow1);
			allDataFlows.add(tmp);
		}
		*/
		allDataFlows = UtilSMT.removeDuplicatedFlows(allDataFlows) as MutableSet
		return allDataFlows
	}


	private fun mergeDataFlowsIntoSingleDataFlow(statementToEnrich: Stmt, originalPath: ResultSourceInfo, pathToMerge: ResultSourceInfo): ResultSourceInfo {
		val pathStmts = ArrayList(Arrays.asList(*originalPath.path))
		val accessPaths = ArrayList(Arrays.asList(*originalPath.pathAccessPaths))

		val pathToMergeStmts = ArrayList(Arrays.asList(*pathToMerge.path))
		val pathToMergeAccessPaths = ArrayList(Arrays.asList(*pathToMerge.pathAccessPaths))


		var index = pathStmts.indexOf(statementToEnrich)
		//		if(index < 0)
		//			throw new RuntimeException("Woops, there is something wonkey here");
		//
		//		for(int i = 0; i < pathToMergeStmts.size(); i++) {
		//			pathStmts.add(index, pathToMergeStmts.get(i));
		//			accessPaths.add(index, pathToMergeAccessPaths.get(i));
		//			index +=1;
		//		}


		val dataToMerge = ArrayList<Pair<Stmt, AccessPath>>()

		var position: Int
		position = 0
		while (position < pathToMergeStmts.size) {
			if (pathStmts.contains(pathToMergeStmts[position]) && !dataToMerge.isEmpty()) {
				var indexToInsertBefore = pathStmts.indexOf(pathToMergeStmts[position])
				indexToInsertBefore -= 1

				//				for(Pair<Stmt,AccessPath> pair : dataToMerge) {
				//					pathStmts.add(indexToInsertBefore, pair.getFirst());
				//					accessPaths.add(indexToInsertBefore, pair.getSecond());
				//					++indexToInsertBefore;
				//				}
			} else if (!pathStmts.contains(pathToMergeStmts[position])) {
				dataToMerge.add(Pair(pathToMergeStmts[position], pathToMergeAccessPaths[position]))
			}
			position++
		}

		if (!dataToMerge.isEmpty()) {
			for (pair in dataToMerge) {
				pathStmts.add(index, pair.first)
				accessPaths.add(index, pair.second)
				++index
			}
		}

		return ResultSourceInfo(accessPaths[0], pathStmts[0], null, pathStmts, accessPaths)
	}


	private fun findDataFlowPathForSink(sinkStmt: Stmt, sinkLokal: Local, allDataFlows: List<ResultSourceInfo>): ResultSourceInfo? {
		for (singleFlow in allDataFlows) {
			val statements = singleFlow.path
			val accessPath = singleFlow.pathAccessPaths

			for (i in statements.indices) {
				val currentStmt = statements[i]
				if (currentStmt === sinkStmt) {
					if (accessPath[i].plainValue === sinkLokal)
						return singleFlow
				} else if (currentStmt is AssignStmt) {
					val lhs = currentStmt.leftOp

					if (lhs === sinkLokal)
						return singleFlow
				}
			}
		}
		return null
	}
}
