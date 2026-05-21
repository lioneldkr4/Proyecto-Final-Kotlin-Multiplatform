package com.itsur.credito.data.repository

import com.itsur.credito.db.AppDatabase
import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono
import com.itsur.credito.domain.repository.ClienteRepository

class ClienteRepositoryImpl(private val database: AppDatabase) : ClienteRepository {

    private val queries get() = database.appDatabaseQueries

    init {
        // Pre-popula catálogos con INSERT OR IGNORE (idempotente)
        database.transaction {
            queries.poblarEstadosCredito()
            queries.poblarTiposAbono()
        }
    }

    // ── Catálogos ────────────────────────────────────────────────────────────

    override fun obtenerEstadosCredito(): List<EstadoCredito> =
        queries.obtenerEstadosCredito().executeAsList().map { it.toDomain() }

    override fun obtenerTiposAbono(): List<TipoAbono> =
        queries.obtenerTiposAbono().executeAsList().map { it.toDomain() }

    // ── Cliente ───────────────────────────────────────────────────────────────

    override fun obtenerTodos(): List<Cliente> =
        queries.obtenerTodosLosClientes().executeAsList().map { it.toDomain() }

    override fun buscar(query: String): List<Cliente> =
        queries.buscarClientesGenerales("%$query%").executeAsList().map { it.toDomain() }

    override fun insertar(nombre: String, telefono: String?, direccion: String?, limiteCredito: Double) {
        queries.insertarCliente(nombre, telefono, direccion, limiteCredito)
    }

    override fun actualizar(id: Long, nombre: String, telefono: String?, direccion: String?, limiteCredito: Double) {
        queries.actualizarCliente(nombre, telefono, direccion, limiteCredito, id)
    }

    override fun eliminar(id: Long) {
        // Elimina en cascada: primero abonos, luego crédito, luego cliente
        database.transaction {
            val credito = queries.obtenerCreditoPorCliente(id).executeAsOneOrNull()
            if (credito != null) {
                queries.eliminarAbonosPorCredito(credito.id)
                queries.eliminarCredito(credito.id)
            }
            queries.eliminarCliente(id)
        }
    }

    // ── Crédito ───────────────────────────────────────────────────────────────

    override fun obtenerCreditoPorCliente(clienteId: Long): Credito? =
        queries.obtenerCreditoPorCliente(clienteId).executeAsOneOrNull()?.toDomain()

    override fun registrarNuevoCredito(clienteId: Long, monto: Double): Credito? {
        database.transaction {
            val credito = queries.obtenerCreditoPorCliente(clienteId).executeAsOneOrNull()
            if (credito == null) {
                queries.insertarCredito(clienteId, monto, monto)
            } else {
                queries.acumularCredito(monto, monto, clienteId)
            }
        }
        return queries.obtenerCreditoPorCliente(clienteId).executeAsOneOrNull()?.toDomain()
    }

    override fun liquidarCredito(clienteId: Long) {
        queries.liquidarCredito(clienteId)
    }

    // ── Abono ─────────────────────────────────────────────────────────────────

    override fun obtenerAbonosPorCliente(clienteId: Long): List<Abono> =
        queries.obtenerAbonosPorCliente(clienteId).executeAsList().map { it.toDomain() }

    override fun registrarAbono(clienteId: Long, limiteCredito: Double, monto: Double, fecha: String, tipoId: Long) {
        // Transacción: si falla la actualización del saldo, el abono se revierte (rollback)
        database.transaction {
            var credito = queries.obtenerCreditoPorCliente(clienteId).executeAsOneOrNull()

            if (credito != null && credito.saldo_pendiente <= 0.0) {
                throw IllegalStateException("El crédito ya se encuentra liquidado")
            }

            if (credito == null) {
                queries.insertarCredito(clienteId, limiteCredito, limiteCredito)
                credito = queries.obtenerCreditoPorCliente(clienteId).executeAsOne()
            }

            queries.insertarAbono(credito.id, monto, fecha, tipoId)
            queries.actualizarSaldoPendiente(monto, credito.id)

            // Auto-liquidar cuando el saldo llega a cero
            if (credito.saldo_pendiente - monto <= 0.0) {
                queries.liquidarCredito(clienteId)
            }
        }
    }

    // ── Mappers db → domain ───────────────────────────────────────────────────

    private fun com.itsur.credito.db.EstadoCredito.toDomain() =
        EstadoCredito(id = id, nombre = nombre)

    private fun com.itsur.credito.db.TipoAbono.toDomain() =
        TipoAbono(id = id, nombre = nombre)

    private fun com.itsur.credito.db.Cliente.toDomain() = Cliente(
        id = id,
        nombre = nombre,
        telefono = telefono,
        direccion = direccion,
        limiteCredito = limite_credito
    )

    private fun com.itsur.credito.db.Credito.toDomain() = Credito(
        id = id,
        clienteId = cliente_id,
        montoPrestado = monto_prestado,
        saldoPendiente = saldo_pendiente,
        estadoId = estado_id
    )

    private fun com.itsur.credito.db.Abono.toDomain() = Abono(
        id = id,
        creditoId = credito_id,
        montoAbonado = monto_abonado,
        fecha = fecha,
        tipoId = tipo_id
    )
}
