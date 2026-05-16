package com.itsur.credito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.itsur.credito.data.DatabaseDriverFactory
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.db.AppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val driver = DatabaseDriverFactory(applicationContext).createDriver()
        val database = AppDatabase(driver)
        val pdfGenerator = PdfGenerator(applicationContext)

        setContent {
            App(database = database, pdfGenerator = pdfGenerator)
        }
    }
}
