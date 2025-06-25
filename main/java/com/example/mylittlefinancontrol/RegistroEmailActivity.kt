package com.example.mylittlefinancontrol

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mylittlefinancontrol.databinding.ActivityRegistroEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegistroEmailActivity: AppCompatActivity() {
    private lateinit var binding: ActivityRegistroEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.btnRegistrar.setOnClickListener{
            validarInformacion()
        }

    }

    private var nombre = ""
    private var email = ""
    private var password = ""
    private var r_password = ""

    private fun validarInformacion(){
        nombre = binding.etNombres.text.toString().trim()
        email = binding.etEmail.text.toString().trim()
        password = binding.etPassword.text.toString().trim()
        r_password  = binding.etRPassword.text.toString().trim()

        if (nombre.isEmpty()){
            binding.etNombres.error = "Este campo es obligatorio"
            binding.etNombres.requestFocus()
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.etEmail.error = "correo invalido"
            binding.etEmail.requestFocus()
        }
        else if (email.isEmpty()){
            binding.etEmail.error = "Este campo es obligatorio"
            binding.etEmail.requestFocus()
        }
        else if (password.isEmpty()){
            binding.etPassword.error = "Este campo es obligatorio"
            binding.etPassword.requestFocus()
        }
        else if (r_password.isEmpty()){
            binding.etRPassword.error = "Este campo es obligatorio"
            binding.etRPassword.requestFocus()
        }
        else if (password != r_password){
            binding.etRPassword.error = "No coinciden las contraseñas"
            binding.etRPassword.requestFocus()
        }else{
            registrarUsuario()
        }
    }

    private fun registrarUsuario(){
        progressDialog.setMessage("Creando cuenta de usuario")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                actualizarInformacion()
            }
            .addOnFailureListener{ e ->
                progressDialog.dismiss()
                Toast.makeText(this, "error al crear usuairo ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun actualizarInformacion(){
        progressDialog.setMessage("guardando informacion")
        progressDialog.show()

        val uidu = firebaseAuth.uid
        val nombresU = nombre
        val emailsU = firebaseAuth.currentUser!!.email

        val tiempoR = Constantes.obtenerTiempoD()

        val datos_usuario = HashMap<String, Any>()

        datos_usuario["uid"] =  "$uidu"
        datos_usuario["nombres"] =  "$nombresU"
        datos_usuario["email"] =  "$emailsU"
        datos_usuario["tiempoR"] =  "$tiempoR"
        datos_usuario["proveedor"] =  "Email"
        datos_usuario["estado"] =  "Online"
        datos_usuario["image"] =  ""

        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        reference.child(uidu!!)
            .setValue(datos_usuario)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener{ e ->
                progressDialog.dismiss()
                Toast.makeText(this, "error al crear cuenta ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}