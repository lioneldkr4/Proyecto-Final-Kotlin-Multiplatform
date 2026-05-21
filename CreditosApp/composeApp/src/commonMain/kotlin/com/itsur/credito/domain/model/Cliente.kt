package com.itsur.credito.domain.model

data class Cliente(
    val id: Long,
    val nombre: String,
    val telefono: String?,
    val direccion: String?,
    val limiteCredito: Double
)