package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActualizarMovimientoActivity : AppCompatActivity() {
    private lateinit var db: NotasDatabaseHelper
    private var idMovimiento: Int = -1
    private var movimientoOriginal by mutableStateOf<Movimiento?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = NotasDatabaseHelper.getInstance(this)
        idMovimiento = intent.getIntExtra("id_movimiento", -1)

        if (idMovimiento == -1) {
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val movimiento = db.getIdMovimiento(idMovimiento)
            withContext(Dispatchers.Main) {
                movimientoOriginal = movimiento
            }
        }

        setContent {
            MyLittleFinanControlTheme {
                ActualizarMovimientoScreen(
                    movimiento = movimientoOriginal,
                    onGuardarClick = { descripcion, valorStr, tipoReg ->
                        val original = movimientoOriginal ?: return@ActualizarMovimientoScreen
                        val valorInt = valorStr.toIntOrNull() ?: original.valor

                        lifecycleScope.launch(Dispatchers.IO) {
                            val movimientoActualizado = Movimiento(
                                id = original.id,
                                descripcion = descripcion.ifBlank { original.descripcion },
                                valor = valorInt,
                                tipo_reg = tipoReg.ifBlank { original.tipo_reg },
                                fecha = original.fecha,
                                gastoFijoId = original.gastoFijoId,
                                quincenaPagada = original.quincenaPagada,
                                categoria = original.categoria,
                                fechaCreacion = original.fechaCreacion,
                                metodoPago = original.metodoPago,
                                ahorroId = original.ahorroId
                            )
                            db.updateMovimiento(movimientoActualizado)

                            withContext(Dispatchers.Main) {
                                startActivity(Intent(this@ActualizarMovimientoActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActualizarMovimientoScreen(
    movimiento: Movimiento?,
    onGuardarClick: (descripcion: String, valor: String, tipoReg: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    var descripcion by remember(movimiento) { mutableStateOf(movimiento?.descripcion.orEmpty()) }
    var valor by remember(movimiento) { mutableStateOf(movimiento?.valor?.toString().orEmpty()) }
    var tipoReg by remember(movimiento) { mutableStateOf(movimiento?.tipo_reg.orEmpty()) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.surfaceCard,
        unfocusedContainerColor = colors.surfaceCard,
        focusedBorderColor = colors.primaryEmerald,
        unfocusedBorderColor = colors.borderSubtle
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 36.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primaryEmerald50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = null,
                    tint = colors.primaryEmerald,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "Editar Movimiento",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Modifica los detalles del registro",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text(stringResource(id = R.string.etDescripcion)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(14.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = valor,
            onValueChange = { valor = it },
            label = { Text(stringResource(id = R.string.etValor)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = tipoReg,
            onValueChange = { tipoReg = it },
            label = { Text(stringResource(id = R.string.etTipoReg)) },
            shape = RoundedCornerShape(14.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = { onGuardarClick(descripcion.trim(), valor.trim(), tipoReg.trim()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryEmerald,
                contentColor = Color.White
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Actualizar Movimiento",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActualizarMovimientoScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        ActualizarMovimientoScreen(
            movimiento = Movimiento(1, "Supermercado", 120000, "GASTO", "2026-11-07"),
            onGuardarClick = { _, _, _ -> }
        )
    }
}
