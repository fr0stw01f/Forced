package me.zhenhao.forced.bootstrap

import java.util.HashSet

class AnalysisTask : Cloneable {

    val dexFilesToMerge = HashSet<DexFile>()
        get
    val statementsToRemove = HashSet<InstanceIndependentCodePosition>()
        get

    @JvmOverloads fun deriveNewTask(fileToMerge: DexFile,
                                    toRemove: Set<InstanceIndependentCodePosition>? = null): AnalysisTask {
        val newTask = clone()
        newTask.dexFilesToMerge.add(fileToMerge)
        if (toRemove != null)
            newTask.statementsToRemove.addAll(toRemove)
        return newTask
    }

    public override fun clone(): AnalysisTask {
        val clone = AnalysisTask()
        clone.dexFilesToMerge.addAll(dexFilesToMerge)
        return clone
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + dexFilesToMerge.hashCode()
        result = prime * result + statementsToRemove.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false

        val other = obj as AnalysisTask

        if (dexFilesToMerge != other.dexFilesToMerge)
            return false
        if (statementsToRemove != other.statementsToRemove)
            return false
        return true
    }

}
