package com.jobmatch

import com.google.firebase.firestore.IgnoreExtraProperties

// Classe de dados para representar um Endereço, compatível com o Firestore.
@IgnoreExtraProperties
data class Endereco(
    val rua: String? = null,
    val cidade: String? = null,
    val estado: String? = null,
    val cep: String? = null,
    val pais: String? = null
) {
    // Um construtor vazio é necessário para que o Firebase possa recriar o objeto a partir dos dados.
    constructor() : this(null, null, null, null, null)
}
