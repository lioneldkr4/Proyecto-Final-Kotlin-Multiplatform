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
import com.itsur.credito.db.AppDatabase
// --- IMPORTS CORREGIDOS PARA COMPATIBILIDAD DESDE API 1 ---
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class Pantalla {
    Inicio, Detalles, AgregarCliente
}

// Estructura para unificar Abonos y Créditos en la interfaz
data class AccionUI(
    val tipo: String,
    val monto: Double,
    val detalle: String
)

@Composable
fun App(database: AppDatabase) {
    var pantallaActual by remember { mutableStateOf(Pantalla.Inicio) }
    var clienteSeleccionado by remember { mutableStateOf<com.itsur.credito.db.Cliente?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (pantallaActual) {
                Pantalla.Inicio -> PantallaCobranza(
                    database = database,
                    onVerMasClick = { cliente ->
                        clienteSeleccionado = cliente
                        pantallaActual = Pantalla.Detalles
                    },
                    onNuevoClienteClick = { pantallaActual = Pantalla.AgregarCliente }

                )
                Pantalla.Detalles -> {
                    clienteSeleccionado?.let { cliente ->
                        PantallaDetalles(
                            cliente = cliente,
                            database = database,
                            onVolver = {
                                pantallaActual = Pantalla.Inicio
                                clienteSeleccionado = null
                            },
                            onImprimirPdf = { /* Pendiente */ }
                        )
                    }
                }
                Pantalla.AgregarCliente -> PantallaAgregar(
                    onVolver = {
                        pantallaActual = Pantalla.Inicio
                    },
                    onGuardar = { nombre, telefono, direccion, limite ->
                        try {
                            database.appDatabaseQueries.insertarCliente(
                                nombre = nombre,
                                telefono = telefono.ifBlank { null },
                                direccion = direccion.ifBlank { null },
                                limite_credito = limite
                            )
                            println("Cliente guardado en SQLite exitosamente")
                        } catch (e: Exception) {
                            println("Error al guardar: ${e.message}")
                        }
                        pantallaActual = Pantalla.Inicio
                    }
                )
            }
        }
    }
}

@Composable
fun PantallaCobranza(
    database: com.itsur.credito.db.AppDatabase,
    onVerMasClick: (com.itsur.credito.db.Cliente) -> Unit,
    onNuevoClienteClick: () -> Unit
) {
    val queries = database.appDatabaseQueries
    var nombreBusqueda by remember { mutableStateOf("") }
    var clientesEncontrados by remember { mutableStateOf<List<com.itsur.credito.db.Cliente>>(emptyList()) }
    var busquedaRealizada by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            clientesEncontrados = queries.obtenerTodosLosClientes().executeAsList()
            busquedaRealizada = true
        } catch (e: Exception) {
            println("Aún no hay datos para cargar")
        }
    }

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

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                try {
                    clientesEncontrados = queries.buscarClientesGenerales("%$nombreBusqueda%").executeAsList()
                    busquedaRealizada = true
                } catch (e: Exception) {
                    println("Error al buscar: ${e.message}")
                }
            }, modifier = Modifier.weight(1f)) { Text("Buscar") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNuevoClienteClick,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Nuevo Cliente")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (busquedaRealizada) {
            Text("Resultados de la búsqueda:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (clientesEncontrados.isEmpty()) {
                Text("No se encontraron clientes con ese nombre.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clientesEncontrados) { cliente ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Nombre: ${cliente.nombre}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Límite Autorizado: $${cliente.limite_credito}", color = MaterialTheme.colorScheme.primary)

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
        } else {
            Text("Ingresa un nombre y presiona Buscar para ver a los clientes.", color = Color.Gray)
        }
    }
}

@Composable
fun PantallaDetalles(
    cliente: com.itsur.credito.db.Cliente,
    database: com.itsur.credito.db.AppDatabase,
    onVolver: () -> Unit,
    onImprimirPdf: () -> Unit
) {
    val queries = database.appDatabaseQueries
    var mostrarDialogoAbono by remember { mutableStateOf(false) }
    var montoAbono by remember { mutableStateOf("") }

    var mostrarDialogoCredito by remember { mutableStateOf(false) }
    var montoCredito by remember { mutableStateOf("") }

    var creditoActual by remember { mutableStateOf<com.itsur.credito.db.Credito?>(null) }

    var abonosList by remember { mutableStateOf(listOf<com.itsur.credito.db.Abono>()) }
    var creditosNuevosList by remember { mutableStateOf(listOf<AccionUI>()) }

    LaunchedEffect(cliente.id) {
        creditoActual = queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull()
        abonosList = queries.obtenerAbonosPorCliente(cliente.id).executeAsList()
    }

    val accionesList = remember(abonosList, creditosNuevosList) {
        val abonosUI = abonosList.map {
            AccionUI(tipo = "Abono", monto = it.monto_abonado, detalle = "Fecha: ${it.fecha}")
        }
        abonosUI + creditosNuevosList
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
                Text(text = cliente.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            Text("Acciones realizadas:", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.SemiBold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = montoAbono,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    montoAbono = it
                                }
                            },
                            label = { Text("Monto del abono ($)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val monto = montoAbono.toDoubleOrNull()
                            if (monto != null) {
                                try {
                                    var credito = queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull()

                                    if (credito == null) {
                                        queries.insertarCredito(
                                            cliente.id,
                                            cliente.limite_credito,
                                            cliente.limite_credito
                                        )
                                        credito = queries.obtenerCreditoPorCliente(cliente.id).executeAsOne()
                                    }

                                    // --- CORRECCIÓN DE FECHA COMPATIBLE CON API 24 ---
                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    val fechaDispositivo = sdf.format(Date())

                                    queries.insertarAbono(credito.id, monto, fechaDispositivo)
                                    queries.actualizarSaldoPendiente(monto, credito.id)

                                    creditoActual = queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull()
                                    abonosList = queries.obtenerAbonosPorCliente(cliente.id).executeAsList()

                                    mostrarDialogoAbono = false
                                    montoAbono = ""
                                } catch (e: Exception) {
                                    println("Error al guardar abono: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Text("Guardar")
                    }
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = montoCredito,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    montoCredito = it
                                }
                            },
                            label = { Text("Monto del nuevo crédito ($)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val monto = montoCredito.toDoubleOrNull()
                            if (monto != null && monto > 0) {
                                try {
                                    var credito = queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull()

                                    if (credito == null) {
                                        queries.insertarCredito(cliente.id, monto, monto)
                                    } else {
                                        queries.acumularCredito(monto, monto, cliente.id)
                                    }

                                    creditoActual = queries.obtenerCreditoPorCliente(cliente.id).executeAsOneOrNull()

                                    // --- CORRECCIÓN DE FECHA COMPATIBLE CON API 24 ---
                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    val fechaDispositivo = sdf.format(Date())

                                    creditosNuevosList = creditosNuevosList + AccionUI(
                                        tipo = "Nuevo Crédito",
                                        monto = monto,
                                        detalle = "Fecha: $fechaDispositivo"
                                    )

                                    mostrarDialogoCredito = false
                                    montoCredito = ""
                                } catch (e: Exception) {
                                    println("Error al añadir crédito: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Text("Guardar")
                    }
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