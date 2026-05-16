package com.itsur.credito.data

import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual class PdfGenerator {

    actual fun generarPdf(
        nombre: String,
        telefono: String?,
        direccion: String?,
        limiteCredito: Double,
        saldoPendiente: Double?,
        acciones: List<AccionPdf>
    ) {
        val streamContent = buildStream(nombre, telefono, direccion, limiteCredito, saldoPendiente, acciones)
        val pdfBytes = buildPdf(streamContent)

        val docsDir = File(System.getProperty("user.home"), "Documents").also { it.mkdirs() }
        val fileName = "credito_${nombre.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(docsDir, fileName)
        file.writeBytes(pdfBytes)

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        }
    }

    private fun String.escapePdf() =
        replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    private fun Double.fmt() = String.format(Locale.US, "%.2f", this)

    private fun buildStream(
        nombre: String,
        telefono: String?,
        direccion: String?,
        limiteCredito: Double,
        saldoPendiente: Double?,
        acciones: List<AccionPdf>
    ): String {
        data class Linea(val size: Int, val y: Double, val text: String)

        val lines = mutableListOf<Linea>()
        var y = 800.0

        fun add(text: String, size: Int = 11, step: Double = size * 1.6) {
            lines.add(Linea(size, y, text))
            y -= step
        }

        add("Resumen de Credito", size = 18, step = 28.0)
        add(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()), size = 9, step = 24.0)
        add("Cliente: $nombre", size = 12, step = 18.0)
        telefono?.let { add("Telefono: $it") }
        direccion?.let { add("Direccion: $it") }
        add("Limite de Credito: \$${limiteCredito.fmt()}")
        add("Saldo Pendiente: \$${(saldoPendiente ?: limiteCredito).fmt()}")
        y -= 10.0
        add("Historial de Acciones", size = 13, step = 22.0)

        if (acciones.isEmpty()) {
            add("Sin acciones registradas.")
        } else {
            acciones.forEach { accion ->
                if (y < 50.0) return@forEach
                add("${accion.tipo}: \$${accion.monto.fmt()}   |   ${accion.fecha}", size = 10, step = 15.0)
            }
        }

        return buildString {
            append("BT\n")
            lines.forEach { (size, ly, text) ->
                append("/F1 $size Tf\n1 0 0 1 50 $ly Tm\n(${text.escapePdf()}) Tj\n")
            }
            append("ET\n")
        }
    }

    private fun buildPdf(streamContent: String): ByteArray {
        // PDF stream must be encoded in ISO-8859-1 so accented chars (á,é,ñ…) render correctly
        val streamBytes = streamContent.toByteArray(Charsets.ISO_8859_1)
        val out = ByteArrayOutputStream()
        val offsets = IntArray(5)

        fun w(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        // xref entries must be exactly 20 bytes: 10-digit offset + " 00000 n\r\n"
        fun xrefLine(offset: Int) = offset.toString().padStart(10, '0') + " 00000 n\r\n"

        w("%PDF-1.4\n")

        offsets[0] = out.size()
        w("1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n")

        offsets[1] = out.size()
        w("2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n")

        offsets[2] = out.size()
        w("3 0 obj\n<</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                "/Contents 4 0 R /Resources <</Font <</F1 5 0 R>>>>>>\nendobj\n")

        offsets[3] = out.size()
        w("4 0 obj\n<</Length ${streamBytes.size}>>\nstream\n")
        out.write(streamBytes)
        w("\nendstream\nendobj\n")

        offsets[4] = out.size()
        // WinAnsiEncoding enables á, é, í, ó, ú, ñ via ISO-8859-1/Windows-1252
        w("5 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Helvetica " +
                "/Encoding /WinAnsiEncoding>>\nendobj\n")

        val xrefOffset = out.size()
        // 6 entries: 1 free (obj 0) + 5 objects (obj 1-5)
        w("xref\n0 6\n0000000000 65535 f\r\n")
        offsets.forEach { w(xrefLine(it)) }

        w("trailer\n<</Size 6 /Root 1 0 R>>\nstartxref\n$xrefOffset\n%%EOF\n")

        return out.toByteArray()
    }
}
