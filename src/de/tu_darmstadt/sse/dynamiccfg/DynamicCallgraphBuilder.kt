package de.tu_darmstadt.sse.dynamiccfg

import de.tu_darmstadt.sse.apkspecific.CodeModel.CodePositionManager
import de.tu_darmstadt.sse.apkspecific.CodeModel.StaticCodeIndexer
import de.tu_darmstadt.sse.sharedclasses.dynamiccfg.AbstractDynamicCFGItem
import de.tu_darmstadt.sse.sharedclasses.dynamiccfg.MethodCallItem
import de.tu_darmstadt.sse.sharedclasses.dynamiccfg.MethodEnterItem
import de.tu_darmstadt.sse.sharedclasses.dynamiccfg.MethodReturnItem
import soot.Unit
import java.util.concurrent.LinkedBlockingQueue


class DynamicCallgraphBuilder(private val callgraph: Callgraph,
                              private val codePositionManager: CodePositionManager,
                              private val codeIndexer: StaticCodeIndexer) {

    private val itemQueue = LinkedBlockingQueue<AbstractDynamicCFGItem>()

    fun enqueueItem(item: AbstractDynamicCFGItem) {
        this.itemQueue.add(item)
    }

    fun updateCFG() {
        var lastCallSite: Unit? = null
        while (itemQueue.size >= 2) {
            val item = itemQueue.poll() ?: break

            if (item is MethodCallItem) {
                lastCallSite = codePositionManager.getUnitForCodePosition(
                        item.lastExecutedStatement)
            } else if (item is MethodEnterItem && lastCallSite != null) {
                val newMethodUnit = codePositionManager.getUnitForCodePosition(
                        item.lastExecutedStatement)
                val callee = codeIndexer.getMethodOf(newMethodUnit!!)

                // Create the callgraph edge
                callgraph.addEdge(Edge(lastCallSite, callee))
                lastCallSite = null
            } else if (item is MethodReturnItem) {
                // This ends the current call
                lastCallSite = null
            }
        }
    }

}
