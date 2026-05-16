package com.itsur.credito.data

data class AccionPdf(
    val tipo: String,
    val monto: Double,
    val fecha: String
)

expect class PdfGenerator {
    fun generarPdf(
        nombre: String,
        telefono: String?,
        direccion: String?,
        limiteCredito: Double,
        saldoPendiente: Double?,
        acciones: List<AccionPdf>
    )
}
