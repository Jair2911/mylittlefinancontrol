package com.example.mylittlefinancontrol.Modelos

data class ResumenPeriodoData(
    val totalIngresos: Double,
    val totalGastos: Double,
    val balanceNeto: Double,
    val listaCategorias: List<CategoriaGastoItem>,
    val tendenciaPorcentualGastos: Double = 0.0,
    val proyeccionCierre: Double = 0.0
)
