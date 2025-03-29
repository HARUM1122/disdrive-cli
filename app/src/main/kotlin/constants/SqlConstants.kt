const val DB_URL: String = "jdbc:sqlite:resources\\Metadata.db"

const val CREATE_TABLE_QUERY: String = "CREATE TABLE IF NOT EXISTS metadata (id TEXT PRIMARY KEY, data TEXT)"


const val CREATE_CONFIG_TABLE: String = "CREATE TABLE IF NOT EXISTS config (id TEXT PRIMARY KEY, data TEXT)"


const val INSERT_ROOT_QUERY: String = "INSERT OR IGNORE INTO metadata (id, data) VALUES (?, ?)"


const val INSERT_CONFIG_QUERY: String = "INSERT OR IGNORE INTO config (id, data) VALUES ('token', ''), ('channel_id', '')"


const val UPDATE_CONFIG_QUERY: String = "UPDATE config SET data = CASE WHEN id = 'token' THEN COALESCE(?, data) WHEN id = 'channel_id' THEN COALESCE(?, data) ELSE data END WHERE id IN ('token', 'channel_id')"


const val GET_FROM_CONFIG_QUERY: String = "SELECT data FROM config WHERE id = ?"


const val UPDATE_STORAGE_QUERY: String = "UPDATE metadata SET data = json_set(data, '$.total_storage_used', json_extract(data, '$.total_storage_used') + ?) WHERE id = 'root'"


const val ADD_CHILD_TO_PARENT_QUERY: String = "UPDATE metadata SET data = json_set(data, '$.children', json_insert(json_extract(data, '$.children'), '$[' || json_array_length(json_extract(data, '$.children')) || ']', ?)) WHERE id = ?;"


const val REMOVE_CHILD_FROM_PARENT_QUERY: String = "UPDATE metadata SET data = json_set(data, '$.children', json_insert(json_extract(data, '$.children'), '$[' || json_array_length(json_extract(data, '$.children')) || ']', ?)) WHERE id = ?;"


const val INSERT_METADATA_QUERY: String = "INSERT OR REPLACE INTO metadata (id, data) VALUES (?, ?)"


const val GET_NODE_QUERY: String = "SELECT data FROM metadata WHERE id = ?"


const val GET_CHILD_NODES_QUERY: String = "SELECT child.id as id, child.data as data FROM metadata AS parent, json_each(parent.data, '$.children') AS childId, metadata AS child WHERE parent.id = ? AND child.id = TRIM(childId.value) ORDER BY CASE json_extract(child.data, '$.type') WHEN 'DIRECTORY' THEN 0 ELSE 1 END;"


const val DELETE_NODE_QUERY: String = "DELETE FROM metadata WHERE id = ?"