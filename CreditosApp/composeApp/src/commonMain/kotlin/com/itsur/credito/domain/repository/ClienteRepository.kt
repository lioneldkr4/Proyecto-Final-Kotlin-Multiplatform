package com.itsur.credito.domain.repository

import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono

interface ClienteRepository {

    // Catálogos
    fun obtenerEstadosCredito(): List<EstadoCredito>
    fun obtenerTiposAbono(): List<TipoAbono>

    // Cliente
    fun obtenerTodos(): List<Cliente>
    fun buscar(query: String): List<Cliente>
    fun insertar(nombre: String, telefono: String?, direccion: String?, limiteCredito: Double)
    fun actualizar(id: Long, nombre: String, telefono: String?, direccion: String?, limiteCredito: Double)
    fun eliminar(id: Long)

    // Crédito (operaciones consolidadas con transacción)
    fun obtenerCreditoPorCliente(clienteId: Long): Credito?
    fun registrarNuevoCredito(clienteId: Long, monto: Double): Credito?
    fun liquidarCredito(clienteId: Long)

    // Abono (operación consolidada con transacción)
    fun obtenerAbonosPorCliente(clienteId: Long): List<Abono>
    fun registrarAbono(clienteId: Long, limiteCredito: Double, monto: Double, fecha: String, tipoId: Long)
}
