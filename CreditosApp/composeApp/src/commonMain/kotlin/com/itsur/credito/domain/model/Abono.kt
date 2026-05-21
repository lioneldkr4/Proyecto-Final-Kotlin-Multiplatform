package com.itsur.credito.domain.model

data class Abono(
    val id: Long,
    val creditoId: Long,
    val montoAbonado: Double,
    val fecha: String,
    val tipoId: Long
)
