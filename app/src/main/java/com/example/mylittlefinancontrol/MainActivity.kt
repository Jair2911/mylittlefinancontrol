package com.example.mylittlefinancontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.mylittlefinancontrol.Fragmentos.FragmentBalance
import com.example.mylittlefinancontrol.Fragmentos.FragmentGastosFijos
import com.example.mylittlefinancontrol.Fragmentos.FragmentInicio
import com.example.mylittlefinancontrol.Fragmentos.FragmentMovimiento
import com.example.mylittlefinancontrol.Fragmentos.FragmentPerfil
import com.example.mylittlefinancontrol.Servicios.AppTourManager
import com.example.mylittlefinancontrol.Servicios.TutorialHelper
import com.example.mylittlefinancontrol.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

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

        if (firebaseAuth.currentUser == null){
            irOpcionesLogin()
        } else {
            verificarTutorial()
        }

        // Carga inicial: Home (sin disparar el listener del menú aún)
        reemplazarFragmento(FragmentInicio(), "Inicio")

        // Botón del Toolbar (izquierda) para el Perfil
        // CORRECCIÓN: Se cambió icPerfilToolbar por btnPerfilToolbar (ID real en activity_main.xml)
        binding.btnPerfilToolbar.setOnClickListener {
            verFragmentoPerfil()
        }

        binding.icSignOut.setOnClickListener {
            firebaseAuth.signOut()
            irOpcionesLogin()
        }

        binding.btnGeminiAdvisor.setOnClickListener {
            android.util.Log.e("MainActivity", "Botón Gemini presionado")
            GeminiAdvisorDialogFragment().show(supportFragmentManager, "GeminiAdvisor")
        }

        // Único punto de control para la navegación del menú inferior
        binding.botonNV.setOnItemSelectedListener { item ->
            android.util.Log.e("MainActivity", "Menú presionado: ${item.itemId}")
            when(item.itemId) {
                R.id.item_inicio -> {
                    reemplazarFragmento(FragmentInicio(), "Inicio")
                    true
                }
                R.id.item_movimientos -> {
                    reemplazarFragmento(FragmentMovimiento(), "Movimientos")
                    true
                }
                R.id.item_gastos_fijos -> {
                    reemplazarFragmento(FragmentGastosFijos(), "Gastos Fijos")
                    true
                }
                R.id.item_balance -> {
                    reemplazarFragmento(FragmentBalance(), "Deudas")
                    true
                }
                else -> false
            }
        }
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

    // Función interna privada para realizar la transacción de fragmentos
    private fun reemplazarFragmento(fragment: Fragment, titulo: String) {
        binding.appName.text = titulo
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment)
            .commit()
    }

    // Funciones públicas para interactividad desde el Dashboard (FragmentInicio)
    // Estas funciones mueven el menú, lo que dispara el listener y cambia la pantalla sin bucles.
    fun verFragmentoPerfil() {
        // Como Perfil no está en el menú inferior, lo cargamos directamente y limpiamos la selección
        binding.botonNV.selectedItemId = -1 
        reemplazarFragmento(FragmentPerfil(), "Perfil")
    }
    
    fun verFragmentoMovimientos() {
        binding.botonNV.selectedItemId = R.id.item_movimientos
    }

    fun verFragmentoGastosFijos() {
        binding.botonNV.selectedItemId = R.id.item_gastos_fijos
    }
    
    fun verFragmentoDeudas() {
        binding.botonNV.selectedItemId = R.id.item_balance
    }

    fun verFragmentoInicio() {
        binding.botonNV.selectedItemId = R.id.item_inicio
    }

    private fun verificarTutorial() {
        val uid = firebaseAuth.uid ?: return
        AppTourManager.checkTutorialStatus(uid) { completado ->
            if (!completado) {
                // Pequeño delay para asegurar que el fragmento se cargó
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
            verFragmentoInicio() // Regresar al inicio al finalizar
        }
    }

    private fun iniciarTourGuiado() {
        // PASO 1: Dashboard - Balance General
        tutorialHelper.waitForViewAndShow(
            R.id.card_balance_general,
            "Resumen Global",
            "Aquí tienes tu balance total histórico de todos tus registros.",
            true
        ) {
            // PASO 2: Dashboard - Flujo del Mes
            tutorialHelper.waitForViewAndShow(
                R.id.card_ingresos,
                "Flujo del Mes",
                "Consulta rápidamente tus ingresos y gastos del mes actual.",
                true
            ) {
                // PASO 3: Dashboard - Consulta Rápida
                tutorialHelper.waitForViewAndShow(
                    R.id.rv_ultimos_movimientos,
                    "Consulta desde Inicio",
                    "Desliza hacia abajo para ver tus 5 registros más recientes sin cambiar de pantalla.",
                    true
                ) {
                    // PASO 4: Transición a Movimientos
                    verFragmentoMovimientos()
                    tutorialHelper.waitForViewAndShow(
                        R.id.cv_resumen_mov,
                        "Gestión de Movimientos",
                        "Aquí puedes ver el historial de tus transacciones y filtrar por fecha.",
                        true
                    ) {
                        // PASO 5: Agregar Movimiento
                        tutorialHelper.waitForViewAndShow(
                            R.id.FABAgregarNota,
                            "¿Cómo Agregar Datos?",
                            "Toca este botón (+) para registrar un nuevo ingreso o gasto rápidamente.",
                            false
                        ) {
                            // PASO 6: Transición a Gastos Fijos
                            verFragmentoGastosFijos()
                            tutorialHelper.waitForViewAndShow(
                                R.id.cv_total,
                                "Gastos Recurrentes",
                                "Administra tus suscripciones y pagos mensuales fijos.",
                                true
                            ) {
                                // PASO 7: Transición a Deudas
                                verFragmentoDeudas()
                                tutorialHelper.waitForViewAndShow(
                                    R.id.cv_total_deudas,
                                    "Control de Deudas",
                                    "Mantén el control de lo que debes y tus pagos realizados.",
                                    true
                                ) {
                                    // PASO 8: Transición a Perfil
                                    verFragmentoPerfil()
                                    tutorialHelper.waitForViewAndShow(
                                        R.id.sw_tema,
                                        "Personalización",
                                        "Activa el modo oscuro para una experiencia visual más cómoda."
                                    ) {
                                        // PASO 9: Vinculación Google
                                        tutorialHelper.waitForViewAndShow(
                                            R.id.btn_google_auth,
                                            "Protege tus Datos",
                                            "Víncula tu cuenta de Google Drive para que nunca pierdas tu información.",
                                            true
                                        ) {
                                            // PASO 10: Respaldo Manual
                                            tutorialHelper.waitForViewAndShow(
                                                R.id.btn_backup_now,
                                                "Sincronización en la Nube",
                                                "Realiza copias de seguridad manuales aquí o configura respaldos automáticos.",
                                                true
                                            ) {
                                                // PASO 11: Finalización
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
