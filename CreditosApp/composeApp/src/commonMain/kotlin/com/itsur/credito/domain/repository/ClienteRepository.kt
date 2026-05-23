package com.itsur.credito.domain.repository

import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.DashboardStats
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono

interface ClienteRepository {

    // Dashboard
    suspend fun obtenerEstadisticas(): DashboardStats

    // Catálogos
    suspend fun obtenerEstadosCredito(): List<EstadoCredito>
    suspend fun obtenerTiposAbono(): List<TipoAbono>

    // Cliente
    suspend fun obtenerTodos(): List<Cliente>
    suspend fun buscar(query: String): List<Cliente>
    suspend fun insertar(nombre: String, telefono: String?, direccion: String?, limiteCredito: Double)
    suspend fun actualizar(id: Long, nombre: String, telefono: String?, direccion: String?, limiteCredito: Double)
    suspend fun eliminar(id: Long)

    // Crédito — múltiples disposiciones por cliente
    suspend fun obtenerCreditosPorCliente(clienteId: Long): List<Credito>
    suspend fun registrarNuevoCredito(clienteId: Long, monto: Double, limiteCredito: Double, fechaVencimiento: String?): Credito?
    suspend fun liquidarCredito(creditoId: Long)
    suspend fun marcarCreditosVencidos()

    // Abono — aplica a un crédito específico
    suspend fun obtenerAbonosPorCliente(clienteId: Long): List<Abono>
    suspend fun registrarAbono(creditoId: Long, monto: Double, fecha: String, tipoId: Long)
}
