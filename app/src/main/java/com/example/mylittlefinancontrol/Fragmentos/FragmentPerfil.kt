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
    private var pendingAction: String? = null

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
        setupDemoDataUI()

        binding.btnVolverPerfil.setOnClickListener {
            (activity as? com.example.mylittlefinancontrol.MainActivity)?.verFragmentoInicio() ?: activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.btnAyudaPerfil.setOnClickListener {
            Toast.makeText(mContext, "MyLittleFinanControl v2.4 - Centro de Ayuda", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, OpcionesLoginActivity::class.java))
            activity?.finishAffinity()
        }

        binding.btnActualizar.setOnClickListener {
            startActivity(Intent(mContext, EditarInformacion::class.java))
        }
    }

    private fun setupDemoDataUI() {
        val db = com.example.mylittlefinancontrol.NotasDatabaseHelper.getInstance(mContext)

        binding.btnCargarDemo.setOnClickListener {
            AlertDialog.Builder(mContext)
                .setTitle("Cargar Datos de Demostración")
                .setMessage("¿Deseas poblar todos los módulos con datos de prueba realistas (Movimientos, Gastos Fijos, Deudas y Metas de Ahorro)?")
                .setPositiveButton("Cargar Datos") { _, _ ->
                    scope.launch(Dispatchers.IO) {
                        DemoDataHelper.cargarDatosPrueba(mContext, db)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(mContext, "¡Datos de prueba cargados en todos los módulos!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnLimpiarDemo.setOnClickListener {
            AlertDialog.Builder(mContext)
                .setTitle("Limpiar Base de Datos")
                .setMessage("¿Estás seguro de vaciar todos los registros para comenzar con una base de datos limpia?")
                .setPositiveButton("Limpiar Todo") { _, _ ->
                    scope.launch(Dispatchers.IO) {
                        DemoDataHelper.limpiarTodosLosDatos(mContext, db)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(mContext, "Base de datos limpiada correctamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
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
            val account = backupPrefs.getString("GoogleAccount", null)
            if (account != null) {
                forzarRespaldoManual()
            } else {
                pendingAction = "BACKUP"
                Toast.makeText(mContext, "Iniciando vinculación con Google Drive...", Toast.LENGTH_SHORT).show()
                iniciarVinculacionGoogle()
            }
        }

        binding.btnRestore.setOnClickListener {
            val account = backupPrefs.getString("GoogleAccount", null)
            if (account != null) {
                confirmarRestauracion()
            } else {
                pendingAction = "RESTORE"
                Toast.makeText(mContext, "Iniciando vinculación con Google Drive para restaurar...", Toast.LENGTH_SHORT).show()
                iniciarVinculacionGoogle()
            }
        }
    }

    private fun actualizarEstadoBackup() {
        var account = backupPrefs.getString("GoogleAccount", null)
        if (account.isNullOrBlank() || account == "null") {
            account = null
        }

        // Si no se ha guardado explícitamente pero hay sesión activa de Google con permisos de Drive, autodetectar
        if (account == null) {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(mContext)
            val driveScope = Scope(DriveScopes.DRIVE_APPDATA)
            if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, driveScope)) {
                val email = lastAccount.email ?: lastAccount.account?.name
                if (!email.isNullOrBlank() && email != "null") {
                    account = email
                    backupPrefs.edit().putString("GoogleAccount", account).apply()
                }
            }
        }

        val lastTime = backupPrefs.getLong("LastBackupTime", 0)

        if (!account.isNullOrBlank()) {
            binding.tvGoogleAccount.text = "Vinculado como: $account"
            binding.btnGoogleAuth.text = "Desvincular"
        } else {
            val userEmail = firebaseAuth.currentUser?.email
            if (!userEmail.isNullOrBlank()) {
                binding.tvGoogleAccount.text = "Cuenta: $userEmail (Drive no vinculado)"
            } else {
                binding.tvGoogleAccount.text = "Cuenta: No vinculada"
            }
            binding.btnGoogleAuth.text = "Vincular Drive"
        }

        // MANTENER SIEMPRE LOS BOTONES ACTIVOS PARA QUE EL USUARIO PUEDA PULSARLOS
        binding.btnBackupNow.isEnabled = true
        binding.btnRestore.isEnabled = true

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
        client.signOut().addOnCompleteListener {
            startActivityForResult(client.signInIntent, 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val email = account?.email ?: account?.account?.name
                if (!email.isNullOrBlank() && email != "null") {
                    backupPrefs.edit().putString("GoogleAccount", email).apply()
                    Toast.makeText(mContext, "Cuenta de Drive vinculada: $email", Toast.LENGTH_SHORT).show()
                    actualizarEstadoBackup()

                    val action = pendingAction
                    pendingAction = null
                    if (action == "RESTORE") {
                        confirmarRestauracion()
                    } else if (action == "BACKUP") {
                        forzarRespaldoManual()
                    }
                } else {
                    pendingAction = null
                    Toast.makeText(mContext, "No se pudo obtener el correo de la cuenta de Google", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                pendingAction = null
                Toast.makeText(mContext, "Error al vincular Drive: ${e.message}", Toast.LENGTH_LONG).show()
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
        var accountName = backupPrefs.getString("GoogleAccount", null)
        if (accountName.isNullOrBlank() || accountName == "null") {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(mContext)
            val email = lastAccount?.email ?: lastAccount?.account?.name
            if (!email.isNullOrBlank() && email != "null") {
                accountName = email
                backupPrefs.edit().putString("GoogleAccount", accountName).apply()
            }
        }

        if (accountName.isNullOrBlank() || accountName == "null") {
            pendingAction = "BACKUP"
            iniciarVinculacionGoogle()
            return
        }
        
        binding.btnBackupNow.isEnabled = false
        
        val progressDialog = android.app.ProgressDialog(mContext)
        progressDialog.setTitle("Respaldando...")
        progressDialog.setMessage("Subiendo datos a Google Drive, por favor espera.")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        scope.launch(Dispatchers.IO) {
            try {
                val driveService = GoogleDriveService(mContext, accountName)
                val backupManager = BackupManager(mContext)
                val dbFile = backupManager.getDatabaseFile()

                if (dbFile != null) {
                    val fileId = driveService.uploadFile(dbFile, "finanzas_backup.db")
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        if (fileId != null) {
                            backupPrefs.edit().putLong("LastBackupTime", System.currentTimeMillis()).apply()
                            actualizarEstadoBackup()
                            Toast.makeText(mContext, "Respaldo exitoso en Google Drive", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(mContext, "Error al subir archivo a Drive", Toast.LENGTH_SHORT).show()
                        }
                        binding.btnBackupNow.isEnabled = true
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(mContext, "No hay base de datos local para respaldar", Toast.LENGTH_SHORT).show()
                        binding.btnBackupNow.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(mContext, "Error en respaldo: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnBackupNow.isEnabled = true
                }
            }
        }
    }

    private fun confirmarRestauracion() {
        AlertDialog.Builder(mContext)
            .setTitle("Restaurar Datos desde la Nube")
            .setMessage("¿Estás seguro? Se reemplazarán todos los datos actuales de la app con la copia de seguridad guardada en tu Google Drive. La app se reiniciará.")
            .setPositiveButton("Restaurar") { _, _ -> ejecutarRestauracion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarRestauracion() {
        var accountName = backupPrefs.getString("GoogleAccount", null)
        if (accountName.isNullOrBlank() || accountName == "null") {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(mContext)
            val email = lastAccount?.email ?: lastAccount?.account?.name
            if (!email.isNullOrBlank() && email != "null") {
                accountName = email
                backupPrefs.edit().putString("GoogleAccount", accountName).apply()
            }
        }

        if (accountName.isNullOrBlank() || accountName == "null") {
            pendingAction = "RESTORE"
            iniciarVinculacionGoogle()
            return
        }
        
        val progressDialog = android.app.ProgressDialog(mContext)
        progressDialog.setTitle("Restaurando...")
        progressDialog.setMessage("Descargando datos desde Google Drive, por favor espera.")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        scope.launch(Dispatchers.IO) {
            try {
                val driveService = GoogleDriveService(mContext, accountName)
                val backupManager = BackupManager(mContext)
                
                val tempFile = File(mContext.cacheDir, "temp_restore.db")
                val success = driveService.downloadFile("finanzas_backup.db", tempFile)

                if (success) {
                    val restored = backupManager.restoreDatabase(tempFile)
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        if (restored) {
                            Toast.makeText(mContext, "¡Restauración completada con éxito! Reiniciando la app...", Toast.LENGTH_LONG).show()
                            
                            // Usar Process.killProcess para un reinicio duro forzado sin bloquear la UI principal
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                val intent = mContext.packageManager.getLaunchIntentForPackage(mContext.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                mContext.startActivity(intent)
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 500)
                        } else {
                            Toast.makeText(mContext, "Error al sobreescribir la base de datos local", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(mContext, "No se encontró ningún archivo 'finanzas_backup.db' en la cuenta de Google Drive: $accountName", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(mContext, "Error al conectar con Drive: ${e.message}", Toast.LENGTH_LONG).show()
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

        // 1. Programar tarea periódica
        val periodicRequest = PeriodicWorkRequestBuilder<BackupWorker>(interval.first, interval.second)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "AutoBackup",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicRequest
        )

        // 2. Ejecutar un respaldo inmediato si hay cuenta vinculada
        val currentAcc = backupPrefs.getString("GoogleAccount", null)
        if (!currentAcc.isNullOrBlank() && currentAcc != "null") {
            val immediateRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(constraints)
                .addTag("ImmediateBackup")
                .build()
            
            workManager.enqueue(immediateRequest)
            Toast.makeText(mContext, "Sincronización inicial en curso...", Toast.LENGTH_SHORT).show()
        }
        
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

    override fun onResume() {
        super.onResume()
        cargarInformacion()
        actualizarEstadoBackup()
    }

    private fun cargarInformacion() {
        val user = firebaseAuth.currentUser
        val userEmail = user?.email ?: "correo@ejemplo.com"
        val userName = user?.displayName?.ifBlank { null } ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val provider = if (user?.providerData?.any { it.providerId.contains("google", true) } == true) "Google" else "Email"

        binding.tvNombres.text = userName
        binding.tvEmail.text = userEmail
        binding.tvProveedor.text = provider
        binding.tvMienbroDesde.text = "Miembro activo"

        val inicialesFallback = userName
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.first().uppercase() }
            .take(2)
            .joinToString("")
        binding.tvAvatarIniciales.text = if (inicialesFallback.isNotEmpty()) inicialesFallback else "JR"

        val uid = firebaseAuth.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = snapshot.child("nombres").value?.toString()
                    val email = snapshot.child("email").value?.toString()
                    val prov = snapshot.child("proveedor").value?.toString()
                    var tiempoR = snapshot.child("tiempoR").value?.toString() ?: "0"

                    if (tiempoR == "null") tiempoR = "0"
                    val tiempoLong = tiempoR.toLongOrNull() ?: 0L
                    val fecha = if (tiempoLong > 0) Constantes.formatoFecha(tiempoLong) else "Reciente"

                    val nombreFinal = if (!nombres.isNullOrBlank() && nombres != "null") nombres else userName
                    val emailFinal = if (!email.isNullOrBlank() && email != "null") email else userEmail
                    val provFinal = if (!prov.isNullOrBlank() && prov != "null") prov else provider

                    binding.tvNombres.text = nombreFinal
                    binding.tvEmail.text = emailFinal
                    binding.tvProveedor.text = provFinal
                    binding.tvMienbroDesde.text = "Miembro desde: $fecha"

                    val iniciales = nombreFinal
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .map { it.first().uppercase() }
                        .take(2)
                        .joinToString("")
                    binding.tvAvatarIniciales.text = if (iniciales.isNotEmpty()) iniciales else "JR"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
