package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mylittlefinancontrol.databinding.ActivityActualizarMovimientoBinding

class ActualizarMovimientoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActualizarMovimientoBinding
    private lateinit var db : NotasDatabaseHelper
    private var idMovimiento : Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActualizarMovimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotasDatabaseHelper(this)

        idMovimiento = intent.getIntExtra("id_movimiento", -1)

        if(idMovimiento == -1){
            finish()
            return
        }

        val movimiento = db.getIdMovimiento(idMovimiento)
        binding.etDescripcion.setText(movimiento.descripcion)
        binding.etValor.setText(movimiento.valor.toString())
        binding.etTipoReg.setText(movimiento.tipo_reg)

        binding.ivActualizarMovimiento.setOnClickListener{
            val descripcion = binding.etDescripcion.text.toString();
            val valor = binding.etValor.text.toString();
            val tipo_reg = binding.etTipoReg.text.toString();
            val fecha = Constantes.formatoFecha(Constantes.obtenerTiempoD());

            val movimiento_actualizado = Movimiento(idMovimiento, descripcion, valor.toInt(), tipo_reg, fecha);
            db.updateMovimiento(movimiento_actualizado);

            startActivity(Intent(this, MainActivity::class.java))
            finish();
        }
    }
}