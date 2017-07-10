package me.zhenhao.forced.decisionmaker.server

import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class TraceManager {

    private val threadToManager = ConcurrentHashMap<Long, ThreadTraceManager>()
    private val onCreateHandler = HashSet<ThreadTraceManagerCreatedHandler>()


    fun getAllThreadTraceManagers(): Collection<ThreadTraceManager> {
        return threadToManager.values
    }


    fun getThreadTraceManager(threadId: Long): ThreadTraceManager {
        return threadToManager[threadId]!!
    }


    fun getOrCreateThreadTraceManager(threadId: Long): ThreadTraceManager {
        val newManager = ThreadTraceManager(threadId)
        val existingManager = (threadToManager as java.util.Map<Long, ThreadTraceManager>).putIfAbsent(threadId, newManager)
        if (existingManager == null) {
            for (handler in onCreateHandler)
                handler.onThreadTraceManagerCreated(newManager)
            return newManager
        } else
            return existingManager
    }


    fun addThreadTraceCreateHandler(handler: ThreadTraceManagerCreatedHandler) {
        this.onCreateHandler.add(handler)
    }

}//
