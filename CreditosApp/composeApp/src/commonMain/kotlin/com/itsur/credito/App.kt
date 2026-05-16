package com.itsur.credito

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itsur.credito.data.PdfGenerator
import com.itsur.credito.db.Abono
import com.itsur.credito.db.AppDatabase
import com.itsur.credito.db.Cliente
import com.itsur.credito.db.Credito
import com.itsur.credito.presentation.AccionUI
import com.itsur.credito.presentation.AppViewModel
import com.itsur.credito.presentation.Pantalla

@Composable
fun App(database: AppDatabase, pdfGenerator: PdfGenerator) {
    val viewModel = viewModel { AppViewModel(database, pdfGenerator) }
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (uiState.pantalla) {
                Pantalla.Inicio -> PantallaCobranza(
                    clientes = uiState.clientes,
                    onBuscar = viewModel::buscarClientes,
                    onVerMasClick = viewModel::navegarADetalles,
                    onNuevoClienteClick = viewModel::navegarAAgregarCliente
                )
                Pantalla.Detalles -> {
                    uiState.clienteSeleccionado?.let { cliente ->
                        PantallaDetalles(
                            cliente = cliente,
                            creditoActual = uiState.creditoActual,
                            abonos = uiState.abonos,
                            creditosNuevos = uiState.creditosNuevos,
                            onVolver = viewModel::volverAInicio,
                            onImprimirPdf = viewModel::generarPdf,
                            onRegistrarAbono = viewModel::registrarAbono,
                            onRegistrarCredito = viewModel::registrarNuevoCredito
                        )
                    }
                }
                Pantalla.AgregarCliente -> PantallaAgregar(
                    onVolver = viewModel::volverAInicio,
                    onGuardar = viewModel::agregarCliente
                )
            }
        }
    }
}

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
                                "Límite Autorizado: $${cliente.limite_credito}",
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

@Composable
fun PantallaDetalles(
    cliente: Cliente,
    creditoActual: Credito?,
    abonos: List<Abono>,
    creditosNuevos: List<AccionUI>,
    onVolver: () -> Unit,
    onImprimirPdf: () -> Unit,
    onRegistrarAbono: (Double) -> Unit,
    onRegistrarCredito: (Double) -> Unit
) {
    var mostrarDialogoAbono by remember { mutableStateOf(false) }
    var montoAbono by remember { mutableStateOf("") }
    var mostrarDialogoCredito by remember { mutableStateOf(false) }
    var montoCredito by remember { mutableStateOf("") }

    val accionesList = remember(abonos, creditosNuevos) {
        abonos.map { AccionUI(tipo = "Abono", monto = it.monto_abonado, detalle = "Fecha: ${it.fecha}") } +
                creditosNuevos
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver) { Text("<- Regresar") }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Detalles del Cliente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                val creditoDisponible = creditoActual?.saldo_pendiente ?: cliente.limite_credito
                Text(
                    text = "Límite de crédito: $$creditoDisponible",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onImprimirPdf) { Text("Generar PDF") }
            Button(onClick = { mostrarDialogoAbono = true }) { Text("Abonar") }
            Button(onClick = { mostrarDialogoCredito = true }) { Text("Nuevo Crédito") }
        }

        if (accionesList.isNotEmpty()) {
            Text(
                "Acciones realizadas:",
                modifier = Modifier.padding(top = 16.dp),
                fontWeight = FontWeight.SemiBold
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
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
        }

        if (mostrarDialogoAbono) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoAbono = false },
                title = { Text("Registrar Abono") },
                text = {
                    OutlinedTextField(
                        value = montoAbono,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) montoAbono = it
                        },
                        label = { Text("Monto del abono ($)") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        montoAbono.toDoubleOrNull()?.let { monto ->
                            onRegistrarAbono(monto)
                            mostrarDialogoAbono = false
                            montoAbono = ""
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoAbono = false; montoAbono = "" }) {
                        Text("Cancelar")
                    }
                }
            )
        }

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
