package com.itsur.credito.data.repository

import com.itsur.credito.db.AppDatabase
import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.DashboardStats
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono
import com.itsur.credito.domain.repository.ClienteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClienteRepositoryImpl(private val database: AppDatabase) : ClienteRepository {

    private val queries get() = database.appDatabaseQueries

    init {
        database.transaction {
            queries.poblarEstadosCredito()
            queries.poblarTiposAbono()
        }
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    override suspend fun obtenerEstadisticas(): DashboardStats = withContext(Dispatchers.IO) {
        val totalClientes = queries.obtenerTotalClientes().executeAsOne()
        val creditosActivos = queries.contarCreditosActivos().executeAsOne()
        val creditosVencidos = queries.contarCreditosVencidos().executeAsOne()
        val totalPrestado = queries.sumarTotalPrestado().executeAsOne().SUM ?: 0.0
        val totalPendiente = queries.sumarTotalPendiente().executeAsOne().SUM ?: 0.0
        val topDeudores = queries.obtenerTopDeudores().executeAsList()
            .map { Pair(it.nombre, it.deuda_total ?: 0.0) }
        DashboardStats(
            totalClientes = totalClientes,
            creditosActivos = creditosActivos,
            creditosVencidos = creditosVencidos,
            totalPrestado = totalPrestado,
            totalPendiente = totalPendiente,
            topDeudores = topDeudores
        )
    }

    // ── Catálogos ────────────────────────────────────────────────────────────

    override suspend fun obtenerEstadosCredito(): List<EstadoCredito> = withContext(Dispatchers.IO) {
        queries.obtenerEstadosCredito().executeAsList().map { it.toDomain() }
    }

    override suspend fun obtenerTiposAbono(): List<TipoAbono> = withContext(Dispatchers.IO) {
        queries.obtenerTiposAbono().executeAsList().map { it.toDomain() }
    }

    // ── Cliente ───────────────────────────────────────────────────────────────

    override suspend fun obtenerTodos(): List<Cliente> = withContext(Dispatchers.IO) {
        queries.obtenerTodosLosClientes().executeAsList().map { it.toDomain() }
    }

    override suspend fun buscar(query: String): List<Cliente> = withContext(Dispatchers.IO) {
        queries.buscarClientesGenerales("%$query%").executeAsList().map { it.toDomain() }
    }

    override suspend fun insertar(nombre: String, telefono: String?, direccion: String?, limiteCredito: Double) = withContext(Dispatchers.IO) {
        queries.insertarCliente(nombre, telefono, direccion, limiteCredito)
    }

    override suspend fun actualizar(id: Long, nombre: String, telefono: String?, direccion: String?, limiteCredito: Double) = withContext(Dispatchers.IO) {
        queries.actualizarCliente(nombre, telefono, direccion, limiteCredito, id)
    }

    override suspend fun eliminar(id: Long) = withContext(Dispatchers.IO) {
        queries.eliminarCliente(id)
    }

    // ── Crédito ───────────────────────────────────────────────────────────────

    override suspend fun obtenerCreditosPorCliente(clienteId: Long): List<Credito> = withContext(Dispatchers.IO) {
        queries.obtenerCreditosPorCliente(clienteId).executeAsList().map { it.toDomain() }
    }

    override suspend fun registrarNuevoCredito(clienteId: Long, monto: Double, limiteCredito: Double, fechaVencimiento: String?): Credito? = withContext(Dispatchers.IO) {
        database.transaction {
            val saldoUsado = queries.obtenerCreditosPorCliente(clienteId)
                .executeAsList()
                .filter { it.estado_id == 1L }
                .sumOf { it.saldo_pendiente }
            val disponible = limiteCredito - saldoUsado
            if (monto > disponible) {
                throw IllegalStateException("El monto $${"%.2f".format(monto)} supera el crédito disponible $${"%.2f".format(disponible)}")
            }
            queries.insertarCredito(clienteId, monto, monto, fechaVencimiento)
        }
        queries.obtenerCreditosPorCliente(clienteId).executeAsList().firstOrNull()?.toDomain()
    }

    override suspend fun liquidarCredito(creditoId: Long) = withContext(Dispatchers.IO) {
        queries.liquidarCredito(creditoId)
    }

    override suspend fun marcarCreditosVencidos() = withContext(Dispatchers.IO) {
        queries.marcarCreditosVencidos()
    }

    // ── Abono ─────────────────────────────────────────────────────────────────

    override suspend fun obtenerAbonosPorCliente(clienteId: Long): List<Abono> = withContext(Dispatchers.IO) {
        queries.obtenerAbonosPorCliente(clienteId).executeAsList().map { it.toDomain() }
    }

    override suspend fun registrarAbono(creditoId: Long, monto: Double, fecha: String, tipoId: Long) = withContext(Dispatchers.IO) {
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
        estadoId = estado_id,
        fechaVencimiento = fecha_vencimiento
    )

    private fun com.itsur.credito.db.Abono.toDomain() = Abono(
        id = id,
        creditoId = credito_id,
        montoAbonado = monto_abonado,
        fecha = fecha,
        tipoId = tipo_id
    )
}
