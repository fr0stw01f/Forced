package me.zhenhao.forced.apkspecific.CodeModel

import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter


class CodePositionWriter(private val codePositionManager: CodePositionManager) {

	@Throws(FileNotFoundException::class)
	fun writeCodePositions(fileName: String) {
		var writer: PrintWriter? = null
		try {
			writer = PrintWriter(File(fileName))
			for (signature in codePositionManager.methodsWithCodePositions) {
				writer.write("METHOD " + signature + "\n")
				writer.write("OFFSET " + codePositionManager.getMethodOffset(signature) + "\n")
				writer.write("\n")
			}
		} finally {
			if (writer != null)
				writer.close()
		}
	}

}
