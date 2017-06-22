package de.tu_darmstadt.sse.progressmetric

import soot.Unit
import de.tu_darmstadt.sse.decisionmaker.server.history.ClientHistory


interface IProgressMetric {


    fun update(history: ClientHistory): Int


    fun getMetricName(): String


    fun getMetricIdentifier(): String


    fun initialize()


    fun setCurrentTargetLocation(currentTargetLocation: Unit)

}
