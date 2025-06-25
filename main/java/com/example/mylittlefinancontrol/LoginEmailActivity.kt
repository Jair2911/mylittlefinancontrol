package com.example.mylittlefinancontrol

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mylittlefinancontrol.databinding.ActivityLoginEmailBinding
import com.google.firebase.auth.FirebaseAuth

class LoginEmailActivity: AppCompatActivity() {
    private lateinit var binding: ActivityLoginEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.tvRegistrarme.setOnClickListener{
            startActivity(Intent(applicationContext, RegistroEmailActivity::class.java))
        }
        binding.btnIngresar.setOnClickListener{
            validarInformacion()
        }
    }

    private var email = ""
    private var password = ""

    private fun validarInformacion() {
        email = binding.etEmail.text.toString().trim()
        password = binding.etPassword.text.toString().trim()

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
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
        }else{
            loguearUsuario()
        }
    }

    private fun loguearUsuario(){
        progressDialog.setMessage("ingresando")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                progressDialog.dismiss()

                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()

                Toast.makeText(this, "no se realiza el logueo ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}