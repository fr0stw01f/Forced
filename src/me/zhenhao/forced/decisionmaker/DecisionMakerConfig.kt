package me.zhenhao.forced.decisionmaker

import java.io.File
import java.util.HashMap
import java.util.HashSet

import soot.Unit
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.analysis.FuzzyAnalysis
import me.zhenhao.forced.dynamiccfg.utils.FileUtils
import me.zhenhao.forced.progressmetric.IProgressMetric


class DecisionMakerConfig {

    private val ANALYSES_FILENAME = "." + File.separator + "files" + File.separator + "analysesNames.txt"
    private val METRICS_FILENAME = "." + File.separator + "files" + File.separator + "metricsNames.txt"

    private var allAnalyses: MutableSet<FuzzyAnalysis> = HashSet()
    private var nameToAnalysis: MutableMap<String, FuzzyAnalysis> = HashMap()

    private var progressMetrics: MutableSet<IProgressMetric> = HashSet()

    var allTargetLocations: MutableSet<Unit> = HashSet()

    lateinit var backwardsCFG: BackwardsInfoflowCFG
        private set

    fun initialize(targetLocations: Set<Unit>): Boolean {
        var successful = registerFuzzyAnalyses()
        allTargetLocations.addAll(targetLocations)
        if (!successful)
            return false
        successful = registerProgressMetrics()
        return successful
    }

    fun initializeCFG() {
        val forwardCFG = InfoflowCFG()
        backwardsCFG = BackwardsInfoflowCFG(forwardCFG)
    }


    private fun registerFuzzyAnalyses(): Boolean {
        val registeredAnalyses = analysesNames
        registeredAnalyses
                .filterNot { it.startsWith("%") }
                .forEach {
                    try {
                        val analysisClass = Class.forName(it)
                        val defaultConstructor = analysisClass.getConstructor()
                        defaultConstructor.isAccessible
                        val constructorObject = defaultConstructor.newInstance() as? FuzzyAnalysis ?: throw RuntimeException("There is a problem with the registered analysis in the files/analysesNames.txt file!")
                        val analysis = constructorObject

                        allAnalyses.add(analysis)
                        nameToAnalysis.put(analysis.getAnalysisName(), analysis)
                        LoggerHelper.logEvent(MyLevel.ANALYSIS, "[ANALYSIS-TYPE] " + it)
                    } catch (ex: Exception) {
                        LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                        ex.printStackTrace()
                        return false
                    }
                }
        return true
    }


    private fun registerProgressMetrics(): Boolean {
        val registeredMetrics = progressMetricNames
        registeredMetrics
                .filterNot { it.startsWith("%") }
                .forEach {
                    try {
                        val metricClass = Class.forName(it)
                        val defaultConstructor = metricClass.getConstructor(Collection::class.java, InfoflowCFG::class.java)
                        defaultConstructor.isAccessible
                        val constructorObject = defaultConstructor.newInstance(allTargetLocations, backwardsCFG) as? IProgressMetric ?: throw RuntimeException("There is a problem with the registered metric in the files/metricsNames.txt file!")
                        val metric = constructorObject
                        LoggerHelper.logEvent(MyLevel.ANALYSIS, "[METRIC-TYPE] " + it)

                        //currently, there can be only a single target
                        if (allTargetLocations.size != 1)
                            throw RuntimeException("There can be only 1 target location per run")
                        val target = allTargetLocations.iterator().next()
                        if (backwardsCFG.getMethodOf(target) != null) {
                            metric.setCurrentTargetLocation(target)

                            //initialize the metric, otherwise it is empty!
                            metric.initialize()
                            progressMetrics.add(metric)
                        } else {
                            LoggerHelper.logEvent(MyLevel.LOGGING_POINT, "target is not statically reachable!")
                            return false
                        }
                    } catch (ex: Exception) {
                        LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                        ex.printStackTrace()
                        System.exit(-1)
                    }
                }
        return true
    }


    private val analysesNames: Set<String>
        get() = FileUtils.textFileToLineSet(ANALYSES_FILENAME)


    private val progressMetricNames: Set<String>
        get() = FileUtils.textFileToLineSet(METRICS_FILENAME)

    //
    //	private void extractAllTargetLocations() {
    //		//extract all logging points from file
    //		Set<String> targetLocationsTmp = new HashSet<String>();
    //
    //		try{
    //			BufferedReader br = new BufferedReader(new FileReader(TARGET_METHODS_FILENAME));
    //		    try {
    //		        String line;
    //		        while ((line = br.readLine()) != null) {
    //		        	targetLocationsTmp.add(line);
    //		        }
    //		    } finally {
    //		        br.close();
    //		    }
    //		}catch(Exception ex) {
    //			ex.printStackTrace();
    //			System.exit(-1);
    //		}
    //
    //		targetMethods.addAll(targetLocationsTmp);
    //
    //		if(!targetLocationsTmp.isEmpty()) {
    //
    //			Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
    //			for(SootClass clazz : applicationClasses) {
    //				//no need to look into our code
    //				if (!UtilInstrumenter.isAppDeveloperCode(clazz))
    //					continue;
    //
    //				for(SootMethod method : clazz.getMethods()) {
    //					if(method.hasActiveBody()) {
    //						Body body = method.retrieveActiveBody();
    //						for (Iterator<Unit> unitIt = body.getUnits().iterator(); unitIt.hasNext(); ) {
    //							Unit curUnit = unitIt.next();
    //							if(curUnit instanceof Stmt) {
    //								Stmt statement = (Stmt)curUnit;
    //
    //								if(statement.containsInvokeExpr()){
    //									InvokeExpr invExpr = statement.getInvokeExpr();
    //									String invokeExprMethodSignature = invExpr.getMethod().getSignature();
    //
    //									for(String targetLocation : targetLocationsTmp) {
    //										//we accept all classes
    //										if(targetLocation.startsWith("<*:")) {
    //											String pattern = "<.:\\s(.*)\\s(.*)\\((.*)\\)>";
    //										      Pattern r = Pattern.compile(pattern);
    //
    //										      Matcher m = r.matcher(targetLocation);
    //										      if (m.find()) {
    //										    	  if(m.group(1).equals(invExpr.getMethod().getReturnType().toString()) &&
    //										    		  m.group(2).equals(invExpr.getMethod().getName()))
    //										    		  this.allTargetLocations.add(curUnit);
    //										      }
    //										}
    //										else if(targetLocation.equals(invokeExprMethodSignature))
    //											this.allTargetLocations.add(curUnit);
    //									}
    //								}
    //							}
    //						}
    //					}
    //				}
    //			}
    //		}
    //		if(this.allTargetLocations.size() == 0) {
    //			LoggerHelper.logWarning("There are no reachable target locations");
    //			System.exit(0);
    //		}
    //
    //	}

    fun getAllAnalyses(): Set<FuzzyAnalysis> {
        return allAnalyses
    }

    fun getProgressMetrics(): Set<IProgressMetric> {
        return progressMetrics
    }

    fun getAnalysisByName(name: String): FuzzyAnalysis? {
        return nameToAnalysis[name]
    }

}
