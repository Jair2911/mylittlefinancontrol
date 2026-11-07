package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegistroEmailActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            MyLittleFinanControlTheme {
                RegistroEmailScreen(
                    isLoading = isLoading,
                    onRegistrarSubmit = { nombre, email, password ->
                        registrarUsuario(nombre, email, password)
                    }
                )
            }
        }
    }

    private fun registrarUsuario(nombre: String, email: String, password: String) {
        isLoading = true

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                actualizarInformacion(nombre, email)
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarInformacion(nombre: String, email: String) {
        val uid = firebaseAuth.uid
        val tiempoR = Constantes.obtenerTiempoD()

        val datosUsuario = HashMap<String, Any>()
        datosUsuario["uid"] = "$uid"
        datosUsuario["nombres"] = nombre
        datosUsuario["email"] = email
        datosUsuario["tiempoR"] = "$tiempoR"
        datosUsuario["proveedor"] = "Email"
        datosUsuario["estado"] = "Online"
        datosUsuario["image"] = ""
        datosUsuario["tutorial"] = false

        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        reference.child(uid!!)
            .setValue(datosUsuario)
            .addOnSuccessListener {
                isLoading = false
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun RegistroEmailScreen(
    isLoading: Boolean,
    onRegistrarSubmit: (nombre: String, email: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rPassword by remember { mutableStateOf("") }

    var passVisible by remember { mutableStateOf(false) }
    var rPassVisible by remember { mutableStateOf(false) }

    var nombreError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passError by remember { mutableStateOf<String?>(null) }
    var rPassError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_form),
            contentDescription = null,
            tint = colors.primaryEmerald,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 24.dp)
        )

        Text(
            text = stringResource(id = R.string.txt_registro).uppercase(),
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                nombreError = null
            },
            label = { Text(stringResource(id = R.string.et_nombres)) },
            isError = nombreError != null,
            supportingText = nombreError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text(stringResource(id = R.string.et_email)) },
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passError = null
            },
            label = { Text("Contraseña") },
            isError = passError != null,
            supportingText = passError?.let { { Text(it) } },
            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passVisible = !passVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (passVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                        ),
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = rPassword,
            onValueChange = {
                rPassword = it
                rPassError = null
            },
            label = { Text("Repetir contraseña") },
            isError = rPassError != null,
            supportingText = rPassError?.let { { Text(it) } },
            visualTransformation = if (rPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { rPassVisible = !rPassVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (rPassVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                        ),
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = {
                val n = nombre.trim()
                val e = email.trim()
                val p = password.trim()
                val rp = rPassword.trim()

                when {
                    n.isEmpty() -> nombreError = "Campo obligatorio"
                    e.isEmpty() -> emailError = "Campo obligatorio"
                    !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> emailError = "Email inválido"
                    p.isEmpty() -> passError = "Campo obligatorio"
                    p.length < 6 -> passError = "Mínimo 6 caracteres"
                    rp != p -> rPassError = "Las contraseñas no coinciden"
                    else -> onRegistrarSubmit(n, e, p)
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryEmerald,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(id = R.string.btn_registrar).uppercase(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistroEmailScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        RegistroEmailScreen(
            isLoading = false,
            onRegistrarSubmit = { _, _, _ -> }
        )
    }
}
