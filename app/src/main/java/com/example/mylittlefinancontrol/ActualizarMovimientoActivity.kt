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
    private var movimientoOriginal: Movimiento? = null

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
            movimientoOriginal = movimiento
            withContext(Dispatchers.Main) {
                binding.etDescripcion.setText(movimiento.descripcion)
                binding.etValor.setText(movimiento.valor.toString())
                binding.etTipoReg.setText(movimiento.tipo_reg)
            }
        }

        binding.ivActualizarMovimiento.setOnClickListener {
            val original = movimientoOriginal ?: return@setOnClickListener

            val descripcion = binding.etDescripcion.text.toString().trim()
            val valorInt = binding.etValor.text.toString().toIntOrNull() ?: original.valor
            val tipo_reg = binding.etTipoReg.text.toString().trim()

            lifecycleScope.launch(Dispatchers.IO) {
                val movimiento_actualizado = Movimiento(
                    id = original.id,
                    descripcion = if (descripcion.isNotBlank()) descripcion else original.descripcion,
                    valor = valorInt,
                    tipo_reg = if (tipo_reg.isNotBlank()) tipo_reg else original.tipo_reg,
                    fecha = original.fecha, // Preserva la fecha real de la transacción original
                    gastoFijoId = original.gastoFijoId,
                    quincenaPagada = original.quincenaPagada,
                    categoria = original.categoria,
                    fechaCreacion = original.fechaCreacion // Preserva la fecha original de registro en la app
                )
                db.updateMovimiento(movimiento_actualizado)

                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@ActualizarMovimientoActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
