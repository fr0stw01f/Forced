package de.tu_darmstadt.sse.dynamiccfg

import java.util.Collections
import java.util.HashMap
import java.util.HashSet

import soot.Unit


class Callgraph : Cloneable {

    private val edges = HashSet<Edge>()
    private val edgesOut = HashMap<Unit, MutableSet<Edge>>()

    constructor()

    private constructor(original: Callgraph) {
        this.edges.addAll(original.edges)
        this.edgesOut.putAll(original.edgesOut)
    }


    fun addEdge(edge: Edge): Boolean {
        if (!this.edges.add(edge))
            return false

        // Add the lookup values
        var edgeSet: MutableSet<Edge>? = edgesOut[edge.callSite]
        if (edgeSet == null) {
            edgeSet = HashSet()
            edgesOut.put(edge.callSite!!, edgeSet)
        }
        return edgeSet.add(edge)
    }

    fun getEdges(): Set<Edge> {
        return Collections.unmodifiableSet(this.edges)
    }

    fun getEdgesOutOf(u: Unit): Set<Edge> {
        return edgesOut[u]!!
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + edges.hashCode()
        result = prime * result + edgesOut.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as Callgraph?

        if (edges != other!!.edges)
            return false
        if (edgesOut != other.edgesOut)
            return false
        return true
    }

    public override fun clone(): Callgraph {
        return Callgraph(this)
    }

}
