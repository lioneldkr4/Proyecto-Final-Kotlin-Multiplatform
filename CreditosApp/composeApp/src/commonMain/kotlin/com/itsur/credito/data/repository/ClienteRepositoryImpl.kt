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
        database.transaction {
            val creditos = queries.obtenerCreditosPorCliente(id).executeAsList()
            for (credito in creditos) {
                queries.eliminarAbonosPorCredito(credito.id)
                queries.eliminarCredito(credito.id)
            }
            queries.eliminarCliente(id)
        }
    }

    // ── Crédito ───────────────────────────────────────────────────────────────

    override fun obtenerCreditosPorCliente(clienteId: Long): List<Credito> =
        queries.obtenerCreditosPorCliente(clienteId).executeAsList().map { it.toDomain() }

    override fun registrarNuevoCredito(clienteId: Long, monto: Double, limiteCredito: Double): Credito? {
        database.transaction {
            val saldoUsado = queries.obtenerCreditosPorCliente(clienteId)
                .executeAsList()
                .filter { it.estado_id == 1L }
                .sumOf { it.saldo_pendiente }
            val disponible = limiteCredito - saldoUsado
            if (monto > disponible) {
                throw IllegalStateException("El monto $${"%.2f".format(monto)} supera el crédito disponible $${"%.2f".format(disponible)}")
            }
            queries.insertarCredito(clienteId, monto, monto)
        }
        return queries.obtenerCreditosPorCliente(clienteId).executeAsList().firstOrNull()?.toDomain()
    }

    override fun liquidarCredito(creditoId: Long) {
        queries.liquidarCredito(creditoId)
    }

    // ── Abono ─────────────────────────────────────────────────────────────────

    override fun obtenerAbonosPorCliente(clienteId: Long): List<Abono> =
        queries.obtenerAbonosPorCliente(clienteId).executeAsList().map { it.toDomain() }

    override fun registrarAbono(creditoId: Long, monto: Double, fecha: String, tipoId: Long) {
        database.transaction {
            val credito = queries.obtenerCreditoPorId(creditoId).executeAsOneOrNull()
                ?: throw IllegalStateException("Crédito no encontrado")

            if (credito.saldo_pendiente <= 0.0) {
                throw IllegalStateException("El crédito ya se encuentra liquidado")
            }

            queries.insertarAbono(credito.id, monto, fecha, tipoId)
            queries.actualizarSaldoPendiente(monto, credito.id)

            if (credito.saldo_pendiente - monto <= 0.0) {
                queries.liquidarCredito(credito.id)
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
