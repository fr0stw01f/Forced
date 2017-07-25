package me.zhenhao.forced.dynamiccfg

import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.apkspecific.CodeModel.StaticCodeIndexer
import me.zhenhao.forced.shared.dynamiccfg.AbstractDynamicCFGItem
import me.zhenhao.forced.shared.dynamiccfg.MethodCallItem
import me.zhenhao.forced.shared.dynamiccfg.MethodEnterItem
import me.zhenhao.forced.shared.dynamiccfg.MethodReturnItem
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
