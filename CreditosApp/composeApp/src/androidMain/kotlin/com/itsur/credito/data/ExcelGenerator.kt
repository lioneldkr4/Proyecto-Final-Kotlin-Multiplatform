package com.itsur.credito.data

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual class ExcelGenerator(private val context: Context) {

    actual fun generarExcel(estadoCuenta: EstadoCuentaExport) {
        val bytes = buildXlsxBytes(estadoCuenta)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        dir.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safe = estadoCuenta.nombre.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(dir, "credito_${safe}_$ts.xlsx")
        file.writeBytes(bytes)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun buildXlsxBytes(ec: EstadoCuentaExport): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun add(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            add("[Content_Types].xml", contentTypes())
            add("_rels/.rels", rootRels())
            add("xl/workbook.xml", workbook())
            add("xl/_rels/workbook.xml.rels", workbookRels())
            add("xl/styles.xml", styles())
            add("xl/worksheets/sheet1.xml", buildSheetXml(ec))
        }
        return out.toByteArray()
    }

    private fun buildSheetXml(ec: EstadoCuentaExport): String {
        val sb = StringBuilder()
        var r = 1

        fun String.esc() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        fun Double.fmt() = String.format(Locale.US, "%.2f", this)

        fun str(col: String, v: String, bold: Boolean = false): String {
            val s = if (bold) " s=\"1\"" else ""
            return "<c r=\"$col$r\" t=\"inlineStr\"$s><is><t>${v.esc()}</t></is></c>"
        }
        fun num(col: String, v: Double, bold: Boolean = false): String {
            val s = if (bold) " s=\"1\"" else ""
            return "<c r=\"$col$r\"$s><v>${v.fmt()}</v></c>"
        }
        fun lng(col: String, v: Long) = "<c r=\"$col$r\"><v>$v</v></c>"
        fun row(vararg cells: String) { sb.append("<row r=\"$r\">${cells.joinToString("")}</row>"); r++ }
        fun gap() { sb.append("<row r=\"$r\"/>"); r++ }

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")

        // ── Encabezado ────────────────────────────────────────────────────────
        row(str("A", "ESTADO DE CUENTA", bold = true))
        row(str("A", "Generado: ${ec.fechaGeneracion}"))
        gap()

        // ── Datos del cliente ─────────────────────────────────────────────────
        row(str("A", "DATOS DEL CLIENTE", bold = true))
        row(str("A", "Nombre"), str("B", ec.nombre))
        ec.telefono?.let { row(str("A", "Telefono"), str("B", it)) }
        ec.direccion?.let { row(str("A", "Direccion"), str("B", it)) }
        gap()

        // ── Resumen financiero ────────────────────────────────────────────────
        row(str("A", "RESUMEN FINANCIERO", bold = true))
        row(str("A", "Limite de credito"),   num("B", ec.limiteCredito))
        row(str("A", "Credito utilizado"),   num("B", ec.creditoUtilizado))
        row(str("A", "Credito disponible"),  num("B", ec.creditoDisponible))
        row(str("A", "Total abonado"),       num("B", ec.totalAbonado))
        val activos    = ec.creditos.count { it.estadoNombre == "Activo" }.toLong()
        val liquidados = ec.creditos.count { it.estadoNombre == "Liquidado" }.toLong()
        val vencidos   = ec.creditos.count { it.estadoNombre == "Vencido" }.toLong()
        row(str("A", "Creditos activos"),    lng("B", activos))
        row(str("A", "Creditos liquidados"), lng("B", liquidados))
        if (vencidos > 0) row(str("A", "Creditos vencidos"), lng("B", vencidos))
        gap()

        // ── Tabla de créditos ─────────────────────────────────────────────────
        row(str("A", "DETALLE DE CREDITOS", bold = true))
        row(
            str("A", "#", bold = true),
            str("B", "Estado", bold = true),
            str("C", "Monto Prestado ($)", bold = true),
            str("D", "Monto Pagado ($)", bold = true),
            str("E", "Saldo Pendiente ($)", bold = true),
            str("F", "Vencimiento", bold = true)
        )
        if (ec.creditos.isEmpty()) {
            row(str("A", "Sin creditos registrados."))
        } else {
            ec.creditos.forEach { c ->
                val pagado = c.montoPrestado - c.saldoPendiente
                val fechaDisp = c.fechaVencimiento?.let { iso ->
                    val p = iso.split("-")
                    if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
                } ?: "-"
                row(
                    str("A", "#${c.id}"),
                    str("B", c.estadoNombre),
                    num("C", c.montoPrestado),
                    num("D", pagado),
                    num("E", c.saldoPendiente),
                    str("F", fechaDisp)
                )
            }
        }
        gap()

        // ── Tabla de abonos ───────────────────────────────────────────────────
        row(str("A", "HISTORIAL DE ABONOS", bold = true))
        row(
            str("A", "Credito #", bold = true),
            str("B", "Monto ($)", bold = true),
            str("C", "Fecha", bold = true),
            str("D", "Tipo de Pago", bold = true)
        )
        var hayAbonos = false
        ec.creditos.forEach { c ->
            c.abonos.forEach { a ->
                hayAbonos = true
                row(str("A", "#${c.id}"), num("B", a.monto), str("C", a.fecha), str("D", a.tipoNombre))
            }
        }
        if (!hayAbonos) row(str("A", "Sin abonos registrados."))

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""

    private fun rootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Estado de Cuenta" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""

    private fun styles() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs></styleSheet>"""
}
