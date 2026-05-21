package com.itsur.credito.data

import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual class ExcelGenerator {

    actual fun generarExcel(
        nombre: String,
        telefono: String?,
        direccion: String?,
        limiteCredito: Double,
        saldoPendiente: Double?,
        acciones: List<AccionPdf>
    ) {
        val bytes = buildXlsxBytes(nombre, telefono, direccion, limiteCredito, saldoPendiente, acciones)
        val docsDir = File(System.getProperty("user.home"), "Documents").also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safe = nombre.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(docsDir, "credito_${safe}_$ts.xlsx")
        file.writeBytes(bytes)
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file)
    }

    private fun buildXlsxBytes(
        nombre: String, telefono: String?, direccion: String?,
        limiteCredito: Double, saldoPendiente: Double?, acciones: List<AccionPdf>
    ): ByteArray {
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
            add("xl/worksheets/sheet1.xml",
                buildSheetXml(nombre, telefono, direccion, limiteCredito, saldoPendiente, acciones))
        }
        return out.toByteArray()
    }

    private fun buildSheetXml(
        nombre: String, telefono: String?, direccion: String?,
        limiteCredito: Double, saldoPendiente: Double?, acciones: List<AccionPdf>
    ): String {
        val sb = StringBuilder()
        var r = 1

        fun String.esc() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        fun Double.fmt() = String.format(Locale.US, "%.2f", this)

        fun strCell(col: String, v: String, bold: Boolean = false): String {
            val s = if (bold) " s=\"1\"" else ""
            return "<c r=\"$col$r\" t=\"inlineStr\"$s><is><t>${v.esc()}</t></is></c>"
        }
        fun numCell(col: String, v: Double) = "<c r=\"$col$r\"><v>${v.fmt()}</v></c>"
        fun row(vararg cells: String) { sb.append("<row r=\"$r\">${cells.joinToString("")}</row>"); r++ }
        fun emptyRow() { sb.append("<row r=\"$r\"/>"); r++ }

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")

        row(strCell("A", "Resumen de Crédito", bold = true))
        emptyRow()
        row(strCell("A", "Campo", bold = true), strCell("B", "Valor", bold = true))
        row(strCell("A", "Nombre"), strCell("B", nombre))
        telefono?.let { row(strCell("A", "Teléfono"), strCell("B", it)) }
        direccion?.let { row(strCell("A", "Dirección"), strCell("B", it)) }
        row(strCell("A", "Límite de Crédito"), numCell("B", limiteCredito))
        row(strCell("A", "Saldo Pendiente"), numCell("B", saldoPendiente ?: limiteCredito))
        emptyRow()
        row(strCell("A", "Historial de Acciones", bold = true))
        row(strCell("A", "Tipo", bold = true), strCell("B", "Monto ($)", bold = true), strCell("C", "Fecha", bold = true))

        if (acciones.isEmpty()) {
            row(strCell("A", "Sin acciones registradas."))
        } else {
            acciones.forEach { a -> row(strCell("A", a.tipo), numCell("B", a.monto), strCell("C", a.fecha)) }
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""

    private fun rootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Estado de Cuenta" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""

    private fun styles() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs></styleSheet>"""
}
