package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.*
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.EditarInformacion
import com.example.mylittlefinancontrol.OpcionesLoginActivity
import com.example.mylittlefinancontrol.Servicios.*
import com.example.mylittlefinancontrol.databinding.FragmentPerfilBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit

class FragmentPerfil : Fragment() {

    private lateinit var binding: FragmentPerfilBinding
    private lateinit var mContext: Context
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var backupPrefs: SharedPreferences

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPerfilBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        backupPrefs = mContext.getSharedPreferences("BackupPrefs", Context.MODE_PRIVATE)
        
        cargarInformacion()
        configurarTema()
        setupBackupUI()

        binding.btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, OpcionesLoginActivity::class.java))
            activity?.finishAffinity()
        }

        binding.btnActualizar.setOnClickListener {
            startActivity(Intent(mContext, EditarInformacion::class.java))
        }
    }

    private fun setupBackupUI() {
        actualizarEstadoBackup()

        // Selector de Frecuencia
        val opciones = arrayOf("Desactivado", "Diario", "Semanal", "Mensual")
        val adapter = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, opciones)
        binding.autoFrecuenciaBackup.setAdapter(adapter)
        
        val frecActual = backupPrefs.getString("Frecuencia", "Desactivado")
        binding.autoFrecuenciaBackup.setText(frecActual, false)

        binding.autoFrecuenciaBackup.setOnItemClickListener { _, _, position, _ ->
            val seleccion = opciones[position]
            backupPrefs.edit().putString("Frecuencia", seleccion).apply()
            configurarWorkManager(seleccion)
        }

        binding.btnGoogleAuth.setOnClickListener {
            if (backupPrefs.getString("GoogleAccount", null) == null) {
                iniciarVinculacionGoogle()
            } else {
                desvincularGoogle()
            }
        }

        binding.btnBackupNow.setOnClickListener {
            forzarRespaldoManual()
        }

        binding.btnRestore.setOnClickListener {
            confirmarRestauracion()
        }
    }

    private fun actualizarEstadoBackup() {
        val account = backupPrefs.getString("GoogleAccount", null)
        val lastTime = backupPrefs.getLong("LastBackupTime", 0)

        if (account != null) {
            binding.tvGoogleAccount.text = "Vinculado como: $account"
            binding.btnGoogleAuth.text = "Desvincular"
            binding.btnBackupNow.isEnabled = true
            binding.btnRestore.isEnabled = true
        } else {
            binding.tvGoogleAccount.text = "Cuenta: No vinculada"
            binding.btnGoogleAuth.text = "Vincular"
            binding.btnBackupNow.isEnabled = false
            binding.btnRestore.isEnabled = false
        }

        if (lastTime > 0) {
            binding.tvLastBackup.text = "Última copia: ${Constantes.formatoFecha(lastTime)}"
        } else {
            binding.tvLastBackup.text = "Última copia: Nunca"
        }
    }

    private fun iniciarVinculacionGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        startActivityForResult(client.signInIntent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    backupPrefs.edit().putString("GoogleAccount", account.email).apply()
                    Toast.makeText(mContext, "Cuenta vinculada: ${account.email}", Toast.LENGTH_SHORT).show()
                    actualizarEstadoBackup()
                }
            } catch (e: Exception) {
                Toast.makeText(mContext, "Error al vincular: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun desvincularGoogle() {
        backupPrefs.edit().remove("GoogleAccount").apply()
        actualizarEstadoBackup()
        WorkManager.getInstance(mContext).cancelUniqueWork("AutoBackup")
        Toast.makeText(mContext, "Cuenta desvinculada", Toast.LENGTH_SHORT).show()
    }

    private fun forzarRespaldoManual() {
        val accountName = backupPrefs.getString("GoogleAccount", null) ?: return
        
        binding.btnBackupNow.isEnabled = false
        Toast.makeText(mContext, "Iniciando respaldo...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.IO) {
            try {
                val driveService = GoogleDriveService(mContext, accountName)
                val backupManager = BackupManager(mContext)
                val dbFile = backupManager.getDatabaseFile()

                if (dbFile != null) {
                    val fileId = driveService.uploadFile(dbFile, "finanzas_backup.db")
                    withContext(Dispatchers.Main) {
                        if (fileId != null) {
                            backupPrefs.edit().putLong("LastBackupTime", System.currentTimeMillis()).apply()
                            actualizarEstadoBackup()
                            Toast.makeText(mContext, "Respaldo exitoso", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(mContext, "Error al subir archivo", Toast.LENGTH_SHORT).show()
                        }
                        binding.btnBackupNow.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(mContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnBackupNow.isEnabled = true
                }
            }
        }
    }

    private fun confirmarRestauracion() {
        AlertDialog.Builder(mContext)
            .setTitle("Restaurar Datos")
            .setMessage("¿Estás seguro? Se sobreescribirán todos los datos actuales con la copia de la nube. La app se reiniciará.")
            .setPositiveButton("Restaurar") { _, _ -> ejecutarRestauracion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarRestauracion() {
        val accountName = backupPrefs.getString("GoogleAccount", null) ?: return
        
        Toast.makeText(mContext, "Descargando copia...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.IO) {
            try {
                val driveService = GoogleDriveService(mContext, accountName)
                val backupManager = BackupManager(mContext)
                
                val tempFile = File(mContext.cacheDir, "temp_restore.db")
                val success = driveService.downloadFile("finanzas_backup.db", tempFile)

                if (success) {
                    val restored = backupManager.restoreDatabase(tempFile)
                    withContext(Dispatchers.Main) {
                        if (restored) {
                            Toast.makeText(mContext, "Restauración completada. Reiniciando...", Toast.LENGTH_LONG).show()
                            // Reiniciar app
                            val intent = mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                        } else {
                            Toast.makeText(mContext, "Error al sobreescribir base de datos", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(mContext, "No se encontró respaldo en la nube", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(mContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun configurarWorkManager(frecuencia: String) {
        val workManager = WorkManager.getInstance(mContext)
        workManager.cancelUniqueWork("AutoBackup")

        if (frecuencia == "Desactivado") return

        val interval = when (frecuencia) {
            "Diario" -> 1L to TimeUnit.DAYS
            "Semanal" -> 7L to TimeUnit.DAYS
            "Mensual" -> 30L to TimeUnit.DAYS
            else -> return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(interval.first, interval.second)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "AutoBackup",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
        
        Toast.makeText(mContext, "Respaldo automático: $frecuencia", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun configurarTema() {
        val sp = mContext.getSharedPreferences("TemaPrefs", Context.MODE_PRIVATE)
        val esOscuro = sp.getBoolean("EsTemaOscuro", false)

        binding.swTema.isChecked = esOscuro

        binding.swTema.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                sp.edit().putBoolean("EsTemaOscuro", true).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                sp.edit().putBoolean("EsTemaOscuro", false).apply()
            }
        }
    }

    private fun cargarInformacion() {
        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val email = "${snapshot.child("email").value}"
                    val proveedor = "${snapshot.child("proveedor").value}"
                    var tiempoR = "${snapshot.child("tiempoR").value}"

                    if (tiempoR == "null") {
                        tiempoR = "0"
                    }

                    val fecha = Constantes.formatoFecha(tiempoR.toLong())

                    binding.tvNombres.text = nombres
                    binding.tvEmail.text = email
                    binding.tvProveedor.text = proveedor
                    binding.tvMienbroDesde.text = fecha
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
