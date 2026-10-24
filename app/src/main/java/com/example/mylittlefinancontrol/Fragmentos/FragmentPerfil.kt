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
import java.util.UUID
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
        setupMetodosPagoUI()

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

    private fun setupMetodosPagoUI() {
        binding.btnAgregarMetodoPago.setOnClickListener {
            mostrarDialogoMetodoPago(null)
        }
        cargarMetodosPago()
    }

    private fun cargarMetodosPago() {
        val db = com.example.mylittlefinancontrol.NotasDatabaseHelper.getInstance(mContext)
        val uid = firebaseAuth.uid

        // 1. Descargar desde Firebase y consolidar en SQLite
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("metodos_pago")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (mpSnapshot in snapshot.children) {
                            val id = mpSnapshot.child("id").getValue(String::class.java) ?: continue
                            val nombre = mpSnapshot.child("nombre").getValue(String::class.java) ?: ""
                            val tipo = mpSnapshot.child("tipo").getValue(String::class.java) ?: "EFECTIVO"
                            val ultimos4 = mpSnapshot.child("ultimos4").getValue(String::class.java) ?: ""
                            val esPredeterminado = mpSnapshot.child("esPredeterminado").getValue(Boolean::class.java) ?: false

                            val mp = com.example.mylittlefinancontrol.Modelos.MetodoPago(id, nombre, tipo, ultimos4, esPredeterminado)
                            db.insertarOActualizarMetodoPago(mp)
                        }
                        renderizarMetodosPago(db.obtenerMetodosPago())
                    }
                    override fun onCancelled(error: DatabaseError) {
                        renderizarMetodosPago(db.obtenerMetodosPago())
                    }
                })
        } else {
            renderizarMetodosPago(db.obtenerMetodosPago())
        }
    }

    private fun renderizarMetodosPago(lista: List<com.example.mylittlefinancontrol.Modelos.MetodoPago>) {
        if (!isAdded) return
        binding.llListaMetodosPago.removeAllViews()

        val inflater = LayoutInflater.from(mContext)
        for (mp in lista) {
            val view = inflater.inflate(com.example.mylittlefinancontrol.R.layout.item_categoria_resumen, binding.llListaMetodosPago, false)

            val tvNombre = view.findViewById<android.widget.TextView>(com.example.mylittlefinancontrol.R.id.tv_nombre_categoria)
            val tvSub = view.findViewById<android.widget.TextView>(com.example.mylittlefinancontrol.R.id.tv_conteo_transacciones)
            val icon = view.findViewById<android.widget.ImageView>(com.example.mylittlefinancontrol.R.id.iv_icono_categoria)
            val iconContainer = view.findViewById<android.widget.FrameLayout>(com.example.mylittlefinancontrol.R.id.fl_icono_container)
            val tvMonto = view.findViewById<android.widget.TextView>(com.example.mylittlefinancontrol.R.id.tv_monto_categoria)
            tvMonto?.visibility = View.GONE
            val tvPorcentaje = view.findViewById<android.widget.TextView>(com.example.mylittlefinancontrol.R.id.tv_porcentaje_categoria)
            tvPorcentaje?.visibility = View.GONE
            val pb = view.findViewById<android.widget.ProgressBar>(com.example.mylittlefinancontrol.R.id.pb_porcentaje_categoria)
            pb?.visibility = View.GONE

            tvNombre.text = mp.nombreFormateado()
            tvSub.text = if (mp.esPredeterminado) "Predeterminado" else mp.tipo.replace("_", " ")

            val colorAcento = androidx.core.content.ContextCompat.getColor(mContext, com.example.mylittlefinancontrol.R.color.primary_emerald)
            val bgAcento = androidx.core.content.ContextCompat.getColor(mContext, com.example.mylittlefinancontrol.R.color.ghost_bg)

            when {
                mp.tipo == "EFECTIVO" -> {
                    icon.setImageResource(com.example.mylittlefinancontrol.R.drawable.ic_account_balance_wallet)
                }
                mp.tipo.contains("TARJETA") -> {
                    icon.setImageResource(com.example.mylittlefinancontrol.R.drawable.ic_credit_card)
                }
                else -> {
                    icon.setImageResource(com.example.mylittlefinancontrol.R.drawable.ic_payments)
                }
            }

            if (mp.esPredeterminado) {
                icon.imageTintList = android.content.res.ColorStateList.valueOf(colorAcento)
                iconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(bgAcento)
                tvSub.setTextColor(colorAcento)
            } else {
                val colorMuted = androidx.core.content.ContextCompat.getColor(mContext, com.example.mylittlefinancontrol.R.color.text_secondary)
                val bgMuted = androidx.core.content.ContextCompat.getColor(mContext, com.example.mylittlefinancontrol.R.color.surface_container_high)
                icon.imageTintList = android.content.res.ColorStateList.valueOf(colorMuted)
                iconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(bgMuted)
                tvSub.setTextColor(colorMuted)
            }

            view.setOnClickListener { mostrarDialogoMetodoPago(mp) }
            binding.llListaMetodosPago.addView(view)
        }
    }

    private fun mostrarDialogoMetodoPago(mpExistente: com.example.mylittlefinancontrol.Modelos.MetodoPago?) {
        val dialogView = LayoutInflater.from(mContext).inflate(com.example.mylittlefinancontrol.R.layout.dialog_metodo_pago, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(mContext, com.example.mylittlefinancontrol.R.style.BottomSheetDialogTheme)
        dialog.setContentView(dialogView)

        val autoTipo = dialogView.findViewById<android.widget.AutoCompleteTextView>(com.example.mylittlefinancontrol.R.id.auto_tipo_metodo_pago)
        val etNombre = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.mylittlefinancontrol.R.id.et_nombre_metodo)
        val etUltimos4 = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.mylittlefinancontrol.R.id.et_ultimos4)
        val tilUltimos4 = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(com.example.mylittlefinancontrol.R.id.til_ultimos4)
        val tvAdvertencia = dialogView.findViewById<android.widget.TextView>(com.example.mylittlefinancontrol.R.id.tv_advertencia_seguridad)
        val swPredeterminado = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(com.example.mylittlefinancontrol.R.id.sw_predeterminado)
        val btnGuardar = dialogView.findViewById<android.view.View>(com.example.mylittlefinancontrol.R.id.btn_guardar_metodo)
        val btnCancelar = dialogView.findViewById<android.view.View>(com.example.mylittlefinancontrol.R.id.btn_cancelar_metodo)
        val btnEliminar = dialogView.findViewById<android.view.View>(com.example.mylittlefinancontrol.R.id.btn_eliminar_metodo)

        val tipos = arrayOf("EFECTIVO", "TARJETA_DEBITO", "TARJETA_CREDITO", "CUENTA_BANCARIA", "BILLETERA_DIGITAL")
        autoTipo.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, tipos))

        if (mpExistente != null) {
            autoTipo.setText(mpExistente.tipo, false)
            etNombre.setText(mpExistente.nombre)
            etUltimos4.setText(mpExistente.ultimos4)
            swPredeterminado.isChecked = mpExistente.esPredeterminado

            if (mpExistente.id != "efectivo_default") {
                btnEliminar.visibility = View.VISIBLE
            }
        } else {
            autoTipo.setText("TARJETA_DEBITO", false)
        }

        fun updateUI() {
            val isEfectivo = autoTipo.text.toString() == "EFECTIVO"
            tilUltimos4.visibility = if (isEfectivo) View.GONE else View.VISIBLE
            tvAdvertencia.visibility = if (isEfectivo) View.GONE else View.VISIBLE
            if (isEfectivo) etUltimos4.setText("")
        }

        autoTipo.setOnItemClickListener { _, _, _, _ -> updateUI() }
        updateUI()

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val tipo = autoTipo.text.toString()
            val ultimos4 = etUltimos4.text.toString().trim()
            val esPred = swPredeterminado.isChecked

            if (nombre.isBlank()) {
                Toast.makeText(mContext, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = firebaseAuth.uid
            val db = com.example.mylittlefinancontrol.NotasDatabaseHelper.getInstance(mContext)

            val mp = mpExistente ?: com.example.mylittlefinancontrol.Modelos.MetodoPago(
                id = java.util.UUID.randomUUID().toString()
            )
            mp.nombre = nombre
            mp.tipo = tipo
            mp.ultimos4 = ultimos4
            mp.esPredeterminado = esPred

            db.insertarOActualizarMetodoPago(mp)

            if (uid != null) {
                FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("metodos_pago")
                    .child(mp.id).setValue(mp)
            }

            cargarMetodosPago()
            dialog.dismiss()
        }

        btnEliminar.setOnClickListener {
            if (mpExistente != null && mpExistente.id != "efectivo_default") {
                val db = com.example.mylittlefinancontrol.NotasDatabaseHelper.getInstance(mContext)
                db.eliminarMetodoPago(mpExistente.id)
                val uid = firebaseAuth.uid
                if (uid != null) {
                    FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("metodos_pago")
                        .child(mpExistente.id).removeValue()
                }
                cargarMetodosPago()
                dialog.dismiss()
            }
        }

        dialog.show()
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
