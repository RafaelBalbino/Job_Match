package com.jobmatch

//Serviço
data class Servico(
    val nomeServico: String = "",
    val descricaoServico: String = "",
    val fotoServico: String = "", //URL para o Firebase Storage
    val modeloCobranca: String = "", //Ex: "Por Hora", "Preço Fixo"
    val categoria: String = "" //Ex: "TI", "Elétrica", "Geral"
) {

    //Método para cadastrar (embora a lógica esteja na classe Autônomo)
    fun cadastrarServico() {
        println("Cadastrando detalhes do serviço no Firestore.")
    }

}