package com.itsur.credito.presentation

import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.DashboardStats
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono

enum class Pantalla {
    Inicio, Detalles, AgregarCliente, EditarCliente
}

data class AppUiState(
    val pantalla: Pantalla = Pantalla.Inicio,
    val clienteSeleccionado: Cliente? = null,
    val clientes: List<Cliente> = emptyList(),
    val creditos: List<Credito> = emptyList(),
    val abonos: List<Abono> = emptyList(),
    val tiposAbono: List<TipoAbono> = emptyList(),
    val estadosCredito: List<EstadoCredito> = emptyList(),
    val dashboardStats: DashboardStats? = null,
    val error: String? = null
)
