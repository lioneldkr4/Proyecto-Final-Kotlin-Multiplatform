package com.itsur.credito.domain.repository

import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.DashboardStats
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono

interface ClienteRepository {

    // Dashboard
    fun obtenerEstadisticas(): DashboardStats

    // Catálogos
    fun obtenerEstadosCredito(): List<EstadoCredito>
    fun obtenerTiposAbono(): List<TipoAbono>

    // Cliente
    fun obtenerTodos(): List<Cliente>
    fun buscar(query: String): List<Cliente>
    fun insertar(nombre: String, telefono: String?, direccion: String?, limiteCredito: Double)
    fun actualizar(id: Long, nombre: String, telefono: String?, direccion: String?, limiteCredito: Double)
    fun eliminar(id: Long)

    // Crédito — múltiples disposiciones por cliente
    fun obtenerCreditosPorCliente(clienteId: Long): List<Credito>
    fun registrarNuevoCredito(clienteId: Long, monto: Double, limiteCredito: Double, fechaVencimiento: String?): Credito?
    fun liquidarCredito(creditoId: Long)
    fun marcarCreditosVencidos()

    // Abono — aplica a un crédito específico
    fun obtenerAbonosPorCliente(clienteId: Long): List<Abono>
    fun registrarAbono(creditoId: Long, monto: Double, fecha: String, tipoId: Long)
}
