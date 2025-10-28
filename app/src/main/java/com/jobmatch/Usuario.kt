package com.jobmatch

//Usuário
open class Usuario(
    open var nome: String = "",
    open var numeroTelefone: String = "",
    open var email: String = "",
    private var senha: String = "", //Propriedade privada por segurança
    open var endereco: Endereco? = null //Relação 0..1 com Endereço
){
    //Métodos de Usuário
    fun login() {
        //Lógica de autenticação com Firebase Auth
        println("Usuário tentando logar...")
    }

    fun cadastro() {
        //Lógica para registrar novo usuário no Firebase Auth e Firestore
        println("Realizando cadastro...")
    }

}