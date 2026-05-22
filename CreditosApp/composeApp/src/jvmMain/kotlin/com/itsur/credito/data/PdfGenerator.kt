package com.itsur.credito.data

import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

actual class PdfGenerator {

    actual fun generarPdf(estadoCuenta: EstadoCuentaExport) {
        val streamContent = buildStream(estadoCuenta)
        val pdfBytes = buildPdf(streamContent)
        val docsDir = File(System.getProperty("user.home"), "Documents").also { it.mkdirs() }
        val safe = estadoCuenta.nombre.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(docsDir, "credito_${safe}_${System.currentTimeMillis()}.pdf")
        file.writeBytes(pdfBytes)
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file)
    }

    private fun String.escapePdf() =
        replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    private fun Double.fmt() = String.format(Locale.US, "%,.2f", this)

    private fun buildStream(ec: EstadoCuentaExport): String {
        // font: 1 = Helvetica, 2 = Helvetica-Bold
        data class Line(val x: Double, val y: Double, val font: Int, val size: Int, val text: String)

        val items = mutableListOf<Line>()
        var y = 800.0

        fun add(text: String, size: Int = 11, x: Double = 50.0, bold: Boolean = false,
                step: Double = size * 1.6) {
            items.add(Line(x, y, if (bold) 2 else 1, size, text))
            y -= step
        }

        fun sep() = add("-".repeat(72), size = 8, step = 12.0)
        fun section(title: String) { y -= 4.0; add(title, size = 12, bold = true, step = 20.0) }
        fun gap(px: Double = 8.0) { y -= px }

        // ── Encabezado ─────────────────────────────────────────────────────────
        add("ESTADO DE CUENTA", size = 18, bold = true, step = 26.0)
        add("Generado: ${ec.fechaGeneracion}", size = 9, step = 18.0)
        sep()

        // ── Datos del cliente ───────────────────────────────────────────────────
        section("DATOS DEL CLIENTE")
        add("Nombre:    ${ec.nombre}")
        ec.telefono?.let { add("Telefono:  $it") }
        ec.direccion?.let { add("Direccion: $it") }
        gap()
        sep()

        // ── Resumen financiero ──────────────────────────────────────────────────
        section("RESUMEN FINANCIERO")
        add("Limite de credito:   \$${ec.limiteCredito.fmt()}")
        add("Credito utilizado:   \$${ec.creditoUtilizado.fmt()}")
        add("Credito disponible:  \$${ec.creditoDisponible.fmt()}")
        add("Total abonado:       \$${ec.totalAbonado.fmt()}")
        val activos = ec.creditos.count { it.estadoNombre == "Activo" }
        val liquidados = ec.creditos.count { it.estadoNombre == "Liquidado" }
        val vencidos = ec.creditos.count { it.estadoNombre == "Vencido" }
        add("Creditos: $activos activo(s)   $liquidados liquidado(s)   $vencidos vencido(s)")
        gap()
        sep()

        // ── Detalle de créditos ─────────────────────────────────────────────────
        section("DETALLE DE CREDITOS")

        if (ec.creditos.isEmpty()) {
            add("Sin creditos registrados.")
        } else {
            ec.creditos.forEach { c ->
                if (y < 80.0) return@forEach
                gap()
                add("Credito #${c.id}   [${c.estadoNombre.uppercase()}]", size = 11, bold = true, step = 16.0)
                add("  Monto prestado:  \$${c.montoPrestado.fmt()}", size = 10, step = 14.0)
                val pagado = c.montoPrestado - c.saldoPendiente
                add("  Monto pagado:    \$${pagado.fmt()}", size = 10, step = 14.0)
                if (c.saldoPendiente > 0.0) {
                    add("  Saldo pendiente: \$${c.saldoPendiente.fmt()}", size = 10, step = 14.0)
                }
                c.fechaVencimiento?.let { iso ->
                    val p = iso.split("-")
                    val disp = if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
                    add("  Vencimiento:     $disp", size = 10, step = 14.0)
                }
                if (c.abonos.isEmpty()) {
                    add("  Sin abonos registrados.", size = 10, step = 14.0)
                } else {
                    add("  Abonos (${c.abonos.size}):", size = 10, bold = true, step = 14.0)
                    c.abonos.forEach { a ->
                        if (y < 50.0) return@forEach
                        add("    \$${a.monto.fmt()}   ${a.fecha}   ${a.tipoNombre}",
                            size = 9, step = 13.0)
                    }
                }
            }
        }

        return buildString {
            append("BT\n")
            items.forEach { (x, ly, font, size, text) ->
                append("/F$font $size Tf\n1 0 0 1 $x $ly Tm\n(${text.escapePdf()}) Tj\n")
            }
            append("ET\n")
        }
    }

    private fun buildPdf(streamContent: String): ByteArray {
        val streamBytes = streamContent.toByteArray(Charsets.ISO_8859_1)
        val out = ByteArrayOutputStream()
        val offsets = IntArray(6)

        fun w(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun xrefLine(offset: Int) = offset.toString().padStart(10, '0') + " 00000 n\r\n"

        w("%PDF-1.4\n")

        offsets[0] = out.size()
        w("1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n")

        offsets[1] = out.size()
        w("2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n")

        offsets[2] = out.size()
        w("3 0 obj\n<</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                "/Contents 4 0 R /Resources <</Font <</F1 5 0 R /F2 6 0 R>>>>>>\nendobj\n")

        offsets[3] = out.size()
        w("4 0 obj\n<</Length ${streamBytes.size}>>\nstream\n")
        out.write(streamBytes)
        w("\nendstream\nendobj\n")

        offsets[4] = out.size()
        w("5 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Helvetica " +
                "/Encoding /WinAnsiEncoding>>\nendobj\n")

        offsets[5] = out.size()
        w("6 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold " +
                "/Encoding /WinAnsiEncoding>>\nendobj\n")

        val xrefOffset = out.size()
        w("xref\n0 7\n0000000000 65535 f\r\n")
        offsets.forEach { w(xrefLine(it)) }
        w("trailer\n<</Size 7 /Root 1 0 R>>\nstartxref\n$xrefOffset\n%%EOF\n")

        return out.toByteArray()
    }
}
