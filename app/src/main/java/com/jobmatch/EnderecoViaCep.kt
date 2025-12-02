package com.jobmatch

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


data class EnderecoViaCep(
    val cep: String? = null,
    val logradouro: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val estado: String? = null
)

interface CepService{
    @GET("{cep}/json/")
    fun buscarCep(@Path("cep") cep: String): Call<EnderecoViaCep>
}