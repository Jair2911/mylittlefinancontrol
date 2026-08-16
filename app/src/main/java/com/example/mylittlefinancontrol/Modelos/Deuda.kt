package com.example.mylittlefinancontrol.Modelos

data class Deuda(
    var id: String = "",
    var tipo: String = "",
    var concepto: String = "",
    var descripcion: String = "",
    var totalDeuda: Double = 0.0,
    var totalPagado: Double = 0.0,
    var fechaInicio: String = "",
    var fechaFin: String = ""
)
