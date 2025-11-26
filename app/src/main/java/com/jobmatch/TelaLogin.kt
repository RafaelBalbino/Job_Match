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
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.jobmatch.databinding.ActivityTelaLoginBinding // Importe a classe de binding

class TelaLogin : AppCompatActivity() {

    // Declare a variável para o view binding
    private lateinit var binding: ActivityTelaLoginBinding
    // Declare a variável do Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Se não houver usuário logado, continua e infla o layout da tela de login
        binding = ActivityTelaLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- LÓGICA DO BOTÃO INICIAR SESSÃO ---
        binding.btnIniciarSessao.setOnClickListener {
            // Limpa erros anteriores
            binding.tilEmailLogin.error = null
            binding.tilSenhaLogin.error = null
            
            val email = binding.txtEmailLogin.text.toString().trim()
            val senha = binding.txtSenhaLogin.text.toString()

            // 1. Checar se os campos estão preenchidos
            if (email.isEmpty()) {
                binding.tilEmailLogin.error = "Email é obrigatório"
                return@setOnClickListener
            }
            if (senha.isEmpty()) {
                binding.tilSenhaLogin.error = "Senha é obrigatória"
                return@setOnClickListener
            }
            
            showLoading(true)

            // 2. Lógica de login com Firebase
            auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this) { task ->
                    showLoading(false)
                    if (task.isSuccessful) {
                        // Login bem-sucedido
                        Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()

                        // Navega para a tela principal
                        val intent = Intent(this, TelaMenuPrincipal::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Finaliza a TelaLogin
                    } else {
                        // Trata os erros de login
                        val exception = task.exception
                        val errorMessage = when (exception) {
                            is FirebaseAuthInvalidUserException -> "Nenhuma conta encontrada com este e-mail."
                            is FirebaseAuthInvalidCredentialsException -> "Senha incorreta. Tente novamente."
                            else -> "Falha na autenticação: Verifique sua conexão."
                        }
                        
                        if (exception is FirebaseAuthInvalidUserException) {
                            binding.tilEmailLogin.error = errorMessage
                        } else if (exception is FirebaseAuthInvalidCredentialsException) {
                            binding.tilSenhaLogin.error = errorMessage
                        } else {
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        // --- LÓGICA EXISTENTE PARA LINKS (ADAPTADA PARA VIEW BINDING) ---
        setupClickableTextToCadastro()
        setupClickableTextToRecuperarSenha()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnIniciarSessao.text = ""
            binding.loginProgressBar.visibility = View.VISIBLE
            binding.btnIniciarSessao.isEnabled = false
        } else {
            binding.btnIniciarSessao.text = getString(R.string.login_button_text) // Usa a string do resources
            binding.loginProgressBar.visibility = View.GONE
            binding.btnIniciarSessao.isEnabled = true
        }
    }

    private fun setupClickableTextToCadastro() {
        val textViewToCadastro = binding.textView13
        val fullTextToCadastro = textViewToCadastro.text.toString()
        val spannableStringToCadastro = SpannableString(fullTextToCadastro)
        val linkColorCadastro = MaterialColors.getColor(this, MaterialR.attr.colorSecondary, Color.BLACK)
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
        val startRecuperarSenha = 0
        val endRecuperarSenha = fullTextRecuperarSenha.length
        val linkColor = MaterialColors.getColor(this, MaterialR.attr.colorSecondary, Color.BLACK)

        val clickableSpanRecuperarSenha = object : ClickableSpan() {
            override fun onClick(widget: View) {
                 // Apontando para a tela correta de recuperação de senha
                val intent = Intent(this@TelaLogin, TelaRecuperarSenha::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = linkColor
            }
        }
        spannableStringRecuperarSenha.setSpan(clickableSpanRecuperarSenha, startRecuperarSenha, endRecuperarSenha, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textViewToRecuperarSenha.text = spannableStringRecuperarSenha
        textViewToRecuperarSenha.movementMethod = LinkMovementMethod.getInstance()
        textViewToRecuperarSenha.highlightColor = Color.TRANSPARENT
    }
}
