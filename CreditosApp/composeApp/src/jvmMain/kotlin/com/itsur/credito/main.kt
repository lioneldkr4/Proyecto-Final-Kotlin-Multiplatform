package com.itsur.credito

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.itsur.credito.data.DatabaseDriverFactory
import com.itsur.credito.data.ExcelGenerator
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.data.repository.ClienteRepositoryImpl
import com.itsur.credito.db.AppDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gestión de Créditos"
    ) {
        val database = AppDatabase(DatabaseDriverFactory().createDriver())
        val repository = ClienteRepositoryImpl(database)
        val pdfGenerator = PdfGenerator()
        val excelGenerator = ExcelGenerator()

        App(repository = repository, pdfGenerator = pdfGenerator, excelGenerator = excelGenerator)
    }
}
