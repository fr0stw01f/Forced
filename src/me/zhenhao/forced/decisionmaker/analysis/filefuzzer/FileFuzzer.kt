package me.zhenhao.forced.decisionmaker.analysis.filefuzzer

import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.decisionmaker.analysis.FuzzyAnalysis
import me.zhenhao.forced.decisionmaker.server.ThreadTraceManager
import me.zhenhao.forced.decisionmaker.server.TraceManager
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest
import me.zhenhao.forced.sharedclasses.networkconnection.FileFormat
import me.zhenhao.forced.sharedclasses.networkconnection.ServerResponse
import me.zhenhao.forced.sharedclasses.networkconnection.serializables.FileFuzzingSerializableObject
import me.zhenhao.forced.sharedclasses.util.Pair
import soot.Scene
import soot.SootMethod
import soot.Unit
import soot.jimple.Stmt
import soot.jimple.infoflow.Infoflow
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder
import soot.jimple.infoflow.source.ISourceSinkManager
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper
import java.util.*


class FileFuzzer : FuzzyAnalysis() {
	internal var codePositionManager = CodePositionManager.codePositionManagerInstance

	//code position to file format map
	private val fileFormatsFromDataflow = HashMap<Int, FileFormat>()

	override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {
		runDataflowAnalysis()
	}

	override fun resolveRequest(clientRequest: DecisionRequest,
								threadTraceManager: ThreadTraceManager): List<AnalysisDecision> {
		val decisions = ArrayList<AnalysisDecision>()

		var codePosID = clientRequest.codePosition
		//we have to add one to it
		codePosID += 1

		//decision available: concrete file format
		if (fileFormatsFromDataflow.keys.contains(codePosID)) {
			//we always add an event for "no action" to it, since it can be the
			//case that the program will add the file at a later stage or it is not required
			//to create a file
			val response = ServerResponse()
			response.setResponseExist(false)
			response.analysisName = getAnalysisName()
			val finalDecision = AnalysisDecision()
			finalDecision.analysisName = getAnalysisName()
			finalDecision.decisionWeight = 8
			finalDecision.serverResponse = response
			decisions.add(finalDecision)

			val decision = getFileFormatFromDataflow(codePosID)
			decision.analysisName = getAnalysisName()
			decisions.add(decision)
		} else if (fileFormatAvailable(codePosID)) {
			val decision = getFileFormat(codePosID)
			decision!!.analysisName = getAnalysisName()
			decisions.add(decision)
		} else {
			val response = ServerResponse()
			response.analysisName = getAnalysisName()
			response.setResponseExist(false)
			val finalDecision = AnalysisDecision()
			finalDecision.decisionWeight = 8
			finalDecision.serverResponse = response
			decisions.add(finalDecision)
		}//no decision available
		//decision available: text-based file format

		return decisions
	}

	override fun reset() {
	}

	override fun getAnalysisName(): String {
		return "FileFuzzer"
	}

	private fun runDataflowAnalysis() {
		try {
			Scene.v().orMakeFastHierarchy

			val infoflow = InplaceInfoflow()
			//			InfoflowConfiguration.setAccessPathLength(2);
			infoflow.setPathBuilderFactory(DefaultPathBuilderFactory(
					PathBuilder.ContextSensitive, true))
			infoflow.taintWrapper = EasyTaintWrapper(TAINT_WRAPPER_PATH)
			infoflow.config.enableExceptionTracking = false
			infoflow.config.enableArraySizeTainting = false
			//			infoflow.getConfig().setCallgraphAlgorithm(CallgraphAlgorithm.CHA);

			println("Running data flow analysis...")
			val pmp = PermissionMethodParser.fromFile(SOURCES_SINKS_FILE)
			val srcSinkManager = AccessPathBasedSourceSinkManager(pmp.sources, pmp.sinks)

			infoflow.addResultsAvailableHandler(FileFuzzerResultsAvailableHandler(fileFormatsFromDataflow))
			infoflow.runAnalysis(srcSinkManager)
		} catch (ex: Exception) {
			ex.printStackTrace()
		}

	}


	private fun getFileFormatFromDataflow(codePosID: Int): AnalysisDecision {
		val unit = codePositionManager.getUnitForCodePosition(codePosID)
		if (unit is Stmt) {
			val stmt = unit
			if (stmt.containsInvokeExpr()) {
				val inv = stmt.invokeExpr
				val sm = inv.method
				val paramValue = retrieveCorrectFileInformation(sm)

				val response = ServerResponse()
				response.analysisName = getAnalysisName()
				response.setResponseExist(true)
				response.paramValues = setOf(paramValue)
				val finalDecision = AnalysisDecision()
				finalDecision.analysisName = getAnalysisName()
				finalDecision.decisionWeight = 8
				finalDecision.serverResponse = response
				return finalDecision
			} else
				return noResults()
		} else {
			return noResults()
		}
	}


	private fun retrieveCorrectFileInformation(sm: SootMethod): Pair<Int, Any>? {
		//property files
		if (sm.subSignature == "java.io.FileInputStream openFileInput(java.lang.String)")
			return Pair(0, FileFuzzingSerializableObject(FileFormat.PROPERTIES, 0))

		return null
	}


	private inner class InplaceInfoflow : Infoflow() {

		public override fun runAnalysis(sourcesSinks: ISourceSinkManager) {
			super.runAnalysis(sourcesSinks)
		}
	}


	private fun noResults(): AnalysisDecision {
		val response = ServerResponse()
		response.analysisName = getAnalysisName()
		response.setResponseExist(false)
		val finalDecision = AnalysisDecision()
		finalDecision.decisionWeight = 8
		finalDecision.serverResponse = response
		return finalDecision
	}


	private fun fileFormatAvailable(codePosID: Int): Boolean {
		val unit = codePositionManager.getUnitForCodePosition(codePosID)
		if (unit is Stmt) {
			val stmt = unit
			if (stmt.containsInvokeExpr()) {
				val inv = stmt.invokeExpr
				val sm = inv.method
				val methodSig = sm.signature

				when (methodSig) {
					"<android.content.Context: java.io.FileInputStream openFileInput(java.lang.String)>",
					"<java.io.File: void <init>(java.io.File,java.lang.String)>",
					"<java.io.File: void <init>(java.lang.String,java.lang.String)>",
					"<java.io.File: void <init>(java.lang.String)>",
					"<java.io.File: void <init>(java.net.URI)>",
					"<android.content.ContextWrapper: java.io.FileInputStream openFileInput(java.lang.String)>",
					"<android.content.Context: java.io.File getFileStreamPath(java.lang.String)>",
					"<android.content.Context: java.io.File getDir(java.lang.String,int)>",
					"<android.content.Context: java.io.File getDatabasePath(java.lang.String)>",
					"<android.content.ContextWrapper: java.io.File getFileStreamPath(java.lang.String)>",
					"<android.content.ContextWrapper: java.io.File getDir(java.lang.String,int)>",
					"<android.content.ContextWrapper: java.io.File getDatabasePath(java.lang.String)>",
					"<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.io.File,android.database.sqlite.SQLiteDatabase\$CursorFactory)>",
					"<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory)>",
					"<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory,android.database.DatabaseErrorHandler)>",
					"<android.content.ContextWrapper: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory)>",
					"<android.content.ContextWrapper: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory,android.database.DatabaseErrorHandler)>"
							-> return true
					else	-> return false
				}
			} else
				return false
		} else
			return false
	}


	private fun getFileFormat(codePosID: Int): AnalysisDecision? {
		val response = ServerResponse()
		response.analysisName = getAnalysisName()
		response.setResponseExist(false)

		val unit = codePositionManager.getUnitForCodePosition(codePosID)
		if (unit is Stmt) {
			val stmt = unit
			if (stmt.containsInvokeExpr()) {
				val inv = stmt.invokeExpr
				val sm = inv.method
				val methodSig = sm.signature
				val param: Pair<Int, Any>

				when (methodSig) {
					"<android.content.Context: java.io.FileInputStream openFileInput(java.lang.String)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 0))
						response.paramValues = setOf(param)
					}

					"<java.io.File: void <init>(java.io.File,java.lang.String)>" -> {
						response.setResponseExist(true)
						//there is no need for a specific param index (=> -1); we need the complete file object anyway
						param = Pair<Int, Any>(-1, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 1))
						response.paramValues = setOf(param)
					}

					"<java.io.File: void <init>(java.lang.String,java.lang.String)>" -> {
						response.setResponseExist(true)
						//there is no need for a specific param index (=> -1); we need the complete file object anyway
						param = Pair<Int, Any>(-1, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 1))
						response.paramValues = setOf(param)
					}

					"<java.io.File: void <init>(java.lang.String)>" -> {
						response.setResponseExist(true)
						//there is no need for a specific param index (=> -1); we need the complete file object anyway
						param = Pair<Int, Any>(-1, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 1))
						response.paramValues = setOf(param)
					}

					"<java.io.File: void <init>(java.net.URI)>" -> {
						response.setResponseExist(true)
						//there is no need for a specific param index (=> -1); we need the complete file object anyway
						param = Pair<Int, Any>(-1, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 1))
						response.paramValues = setOf(param)
					}

					"<android.content.ContextWrapper: java.io.FileInputStream openFileInput(java.lang.String)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 0))
						response.paramValues = setOf(param)
					}

					"<android.content.Context: java.io.File getFileStreamPath(java.lang.String)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 0))
						response.paramValues = setOf(param)
					}

					"<android.content.Context: java.io.File getDir(java.lang.String,int)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DIRECTORY, 0))
						response.paramValues = setOf(param)
					}

					"<android.content.Context: java.io.File getDatabasePath(java.lang.String)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DATABASE, 2))
						response.paramValues = setOf(param)
					}

					"<android.content.ContextWrapper: java.io.File getFileStreamPath(java.lang.String)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.UNKNOWN, 0))
						response.paramValues = setOf(param)
					}

					"<android.content.ContextWrapper: java.io.File getDir(java.lang.String,int)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DIRECTORY, 0))
						response.paramValues = setOf(param)
					}

					"<android.content.ContextWrapper: java.io.File getDatabasePath(java.lang.String)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DATABASE, 2))
						response.paramValues = setOf(param)
					}

					"<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.io.File,android.database.sqlite.SQLiteDatabase\$CursorFactory)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DATABASE, 1))
						response.paramValues = setOf(param)
					}

					"<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DATABASE, 0))
						response.paramValues = setOf(param)
					}

					"<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory,android.database.DatabaseErrorHandler)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DATABASE, 0))
						response.paramValues = setOf(param)
					}

					"<android.content.ContextWrapper: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DATABASE, 0))
						response.paramValues = setOf(param)
					}

					"<android.content.ContextWrapper: android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase\$CursorFactory,android.database.DatabaseErrorHandler)>" -> {
						response.setResponseExist(true)
						param = Pair<Int, Any>(0, FileFuzzingSerializableObject(FileFormat.DATABASE, 0))
						response.paramValues = setOf(param)
					}
				}
			}
		}

		if (response.doesResponseExist()) {
			val finalDecision = AnalysisDecision()
			finalDecision.analysisName = getAnalysisName()
			finalDecision.decisionWeight = 8
			finalDecision.serverResponse = response
			return finalDecision
		} else
			return null
	}

	companion object {
		private val TAINT_WRAPPER_PATH = FrameworkOptions.frameworkDir + "src/me/zhenhao/forced/decisionmaker/analysis/EasyTaintWrapperSource.txt"
		private val SOURCES_SINKS_FILE = FrameworkOptions.frameworkDir + "src/me/zhenhao/forced/decisionmaker/analysis/filefuzzer/SourcesAndSinks.txt"
		val FUZZY_FILES_DIR = FrameworkOptions.frameworkDir + "fuzzyFiles/"
	}
}
