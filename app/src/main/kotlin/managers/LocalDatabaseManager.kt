import javax.inject.Inject

import java.sql.Connection
import java.sql.SQLException

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalDatabaseManager @Inject constructor(
    private val connection: Connection
) {

    private val gson: Gson = Gson()

    init {
        connection.createStatement().executeUpdate(CREATE_TABLE_QUERY)
        connection.createStatement().executeUpdate(CREATE_CONFIG_TABLE)
        insertRootIfDoesNotExist()
        insertConfigIfDoesNotExist()
    }

    private fun insertRootIfDoesNotExist() {
        connection.prepareStatement(INSERT_ROOT_QUERY).use { stmt ->
            stmt.setString(1, "root")
            stmt.setString(2, gson.toJson(
                mapOf(
                    "total_storage_used" to 0L,
                    "children" to emptyList<String>()
                ))
            )
            stmt.executeUpdate()
        }
    }

    private fun insertConfigIfDoesNotExist() {
        connection.prepareStatement(INSERT_CONFIG_QUERY).use { stmt ->
            stmt.executeUpdate()
        }
    }
    
    private fun updateStorage(storage: Long) {
        connection.prepareStatement(UPDATE_STORAGE_QUERY).use { stmt ->
            stmt.setLong(1, storage)
            stmt.executeUpdate()
        }
    }
    
    private fun addChildIdToParent(parentId: String, childId: String): Boolean {
        connection.prepareStatement(ADD_CHILD_TO_PARENT_QUERY).use { stmt ->
            stmt.setString(1, childId)
            stmt.setString(2, parentId)
            return stmt.executeUpdate() > 0
        }
    }

    private fun removeChildIdFromParent(parentId: String, childId: String) {
        connection.prepareStatement(REMOVE_CHILD_FROM_PARENT_QUERY).use { stmt ->
            stmt.setString(1, childId)
            stmt.setString(2, parentId)
            stmt.executeUpdate()
        }
    }

    private fun getDescendantsWithStorage(directoryId: String, memo: MutableMap<String, Map<String, Any>> = mutableMapOf()): Map<String, Any> {
        memo[directoryId]?.let { return it }
        val node: Map<String, Any> = getNode(directoryId) ?: return mapOf("storage" to 0L, "ids" to emptyList<String>())
        val children: List<String> = node.getTypedValue("children") ?: emptyList()
        val descendants: MutableList<String> = mutableListOf()
        var totalStorage: Long = 0L
        for (childId: String in children) {
            descendants.add(childId)
            val childNode: Map<String, Any> = getNode(childId) ?: continue
            if (childNode["type"] == "DIRECTORY") {
                val result: Map<String, Any> = getDescendantsWithStorage(childId, memo)
                descendants.addAll(result.getTypedValue("ids")!!)
                totalStorage += result["storage"] as Long
            } else {
                totalStorage += (childNode["size"] as Number).toLong()
            }
        }
        val result: Map<String, Any> = mapOf("storage" to totalStorage, "ids" to descendants)
        memo[directoryId] = result
        return result
    }

    fun getFromConfig(key: String): String {
        connection.prepareStatement(GET_FROM_CONFIG_QUERY).use { stmt ->
            stmt.setString(1, key)
            val resultSet = stmt.executeQuery()
            if (resultSet.next()) {
                return resultSet.getString("data")
            } else {
                throw NullPointerException("No value found for key: $key")
            }
        }
    }

    fun updateConfig(token: String? = null, channelId: String? = null) {
        if (token == null && channelId == null) {
            return
        }
        connection.prepareStatement(UPDATE_CONFIG_QUERY).use { stmt ->
            stmt.setString(1, token)
            stmt.setString(2, channelId)
            stmt.executeUpdate()
        }
    }

    fun deleteDirectory(directoryId: String) {
        connection.autoCommit = false
        try {
            val directoryNode: Map<String, Any> = getNode(directoryId) ?: return
            val parentId: String = directoryNode["parent_id"] as String
            val descendantsWithStorage: Map<String, Any> = getDescendantsWithStorage(directoryId)
            val idsToDelete: List<String> = listOf(directoryId) + descendantsWithStorage.getTypedValue<List<String>>("ids")!!
            connection.prepareStatement(DELETE_NODE_QUERY).use { stmt ->
                for (id: String in idsToDelete) {
                    stmt.setString(1, id)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
            removeChildIdFromParent(parentId, directoryId)
            updateStorage(-(descendantsWithStorage["storage"] as Long))
            connection.commit()
        } catch (e: SQLException) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    fun deleteFile(fileId: String) {
        connection.autoCommit = false
        try {
            val fileNode: Map<String, Any> = getNode(fileId) ?: return
            connection.prepareStatement(DELETE_NODE_QUERY).use { stmt ->
                stmt.setString(1, fileId)
                stmt.executeUpdate()
            }
            removeChildIdFromParent(fileNode["parent_id"] as String, fileId)
            updateStorage(-((fileNode["size"] as Number).toLong()))
            connection.commit()
        } catch (e: SQLException) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }   
    }

    fun insertMetadata(metadata: MutableMap<String, MutableMap<String, Any>>, parentId: String) {
        connection.autoCommit = false
        var size: Long = 0L
        try {
            val childId: String = metadata.keys.first()
            connection.prepareStatement(INSERT_METADATA_QUERY).use { stmt ->
                for ((id, node) in metadata) {
                    stmt.setString(1, id)
                    stmt.setString(2, gson.toJson(node))
                    stmt.addBatch()
                    
                    if (node["type"] == "FILE") {
                        size += node["size"] as Long
                    }
                }
                stmt.executeBatch() 
            }
            addChildIdToParent(parentId, childId)
            updateStorage(size)
            connection.commit()
        } catch (e: SQLException) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    fun getNode(id: String): Map<String, Any>? {
        connection.prepareStatement(GET_NODE_QUERY).use { stmt -> 
            stmt.setString(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val jsonNode: String = rs.getString("data")
                val type = object : TypeToken<Map<String, Any>>() {}.type
                return gson.fromJson(jsonNode, type)
            }
        }
        return null
    }

    fun getChildNodes(parentId: String): List<Map<String, Any>> {
        val childNodes: MutableList<Map<String, Any>> = mutableListOf()
        connection.prepareStatement(GET_CHILD_NODES_QUERY).use { stmt ->
            stmt.setString(1, parentId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val node: Map<String, Any> = gson.fromJson(
                        rs.getString("data"),
                        object : TypeToken<Map<String, Any>>() {}.type
                    )
                    childNodes.add(node)
                }
            }
        }
        return childNodes
    }
}