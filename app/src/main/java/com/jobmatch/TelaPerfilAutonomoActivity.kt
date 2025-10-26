package com.jobmatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaPerfilAutonomoBinding

class TelaPerfilAutonomoActivity : AppCompatActivity() {
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
                TelaEdicaoPerfilAutonomoActivity::class.java
                )
            )
        }

        binding.btnCadastrarServico.setOnClickListener {
            //Abrir tela Cadastrar Serviço
            startActivity(
                Intent(
                this,
                CadastrarServicoActivity::class.java
                )
            )
        }


    }
}