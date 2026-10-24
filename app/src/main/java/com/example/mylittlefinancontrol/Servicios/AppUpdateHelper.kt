package com.example.mylittlefinancontrol.Servicios

import android.app.Dialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.mylittlefinancontrol.BuildConfig
import com.example.mylittlefinancontrol.R
import com.google.android.material.button.MaterialButton
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
                    // Si el nodo no existe en Firebase, crearlo automáticamente
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
        val dialog = Dialog(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_actualizacion_app, null)
        dialog.setContentView(dialogView)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(!obligatoria)

        val tvTitulo = dialogView.findViewById<android.widget.TextView>(R.id.tv_titulo_actualizacion)
        val tvNovedades = dialogView.findViewById<android.widget.TextView>(R.id.tv_novedades_actualizacion)
        val btnActualizar = dialogView.findViewById<MaterialButton>(R.id.btn_actualizar_ahora)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_mas_tarde)

        tvTitulo.text = "¡Nueva versión v$versionName disponible!"
        tvNovedades.text = novedades

        if (obligatoria) {
            btnCancelar.visibility = android.view.View.GONE
        } else {
            btnCancelar.setOnClickListener {
                dialog.dismiss()
            }
        }

        btnActualizar.setOnClickListener {
            if (urlApk.isBlank()) {
                Toast.makeText(context, "La URL de descarga de la APK no está disponible aún en Firebase.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Por favor autoriza la instalación de aplicaciones desconocidas para actualizar DABAFINN", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return@setOnClickListener
            }

            descargarEInstalarApk(context, urlApk, versionName)
            if (!obligatoria) {
                dialog.dismiss()
            }
        }

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
