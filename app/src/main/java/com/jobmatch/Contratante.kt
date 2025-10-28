package com.jobmatch

//Contratante
data class Contratante(
    override var nome: String = "",
    override var numeroTelefone: String = "",
    override var email: String = "",
    val cpf: String = ""
) : Usuario(nome, numeroTelefone, email) { //Herda de Usuário

    //Métodos de Contratante
    fun fazerPedido() {
        //Lógica para iniciar a tela de Criação de Pedido
        println("Contratante fez um novo pedido.")
    }

    fun atualizarPedido() {
        //Lógica para modificar um pedido existente (antes da aceitação)
        println("Contratante atualizou um pedido.")
    }

    fun cancelarPedido() {
        //Lógica para cancelar um pedido
        println("Contratante cancelou um pedido.")
    }

}