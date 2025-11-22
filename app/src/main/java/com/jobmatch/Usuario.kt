package com.jobmatch

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Representa o usuário principal do sistema.
 * Agora, em vez de herança, ele "compõe" os perfis usando as classes Contratante e Autonomo.
 */
@IgnoreExtraProperties
data class Usuario(
    val uid: String? = null,
    val nome: String? = null,
    val email: String? = null,
    val fotoUrl: String? = null,
    val numeroTelefone: String? = null,
    val cidade: String? = null,      // Adicionado aqui
    val estado: String? = null,        // Adicionado aqui
    val contratante: Contratante? = null,
    val autonomo: Autonomo? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null, null)
}
