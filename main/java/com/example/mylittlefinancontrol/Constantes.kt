package com.example.mylittlefinancontrol

import android.text.format.DateFormat
import java.util.Locale
import java.util.Calendar

object Constantes {
    fun obtenerTiempoD() : Long {
        return System.currentTimeMillis()
    }

    fun formatoFecha(tiempo: Long):String{
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = tiempo

        return DateFormat.format("yyyy-MM-dd HH:mm:ss", calendar).toString()
    }
}