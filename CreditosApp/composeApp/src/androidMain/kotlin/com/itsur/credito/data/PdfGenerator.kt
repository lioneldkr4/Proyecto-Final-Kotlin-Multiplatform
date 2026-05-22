package com.itsur.credito.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

actual class PdfGenerator(private val context: Context) {

    actual fun generarPdf(estadoCuenta: EstadoCuentaExport) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        renderPage(page.canvas, estadoCuenta)
        document.finishPage(page)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        dir.mkdirs()
        val safe = estadoCuenta.nombre.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(dir, "credito_${safe}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun Double.fmt() = String.format(Locale.US, "%,.2f", this)

    private fun renderPage(canvas: Canvas, ec: EstadoCuentaExport) {
        val pTitle   = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val pSection = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val pBold    = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val pNormal  = Paint().apply { textSize = 11f }
        val pSmall   = Paint().apply { textSize = 9f }
        val pMini    = Paint().apply { textSize = 9f }
        val pBoldSm  = Paint().apply { textSize = 10f; isFakeBoldText = true }
        val pLine    = Paint().apply { strokeWidth = 0.5f }

        var y = 50f

        fun text(s: String, paint: Paint, x: Float = 50f, step: Float = paint.textSize * 1.5f) {
            canvas.drawText(s, x, y, paint)
            y += step
        }

        fun sep() {
            canvas.drawLine(50f, y, 545f, y, pLine)
            y += 12f
        }

        fun section(title: String) {
            y += 4f
            text(title, pSection, step = 20f)
        }

        // ── Encabezado ─────────────────────────────────────────────────────────
        text("ESTADO DE CUENTA", pTitle, step = 22f)
        text("Generado: ${ec.fechaGeneracion}", pSmall, step = 16f)
        sep()

        // ── Datos del cliente ───────────────────────────────────────────────────
        section("DATOS DEL CLIENTE")
        text("Nombre:    ${ec.nombre}", pNormal)
        ec.telefono?.let { text("Telefono:  $it", pNormal) }
        ec.direccion?.let { text("Direccion: $it", pNormal) }
        y += 6f
        sep()

        // ── Resumen financiero ──────────────────────────────────────────────────
        section("RESUMEN FINANCIERO")
        text("Limite de credito:   $${ec.limiteCredito.fmt()}", pNormal)
        text("Credito utilizado:   $${ec.creditoUtilizado.fmt()}", pNormal)
        text("Credito disponible:  $${ec.creditoDisponible.fmt()}", pNormal)
        text("Total abonado:       $${ec.totalAbonado.fmt()}", pNormal)
        val activos   = ec.creditos.count { it.estadoNombre == "Activo" }
        val liquidados = ec.creditos.count { it.estadoNombre == "Liquidado" }
        val vencidos  = ec.creditos.count { it.estadoNombre == "Vencido" }
        text("Creditos: $activos activo(s)   $liquidados liquidado(s)   $vencidos vencido(s)", pNormal)
        y += 6f
        sep()

        // ── Detalle de créditos ─────────────────────────────────────────────────
        section("DETALLE DE CREDITOS")

        if (ec.creditos.isEmpty()) {
            text("Sin creditos registrados.", pNormal)
        } else {
            ec.creditos.forEach { c ->
                if (y > 810f) return@forEach
                y += 8f
                text("Credito #${c.id}   [${c.estadoNombre.uppercase()}]", pBold, step = 16f)
                text("  Monto prestado:  $${c.montoPrestado.fmt()}", pMini, step = 14f)
                val pagado = c.montoPrestado - c.saldoPendiente
                text("  Monto pagado:    $${pagado.fmt()}", pMini, step = 14f)
                if (c.saldoPendiente > 0.0) {
                    text("  Saldo pendiente: $${c.saldoPendiente.fmt()}", pMini, step = 14f)
                }
                c.fechaVencimiento?.let { iso ->
                    val p = iso.split("-")
                    val disp = if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
                    text("  Vencimiento:     $disp", pMini, step = 14f)
                }
                if (c.abonos.isEmpty()) {
                    text("  Sin abonos registrados.", pMini, step = 14f)
                } else {
                    text("  Abonos (${c.abonos.size}):", pBoldSm, step = 14f)
                    c.abonos.forEach { a ->
                        if (y > 825f) return@forEach
                        text("    $${a.monto.fmt()}   ${a.fecha}   ${a.tipoNombre}",
                            pSmall, step = 13f)
                    }
                }
            }
        }
    }
}
