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

            for ((unit, codePos) in codePositionManager.unitToCodePosition) {
                writer.write("[UNIT] " + unit + "\n")
                writer.write(" [POS]  " + codePos + "\n")
                writer.write("\n")
            }
        } finally {
            if (writer != null)
                writer.close()
        }
    }

}
