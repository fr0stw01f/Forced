package me.zhenhao.forced.commandlinelogger

import me.zhenhao.forced.FrameworkOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

object LoggerHelper {
	private val log = Logger.getLogger(LoggerHelper::class.java.name)
	private var fh: FileHandler? = null

	fun logInfo(message: String) {
		log.log(Level.INFO, message)
		if (fh != null)
			fh!!.flush()
	}

	fun logWarning(message: String) {
		//		log.log(Level.WARNING, message);
		System.err.println(message)
		if (fh != null)
			fh!!.flush()
	}

	fun logEvent(level: Level, message: String?) {
		log.log(level, message)
		if (fh != null)
			fh!!.flush()
	}

	fun initialize() {
		log.useParentHandlers = false
		val conHdlr = ConsoleHandler()
		conHdlr.formatter = object : Formatter() {
			override fun format(record: LogRecord): String {
				val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(record.millis))
				return "[" + timestamp + " - " + record.level + "] " + record.message + "\n"
			}
		}
		log.addHandler(conHdlr)

		//analysis results:
		try {
			val resultsDir = File(FrameworkOptions.resultsDir)
			if (!resultsDir.exists())
				resultsDir.mkdir()
			val logFile = String.format("%s/%s.xml", resultsDir.absolutePath, FrameworkOptions.apkMD5)
			fh = FileHandler(logFile)
		} catch (e: SecurityException) {
			e.printStackTrace()
		} catch (e: IOException) {
			e.printStackTrace()
		}

		log.addHandler(fh)
	}
}
