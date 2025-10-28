package com.jobmatch

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jobmatch.databinding.ActivityTelaPerfilAutonomoBinding

class TelaPerfilAutonomo : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaPerfilAutonomoBinding.inflate( layoutInflater )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //botão Editar Perfil - Sempre que apertar esse botão vai iniciar a Activity Editar Perfil
        binding.btnEditarPerfil.setOnClickListener {
            //Abrir tela Edição Perfil Autonomo
            startActivity(
                Intent(
                this,
                TelaEdicaoPerfilAutonomo::class.java
                )
            )
        }

        binding.btnCadastrarServico.setOnClickListener {
            //Abrir tela Cadastrar Serviço
            startActivity(
                Intent(
                this,
                CadastrarServico::class.java
                )
            )
        }


    }
}