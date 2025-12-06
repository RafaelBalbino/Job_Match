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
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaLoginBinding // Importe a classe de binding

class TelaLogin : AppCompatActivity() {

    // Declare a variável para o view binding
    private lateinit var binding: ActivityTelaLoginBinding
    // Declare a variável do Firebase Auth e Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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

            // Lógica para o usuário de suporte
            if (email.equals("suportejobmatch@email.com", ignoreCase = true) && senha == "Suporte12#") {
                navigateToSuporte()
                return@setOnClickListener
            }
            
            showLoading(true)

            // 2. Lógica de login com Firebase
            auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // ETAPA DE VERIFICAÇÃO DE TIPO DE USUÁRIO
                        // Se for um usuário normal, verifica o status da conta (bloqueado/ativo)
                        checkUserStatus()
                    } else {
                        // Trata os erros de login
                        showLoading(false)
                        handleLoginFailure(task.exception)
                    }
                }
        }

        // --- LÓGICA EXISTENTE PARA LINKS (ADAPTADA PARA VIEW BINDING) ---
        setupClickableTextToCadastro()
        setupClickableTextToRecuperarSenha()
    }

    /**
     * Verifica o documento do usuário no Firestore para checar o status de bloqueio.
     */
    private fun checkUserStatus() {
        val userId = auth.currentUser?.uid ?: return // Sai se não houver ID

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userIsBlocked = document.getBoolean("isBlocked") ?: false
                    if (userIsBlocked) {
                        // Se o usuário estiver bloqueado, exibe a mensagem e desloga
                        showLoading(false)
                        Toast.makeText(this, "Esta conta foi bloqueada por um administrador.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                    } else {
                        // Se não estiver bloqueado, atualiza o campo isBlocked para false (para garantir que usuários antigos sejam atualizados)
                        // e prossegue para a tela principal.
                        document.reference.update("isBlocked", false)
                        navigateToMain()
                    }
                } else {
                    // Caso raro: usuário autenticado mas sem documento no Firestore. Prossegue para a tela principal.
                    navigateToMain()
                }
            }
            .addOnFailureListener { 
                // Em caso de falha na leitura (ex: sem internet), permite o login por segurança, 
                // mas não consegue verificar o status de bloqueio.
                Log.e("TelaLogin", "Falha ao verificar o status do usuário.", it)
                navigateToMain()
            }
    }
    
    /**
     * Navega para a tela principal do aplicativo.
     */
    private fun navigateToMain() {
        showLoading(false)
        val intent = Intent(this, TelaMenuPrincipal::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Navega para o painel de suporte.
     */
    private fun navigateToSuporte() {
        showLoading(false)
        val intent = Intent(this, TelaSuporte::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Mostra mensagens de erro apropriadas com base na exceção de login do Firebase.
     */
    private fun handleLoginFailure(exception: Exception?) {
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