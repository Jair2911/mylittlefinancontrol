package com.example.mylittlefinancontrol

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditarInformacion : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private var nombreActual by mutableStateOf("")
    private var imagenUrl by mutableStateOf("")
    private var isUpdating by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        cargarInformacion()

        setContent {
            MyLittleFinanControlTheme {
                EditarInformacionScreen(
                    nombreInicial = nombreActual,
                    imagenUrl = imagenUrl,
                    isUpdating = isUpdating,
                    onBackClick = { onBackPressedDispatcher.onBackPressed() },
                    onActualizarClick = { nuevoNombre ->
                        actualizarInfo(nuevoNombre)
                    }
                )
            }
        }
    }

    private fun cargarInformacion() {
        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = snapshot.child("nombres").value?.toString().orEmpty()
                    val image = snapshot.child("image").value?.toString().orEmpty()
                    if (nombre != "null") nombreActual = nombre
                    if (image != "null") imagenUrl = image
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun actualizarInfo(nombres: String) {
        isUpdating = true

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = nombres

        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                isUpdating = false
                Toast.makeText(applicationContext, "Información actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isUpdating = false
                Toast.makeText(applicationContext, "error ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun EditarInformacionScreen(
    nombreInicial: String,
    imagenUrl: String,
    isUpdating: Boolean,
    onBackClick: () -> Unit,
    onActualizarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    var nombres by remember(nombreInicial) { mutableStateOf(nombreInicial) }
    var nombreError by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Regresar",
                        tint = colors.textPrimary
                    )
                }

                Text(
                    text = stringResource(id = R.string.txt_titulo_update),
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                HorizontalDivider(
                    color = colors.borderSubtle,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(120.dp)) {
                    AsyncImage(
                        model = imagenUrl.ifBlank { R.drawable.ic_perfil },
                        contentDescription = "Foto de perfil",
                        placeholder = painterResource(id = R.drawable.ic_perfil),
                        error = painterResource(id = R.drawable.ic_perfil),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_editar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.TopEnd)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombres,
                    onValueChange = {
                        nombres = it
                        nombreError = null
                    },
                    label = { Text(stringResource(id = R.string.et_nombres)) },
                    isError = nombreError != null,
                    supportingText = nombreError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(80.dp))

                Button(
                    onClick = {
                        val limpio = nombres.trim()
                        if (limpio.isEmpty()) {
                            nombreError = "Ingrese nombres"
                        } else {
                            onActualizarClick(limpio)
                        }
                    },
                    enabled = !isUpdating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryEmerald,
                        contentColor = Color.White
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.btnActualizar).uppercase(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditarInformacionScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        EditarInformacionScreen(
            nombreInicial = "Juan Rodríguez",
            imagenUrl = "",
            isUpdating = false,
            onBackClick = {},
            onActualizarClick = {}
        )
    }
}
