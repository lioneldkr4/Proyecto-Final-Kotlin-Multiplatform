package com.itsur.credito.presentation

import com.itsur.credito.db.Abono
import com.itsur.credito.db.Cliente
import com.itsur.credito.db.Credito

enum class Pantalla {
    Inicio, Detalles, AgregarCliente
}

data class AccionUI(
    val tipo: String,
    val monto: Double,
    val detalle: String
)

data class AppUiState(
    val pantalla: Pantalla = Pantalla.Inicio,
    val clienteSeleccionado: Cliente? = null,
    val clientes: List<Cliente> = emptyList(),
    val creditoActual: Credito? = null,
    val abonos: List<Abono> = emptyList(),
    val creditosNuevos: List<AccionUI> = emptyList(),
    val error: String? = null
)
