package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylittlefinancontrol.AgregarMovimientoActivity
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasAdaptador
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.FragmentMovimientoBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentMovimiento.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentMovimiento : Fragment() {

    private lateinit var binding: FragmentMovimientoBinding
    private lateinit var mContext : Context
    private lateinit var firebaseAuth : FirebaseAuth

    private lateinit var db : NotasDatabaseHelper
    private lateinit var notasAdaptador: NotasAdaptador

    override fun onAttach(context: Context){
        mContext = context
        super.onAttach(context)
    }

    @Override
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMovimientoBinding.inflate(layoutInflater, container, false);

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.FABAgregarNota.setOnClickListener{
            Toast.makeText(mContext, "funciona", Toast.LENGTH_SHORT).show()
            startActivity(Intent(mContext, AgregarMovimientoActivity::class.java))
        }

        db = NotasDatabaseHelper(mContext)
        notasAdaptador = NotasAdaptador(db.getAllNotas(), mContext)
        binding.notasRv.layoutManager = LinearLayoutManager(mContext)
        binding.notasRv.adapter = notasAdaptador
    }

    override fun onResume() {
        super.onResume()
        notasAdaptador.refrescarLista(db.getAllNotas())
    }
}