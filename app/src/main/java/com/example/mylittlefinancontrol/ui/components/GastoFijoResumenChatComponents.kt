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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylittlefinancontrol.MensajeChat
import com.example.mylittlefinancontrol.Modelos.CategoriaGastoItem
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private val formatMoneda: NumberFormat
    get() = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

@Composable
fun GastoFijoCardItem(
    gasto: GastoFijo,
    onEditarClick: (GastoFijo) -> Unit,
    onEliminarClick: (GastoFijo) -> Unit,
    onControlAnualClick: (GastoFijo) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    val catLower = gasto.categoria.lowercase()
    val (iconRes, iconBg, iconTint) = when {
        catLower.contains("serv") || catLower.contains("básic") || catLower.contains("luz") || catLower.contains("agua") || catLower.contains("gas") -> Triple(
            R.drawable.ic_balance,
            colors.primaryEmerald50,
            colors.primaryEmerald
        )
        catLower.contains("viv") || catLower.contains("arriend") || catLower.contains("casa") || catLower.contains("hogar") -> Triple(
            R.drawable.ic_cottage,
            colors.surfaceContainer,
            colors.textPrimary
        )
        catLower.contains("tele") || catLower.contains("inter") || catLower.contains("fibra") || catLower.contains("movil") || catLower.contains("wifi") -> Triple(
            R.drawable.ic_router,
            colors.primaryEmerald50,
            colors.primaryEmerald
        )
        catLower.contains("suscrip") || catLower.contains("stream") || catLower.contains("netf") || catLower.contains("spot") -> Triple(
            R.drawable.ic_subscriptions,
            colors.surfaceContainer,
            colors.textPrimary
        )
        catLower.contains("educ") || catLower.contains("univ") || catLower.contains("col") -> Triple(
            R.drawable.ic_school,
            colors.surfaceContainerHigh,
            colors.textVariant
        )
        else -> Triple(
            R.drawable.ic_more_horiz,
            colors.surfaceContainerLow,
            colors.textSecondary
        )
    }

    val mesAnioActual = remember {
        val cal = Calendar.getInstance()
        String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    val esQuincenal = gasto.periodo.contains("Quincenal", ignoreCase = true)

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
                .padding(14.dp)
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
                        Surface(
                            color = colors.surfaceContainer,
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Text(
                                text = gasto.categoria.uppercase(),
                                color = colors.textVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Día ${gasto.diaPago} · ${gasto.periodo}",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    Text(
                        text = gasto.concepto,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )

                    Text(
                        text = formatMoneda.format(gasto.monto),
                        color = colors.primaryEmerald,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row {
                    IconButton(
                        onClick = { onEditarClick(gasto) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Editar",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onEliminarClick(gasto) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Eliminar",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Contenedor inferior de Estado de Pago
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceContainerLow)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (esQuincenal) {
                    Text(
                        text = "CUOTAS DEL MES:",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    val q1Pagada = gasto.ultimoPagoQ1 == mesAnioActual
                    val q2Pagada = gasto.ultimoPagoQ2 == mesAnioActual

                    Surface(
                        color = if (q1Pagada) colors.primaryEmerald50 else colors.surfaceContainer,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = if (q1Pagada) "✓ Q1 PAGADA" else "⏱️ Q1 PENDIENTE",
                            color = if (q1Pagada) colors.primaryEmerald else colors.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = if (q2Pagada) colors.primaryEmerald50 else colors.surfaceContainer,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = if (q2Pagada) "✓ Q2 PAGADA" else "⏱️ Q2 PENDIENTE",
                            color = if (q2Pagada) colors.primaryEmerald else colors.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    val estaPagado = gasto.ultimoPagoQ1 == mesAnioActual
                    val dia = gasto.diaPago.trim()
                    val statusLabel = when {
                        estaPagado -> "Abonado automáticamente"
                        dia == "30" || dia.contains("fin", ignoreCase = true) -> "Vence a fin de mes"
                        else -> "Vence el día $dia"
                    }

                    Text(
                        text = statusLabel,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        color = if (estaPagado) colors.primaryEmerald50 else colors.surfaceContainerHigh,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = if (estaPagado) "✓ PAGADO" else "⏱️ PENDIENTE",
                            color = if (estaPagado) colors.primaryEmerald else colors.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { onControlAnualClick(gasto) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.primaryEmerald),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "📅 Control Anual de Pagos",
                    color = colors.primaryEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MesControlAnualItem(
    nombreMes: String,
    detallePago: String,
    badgeEstadoTexto: String,
    esPagado: Boolean,
    mostrarBotonPagar: Boolean = false,
    onPagarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombreMes,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detallePago,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                color = if (esPagado) colors.primaryEmerald50 else colors.surfaceContainerHigh,
                shape = RoundedCornerShape(percent = 50)
            ) {
                Text(
                    text = badgeEstadoTexto,
                    color = if (esPagado) colors.primaryEmerald else colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (mostrarBotonPagar && onPagarClick != null) {
                Button(
                    onClick = onPagarClick,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryEmerald,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Pagar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriaResumenItem(
    item: CategoriaGastoItem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    val catUpper = item.nombreCategoria.uppercase()
    val iconRes = when {
        catUpper.contains("SUPERMERCADO") || catUpper.contains("TIENDA") -> R.drawable.ic_shopping_cart
        catUpper.contains("COMIDA") || catUpper.contains("RESTAURANTE") -> R.drawable.ic_restaurant
        catUpper.contains("TRANSPORTE") || catUpper.contains("AUTO") -> R.drawable.ic_directions_car
        catUpper.contains("VIVIENDA") || catUpper.contains("CASA") -> R.drawable.ic_cottage
        catUpper.contains("OCIO") || catUpper.contains("STREAMING") -> R.drawable.ic_subscriptions
        catUpper.contains("AHORRO") -> R.drawable.ic_savings
        catUpper.contains("DEUDA") || catUpper.contains("PRÉSTAMO") -> R.drawable.ic_credit_card
        catUpper.contains("SERVICIO") -> R.drawable.ic_wifi
        catUpper.contains("GASTO FIJO") -> R.drawable.ic_autorenew
        else -> R.drawable.ic_receipt_long
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = item.nombreCategoria,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (item.conteoTransacciones == 1) "1 transacción" else "${item.conteoTransacciones} transacciones",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-${formatMoneda.format(item.totalMonto)}",
                        color = colors.expenseRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = colors.surfaceContainerLow,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", item.porcentaje),
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (item.porcentaje.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(percent = 50)),
                color = colors.primaryEmerald,
                trackColor = colors.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ChatMensajeItem(
    mensaje: MensajeChat,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (mensaje.esUsuario) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (mensaje.esUsuario) colors.primaryEmerald else colors.surfaceContainerHigh,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mensaje.esUsuario) 16.dp else 4.dp,
                bottomEnd = if (mensaje.esUsuario) 4.dp else 16.dp
            )
        ) {
            Text(
                text = mensaje.texto,
                color = if (mensaje.esUsuario) Color.White else colors.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GastoFijoResumenChatPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(12.dp)) {
            GastoFijoCardItem(
                gasto = GastoFijo(
                    id = "1",
                    concepto = "Internet Fibra Óptica",
                    categoria = "Telecomunicaciones",
                    monto = 110000.0,
                    diaPago = "15",
                    periodo = "Mensual"
                ),
                onEditarClick = {},
                onEliminarClick = {},
                onControlAnualClick = {}
            )
            MesControlAnualItem(
                nombreMes = "Noviembre 2026",
                detallePago = "Pagado el 15/11/2026",
                badgeEstadoTexto = "✓ PAGADO",
                esPagado = true
            )
            CategoriaResumenItem(
                item = CategoriaGastoItem(
                    nombreCategoria = "Supermercado",
                    totalMonto = 420000.0,
                    porcentaje = 42.5,
                    conteoTransacciones = 5
                )
            )
            ChatMensajeItem(mensaje = MensajeChat("Hola, ¿cómo van mis finanzas?", esUsuario = true))
            ChatMensajeItem(mensaje = MensajeChat("¡Vas muy bien! Tu ahorro subió un 15%.", esUsuario = false))
        }
    }
}
