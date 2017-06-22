package de.tu_darmstadt.sse.frameworkevents

import com.android.ddmlib.MultiLineReceiver


class GenericReceiver : MultiLineReceiver() {

    override fun processNewLines(lines: Array<String>) {
        for (line in lines) {
            System.out.format("[ADB] %s\n", line)
        }
    }

    override fun isCancelled(): Boolean {
        return false
    }
}
