package me.zhenhao.forced

import com.android.ddmlib.AndroidDebugBridge
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.apkspecific.UtilApk
import me.zhenhao.forced.appinstrumentation.Instrumenter
import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.appinstrumentation.transformer.InstrumentedCodeTag
import me.zhenhao.forced.bootstrap.AnalysisTask
import me.zhenhao.forced.bootstrap.AnalysisTaskManager
import me.zhenhao.forced.bootstrap.DexFileManager
import me.zhenhao.forced.bootstrap.InstanceIndependentCodePosition
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.DecisionMaker
import me.zhenhao.forced.decisionmaker.DecisionMakerConfig
import me.zhenhao.forced.decisionmaker.DeterministicRandom
import me.zhenhao.forced.decisionmaker.UtilDecisionMaker
import me.zhenhao.forced.frameworkevents.FrameworkEvent
import me.zhenhao.forced.frameworkevents.manager.FrameworkEventManager
import org.apache.commons.io.FileUtils
import soot.PackManager
import soot.Scene
import soot.SootMethod
import soot.Unit
import soot.jimple.infoflow.android.SetupApplication
import soot.jimple.infoflow.android.config.SootConfigForAndroid
import soot.jimple.infoflow.cfg.LibraryClassPatcher
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG
import soot.jimple.infoflow.source.data.NullSourceSinkDefinitionProvider
import soot.options.Options
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.logging.Level


class ForcedMain private constructor() {

	private fun run(args: Array<String>): Set<EnvironmentResult>? {
		val frameworkOptions = FrameworkOptions()
		frameworkOptions.parse(args)
		LoggerHelper.initialize()
		LoggerHelper.logEvent(MyLevel.APKPATH, FrameworkOptions.apkPath)
		LoggerHelper.logEvent(MyLevel.ANALYSIS,
				String.format("Force timeout: %d || Inactivity timeout: %d || Max restarts: %d",
						FrameworkOptions.forceTimeout, FrameworkOptions.inactivityTimeout, FrameworkOptions.maxRestarts))

        // make sure we have the latest class files to instrument
        makeForcedBinReadyForInstrument()

		//remove all files in sootOutput folder
		FileUtils.cleanDirectory(File(UtilInstrumenter.SOOT_OUTPUT))

		if (UtilMain.blacklistedAPKs.contains(FrameworkOptions.apkPath))
			return null

		// Schedule the initial analysis task
		// Task manager is passed to SocketServer to enqueue new tasks by calling
        // handleDexFileReceived(DexFileTransferTraceItem)
		val analysisTaskManager = AnalysisTaskManager()
		analysisTaskManager.enqueueAnalysisTask(AnalysisTask())

		// Execute all the tasks from the task manager
		val results = HashSet<EnvironmentResult>()
		var currentTask = analysisTaskManager.scheduleNextTask()
        var taskId = 0
		while (currentTask != null) {
			handleAllTargets(currentTask, analysisTaskManager, results, taskId++)
			currentTask = analysisTaskManager.scheduleNextTask()
		}

		return results
	}

    fun handleAllTargets(analysisTask: AnalysisTask, analysisTaskManager: AnalysisTaskManager,
                         results: MutableSet<EnvironmentResult>, taskId: Int) {
        LoggerHelper.logInfo(String.format("Starting analysis for task %d with %d dex files",
                taskId, analysisTask.dexFilesToMerge.size))
        try {
            //needed for the extractAllTargetLocations method
            initializeSoot(analysisTask)
        } catch (ex: Exception) {
            System.err.println("Could not initialize Soot, skipping analysis task: " + ex.message)
            ex.printStackTrace()
            return
        }
        // The DecisionMaker configuration needs to initialize the metrics.
        // Therefore, it requires a running Soot instance.
        val config = DecisionMakerConfig()
        config.initializeCFG()

        //extract all target locations
        val allTargetLocations = UtilDecisionMaker.extractAllTargetLocations()
        if (allTargetLocations.isEmpty()) {
            LoggerHelper.logEvent(MyLevel.NO_TARGETS, "There are no reachable target locations")
            UtilMain.writeToFile("noLoggingPoint.txt", FrameworkOptions.apkPath + "\n")
        }

        // We have to do this hack due to a reset of soot.
        // This step is necessary otherwise, we will not get a clean apk for each logging point
        val targetsAsCodePos = UtilMain.convertUnitsToIndependentCodePositions(allTargetLocations,
                config.backwardsCFG)

        var initSoot = false

        // Treat each target location individually
        for (singleTargetAsPos in targetsAsCodePos) {
            handleSingleTarget(singleTargetAsPos, analysisTask, analysisTaskManager, results, initSoot)
            initSoot = true
        }
    }

    fun handleSingleTarget(singleTargetAsPos: InstanceIndependentCodePosition, analysisTask: AnalysisTask,
                           analysisTaskManager: AnalysisTaskManager, results: MutableSet<EnvironmentResult>,
                           initSoot: Boolean) {

        if (initSoot)
            initializeSoot(analysisTask)

        //we need to do this step, because we reset soot
        val singleTarget = UtilMain.convertIndependentCodePositionToUnit(singleTargetAsPos)
        if (singleTarget == null) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "############ PLEASE DOUBLE CHECK TARGET LOCATION ")
            return
        }

        val singleTargetLocation = setOf(singleTarget)

        // We may need to remove some statements before running the analysis
        removeStatementsForAnalysis(analysisTask)

        // needs to be re-initialized due to soot-reset
        val config = DecisionMakerConfig()
        config.initializeCFG()

        //now we have access to the CFG, check if target is reachable:
        if (config.backwardsCFG.getMethodOf(singleTarget) == null) {
            LoggerHelper.logEvent(MyLevel.LOGGING_POINT, "target is not statically reachable!")
            return
        }

        // register fuzzy analyses for the single target
        if (config.initialize(singleTargetLocation)) {
            //get potential Android event which trigger the initial code section for reaching the logging point
            val events: MutableSet<FrameworkEvent?>
            //todo PAPER-EVAL ONLY
            if (FrameworkOptions.evaluationJustStartApp) {
                events = HashSet()
                events.add(null)
            } else {
                events = getFrameworkEvents(singleTarget, config.backwardsCFG) as MutableSet

                if (events.isEmpty()) {
                    LoggerHelper.logEvent(MyLevel.ADB_EVENT, "no events available")
                    events.add(null)
                }
            }

            val decisionMaker = DecisionMaker(config, DexFileManager(), analysisTaskManager)
            val codePositionManager = CodePositionManager.codePositionManagerInstance

            // instrument, sign and align
            appPreparationPhase(codePositionManager, config)

            val resultsPerTarget = handleEventsForTarget(singleTarget, events, decisionMaker, codePositionManager)

            results.addAll(resultsPerTarget)
        }
    }

    fun handleEventsForTarget(target: Unit, events: Set<FrameworkEvent?>, decisionMaker: DecisionMaker,
                              codePositionManager: CodePositionManager) : HashSet<EnvironmentResult> {
        var currentEvent = 0
        val resultsPerApp = HashSet<EnvironmentResult>()

        for (event in events) {
            try {
                ++currentEvent

                //todo PAPER-EVAL ONLY
                if (!FrameworkOptions.evaluationOnly)
                // do server-side analyses
                    decisionMaker.runPreAnalysisPhase()

                // After the pre-analysis, we are able to get access to the code positions
                // Log the code position
                val codePos = codePositionManager.getCodePositionForUnit(target)

                val info = String.format("Enclosing Method: %s | %d | %s",
                        codePos.enclosingMethod, codePos.id, target.toString())
                LoggerHelper.logEvent(MyLevel.LOGGING_POINT, info)

                LoggerHelper.logEvent(Level.INFO, String.format("%s: %s/%s events (%s) ready for process",
                        codePos.id, currentEvent, events.size, event))

                LoggerHelper.logEvent(MyLevel.EXECUTION_START, "")

                repeatedlyExecuteAnalysis(decisionMaker, resultsPerApp, event)

                LoggerHelper.logEvent(MyLevel.EXECUTION_STOP, "")

                // If we have reached the goal, there is no need to try the other events
                if (resultsPerApp.any { it.isTargetReached })
                    break
            } catch (ex: Exception) {
                LoggerHelper.logEvent(MyLevel.EXCEPTION_RUNTIME, ex.message)
                ex.printStackTrace()
            }

        }
        return resultsPerApp
    }

    fun makeForcedBinReadyForInstrument() {
        val android_code_src_path = File(FrameworkOptions.frameworkDir + "bin/me/zhenhao/forced/android")
        val android_code_dst_path = File(FrameworkOptions.frameworkDir + "android-bin/me/zhenhao/forced/android")
        FileUtils.deleteDirectory(android_code_dst_path)
        FileUtils.copyDirectory(android_code_src_path, android_code_dst_path)

        val shared_code_src_path = File(FrameworkOptions.frameworkDir + "bin/me/zhenhao/forced/shared")
        val shared_code_dst_path = File(FrameworkOptions.frameworkDir + "shared-bin/me/zhenhao/forced/shared")
        FileUtils.deleteDirectory(shared_code_dst_path)
        FileUtils.copyDirectory(shared_code_src_path, shared_code_dst_path)
    }

	private fun removeStatementsForAnalysis(currentTask: AnalysisTask) {
		for (codePos in currentTask.statementsToRemove) {
			// Get the method in which to remove the statement
			val sm = Scene.v().grabMethod(codePos.methodSignature)
			if (sm == null) {
				LoggerHelper.logWarning("Method " + codePos.methodSignature + " not found")
				continue
			}

			var lineNum = 0
			val unitIt = sm.activeBody.units.iterator()
			while (unitIt.hasNext()) {
				// Is this the statement to remove?
				val u = unitIt.next()

				// Ignore statements that were added by an instrumenter
				if (!u.hasTag(InstrumentedCodeTag.name))
					if (lineNum == codePos.lineNumber)
						if (u.toString() == codePos.statement) {
							unitIt.remove()
							break
						}
				lineNum++
			}
		}
	}

	private fun appPreparationPhase(codePositionManager: CodePositionManager, config: DecisionMakerConfig) {
		LoggerHelper.logEvent(MyLevel.ANALYSIS, "Prepare app for fuzzing...")

		UtilApk.removeOldAPKs()

		val instrumenter = Instrumenter(codePositionManager, config)
		LoggerHelper.logEvent(MyLevel.INSTRUMENTATION_START, "")
		instrumenter.doInstrumentation()
		LoggerHelper.logEvent(MyLevel.INSTRUMENTATION_STOP, "")

		if (FrameworkOptions.deployApp) {
			UtilApk.jarsigner()
			UtilApk.zipalign()
		}
	}

	private fun repeatedlyExecuteAnalysis(decisionMaker: DecisionMaker,
										  results: MutableSet<EnvironmentResult>, event: FrameworkEvent?) {
		decisionMaker.initialize()

		for (seed in 0..FrameworkOptions.numSeeds -1) {
			LoggerHelper.logEvent(MyLevel.ANALYSIS, "Running analysis with seed " + seed)
			DeterministicRandom.reinitialize(seed)
			val curResult = decisionMaker.executeDecisionMaker(event)

			results.add(curResult)
			if (curResult.isTargetReached)
				LoggerHelper.logEvent(MyLevel.RUNTIME, "Target reached")
			else
				LoggerHelper.logEvent(MyLevel.RUNTIME, "Target not reached")
		}

        // print branch tracking info of all traces
        decisionMaker.printBranchTrackingForThreadId(-1)

		decisionMaker.tearDown()
	}

	private fun initializeSoot(currentTask: AnalysisTask) {
		val app = SetupApplication(FrameworkOptions.androidJarPath, FrameworkOptions.apkPath)
		app.calculateSourcesSinksEntrypoints(NullSourceSinkDefinitionProvider())

		SootConfigForAndroid().setSootOptions(Options.v())

		Options.v().set_allow_phantom_refs(true)
		Options.v().set_validate(true)
		Options.v().set_force_overwrite(true)

		// We need a callgraph
		Options.v().set_whole_program(true)
		Options.v().setPhaseOption("cg.spark", "on")

		Options.v().set_src_prec(Options.src_prec_apk)
		Options.v().set_output_format(Options.output_format_dex)

		val processDir = ArrayList<String>()
		processDir.add(FrameworkOptions.apkPath)

		// dex files to merge
		for (dexFile in currentTask.dexFilesToMerge) {
			if (!dexFile.localFileName.isEmpty() && File(dexFile.localFileName).exists())
				processDir.add(dexFile.localFileName)
			else
				throw RuntimeException("Could not find local dex file")
		}

		// app code bin
		processDir.add(UtilInstrumenter.ADDITIONAL_APP_CLASSES_BIN)
		processDir.add(UtilInstrumenter.SHARED_CLASSES_BIN)
		Options.v().set_process_dir(processDir)

		Options.v().set_android_jars(FrameworkOptions.androidJarPath)
		Options.v().set_no_writeout_body_releasing(true)

		//the bin folder has to be added to the classpath in order to
		//use the Java part for the instrumentation (JavaClassForInstrumentation)
		val androidJarPath = Scene.v().getAndroidJarPath(FrameworkOptions.androidJarPath, FrameworkOptions.apkPath)
		val sootClassPath = UtilInstrumenter.ADDITIONAL_APP_CLASSES_BIN + File.pathSeparator +
				UtilInstrumenter.SHARED_CLASSES_BIN + File.pathSeparator + androidJarPath
		Options.v().set_soot_classpath(sootClassPath)

		Scene.v().loadNecessaryClasses()

		LibraryClassPatcher().patchLibraries()

		// Create the entry point
		app.entryPointCreator.setDummyMethodName("main")
		val entryPoint = app.entryPointCreator.createDummyMain()
		entryPoint.declaringClass.setLibraryClass()
		Options.v().set_main_class(entryPoint.declaringClass.name)
		Scene.v().entryPoints = listOf<SootMethod>(entryPoint)

		PackManager.v().runPacks()
	}

	private fun printTargetLocationInfo(config: DecisionMakerConfig, codePositionManager: CodePositionManager) {
		LoggerHelper.logEvent(MyLevel.ANALYSIS, "Found " + config.allTargetLocations.size + " target location(s)")
		for (unit in config.allTargetLocations) {
			val codePos = codePositionManager.getCodePositionForUnit(unit)
			val info = String.format("Enclosing Method: %s | %d | %s", codePos.enclosingMethod, codePos.id, unit.toString())
			LoggerHelper.logEvent(MyLevel.LOGGING_POINT, info)
		}
	}

	private fun getFrameworkEvents(targetLocation: Unit, cfg: BackwardsInfoflowCFG): Set<FrameworkEvent?> {
		val eventManager = FrameworkEventManager.eventManager
		val manifest = UtilApk.getManifest()
		if (manifest != null)
			return eventManager.extractInitialEventsForReachingTarget(targetLocation, cfg, manifest)
		return emptySet()
	}

	companion object {
		private val SINGLETON = ForcedMain()

		fun v(): ForcedMain {
			return SINGLETON
		}

		@JvmStatic fun main(args: Array<String>) {
			Timer().schedule(object : TimerTask() {
				override fun run() {
					LoggerHelper.logEvent(MyLevel.TIMEOUT, "-1 | Complete analysis stopped due to timeout of 40 minutes")
					System.exit(0)
				}
			}, (40 * 60000).toLong())

			try {
				v().run(args)
				AndroidDebugBridge.terminate()
			} catch (ex: Exception) {
				val sw = StringWriter()
				val pw = PrintWriter(sw)
				ex.printStackTrace(pw)
				LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, sw.toString())
				UtilMain.writeToFile("mainException.txt", FrameworkOptions.apkPath + "\n")
			}

			LoggerHelper.logEvent(MyLevel.EXECUTION_STOP, "Analysis successfully terminated")
			//this is necessary otherwise we will wait for a max of 20 minutes for the TimerTask
			System.exit(0)
		}
	}
}
