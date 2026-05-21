package com.itsur.credito.domain.model

data class Credito(
    val id: Long,
    val clienteId: Long,
    val montoPrestado: Double,
    val saldoPendiente: Double,
    val estadoId: Long
)
