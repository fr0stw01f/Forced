package me.zhenhao.forced.bootstrap

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class DexFileManager {

    private val dexFiles = ConcurrentHashMap<DexFile, DexFile>()

    fun add(dexFile: DexFile): DexFile {
        val ret = (dexFiles as java.util.Map<DexFile, DexFile>).putIfAbsent(dexFile, dexFile)
        return ret ?: dexFile
    }

}
