package me.zhenhao.forced.appinstrumentation.transformer

import soot.tagkit.Tag


object InstrumentedCodeTag : Tag {

    override fun getName(): String {
        return "InstrumentedCodeTag"
    }

    override fun getValue(): ByteArray? {
        return null
    }

}
