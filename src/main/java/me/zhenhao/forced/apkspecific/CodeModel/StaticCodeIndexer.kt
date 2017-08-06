package me.zhenhao.forced.apkspecific.CodeModel

import java.util.HashMap

import me.zhenhao.forced.appinstrumentation.InstrumentUtil
import me.zhenhao.forced.appinstrumentation.transformer.InstrumentedCodeTag
import soot.Scene
import soot.SootMethod
import soot.Unit


class StaticCodeIndexer {

    private val unitToMethod = HashMap<Unit, SootMethod>()

    init {
        initializeUnitToMethod()
    }

    private fun initializeUnitToMethod() {
        Scene.v().applicationClasses
                .filter { InstrumentUtil.isAppDeveloperCode(it) && it.isConcrete }
                .forEach { it.methods
                            .filter { it.isConcrete }
                            .forEach { sm ->
                                sm.retrieveActiveBody().units
                                        .filterNot { it.hasTag(InstrumentedCodeTag.name) }
                                        .forEach { unitToMethod.put(it, sm) }
                            }
                }
    }


    fun getMethodOf(u: Unit): SootMethod {
        return unitToMethod[u]!!
    }

}
