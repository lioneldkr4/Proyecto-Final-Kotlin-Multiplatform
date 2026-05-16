package com.itsur.credito.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsur.credito.data.AccionPdf
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val database: AppDatabase,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val queries get() = database.appDatabaseQueries

    init {
        cargarTodosLosClientes()
    }

    fun cargarTodosLosClientes() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { queries.obtenerTodosLosClientes().executeAsList() }
                .onSuccess { clientes -> _uiState.update { it.copy(clientes = clientes) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun buscarClientes(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { queries.buscarClientesGenerales("%$query%").executeAsList() }
                .onSuccess { clientes -> _uiState.update { it.copy(clientes = clientes) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun navegarADetalles(cliente: com.itsur.credito.db.Cliente) {
        _uiState.update {
            it.copy(
                pantalla = Pantalla.Detalles,
                clienteSeleccionado = cliente,
                creditosNuevos = emptyList()
            )
        }
        cargarDetallesCliente(cliente.id)
    }

    fun navegarAAgregarCliente() {
        _uiState.update { it.copy(pantalla = Pantalla.AgregarCliente) }
    }

    fun volverAInicio() {
        _uiState.update {
            it.copy(
                pantalla = Pantalla.Inicio,
                clienteSeleccionado = null,
                creditoActual = null,
                abonos = emptyList(),
                creditosNuevos = emptyList()
            )
        }
        cargarTodosLosClientes()
    }

    fun agregarCliente(nombre: String, telefono: String, direccion: String, limite: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                queries.insertarCliente(
                    nombre = nombre,
                    telefono = telefono.ifBlank { null },
                    direccion = direccion.ifBlank { null },
                    limite_credito = limite
                )
            }
                .onSuccess { volverAInicio() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun registrarAbono(monto: Double) {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                var credito = queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull()
                if (credito == null) {
                    queries.insertarCredito(cliente.id, cliente.limite_credito, cliente.limite_credito)
                    credito = queries.obtenerCreditoPorCliente(cliente.id).executeAsOne()
                }
                queries.insertarAbono(credito.id, monto, obtenerFecha())
                queries.actualizarSaldoPendiente(monto, credito.id)
            }
                .onSuccess { cargarDetallesCliente(cliente.id) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun registrarNuevoCredito(monto: Double) {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val credito = queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull()
                if (credito == null) {
                    queries.insertarCredito(cliente.id, monto, monto)
                } else {
                    queries.acumularCredito(monto, monto, cliente.id)
                }
                Pair(queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull(), obtenerFecha())
            }
                .onSuccess { (creditoActualizado, fecha) ->
                    val nuevaAccion = AccionUI(tipo = "Nuevo Crédito", monto = monto, detalle = "Fecha: $fecha")
                    _uiState.update {
                        it.copy(
                            creditoActual = creditoActualizado,
                            creditosNuevos = it.creditosNuevos + nuevaAccion
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun generarPdf() {
        val estado = _uiState.value
        val cliente = estado.clienteSeleccionado ?: return

        val acciones = estado.abonos.map { AccionPdf("Abono", it.monto_abonado, it.fecha) } +
                estado.creditosNuevos.map { AccionPdf(it.tipo, it.monto, it.detalle.removePrefix("Fecha: ")) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                pdfGenerator.generarPdf(
                    nombre = cliente.nombre,
                    telefono = cliente.telefono,
                    direccion = cliente.direccion,
                    limiteCredito = cliente.limite_credito,
                    saldoPendiente = estado.creditoActual?.saldo_pendiente,
                    acciones = acciones
                )
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun cargarDetallesCliente(clienteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                Pair(
                    queries.obtenerCreditoPorCliente(clienteId).executeAsOneOrNull(),
                    queries.obtenerAbonosPorCliente(clienteId).executeAsList()
                )
            }
                .onSuccess { (credito, abonos) ->
                    _uiState.update { it.copy(creditoActual = credito, abonos = abonos) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun obtenerFecha(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
}
