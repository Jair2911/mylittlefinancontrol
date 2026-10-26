package com.example.mylittlefinancontrol

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.*
import com.example.mylittlefinancontrol.Fragmentos.*
import com.example.mylittlefinancontrol.Servicios.AppTourManager
import com.example.mylittlefinancontrol.Servicios.BackupWorker
import com.example.mylittlefinancontrol.Servicios.GastoFijoWorker
import com.example.mylittlefinancontrol.Servicios.TutorialHelper
import com.example.mylittlefinancontrol.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var tutorialHelper: TutorialHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        aplicarTemaGuardado()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        tutorialHelper = TutorialHelper(this)

        actualizarNombreUsuario()
        verificarConfiguracionBackup()
        solicitarPermisosSmsYNotificaciones()
        programarRecordatoriosGastosFijos()

        if (firebaseAuth.currentUser == null) {
            irOpcionesLogin()
        } else {
            verificarTutorial()
        }

        // Carga inicial: Inicio
        verFragmentoInicio()

        // Verificar actualizaciones de la app en segundo plano
        com.example.mylittlefinancontrol.Servicios.AppUpdateHelper.verificarActualizacion(this, esManual = false)

        // Clic en el avatar circular o cabecera izquierda -> Ir a Inicio
        binding.llHeaderUsuario.setOnClickListener { verFragmentoInicio() }
        binding.flAvatarHeader.setOnClickListener { verFragmentoInicio() }
        binding.ivAvatarIcono.setOnClickListener { verFragmentoInicio() }
        binding.appName.setOnClickListener { verFragmentoInicio() }

        // Clic en el botón Perfil Toolbar -> Ir a Perfil
        binding.btnPerfilToolbar.setOnClickListener {
            verFragmentoPerfil()
        }

        // Clic en notificaciones
        binding.btnNotificaciones.setOnClickListener {
            Toast.makeText(this, "No tienes notificaciones pendientes", Toast.LENGTH_SHORT).show()
        }

        // Navegación Inferior (Bottom Navigation con 5 Secciones)
        binding.navMovimientos.setOnClickListener { verFragmentoMovimientos() }
        binding.navGastosFijos.setOnClickListener { verFragmentoGastosFijos() }
        binding.navInicio.setOnClickListener { verFragmentoInicio() }
        binding.navAhorros.setOnClickListener { verFragmentoAhorros() }
        binding.navDeudas.setOnClickListener { verFragmentoDeudas() }

        // Manejo de retroceso para volver a Inicio
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentoActual = supportFragmentManager.findFragmentById(binding.fragmentoFL.id)
                if (fragmentoActual !is FragmentInicio) {
                    verFragmentoInicio()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun actualizarNombreUsuario() {
        val user = firebaseAuth.currentUser
        val userEmail = user?.email ?: "correo@ejemplo.com"
        val userName = user?.displayName?.ifBlank { null } ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        binding.appName.text = userName

        val inicialesFallback = userName
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.first().uppercase() }
            .take(2)
            .joinToString("")
        binding.ivAvatarIcono.text = if (inicialesFallback.isNotEmpty()) inicialesFallback else "JR"

        val uid = firebaseAuth.uid ?: return
        val ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child(uid).addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val nombres = snapshot.child("nombres").value?.toString()
                val nombreFinal = if (!nombres.isNullOrBlank() && nombres != "null") nombres else userName
                binding.appName.text = nombreFinal
                val iniciales = nombreFinal
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .map { it.first().uppercase() }
                    .take(2)
                    .joinToString("")
                binding.ivAvatarIcono.text = if (iniciales.isNotEmpty()) iniciales else "JR"
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
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

    companion object {
        private const val TAG_INICIO = "TAG_INICIO"
        private const val TAG_MOVIMIENTOS = "TAG_MOVIMIENTOS"
        private const val TAG_GASTOS_FIJOS = "TAG_GASTOS_FIJOS"
        private const val TAG_AHORROS = "TAG_AHORROS"
        private const val TAG_DEUDAS = "TAG_DEUDAS"
        private const val TAG_PERFIL = "TAG_PERFIL"
    }

    private fun actualizarEstadoNavegacion(tabActivo: Int) {
        val colorActivo = ContextCompat.getColor(this, R.color.primary_emerald)
        val colorInactivo = ContextCompat.getColor(this, R.color.text_secondary)

        fun aplicarColorTab(iv: ImageView, tv: TextView, esActivo: Boolean) {
            val color = if (esActivo) colorActivo else colorInactivo
            iv.imageTintList = ColorStateList.valueOf(color)
            tv.setTextColor(color)
        }

        aplicarColorTab(binding.ivNavMovimientos, binding.tvNavMovimientos, tabActivo == 0)
        aplicarColorTab(binding.ivNavGastosFijos, binding.tvNavGastosFijos, tabActivo == 1)
        aplicarColorTab(binding.ivNavInicio, binding.tvNavInicio, tabActivo == 2)
        aplicarColorTab(binding.ivNavAhorros, binding.tvNavAhorros, tabActivo == 3)
        aplicarColorTab(binding.ivNavDeudas, binding.tvNavDeudas, tabActivo == 4)
    }

    private fun mostrarFragmentoPorTag(tag: String, factory: () -> Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        
        for (fragment in supportFragmentManager.fragments) {
            if (fragment.isVisible) {
                transaction.hide(fragment)
            }
        }

        val targetFragment = supportFragmentManager.findFragmentByTag(tag)
        if (targetFragment == null) {
            val newFragment = factory()
            transaction.add(binding.fragmentoFL.id, newFragment, tag)
        } else {
            transaction.show(targetFragment)
        }

        transaction.commitAllowingStateLoss()
    }

    fun verFragmentoPerfil() {
        actualizarEstadoNavegacion(-1)
        mostrarFragmentoPorTag(TAG_PERFIL) { FragmentPerfil() }
    }
    
    fun verFragmentoMovimientos() {
        actualizarEstadoNavegacion(0)
        mostrarFragmentoPorTag(TAG_MOVIMIENTOS) { FragmentMovimiento() }
    }

    fun verFragmentoGastosFijos() {
        actualizarEstadoNavegacion(1)
        mostrarFragmentoPorTag(TAG_GASTOS_FIJOS) { FragmentGastosFijos() }
    }

    fun verFragmentoAhorros() {
        actualizarEstadoNavegacion(3)
        mostrarFragmentoPorTag(TAG_AHORROS) { FragmentAhorros() }
    }
    
    fun verFragmentoDeudas() {
        actualizarEstadoNavegacion(4)
        mostrarFragmentoPorTag(TAG_DEUDAS) { FragmentBalance() }
    }

    fun verFragmentoInicio() {
        actualizarEstadoNavegacion(2)
        mostrarFragmentoPorTag(TAG_INICIO) { FragmentInicio() }
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
            ) { _ -> }
            requestLauncher.launch(permisos.toTypedArray())
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
                binding.root.postDelayed({
                    iniciarTourGuiado()
                }, 1000)
            }
        }
    }

    private fun finalizarTour() {
        tutorialHelper.dismissCurrent()
        
        val uid = firebaseAuth.uid
        if (uid != null) {
            AppTourManager.completeTutorial(uid)
            verFragmentoInicio()
        }
    }

    private fun iniciarTourGuiado() {
        verFragmentoMovimientos()
        tutorialHelper.waitForViewAndShow(
            R.id.cv_resumen_mov,
            "Gestión de Movimientos",
            "Aquí puedes ver el historial de tus transacciones y filtrar por fecha.",
            true
        ) {
            tutorialHelper.waitForViewAndShow(
                R.id.FABAgregarNota,
                "¿Cómo Agregar Datos?",
                "Toca este botón (+) para registrar un nuevo ingreso o gasto rápidamente.",
                false
            ) {
                verFragmentoGastosFijos()
                tutorialHelper.waitForViewAndShow(
                    R.id.cv_total,
                    "Gastos Recurrentes",
                    "Administra tus suscripciones y pagos mensuales fijos.",
                    true
                ) {
                    verFragmentoAhorros()
                    tutorialHelper.waitForViewAndShow(
                        R.id.cv_total_ahorros,
                        "Metas de Ahorro",
                        "Define tus objetivos y ahorra progresivamente.",
                        true
                    ) {
                        verFragmentoDeudas()
                        tutorialHelper.waitForViewAndShow(
                            R.id.cv_total_deudas,
                            "Control de Deudas",
                            "Mantén el control de lo que debes y tus pagos realizados.",
                            true
                        ) {
                            verFragmentoPerfil()
                            tutorialHelper.waitForViewAndShow(
                                R.id.sw_tema,
                                "Personalización",
                                "Activa el modo oscuro para una experiencia visual más cómoda."
                            ) {
                                tutorialHelper.waitForViewAndShow(
                                    R.id.btn_google_auth,
                                    "Protege tus Datos",
                                    "Víncula tu cuenta de Google Drive para que nunca pierdas tu información.",
                                    true
                                ) {
                                    tutorialHelper.waitForViewAndShow(
                                        R.id.btn_backup_now,
                                        "Sincronización en la Nube",
                                        "Realiza copias de seguridad manuales aquí o configura respaldos automáticos.",
                                        true
                                    ) {
                                        tutorialHelper.showStep(
                                            binding.btnPerfilToolbar,
                                            "¡Todo Listo!",
                                            "Ya conoces las herramientas principales para controlar tus finanzas."
                                        ) {
                                            finalizarTour()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}