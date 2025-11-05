package com.jobmatch

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Representa o usuário principal do sistema.
 * Agora, em vez de herança, ele "compõe" os perfis usando as classes Contratante e Autonomo.
 */
@IgnoreExtraProperties
// Em Usuario.kt
data class Usuario(
    val uid: String? = null,
    val nome: String? = null,
    val email: String? = null,
    val fotoUrl: String? = null, // <--- CAMPO DE FOTO PRINCIPAL AQUI
    val numeroTelefone: String? = null,
    val endereco: Endereco? = null,
    val contratante: Contratante? = null,
    val autonomo: Autonomo? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null)
}

