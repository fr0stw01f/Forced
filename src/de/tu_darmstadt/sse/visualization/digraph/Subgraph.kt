package de.tu_darmstadt.sse.visualization.digraph

class Subgraph(val clusterName: String, val label: String, val color: String, val items: MutableList<String>) {

    fun addItem(item: String) {
        if (!this.items.contains(item)) {
            this.items.add(item)
        }
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is Subgraph)
            return false
        if (obj === this)
            return true

        return this.label == obj.label
    }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append(String.format("\tsubgraph %s { \n\t\t", this.clusterName))
        for (item in this.items) {
            sb.append(String.format("\"%s\" ", item))
        }
        sb.append(String.format(";\n\tlabel = \"%s\";\n\tcolor = %s;",
                this.label, this.color))

        sb.append("}")

        return sb.toString()
    }
}
