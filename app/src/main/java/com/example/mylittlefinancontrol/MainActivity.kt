package com.example.mylittlefinancontrol

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.*
import com.example.mylittlefinancontrol.Fragmentos.*
import com.example.mylittlefinancontrol.Servicios.AppTourManager
import com.example.mylittlefinancontrol.Servicios.BackupWorker
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

        if (firebaseAuth.currentUser == null) {
            irOpcionesLogin()
        } else {
            verificarTutorial()
        }

        // Carga inicial: Inicio
        verFragmentoInicio()

        // Clic en la cabecera izquierda -> Ir a Inicio
        binding.llHeaderUsuario.setOnClickListener {
            verFragmentoInicio()
        }

        // Clic en el botón Perfil Toolbar -> Ir a Perfil
        binding.btnPerfilToolbar.setOnClickListener {
            verFragmentoPerfil()
        }

        // Clic en notificaciones
        binding.btnNotificaciones.setOnClickListener {
            Toast.makeText(this, "No tienes notificaciones pendientes", Toast.LENGTH_SHORT).show()
        }

        // Navegación Inferior (Bottom Navigation)
        binding.navInicio.setOnClickListener { verFragmentoInicio() }
        binding.navMovimientos.setOnClickListener { verFragmentoMovimientos() }
        binding.navGastosFijos.setOnClickListener { verFragmentoGastosFijos() }
        binding.navAhorros.setOnClickListener { verFragmentoAhorros() }
        binding.navDeudas.setOnClickListener { verFragmentoDeudas() }

    }

    private fun actualizarNombreUsuario() {
        val user = firebaseAuth.currentUser
        val nombre = user?.displayName ?: user?.email?.substringBefore("@") ?: "Jair"
        binding.appName.text = nombre
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

        if (account != null && frecuencia != "Desactivado") {
            val workManager = WorkManager.getInstance(this)
            
            val workInfo = workManager.getWorkInfosForUniqueWork("AutoBackup").get()
            val isScheduled = workInfo.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }

            if (!isScheduled) {
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
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }

    private fun actualizarEstadoNavegacion(tabActivo: Int) {
        val colorActivo = ContextCompat.getColor(this, R.color.primary_emerald)
        val colorInactivo = ContextCompat.getColor(this, R.color.text_secondary)

        fun aplicarColorTab(iv: ImageView, tv: TextView, esActivo: Boolean) {
            val color = if (esActivo) colorActivo else colorInactivo
            iv.imageTintList = ColorStateList.valueOf(color)
            tv.setTextColor(color)
        }

        aplicarColorTab(binding.ivNavInicio, binding.tvNavInicio, tabActivo == 0)
        aplicarColorTab(binding.ivNavMovimientos, binding.tvNavMovimientos, tabActivo == 1)
        aplicarColorTab(binding.ivNavGastosFijos, binding.tvNavGastosFijos, tabActivo == 2)
        aplicarColorTab(binding.ivNavAhorros, binding.tvNavAhorros, tabActivo == 3)
        aplicarColorTab(binding.ivNavDeudas, binding.tvNavDeudas, tabActivo == 4)
    }

    private fun reemplazarFragmento(fragment: Fragment, titulo: String) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment)
            .commit()
    }

    fun verFragmentoPerfil() {
        actualizarEstadoNavegacion(-1)
        reemplazarFragmento(FragmentPerfil(), "Perfil")
    }
    
    fun verFragmentoMovimientos() {
        actualizarEstadoNavegacion(1)
        reemplazarFragmento(FragmentMovimiento(), "Movimientos")
    }

    fun verFragmentoAhorros() {
        actualizarEstadoNavegacion(3)
        reemplazarFragmento(FragmentAhorros(), "Ahorros")
    }

    fun verFragmentoGastosFijos() {
        actualizarEstadoNavegacion(2)
        reemplazarFragmento(FragmentGastosFijos(), "Gastos Fijos")
    }
    
    fun verFragmentoDeudas() {
        actualizarEstadoNavegacion(4)
        reemplazarFragmento(FragmentBalance(), "Deudas")
    }

    fun verFragmentoInicio() {
        actualizarEstadoNavegacion(0)
        reemplazarFragmento(FragmentInicio(), "Inicio")
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
        tutorialHelper.waitForViewAndShow(
            R.id.card_balance_general,
            "Resumen Global",
            "Aquí tienes tu balance total histórico de todos tus registros.",
            true
        ) {
            tutorialHelper.waitForViewAndShow(
                R.id.card_ingresos,
                "Flujo del Mes",
                "Consulta rápidamente tus ingresos y gastos del mes actual.",
                true
            ) {
                tutorialHelper.waitForViewAndShow(
                    R.id.rv_ultimos_movimientos,
                    "Consulta desde Inicio",
                    "Desliza hacia abajo para ver tus registros más recientes sin cambiar de pantalla.",
                    true
                ) {
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
    }
}