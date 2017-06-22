package de.tu_darmstadt.sse.decisionmaker.server.history


interface ClientHistoryCreatedHandler {

    fun onClientHistoryCreated(history: ClientHistory)

}
