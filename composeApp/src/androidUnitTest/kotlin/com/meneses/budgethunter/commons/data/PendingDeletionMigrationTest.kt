package com.meneses.budgethunter.commons.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Migration 5 adds the table of deletions waiting to be pushed to a database that has none. */
class PendingDeletionMigrationTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `migration 5 creates the pending deletion table`() {
        Database.Schema.create(driver)
        driver.execute(null, "DROP TABLE pending_deletion", 0)

        Database.Schema.migrate(driver, 5, 6)

        val database = Database(driver, Budget_entry.Adapter(typeAdapter, categoryAdapter))
        database.budgetQueries.insert(
            amount = 1.0,
            name = "Budget",
            date = "2026-01-01",
            server_id = 101,
            is_synced = 1,
            last_synced_at = null,
            collaborator_count = 1
        )
        val budgetId = database.budgetQueries.selectLastId().executeAsOne()
        database.budgetQueries.recordDeletion(created_at = "2026-01-02T10:00:00Z", id = budgetId)
        assertEquals(listOf(101L), database.budgetQueries.selectPendingDeletions().executeAsList())
    }
}
