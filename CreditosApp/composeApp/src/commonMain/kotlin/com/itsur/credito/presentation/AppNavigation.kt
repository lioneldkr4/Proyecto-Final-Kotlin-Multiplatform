package com.itsur.credito.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val INICIO = "inicio"
    const val DETALLES = "detalles"
    const val AGREGAR = "agregar"
    const val EDITAR = "editar"
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    // Puente: cuando el ViewModel cambia de pantalla, el NavController navega
    LaunchedEffect(uiState.pantalla) {
        when (uiState.pantalla) {
            Pantalla.Inicio -> navController.navigate(Routes.INICIO) {
                popUpTo(Routes.INICIO) { inclusive = true }
                launchSingleTop = true
            }
            Pantalla.Detalles -> navController.navigate(Routes.DETALLES) {
                launchSingleTop = true
            }
            Pantalla.AgregarCliente -> navController.navigate(Routes.AGREGAR) {
                launchSingleTop = true
            }
            Pantalla.EditarCliente -> navController.navigate(Routes.EDITAR) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.INICIO) {

        composable(Routes.INICIO) {
            PantallaCobranza(
                clientes = uiState.clientes,
                onBuscar = viewModel::buscarClientes,
                onVerMasClick = viewModel::navegarADetalles,
                onNuevoClienteClick = viewModel::navegarAAgregarCliente
            )
        }

        composable(Routes.DETALLES) {
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

        composable(Routes.AGREGAR) {
            PantallaAgregar(
                onVolver = viewModel::volverAInicio,
                onGuardar = viewModel::agregarCliente
            )
        }

        composable(Routes.EDITAR) {
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
