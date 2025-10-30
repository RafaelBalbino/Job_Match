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
    val numeroTelefone: String? = null,
    val endereco: Endereco? = null,
    val contratante: Contratante? = null, // Perfil de contratante (pode ser nulo, mas no nosso caso, sempre existirá)
    val autonomo: Autonomo? = null      // Perfil de autônomo (opcional)
) {
    // Construtor vazio para o Firebase
    constructor() : this(null, null, null, null, null, null, null)
}
