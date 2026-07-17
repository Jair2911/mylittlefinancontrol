package com.example.mylittlefinancontrol

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.mylittlefinancontrol.databinding.ActivityOpcionesLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class OpcionesLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpcionesLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar tema antes de inflar la vista
        aplicarTemaGuardado()
        
        super.onCreate(savedInstanceState)
        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere..")
        progressDialog.setCanceledOnTouchOutside(false)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(applicationContext, LoginEmailActivity::class.java))
        }

        binding.btnSignIn.setOnClickListener {
            iniciarConGoogle()
        }
    }

    private fun aplicarTemaGuardado() {
        val sp = getSharedPreferences("TemaPrefs", Context.MODE_PRIVATE)
        val esOscuro = sp.getBoolean("EsTemaOscuro", false)
        if (esOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val data = resultado.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val cuenta = task.getResult(ApiException::class.java)
            autenticaCuentaGoogle(cuenta.idToken)
        } catch (e: ApiException) {
            val mensaje = when (e.statusCode) {
                10 -> "Error 10: Revisa el SHA-1 en Firebase."
                12500 -> "Error 12500: Fallo de configuración."
                else -> "Error Google: ${e.statusCode}"
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Fallo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun comprobarSesion() {
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }

    private fun iniciarConGoogle() {
        val googleSignInIntent = mGoogleSignInClient.signInIntent
        googleSignInARL.launch(googleSignInIntent)
    }

    private fun autenticaCuentaGoogle(idToken: String?) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credencial)
            .addOnSuccessListener { authResultado ->
                if (authResultado.additionalUserInfo!!.isNewUser) {
                    actualizarInfoUsuarios()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "fallo ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarInfoUsuarios() {
        progressDialog.setMessage("guardando informacion")
        progressDialog.show()

        val uidu = firebaseAuth.uid
        val nombresU = firebaseAuth.currentUser!!.displayName
        val emailsU = firebaseAuth.currentUser!!.email
        val tiempoR = Constantes.obtenerTiempoD()

        val datos_usuario = HashMap<String, Any>()
        datos_usuario["uid"] = "$uidu"
        datos_usuario["nombres"] = "$nombresU"
        datos_usuario["email"] = "$emailsU"
        datos_usuario["tiempoR"] = "$tiempoR"
        datos_usuario["proveedor"] = "Google"
        datos_usuario["estado"] = "Online"
        datos_usuario["image"] = ""

        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        reference.child(uidu!!)
            .setValue(datos_usuario)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "error al crear cuenta ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
