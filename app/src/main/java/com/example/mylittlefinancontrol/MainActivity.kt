package com.example.mylittlefinancontrol

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import com.example.mylittlefinancontrol.ui.components.ModalMenuDabafinn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.mylittlefinancontrol.Fragmentos.AhorrosStatefulScreen
import com.example.mylittlefinancontrol.Fragmentos.DeudasStatefulScreen
import com.example.mylittlefinancontrol.Fragmentos.GastosFijosStatefulScreen
import com.example.mylittlefinancontrol.Fragmentos.InicioStatefulScreen
import com.example.mylittlefinancontrol.Fragmentos.MovimientoStatefulScreen
import com.example.mylittlefinancontrol.Fragmentos.PerfilStatefulScreen
import com.example.mylittlefinancontrol.Servicios.AppTourManager
import com.example.mylittlefinancontrol.Servicios.AppUpdateHelper
import com.example.mylittlefinancontrol.Servicios.BackupWorker
import com.example.mylittlefinancontrol.Servicios.GastoFijoWorker
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

enum class MainSection(val label: String, val iconRes: Int) {
    MOVIMIENTOS("Movimientos", R.drawable.ic_receipt_long),
    GASTOS_FIJOS("Gastos Fijos", R.drawable.ic_pie_chart),
    INICIO("Inicio", R.drawable.ic_home_icon),
    AHORROS("Ahorros", R.drawable.ic_savings),
    DEUDAS("Deudas", R.drawable.ic_credit_card),
    PERFIL("Perfil", R.drawable.ic_person)
}

data class TourStepInfo(
    val section: MainSection,
    val title: String,
    val description: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    private var currentSectionState by mutableStateOf(MainSection.INICIO)
    private var userNameState by mutableStateOf("DABAFINN")
    private var userInitialsState by mutableStateOf("JR")
    private var showNotifPermissionDialog by mutableStateOf(false)
    private var showTourDialog by mutableStateOf(false)
    private var currentTourStepIndex by mutableIntStateOf(0)

    private val tourSteps = listOf(
        TourStepInfo(
            section = MainSection.MOVIMIENTOS,
            title = "Gestión de Movimientos",
            description = "Aquí puedes ver el historial de tus transacciones, filtrar por fecha y tocar el botón (+) para registrar un nuevo ingreso o gasto rápidamente."
        ),
        TourStepInfo(
            section = MainSection.GASTOS_FIJOS,
            title = "Gastos Recurrentes",
            description = "Administra tus suscripciones y pagos mensuales fijos con control anual de estados."
        ),
        TourStepInfo(
            section = MainSection.AHORROS,
            title = "Metas de Ahorro",
            description = "Define tus objetivos financieros y registra abonos progresivamente."
        ),
        TourStepInfo(
            section = MainSection.DEUDAS,
            title = "Control de Deudas",
            description = "Mantén el control de tus obligaciones financieras y tus pagos realizados."
        ),
        TourStepInfo(
            section = MainSection.PERFIL,
            title = "Personalización y Respaldo",
            description = "Activa el modo oscuro, vincula tu cuenta de Google Drive y realiza copias de seguridad automáticas en la nube."
        ),
        TourStepInfo(
            section = MainSection.INICIO,
            title = "¡Todo Listo!",
            description = "Ya conoces las herramientas principales de DABAFINN para controlar tus finanzas."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        aplicarTemaGuardado()
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        actualizarNombreUsuario()
        verificarConfiguracionBackup()
        solicitarPermisosSmsYNotificaciones()
        programarRecordatoriosGastosFijos()

        if (firebaseAuth.currentUser == null) {
            irOpcionesLogin()
            return
        } else {
            verificarTutorial()
        }

        // Verificar actualizaciones de la app en segundo plano
        AppUpdateHelper.verificarActualizacion(this, esManual = false)

        // Manejo de retroceso para volver a Inicio
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentSectionState != MainSection.INICIO) {
                    currentSectionState = MainSection.INICIO
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            MyLittleFinanControlTheme {
                var showGeminiAdvisor by remember { mutableStateOf(false) }

                MainHostScreen(
                    currentSection = currentSectionState,
                    userName = userNameState,
                    userInitials = userInitialsState,
                    onSectionSelect = { section ->
                        currentSectionState = section
                    },
                    onNotificacionesClick = {
                        Toast.makeText(this, "No tienes notificaciones pendientes", Toast.LENGTH_SHORT).show()
                    },
                    onOpenGeminiAdvisor = {
                        showGeminiAdvisor = true
                    },
                    onOpenDetallePeriodo = { tipoPeriodo ->
                        val intent = Intent(this, DetallePeriodoActivity::class.java).apply {
                            putExtra("TIPO_PERIODO", tipoPeriodo)
                        }
                        startActivity(intent)
                    },
                    onCerrarSesion = {
                        cerrarSesion()
                    }
                )

                if (showGeminiAdvisor) {
                    GeminiAdvisorBottomSheet(
                        onDismiss = { showGeminiAdvisor = false }
                    )
                }

                if (showNotifPermissionDialog) {
                    val sp = remember { getSharedPreferences("PermisosPrefs", Context.MODE_PRIVATE) }
                    AlertDialog(
                        onDismissRequest = {
                            sp.edit().putBoolean("NotifListenerPrompted", true).apply()
                            showNotifPermissionDialog = false
                        },
                        title = {
                            Text(
                                text = "Automatización de Gastos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "Permite que DABAFINN lea automáticamente las notificaciones de tus bancos para registrar tus gastos sin que tengas que escribirlos (100% offline y privado).",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    sp.edit().putBoolean("NotifListenerPrompted", true).apply()
                                    showNotifPermissionDialog = false
                                    startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    Toast.makeText(this@MainActivity, "Autoriza a DABAFINN en la lista de aplicaciones.", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DabaTheme.colors.primaryEmerald)
                            ) {
                                Text("Conceder Permiso", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    sp.edit().putBoolean("NotifListenerPrompted", true).apply()
                                    showNotifPermissionDialog = false
                                }
                            ) {
                                Text("Más tarde", color = DabaTheme.colors.textSecondary)
                            }
                        }
                    )
                }

                if (showTourDialog && currentTourStepIndex in tourSteps.indices) {
                    val step = tourSteps[currentTourStepIndex]
                    OnboardingTourDialog(
                        stepIndex = currentTourStepIndex,
                        totalSteps = tourSteps.size,
                        title = step.title,
                        description = step.description,
                        onNext = {
                            if (currentTourStepIndex < tourSteps.lastIndex) {
                                val nextIndex = currentTourStepIndex + 1
                                currentTourStepIndex = nextIndex
                                currentSectionState = tourSteps[nextIndex].section
                            } else {
                                finalizarTour()
                            }
                        },
                        onSkip = {
                            finalizarTour()
                        }
                    )
                }
            }
        }
    }

    private fun actualizarNombreUsuario() {
        val user = firebaseAuth.currentUser
        val userEmail = user?.email ?: "correo@ejemplo.com"
        val userName = user?.displayName?.ifBlank { null }
            ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        userNameState = userName

        val inicialesFallback = userName
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.first().uppercase() }
            .take(2)
            .joinToString("")
        userInitialsState = if (inicialesFallback.isNotEmpty()) inicialesFallback else "JR"

        val uid = firebaseAuth.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombres = snapshot.child("nombres").value?.toString()
                val nombreFinal = if (!nombres.isNullOrBlank() && nombres != "null") nombres else userName
                userNameState = nombreFinal
                val iniciales = nombreFinal
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .map { it.first().uppercase() }
                    .take(2)
                    .joinToString("")
                userInitialsState = if (iniciales.isNotEmpty()) iniciales else "JR"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
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

    private fun irOpcionesLogin() {
        startActivity(Intent(applicationContext, OpcionesLoginActivity::class.java))
        finishAffinity()
    }

    fun cerrarSesion() {
        firebaseAuth.signOut()
        irOpcionesLogin()
    }

    private fun verificarConfiguracionBackup() {
        val sp = getSharedPreferences("BackupPrefs", Context.MODE_PRIVATE)
        val account = sp.getString("GoogleAccount", null)
        val frecuencia = sp.getString("Frecuencia", "Desactivado")

        if (!account.isNullOrBlank() && account != "null" && frecuencia != "Desactivado") {
            val workManager = WorkManager.getInstance(this)

            workManager.getWorkInfosForUniqueWorkLiveData("AutoBackup")
                .observe(this) { workInfoList ->
                    val isScheduled = workInfoList.any {
                        it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
                    }

                    if (!isScheduled) {
                        val interval = when (frecuencia) {
                            "Diario" -> 1L to TimeUnit.DAYS
                            "Semanal" -> 7L to TimeUnit.DAYS
                            "Mensual" -> 30L to TimeUnit.DAYS
                            else -> return@observe
                        }

                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()

                        val request = PeriodicWorkRequestBuilder<BackupWorker>(interval.first, interval.second)
                            .setConstraints(constraints)
                            .build()

                        workManager.enqueueUniquePeriodicWork(
                            "AutoBackup",
                            ExistingPeriodicWorkPolicy.KEEP,
                            request
                        )
                    }
                }
        }
    }

    fun verFragmentoPerfil() {
        currentSectionState = MainSection.PERFIL
    }

    fun verFragmentoMovimientos() {
        currentSectionState = MainSection.MOVIMIENTOS
    }

    fun verFragmentoGastosFijos() {
        currentSectionState = MainSection.GASTOS_FIJOS
    }

    fun verFragmentoAhorros() {
        currentSectionState = MainSection.AHORROS
    }

    fun verFragmentoDeudas() {
        currentSectionState = MainSection.DEUDAS
    }

    fun verFragmentoInicio() {
        currentSectionState = MainSection.INICIO
    }

    private fun solicitarPermisosSmsYNotificaciones() {
        val permisos = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permisos.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permisos.isNotEmpty()) {
            val requestLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { _ ->
                verificarPermisoLecturaNotificaciones()
            }
            requestLauncher.launch(permisos.toTypedArray())
        } else {
            verificarPermisoLecturaNotificaciones()
        }
    }

    private fun tienePermisoLecturaNotificaciones(): Boolean {
        val enabledListeners = android.provider.Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return !enabledListeners.isNullOrBlank() && enabledListeners.contains(packageName)
    }

    private fun verificarPermisoLecturaNotificaciones() {
        if (!tienePermisoLecturaNotificaciones()) {
            val sp = getSharedPreferences("PermisosPrefs", Context.MODE_PRIVATE)
            val yaSolicitadoEnSesion = sp.getBoolean("NotifListenerPrompted", false)
            if (!yaSolicitadoEnSesion) {
                showNotifPermissionDialog = true
            }
        }
    }

    private fun programarRecordatoriosGastosFijos() {
        try {
            val workManager = WorkManager.getInstance(this)
            val periodicWorkRequest = PeriodicWorkRequestBuilder<GastoFijoWorker>(
                24, TimeUnit.HOURS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "GastoFijoWorkerPeriodic",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error iniciando recordatorios de gastos fijos: ${e.message}")
        }
    }

    private fun verificarTutorial() {
        val uid = firebaseAuth.uid ?: return
        AppTourManager.checkTutorialStatus(uid) { completado ->
            if (!completado) {
                iniciarTourGuiado()
            }
        }
    }

    private fun iniciarTourGuiado() {
        currentTourStepIndex = 0
        currentSectionState = tourSteps[0].section
        showTourDialog = true
    }

    private fun finalizarTour() {
        showTourDialog = false
        val uid = firebaseAuth.uid
        if (uid != null) {
            AppTourManager.completeTutorial(uid)
            verFragmentoInicio()
        }
    }
}

@Composable
fun MainHostScreen(
    currentSection: MainSection,
    userName: String,
    userInitials: String,
    onSectionSelect: (MainSection) -> Unit,
    onNotificacionesClick: () -> Unit,
    onOpenGeminiAdvisor: () -> Unit,
    onOpenDetallePeriodo: (String) -> Unit,
    onCerrarSesion: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var mostrarMenuModal by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.7f,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                MainTopHeaderBar(
                    userName = userName,
                    userInitials = userInitials,
                    isPerfilActive = mostrarMenuModal || currentSection == MainSection.PERFIL,
                    onHeaderHomeClick = { onSectionSelect(MainSection.INICIO) },
                    onNotificacionesClick = onNotificacionesClick,
                    onMenuClick = { mostrarMenuModal = true }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentSection) {
                    MainSection.INICIO -> InicioStatefulScreen(
                        onNavigateMovimientos = { onSectionSelect(MainSection.MOVIMIENTOS) },
                        onNavigateGastosFijos = { onSectionSelect(MainSection.GASTOS_FIJOS) },
                        onNavigateDeudas = { onSectionSelect(MainSection.DEUDAS) },
                        onNavigateAhorros = { onSectionSelect(MainSection.AHORROS) },
                        onOpenGeminiAdvisor = onOpenGeminiAdvisor,
                        onOpenDetallePeriodo = onOpenDetallePeriodo
                    )
                    MainSection.MOVIMIENTOS -> MovimientoStatefulScreen(
                        onNavigateAhorros = { onSectionSelect(MainSection.AHORROS) }
                    )
                    MainSection.GASTOS_FIJOS -> GastosFijosStatefulScreen()
                    MainSection.AHORROS -> AhorrosStatefulScreen()
                    MainSection.DEUDAS -> DeudasStatefulScreen()
                    MainSection.PERFIL -> PerfilStatefulScreen(
                        onBack = { onSectionSelect(MainSection.INICIO) }
                    )
                }
            }
        }
    }

    if (mostrarMenuModal) {
        ModalMenuDabafinn(
            alNavegar = { ruta ->
                when (ruta) {
                    "movimientos" -> onSectionSelect(MainSection.MOVIMIENTOS)
                    "gastos_fijos" -> onSectionSelect(MainSection.GASTOS_FIJOS)
                    "deudas" -> onSectionSelect(MainSection.DEUDAS)
                    "ahorros" -> onSectionSelect(MainSection.AHORROS)
                    "perfil" -> onSectionSelect(MainSection.PERFIL)
                    "cerrar_sesion" -> onCerrarSesion()
                }
            },
            alCerrar = { mostrarMenuModal = false }
        )
    }
}

@Composable
fun MainTopHeaderBar(
    userName: String,
    userInitials: String,
    isPerfilActive: Boolean,
    onHeaderHomeClick: () -> Unit,
    onNotificacionesClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Bloque Izquierdo: Avatar Monograma + Bienvenida + Nombre
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onHeaderHomeClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceCard)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.6.sp
                            ),
                            color = colors.primaryEmerald
                        )
                    }
                    // Dot indicador de estado activo
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(colors.activeStateDot)
                            .border(1.5.dp, colors.surfaceBg, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "BIENVENIDO DE NUEVO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        ),
                        color = colors.primaryEmerald
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bloque Derecho: Notificaciones + Menú Hamburguesa
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    onClick = onNotificacionesClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification),
                            contentDescription = "Notificaciones",
                            tint = colors.textVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = if (isPerfilActive) colors.ghostBg else colors.surfaceCard,
                    border = BorderStroke(
                        1.dp,
                        if (isPerfilActive) colors.primaryEmerald else colors.borderSubtle
                    ),
                    onClick = onMenuClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menú",
                            tint = if (isPerfilActive) colors.primaryEmerald else colors.textVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = colors.borderSubtle
        )
    }
}

@Composable
fun MainBottomNavigationBar(
    currentSection: MainSection,
    onSectionSelect: (MainSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    val bottomTabs = remember {
        listOf(
            MainSection.MOVIMIENTOS,
            MainSection.GASTOS_FIJOS,
            MainSection.INICIO,
            MainSection.AHORROS,
            MainSection.DEUDAS
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceCard)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = colors.borderSubtle
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomTabs.forEach { tab ->
                val isSelected = currentSection == tab
                val tintColor = if (isSelected) colors.primaryEmerald else colors.textSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSectionSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = tab.label,
                        tint = tintColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = tintColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingTourDialog(
    stepIndex: Int,
    totalSteps: Int,
    title: String,
    description: String,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = DabaTheme.colors

    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = colors.surfaceCard,
        title = {
            Column {
                Text(
                    text = "TOUR GUIADO • PASO ${stepIndex + 1} DE $totalSteps",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = colors.primaryEmerald
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (stepIndex < totalSteps - 1) "Siguiente" else "Finalizar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (stepIndex < totalSteps - 1) {
                TextButton(onClick = onSkip) {
                    Text("Omitir tour", color = colors.textSecondary)
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun MainBarsPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        Scaffold(
            topBar = {
                MainTopHeaderBar(
                    userName = "Alexis Rojas",
                    userInitials = "AR",
                    isPerfilActive = false,
                    onHeaderHomeClick = {},
                    onNotificacionesClick = {},
                    onMenuClick = {}
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding))
        }
    }
}
