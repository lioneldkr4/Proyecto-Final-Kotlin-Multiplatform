package com.itsur.credito.data

expect class ExcelGenerator {
    fun generarExcel(
        nombre: String,
        telefono: String?,
        direccion: String?,
        limiteCredito: Double,
        saldoPendiente: Double?,
        acciones: List<AccionPdf>
    )
}
