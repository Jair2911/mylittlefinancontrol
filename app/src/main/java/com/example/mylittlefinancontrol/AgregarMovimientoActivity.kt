package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mylittlefinancontrol.Fragmentos.FragmentMovimiento
import com.example.mylittlefinancontrol.databinding.ActivityAgregarMovimientoBinding

class AgregarMovimientoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarMovimientoBinding
    private lateinit var db: NotasDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarMovimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotasDatabaseHelper(this)

        binding.ivGuardarMovimiento.setOnClickListener{
            val descripcion = binding.etDescripcion.text.toString()
            val valor = binding.etValor.text.toString()
            val tipo_reg = binding.etTipoReg.text.toString()
            val fecha = Constantes.formatoFecha(Constantes.obtenerTiempoD());

            if (descripcion.isEmpty()){
                binding.etDescripcion.error = "Este campo es obligatorio"
                binding.etDescripcion.requestFocus()
            }
            if (valor.isEmpty()){
                binding.etDescripcion.error = "Este campo es obligatorio"
                binding.etDescripcion.requestFocus()
            }
            if (tipo_reg.isEmpty()){
                binding.etDescripcion.error = "Este campo es obligatorio"
                binding.etDescripcion.requestFocus()
            }

            if(!descripcion.isEmpty() && !valor.isEmpty()){
                guardarMovimento(descripcion.uppercase(), valor.toInt(), tipo_reg.uppercase(), fecha)
            }else{
                Toast.makeText(
                    applicationContext,
                    "Todos los campos son obligatorios",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    private fun guardarMovimento(descripcion: String, valor: Int, tipo_reg: String, fecha: String){
        val movimento = Movimiento(0, descripcion, valor, tipo_reg, fecha)

        db.insertNota(movimento)

        startActivity(Intent(applicationContext, MainActivity::class.java))
        finishAffinity()

        Toast.makeText(
            applicationContext,
            "Se ha agregado la nota",
            Toast.LENGTH_SHORT
        ).show()
    }
}