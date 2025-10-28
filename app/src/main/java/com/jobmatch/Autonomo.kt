package com.jobmatch

//Autônomo
data class Autonomo(
    override var nome: String = "",
    override var numeroTelefone: String = "",
    override var email: String = "",
    val cnpj: String = "",
    val especializacao: String = "",
    val formacao: String = ""
) : Usuario(nome, numeroTelefone, email) {

    //Métodos de Autônomo
    fun cadastrarServico(servico: Servico) {
        //Lógica para adicionar um novo Serviço oferecido ao Firebase
        println("Autônomo cadastrou um novo serviço: ${servico.nomeServico}")
    }

    fun negociarPedido() {
        //Lógica para iniciar o chat de negociação para um pedido
        println("Autônomo iniciou a negociação de um pedido.")
    }
}