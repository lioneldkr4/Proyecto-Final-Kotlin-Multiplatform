package com.itsur.credito.data

expect class PdfGenerator {
    fun generarPdf(estadoCuenta: EstadoCuentaExport)
}
