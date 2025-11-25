package com.jobmatch

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pedidos(
    @DocumentId val id: String = "",
    val nomeSolicitacao: String = "",
    val telefoneSolicitante: String = "",
    val emailSolicitante: String = "",
    val tipoServico: String = "",
    val descricaoServico: String = "",
    val cidade: String = "",
    val estado: String = "",
    val tempoServico: String = "",
    val anexos: List<String> = emptyList(),
    val status: String = "disponivel",
    val contratanteId: String = "",
    val autonomo: String = "",

    @ServerTimestamp
    val dataHora: Date? = null // CORREÇÃO FINAL: Date é Parcelable e compatível com o Timestamp do servidor.
) : Parcelable