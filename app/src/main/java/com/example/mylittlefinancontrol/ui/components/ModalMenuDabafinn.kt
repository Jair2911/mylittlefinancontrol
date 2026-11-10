package com.example.mylittlefinancontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme

/**
 * Modelo de datos para las opciones del menú modal de DABAFINN basado en la paleta oficial DABA.
 */
data class OpcionMenuDabafinn(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector,
    val colorFondo: Color,
    val colorIcono: Color,
    val colorBorde: Color,
)

/**
 * Modal emergente (Dialog) de navegación principal para DABAFINN.
 * Basado 100% en la paleta de diseño DABA (Esmeralda / Obsidian / Semánticos).
 */
@Composable
fun ModalMenuDabafinn(
    rutaActual: String = "inicio",
    alNavegar: (String) -> Unit,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = alCerrar) {
        ModalMenuContent(
            rutaActual = rutaActual,
            alNavegar = alNavegar,
            alCerrar = alCerrar,
            modifier = modifier
        )
    }
}

/**
 * Contenedor visual del menú modal (Surface con esquinas 24.dp, chip de Inicio y grid de 3 columnas).
 */
@Composable
fun ModalMenuContent(
    rutaActual: String = "inicio",
    alNavegar: (String) -> Unit,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    // Opciones basadas estrictamente en la paleta DABA establecida
    val opciones = remember(colors) {
        listOf(
            OpcionMenuDabafinn(
                ruta = "movimientos",
                titulo = "Movimientos",
                icono = Icons.AutoMirrored.Filled.List,
                colorFondo = colors.surfaceContainer,
                colorIcono = colors.textVariant,
                colorBorde = colors.borderSubtle,
            ),
            OpcionMenuDabafinn(
                ruta = "gastos_fijos",
                titulo = "Gastos Fijos",
                icono = Icons.Filled.DateRange,
                colorFondo = colors.ghostBg,
                colorIcono = colors.primaryEmerald,
                colorBorde = colors.ghostBorder,
            ),
            OpcionMenuDabafinn(
                ruta = "deudas",
                titulo = "Deudas",
                icono = Icons.Filled.MoneyOff,
                colorFondo = colors.expenseRedBg,
                colorIcono = colors.expenseRed,
                colorBorde = colors.expenseRed.copy(alpha = 0.35f),
            ),
            OpcionMenuDabafinn(
                ruta = "ahorros",
                titulo = "Ahorros",
                icono = Icons.Filled.Savings,
                colorFondo = colors.ghostBg,
                colorIcono = colors.primaryEmerald,
                colorBorde = colors.ghostBorder,
            ),
            OpcionMenuDabafinn(
                ruta = "perfil",
                titulo = "Perfil",
                icono = Icons.Filled.Person,
                colorFondo = colors.debtAmberBg,
                colorIcono = colors.debtAmber,
                colorBorde = colors.debtAmber.copy(alpha = 0.35f),
            ),
            OpcionMenuDabafinn(
                ruta = "cerrar_sesion",
                titulo = "Cerrar Sesión",
                icono = Icons.AutoMirrored.Filled.ExitToApp,
                colorFondo = colors.surfaceContainer,
                colorIcono = colors.textVariant,
                colorBorde = colors.borderSubtle,
            ),
        )
    }

    val esInicioActivo = rutaActual == "inicio"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera del modal con título y botón rápido para ir a Inicio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menú Principal",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = colors.textPrimary
                )

                Surface(
                    onClick = {
                        alCerrar()
                        alNavegar("inicio")
                    },
                    shape = RoundedCornerShape(50),
                    color = if (esInicioActivo) colors.ghostBg else colors.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        if (esInicioActivo) colors.primaryEmerald else colors.borderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Ir a Inicio",
                            tint = if (esInicioActivo) colors.primaryEmerald else colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (esInicioActivo) "En Inicio" else "Ir a Inicio",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (esInicioActivo) colors.primaryEmerald else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cuadrícula de 6 opciones (3 columnas x 2 filas)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                userScrollEnabled = false
            ) {
                items(opciones) { opcion ->
                    val esActiva = opcion.ruta == rutaActual
                    BotonOpcionMenuDabafinn(
                        opcion = opcion,
                        esActiva = esActiva,
                        onClick = {
                            alCerrar()
                            alNavegar(opcion.ruta)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Botón circular individual con indicador visual cuando la sección está activa.
 */
@Composable
fun BotonOpcionMenuDabafinn(
    opcion: OpcionMenuDabafinn,
    esActiva: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (esActiva) colors.ghostBg.copy(alpha = 0.65f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        // Contenedor circular de 60.dp con anillo resaltado y dot cuando está activo
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(opcion.colorFondo)
                    .border(
                        width = if (esActiva) 2.dp else 1.dp,
                        color = if (esActiva) opcion.colorIcono else opcion.colorBorde,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = opcion.icono,
                    contentDescription = opcion.titulo,
                    tint = opcion.colorIcono,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Punto indicador de vista activa
            if (esActiva) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(colors.activeStateDot)
                        .border(2.dp, colors.surfaceCard, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Texto de la opción debajo del círculo
        Text(
            text = opcion.titulo,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (esActiva) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (esActiva) colors.primaryEmerald else colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModalMenuDabafinnPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ModalMenuContent(
                rutaActual = "gastos_fijos",
                alNavegar = {},
                alCerrar = {}
            )
        }
    }
}
