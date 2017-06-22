package de.tu_darmstadt.sse.commandlinelogger

import java.util.logging.Level

class MyLevel(name: String, value: Int) : Level(name, value) {
    companion object {
        val RUNTIME: Level = MyLevel("RUNTIME", Level.SEVERE.intValue() + 1)
        val ANALYSIS: Level = MyLevel("ANALYSIS", Level.SEVERE.intValue() + 1)
        val EXCEPTION_RUNTIME: Level = MyLevel("EXCEPTION_RUNTIME", Level.SEVERE.intValue() + 1)
        val EXCEPTION_ANALYSIS: Level = MyLevel("EXCEPTION_ANALYSIS", Level.SEVERE.intValue() + 1)
        val APKPATH: Level = MyLevel("APKPATH", Level.SEVERE.intValue() + 1)
        val LOGGING_POINT: Level = MyLevel("LOGGING_POINT", Level.SEVERE.intValue() + 1)
        val LOGGING_POINT_REACHED: Level = MyLevel("LOGGING_POINT_REACHED", Level.SEVERE.intValue() + 1)
        val DECISION_REQUEST_AND_RESPONSE: Level = MyLevel("DECISION_REQUEST_AND_RESPONSE", Level.SEVERE.intValue() + 1)
        val REBOOT: Level = MyLevel("REBOOT", Level.SEVERE.intValue() + 1)
        val PRE_ANALYSIS_START: Level = MyLevel("PRE_ANALYSIS_START", Level.SEVERE.intValue() + 1)
        val PRE_ANALYSIS_STOP: Level = MyLevel("PRE_ANALYSIS_STOP", Level.SEVERE.intValue() + 1)
        val EXECUTION_START: Level = MyLevel("EXECUTION_START", Level.SEVERE.intValue() + 1)
        val EXECUTION_STOP: Level = MyLevel("EXECUTION_STOP", Level.SEVERE.intValue() + 1)
        val TODO: Level = MyLevel("TODO", Level.SEVERE.intValue() + 1)
        val ADB_EVENT: Level = MyLevel("ADB_EVENT", Level.SEVERE.intValue() + 1)
        val VMCRASH: Level = MyLevel("VMCRASH", Level.SEVERE.intValue() + 1)
        val NO_TARGETS: Level = MyLevel("NO_TARGETS", Level.SEVERE.intValue() + 1)
        val OPEN_APK: Level = MyLevel("OPEN_APK", Level.SEVERE.intValue() + 1)
        val INSTRUMENTATION_START: Level = MyLevel("INSTRUMENTATION_START", Level.SEVERE.intValue() + 1)
        val INSTRUMENTATION_STOP: Level = MyLevel("INSTRUMENTATION_STOP", Level.SEVERE.intValue() + 1)
        val START_ACTIVITY: Level = MyLevel("START_ACTIVITY", Level.SEVERE.intValue() + 1)
        val ANALYSIS_NAME: Level = MyLevel("ANALYSIS_NAME", Level.SEVERE.intValue() + 1)
        val RESTART: Level = MyLevel("APPANALYSIS_RESTART", Level.SEVERE.intValue() + 1)
        val TIMEOUT: Level = MyLevel("TIMEOUT", Level.SEVERE.intValue() + 1)
        val TIMING_BOMB: Level = MyLevel("TIMING_BOMB", Level.SEVERE.intValue() + 1)
        val SMT_SOLVER_VALUE: Level = MyLevel("SMT_SOLVER_VALUE", Level.SEVERE.intValue() + 1)
        val GENTETIC_ONLY_MODE: Level = MyLevel("GENTETIC_ONLY_MODE", Level.SEVERE.intValue() + 1)
        val DEXFILE: Level = MyLevel("DEXFILE", Level.SEVERE.intValue() + 1)
    }
}
