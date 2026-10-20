package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.databinding.ActivityActualizarMovimientoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActualizarMovimientoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActualizarMovimientoBinding
    private lateinit var db: NotasDatabaseHelper
    private var idMovimiento: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActualizarMovimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotasDatabaseHelper.getInstance(this)

        idMovimiento = intent.getIntExtra("id_movimiento", -1)

        if (idMovimiento == -1) {
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val movimiento = db.getIdMovimiento(idMovimiento)
            withContext(Dispatchers.Main) {
                binding.etDescripcion.setText(movimiento.descripcion)
                binding.etValor.setText(movimiento.valor.toString())
                binding.etTipoReg.setText(movimiento.tipo_reg)
            }
        }

        binding.ivActualizarMovimiento.setOnClickListener {
            val descripcion = binding.etDescripcion.text.toString()
            val valorInt = binding.etValor.text.toString().toIntOrNull() ?: 0
            val tipo_reg = binding.etTipoReg.text.toString()
            val fecha = Constantes.formatoFecha(Constantes.obtenerTiempoD())

            lifecycleScope.launch(Dispatchers.IO) {
                val movimiento_actualizado = Movimiento(idMovimiento, descripcion, valorInt, tipo_reg, fecha)
                db.updateMovimiento(movimiento_actualizado)

                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@ActualizarMovimientoActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
