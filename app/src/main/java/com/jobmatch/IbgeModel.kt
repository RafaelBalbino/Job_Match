package com.jobmatch

import com.google.gson.annotations.SerializedName

data class EstadoIbge(
    val sigla: String,
    val nome: String,
)

data class Municipio(
    val id: Long,
    val nome: String,

    @SerializedName("microrregiao")
    val microrregiao: Microrregiao
)

data class Microrregiao(
    @SerializedName("mesorregiao")
    val mesorregiao: Mesorregiao
)

data class Mesorregiao(
    @SerializedName("UF")
    val uf: EstadoIbge
)