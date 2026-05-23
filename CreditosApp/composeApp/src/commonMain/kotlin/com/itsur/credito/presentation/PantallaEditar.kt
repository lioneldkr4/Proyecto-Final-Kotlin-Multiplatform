package com.itsur.credito.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itsur.credito.domain.model.Cliente

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
