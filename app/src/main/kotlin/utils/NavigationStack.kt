object NavigationStack {
    private val locationStack: MutableList<Map<String, String>> = mutableListOf(
        mapOf(
            "name" to "root",
            "id" to "root"
        )
    )

    fun navigateTo(name: String, id: String) {
        locationStack.add(
            mapOf(
                "name" to name,
                "id" to id
            )
        )
    }

    fun navigateBack(): Boolean {
        if (locationStack.size > 1) {
            locationStack.removeAt(locationStack.size - 1)
            return true
        }
        return false
    }

    fun getCurrentLocation(): Map<String, String> = locationStack.last()

    fun getFullPath(): String = locationStack.joinToString("/") { it["name"]!! }
}