package com.itsur.credito.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itsur.credito.data.generarQrImageBitmap
import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.DashboardStats
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono

@Composable
fun PantallaCobranza(
    clientes: List<Cliente>,
    dashboardStats: DashboardStats?,
    onBuscar: (String) -> Unit,
    onVerMasClick: (Cliente) -> Unit,
    onNuevoClienteClick: () -> Unit
) {
    var nombreBusqueda by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Encabezado ───────────────────────────────────────────────────────
        item {
            Text(
                "Gestión de Créditos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Dashboard ────────────────────────────────────────────────────────
        if (dashboardStats != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TarjetaStat(
                        titulo = "Clientes",
                        valor = "${dashboardStats.totalClientes}",
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaStat(
                        titulo = "Activos",
                        valor = "${dashboardStats.creditosActivos}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaStat(
                        titulo = "Vencidos",
                        valor = "${dashboardStats.creditosVencidos}",
                        color = if (dashboardStats.creditosVencidos > 0)
                            MaterialTheme.colorScheme.error else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TarjetaStat(
                        titulo = "Total prestado",
                        valor = "$${"%,.0f".format(dashboardStats.totalPrestado)}",
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaStat(
                        titulo = "Por cobrar",
                        valor = "$${"%,.0f".format(dashboardStats.totalPendiente)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (dashboardStats.topDeudores.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Top deudores",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(12.dp))
                            GraficaBarrasDeudores(deudores = dashboardStats.topDeudores)
                        }
                    }
                }
            }
            item { HorizontalDivider() }
        }

        // ── Búsqueda y nuevo cliente ─────────────────────────────────────────
        item {
            OutlinedTextField(
                value = nombreBusqueda,
                onValueChange = { nombreBusqueda = it },
                label = { Text("Buscar cliente") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onBuscar(nombreBusqueda) },
                    modifier = Modifier.weight(1f)
                ) { Text("Buscar") }
                Button(
                    onClick = onNuevoClienteClick,
                    modifier = Modifier.weight(1f)
                ) { Text("Nuevo Cliente") }
            }
        }

        // ── Lista de clientes ─────────────────────────────────────────────────
        if (clientes.isEmpty()) {
            item { Text("No se encontraron clientes.", color = Color.Gray) }
        } else {
            item {
                Text(
                    "Clientes (${clientes.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            items(clientes) { cliente ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            cliente.nombre,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Límite: $${"%,.2f".format(cliente.limiteCredito)}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = { onVerMasClick(cliente) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Ver cuenta ->")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalles(
    cliente: Cliente,
    creditos: List<Credito>,
    abonos: List<Abono>,
    tiposAbono: List<TipoAbono>,
    estadosCredito: List<EstadoCredito>,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onImprimirPdf: () -> Unit,
    onGenerarExcel: () -> Unit,
    onRegistrarAbono: (creditoId: Long, monto: Double, tipoId: Long) -> Unit,
    onRegistrarCredito: (monto: Double, fechaVencimiento: String) -> Unit,
    onLiquidarCredito: (creditoId: Long) -> Unit,
    onEliminar: () -> Unit
) {
    var mostrarDialogoAbono by remember { mutableStateOf(false) }
    var creditoParaAbonar by remember { mutableStateOf<Long?>(null) }
    var montoAbono by remember { mutableStateOf("") }
    var errorMonto by remember { mutableStateOf(false) }

    var mostrarDialogoCredito by remember { mutableStateOf(false) }
    var montoCredito by remember { mutableStateOf("") }
    var errorCreditoDisponible by remember { mutableStateOf(false) }
    var fechaVencimientoStr by remember { mutableStateOf("") }
    var errorFechaVencimiento by remember { mutableStateOf(false) }

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarQr by remember { mutableStateOf(false) }

    val creditoUsado = remember(creditos) {
        creditos.filter { it.estadoId == 1L }.sumOf { it.saldoPendiente }
    }
    val creditoDisponible = cliente.limiteCredito - creditoUsado

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

        // ── Encabezado ──────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver) { Text("<- Regresar") }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Detalles del Cliente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Tarjeta de info del cliente ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cliente.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Teléfono: ${cliente.telefono ?: "No proporcionado"}")
                    Text("Dirección: ${cliente.direccion ?: "No proporcionada"}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Límite", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("$${"%,.2f".format(cliente.limiteCredito)}", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Disponible", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                "$${"%,.2f".format(creditoDisponible)}",
                                fontWeight = FontWeight.Bold,
                                color = if (creditoDisponible > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                        Column {
                            Text("Utilizado", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                "$${"%,.2f".format(creditoUsado)}",
                                fontWeight = FontWeight.Bold,
                                color = if (creditoUsado > 0) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                GraficaDona(
                    utilizado = creditoUsado,
                    limite = cliente.limiteCredito,
                    modifier = Modifier.size(110.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Botón Nuevo Crédito ──────────────────────────────────────────────
        Button(
            onClick = { mostrarDialogoCredito = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = creditoDisponible > 0
        ) {
            Text("+ Nuevo Crédito")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Botones de exportación / QR ──────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onImprimirPdf) { Text("PDF") }
            OutlinedButton(onClick = onGenerarExcel) { Text("Excel") }
            OutlinedButton(onClick = { mostrarQr = true }) { Text("Ver QR") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Lista de créditos ────────────────────────────────────────────────
        if (creditos.isEmpty()) {
            Text("Sin créditos registrados.", color = Color.Gray)
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Text("Créditos:", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(creditos) { credito ->
                    TarjetaCredito(
                        credito = credito,
                        abonos = abonos.filter { it.creditoId == credito.id },
                        tiposAbono = tiposAbono,
                        estadosCredito = estadosCredito,
                        onAbonar = {
                            creditoParaAbonar = credito.id
                            montoAbono = ""
                            errorMonto = false
                            mostrarDialogoAbono = true
                        },
                        onLiquidar = { onLiquidarCredito(credito.id) }
                    )
                }
            }
        }

        // ── Botones de gestión de cliente ────────────────────────────────────
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEditar,
                modifier = Modifier.weight(1f)
            ) {
                Text("Editar Cliente")
            }
            OutlinedButton(
                onClick = { mostrarDialogoEliminar = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar Cliente")
            }
        }
    }

    // ── Diálogo: Registrar Abono ─────────────────────────────────────────────
    if (mostrarDialogoAbono) {
        val creditoActivo = creditos.find { it.id == creditoParaAbonar }
        val saldoDisponible = creditoActivo?.saldoPendiente ?: 0.0
        var tipoAbonoSeleccionado by remember { mutableStateOf(tiposAbono.firstOrNull()?.id ?: 1L) }
        var expandidoTipoAbono by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                mostrarDialogoAbono = false
                montoAbono = ""
                errorMonto = false
            },
            title = { Text("Registrar Abono") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Saldo pendiente: $${"%,.2f".format(saldoDisponible)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = montoAbono,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                montoAbono = it
                                errorMonto = false
                            }
                        },
                        label = { Text("Monto del abono ($)") },
                        singleLine = true,
                        isError = errorMonto
                    )
                    if (errorMonto) {
                        Text(
                            text = "El monto supera el saldo pendiente ($${"%,.2f".format(saldoDisponible)})",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (tiposAbono.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandidoTipoAbono,
                            onExpandedChange = { expandidoTipoAbono = it }
                        ) {
                            OutlinedTextField(
                                value = tiposAbono.find { it.id == tipoAbonoSeleccionado }?.nombre ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de abono") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoTipoAbono) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandidoTipoAbono,
                                onDismissRequest = { expandidoTipoAbono = false }
                            ) {
                                tiposAbono.forEach { tipo ->
                                    DropdownMenuItem(
                                        text = { Text(tipo.nombre) },
                                        onClick = {
                                            tipoAbonoSeleccionado = tipo.id
                                            expandidoTipoAbono = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val monto = montoAbono.toDoubleOrNull()
                    when {
                        monto == null || monto <= 0 -> {}
                        monto > saldoDisponible -> errorMonto = true
                        else -> {
                            creditoParaAbonar?.let { id ->
                                onRegistrarAbono(id, monto, tipoAbonoSeleccionado)
                            }
                            mostrarDialogoAbono = false
                            montoAbono = ""
                            errorMonto = false
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoAbono = false
                    montoAbono = ""
                    errorMonto = false
                }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: Nuevo Crédito ───────────────────────────────────────────────
    if (mostrarDialogoCredito) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoCredito = false
                montoCredito = ""
                errorCreditoDisponible = false
                fechaVencimientoStr = ""
                errorFechaVencimiento = false
            },
            title = { Text("Nuevo Crédito") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Crédito disponible: $${"%,.2f".format(creditoDisponible)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = montoCredito,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                montoCredito = it
                                errorCreditoDisponible = false
                            }
                        },
                        label = { Text("Monto a disponer ($)") },
                        singleLine = true,
                        isError = errorCreditoDisponible
                    )
                    if (errorCreditoDisponible) {
                        Text(
                            text = "El monto supera el crédito disponible ($${"%,.2f".format(creditoDisponible)})",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = fechaVencimientoStr,
                        onValueChange = {
                            if (it.length <= 10) {
                                fechaVencimientoStr = it
                                errorFechaVencimiento = false
                            }
                        },
                        label = { Text("Fecha de vencimiento (Opcional)") },
                        placeholder = { Text("dd/MM/yyyy") },
                        singleLine = true,
                        isError = errorFechaVencimiento
                    )
                    if (errorFechaVencimiento) {
                        Text(
                            text = "Formato inválido. Use dd/MM/yyyy.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val monto = montoCredito.toDoubleOrNull()
                    val fechaValida = fechaVencimientoStr.isBlank() ||
                        fechaVencimientoStr.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))
                    when {
                        monto == null || monto <= 0 -> {}
                        monto > creditoDisponible -> errorCreditoDisponible = true
                        !fechaValida -> errorFechaVencimiento = true
                        else -> {
                            onRegistrarCredito(monto, fechaVencimientoStr)
                            mostrarDialogoCredito = false
                            montoCredito = ""
                            errorCreditoDisponible = false
                            fechaVencimientoStr = ""
                            errorFechaVencimiento = false
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoCredito = false
                    montoCredito = ""
                    errorCreditoDisponible = false
                    fechaVencimientoStr = ""
                    errorFechaVencimiento = false
                }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: Código QR del cliente ───────────────────────────────────────
    if (mostrarQr) {
        val qrTexto = buildString {
            append("CREDITO_APP\n")
            append("ID: ${cliente.id}\n")
            append("Nombre: ${cliente.nombre}\n")
            cliente.telefono?.let { append("Tel: $it\n") }
            cliente.direccion?.let { append("Dir: $it\n") }
            append("Limite: $${"%,.2f".format(cliente.limiteCredito)}\n")
            append("Utilizado: $${"%,.2f".format(creditoUsado)}\n")
            append("Disponible: $${"%,.2f".format(creditoDisponible)}\n")
            val activos = creditos.count { it.estadoId == 1L }
            val vencidos = creditos.count { it.estadoId == 3L }
            append("Creditos activos: $activos\n")
            if (vencidos > 0) append("Creditos vencidos: $vencidos\n")
            append("Total adeudado: $${"%,.2f".format(creditoUsado)}")
        }
        val qrBitmap: ImageBitmap = remember(cliente.id) { generarQrImageBitmap(qrTexto) }
        AlertDialog(
            onDismissRequest = { mostrarQr = false },
            title = { Text("Identificación del Cliente") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap,
                        contentDescription = "Código QR de ${cliente.nombre}",
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cliente.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${cliente.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarQr = false }) { Text("Cerrar") }
            }
        )
    }

    // ── Diálogo: Confirmar Eliminación ───────────────────────────────────────
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar cliente") },
            text = {
                Text("¿Eliminar a ${cliente.nombre}? Se borrará también su historial de créditos y abonos. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEliminar()
                        mostrarDialogoEliminar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TarjetaCredito(
    credito: Credito,
    abonos: List<Abono>,
    tiposAbono: List<TipoAbono>,
    estadosCredito: List<EstadoCredito>,
    onAbonar: () -> Unit,
    onLiquidar: () -> Unit
) {
    val estadoNombre = estadosCredito.find { it.id == credito.estadoId }?.nombre ?: ""
    val estadoColor = when (credito.estadoId) {
        2L -> Color(0xFF4CAF50)
        3L -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val activo = credito.estadoId == 1L
    val pagado = credito.montoPrestado - credito.saldoPendiente
    val progresoPago = if (credito.montoPrestado > 0)
        (pagado / credito.montoPrestado).toFloat().coerceIn(0f, 1f)
    else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (activo) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Crédito #${credito.id}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Card(colors = CardDefaults.cardColors(containerColor = estadoColor)) {
                    Text(
                        text = estadoNombre,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Monto", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("$${"%,.2f".format(credito.montoPrestado)}", fontWeight = FontWeight.SemiBold)
                }
                if (activo) {
                    Column {
                        Text("Pendiente", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            "$${"%,.2f".format(credito.saldoPendiente)}",
                            fontWeight = FontWeight.SemiBold,
                            color = estadoColor
                        )
                    }
                    Column {
                        Text("Pagado", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            "$${"%,.2f".format(credito.montoPrestado - credito.saldoPendiente)}",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Progreso de pago",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    "${"%.0f".format(progresoPago * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progresoPago >= 1f) Color(0xFF2E7D32) else estadoColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progresoPago },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            credito.fechaVencimiento?.let { isoFecha ->
                val partes = isoFecha.split("-")
                val fechaDisplay = if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else isoFecha
                val colorFecha = if (credito.estadoId == 3L) MaterialTheme.colorScheme.error else Color.Gray
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Vencimiento: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        fechaDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorFecha
                    )
                }
            }

            if (abonos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Abonos:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                abonos.forEach { abono ->
                    val tipoNombre = tiposAbono.find { it.id == abono.tipoId }?.nombre ?: ""
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$${"%,.2f".format(abono.montoAbonado)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            abono.fecha,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        if (tipoNombre.isNotBlank()) {
                            Text(
                                tipoNombre,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            if (activo) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAbonar, modifier = Modifier.weight(1f)) {
                        Text("Abonar")
                    }
                    OutlinedButton(
                        onClick = onLiquidar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Text("Liquidar")
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaEditar(
    cliente: Cliente,
    onVolver: () -> Unit,
    onGuardar: (nombre: String, telefono: String, direccion: String, limite: Double) -> Unit
) {
    var nombre by remember { mutableStateOf(cliente.nombre) }
    var telefono by remember { mutableStateOf(cliente.telefono ?: "") }
    var direccion by remember { mutableStateOf(cliente.direccion ?: "") }
    var limiteCredito by remember { mutableStateOf(cliente.limiteCredito.toString()) }
    var mostrarError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Editar Cliente", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it; mostrarError = false },
            label = { Text("Nombre completo *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono (Opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección (Opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = limiteCredito,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                    limiteCredito = it
                    mostrarError = false
                }
            },
            label = { Text("Límite de crédito ($) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (mostrarError) {
            Text(
                text = "Nombre y Límite son obligatorios.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    val limite = limiteCredito.toDoubleOrNull()
                    if (nombre.isNotBlank() && limite != null && limite > 0) {
                        onGuardar(nombre, telefono, direccion, limite)
                    } else {
                        mostrarError = true
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar")
            }
        }
    }
}

@Composable
fun PantallaAgregar(
    onVolver: () -> Unit,
    onGuardar: (nombre: String, telefono: String, direccion: String, limite: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var limiteCredito by remember { mutableStateOf("") }
    var mostrarError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Nuevo Cliente", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it; mostrarError = false },
            label = { Text("Nombre completo *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono (Opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección (Opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = limiteCredito,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                    limiteCredito = it
                    mostrarError = false
                }
            },
            label = { Text("Límite de crédito ($) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (mostrarError) {
            Text(
                text = "Nombre y Límite son obligatorios.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    val limite = limiteCredito.toDoubleOrNull()
                    if (nombre.isNotBlank() && limite != null && limite > 0) {
                        onGuardar(nombre, telefono, direccion, limite)
                    } else {
                        mostrarError = true
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun GraficaDona(
    utilizado: Double,
    limite: Double,
    modifier: Modifier = Modifier
) {
    val progreso = if (limite > 0) (utilizado / limite).toFloat().coerceIn(0f, 1f) else 0f

    val colorUsado = when {
        progreso >= 0.9f -> Color(0xFFE53935)
        progreso >= 0.7f -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.primary
    }
    val colorDisponible = Color(0xFF4CAF50)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = size.minDimension * 0.18f
            val radio = size.minDimension / 2f - grosor / 2f
            val centro = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(centro.x - radio, centro.y - radio)
            val arcSize = Size(radio * 2f, radio * 2f)
            val estilo = Stroke(width = grosor, cap = StrokeCap.Butt)

            // Fondo: disponible (arco completo verde)
            drawArc(
                color = colorDisponible,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = estilo
            )

            // Encima: utilizado
            if (progreso > 0f) {
                drawArc(
                    color = colorUsado,
                    startAngle = -90f,
                    sweepAngle = 360f * progreso,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = estilo
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${"%.0f".format(progreso * 100)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (progreso == 0f) colorDisponible else colorUsado
            )
            Text(
                text = "usado",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun TarjetaStat(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GraficaBarrasDeudores(
    deudores: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val max = deudores.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        deudores.forEach { (nombre, deuda) ->
            val progreso = (deuda / max).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (nombre.length > 18) nombre.take(17) + "…" else nombre,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    Text(
                        text = "$${"%,.2f".format(deuda)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(color = trackColor, size = size, cornerRadius = r)
                    if (progreso > 0f) {
                        drawRoundRect(
                            color = barColor,
                            size = Size(size.width * progreso, size.height),
                            cornerRadius = r
                        )
                    }
                }
            }
        }
    }
}
