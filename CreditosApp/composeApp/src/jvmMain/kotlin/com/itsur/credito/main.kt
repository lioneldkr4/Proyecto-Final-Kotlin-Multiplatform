package com.itsur.credito

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.itsur.credito.data.DatabaseDriverFactory
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.db.AppDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gestión de Créditos"
    ) {
        val driver = DatabaseDriverFactory().createDriver()
        val database = AppDatabase(driver)
        val pdfGenerator = PdfGenerator()

        App(database = database, pdfGenerator = pdfGenerator)
    }
}
