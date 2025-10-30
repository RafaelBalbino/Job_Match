package com.jobmatch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaCadastroBinding // Importe a classe de binding gerada

class TelaCadastro : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupClickableText()
        setupPasswordFocusListener()
        setupUserTypeSelection()

        binding.btnEnviaCadastro.setOnClickListener {
            cadastrarUsuario()
        }
    }

    /**
     * Configura a visibilidade dos campos de autônomo com base na seleção do RadioGroup.
     */
    private fun setupUserTypeSelection() {
        binding.rgUserType.setOnCheckedChangeListener { _, checkedId ->
            val isFreelancer = checkedId == R.id.rb_freelancer
            toggleFreelancerFields(isFreelancer)
        }
    }

    /**
     * Mostra ou esconde os campos específicos de autônomo.
     */
    private fun toggleFreelancerFields(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        // Anima a visibilidade para uma transição suave
        binding.tilCnpj.visibility = visibility
        binding.tilSpecialization.visibility = visibility
    }

    private fun setupClickableText() {
        val textView = binding.textView21
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)
        val verdeAgua = ContextCompat.getColor(this, R.color.verde_agua)

        val start = fullText.indexOf("Inicie")
        val end = fullText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@TelaCadastro, TelaLogin::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = verdeAgua
                ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }

    private fun setupPasswordFocusListener() {
        binding.txtSenha.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvPasswordRestrictions.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).setDuration(300).setListener(null)
                }
            } else {
                binding.tvPasswordRestrictions.animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.tvPasswordRestrictions.visibility = View.GONE
                    }
                })
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnEnviaCadastro.text = ""
            binding.cadastroProgressBar.visibility = View.VISIBLE
            binding.btnEnviaCadastro.isEnabled = false
        } else {
            binding.btnEnviaCadastro.text = getString(R.string.register_button_text)
            binding.cadastroProgressBar.visibility = View.GONE
            binding.btnEnviaCadastro.isEnabled = true
        }
    }

    private fun cadastrarUsuario() {
        // Limpa erros anteriores
        binding.tilNome.error = null
        binding.tilEmail.error = null
        binding.tilTelefone.error = null
        binding.tilSenha.error = null
        binding.tilConfirmarSenha.error = null
        binding.tilSpecialization.error = null

        val nome = binding.txtNome.text.toString().trim()
        val email = binding.txtEmail.text.toString().trim()
        val telefone = binding.txtTelefone.text.toString().trim()
        val senha = binding.txtSenha.text.toString()
        val confirmarSenha = binding.txtConfirmarSenha.text.toString()
        val politicasAceitas = binding.radioButton.isChecked
        val isFreelancer = binding.rbFreelancer.isChecked

        if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos básicos.", Toast.LENGTH_SHORT).show()
            return
        }
        if (senha != confirmarSenha) {
            binding.tilConfirmarSenha.error = "As senhas não coincidem"
            return
        }

        var autonomo: Autonomo? = null
        if(isFreelancer) {
            val especializacao = binding.txtSpecialization.text.toString().trim()
            val cnpj = binding.txtCnpj.text.toString().trim()
            if (especializacao.isEmpty()){
                binding.tilSpecialization.error = "Especialização é obrigatória para autônomo"
                return
            }
            // Cria o objeto Autonomo se o usuário escolheu ser um
            autonomo = Autonomo(cnpj = cnpj.ifEmpty { null }, especializacao = especializacao)
        }

        if (!politicasAceitas) {
            Toast.makeText(this, "Você deve aceitar as políticas de privacidade.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Todo usuário, por padrão, pode contratar. Cria o perfil de contratante.
                    val contratante = Contratante() // O CPF pode ser adicionado depois, no perfil.
                    salvarDadosUsuario(nome, email, telefone, contratante, autonomo)
                } else {
                    showLoading(false)
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso por outra conta."
                        is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. A senha deve ter no mínimo 6 caracteres."
                        else -> "Falha no cadastro: ${exception?.message}"
                    }
                    if (exception is FirebaseAuthUserCollisionException) {
                        binding.tilEmail.error = errorMessage
                    } else if (exception is FirebaseAuthWeakPasswordException) {
                        binding.tilSenha.error = errorMessage
                    } else {
                        Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun salvarDadosUsuario(nome: String, email: String, telefone: String, contratante: Contratante, autonomo: Autonomo?) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            Toast.makeText(baseContext, "Erro ao obter ID do usuário.", Toast.LENGTH_SHORT).show()
            return
        }

        // Monta o objeto Usuario com a nova arquitetura de composição
        val novoUsuario = Usuario(
            uid = userId,
            nome = nome,
            email = email,
            numeroTelefone = telefone,
            contratante = contratante, // Aninha o objeto Contratante
            autonomo = autonomo      // Aninha o objeto Autonomo (pode ser nulo)
        )

        db.collection("users").document(userId)
            .set(novoUsuario)
            .addOnSuccessListener { 
                Log.d("Firestore", "Usuário salvo com a nova estrutura. ID: $userId")
                val intent = Intent(this, TelaMenuPrincipal::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() 
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.w("Firestore", "Erro ao salvar usuário", e)
                auth.currentUser?.delete()
                Toast.makeText(baseContext, "Falha ao salvar dados do perfil.", Toast.LENGTH_SHORT).show()
            }
    }
}