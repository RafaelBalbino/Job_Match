package com.jobmatch

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Representa os dados específicos de um perfil Autônomo.
 * Esta classe agora é usada como um objeto aninhado dentro da classe Usuario.
 */
@IgnoreExtraProperties
data class Autonomo(
    val cnpj: String? = null,
    val especializacao: String? = null,
    val formacao: String? = null
) {
    // Construtor vazio para o Firebase
    constructor() : this(null, null, null)
}
