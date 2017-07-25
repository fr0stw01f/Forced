package me.zhenhao.forced.progressmetric

import soot.Unit
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory


interface IProgressMetric {

	fun update(history: ClientHistory): Int

	fun getMetricName(): String

	fun getMetricIdentifier(): String

	fun initialize()

	fun setCurrentTargetLocation(currentTargetLocation: Unit)

}
