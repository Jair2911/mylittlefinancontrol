package com.example.mylittlefinancontrol

class Movimiento (
    val id: Int, 
    val descripcion: String, 
    val valor : Int, 
    val tipo_reg : String, 
    val fecha : String,
    val gastoFijoId: Int? = null,
    val quincenaPagada: Int? = null // 0 para mensual, 1 para Q1, 2 para Q2
)