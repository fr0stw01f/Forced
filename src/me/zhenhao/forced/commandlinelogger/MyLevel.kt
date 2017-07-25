package me.zhenhao.forced.commandlinelogger

import java.util.logging.Level

class MyLevel(name: String, value: Int) : Level(name, value) {
	companion object {
		val RUNTIME: Level = MyLevel("RUNTIME", SEVERE.intValue() + 1)
		val ANALYSIS: Level = MyLevel("ANALYSIS", SEVERE.intValue() + 1)
		val EXCEPTION_RUNTIME: Level = MyLevel("EXCEPTION_RUNTIME", SEVERE.intValue() + 1)
		val EXCEPTION_ANALYSIS: Level = MyLevel("EXCEPTION_ANALYSIS", SEVERE.intValue() + 1)
		val APKPATH: Level = MyLevel("APKPATH", SEVERE.intValue() + 1)
		val LOGGING_POINT: Level = MyLevel("LOGGING_POINT", SEVERE.intValue() + 1)
		val LOGGING_POINT_REACHED: Level = MyLevel("LOGGING_POINT_REACHED", SEVERE.intValue() + 1)
		val DECISION_REQUEST_AND_RESPONSE: Level = MyLevel("DECISION_REQUEST_AND_RESPONSE", SEVERE.intValue() + 1)
		val REBOOT: Level = MyLevel("REBOOT", SEVERE.intValue() + 1)
		val PRE_ANALYSIS_START: Level = MyLevel("PRE_ANALYSIS_START", SEVERE.intValue() + 1)
		val PRE_ANALYSIS_STOP: Level = MyLevel("PRE_ANALYSIS_STOP", SEVERE.intValue() + 1)
		val EXECUTION_START: Level = MyLevel("EXECUTION_START", SEVERE.intValue() + 1)
		val EXECUTION_STOP: Level = MyLevel("EXECUTION_STOP", SEVERE.intValue() + 1)
		val TODO: Level = MyLevel("TODO", SEVERE.intValue() + 1)
		val ADB_EVENT: Level = MyLevel("ADB_EVENT", SEVERE.intValue() + 1)
		val VMCRASH: Level = MyLevel("VMCRASH", SEVERE.intValue() + 1)
		val NO_TARGETS: Level = MyLevel("NO_TARGETS", SEVERE.intValue() + 1)
		val OPEN_APK: Level = MyLevel("OPEN_APK", SEVERE.intValue() + 1)
		val INSTRUMENTATION_START: Level = MyLevel("INSTRUMENTATION_START", SEVERE.intValue() + 1)
		val INSTRUMENTATION_STOP: Level = MyLevel("INSTRUMENTATION_STOP", SEVERE.intValue() + 1)
		val START_ACTIVITY: Level = MyLevel("START_ACTIVITY", SEVERE.intValue() + 1)
		val ANALYSIS_NAME: Level = MyLevel("ANALYSIS_NAME", SEVERE.intValue() + 1)
		val RESTART: Level = MyLevel("APPANALYSIS_RESTART", SEVERE.intValue() + 1)
		val TIMEOUT: Level = MyLevel("TIMEOUT", SEVERE.intValue() + 1)
		val TIMING_BOMB: Level = MyLevel("TIMING_BOMB", SEVERE.intValue() + 1)
		val SMT_SOLVER_VALUE: Level = MyLevel("SMT_SOLVER_VALUE", SEVERE.intValue() + 1)
		val GENTETIC_ONLY_MODE: Level = MyLevel("GENTETIC_ONLY_MODE", SEVERE.intValue() + 1)
		val DEXFILE: Level = MyLevel("DEXFILE", SEVERE.intValue() + 1)
	}
}
