package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.EditarInformacion
import com.example.mylittlefinancontrol.OpcionesLoginActivity
import com.example.mylittlefinancontrol.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FragmentPerfil : Fragment() {

    private lateinit var binding: FragmentPerfilBinding
    private lateinit var mContext: Context
    private lateinit var firebaseAuth: FirebaseAuth

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
        
        cargarInformacion()
        configurarTema()

        binding.btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, OpcionesLoginActivity::class.java))
            activity?.finishAffinity()
        }

        binding.btnActualizar.setOnClickListener {
            startActivity(Intent(mContext, EditarInformacion::class.java))
        }
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

    private fun cargarInformacion() {
        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val email = "${snapshot.child("email").value}"
                    val proveedor = "${snapshot.child("proveedor").value}"
                    var tiempoR = "${snapshot.child("tiempoR").value}"

                    if (tiempoR == "null") {
                        tiempoR = "0"
                    }

                    val fecha = Constantes.formatoFecha(tiempoR.toLong())

                    binding.tvNombres.text = nombres
                    binding.tvEmail.text = email
                    binding.tvProveedor.text = proveedor
                    binding.tvMienbroDesde.text = fecha
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
