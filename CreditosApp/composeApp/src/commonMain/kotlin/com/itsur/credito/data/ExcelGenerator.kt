package com.itsur.credito.data

expect class ExcelGenerator {
    fun generarExcel(estadoCuenta: EstadoCuentaExport)
}
