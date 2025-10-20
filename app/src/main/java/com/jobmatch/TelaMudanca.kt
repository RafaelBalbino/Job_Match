package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaMudancaBinding

class TelaMudanca : AppCompatActivity() {

    private lateinit var binding: ActivityTelaMudancaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla o layout usando View Binding
        binding = ActivityTelaMudancaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configura o listener do botão de confirmação
        binding.btnConfirmar.setOnClickListener {
            confirmarNovaSenha()
        }
    }

    private fun confirmarNovaSenha() {
        val novaSenha = binding.etNovaSenha.text.toString()
        val confirmaSenha = binding.etConfirmaSenha.text.toString()

        // 1. Verifica se os campos estão vazios
        if (novaSenha.isEmpty() || confirmaSenha.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha ambos os campos de senha.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Verifica se as senhas são iguais
        if (novaSenha != confirmaSenha) {
            Toast.makeText(this, "As senhas não correspondem. Tente novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Verifica se a nova senha atende aos critérios
        if (!isSenhaValida(novaSenha)) {
            Toast.makeText(this, "A senha não atende aos critérios de segurança.", Toast.LENGTH_LONG).show()
            return
        }

        // Se tudo estiver correto, exibe sucesso e navega para a próxima tela
        Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show()

        // Inicia a activity de sucesso
        val intent = Intent(this, TelaMudancaSucesso::class.java)
        startActivity(intent)
        finish() // Finaliza esta tela para o usuário não voltar
    }

    /**
     * Valida a senha com base nos critérios definidos.
     * @param senha A senha a ser validada.
     * @return true se a senha for válida, false caso contrário.
     */
    private fun isSenhaValida(senha: String): Boolean {
        val temOitoCaracteres = senha.length >= 8
        val temLetraMaiuscula = senha.any { it.isUpperCase() }
        val temNumero = senha.any { it.isDigit() }
        val temSimbolo = senha.any { !it.isLetterOrDigit() }

        return temOitoCaracteres && temLetraMaiuscula && temNumero && temSimbolo
    }
}
