package com.jobmatch

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Representa um Serviço oferecido por um Autônomo.
 * Esta classe é compatível com o Firestore para ser salva e lida diretamente.
 */
@IgnoreExtraProperties
data class Servico(
    val uidUsuario: String? = null, // ID do Autônomo que oferece o serviço
    val nomeServico: String? = null,
    val descricaoServico: String? = null,
    val fotoServico: String? = null,    // URL para a imagem no Firebase Storage
    val modeloCobranca: String? = null, // Ex: "Por Hora", "Preço Fixo"
    val categoria: String? = null,      // Ex: "TI", "Elétrica", "Geral"
    val preco: Double? = 0.0
) {
    // Construtor vazio para o Firebase
    constructor() : this(null, null, null, null, null, null, 0.0)
}
