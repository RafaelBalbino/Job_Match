package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaRecuperarSenhaBinding

class TelaRecuperarSenha : AppCompatActivity() {

    // Declara o objeto de binding para acessar as views do XML
    private lateinit var binding: ActivityTelaRecuperarSenhaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla o layout usando o View Binding
        binding = ActivityTelaRecuperarSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura o padding para não sobrepor as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configura o listener de clique para o botão de enviar (usando o ID correto do XML: 'button')
        binding.button.setOnClickListener {
            enviarCodigoRecuperacao()
        }

        // Configura o botão de voltar (usando o ID correto do XML: 'btn_voltar_inicio')
        binding.btnVoltarInicio.setOnClickListener {
            finish() // Fecha a tela atual e volta para a anterior na pilha de atividades.
        }
    }

    private fun enviarCodigoRecuperacao() {
        // Pega o texto digitado no campo (usando o ID correto do XML: 'txt_endereco')
        val endereco = binding.txtEndereco.text.toString().trim()

        // Verifica se o campo não está vazio
        if (endereco.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu e-mail ou celular.", Toast.LENGTH_SHORT).show()
            return
        }

        // Cria a Intent para iniciar a próxima tela (TelaCodigoSenha)
        val intent = Intent(this, TelaCodigoSenha::class.java)

        // Adiciona o endereço (email/celular) como um "extra" para ser recuperado na próxima tela
        intent.putExtra("ENDERECO_RECUPERACAO", endereco)

        // Inicia a Activity
        startActivity(intent)
    }
}
