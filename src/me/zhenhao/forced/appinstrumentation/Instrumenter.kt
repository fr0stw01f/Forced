package me.zhenhao.forced.appinstrumentation

import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionWriter
import me.zhenhao.forced.apkspecific.UtilApk
import me.zhenhao.forced.appinstrumentation.transformer.*
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.DecisionMakerConfig
import org.xmlpull.v1.XmlPullParserException
import soot.*
import soot.Unit
import soot.jimple.InvokeStmt
import soot.jimple.Jimple
import soot.jimple.infoflow.android.manifest.ProcessManifest
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.*


class Instrumenter(private val codePositionManager: CodePositionManager, private val config: DecisionMakerConfig) {

	private fun manipulateManifest(apkFile: String) {
		//manipulate AndroidManifest.xml
		var androidManifest: ProcessManifest? = null
		try {
			androidManifest = ProcessManifest(apkFile)
			UtilApk.manipulateAndroidManifest(androidManifest)
			addTemporalManifestChanges(androidManifest)
		} catch (e: IOException) {
			e.printStackTrace()
		} catch (e: XmlPullParserException) {
			e.printStackTrace()
		} finally {
			if (androidManifest != null)
				androidManifest.close()
		}
	}

	private fun addFilesToZip(instrumentedAPKPath: String, armHookingFilePath: String, x86HookingFilePath: String) {
		var instrumentedAPKPath2 = instrumentedAPKPath
		// Make sure that we don't add any removable parts
		instrumentedAPKPath2 = instrumentedAPKPath2.replace("/./", "/")
		instrumentedAPKPath2 = instrumentedAPKPath2.replace("\\.\\", "\\")

		val apkFile = File(instrumentedAPKPath2)
		if (!apkFile.exists())
			throw RuntimeException("Output APK file not found: " + apkFile)

		val uri: URI
		try {
			var uriPath = apkFile.toURI().toString()
			uriPath = uriPath.replace("%20", " ")		// Windows hiccups
			uri = URI("jar", uriPath, null)
		} catch (ex: URISyntaxException) {
			throw RuntimeException(ex)
		}

		val env = Collections.singletonMap("create", "true")

		try {
			FileSystems.newFileSystem(uri, env).use { fs ->
				val armDir = fs.getPath("lib", "armeabi")
				val x86Dir = fs.getPath("lib", "x86")
				val armFile = fs.getPath("lib", "armeabi", "libZHook.so")
				val x86File = fs.getPath("lib", "x86", "libZHook.so")

				val perms = PosixFilePermissions.fromString("rwxrwx--x")
				val fileAttributes = PosixFilePermissions.asFileAttribute(perms)

				Files.createDirectories(armDir, fileAttributes)
				Files.createDirectories(x86Dir, fileAttributes)

				val armSrc = Paths.get(armHookingFilePath)
				val x86Src = Paths.get(x86HookingFilePath)

				if (!Files.exists(armFile))
					Files.copy(armSrc, armFile)
				if (!Files.exists(x86File))
					Files.copy(x86Src, x86File)
			}
		} catch (e: Exception) {
			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "There is a problem in addFilesToZip:\n")
			e.printStackTrace()
		}

	}


	fun doInstrumentation() {
		LoggerHelper.logInfo("Started instrumentation...")

		//get the manifest
		var manifest: ProcessManifest? = null
		try {
			val instrumentedAPK = File(FrameworkOptions.apkPath)
			try {
				manifest = ProcessManifest(instrumentedAPK)
			} finally {
				if (manifest != null)
					manifest.close()
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
		}

		if (manifest == null) {
			throw Exception("Fail to get manifest.")
		}

		executeTransformers(manifest)
		//todo PAPER-EVAL ONLY
		if(!FrameworkOptions.evaluationOnly)
			initializeBytecodeLogger(manifest)

		PackManager.v().writeOutput()

		try {
			//todo PAPER-EVAL ONLY
			if (!FrameworkOptions.evaluationOnly) {
				//hooking & path-tracking
				//postProcessForHookingFunctionality(UtilInstrumenter.SOOT_OUTPUT_APK);
				manipulateManifest(UtilInstrumenter.SOOT_OUTPUT_APK)
				//codePositionTracking
				postProcessForPositionTracking()
			}

			LoggerHelper.logInfo("Finished instrumentation...")
		} catch (e: Exception) {
			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "There is a problem in the instrumentation phase: " + e.message)
			e.printStackTrace()
		}

	}


	private fun executeTransformers(manifest: ProcessManifest) {
		// We first need to retrieve some information from the manifest
		val constructors = manifest.entryPointClasses
				.map { "<$it: void <init>()>" }
				.toSet()

		val conditionTracking = BranchTracking()
		val codePositionTracking = CodePositionTracking(codePositionManager)
		val dynamicCallGraphTracking = DynamicCallGraphTracking(codePositionManager)
		val pathExecutionTransformer = PathExecutionTransformer()
		val goalReachedTracking = GoalReachedTracking(config.allTargetLocations)
		val timingBombs = TimingBombTransformer()
		val dummyMethods = DummyMethodHookTransformer()
		val dynamicValues = DynamicValueTransformer(true)
		val classLoaders = ClassLoaderTransformer()

		for (sc in Scene.v().applicationClasses)
			for (sm in sc.methods)
				if (sm.isConcrete) {
					val body = sm.activeBody
					//todo PAPER-EVAL ONLY
					if (!FrameworkOptions.evaluationOnly) {
						conditionTracking.transform(body)
						//dynamicCallGraphTracking.transform(body)
					}
					codePositionTracking.transform(body)
					//if (FrameworkOptions.recordPathExecution)
						//pathExecutionTransformer.transform(body)
					//goalReachedTracking.transform(body)
					//todo PAPER-EVAL ONLY
					if (!FrameworkOptions.evaluationOnly) {
						//timingBombs.transform(body)
						//dummyMethods.transform(body)
						//dynamicValues.transform(body)
					}
					classLoaders.transform(body)

//				  body.validate()
				}
		//todo PAPER-EVAL ONLY
		if (!FrameworkOptions.evaluationOnly)
			CrashReporterInjection(constructors).transform()
		GlobalInstanceTransformer().transform()
	}

	private fun initializeBytecodeLogger(manifest: ProcessManifest) {
		var applicationName: String? = manifest.applicationName
		//case 1
		if (applicationName != null) {
			if (applicationName.startsWith(".")) {
				val packageName = manifest.packageName ?:
						throw RuntimeException("There is a problem with the package name")
				applicationName = packageName + applicationName
			}
			val applicationSootClass = Scene.v().getSootClass(applicationName)
			if (applicationSootClass != null) {
				var attachMethodName = String.format("<%s: void attachBaseContext(android.content.Context)>",
						applicationName)
				var attachMethod: SootMethod? = Scene.v().grabMethod(attachMethodName)
				//case 1
				if (attachMethod != null) {
					val body = attachMethod.activeBody
					val contextParam = body.getParameterLocal(0)

					val unitsToInstrument = ArrayList<Unit>()
					val hookingHelperApplicationClassAttachMethodName =
							String.format("<%s: void initialize(android.content.Context)>",
									UtilInstrumenter.JAVA_CLASS_FOR_INSTRUMENTATION)
					val hookingHelperApplicationClassAttachMethod = Scene.v().
							getMethod(hookingHelperApplicationClassAttachMethodName) ?:
							throw RuntimeException("this should not happen")
					val ref = hookingHelperApplicationClassAttachMethod.makeRef()
					val invExpr = Jimple.v().newStaticInvokeExpr(ref, contextParam)
					unitsToInstrument.add(Jimple.v().newInvokeStmt(invExpr))


					val instrumentAfterUnit: Unit = body.units
							.firstOrNull { it is InvokeStmt && it.invokeExpr.method.subSignature ==
									"void attachBaseContext(android.content.Context)" }
							?: throw RuntimeException("this should not happen")

					body.units.insertAfter(unitsToInstrument, instrumentAfterUnit)
				} else {
					attachMethodName = String.format("<%s: void attachBaseContext(android.content.Context)>",
							UtilInstrumenter.HELPER_APPLICATION_FOR_FORCED_CODE_INIT)
					attachMethod = Scene.v().grabMethod(attachMethodName)
					if (attachMethod == null)
						throw RuntimeException("this should not happen")

					val params = ArrayList<Type>()
					val contextClass = Scene.v().getSootClass("android.content.Context")
					params.add(contextClass.type)
					val newAttachMethod = SootMethod("attachBaseContext", params, VoidType.v())
					newAttachMethod.modifiers = soot.Modifier.PROTECTED
					newAttachMethod.activeBody = attachMethod.activeBody
					applicationSootClass.addMethod(newAttachMethod)
				}//case 2

				//there is no need for our Application class
				Scene.v().getSootClass(UtilInstrumenter.HELPER_APPLICATION_FOR_FORCED_CODE_INIT).setLibraryClass()
			} else {
				throw RuntimeException("There is a problem with the Application class!")
			}
		} else {
			//there is no need for any instrumentation since the Application class is set to application-class.
		}//case 3
	}

	private fun addTemporalManifestChanges(androidManifest: ProcessManifest) {
		var manifestFile: File? = null
		try {
			//temporarily save the modified AndroidManifest
			manifestFile = File.createTempFile("AndroidManifest.xml", null)
			val fos = FileOutputStream(manifestFile!!.path)
			val output = androidManifest.output
			fos.write(output)
			fos.close()

			val files = ArrayList<File>()
			files.add(manifestFile)
			val paths = HashMap<String, String>()
			paths.put(manifestFile.absolutePath, "AndroidManifest.xml")
			//add the modified AndroidManifest into the original APK
			androidManifest.apk.addFilesToApk(files, paths)
		} catch (ex: Exception) {
			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
			ex.printStackTrace()
			System.exit(-1)
		} finally {
			if (manifestFile != null && manifestFile.exists())
				manifestFile.delete()
		}
	}

	private fun postProcessForPositionTracking() {
		val writer = CodePositionWriter(codePositionManager)
		try {
			writer.writeCodePositions("CodePositions.log")
		} catch (e: FileNotFoundException) {
			System.err.println("Could not write code position file")
			e.printStackTrace()
		}

	}

}
