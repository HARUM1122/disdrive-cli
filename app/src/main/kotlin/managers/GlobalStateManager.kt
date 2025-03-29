object GlobalStateManager {
    private val nodesState: MutableList<Map<String, Any>> = mutableListOf()

    fun addNode(node: Map<String, Any>) {
        nodesState.add(node)
    }

    fun getNode(index: Int): Map<String, Any>? {
        if (index < nodesState.size) {
            return nodesState[index]
        }
        return null
    }

    fun clearState() {
        nodesState.clear()
    }
}