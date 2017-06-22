package de.tu_darmstadt.sse.decisionmaker.analysis.integritycheck

import de.tu_darmstadt.sse.FrameworkOptions
import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel
import de.tu_darmstadt.sse.decisionmaker.analysis.AnalysisDecision
import de.tu_darmstadt.sse.decisionmaker.analysis.FuzzyAnalysis
import de.tu_darmstadt.sse.decisionmaker.server.ThreadTraceManager
import de.tu_darmstadt.sse.decisionmaker.server.TraceManager
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest
import de.tu_darmstadt.sse.sharedclasses.networkconnection.ServerResponse
import de.tu_darmstadt.sse.sharedclasses.networkconnection.serializables.SignatureSerializableObject
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
                                completeHistory: ThreadTraceManager): List<AnalysisDecision> {
        val decisions = ArrayList<AnalysisDecision>()
        if (clientRequest.loggingPointSignature == "<de.tu_darmstadt.sse.additionalappclasses.wrapper.DummyWrapper: android.content.pm.PackageInfo dummyWrapper_getPackageInfo(android.content.pm.PackageManager,java.lang.String,int)>") {
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
