package com.jobmatch

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jobmatch.databinding.ActivityTelaLoginBinding // Importe a classe de binding

class TelaLogin : AppCompatActivity() {

    // Declare a variável para o view binding
    private lateinit var binding: ActivityTelaLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla o layout usando o view binding
        binding = ActivityTelaLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- LÓGICA DO BOTÃO INICIAR SESSÃO ---
        binding.btnIniciarSessao.setOnClickListener {
            val email = binding.txtEmailLogin.text.toString().trim()
            // ALTERAÇÃO AQUI: de txtEmailSenha2 para txtSenhaLogin
            val senha = binding.txtSenhaLogin.text.toString()

            // 1. Checar se os campos estão preenchidos
            if (email.isEmpty()) {
                binding.txtEmailLogin.error = "Email é obrigatório"
                return@setOnClickListener // Para a execução aqui
            }
            if (senha.isEmpty()) {
                // ALTERAÇÃO AQUI: de txtEmailSenha2 para txtSenhaLogin
                binding.txtSenhaLogin.error = "Senha é obrigatória"
                return@setOnClickListener // Para a execução aqui
            }

            // 2. Lógica de login de teste
            if (email == "jobmatch@hotmail.com" && senha == "12345") {
                Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()

                // Navega para a tela principal
                val intent = Intent(this, TelaMenuPrincipal::class.java)
                // Limpa as activities anteriores para que o usuário não volte para a tela de login
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Finaliza a TelaLogin
            } else {
                // Credenciais incorretas
                Toast.makeText(this, "Email ou senha incorretos.", Toast.LENGTH_LONG).show()
            }
        }


        // --- LÓGICA EXISTENTE PARA LINKS (ADAPTADA PARA VIEW BINDING) ---

        // Link para TelaCadastro
        setupClickableTextToCadastro()

        // Link para TelaRecuperarSenha
        setupClickableTextToRecuperarSenha()
    }

    private fun setupClickableTextToCadastro() {
        val textViewToCadastro = binding.textView13
        val fullTextToCadastro = textViewToCadastro.text.toString()
        val spannableStringToCadastro = SpannableString(fullTextToCadastro)
        val linkColorCadastro = ContextCompat.getColor(this, R.color.texto_secundario)
        val clickableTextCadastro = "Cadastre-se aqui"
        val startCadastro = fullTextToCadastro.indexOf(clickableTextCadastro)

        if (startCadastro != -1) {
            val endCadastro = startCadastro + clickableTextCadastro.length
            val clickableSpanCadastro = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@TelaLogin, TelaCadastro::class.java)
                    startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = linkColorCadastro
                    ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
            spannableStringToCadastro.setSpan(clickableSpanCadastro, startCadastro, endCadastro, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textViewToCadastro.text = spannableStringToCadastro
            textViewToCadastro.movementMethod = LinkMovementMethod.getInstance()
            textViewToCadastro.highlightColor = Color.TRANSPARENT
        } else {
            Log.e("TelaLogin", "Texto clicável '$clickableTextCadastro' não encontrado em textView13.")
        }
    }

    private fun setupClickableTextToRecuperarSenha() {
        val textViewToRecuperarSenha = binding.lblEsqueceuSenha
        val fullTextRecuperarSenha = textViewToRecuperarSenha.text.toString()
        val spannableStringRecuperarSenha = SpannableString(fullTextRecuperarSenha)
        val linkColorRecuperarSenha = ContextCompat.getColor(this, R.color.texto_secundario)
        val startRecuperarSenha = 0
        val endRecuperarSenha = fullTextRecuperarSenha.length

        val clickableSpanRecuperarSenha = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Checando: O código já está corretamente apontando para TelaRecuperarSenha. Nenhuma mudança necessária.
                val intent = Intent(this@TelaLogin, TelaRecuperarSenha::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = linkColorRecuperarSenha
            }
        }
        spannableStringRecuperarSenha.setSpan(clickableSpanRecuperarSenha, startRecuperarSenha, endRecuperarSenha, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textViewToRecuperarSenha.text = spannableStringRecuperarSenha
        textViewToRecuperarSenha.movementMethod = LinkMovementMethod.getInstance()
        textViewToRecuperarSenha.highlightColor = Color.TRANSPARENT
    }
}
