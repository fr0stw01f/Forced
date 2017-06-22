package de.tu_darmstadt.sse.bootstrap

import java.util.ArrayList
import java.util.HashSet


class AnalysisTaskManager {

    private val openTasks = ArrayList<AnalysisTask>()
    private val finishedTasks = HashSet<AnalysisTask>()
    var currentTask: AnalysisTask? = null
        private set

    fun scheduleNextTask(): AnalysisTask? {
        if (openTasks.isEmpty())
            currentTask = null
        else
            currentTask = openTasks.removeAt(0)
        return currentTask
    }

    fun enqueueAnalysisTask(task: AnalysisTask): Boolean {
        if (finishedTasks.contains(task))
            return false

        openTasks.add(task)
        finishedTasks.add(task)
        return true
    }

}
