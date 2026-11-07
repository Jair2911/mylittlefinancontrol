package com.example.mylittlefinancontrol.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasAdaptador
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import java.text.NumberFormat
import java.util.Locale

private val currencyFormatter: NumberFormat
    get() = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

@Composable
fun MovimientoInicioItem(
    movimiento: Movimiento,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = DabaTheme.colors
    val esIngreso = movimiento.tipo_reg.contains("INGRESO", ignoreCase = true)
    val descLower = movimiento.descripcion.lowercase()
    val montoFormateado = remember(movimiento.valor) { currencyFormatter.format(movimiento.valor) }

    val (badgeText, badgeBg, badgeFg, iconRes, iconBg, iconTint) = when {
        esIngreso -> Sextuple(
            "Ingreso",
            colors.primaryEmerald50,
            colors.primaryEmerald,
            R.drawable.ic_add,
            colors.primaryEmerald50,
            colors.primaryEmerald
        )
        movimiento.gastoFijoId != null || descLower.contains("pago") || descLower.contains("fijo") -> Sextuple(
            "Fijo",
            colors.surfaceContainerHigh,
            colors.textSecondary,
            R.drawable.ic_balance,
            colors.surfaceContainerHigh,
            colors.textSecondary
        )
        descLower.contains("super") || descLower.contains("mercado") || descLower.contains("compra") -> Sextuple(
            "Supermercado",
            colors.primaryEmerald50,
            colors.primaryEmerald,
            R.drawable.ic_shopping_cart,
            colors.primaryEmerald50,
            colors.primaryEmerald
        )
        else -> Sextuple(
            "Varios",
            colors.surfaceContainer,
            colors.textPrimary,
            R.drawable.ic_shopping_bag,
            colors.surfaceContainer,
            colors.textPrimary
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
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
                Text(
                    text = movimiento.descripcion,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeFg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = movimiento.fecha,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Text(
                text = if (esIngreso) "+$montoFormateado" else "-$montoFormateado",
                color = if (esIngreso) colors.primaryEmerald else colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MovimientoHeaderItem(
    fechaTitulo: String,
    conteoTexto: String,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceBg)
            .padding(horizontal = 4.dp)
            .padding(top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = fechaTitulo,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Text(
            text = conteoTexto,
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MovimientoItem(
    movimiento: Movimiento,
    onEditarClick: (Movimiento) -> Unit,
    onEliminarClick: (Movimiento) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    val descUpper = movimiento.descripcion.uppercase()
    val catUpper = movimiento.categoria.uppercase()
    val tipoUpper = movimiento.tipo_reg.uppercase()
    val esIngreso = tipoUpper.contains("INGRESO")

    val style = when {
        tipoUpper.contains("GASTO FIJO") || descUpper.contains("GASTO FIJO") || descUpper.startsWith("PAGO:") || catUpper.contains("GASTO FIJO") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_autorenew,
                iconTint = colors.primaryEmerald,
                iconBg = colors.primaryEmerald50,
                badgeText = "Gasto Fijo",
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textPrimary,
                metodoBase = "Débito aut."
            )
        }
        catUpper.contains("SUPERMERCADO") || catUpper.contains("TIENDA") || descUpper.contains("SUPERMERCADO") || descUpper.contains("DESPENSA") || descUpper.contains("LÍDER") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_shopping_cart,
                iconTint = colors.primaryEmerald,
                iconBg = colors.ghostBg,
                badgeText = movimiento.categoria.ifBlank { "Supermercado" },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = "Tarjeta Visa / Débito"
            )
        }
        catUpper.contains("COMIDA") || catUpper.contains("RESTAURANTE") || descUpper.contains("COMIDA") || descUpper.contains("ALIMENTOS") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_restaurant,
                iconTint = colors.primaryEmerald,
                iconBg = colors.ghostBg,
                badgeText = movimiento.categoria.ifBlank { "Comida" },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = "Efectivo / Débito"
            )
        }
        catUpper.contains("TRANSPORTE") || catUpper.contains("AUTO") || descUpper.contains("COMBUSTIBLE") || descUpper.contains("UBER") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_directions_car,
                iconTint = colors.primaryEmerald,
                iconBg = colors.ghostBg,
                badgeText = movimiento.categoria.ifBlank { "Transporte" },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = "Efectivo"
            )
        }
        catUpper.contains("VIVIENDA") || catUpper.contains("CASA") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_cottage,
                iconTint = colors.primaryEmerald,
                iconBg = colors.ghostBg,
                badgeText = movimiento.categoria.ifBlank { "Vivienda" },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = movimiento.metodoPago
            )
        }
        catUpper.contains("OCIO") || catUpper.contains("HOBBIES") || catUpper.contains("CINE") || catUpper.contains("STREAMING") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_subscriptions,
                iconTint = colors.primaryEmerald,
                iconBg = colors.ghostBg,
                badgeText = movimiento.categoria.ifBlank { "Ocio" },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = movimiento.metodoPago
            )
        }
        catUpper.contains("AHORRO") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_savings,
                iconTint = colors.primaryEmerald,
                iconBg = colors.ghostBg,
                badgeText = "Ahorro",
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.primaryEmerald,
                metodoBase = movimiento.metodoPago
            )
        }
        catUpper.contains("PRÉSTAMO") || catUpper.contains("DEUDA") || tipoUpper.contains("DEUDA") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_credit_card,
                iconTint = colors.textSecondary,
                iconBg = colors.surfaceContainer,
                badgeText = movimiento.categoria.ifBlank { "Deuda" },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = movimiento.metodoPago
            )
        }
        esIngreso || descUpper.contains("HONORARIOS") || descUpper.contains("SUELDO") || descUpper.contains("TRANSFERENCIA") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_payments,
                iconTint = colors.primaryEmerald,
                iconBg = colors.primaryEmerald50,
                badgeText = if (descUpper.contains("HONORARIOS")) "Honorarios" else movimiento.categoria.ifBlank { "Ingreso" },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.primaryEmerald,
                metodoBase = movimiento.metodoPago
            )
        }
        catUpper.contains("SERVICIOS") || descUpper.contains("INTERNET") || descUpper.contains("FIBRA") || descUpper.contains("LUZ") || descUpper.contains("AGUA") -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_wifi,
                iconTint = colors.textSecondary,
                iconBg = colors.surfaceContainer,
                badgeText = "Servicios",
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = movimiento.metodoPago
            )
        }
        else -> {
            MovimientoVisualStyle(
                iconRes = R.drawable.ic_receipt_long,
                iconTint = colors.textSecondary,
                iconBg = colors.surfaceContainer,
                badgeText = movimiento.categoria.ifBlank { movimiento.tipo_reg.uppercase() },
                badgeBg = colors.surfaceContainer,
                badgeTextColor = colors.textSecondary,
                metodoBase = movimiento.metodoPago
            )
        }
    }

    val metodoSubtexto = remember(movimiento.fechaCreacion, movimiento.fecha, style.metodoBase) {
        if (movimiento.fechaCreacion.isNotBlank() && movimiento.fechaCreacion != movimiento.fecha) {
            val fcLimpia = if (movimiento.fechaCreacion.length >= 10) {
                movimiento.fechaCreacion.substring(0, 10)
            } else {
                movimiento.fechaCreacion.split(" ")[0]
            }
            "${style.metodoBase} (Reg: $fcLimpia)"
        } else {
            style.metodoBase
        }
    }

    val horaFormateada = remember(movimiento.fecha, movimiento.fechaCreacion) {
        NotasAdaptador.obtenerHoraFormateada(movimiento)
    }

    val montoFormateado = remember(movimiento.valor) {
        currencyFormatter.format(movimiento.valor)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Section: Icon + Description + Badge + Amount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(style.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = style.iconRes),
                        contentDescription = null,
                        tint = style.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = movimiento.descripcion,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = style.badgeBg,
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Text(
                                text = style.badgeText,
                                color = style.badgeTextColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.5.dp)
                            )
                        }

                        Text(
                            text = horaFormateada,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (esIngreso) "+$montoFormateado" else "-$montoFormateado",
                        color = if (esIngreso) colors.primaryEmerald else colors.expenseRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = metodoSubtexto,
                        color = colors.textMuted,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Bottom Contextual Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_verified),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Registrado con éxito",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = colors.surfaceCard,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { onEditarClick(movimiento) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Editar",
                                tint = colors.primaryEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Editar",
                                color = colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.expenseRedBg)
                            .clickable { onEliminarClick(movimiento) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Eliminar",
                            tint = colors.expenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

private data class MovimientoVisualStyle(
    @DrawableRes val iconRes: Int,
    val iconTint: Color,
    val iconBg: Color,
    val badgeText: String,
    val badgeBg: Color,
    val badgeTextColor: Color,
    val metodoBase: String
)

@Preview(showBackground = true)
@Composable
private fun MovimientoComponentsPreview() {
    val sampleMov = Movimiento(
        id = 1,
        descripcion = "Compra Supermercado Éxito",
        valor = 145000,
        tipo_reg = "GASTO",
        fecha = "2026-11-07 14:30",
        categoria = "Supermercado",
        metodoPago = "Tarjeta Débito"
    )
    MyLittleFinanControlTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(12.dp)) {
            MovimientoInicioItem(movimiento = sampleMov)
            MovimientoHeaderItem(fechaTitulo = "HOY • 07 NOV 2026", conteoTexto = "1 Movimiento")
            MovimientoItem(
                movimiento = sampleMov,
                onEditarClick = {},
                onEliminarClick = {}
            )
        }
    }
}
