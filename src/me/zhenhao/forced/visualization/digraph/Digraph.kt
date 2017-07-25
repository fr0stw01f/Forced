package me.zhenhao.forced.visualization.digraph

import java.util.ArrayList
import java.util.HashMap

class Digraph {
	private val subgraphs: MutableMap<Subgraph, MutableList<Subgraph>> = HashMap()
	private val connections: MutableMap<String, MutableList<String>> = HashMap()
	private val methodSignatureToShortName: MutableMap<String, String> = HashMap()

	private val METHOD_COLOR = "blue"
	private val COMPONENT_COLOR = "black"

	fun addComponentSubgraph(clusterName: String, label: String) {
		val clusterPseudoName = String.format("cluster_%s", clusterName)
		val subgraph = Subgraph(clusterPseudoName, label, COMPONENT_COLOR, ArrayList<String>())
		if (!subgraphs.containsKey(subgraph)) {
			this.subgraphs.put(subgraph, ArrayList<Subgraph>())
		}
	}

	fun getInitialItemOfMethod(componentLabel: String, methodLabel: String): String {
		val subgraphForMethod = this.getSubgraphForMethod(componentLabel, methodLabel)
		if (subgraphForMethod == null || subgraphForMethod.items.isEmpty()) {
			throw RuntimeException("There should be at least one item in the items list")
		}
		return subgraphForMethod.items[0]
	}

	fun getCallerMethodItem(componentLabel: String, methodLabel: String, calleeMethodSignature: String): String? {
		val subgraphForMethod = this.getSubgraphForMethod(componentLabel, methodLabel)
		if (subgraphForMethod == null || subgraphForMethod.items.isEmpty()) {
			throw RuntimeException("There should be at least one item in the items list")
		}

		for (i in subgraphForMethod.items.size - 1 downTo 0) {
			val item = subgraphForMethod.items[i]
			val shortName = this.generateShortMethodName(calleeMethodSignature)
			if (item.endsWith(String.format("caller_%s", shortName))) {
				return item
			}
		}
		return null
	}


	fun addMethodSubgraph(clusterName: String, methodSignature: String, componentLabel: String): String {
		val clusterPseudoName = String.format("cluster_%s", this.generateShortMethodName(clusterName))
		val methodPseudoName = this.generateShortMethodName(methodSignature)
		val initialItem = String.format("%s__init", methodPseudoName)
		var subgraphForMethod = this.getSubgraphForMethod(componentLabel, methodSignature)
		if (subgraphForMethod == null) {
			val itemsList = ArrayList<String>()
			itemsList.add(initialItem)
			subgraphForMethod = Subgraph(clusterPseudoName, methodPseudoName, METHOD_COLOR, itemsList)
			val subgraphForComponent = this.getSubgraphForComponent(componentLabel)
			subgraphs[subgraphForComponent]!!.add(subgraphForMethod)
		}
		return initialItem
	}

	fun addCallerItemToMethodSubgraph(componentLabel: String, methodLabel: String, callerMethodSignature: String): String {
		val subgraphForMethod = this.getSubgraphForMethod(componentLabel, methodLabel)
		val callerShortName = this.generateShortMethodName(callerMethodSignature)
		val callerItem = String.format("%s__caller_%s", subgraphForMethod!!.label, callerShortName)
		subgraphForMethod.addItem(callerItem)
		return callerItem
	}

	fun addAPIItemToMethodSubgraph(componentLabel: String, methodLabel: String, returnValue: String?, apiCallSignatureWithValues: String): String {
		val subgraphForMethod = this.getSubgraphForMethod(componentLabel, methodLabel)
		val apiItem: String?
		if (returnValue == null) {
			apiItem = apiCallSignatureWithValues
		} else {
			apiItem = String.format("%s <- %s", returnValue, apiCallSignatureWithValues)
		}
		subgraphForMethod!!.addItem(apiItem)
		return apiItem
	}

	fun addReturnItemToMethodSubgraph(componentLabel: String, methodLabel: String, returnValue: String): String {
		val subgraphForMethod = this.getSubgraphForMethod(componentLabel, methodLabel)
		val itemContent = String.format("%s__return %s", subgraphForMethod!!.label, returnValue)
		subgraphForMethod.addItem(itemContent)
		return itemContent
	}

	fun addConnection(from: String?, to: String) {
		if (this.connections.containsKey(from)) {
			if (!this.connections[from]!!.contains(to)) {
				this.connections[from]!!.add(to)
			}
		} else {
			val toList = ArrayList<String>()
			toList.add(to)
			if (from == null) {
				throw RuntimeException("This should not happen!")
			}
			this.connections.put(from, toList)
		}
	}

	private fun getSubgraphForComponent(label: String): Subgraph? {
		return subgraphs.keys.firstOrNull { it.label == label }
	}


	fun getSubgraphForMethod(componentLabel: String, methodLabel: String): Subgraph? {
		val methodPseudoName = this.generateShortMethodName(methodLabel)
		val subgraphForComponent = this.getSubgraphForComponent(componentLabel) ?: throw RuntimeException("There should be already a component subgraph")
		return subgraphs[subgraphForComponent]!!.firstOrNull { it.label == methodPseudoName }
	}

	private fun generateShortMethodName(methodSignature: String): String {
		if (this.methodSignatureToShortName.containsKey(methodSignature)) {
			return this.methodSignatureToShortName[methodSignature]!!
		} else {
			val pseudoName = String.format("method%s", this.methodSignatureToShortName.keys.size)
			this.methodSignatureToShortName.put(methodSignature, pseudoName)
			return pseudoName
		}
	}

	override fun toString(): String {
		val tmp = ArrayList<String>()
		val sb = StringBuilder()
		sb.append("digraph d{\n\n")

		for ((key, value) in subgraphs) {
			sb.append(String.format("subgraph %s {\n \tlabel = \"%s\"; \n\t color = %s;\n\n",
					key.clusterName,
					key.label,
					key.color))

			for (subgraph in value) {
				sb.append(String.format("%s\n\n", subgraph.toString()))
			}

			sb.append("}")
		}

		sb.append("\n\nsubgraph cluster_legend {\n\t")
		for ((key, value) in methodSignatureToShortName) {
			val line = String.format("\"%s -> %s\"\n", key, value)
			sb.append(line)
			tmp.add(line)
		}
		sb.append(";} \n\n")

		//connections
		for ((key, value) in connections) {
			for (to in value) {
				sb.append(String.format("\t\"%s\" -> \"%s\";\n", key, to))
			}
		}

		for (i in 0..tmp.size - 1 - 1) {
			sb.append(String.format("\t%s -> %s [style=invis];\n", tmp[i].trim { it <= ' ' }, tmp[i + 1].trim { it <= ' ' }))
		}

		sb.append("}")

		return sb.toString()
	}
}
