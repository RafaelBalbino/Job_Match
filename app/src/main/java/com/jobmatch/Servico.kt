package com.jobmatch // Mantendo no pacote principal

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

/**
 * Representa um Serviço oferecido por um Autônomo.
 * Esta classe é compatível com o Firestore para ser salva e lida diretamente.
 * Também é Parcelable para poder ser passada entre Activities.
 */
@IgnoreExtraProperties
@Parcelize
data class Servico(
    // Propriedades movidas para o construtor primário com valores padrão
    var id: String = "",
    val uidUsuario: String = "",
    val nomeServico: String = "",
    val descricaoServico: String = "",
    val valorServico: Double = 0.0,
    val uidAutonomo: String = "",
    val nomeAutonomo: String = "",
    val uidServico: String = "",
    val categoria: String = "",
    val modeloCobranca: String = "",

    // Este campo pode ser nulo, pois um serviço pode não ter foto
    val fotoServico: String? = null

    // O construtor secundário foi removido, pois a data class já lida com valores padrão.
) : Parcelable
