package com.itsur.credito

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.itsur.credito.App
import com.itsur.credito.data.DatabaseDriverFactory
import com.itsur.credito.db.AppDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gestión de Créditos"
    ) {
        val driver = DatabaseDriverFactory().createDriver()
        val database = AppDatabase(driver)

        App(database = database)
    }
}