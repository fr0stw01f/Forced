package me.zhenhao.forced.decisionmaker.analysis.integritycheck

import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.analysis.AnalysisDecision
import me.zhenhao.forced.decisionmaker.analysis.FuzzyAnalysis
import me.zhenhao.forced.decisionmaker.server.ThreadTraceManager
import me.zhenhao.forced.decisionmaker.server.TraceManager
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest
import me.zhenhao.forced.sharedclasses.networkconnection.ServerResponse
import me.zhenhao.forced.sharedclasses.networkconnection.serializables.SignatureSerializableObject
import soot.Unit
import java.io.InputStream
import java.security.cert.Certificate
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile


class AntiIntegrityCheckFuzzyAnalysis : FuzzyAnalysis() {

	private val certificates = HashSet<SignatureSerializableObject>()

	override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {
		val certs = extractCertificates()
		createSignatureSerializableObjects(certs)
	}

	override fun resolveRequest(clientRequest: DecisionRequest,
								threadTraceManager: ThreadTraceManager): List<AnalysisDecision> {
		val decisions = ArrayList<AnalysisDecision>()
		if (clientRequest.loggingPointSignature == "<DummyWrapper: android.content.pm.PackageInfo dummyWrapper_getPackageInfo(android.content.pm.PackageManager,java.lang.String,int)>") {
			for (encodedCertificate in certificates) {
				val response = ServerResponse()
				response.analysisName = getAnalysisName()
				response.setResponseExist(true)
				response.returnValue = encodedCertificate
				val finalDecision = AnalysisDecision()
				finalDecision.analysisName = getAnalysisName()
				finalDecision.decisionWeight = 8
				finalDecision.serverResponse = response

				decisions.add(finalDecision)
			}
		}
		return decisions
	}

	override fun reset() {
	}

	override fun getAnalysisName(): String {
		return "IntegrityCheck"
	}


	private fun extractCertificates(): Set<Certificate> {
		val certificates = HashSet<Certificate>()
		try {
			val apkPath = FrameworkOptions.apkPath
			val jf = JarFile(apkPath, true)
			val entriesVec = Vector<JarEntry>()
			val buffer = ByteArray(8192)

			val entries = jf.entries()
			while (entries.hasMoreElements()) {
				val je = entries.nextElement()
				entriesVec.addElement(je)
				var `is`: InputStream? = null
				try {
					`is` = jf.getInputStream(je)
					var n: Int = `is`!!.read(buffer, 0, buffer.size)
					while (n != -1) {
						// we just read. this will throw a SecurityException
						// if  a signature/digest check fails.
						n = `is`.read(buffer, 0, buffer.size)
					}
				} finally {
					if (`is` != null) {
						`is`.close()
					}
				}
			}

			val man = jf.manifest
			if (man != null) {
				val e = entriesVec.elements()
				if (e.hasMoreElements()) {
					val je = e.nextElement()
					certificates.addAll(Arrays.asList(*je.certificates))
				}
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
		}

		return certificates
	}


	private fun createSignatureSerializableObjects(certs: Set<Certificate>) {
		for (cert in certs) {
			try {
				val sso = SignatureSerializableObject(cert.encoded)
				certificates.add(sso)
			} catch (ex: Exception) {
				LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
				ex.printStackTrace()
				System.exit(0)
			}

		}
	}

}
