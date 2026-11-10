package com.example.mylittlefinancontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
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
    val colorBorde: Color
)

/**
 * Modal emergente (Dialog) de navegación principal para DABAFINN.
 * Basado 100% en la paleta de diseño DABA (Esmeralda / Obsidian / Semánticos).
 */
@Composable
fun ModalMenuDabafinn(
    alNavegar: (String) -> Unit,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = alCerrar) {
        ModalMenuContent(
            alNavegar = alNavegar,
            alCerrar = alCerrar,
            modifier = modifier
        )
    }
}

/**
 * Contenedor visual del menú modal (Surface con esquinas 24.dp y grid de 3 columnas).
 */
@Composable
fun ModalMenuContent(
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título del modal
            Text(
                text = "Menú Principal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.3.sp
                ),
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Cuadrícula de 6 opciones (3 columnas x 2 filas)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(opciones) { opcion ->
                    BotonOpcionMenuDabafinn(
                        opcion = opcion,
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
 * Botón circular individual con fondo y borde suaves de la paleta DABA, e icono centrado.
 */
@Composable
fun BotonOpcionMenuDabafinn(
    opcion: OpcionMenuDabafinn,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        // Contenedor circular de 60.dp con color de la paleta DABA establecida
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(opcion.colorFondo)
                .border(1.dp, opcion.colorBorde, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = opcion.icono,
                contentDescription = opcion.titulo,
                tint = opcion.colorIcono,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Texto de la opción debajo del círculo
        Text(
            text = opcion.titulo,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = colors.textPrimary,
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
                alNavegar = {},
                alCerrar = {}
            )
        }
    }
}
