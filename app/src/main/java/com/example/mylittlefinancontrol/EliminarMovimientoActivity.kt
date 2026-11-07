package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EliminarMovimientoActivity : AppCompatActivity() {
    private lateinit var db: NotasDatabaseHelper
    private var idMovimiento: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = NotasDatabaseHelper.getInstance(this)
        idMovimiento = intent.getIntExtra("id_movimiento", -1)

        if (idMovimiento == -1) {
            Toast.makeText(this, "Error: ID de movimiento no válido.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MyLittleFinanControlTheme {
                val colors = DabaTheme.colors
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("ELIMINAR MOVIMIENTO") },
                    text = { Text("¿Estás seguro de que quieres eliminar este movimiento?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val deletedRows = db.deleteMovimiento(idMovimiento)
                                    withContext(Dispatchers.Main) {
                                        if (deletedRows > 0) {
                                            Toast.makeText(
                                                this@EliminarMovimientoActivity,
                                                "El movimiento se ha eliminado con éxito",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                this@EliminarMovimientoActivity,
                                                "No se pudo eliminar el movimiento",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        startActivity(Intent(this@EliminarMovimientoActivity, MainActivity::class.java))
                                        finish()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.expenseRed,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                Toast.makeText(this@EliminarMovimientoActivity, "Eliminación cancelada", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        ) {
                            Text("Cancelar", color = colors.textSecondary)
                        }
                    }
                )
            }
        }
    }
}
