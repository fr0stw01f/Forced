package me.zhenhao.forced.decisionmaker.analysis.filevalues

import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.decisionmaker.analysis.FuzzyAnalysis
import me.zhenhao.forced.decisionmaker.server.ThreadTraceManager
import me.zhenhao.forced.decisionmaker.server.TraceManager
import me.zhenhao.forced.dynamiccfg.utils.FileUtils
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest
import me.zhenhao.forced.sharedclasses.networkconnection.ServerResponse
import me.zhenhao.forced.sharedclasses.util.Pair
import soot.RefType
import soot.Unit
import soot.jimple.AssignStmt
import java.io.File
import java.util.*


class FileValuesAnalysis : FuzzyAnalysis() {

	private val codePositionManager = CodePositionManager.codePositionManagerInstance
	private var values: Set<String>? = null

	override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {
		// Read the file
		this.values = FileUtils.textFileToLineSet(RANDOM_VALUES_FILENAME)
	}

	override fun resolveRequest(clientRequest: DecisionRequest,
								threadTraceManager: ThreadTraceManager): List<AnalysisDecision> {
		val u = codePositionManager.getUnitForCodePosition(clientRequest.codePosition + 1) as? AssignStmt ?: return emptyList()
		val assignStmt = u

		// We only support strings at the moment
		if (assignStmt.leftOp.type !== RefType.v("java.lang.String"))
			return emptyList()

		val stringType = RefType.v("java.lang.String")

		// Return the dynamically-obtained strings
		val decisions = ArrayList<AnalysisDecision>(values!!.size)
		for (value in values!!) {
			val serverResponse = ServerResponse()
			serverResponse.setResponseExist(true)

			if (clientRequest.isHookAfter) {
				serverResponse.returnValue = value
			} else if (assignStmt.containsInvokeExpr()) {
				val paramValues = HashSet<Pair<Int, Any>>()
				for (i in 0..assignStmt.invokeExpr.argCount - 1) {
					val paramType = assignStmt.invokeExpr.method.getParameterType(i)
					if (paramType === stringType)
						paramValues.add(Pair<Int, Any>(i, value))
				}
				serverResponse.paramValues = paramValues
			}

			val decision = AnalysisDecision()
			decision.analysisName = getAnalysisName()
			decision.serverResponse = serverResponse
			decision.decisionWeight = 5
			decisions.add(decision)
		}
		return decisions
	}

	override fun reset() {
		// nothing to do here
	}

	override fun getAnalysisName(): String {
		return "FileValues"
	}

	companion object {

		private val RANDOM_VALUES_FILENAME = "." + File.separator + "files" + File.separator + "randomValues.txt"
	}

}
