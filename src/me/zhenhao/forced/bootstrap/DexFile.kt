package me.zhenhao.forced.bootstrap

import java.util.Arrays


class DexFile(val fileName: String?, val localFileName: String, val fileContents: ByteArray) {

	override fun hashCode(): Int {
		val prime = 31
		var result = 1
		result = prime * result + Arrays.hashCode(fileContents)
		result = prime * result + (fileName?.hashCode() ?: 0)
		// We deliberately ignore the local file name. This helps ensure that we
		// don't get duplicates of the same dex file.
		return result
	}

	override fun equals(obj: Any?): Boolean {
		if (this === obj)
			return true
		if (obj == null)
			return false
		if (javaClass != obj.javaClass)
			return false
		val other = obj as DexFile?
		if (!Arrays.equals(fileContents, other!!.fileContents))
			return false
		if (fileName == null) {
			if (other.fileName != null)
				return false
		} else if (fileName != other.fileName)
			return false
		// We deliberately ignore the local file name. This helps ensure that we
		// don't get duplicates of the same dex file.
		return true
	}

}
