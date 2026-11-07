package com.example.mylittlefinancontrol.Fragmentos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.*
import com.example.mylittlefinancontrol.BuildConfig
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.EditarInformacion
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.Modelos.MetodoPago
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.OpcionesLoginActivity
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.Servicios.AppUpdateHelper
import com.example.mylittlefinancontrol.Servicios.BackupManager
import com.example.mylittlefinancontrol.Servicios.BackupWorker
import com.example.mylittlefinancontrol.Servicios.DemoDataHelper
import com.example.mylittlefinancontrol.Servicios.GoogleDriveService
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class FragmentPerfil : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyLittleFinanControlTheme {
                    PerfilStatefulScreen(
                        onBack = {
                            (activity as? MainActivity)?.verFragmentoInicio()
                                ?: activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PerfilStatefulScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val db = remember { NotasDatabaseHelper.getInstance(context) }
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val backupPrefs = remember { context.getSharedPreferences("BackupPrefs", Context.MODE_PRIVATE) }
    val temaPrefs = remember { context.getSharedPreferences("TemaPrefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var nombres by remember { mutableStateOf("Usuario") }
    var email by remember { mutableStateOf("correo@ejemplo.com") }
    var proveedor by remember { mutableStateOf("Google") }
    var miembroDesde by remember { mutableStateOf("Miembro activo") }
    var iniciales by remember { mutableStateOf("JR") }

    var esTemaOscuro by remember { mutableStateOf(temaPrefs.getBoolean("EsTemaOscuro", false)) }
    var metodosPago by remember { mutableStateOf<List<MetodoPago>>(emptyList()) }
    var googleAccount by remember { mutableStateOf<String?>(null) }
    var lastBackupTexto by remember { mutableStateOf("Última copia: Nunca") }
    var frecuenciaBackup by remember { mutableStateOf(backupPrefs.getString("Frecuencia", "Desactivado") ?: "Desactivado") }
    var mostrarCardDemo by remember { mutableStateOf(true) }

    var isBusyProgress by remember { mutableStateOf<String?>(null) }
    var pendingDriveAction by remember { mutableStateOf<String?>(null) }

    var showDialogAyuda by remember { mutableStateOf(false) }
    var showDialogMetodoPago by remember { mutableStateOf(false) }
    var metodoEnEdicion by remember { mutableStateOf<MetodoPago?>(null) }
    var showConfirmCargarDemo by remember { mutableStateOf(false) }
    var showConfirmLimpiarDemo by remember { mutableStateOf(false) }
    var showConfirmRestore by remember { mutableStateOf(false) }

    fun validarVisibilidadDatosPrueba() {
        scope.launch {
            val debeOcultar = withContext(Dispatchers.IO) {
                val tieneDatosReales = db.tieneDatosReales()
                val acc = backupPrefs.getString("GoogleAccount", null)
                val lastBackupTime = backupPrefs.getLong("LastBackupTime", 0L)
                val frec = backupPrefs.getString("Frecuencia", "Desactivado")
                val cuentaVinculada = !acc.isNullOrBlank() && acc != "null"
                val tieneCopiaConfigurada = cuentaVinculada &&
                    (lastBackupTime > 0L || (!frec.isNullOrBlank() && frec != "Desactivado"))
                tieneDatosReales && tieneCopiaConfigurada
            }
            mostrarCardDemo = !debeOcultar
        }
    }

    fun actualizarEstadoBackup() {
        var account = backupPrefs.getString("GoogleAccount", null)
        if (account.isNullOrBlank() || account == "null") account = null

        if (account == null) {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            val driveScope = Scope(DriveScopes.DRIVE_APPDATA)
            if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, driveScope)) {
                val mail = lastAccount.email ?: lastAccount.account?.name
                if (!mail.isNullOrBlank() && mail != "null") {
                    account = mail
                    backupPrefs.edit().putString("GoogleAccount", account).apply()
                }
            }
        }
        googleAccount = account
        val lastTime = backupPrefs.getLong("LastBackupTime", 0L)
        lastBackupTexto = if (lastTime > 0) "Última copia: ${Constantes.formatoFecha(lastTime)}" else "Última copia: Nunca"
        validarVisibilidadDatosPrueba()
    }

    fun cargarMetodosPago() {
        val uid = firebaseAuth.uid
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
                            val mp = MetodoPago(id, nombre, tipo, ultimos4, esPredeterminado)
                            db.insertarOActualizarMetodoPago(mp)
                        }
                        metodosPago = db.obtenerMetodosPago()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        metodosPago = db.obtenerMetodosPago()
                    }
                })
        } else {
            metodosPago = db.obtenerMetodosPago()
        }
    }

    fun cargarInformacionUsuario() {
        val user = firebaseAuth.currentUser
        val userEmail = user?.email ?: "correo@ejemplo.com"
        val userName = user?.displayName?.ifBlank { null } ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val prov = if (user?.providerData?.any { it.providerId.contains("google", true) } == true) "Google" else "Email"

        nombres = userName
        email = userEmail
        proveedor = prov
        miembroDesde = "Miembro activo"
        val iniFallback = userName.split(" ").filter { it.isNotBlank() }.map { it.first().uppercase() }.take(2).joinToString("")
        iniciales = iniFallback.ifEmpty { "JR" }

        val uid = firebaseAuth.uid ?: return
        FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nom = snapshot.child("nombres").value?.toString()
                    val em = snapshot.child("email").value?.toString()
                    val pr = snapshot.child("proveedor").value?.toString()
                    var tiempoR = snapshot.child("tiempoR").value?.toString() ?: "0"
                    if (tiempoR == "null") tiempoR = "0"
                    val tiempoLong = tiempoR.toLongOrNull() ?: 0L
                    val fecha = if (tiempoLong > 0) Constantes.formatoFecha(tiempoLong) else "Reciente"

                    val nombreFinal = if (!nom.isNullOrBlank() && nom != "null") nom else userName
                    nombres = nombreFinal
                    email = if (!em.isNullOrBlank() && em != "null") em else userEmail
                    proveedor = if (!pr.isNullOrBlank() && pr != "null") pr else prov
                    miembroDesde = "Miembro desde: $fecha"
                    val ini = nombreFinal.split(" ").filter { it.isNotBlank() }.map { it.first().uppercase() }.take(2).joinToString("")
                    iniciales = ini.ifEmpty { "JR" }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun forzarRespaldoManual(accName: String) {
        isBusyProgress = "Subiendo datos a Google Drive, por favor espera..."
        scope.launch(Dispatchers.IO) {
            try {
                val driveService = GoogleDriveService(context, accName)
                val backupManager = BackupManager(context)
                val dbFile = backupManager.getDatabaseFile()
                if (dbFile != null) {
                    val fileId = driveService.uploadFile(dbFile, "finanzas_backup.db")
                    withContext(Dispatchers.Main) {
                        isBusyProgress = null
                        if (fileId != null) {
                            backupPrefs.edit().putLong("LastBackupTime", System.currentTimeMillis()).apply()
                            actualizarEstadoBackup()
                            Toast.makeText(context, "Respaldo exitoso en Google Drive", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al subir archivo a Drive", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isBusyProgress = null
                        Toast.makeText(context, "No hay base de datos local para respaldar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isBusyProgress = null
                    Toast.makeText(context, "Error en respaldo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun ejecutarRestauracion(accName: String) {
        isBusyProgress = "Descargando datos desde Google Drive, por favor espera..."
        scope.launch(Dispatchers.IO) {
            try {
                val driveService = GoogleDriveService(context, accName)
                val backupManager = BackupManager(context)
                val tempFile = File(context.cacheDir, "temp_restore.db")
                val success = driveService.downloadFile("finanzas_backup.db", tempFile)
                if (success) {
                    val restored = backupManager.restoreDatabase(tempFile)
                    withContext(Dispatchers.Main) {
                        isBusyProgress = null
                        if (restored) {
                            if (backupPrefs.getLong("LastBackupTime", 0L) <= 0L) {
                                backupPrefs.edit().putLong("LastBackupTime", System.currentTimeMillis()).apply()
                            }
                            validarVisibilidadDatosPrueba()
                            Toast.makeText(context, "¡Restauración completada con éxito! Reiniciando la app...", Toast.LENGTH_LONG).show()
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 500)
                        } else {
                            Toast.makeText(context, "Error al sobreescribir la base de datos local", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isBusyProgress = null
                        Toast.makeText(context, "No se encontró ningún archivo 'finanzas_backup.db' en Google Drive: $accName", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isBusyProgress = null
                    Toast.makeText(context, "Error al conectar con Drive: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val mail = account?.email ?: account?.account?.name
            if (!mail.isNullOrBlank() && mail != "null") {
                backupPrefs.edit().putString("GoogleAccount", mail).apply()
                Toast.makeText(context, "Cuenta de Drive vinculada: $mail", Toast.LENGTH_SHORT).show()
                actualizarEstadoBackup()
                val action = pendingDriveAction
                pendingDriveAction = null
                if (action == "RESTORE") {
                    showConfirmRestore = true
                } else if (action == "BACKUP") {
                    forzarRespaldoManual(mail)
                }
            } else {
                pendingDriveAction = null
                Toast.makeText(context, "No se pudo obtener el correo de la cuenta de Google", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            pendingDriveAction = null
            Toast.makeText(context, "Error al vincular Drive: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun iniciarVinculacionGoogle() {
        if (activity == null) return
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    fun configurarWorkManager(frecuencia: String) {
        val workManager = WorkManager.getInstance(context)
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

        val periodicRequest = PeriodicWorkRequestBuilder<BackupWorker>(interval.first, interval.second)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "AutoBackup",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicRequest
        )

        val currentAcc = backupPrefs.getString("GoogleAccount", null)
        if (!currentAcc.isNullOrBlank() && currentAcc != "null") {
            val immediateRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(constraints)
                .addTag("ImmediateBackup")
                .build()
            workManager.enqueue(immediateRequest)
            Toast.makeText(context, "Sincronización inicial en curso...", Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(context, "Respaldo automático: $frecuencia", Toast.LENGTH_SHORT).show()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cargarInformacionUsuario()
                actualizarEstadoBackup()
                cargarMetodosPago()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        cargarInformacionUsuario()
        actualizarEstadoBackup()
        cargarMetodosPago()
    }

    PerfilScreen(
        nombres = nombres,
        email = email,
        proveedor = proveedor,
        miembroDesde = miembroDesde,
        iniciales = iniciales,
        esTemaOscuro = esTemaOscuro,
        metodosPago = metodosPago,
        googleAccount = googleAccount,
        lastBackupTexto = lastBackupTexto,
        frecuenciaBackup = frecuenciaBackup,
        mostrarCardDemo = mostrarCardDemo,
        onBack = onBack,
        onOpenAyuda = { showDialogAyuda = true },
        onToggleTemaOscuro = { isDark ->
            esTemaOscuro = isDark
            temaPrefs.edit().putBoolean("EsTemaOscuro", isDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        },
        onAgregarMetodoPago = {
            metodoEnEdicion = null
            showDialogMetodoPago = true
        },
        onEditarMetodoPago = { mp ->
            metodoEnEdicion = mp
            showDialogMetodoPago = true
        },
        onBackupNow = {
            val acc = googleAccount
            if (!acc.isNullOrBlank()) {
                forzarRespaldoManual(acc)
            } else {
                pendingDriveAction = "BACKUP"
                Toast.makeText(context, "Iniciando vinculación con Google Drive...", Toast.LENGTH_SHORT).show()
                iniciarVinculacionGoogle()
            }
        },
        onRestore = {
            val acc = googleAccount
            if (!acc.isNullOrBlank()) {
                showConfirmRestore = true
            } else {
                pendingDriveAction = "RESTORE"
                Toast.makeText(context, "Iniciando vinculación con Google Drive para restaurar...", Toast.LENGTH_SHORT).show()
                iniciarVinculacionGoogle()
            }
        },
        onChangeFrecuenciaBackup = { seleccion ->
            frecuenciaBackup = seleccion
            backupPrefs.edit().putString("Frecuencia", seleccion).apply()
            configurarWorkManager(seleccion)
            validarVisibilidadDatosPrueba()
        },
        onCargarDemo = { showConfirmCargarDemo = true },
        onLimpiarDemo = { showConfirmLimpiarDemo = true },
        onEditarDatos = {
            context.startActivity(Intent(context, EditarInformacion::class.java))
        },
        onBuscarActualizaciones = {
            AppUpdateHelper.verificarActualizacion(context, esManual = true)
        },
        onLogout = {
            firebaseAuth.signOut()
            context.startActivity(Intent(context, OpcionesLoginActivity::class.java))
            activity?.finishAffinity()
        }
    )

    isBusyProgress?.let { msg ->
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DabaTheme.colors.surfaceCard
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = DabaTheme.colors.primaryEmerald,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = msg, color = DabaTheme.colors.textPrimary, fontSize = 13.sp)
                }
            }
        }
    }

    if (showConfirmRestore) {
        AlertDialog(
            onDismissRequest = { showConfirmRestore = false },
            title = { Text("Restaurar Datos desde la Nube", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro? Se reemplazarán todos los datos actuales de la app con la copia de seguridad guardada en tu Google Drive. La app se reiniciará.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmRestore = false
                        val acc = googleAccount
                        if (!acc.isNullOrBlank()) {
                            ejecutarRestauracion(acc)
                        }
                    }
                ) {
                    Text("Restaurar", color = DabaTheme.colors.primaryEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestore = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showConfirmCargarDemo) {
        AlertDialog(
            onDismissRequest = { showConfirmCargarDemo = false },
            title = { Text("Cargar Datos de Demostración", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas poblar todos los módulos con datos de prueba realistas (Movimientos, Gastos Fijos, Deudas y Metas de Ahorro)?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmCargarDemo = false
                        scope.launch(Dispatchers.IO) {
                            DemoDataHelper.cargarDatosPrueba(context, db)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "¡Datos de prueba cargados en todos los módulos!", Toast.LENGTH_LONG).show()
                                validarVisibilidadDatosPrueba()
                            }
                        }
                    }
                ) {
                    Text("Cargar Datos", color = DabaTheme.colors.primaryEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCargarDemo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showConfirmLimpiarDemo) {
        AlertDialog(
            onDismissRequest = { showConfirmLimpiarDemo = false },
            title = { Text("Limpiar Base de Datos", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de vaciar todos los registros para comenzar con una base de datos limpia?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmLimpiarDemo = false
                        scope.launch(Dispatchers.IO) {
                            DemoDataHelper.limpiarTodosLosDatos(context, db)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Base de datos limpiada correctamente", Toast.LENGTH_SHORT).show()
                                validarVisibilidadDatosPrueba()
                            }
                        }
                    }
                ) {
                    Text("Limpiar Todo", color = DabaTheme.colors.expenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmLimpiarDemo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDialogMetodoPago) {
        DialogMetodoPagoCompose(
            mpExistente = metodoEnEdicion,
            onDismiss = { showDialogMetodoPago = false },
            onSave = { mp ->
                db.insertarOActualizarMetodoPago(mp)
                val uid = firebaseAuth.uid
                if (uid != null) {
                    FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("metodos_pago")
                        .child(mp.id).setValue(mp)
                }
                cargarMetodosPago()
                showDialogMetodoPago = false
            },
            onDelete = { mp ->
                if (mp.id != "efectivo_default") {
                    db.eliminarMetodoPago(mp.id)
                    val uid = firebaseAuth.uid
                    if (uid != null) {
                        FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("metodos_pago")
                            .child(mp.id).removeValue()
                    }
                    cargarMetodosPago()
                    showDialogMetodoPago = false
                }
            }
        )
    }

    if (showDialogAyuda) {
        DialogCentroAyudaCompose(onDismiss = { showDialogAyuda = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    nombres: String,
    email: String,
    proveedor: String,
    miembroDesde: String,
    iniciales: String,
    esTemaOscuro: Boolean,
    metodosPago: List<MetodoPago>,
    googleAccount: String?,
    lastBackupTexto: String,
    frecuenciaBackup: String,
    mostrarCardDemo: Boolean,
    onBack: () -> Unit,
    onOpenAyuda: () -> Unit,
    onToggleTemaOscuro: (Boolean) -> Unit,
    onAgregarMetodoPago: () -> Unit,
    onEditarMetodoPago: (MetodoPago) -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onChangeFrecuenciaBackup: (String) -> Unit,
    onCargarDemo: () -> Unit,
    onLimpiarDemo: () -> Unit,
    onEditarDatos: () -> Unit,
    onBuscarActualizaciones: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = DabaTheme.colors
    var expandedFrecuencia by remember { mutableStateOf(false) }
    val opcionesFrecuencia = remember { listOf("Desactivado", "Diario", "Semanal", "Mensual") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceBg)
    ) {
        // Header: Volver + Mi Perfil + Ayuda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = colors.surfaceCard,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Volver",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mi Perfil",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                onClick = onOpenAyuda,
                shape = CircleShape,
                color = colors.surfaceCard,
                shadowElevation = 1.dp,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_help),
                        contentDescription = "Ayuda",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Tarjeta de Identidad + Modo Oscuro
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(colors.primaryEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = iniciales,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(colors.primaryEmerald)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = nombres,
                                    color = colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colors.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = proveedor,
                                        color = colors.textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = email,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Text(
                                text = miembroDesde,
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = colors.surfaceContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nightlight),
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Modo Oscuro",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Apariencia visual de la app",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Switch(
                            checked = esTemaOscuro,
                            onCheckedChange = onToggleTemaOscuro,
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.primaryEmerald)
                        )
                    }
                }
            }

            // 1B. Mis Métodos de Pago
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MIS MÉTODOS DE PAGO",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    metodosPago.forEach { mp ->
                        val iconRes = when {
                            mp.tipo == "EFECTIVO" -> R.drawable.ic_account_balance_wallet
                            mp.tipo.contains("TARJETA") -> R.drawable.ic_credit_card
                            else -> R.drawable.ic_payments
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEditarMetodoPago(mp) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (mp.esPredeterminado) colors.ghostBg else colors.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = if (mp.esPredeterminado) colors.primaryEmerald else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mp.nombreFormateado(),
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (mp.esPredeterminado) "Predeterminado" else mp.tipo.replace("_", " "),
                                    color = if (mp.esPredeterminado) colors.primaryEmerald else colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onAgregarMetodoPago,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.ghostBg),
                        border = BorderStroke(1.dp, colors.ghostBorder)
                    ) {
                        Text(
                            text = "+ Agregar método de pago",
                            color = colors.primaryEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Copia de Seguridad Google Drive
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceContainerLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_cloud),
                                    contentDescription = null,
                                    tint = colors.primaryEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Copia de Seguridad",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (!googleAccount.isNullOrBlank()) {
                                        "Vinculado como: $googleAccount"
                                    } else {
                                        "Cuenta: $email (Drive no vinculado)"
                                    },
                                    color = colors.primaryEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastBackupTexto,
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onBackupNow,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_upload),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Respaldar ahora", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onRestore,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.borderSubtle)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar datos", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Frecuencia de respaldo automático",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedFrecuencia,
                        onExpandedChange = { expandedFrecuencia = !expandedFrecuencia }
                    ) {
                        OutlinedTextField(
                            value = frecuenciaBackup,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrecuencia) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFrecuencia,
                            onDismissRequest = { expandedFrecuencia = false }
                        ) {
                            opcionesFrecuencia.forEach { op ->
                                DropdownMenuItem(
                                    text = { Text(op) },
                                    onClick = {
                                        onChangeFrecuenciaBackup(op)
                                        expandedFrecuencia = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Datos de Demostración
            if (mostrarCardDemo) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceContainerLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_autorenew),
                                    contentDescription = null,
                                    tint = colors.primaryEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Datos de Demostración",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Carga ejemplos en todos los módulos o limpia la BD",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onCargarDemo,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
                            ) {
                                Text("Cargar Datos", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onLimpiarDemo,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colors.borderSubtle)
                            ) {
                                Text("Limpiar BD", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Botones de Acción Global
            Button(
                onClick = onEditarDatos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Actualizar Datos", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onBuscarActualizaciones,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceContainer)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_download),
                    contentDescription = null,
                    tint = colors.primaryEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar actualizaciones", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sign_out),
                    contentDescription = null,
                    tint = colors.expenseRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", color = colors.expenseRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogMetodoPagoCompose(
    mpExistente: MetodoPago?,
    onDismiss: () -> Unit,
    onSave: (MetodoPago) -> Unit,
    onDelete: (MetodoPago) -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors
    val tipos = remember { listOf("EFECTIVO", "TARJETA_DEBITO", "TARJETA_CREDITO", "CUENTA_BANCARIA", "BILLETERA_DIGITAL") }

    var tipoSel by remember { mutableStateOf(mpExistente?.tipo ?: "TARJETA_DEBITO") }
    var expandedTipo by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf(mpExistente?.nombre ?: "") }
    var ultimos4 by remember { mutableStateOf(mpExistente?.ultimos4 ?: "") }
    var esPredeterminado by remember { mutableStateOf(mpExistente?.esPredeterminado ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (mpExistente != null) "Editar Método de Pago" else "Agregar Método de Pago",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Método") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        tipos.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    tipoSel = t
                                    if (t == "EFECTIVO") ultimos4 = ""
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Entidad (Ej. Bancolombia, Nu, Nequi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (tipoSel != "EFECTIVO") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ultimos4,
                        onValueChange = { if (it.length <= 4) ultimos4 = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Últimos 4 dígitos (Opcional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Por seguridad y para evitar filtraciones de datos, ingresa SOLO los últimos 4 dígitos.",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Usar como método por defecto",
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = esPredeterminado,
                        onCheckedChange = { esPredeterminado = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.primaryEmerald)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = colors.textPrimary)
                    }
                    Button(
                        onClick = {
                            if (nombre.isBlank()) {
                                Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val mp = mpExistente ?: MetodoPago(id = UUID.randomUUID().toString())
                            mp.nombre = nombre.trim()
                            mp.tipo = tipoSel
                            mp.ultimos4 = ultimos4.trim()
                            mp.esPredeterminado = esPredeterminado
                            onSave(mp)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
                    ) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (mpExistente != null && mpExistente.id != "efectivo_default") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onDelete(mpExistente) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.expenseRedBg),
                        border = BorderStroke(1.dp, colors.expenseRed)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null,
                            tint = colors.expenseRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar Método", color = colors.expenseRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogCentroAyudaCompose(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = DabaTheme.colors

    val (versionName, versionCode) = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pInfo.versionName ?: BuildConfig.VERSION_NAME
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            vName to vCode
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME to BuildConfig.VERSION_CODE.toLong()
        }
    }

    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    val permisoNotifActivo = !enabledListeners.isNullOrBlank() && enabledListeners.contains(context.packageName)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Centro de Ayuda y Legal",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "DABAFINN v$versionName (Build $versionCode)",
                            color = colors.primaryEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Cerrar",
                            tint = colors.textSecondary
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = colors.borderSubtle)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. CÓMO USAR DABAFINN Y SUS MÓDULOS", color = colors.primaryEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceBg, border = BorderStroke(1.dp, colors.borderSubtle)) {
                        Text(
                            text = "• Inicio (Dashboard): Visualiza tu balance neto en tiempo real, resumen quincenal/mensual de ingresos y gastos, desglose por categorías y acceso al Asesor Financiero IA.\n\n" +
                                "• Movimientos: Registra ingresos y gastos tocando el botón (+). Puedes adjuntar o escanear facturas con la cámara, asignar categorías, método de pago y filtrar por quincena o mes.\n\n" +
                                "• Gastos Fijos: Administra tus pagos recurrentes (arriendo, servicios, suscripciones) con periodicidad mensual o quincenal.\n\n" +
                                "• Metas de Ahorro y Control de Deudas: Registra objetivos financieros, tarjetas o préstamos y realiza abonos en tiempo real.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Text("2. PERMISOS REQUERIDOS Y SU PROPÓSITO", color = colors.primaryEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceBg, border = BorderStroke(1.dp, colors.borderSubtle)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "• Acceso a Notificaciones: Detecta notificaciones bancarias 100% en tu dispositivo (offline).\n\n" +
                                    "• Cámara: Escanea facturas mediante OCR local.\n\n" +
                                    "• Internet: Sincroniza copias de seguridad en tu Google Drive privado y consulta el Asesor IA.",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onDismiss()
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.ghostBg)
                            ) {
                                Text(
                                    text = if (permisoNotifActivo) "Permiso de lectura bancaria: Activo (Configurar)" else "Conceder permiso de lectura bancaria",
                                    color = colors.primaryEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text("3. PRIVACIDAD Y SEGURIDAD DE DATOS", color = colors.primaryEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceBg, border = BorderStroke(1.dp, colors.borderSubtle)) {
                        Text(
                            text = "• Almacenamiento Local Primero (Offline-First): Todos tus registros se guardan en SQLite dentro de tu teléfono.\n\n" +
                                "• Protección Financiera: DABAFINN nunca solicita claves bancarias ni números completos de tarjetas.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
                ) {
                    Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        PerfilScreen(
            nombres = "Jair",
            email = "jair@ejemplo.com",
            proveedor = "Google",
            miembroDesde = "Miembro desde: 2026",
            iniciales = "JR",
            esTemaOscuro = false,
            metodosPago = listOf(MetodoPago("1", "Efectivo", "EFECTIVO", "", true)),
            googleAccount = "jair@ejemplo.com",
            lastBackupTexto = "Última copia: Hoy",
            frecuenciaBackup = "Diario",
            mostrarCardDemo = true,
            onBack = {},
            onOpenAyuda = {},
            onToggleTemaOscuro = {},
            onAgregarMetodoPago = {},
            onEditarMetodoPago = {},
            onBackupNow = {},
            onRestore = {},
            onChangeFrecuenciaBackup = {},
            onCargarDemo = {},
            onLimpiarDemo = {},
            onEditarDatos = {},
            onBuscarActualizaciones = {},
            onLogout = {}
        )
    }
}
