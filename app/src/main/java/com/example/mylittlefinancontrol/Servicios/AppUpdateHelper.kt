package com.example.mylittlefinancontrol.Servicios

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.mylittlefinancontrol.BuildConfig
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File

object AppUpdateHelper {

    private const val NODE_ACTUALIZACION = "configuracion_app/actualizacion"

    data class ActualizacionConfig(
        var versionCode: Int = 2,
        var versionName: String = "1.1",
        var novedades: String = "Versión base de DABAFINN.",
        var urlApk: String = "",
        var obligatoria: Boolean = false
    )

    fun verificarActualizacion(context: Context, esManual: Boolean = false) {
        val ref = FirebaseDatabase.getInstance().getReference(NODE_ACTUALIZACION)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val configDefecto = ActualizacionConfig(
                        versionCode = BuildConfig.VERSION_CODE,
                        versionName = BuildConfig.VERSION_NAME,
                        novedades = "Versión base inicial de DABAFINN con control financiero inteligente.",
                        urlApk = "",
                        obligatoria = false
                    )
                    ref.setValue(configDefecto)
                    if (esManual) {
                        Toast.makeText(context, "¡Ya tienes la versión más reciente instalada! (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val versionCode = snapshot.child("versionCode").getValue(Int::class.java) ?: 0
                val versionName = snapshot.child("versionName").getValue(String::class.java) ?: "1.0"
                val novedades = snapshot.child("novedades").getValue(String::class.java) ?: "Nuevas mejoras de rendimiento y corrección de errores."
                val urlApk = snapshot.child("urlApk").getValue(String::class.java) ?: ""
                val obligatoria = snapshot.child("obligatoria").getValue(Boolean::class.java) ?: false

                val installedCode = BuildConfig.VERSION_CODE

                if (versionCode > installedCode) {
                    mostrarModalActualizacion(
                        context = context,
                        versionName = versionName,
                        novedades = novedades,
                        urlApk = urlApk,
                        obligatoria = obligatoria
                    )
                } else if (esManual) {
                    Toast.makeText(context, "¡Ya tienes la versión más reciente instalada! (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (esManual) {
                    Toast.makeText(context, "Error al buscar actualizaciones: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun mostrarModalActualizacion(
        context: Context,
        versionName: String,
        novedades: String,
        urlApk: String,
        obligatoria: Boolean
    ) {
        val dialog = ComponentDialog(context)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(!obligatoria)

        val composeView = ComposeView(context).apply {
            setContent {
                MyLittleFinanControlTheme {
                    ModalActualizacionCompose(
                        versionName = versionName,
                        novedades = novedades,
                        obligatoria = obligatoria,
                        onActualizarAhora = {
                            if (urlApk.isBlank()) {
                                Toast.makeText(context, "La URL de descarga de la APK no está disponible aún en Firebase.", Toast.LENGTH_LONG).show()
                                return@ModalActualizacionCompose
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                Toast.makeText(context, "Por favor autoriza la instalación de aplicaciones desconocidas para actualizar DABAFINN", Toast.LENGTH_LONG).show()
                                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                return@ModalActualizacionCompose
                            }

                            descargarEInstalarApk(context, urlApk, versionName)
                            if (!obligatoria) {
                                dialog.dismiss()
                            }
                        },
                        onMasTarde = { dialog.dismiss() }
                    )
                }
            }
        }

        dialog.setContentView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        dialog.show()
    }

    private fun descargarEInstalarApk(context: Context, urlApk: String, versionName: String) {
        try {
            val fileName = "DABAFINN_update.apk"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (file.exists()) {
                file.delete()
            }

            val request = DownloadManager.Request(Uri.parse(urlApk)).apply {
                setTitle("Descargando actualización de DABAFINN v$versionName")
                setDescription("Descargando nuevo paquete APK...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, "Descargando actualización... Revisa la barra de notificaciones.", Toast.LENGTH_LONG).show()

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(cntx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        try {
                            cntx?.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        if (cntx != null) {
                            abrirInstalador(cntx, file)
                        }
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(context, onCompleteReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar la descarga: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun abrirInstalador(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "No se encontró el archivo de actualización descargado.", Toast.LENGTH_SHORT).show()
                return
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)

        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir el instalador de APK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun ModalActualizacionCompose(
    versionName: String,
    novedades: String,
    obligatoria: Boolean,
    onActualizarAhora: () -> Unit,
    onMasTarde: () -> Unit
) {
    val colors = DabaTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.ghostBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_download),
                    contentDescription = null,
                    tint = colors.primaryEmerald,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "¡Nueva versión v$versionName disponible!",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = novedades,
                color = colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onActualizarAhora,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
            ) {
                Text("Actualizar ahora", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (!obligatoria) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onMasTarde,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Más tarde", color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
