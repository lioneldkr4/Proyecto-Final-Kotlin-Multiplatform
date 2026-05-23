package com.itsur.credito.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itsur.credito.domain.model.Cliente
import com.itsur.credito.domain.model.DashboardStats

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
