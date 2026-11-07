package com.example.mylittlefinancontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class OpcionesLoginActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        aplicarTemaGuardado()
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        setContent {
            MyLittleFinanControlTheme {
                OpcionesLoginScreen(
                    isLoading = isSaving,
                    onEmailLoginClick = {
                        startActivity(Intent(applicationContext, LoginEmailActivity::class.java))
                    },
                    onGoogleLoginClick = {
                        iniciarConGoogle()
                    }
                )
            }
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
        isSaving = true

        val uidu = firebaseAuth.uid
        val nombresU = firebaseAuth.currentUser!!.displayName
        val emailsU = firebaseAuth.currentUser!!.email
        val tiempoR = Constantes.obtenerTiempoD()

        val datosUsuario = HashMap<String, Any>()
        datosUsuario["uid"] = "$uidu"
        datosUsuario["nombres"] = "$nombresU"
        datosUsuario["email"] = "$emailsU"
        datosUsuario["tiempoR"] = "$tiempoR"
        datosUsuario["proveedor"] = "Google"
        datosUsuario["estado"] = "Online"
        datosUsuario["image"] = ""
        datosUsuario["tutorial"] = false

        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        reference.child(uidu!!)
            .setValue(datosUsuario)
            .addOnSuccessListener {
                isSaving = false
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(this, "error al crear cuenta ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun OpcionesLoginScreen(
    isLoading: Boolean,
    onEmailLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.fondo_home),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 36.dp)
                    .size(width = 414.dp, height = 398.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = colors.primaryEmerald,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = onEmailLoginClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryEmerald,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "INGRESAR CON EMAIL",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onGoogleLoginClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.primaryEmerald),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.surfaceCard,
                        contentColor = colors.primaryEmerald
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.btn_sign_in).uppercase(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OpcionesLoginScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        OpcionesLoginScreen(
            isLoading = false,
            onEmailLoginClick = {},
            onGoogleLoginClick = {}
        )
    }
}
