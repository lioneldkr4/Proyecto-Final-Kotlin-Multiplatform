package com.itsur.credito.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsur.credito.data.AbonoExport
import com.itsur.credito.data.CreditoExport
import com.itsur.credito.data.EstadoCuentaExport
import com.itsur.credito.data.ExcelGenerator
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.repository.ClienteRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        cargarEstadisticas()
    }

    private fun cargarCatalogos() {
        viewModelScope.launch {
            runCatching {
                Pair(repository.obtenerEstadosCredito(), repository.obtenerTiposAbono())
            }.onSuccess { (estados, tipos) ->
                _uiState.update { it.copy(estadosCredito = estados, tiposAbono = tipos) }
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun cargarTodosLosClientes() {
        viewModelScope.launch {
            runCatching { repository.obtenerTodos() }
                .onSuccess { clientes -> _uiState.update { it.copy(clientes = clientes) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun cargarEstadisticas() {
        viewModelScope.launch {
            runCatching {
                repository.marcarCreditosVencidos()
                repository.obtenerEstadisticas()
            }
                .onSuccess { stats -> _uiState.update { it.copy(dashboardStats = stats) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun buscarClientes(query: String) {
        viewModelScope.launch {
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
                creditos = emptyList(),
                abonos = emptyList()
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
        viewModelScope.launch {
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
                creditos = emptyList(),
                abonos = emptyList()
            )
        }
        cargarTodosLosClientes()
        cargarEstadisticas()
    }

    fun agregarCliente(nombre: String, telefono: String, direccion: String, limite: Double) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            runCatching { repository.eliminar(clienteId) }
                .onSuccess { volverAInicio() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun registrarAbono(creditoId: Long, monto: Double, tipoId: Long) {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch {
            runCatching {
                repository.registrarAbono(creditoId, monto, obtenerFecha(), tipoId)
                Pair(
                    repository.obtenerCreditosPorCliente(cliente.id),
                    repository.obtenerAbonosPorCliente(cliente.id)
                )
            }
                .onSuccess { (creditos, abonos) ->
                    _uiState.update { it.copy(creditos = creditos, abonos = abonos) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun registrarNuevoCredito(monto: Double, fechaVencimientoStr: String = "") {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        val fechaIso = parsearFechaVencimiento(fechaVencimientoStr)
        viewModelScope.launch {
            runCatching {
                repository.registrarNuevoCredito(cliente.id, monto, cliente.limiteCredito, fechaIso)
                Pair(
                    repository.obtenerCreditosPorCliente(cliente.id),
                    repository.obtenerAbonosPorCliente(cliente.id)
                )
            }
                .onSuccess { (creditos, abonos) ->
                    _uiState.update { it.copy(creditos = creditos, abonos = abonos) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun liquidarCredito(creditoId: Long) {
        val cliente = _uiState.value.clienteSeleccionado ?: return
        viewModelScope.launch {
            runCatching {
                repository.liquidarCredito(creditoId)
                Pair(
                    repository.obtenerCreditosPorCliente(cliente.id),
                    repository.obtenerAbonosPorCliente(cliente.id)
                )
            }
                .onSuccess { (creditos, abonos) ->
                    _uiState.update { it.copy(creditos = creditos, abonos = abonos) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun generarPdf() {
        val estado = _uiState.value
        if (estado.clienteSeleccionado == null) return
        val export = construirEstadoCuenta(estado)
        viewModelScope.launch {
            runCatching { pdfGenerator.generarPdf(export) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun generarExcel() {
        val estado = _uiState.value
        if (estado.clienteSeleccionado == null) return
        val export = construirEstadoCuenta(estado)
        viewModelScope.launch {
            runCatching { excelGenerator.generarExcel(export) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun construirEstadoCuenta(estado: AppUiState): EstadoCuentaExport {
        val cliente = estado.clienteSeleccionado!!
        val creditoUtilizado = estado.creditos.filter { it.estadoId == 1L }.sumOf { it.saldoPendiente }
        val totalAbonado = estado.abonos.sumOf { it.montoAbonado }

        val creditosExport = estado.creditos.map { c ->
            val estadoNombre = estado.estadosCredito.find { it.id == c.estadoId }?.nombre ?: ""
            val abonosDelCredito = estado.abonos.filter { it.creditoId == c.id }.map { a ->
                val tipoNombre = estado.tiposAbono.find { it.id == a.tipoId }?.nombre ?: ""
                AbonoExport(monto = a.montoAbonado, fecha = a.fecha, tipoNombre = tipoNombre)
            }
            CreditoExport(
                id = c.id,
                estadoNombre = estadoNombre,
                montoPrestado = c.montoPrestado,
                saldoPendiente = c.saldoPendiente,
                fechaVencimiento = c.fechaVencimiento,
                abonos = abonosDelCredito
            )
        }

        return EstadoCuentaExport(
            nombre = cliente.nombre,
            telefono = cliente.telefono,
            direccion = cliente.direccion,
            limiteCredito = cliente.limiteCredito,
            creditoUtilizado = creditoUtilizado,
            creditoDisponible = cliente.limiteCredito - creditoUtilizado,
            totalAbonado = totalAbonado,
            creditos = creditosExport,
            fechaGeneracion = obtenerFecha()
        )
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun cargarDetallesCliente(clienteId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.marcarCreditosVencidos()
                Pair(
                    repository.obtenerCreditosPorCliente(clienteId),
                    repository.obtenerAbonosPorCliente(clienteId)
                )
            }
                .onSuccess { (creditos, abonos) ->
                    _uiState.update { it.copy(creditos = creditos, abonos = abonos) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    private fun obtenerFecha(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

    private fun parsearFechaVencimiento(fechaStr: String): String? {
        if (fechaStr.isBlank()) return null
        return try {
            val input = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            input.isLenient = false
            val date = input.parse(fechaStr) ?: return null
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            null
        }
    }
}
