package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EliminarMovimientoActivity: AppCompatActivity() {
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

        AlertDialog.Builder(this)
            .setTitle("ELIMINAR MOVIMIENTO")
            .setMessage("¿Estás seguro de que quieres eliminar este movimiento?")
            .setPositiveButton("Eliminar") { dialog, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val deletedRows = db.deleteMovimiento(idMovimiento)
                    withContext(Dispatchers.Main) {
                        if (deletedRows > 0) {
                            Toast.makeText(this@EliminarMovimientoActivity, "El movimiento se ha eliminado con éxito", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@EliminarMovimientoActivity, "No se pudo eliminar el movimiento", Toast.LENGTH_SHORT).show()
                        }
                        startActivity(Intent(this@EliminarMovimientoActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
                Toast.makeText(this@EliminarMovimientoActivity, "Eliminación cancelada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
