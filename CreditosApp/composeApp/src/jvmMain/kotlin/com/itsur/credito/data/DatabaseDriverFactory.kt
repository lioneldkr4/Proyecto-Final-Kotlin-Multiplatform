package com.itsur.credito.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.itsur.credito.db.AppDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:creditos.db")
        driver.execute(null, "PRAGMA foreign_keys = ON", 0, null)

        val currentVersion = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
            },
            parameters = 0,
            binders = null
        ).value

        when {
            currentVersion == 0L -> {
                AppDatabase.Schema.create(driver)
                driver.execute(null, "PRAGMA user_version = ${AppDatabase.Schema.version}", 0, null)
            }
            currentVersion < AppDatabase.Schema.version -> {
                AppDatabase.Schema.migrate(driver, currentVersion, AppDatabase.Schema.version)
                driver.execute(null, "PRAGMA user_version = ${AppDatabase.Schema.version}", 0, null)
            }
        }

        return driver
    }
}
