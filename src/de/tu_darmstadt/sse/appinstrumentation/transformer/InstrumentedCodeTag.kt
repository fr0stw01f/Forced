package de.tu_darmstadt.sse.appinstrumentation.transformer

import soot.tagkit.AttributeValueException
import soot.tagkit.Tag


object InstrumentedCodeTag : Tag {
    override fun getName(): String {
        return "InstrumentedCodeTag"
    }

    @Throws(AttributeValueException::class)
    override fun getValue(): ByteArray? {
        return null
    }

}
