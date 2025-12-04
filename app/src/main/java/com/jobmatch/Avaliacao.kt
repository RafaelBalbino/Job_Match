package com.jobmatch

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa o modelo de uma Avaliação no Firestore.
 * Segue o padrão do app, com construtor vazio e anotações para segurança.
 */
@IgnoreExtraProperties
data class Avaliacao(
    val id: String = "",
    val pedidoId: String = "",
    val autonomoId: String = "",
    val contratanteId: String = "",
    val contratanteNome: String = "",
    val contratanteFotoUrl: String? = null,
    val nota: Double = 0.0,
    val comentario: String = "",
    @ServerTimestamp val dataHora: Date? = null,
    val servicoNome: String? = null // Campo adicionado para o nome do serviço
) {
    // Construtor vazio exigido pelo Firebase para a deserialização (toObject())
    constructor() : this("", "", "", "", "", null, 0.0, "", null, null)
}
