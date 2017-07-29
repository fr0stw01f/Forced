package me.zhenhao.forced.decisionmaker.analysis

import java.util.ArrayList

import me.zhenhao.forced.frameworkevents.FrameworkEvent
import me.zhenhao.forced.shared.networkconnection.ServerResponse


class AnalysisDecision : Comparable<AnalysisDecision>, Cloneable {

    val eventTaskList = ArrayList<FrameworkEvent>()
    var serverResponse: ServerResponse
    var analysisName: String
    var decisionWeight = 0
    var isDecisionUsed = false

    constructor() {
        serverResponse = ServerResponse.getEmptyResponse()
        analysisName = "EMPTY_ANALYSIS_NAME"
    }

    private constructor(original: AnalysisDecision) {
        this.decisionWeight = original.decisionWeight
        this.serverResponse = original.serverResponse.clone()
        this.eventTaskList.addAll(original.eventTaskList)
        this.analysisName = original.analysisName

        // We do not copy over the decisionUsed flag. This must always be set
        // explicitly.
    }

    fun addNewEvent(event: FrameworkEvent) {
        eventTaskList.add(event)
    }

    override fun compareTo(other: AnalysisDecision): Int {
        return -Integer.compare(this.decisionWeight, other.decisionWeight)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + analysisName.hashCode()
        result = prime * result + if (isDecisionUsed) 1231 else 1237
        result = prime * result + decisionWeight
        result = prime * result + eventTaskList.hashCode()
        result = prime * result + serverResponse.hashCode()
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
        if (analysisName != other!!.analysisName)
            return false
        if (isDecisionUsed != other.isDecisionUsed)
            return false
        if (decisionWeight != other.decisionWeight)
            return false
        if (eventTaskList != other.eventTaskList)
            return false
        if (serverResponse != other.serverResponse)
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
