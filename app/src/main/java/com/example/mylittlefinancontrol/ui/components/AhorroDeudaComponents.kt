package com.example.mylittlefinancontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import java.text.NumberFormat
import java.util.Locale

private val formatMoneda: NumberFormat
    get() = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

@Composable
fun AhorroCardItem(
    ahorro: Ahorro,
    onEditarClick: (Ahorro) -> Unit,
    onEliminarClick: (Ahorro) -> Unit,
    onAbonarClick: (Ahorro) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    val porcentaje = remember(ahorro.montoActual, ahorro.montoObjetivo) {
        if (ahorro.montoObjetivo > 0) {
            ((ahorro.montoActual / ahorro.montoObjetivo) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    val catLower = ahorro.categoria.lowercase()
    val (iconRes, iconBg, iconTint) = when {
        catLower.contains("emerg") || catLower.contains("salud") || catLower.contains("segur") -> Triple(
            R.drawable.ic_shield,
            colors.primaryEmerald50,
            colors.primaryEmerald
        )
        catLower.contains("veh") || catLower.contains("auto") || catLower.contains("carro") || catLower.contains("moto") -> Triple(
            R.drawable.ic_directions_car,
            colors.surfaceContainer,
            colors.textPrimary
        )
        catLower.contains("viaj") || catLower.contains("vaca") || catLower.contains("vuelo") -> Triple(
            R.drawable.ic_cloud,
            colors.surfaceContainerHigh,
            colors.textVariant
        )
        catLower.contains("viv") || catLower.contains("casa") || catLower.contains("hogar") || catLower.contains("depa") -> Triple(
            R.drawable.ic_cottage,
            colors.surfaceContainer,
            colors.textPrimary
        )
        catLower.contains("tec") || catLower.contains("pc") || catLower.contains("cel") || catLower.contains("comp") -> Triple(
            R.drawable.ic_router,
            colors.surfaceContainerHigh,
            colors.textVariant
        )
        else -> Triple(
            R.drawable.ic_savings,
            colors.primaryEmerald50,
            colors.primaryEmerald
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = colors.surfaceContainer,
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Text(
                                text = ahorro.categoria.uppercase(),
                                color = colors.textVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = if (ahorro.fechaMeta.isNotEmpty()) "Meta: ${ahorro.fechaMeta}" else "Sin fecha límite",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    Text(
                        text = ahorro.titulo,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$porcentaje%",
                        color = colors.primaryEmerald,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    IconButton(
                        onClick = { onEditarClick(ahorro) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Editar meta",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onEliminarClick(ahorro) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Eliminar meta",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { porcentaje / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(percent = 50)),
                color = colors.primaryEmerald,
                trackColor = colors.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatMoneda.format(ahorro.montoActual),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "de ${formatMoneda.format(ahorro.montoObjetivo)}",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                FilledTonalButton(
                    onClick = { onAbonarClick(ahorro) },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.surfaceContainerLow,
                        contentColor = colors.primaryEmerald
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Abonar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DeudaCardItem(
    deuda: Deuda,
    onEditarClick: (Deuda) -> Unit,
    onEliminarClick: (Deuda) -> Unit,
    onAbonarClick: (Deuda) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    val restante = (deuda.totalDeuda - deuda.totalPagado).coerceAtLeast(0.0)
    val esPagadaCompletamente = restante <= 0.0 || deuda.totalPagado >= deuda.totalDeuda
    val progresoPct = if (deuda.totalDeuda > 0) {
        ((deuda.totalPagado / deuda.totalDeuda) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }

    val infoInteres = if (deuda.tasaInteres > 0) " · ${deuda.tasaInteres}% ${deuda.tipoInteres}" else ""
    val infoCuota = when {
        deuda.valorCuota > 0 -> " (${deuda.cuotas}x · ${formatMoneda.format(deuda.valorCuota)}/mes)"
        deuda.cuotas > 0 -> " (${deuda.cuotas} cuotas)"
        else -> ""
    }
    val subtituloTipo = "${deuda.tipo}$infoInteres$infoCuota"

    val tipoLower = deuda.tipo.lowercase()
    val (iconRes, iconBg, iconTint) = when {
        tipoLower.contains("tarjeta") -> Triple(
            R.drawable.ic_credit_card,
            colors.surfaceContainerLow,
            colors.primaryEmerald
        )
        tipoLower.contains("auto") || tipoLower.contains("vehic") -> Triple(
            R.drawable.ic_directions_car,
            colors.surfaceContainerLow,
            colors.primaryEmerald
        )
        tipoLower.contains("personal") -> Triple(
            R.drawable.ic_person,
            colors.surfaceContainer,
            colors.textPrimary
        )
        else -> Triple(
            R.drawable.ic_account_balance_wallet,
            colors.surfaceContainerHigh,
            colors.textSecondary
        )
    }

    val (badgeEstadoTexto, badgeEstadoBg, badgeEstadoColor) = when {
        esPagadaCompletamente -> Triple("✓ PAGADA", colors.surfaceContainerLow, colors.primaryEmerald)
        progresoPct > 0 -> Triple("AL DÍA", colors.surfaceContainerLow, colors.primaryEmerald)
        else -> Triple("EN CURSO", colors.debtAmberBg, colors.debtAmber)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = deuda.concepto,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            color = badgeEstadoBg,
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Text(
                                text = badgeEstadoTexto,
                                color = badgeEstadoColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Text(
                        text = subtituloTipo,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (deuda.fechaInicio.isNotEmpty() || deuda.fechaFin.isNotEmpty()) {
                        Surface(
                            color = colors.surfaceContainerLow,
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Plazo: ${deuda.fechaInicio} - ${deuda.fechaFin}",
                                color = colors.primaryEmerald,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (esPagadaCompletamente) "Deuda Total" else "Pendiente",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatMoneda.format(if (esPagadaCompletamente) deuda.totalDeuda else restante),
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            if (deuda.descripcion.isNotEmpty()) {
                Surface(
                    color = colors.surfaceContainer,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Nota: \"${deuda.descripcion}\"",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = colors.surfaceContainerHigh
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pagado: ${formatMoneda.format(deuda.totalPagado)} ($progresoPct%)",
                    color = colors.primaryEmerald,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restante: ${formatMoneda.format(restante)}",
                    color = colors.expenseRed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { progresoPct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(percent = 50)),
                color = colors.primaryEmerald,
                trackColor = colors.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .alpha(if (esPagadaCompletamente) 0.3f else 1f)
                            .clip(CircleShape)
                            .background(colors.surfaceContainerLow)
                            .clickable(enabled = !esPagadaCompletamente) { onEditarClick(deuda) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Editar deuda",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.expenseRedBg)
                            .clickable { onEliminarClick(deuda) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Eliminar deuda",
                            tint = colors.expenseRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (!esPagadaCompletamente) {
                    Surface(
                        color = colors.surfaceContainerLow,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { onAbonarClick(deuda) }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Abonar cuota",
                                color = colors.primaryEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AhorroDeudaPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(12.dp)) {
            AhorroCardItem(
                ahorro = Ahorro(
                    id = "1",
                    titulo = "Fondo de Emergencia",
                    categoria = "Emergencia",
                    montoActual = 350000.0,
                    montoObjetivo = 500000.0,
                    fechaMeta = "Dic 2026"
                ),
                onEditarClick = {},
                onEliminarClick = {},
                onAbonarClick = {}
            )
            DeudaCardItem(
                deuda = Deuda(
                    id = "1",
                    tipo = "Tarjeta de crédito",
                    concepto = "Visa Gold",
                    descripcion = "Compra computador",
                    totalDeuda = 1200000.0,
                    totalPagado = 400000.0,
                    fechaInicio = "01 Ago 2026",
                    fechaFin = "01 Feb 2027",
                    cuotas = 6,
                    valorCuota = 200000.0
                ),
                onEditarClick = {},
                onEliminarClick = {},
                onAbonarClick = {}
            )
        }
    }
}
