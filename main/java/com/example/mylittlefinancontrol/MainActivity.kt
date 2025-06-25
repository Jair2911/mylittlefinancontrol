package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mylittlefinancontrol.Fragmentos.FragmentBalance
import com.example.mylittlefinancontrol.Fragmentos.FragmentInicio
import com.example.mylittlefinancontrol.Fragmentos.FragmentMovimiento
import com.example.mylittlefinancontrol.Fragmentos.FragmentPerfil
import com.example.mylittlefinancontrol.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
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
                else->{
                    verFragmentoInicio()
                    true
                }
            }
        }
    }

    private fun irOpcionesLogin() {
        startActivity(Intent(applicationContext, OpcionesLoginActivity::class.java))
        finishAffinity()
    }

    private fun verFragmentoPerfil(){
        binding.appName.text = "Perfil"

        val fragment = FragmentPerfil()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFL.id, fragment, "Fragmento Perfil" )
        fragmentTransaction.commit()
    }
    private fun verFragmentoMovimientos(){
        binding.appName.text = "Movimientos"

        val fragment = FragmentMovimiento()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFL.id, fragment, "Fragmento Movimiento" )
        fragmentTransaction.commit()
    }
    private fun verFragmentoBalance(){
        binding.appName.text = "Balance"

        val fragment = FragmentBalance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFL.id, fragment, "Fragmento Balance" )
        fragmentTransaction.commit()
    }

    private fun verFragmentoInicio(){
        binding.appName.text = "My Little FinanControl"

        val fragment = FragmentInicio()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFL.id, fragment, "Fragmento Inicio" )
        fragmentTransaction.commit()
    }
}