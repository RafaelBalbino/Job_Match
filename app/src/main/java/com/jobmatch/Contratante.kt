package com.jobmatch

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Representa os dados específicos de um perfil Contratante.
 * Esta classe agora é usada como um objeto aninhado dentro da classe Usuario.
 */
@IgnoreExtraProperties
data class Contratante(
    val cpf: String? = null
) {
    // Construtor vazio para o Firebase
    constructor() : this(null)
}
