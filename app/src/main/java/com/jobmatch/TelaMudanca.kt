package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.jobmatch.databinding.ActivityTelaMudancaBinding

class TelaMudanca : AppCompatActivity() {

    private lateinit var binding: ActivityTelaMudancaBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla o layout usando View Binding
        binding = ActivityTelaMudancaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase Auth
        auth = FirebaseAuth.getInstance()

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
        
        // Configura o botão de voltar
        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnConfirmar.text = ""
            binding.changePasswordProgressBar.visibility = View.VISIBLE
            binding.btnConfirmar.isEnabled = false
        } else {
            binding.btnConfirmar.text = "Confirmar"
            binding.changePasswordProgressBar.visibility = View.GONE
            binding.btnConfirmar.isEnabled = true
        }
    }

    private fun confirmarNovaSenha() {
        // Limpa erros anteriores
        binding.tilNovaSenha.error = null
        binding.tilConfirmaSenha.error = null

        val novaSenha = binding.etNovaSenha.text.toString()
        val confirmaSenha = binding.etConfirmaSenha.text.toString()

        // 1. Verifica se os campos estão vazios
        if (novaSenha.isEmpty()) {
            binding.tilNovaSenha.error = "Campo obrigatório"
            return
        }
        if (confirmaSenha.isEmpty()) {
            binding.tilConfirmaSenha.error = "Campo obrigatório"
            return
        }

        // 2. Verifica se as senhas são iguais
        if (novaSenha != confirmaSenha) {
            binding.tilConfirmaSenha.error = "As senhas não correspondem"
            return
        }

        // 3. Verifica se a nova senha atende aos critérios (já que o usuário está vendo na tela)
        if (!isSenhaValida(novaSenha)) {
            binding.tilNovaSenha.error = "A senha não atende aos critérios de segurança."
            return
        }

        showLoading(true)

        // 4. Altera a senha no Firebase Auth
        val user = auth.currentUser
        if (user == null) {
            showLoading(false)
            Toast.makeText(this, "Nenhum usuário autenticado. Por favor, reinicie o processo.", Toast.LENGTH_LONG).show()
            // Redireciona para o login para evitar ficar preso na tela
            val intent = Intent(this, TelaLogin::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        user.updatePassword(novaSenha).addOnCompleteListener { task ->
            showLoading(false)
            if (task.isSuccessful) {
                Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, TelaMudancaSucesso::class.java)
                startActivity(intent)
                finishAffinity() // Limpa toda a pilha de recuperação
            } else {
                val errorMessage = when (task.exception) {
                    is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Tente uma mais forte."
                    else -> "Falha ao alterar a senha: ${task.exception?.message}"
                }
                binding.tilNovaSenha.error = errorMessage
            }
        }
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
