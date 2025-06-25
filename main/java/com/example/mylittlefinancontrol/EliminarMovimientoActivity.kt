package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class EliminarMovimientoActivity: AppCompatActivity() {
    private lateinit var db : NotasDatabaseHelper
    private var idMovimiento : Int = -1
    // notasAdaptador no es necesario en esta Activity, ya que solo maneja una eliminación.
    // private lateinit var notasAdaptador: NotasAdaptador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No necesitamos setContentView si solo vamos a mostrar un diálogo y luego finalizar.

        db = NotasDatabaseHelper(this)
        idMovimiento = intent.getIntExtra("id_movimiento", -1)

        if(idMovimiento == -1){
            // Si el ID no es válido, no hacemos nada y finalizamos la actividad.
            Toast.makeText(this, "Error: ID de movimiento no válido.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Mostrar el diálogo de confirmación de eliminación
        AlertDialog.Builder(this)
            .setTitle("ELIMINAR MOVIMIENTO") // Título más descriptivo
            .setMessage("¿Estás seguro de que quieres eliminar este movimiento?")
            .setPositiveButton("Eliminar") { dialog, which ->
                val deletedRows = db.deleteMovimiento(idMovimiento)

                if (deletedRows > 0){
                    Toast.makeText(
                        this,
                        "El movimiento se ha eliminado con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo eliminar el movimiento",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Después de la eliminación (exitosa o no), regresamos a MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Finaliza esta actividad de eliminación
            }
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss() // Cierra el diálogo
                Toast.makeText(
                    this@EliminarMovimientoActivity,
                    "Eliminación cancelada",
                    Toast.LENGTH_SHORT
                ).show()
                // Regresa a la actividad anterior (probablemente MainActivity)
                finish() // Finaliza esta actividad de eliminación sin hacer cambios
            }
            .setCancelable(false) // Opcional: Evita que el diálogo se cierre al tocar fuera
            .show()
    }
}