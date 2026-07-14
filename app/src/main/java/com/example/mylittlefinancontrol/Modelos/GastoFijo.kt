package com.example.mylittlefinancontrol.Modelos

data class GastoFijo(
    var id: String = "",
    var concepto: String = "",
    var categoria: String = "",
    var monto: Double = 0.0,
    var diaPago: String = "",
    var periodo: String = "Mensual" // Mensual o Quincenal
)
