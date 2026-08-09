package com.example.mylittlefinancontrol.Servicios

import com.google.firebase.database.FirebaseDatabase

object AppTourManager {

    /**
     * Verifica en Firebase si el usuario ya completó el tutorial.
     */
    fun checkTutorialStatus(uid: String, onResult: (Boolean) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        ref.child("tutorial").get().addOnSuccessListener { snapshot ->
            val completado = snapshot.getValue(Boolean::class.java) ?: false
            onResult(completado)
        }.addOnFailureListener {
            onResult(true) // En caso de error, asumimos completado para no molestar al usuario
        }
    }

    /**
     * Marca el tutorial como completado en Firebase.
     */
    fun completeTutorial(uid: String) {
        val ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        ref.child("tutorial").setValue(true)
    }
}
