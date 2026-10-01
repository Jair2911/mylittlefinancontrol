package com.example.mylittlefinancontrol

class Movimiento (
    val id: Int, 
    val descripcion: String, 
    val valor : Int, 
    val tipo_reg : String, 
    val fecha : String, // Fecha del movimiento (ej. cuando ocurrió la compra)
    val gastoFijoId: Int? = null,
    val quincenaPagada: Int? = null, // 0 para mensual, 1 para Q1, 2 para Q2
    val categoria: String = "Varios", // Categoría elegida (ej. Supermercado, Ocio, Películas, Préstamo, etc.)
    val fechaCreacion: String = "" // Fecha en la que se guardó el registro en la app
)
