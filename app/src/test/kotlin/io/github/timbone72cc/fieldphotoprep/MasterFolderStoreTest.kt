package io.github.timbone72cc.fieldphotoprep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MasterFolderStoreTest {
    @Test
    fun savesAndLoadsExactDriveIdentity() {
        val values = FakeKeyValueStore()
        val store = MasterFolderStore(values)

        store.save(MasterFolder(id = "drive-folder-123", name = "HNP"))

        assertEquals(
            MasterFolder(id = "drive-folder-123", name = "HNP"),
            store.load(),
        )
    }

    @Test
    fun sameDriveIdCanBeSavedWithRenamedDisplayName() {
        val values = FakeKeyValueStore()
        val store = MasterFolderStore(values)

        store.save(MasterFolder(id = "stable-id", name = "Old Name"))
        store.save(MasterFolder(id = "stable-id", name = "New Name"))

        assertEquals(MasterFolder(id = "stable-id", name = "New Name"), store.load())
    }

    @Test
    fun incompleteStoredIdentityIsRejected() {
        val values = FakeKeyValueStore(
            mutableMapOf("master_folder_id" to "drive-folder-123"),
        )
        val store = MasterFolderStore(values)

        assertNull(store.load())
    }

    @Test
    fun clearRemovesStoredIdentity() {
        val values = FakeKeyValueStore()
        val store = MasterFolderStore(values)
        store.save(MasterFolder(id = "drive-folder-123", name = "HNP"))

        store.clear()

        assertNull(store.load())
    }

    private class FakeKeyValueStore(
        private val values: MutableMap<String, String> = mutableMapOf(),
    ) : KeyValueStore {
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) {
            values[key] = value
        }
        override fun remove(key: String) {
            values.remove(key)
        }
    }
}
