package com.itsur.credito.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.itsur.credito.db.AppDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:creditos.db")

        try {
            AppDatabase.Schema.create(driver)
        } catch (e: Exception) {
        }

        return driver
    }
}