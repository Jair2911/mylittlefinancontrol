package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mylittlefinancontrol.EditarInformacion
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.OpcionesLoginActivity
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.FragmentInicioBinding
import com.example.mylittlefinancontrol.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth

import java.text.NumberFormat
import java.util.Locale

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentInicio.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentInicio : Fragment() {
    private lateinit var binding: FragmentInicioBinding
    private lateinit var mContext : Context
    private lateinit var db : NotasDatabaseHelper

    private lateinit var firebaseAuth : FirebaseAuth

    override fun onAttach(context: Context){
        mContext = context
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Override
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentInicioBinding.inflate(layoutInflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        db = NotasDatabaseHelper(mContext)

        binding.fechaUltimoReg.text = getDateLastReg();
        binding.valueAndDescription.text = getValueDescriptionLastReg();
        binding.balance.text = getBalance()
        binding.sueldoMensual.text = getSueldoMensual()
        binding.totalDeudas.text = getTotalDeudas()
    }

    private fun getDateLastReg():String {
        val date = db.getDateLastReg()
        return "Fecha y Hora: ${date}";
    }

    private fun getValueDescriptionLastReg():String {
        val value_m = db.getValue()
        val description = db.getDescripcion()

        val formato = NumberFormat.getInstance(Locale("es", "CO"))
        val resultado = formato.format(value_m)

        return "Descripcion: ${description}, Valor $ ${resultado}";
    }

    private fun getBalance():String {
        //Balance, muestra la suma de los gastos, ingresos, y salario mensual
        val balance = db.getValueAll().toInt()
        val balance_mov = (balance - 2500000)

        val formato = NumberFormat.getInstance(Locale("es", "CO"))
        val resultado = formato.format(balance_mov)

        return "Balance $ ${resultado}";
    }

    private fun getSueldoMensual():String {
        //Total dle sueldo mensual, editado desde aqui
        val sueldo = 2500000

        val formato = NumberFormat.getInstance(Locale("es", "CO"))
        val resultado = formato.format(sueldo)
        return "Sueldo Mensual $ ${resultado}";
    }

    private fun getTotalDeudas():String {
        //suma de todas las deudas registradas, menos las deudas registradas pagadas en los movimientos
        val deuda = 1800000

        val formato = NumberFormat.getInstance(Locale("es", "CO"))
        val resultado = formato.format(deuda)
        return "Total deudas actualizadas $ ${resultado}";
    }
}