package com.example.mylittlefinancontrol.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.FragmentBalanceBinding

import java.text.NumberFormat
import java.util.Locale

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentBalance.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentBalance : Fragment() {
    // 1. Declara una variable para el binding
    private var _binding: FragmentBalanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // 2. Inflar el binding
        _binding = FragmentBalanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 3. Acceder al botón directamente desde el binding
        binding.ivGuardarInformacion.setOnClickListener {
            Toast.makeText(context, "Botón presionado", Toast.LENGTH_SHORT).show()
        }
        binding.etSueldoMensual.setText("2.5000.000")
        binding.etDeudas.setText("1.800.000")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 4. Limpiar el binding para evitar leaks de memoria
        _binding = null
    }

}