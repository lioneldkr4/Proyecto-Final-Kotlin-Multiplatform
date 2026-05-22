package com.itsur.credito.domain.model

data class Credito(
    val id: Long,
    val clienteId: Long,
    val montoPrestado: Double,
    val saldoPendiente: Double,
    val estadoId: Long,
    val fechaVencimiento: String?  // ISO 8601: yyyy-MM-dd, nullable
)
