package com.meneses.budgethunter.commons.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meneses.budgethunter.auth.domain.GoogleLinkNotice
import com.meneses.budgethunter.sms.data.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GoogleLinkNoticePreferenceTest {

    private val dataStore = InMemoryPreferencesDataStore()
    private val preferences = PreferencesManager(dataStore)

    private val notice = GoogleLinkNotice(
        accountEmail = "google@example.com",
        sharedBudgets = listOf(
            GoogleLinkNotice.SharedBudget("Casa", "other@example.com", listOf("other@example.com", "third@example.com"))
        )
    )

    @Test
    fun `a saved notice is read back until it is cleared`() = runTest {
        preferences.setGoogleLinkNotice(notice)
        assertEquals(notice, preferences.googleLinkNotice.first())

        preferences.setGoogleLinkNotice(null)
        assertNull(preferences.googleLinkNotice.first())
    }

    @Test
    fun `signing out forgets the notice`() = runTest {
        preferences.setGoogleLinkNotice(notice)

        preferences.clearUserPreferences()

        assertNull(preferences.googleLinkNotice.first())
    }

    @Test
    fun `a stored value that cannot be read counts as no notice`() = runTest {
        dataStore.edit { it[stringPreferencesKey("google_link_notice")] = "{not json" }

        assertNull(preferences.googleLinkNotice.first())
    }
}
