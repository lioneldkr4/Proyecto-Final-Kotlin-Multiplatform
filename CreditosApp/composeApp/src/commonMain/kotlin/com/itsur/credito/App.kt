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

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            PantallaCobranza()
        }
    }
}

@Composable
fun PantallaCobranza() {
    var nombreBusqueda by remember { mutableStateOf("") }

    // Datos de prueba temporales (Mock) para visualizar el diseño
    val creditoDisponible = 5000.00
    val deudaActual = 1250.50
    val listaAbonos = listOf(
        "10/05/2026 - Abono de $500.00",
        "12/05/2026 - Abono de $250.00"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Gestión de Créditos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Buscador por nombre
        OutlinedTextField(
            value = nombreBusqueda,
            onValueChange = { nombreBusqueda = it },
            label = { Text("Nombre del cliente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { /* Aquí conectaremos la búsqueda a la BD */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            Text("Buscar Cliente")
        }

        // 2. Tarjeta de Saldos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Resumen de Cuenta", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Crédito Disponible:")
                    Text("$$creditoDisponible", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Deuda Actual:")
                    Text("$$deudaActual", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) // Rojo para deuda
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Lista de Abonos
        Text(
            text = "Historial de Abonos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listaAbonos) { abono ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = abono,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}