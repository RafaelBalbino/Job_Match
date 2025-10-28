package com.jobmatch

//Pedidos
data class Pedidos(
    val nomeSolicitacao: String = "",
    val telefoneSolicitante: String = "",
    val emailSolicitante: String = "",
    val tipoServico: String = "",
    val descricaoServico: String = "",
    val endereco: Endereco? = null,
    val cidade: String = "",
    val estado: String = "",
    val tempoServico: String = "", //Alterado para String (Horário)
    val data: String = "", //Data formatada como String
    val hora: String = "", //Hora formatada como String
    val anexos: List<String> = emptyList(), //Lista de URLs de anexos (Firebase Storage)

    //Chaves de referência para o Firebase (Foreign Keys)
    val contratanteId: String = "",
    val autonomo: String = ""
)