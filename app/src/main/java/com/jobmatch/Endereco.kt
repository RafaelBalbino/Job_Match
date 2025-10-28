package com.jobmatch

// Endereço
data class Endereco(
    var rua: String = "",
    var cidade: String = "",
    var estado: String = "",
    var cep: String = "",
    var pais: String = ""
) {
    //Métodos de Endereço
    fun rastrear() {
        //Lógica para rastrear a localização (ex: abrir mapa ou usar GeoPoint)
        println("Rastreando endereço...")
    }
}