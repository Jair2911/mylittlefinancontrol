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
import com.example.mylittlefinancontrol.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        aplicarTemaGuardado()
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser == null){
            irOpcionesLogin()
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

        // Único punto de control para la navegación del menú inferior
        binding.botonNV.setOnItemSelectedListener { item ->
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
}
