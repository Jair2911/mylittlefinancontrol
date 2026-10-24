package com.example.mylittlefinancontrol.Modelos

data class Ahorro(
    var id: String = "",
    var titulo: String = "",
    var categoria: String = "Emergencia",
    var montoActual: Double = 0.0,
    var montoObjetivo: Double = 0.0,
    var fechaMeta: String = "",
    var icono: String = "savings",
    var descripcion: String = "",
    var ubicacion: String = "Cuenta / Bolsillo"
)
