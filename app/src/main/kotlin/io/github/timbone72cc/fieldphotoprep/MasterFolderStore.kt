package io.github.timbone72cc.fieldphotoprep

import android.content.Context
import android.content.SharedPreferences

interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

class SharedPreferencesKeyValueStore(
    private val preferences: SharedPreferences,
) : KeyValueStore {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

class MasterFolderStore(
    private val values: KeyValueStore,
) {
    fun load(): MasterFolder? {
        val id = values.getString(KEY_ID)?.takeIf { it.isNotBlank() } ?: return null
        val name = values.getString(KEY_NAME)?.takeIf { it.isNotBlank() } ?: return null
        return MasterFolder(id = id, name = name)
    }

    fun save(folder: MasterFolder) {
        require(folder.id.isNotBlank()) { "Master folder ID must not be blank." }
        require(folder.name.isNotBlank()) { "Master folder name must not be blank." }
        values.putString(KEY_ID, folder.id)
        values.putString(KEY_NAME, folder.name)
    }

    fun clear() {
        values.remove(KEY_ID)
        values.remove(KEY_NAME)
    }

    companion object {
        private const val KEY_ID = "master_folder_id"
        private const val KEY_NAME = "master_folder_name"

        fun from(context: Context): MasterFolderStore {
            val preferences = context.getSharedPreferences(
                "field_photo_prep",
                Context.MODE_PRIVATE,
            )
            return MasterFolderStore(SharedPreferencesKeyValueStore(preferences))
        }
    }
}
