package com.example.mylittlefinancontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

        verFragmentoInicio()

        binding.icHomeIcon.setOnClickListener {
            verFragmentoInicio()
        }

        binding.icSignOut.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.botonNV.setOnItemSelectedListener{item->
            when(item.itemId){
                R.id.item_perfil -> {
                    verFragmentoPerfil()
                    true
                }
                R.id.item_balance -> {
                    verFragmentoBalance()
                    true
                }
                R.id.item_movimientos -> {
                    verFragmentoMovimientos()
                    true
                }
                R.id.item_gastos_fijos -> {
                    verFragmentoGastosFijos()
                    true
                }
                else->{
                    verFragmentoInicio()
                    true
                }
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

    private fun verFragmentoPerfil(){
        binding.appName.text = "Perfil"
        val fragment = FragmentPerfil()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragmento Perfil")
            .commit()
    }
    
    private fun verFragmentoMovimientos(){
        binding.appName.text = "Movimientos"
        val fragment = FragmentMovimiento()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragmento Movimiento")
            .commit()
    }

    private fun verFragmentoGastosFijos(){
        binding.appName.text = "Gastos Fijos"
        val fragment = FragmentGastosFijos()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragmento Gastos Fijos")
            .commit()
    }
    
    private fun verFragmentoBalance(){
        binding.appName.text = "Balance"
        val fragment = FragmentBalance()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragmento Balance")
            .commit()
    }

    private fun verFragmentoInicio(){
        binding.appName.text = "My Little FinanControl"
        val fragment = FragmentInicio()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragmento Inicio")
            .commit()
    }
}
