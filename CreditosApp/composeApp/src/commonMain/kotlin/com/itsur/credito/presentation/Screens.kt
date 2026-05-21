package com.itsur.credito.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itsur.credito.data.generarQrImageBitmap
import com.itsur.credito.domain.model.Abono
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.Credito
import com.itsur.credito.domain.model.EstadoCredito
import com.itsur.credito.domain.model.TipoAbono

@Composable
fun PantallaCobranza(
    clientes: List<Cliente>,
    onBuscar: (String) -> Unit,
    onVerMasClick: (Cliente) -> Unit,
    onNuevoClienteClick: () -> Unit
) {
    var nombreBusqueda by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Gestión de Créditos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nombreBusqueda,
            onValueChange = { nombreBusqueda = it },
            label = { Text("Nombre del cliente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onBuscar(nombreBusqueda) },
                modifier = Modifier.weight(1f)
            ) { Text("Buscar") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNuevoClienteClick,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Nuevo Cliente")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (clientes.isEmpty()) {
            Text("No se encontraron clientes.", color = Color.Gray)
        } else {
            Text("Clientes:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clientes) { cliente ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Nombre: ${cliente.nombre}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Límite Autorizado: $${cliente.limiteCredito}",
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { onVerMasClick(cliente) },
                                modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                            ) {
                                Text("Ver cuenta ->")
                            }
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
    creditoActual: Credito?,
    abonos: List<Abono>,
    creditosNuevos: List<AccionUI>,
    tiposAbono: List<TipoAbono>,
    estadosCredito: List<EstadoCredito>,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onImprimirPdf: () -> Unit,
    onGenerarExcel: () -> Unit,
    onRegistrarAbono: (Double, Long) -> Unit,
    onRegistrarCredito: (Double) -> Unit,
    onEliminar: () -> Unit
) {
    var mostrarDialogoAbono by remember { mutableStateOf(false) }
    var montoAbono by remember { mutableStateOf("") }
    var errorMonto by remember { mutableStateOf(false) }

    var mostrarDialogoCredito by remember { mutableStateOf(false) }
    var montoCredito by remember { mutableStateOf("") }

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarQr by remember { mutableStateOf(false) }

    val creditoLiquidado = creditoActual?.estadoId == 2L

    val accionesList = remember(abonos, creditosNuevos) {
        abonos.map { AccionUI(tipo = "Abono", monto = it.montoAbonado, detalle = "Fecha: ${it.fecha}") } +
                creditosNuevos
    }

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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = cliente.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Teléfono: ${cliente.telefono ?: "No proporcionado"}")
                Text("Dirección: ${cliente.direccion ?: "No proporcionada"}")
                Spacer(modifier = Modifier.height(8.dp))

                val estadoNombre = estadosCredito.find { it.id == creditoActual?.estadoId }?.nombre
                val estadoColor = when (creditoActual?.estadoId) {
                    2L -> Color(0xFF4CAF50)
                    3L -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }

                if (estadoNombre != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = estadoColor)) {
                        Text(
                            text = estadoNombre,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (!creditoLiquidado) {
                    val saldo = creditoActual?.saldoPendiente ?: cliente.limiteCredito
                    Text(
                        text = "Saldo pendiente: $$saldo",
                        color = estadoColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Botones de acción financiera ─────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!creditoLiquidado) {
                Button(onClick = { mostrarDialogoAbono = true }) { Text("Abonar") }
            }
            Button(onClick = { mostrarDialogoCredito = true }) { Text("Nuevo Crédito") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Botones de exportación / QR ──────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onImprimirPdf) { Text("PDF") }
            OutlinedButton(onClick = onGenerarExcel) { Text("Excel") }
            OutlinedButton(onClick = { mostrarQr = true }) { Text("Ver QR") }
        }

        // Badge de crédito completado
        if (creditoLiquidado) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.12f))
            ) {
                Text(
                    text = "✓ Este cliente ha liquidado su deuda por completo",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Historial de acciones ────────────────────────────────────────────
        if (accionesList.isNotEmpty()) {
            Text(
                "Acciones realizadas:",
                modifier = Modifier.padding(top = 16.dp),
                fontWeight = FontWeight.SemiBold
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .weight(1f)
            ) {
                items(accionesList) { accion ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${accion.tipo}: $${accion.monto}", fontWeight = FontWeight.Bold)
                            Text(text = accion.detalle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
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
        val saldoDisponible = creditoActual?.saldoPendiente ?: cliente.limiteCredito
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
                            text = "El monto supera el saldo pendiente ($$${"%.2f".format(saldoDisponible)})",
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
                        monto == null || monto <= 0 -> { /* campo vacío o inválido */ }
                        monto > saldoDisponible -> errorMonto = true
                        else -> {
                            onRegistrarAbono(monto, tipoAbonoSeleccionado)
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
            onDismissRequest = { mostrarDialogoCredito = false },
            title = { Text("Registrar Nuevo Crédito") },
            text = {
                OutlinedTextField(
                    value = montoCredito,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) montoCredito = it
                    },
                    label = { Text("Monto del nuevo crédito ($)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val monto = montoCredito.toDoubleOrNull()
                    if (monto != null && monto > 0) {
                        onRegistrarCredito(monto)
                        mostrarDialogoCredito = false
                        montoCredito = ""
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCredito = false; montoCredito = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Diálogo: Código QR del cliente ───────────────────────────────────────
    if (mostrarQr) {
        val qrTexto = buildString {
            append("CREDITO_APP\n")
            append("ID: ${cliente.id}\n")
            append("Nombre: ${cliente.nombre}\n")
            append("Límite: $${cliente.limiteCredito}\n")
            append("Saldo: $${creditoActual?.saldoPendiente ?: cliente.limiteCredito}")
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
