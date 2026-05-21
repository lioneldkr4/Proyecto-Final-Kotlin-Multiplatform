package com.itsur.credito

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itsur.credito.data.ExcelGenerator
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.domain.repository.ClienteRepository
import com.itsur.credito.presentation.AppViewModel
import com.itsur.credito.presentation.Pantalla
import com.itsur.credito.presentation.PantallaAgregar
import com.itsur.credito.presentation.PantallaCobranza
import com.itsur.credito.presentation.PantallaDetalles
import com.itsur.credito.presentation.PantallaEditar

@Composable
fun App(repository: ClienteRepository, pdfGenerator: PdfGenerator, excelGenerator: ExcelGenerator) {
    val viewModel = viewModel { AppViewModel(repository, pdfGenerator, excelGenerator) }
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (uiState.pantalla) {
                Pantalla.Inicio -> PantallaCobranza(
                    clientes = uiState.clientes,
                    onBuscar = viewModel::buscarClientes,
                    onVerMasClick = viewModel::navegarADetalles,
                    onNuevoClienteClick = viewModel::navegarAAgregarCliente
                )
                Pantalla.Detalles -> {
                    uiState.clienteSeleccionado?.let { cliente ->
                        PantallaDetalles(
                            cliente = cliente,
                            creditoActual = uiState.creditoActual,
                            abonos = uiState.abonos,
                            creditosNuevos = uiState.creditosNuevos,
                            tiposAbono = uiState.tiposAbono,
                            estadosCredito = uiState.estadosCredito,
                            onVolver = viewModel::volverAInicio,
                            onEditar = viewModel::navegarAEditar,
                            onImprimirPdf = viewModel::generarPdf,
                            onGenerarExcel = viewModel::generarExcel,
                            onRegistrarAbono = { monto, tipoId -> viewModel.registrarAbono(monto, tipoId) },
                            onRegistrarCredito = viewModel::registrarNuevoCredito,
                            onEliminar = { viewModel.eliminarCliente(cliente.id) }
                        )
                    }
                }
                Pantalla.AgregarCliente -> PantallaAgregar(
                    onVolver = viewModel::volverAInicio,
                    onGuardar = viewModel::agregarCliente
                )
                Pantalla.EditarCliente -> {
                    uiState.clienteSeleccionado?.let { cliente ->
                        PantallaEditar(
                            cliente = cliente,
                            onVolver = viewModel::volverADetalles,
                            onGuardar = viewModel::editarCliente
                        )
                    }
                }
            }
        }
    }
}
