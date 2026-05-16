package com.itsur.credito.data

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual class PdfGenerator(private val context: Context) {

    actual fun generarPdf(
        nombre: String,
        telefono: String?,
        direccion: String?,
        limiteCredito: Double,
        saldoPendiente: Double?,
        acciones: List<AccionPdf>
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val paintBold = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val paintNormal = Paint().apply { textSize = 11f }
        val paintSmall = Paint().apply { textSize = 9f }
        val paintSection = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val paintAccion = Paint().apply { textSize = 10f }
        val paintDivider = Paint().apply { strokeWidth = 0.5f }

        var y = 50f

        canvas.drawText("Resumen de Credito", 50f, y, paintTitle)
        y += 14f
        canvas.drawText(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()), 50f, y, paintSmall)
        y += 20f
        canvas.drawLine(50f, y, 545f, y, paintDivider)
        y += 18f

        canvas.drawText("Cliente:", 50f, y, paintBold)
        canvas.drawText(nombre, 140f, y, paintNormal)
        y += 16f

        telefono?.let {
            canvas.drawText("Telefono:", 50f, y, paintBold)
            canvas.drawText(it, 140f, y, paintNormal)
            y += 16f
        }

        direccion?.let {
            canvas.drawText("Direccion:", 50f, y, paintBold)
            canvas.drawText(it, 140f, y, paintNormal)
            y += 16f
        }

        y += 4f
        canvas.drawText("Limite de Credito:", 50f, y, paintBold)
        canvas.drawText("$${String.format(Locale.US, "%.2f", limiteCredito)}", 200f, y, paintNormal)
        y += 16f

        canvas.drawText("Saldo Pendiente:", 50f, y, paintBold)
        canvas.drawText("$${String.format(Locale.US, "%.2f", saldoPendiente ?: limiteCredito)}", 200f, y, paintNormal)
        y += 24f

        canvas.drawLine(50f, y, 545f, y, paintDivider)
        y += 18f
        canvas.drawText("Historial de Acciones", 50f, y, paintSection)
        y += 20f

        if (acciones.isEmpty()) {
            canvas.drawText("Sin acciones registradas.", 50f, y, paintNormal)
        } else {
            acciones.forEach { accion ->
                if (y > 815f) return@forEach
                canvas.drawText(
                    "${accion.tipo}: $${String.format(Locale.US, "%.2f", accion.monto)}   |   ${accion.fecha}",
                    50f, y, paintAccion
                )
                y += 15f
            }
        }

        document.finishPage(page)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        dir.mkdirs()
        val fileName = "credito_${nombre.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(dir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
