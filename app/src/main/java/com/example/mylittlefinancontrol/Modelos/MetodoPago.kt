package com.example.mylittlefinancontrol.Modelos

data class MetodoPago(
    var id: String = "",
    var nombre: String = "",
    var tipo: String = "EFECTIVO", // EFECTIVO, TARJETA_DEBITO, TARJETA_CREDITO, CUENTA_BANCARIA, BILLETERA_DIGITAL
    var ultimos4: String = "",
    var esPredeterminado: Boolean = false
) {
    fun nombreFormateado(): String {
        return if (ultimos4.isNotBlank() && tipo != "EFECTIVO") {
            "$nombre (*$ultimos4)"
        } else {
            nombre.ifBlank { "Efectivo" }
        }
    }
}
