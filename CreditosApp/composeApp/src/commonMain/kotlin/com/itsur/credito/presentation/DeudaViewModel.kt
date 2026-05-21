package com.itsur.credito.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsur.credito.data.AccionPdf
import com.itsur.credito.data.ExcelGenerator
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.repository.ClienteRepository
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
    private val repository: ClienteRepository,
    private val pdfGenerator: PdfGenerator,
    private val excelGenerator: ExcelGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        cargarTodosLosClientes()
        cargarCatalogos()
    }

    private fun cargarCatalogos() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                Pair(repository.obtenerEstadosCredito(), repository.obtenerTiposAbono())
            }.onSuccess { (estados, tipos) ->
                _uiState.update { it.copy(estadosCredito = estados, tiposAbono = tipos) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun cargarTodosLosClientes() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.obtenerTodos() }
                .onSuccess { clientes -> _uiState.update { it.copy(clientes = clientes) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun buscarClientes(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.buscar(query) }
                .onSuccess { clientes -> _uiState.update { it.copy(clientes = clientes) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun navegarADetalles(cliente: Cliente) {
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

    fun navegarAEditar() {
        _uiState.update { it.copy(pantalla = Pantalla.EditarCliente) }
    }

    fun volverADetalles() {
        _uiState.update { it.copy(pantalla = Pantalla.Detalles) }
    }

    fun editarCliente(nombre: String, telefono: String, direccion: String, limite: Double) {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.actualizar(
                    id = cliente.id,
                    nombre = nombre,
                    telefono = telefono.ifBlank { null },
                    direccion = direccion.ifBlank { null },
                    limiteCredito = limite
                )
            }
                .onSuccess {
                    val actualizado = cliente.copy(
                        nombre = nombre,
                        telefono = telefono.ifBlank { null },
                        direccion = direccion.ifBlank { null },
                        limiteCredito = limite
                    )
                    _uiState.update { it.copy(pantalla = Pantalla.Detalles, clienteSeleccionado = actualizado) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
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
                repository.insertar(
                    nombre = nombre,
                    telefono = telefono.ifBlank { null },
                    direccion = direccion.ifBlank { null },
                    limiteCredito = limite
                )
            }
                .onSuccess { volverAInicio() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun eliminarCliente(clienteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.eliminar(clienteId) }
                .onSuccess { volverAInicio() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    // Transacción: inserta el abono y actualiza el saldo en una sola operación atómica
    fun registrarAbono(monto: Double, tipoId: Long) {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.registrarAbono(cliente.id, cliente.limiteCredito, monto, obtenerFecha(), tipoId)
                Pair(
                    repository.obtenerCreditoPorCliente(cliente.id),
                    repository.obtenerAbonosPorCliente(cliente.id)
                )
            }
                .onSuccess { (credito, abonos) ->
                    _uiState.update { it.copy(creditoActual = credito, abonos = abonos) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    // Transacción: crea o acumula el crédito de forma atómica
    fun registrarNuevoCredito(monto: Double) {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                Pair(repository.registrarNuevoCredito(cliente.id, monto), obtenerFecha())
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

    fun liquidarCredito() {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.liquidarCredito(cliente.id) }
                .onSuccess { cargarDetallesCliente(cliente.id) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun generarPdf() {
        val estado = _uiState.value
        val cliente = estado.clienteSeleccionado ?: return

        val acciones = estado.abonos.map { AccionPdf("Abono", it.montoAbonado, it.fecha) } +
                estado.creditosNuevos.map { AccionPdf(it.tipo, it.monto, it.detalle.removePrefix("Fecha: ")) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                pdfGenerator.generarPdf(
                    nombre = cliente.nombre,
                    telefono = cliente.telefono,
                    direccion = cliente.direccion,
                    limiteCredito = cliente.limiteCredito,
                    saldoPendiente = estado.creditoActual?.saldoPendiente,
                    acciones = acciones
                )
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun generarExcel() {
        val estado = _uiState.value
        val cliente = estado.clienteSeleccionado ?: return

        val acciones = estado.abonos.map { AccionPdf("Abono", it.montoAbonado, it.fecha) } +
                estado.creditosNuevos.map { AccionPdf(it.tipo, it.monto, it.detalle.removePrefix("Fecha: ")) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                excelGenerator.generarExcel(
                    nombre = cliente.nombre,
                    telefono = cliente.telefono,
                    direccion = cliente.direccion,
                    limiteCredito = cliente.limiteCredito,
                    saldoPendiente = estado.creditoActual?.saldoPendiente,
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
                    repository.obtenerCreditoPorCliente(clienteId),
                    repository.obtenerAbonosPorCliente(clienteId)
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
