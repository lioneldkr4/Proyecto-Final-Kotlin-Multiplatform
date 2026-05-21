package com.itsur.credito

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itsur.credito.data.ExcelGenerator
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.domain.repository.ClienteRepository
import com.itsur.credito.presentation.AppNavigation
import com.itsur.credito.presentation.AppViewModel

@Composable
fun App(repository: ClienteRepository, pdfGenerator: PdfGenerator, excelGenerator: ExcelGenerator) {
    val viewModel = viewModel { AppViewModel(repository, pdfGenerator, excelGenerator) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppNavigation(viewModel)
        }
    }
}
