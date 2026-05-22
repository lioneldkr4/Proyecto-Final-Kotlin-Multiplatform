package com.itsur.credito.domain.model

data class DashboardStats(
    val totalClientes: Long,
    val creditosActivos: Long,
    val creditosVencidos: Long,
    val totalPrestado: Double,
    val totalPendiente: Double,
    val topDeudores: List<Pair<String, Double>>
)
