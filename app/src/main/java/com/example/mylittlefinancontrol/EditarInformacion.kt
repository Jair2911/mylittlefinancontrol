package com.example.mylittlefinancontrol

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.mylittlefinancontrol.databinding.ActivityEditarInformacionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditarInformacion : AppCompatActivity() {

    private lateinit var binding: ActivityEditarInformacionBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarInformacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)

        progressDialog.setTitle("espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        cargarInformacion()

        binding.ibRegregar.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnActualizar.setOnClickListener {
            validarInformacion()
        }
    }

    private fun cargarInformacion(){
        progressDialog.dismiss()

        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = "${snapshot.child("nombres").value}"
                    val image = "${snapshot.child("image").value}"

                    binding.etNombres.setText(nombre)

                    try {
                        Glide.with(applicationContext)
                            .load(image)
                            .placeholder(R.drawable.ic_perfil)
                            .into(binding.ivPerfil)
                    }catch (e: Exception){
                        Toast.makeText(applicationContext, "error ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            });
    }

    private var nombres = ""

    private fun validarInformacion(){
        nombres = binding.etNombres.text.toString().trim()

        if (nombres.isEmpty()){
            binding.etNombres.error = "Ingrese nombres"
            binding.etNombres.requestFocus()
        }else{
            actualizarInfo()
        }
    }

    private fun actualizarInfo(){
        progressDialog.setMessage("actualizando informacion")
        progressDialog.show()

        val hashMap : HashMap<String, Any> = HashMap()
        hashMap["nombres"] = nombres

        val ref = FirebaseDatabase.getInstance().getReference("usuarios")
        ref.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "Informacion actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "error ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}