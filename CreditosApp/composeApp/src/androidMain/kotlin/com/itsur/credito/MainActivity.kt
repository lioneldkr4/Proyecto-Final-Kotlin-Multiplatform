package com.itsur.credito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.itsur.credito.data.DatabaseDriverFactory
import com.itsur.credito.data.ExcelGenerator
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.data.repository.ClienteRepositoryImpl
import com.itsur.credito.db.AppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase(DatabaseDriverFactory(applicationContext).createDriver())
        val repository = ClienteRepositoryImpl(database)
        val pdfGenerator = PdfGenerator(applicationContext)
        val excelGenerator = ExcelGenerator(applicationContext)

        setContent {
            App(repository = repository, pdfGenerator = pdfGenerator, excelGenerator = excelGenerator)
        }
    }
}
