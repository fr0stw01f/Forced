package me.zhenhao.forced

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.regex.Pattern

import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import org.apache.commons.cli.*
import kotlin.experimental.and


class FrameworkOptions {
	enum class TraceConstructionMode {
		AnalysesOnly,
		Genetic,
		RandomCombine
	}

	private val options = Options()

	init {
		setOptions()
	}

	fun parse(args: Array<String>) {
		val parser = DefaultParser()

		try {
			val cmd = parser.parse(options, args)

			if (cmd!!.hasOption("h") || cmd.hasOption("help"))
				showHelpMessage()

			//mandatory options
			apkPath = cmd.getOptionValue("apk")
			androidJarPath = cmd.getOptionValue("androidJar")
			//packageName = cmd.getOptionValue("packageName");
			resultsDir = cmd.getOptionValue("resultsDir")
			frameworkDir = cmd.getOptionValue("frameworkDir")
			devicePort = cmd.getOptionValue("devicePort")

			val devicePortInt = Integer.parseInt(devicePort)
			if (devicePortInt < 5554 || devicePortInt > 5680)
				throw RuntimeException("port number has to be an integer number between 5554 and 5680")
			//if(devicePortInt%2 != 0)
			//throw new RuntimeException("port number has to be an even integer number");

			apkMD5 = generateMD5OfFile(apkPath)

			//optional options
			if (cmd.hasOption("KEYSTORE_PATH")) {
				KEYSTORE_PATH = cmd.getOptionValue("KEYSTORE_PATH")
			}
			if (cmd.hasOption("KEYSTORE_NAME")) {
				KEYSTORE_NAME = cmd.getOptionValue("KEYSTORE_NAME")
			}
			if (cmd.hasOption("KEYSTORE_PASSWORD")) {
				KEYSTORE_PASSWORD = cmd.getOptionValue("KEYSTORE_PASSWORD")
			}
			if (cmd.hasOption("BUILD_TOOLS")) {
				BUILD_TOOLS = cmd.getOptionValue("BUILD_TOOLS")
			}
			if (cmd.hasOption("KEYSTORE_PATH") &&
					cmd.hasOption("KEYSTORE_NAME") &&
					cmd.hasOption("KEYSTORE_PASSWORD") &&
					cmd.hasOption("BUILD_TOOLS"))
				deployApp = true

			if (cmd.hasOption("PLATFORM_TOOLS")) {
				PLATFORM_TOOLS = cmd.getOptionValue("PLATFORM_TOOLS")
			}
			if (cmd.hasOption("Z3SCRIPT_LOCATION")) {
				Z3SCRIPT_LOCATION = cmd.getOptionValue("Z3SCRIPT_LOCATION")
			}
			if (cmd.hasOption("TEST_SERVER")) {
				testServer = true
			}
			if (cmd.hasOption("RECORD_PATH_EXECUTION")) {
				recordPathExecution = true
			}
			if (cmd.hasOption("MERGE_DATAFLOWS")) {
				mergeDataFlows = true
			}

			if (cmd.hasOption("nbSeeds")) {
				nbSeeds = Integer.parseInt(cmd.getOptionValue("nbSeeds"))
			}

			if (cmd.hasOption("inactivityTimeout")) {
				inactivityTimeout = Integer.parseInt(cmd.getOptionValue("inactivityTimeout"))
			}

			if (cmd.hasOption("maxRestarts")) {
				maxRestarts = Integer.parseInt(cmd.getOptionValue("maxRestarts"))
			}
			if (cmd.hasOption("enableLogcatViewer")) {
				enableLogcatViewer = true
			}
			if (cmd.hasOption("traceConstructionMode")) {
				traceConstructionMode = TraceConstructionMode.valueOf(
						cmd.getOptionValue("traceConstructionMode"))
			}
			if (cmd.hasOption("evaluationJustStartApp")) {
				evaluationJustStartApp = true
				evaluationOnly = true
			}
			if (cmd.hasOption("evaluationStartAppAndSimpleEvent")) {
				evaluationStartAppAndSimpleEvent = true
				evaluationOnly = true
			}
		} catch (e: Exception) {
			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, e.message)
			e.printStackTrace()
			showHelpMessage()
			System.exit(1)
		}

	}

	private fun setOptions() {
		options.addOption("h", "help", false, "help")

		options.addOption(Option.builder()
				.desc("Path to apk")
				.required()
				.hasArg()
				.longOpt("apk")
				.build()
		)

		options.addOption(Option.builder()
				.desc("Path to android jar location")
				.required()
				.hasArg()
				.longOpt("androidJar")
				.build())

		options.addOption(Option.builder()
				.desc("Directory for analysis results")
				.required()
				.hasArg()
				.longOpt("resultsDir")
				.build())

		options.addOption(Option.builder()
				.desc("Path to the FuzzDroid Framework")
				.required()
				.hasArg()
				.longOpt("frameworkDir")
				.build())

		options.addOption(Option.builder()
				.desc("Please provide a port for the device; it has to be an even integer number between 5554 and 5680")
				.required()
				.hasArg()
				.longOpt("devicePort")
				.build())

		options.addOption("KEYSTORE_PATH", true, "Path to your keystore")
		options.addOption("KEYSTORE_NAME", true, "Name of your keystore")
		options.addOption("KEYSTORE_PASSWORD", true, "Password of your keystore")
		options.addOption("BUILD_TOOLS", true, "Path to build-tools folder in sdk")
		options.addOption("PLATFORM_TOOLS", true, "Path to platform-tools folder")
		options.addOption("TEST_SERVER", false, "runs only the server-component")
		options.addOption("Z3SCRIPT_LOCATION", true, "path to the Z3str2 python script within the Z3str2 project (e.g. /root/project/Z3/Z3-str.py)")
		options.addOption("RECORD_PATH_EXECUTION", "RECORD", true, "Path to a file location where of a .dot file. The .dot file contains the path execution (method-access, method-callers, conditions taken and return-stmts) of the app.")
		options.addOption("packageName", true, "Package name of your app")
		options.addOption("nbSeeds", true, "How often to repeat the entire experiment with a different seed. Default: " + nbSeeds)
		options.addOption("inactivityTimeout", true, "After how many seconds of inactivity to restart the app. Default: " + inactivityTimeout)
		options.addOption("maxRestarts", true, "Maximum number of restarts of the app (per experiment). -1 means infinitely often. Default: " + maxRestarts)
		options.addOption("MERGE_DATAFLOWS", false, "Merging dataflows can improve the extraction of concrete fuzzing values")
		options.addOption("ORIGINAL_APK_PATH", true, "If one needs to analyse a modfied version of an apk, we still keep the original apk. Path to original apk stored in ORIGINAL_APK_PATH")
		options.addOption("enableHooks", true, "enables specific hooks that are usually not enabled. Usability: enableHooks \"integrity\"")
		options.addOption("enableLogcatViewer", false, "once enabled, we will log whether the VM crashes (useful for bug hunting)")

		options.addOption("evaluationStartAppAndSimpleEvent", false, "EVALUATION-REASOSN")
		options.addOption("evaluationJustStartApp", false, "EVALUATION-REASOSN")
	}

	private fun showHelpMessage() {
		val formatter = HelpFormatter()
		formatter.printHelp("Android Environment Constraint Generator", options)
	}

	private fun generateMD5OfFile(apkPath: String): String {
		var md5 = ""
		try {
			val md = MessageDigest.getInstance("MD5")
			val fis = FileInputStream(apkPath)
			val dataBytes = ByteArray(1024)

			var nread = fis.read(dataBytes)

			while (nread != -1) {
				md.update(dataBytes, 0, nread)
				nread = fis.read(dataBytes)
			}

			val mdbytes = md.digest()

			//convert the byte to hex format
			val sb = StringBuffer("")
			for (i in mdbytes.indices) {
				sb.append(Integer.toString((mdbytes[i] and 0xff.toByte()) + 0x100, 16).substring(1))
			}

			md5 = sb.toString()
		} catch (ex: Exception) {
			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
			ex.printStackTrace()
			System.exit(-1)
		}

		return md5
	}

	companion object {
//		val homeDir = "/home/zhenhao"
		val homeDir = "/Users/tom"

		var androidJarPath = homeDir + "/Work/Android/Sdk/platforms/"
		var resultsDir = homeDir + "/Work/FuzzDroid/"
		var frameworkDir = homeDir + "/IdeaProjects/Forced/"
		var devicePort = "5555"

		var apkPath = ""
		var apkMD5 = ""

		var nbSeeds = 1
		var forceTimeout = 20	   //60
		var inactivityTimeout = 10   //40
		var maxRestarts = 1		 //5

		var uninstallWaitingTime = 3000
		var tryStartAppWaitingTime = 3000

		var KEYSTORE_PATH = homeDir + "/keystores/zhenhao.keystore"
		var KEYSTORE_NAME = "zhenhao"
		var KEYSTORE_PASSWORD = "pursuits23"
		var BUILD_TOOLS = homeDir + "/Library/Android/Sdk/build-tools/25.0.3/"
		var PLATFORM_TOOLS = homeDir + "/Library/Android/Sdk/platform-tools/"
		var Z3SCRIPT_LOCATION = homeDir + "/Work/Z3-str-master/Z3-str.py"

		var deployApp = true
		var recordPathExecution = false
		var mergeDataFlows = false

		var testServer = false

		var evaluationJustStartApp = false
		var evaluationStartAppAndSimpleEvent = false
		var evaluationOnly = false

		var enableLogcatViewer = true
		var traceConstructionMode = TraceConstructionMode.Genetic


		fun getApkName(): String {
			val tokens = apkPath.split(Pattern.quote(File.separator))
			val tmp = tokens[tokens.size - 1]

			return tmp.substring(0, tmp.length - 4)
		}
	}
}

