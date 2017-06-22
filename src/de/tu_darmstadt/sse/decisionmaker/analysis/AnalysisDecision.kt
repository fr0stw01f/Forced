package de.tu_darmstadt.sse.decisionmaker.analysis

import java.util.ArrayList

import de.tu_darmstadt.sse.frameworkevents.FrameworkEvent
import de.tu_darmstadt.sse.sharedclasses.networkconnection.ServerResponse


class AnalysisDecision : Comparable<AnalysisDecision>, Cloneable {

    var decisionWeight = 0
    var serverResponse: ServerResponse? = null
    private val eventTaskList = ArrayList<FrameworkEvent>()
    lateinit var analysisName: String
    var isDecisionUsed = false
        private set

    constructor()

    constructor(original: AnalysisDecision) {
        this.decisionWeight = original.decisionWeight
        this.serverResponse = original.serverResponse!!.clone()
        this.eventTaskList.addAll(original.eventTaskList)
        this.analysisName = original.analysisName

        // We do not copy over the decisionUsed flag. This must always be set
        // explicitly.
    }

    fun getEventTaskList(): List<FrameworkEvent> {
        return eventTaskList
    }

    fun addNewEvent(event: FrameworkEvent) {
        eventTaskList.add(event)
    }


    fun setDecisionUsed() {
        this.isDecisionUsed = true
    }

    override fun compareTo(other: AnalysisDecision): Int {
        return -Integer.compare(this.decisionWeight, other.decisionWeight)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (analysisName == null) 0 else analysisName!!.hashCode()
        result = prime * result + if (isDecisionUsed) 1231 else 1237
        result = prime * result + decisionWeight
        result = prime * result + eventTaskList.hashCode()
        result = prime * result + if (serverResponse == null) 0 else serverResponse!!.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as AnalysisDecision?
        if (analysisName == null) {
            if (other!!.analysisName != null)
                return false
        } else if (analysisName != other!!.analysisName)
            return false
        if (isDecisionUsed != other.isDecisionUsed)
            return false
        if (decisionWeight != other.decisionWeight)
            return false
        if (eventTaskList != other.eventTaskList)
            return false
        if (serverResponse == null) {
            if (other.serverResponse != null)
                return false
        } else if (serverResponse != other.serverResponse)
            return false
        return true
    }

    public override fun clone(): AnalysisDecision {
        return AnalysisDecision(this)
    }

    override fun toString(): String {
        return "[$analysisName] $serverResponse"
    }

}
