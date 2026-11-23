package com.meneses.budgethunter.sms.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Stands for the preferences file: what is written stays for every reader, like on disk. */
class InMemoryPreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    /** Makes every write fail, like a full disk. */
    var failWrites = false

    /** Makes every read fail, like a corrupt file. */
    var failReads = false

    override val data: Flow<Preferences> = flow {
        if (failReads) throw java.io.IOException("Unreadable preferences")
        emitAll(state)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock {
            if (failWrites) throw java.io.IOException("No space left on device")
            transform(state.value).also { state.value = it }
        }
}
