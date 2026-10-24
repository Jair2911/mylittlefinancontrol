package com.example.mylittlefinancontrol.Modelos

data class ResumenFinanciero(
    val balanceTotal: Double,
    val totalGastosFijos: Double,
    val totalDeudasPendientes: Double,
    val totalAhorros: Double = 0.0,
    val ahorroEnCuenta: Double = 0.0,
    val ahorroEnAlcancia: Double = 0.0
)
