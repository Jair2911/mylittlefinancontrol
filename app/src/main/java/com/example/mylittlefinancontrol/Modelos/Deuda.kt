package com.example.mylittlefinancontrol.Modelos

data class Deuda(
    var id: String = "",
    var tipo: String = "",
    var concepto: String = "",
    var descripcion: String = "",
    var totalDeuda: Double = 0.0,
    var totalPagado: Double = 0.0,
    var fechaInicio: String = "",
    var fechaFin: String = "",
    var tasaInteres: Double = 0.0,
    var tipoInteres: String = "Anual",
    var cuotas: Int = 12,
    var valorCuota: Double = 0.0
)
