package com.itsur.credito.data

data class AbonoExport(
    val monto: Double,
    val fecha: String,
    val tipoNombre: String
)

data class CreditoExport(
    val id: Long,
    val estadoNombre: String,
    val montoPrestado: Double,
    val saldoPendiente: Double,
    val fechaVencimiento: String?,
    val abonos: List<AbonoExport>
)

data class EstadoCuentaExport(
    val nombre: String,
    val telefono: String?,
    val direccion: String?,
    val limiteCredito: Double,
    val creditoUtilizado: Double,
    val creditoDisponible: Double,
    val totalAbonado: Double,
    val creditos: List<CreditoExport>,
    val fechaGeneracion: String
)
