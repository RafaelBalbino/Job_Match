package com.jobmatch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
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
import com.jobmatch.databinding.ActivityTelaCadastroBinding
import kotlin.math.min

class TelaCadastro : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //                                         ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    // PASSO FINAL: SUBSTITUA ESTA LINHA PELA SUA URL DO FIREBASE STORAGE
    private val DEFAULT_PROFILE_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/jobmatch-3faec.firebasestorage.app/o/avatar-do-usuario.png?alt=media&token=d1d15194-bf59-4a2b-9df3-75c0a23053d1"
    //                                         ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

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
        setupPrivacyPolicyClick()
        setupPhoneMask()

        binding.btnEnviaCadastro.setOnClickListener {
            cadastrarUsuario()
        }
    }

    private fun setupPhoneMask() {
        binding.txtTelefone.setText("+55")
        binding.txtTelefone.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private var old = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Não é necessário
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Não é necessário
            }

            override fun afterTextChanged(s: Editable) {
                val str = s.toString().replace(Regex("\\D"), "")
                if (isUpdating || str == old) {
                    return
                }

                isUpdating = true
                var formatted = "+55"

                if (str.length > 2) {
                    formatted += " (${str.substring(2, min(4, str.length))}"
                }
                if (str.length >= 5) {
                    formatted += ") ${str.substring(4, min(9, str.length))}"
                }
                if (str.length >= 10) {
                    formatted += "-${str.substring(9, min(13, str.length))}"
                }

                s.replace(0, s.length, formatted)

                old = str
                isUpdating = false
            }
        })
    }


    private fun setupPrivacyPolicyClick() {
        val radioButton = binding.radioButton
        val fullText = radioButton.text.toString()
        val clickableText = "Políticas de Privacidade"
        val spannableString = SpannableString(fullText)
        val verdeAgua = ContextCompat.getColor(this, R.color.verde_agua)

        val start = fullText.indexOf(clickableText)
        if (start == -1) return

        val end = start + clickableText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val privacyPolicyText = "Nossa política de privacidade segue as diretrizes da LGPD, garantindo a proteção e o uso consciente dos seus dados. Ao se cadastrar, você concorda com a coleta e o tratamento de suas informações para os fins descritos em nossos termos."
                Toast.makeText(this@TelaCadastro, privacyPolicyText, Toast.LENGTH_LONG).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = verdeAgua
                ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        radioButton.text = spannableString
        radioButton.movementMethod = LinkMovementMethod.getInstance()
        radioButton.highlightColor = Color.TRANSPARENT
    }

    private fun setupUserTypeSelection() {
        binding.rgUserType.setOnCheckedChangeListener { _, checkedId ->
            val isFreelancer = checkedId == R.id.rb_freelancer
            toggleFreelancerFields(isFreelancer)
        }
    }

    private fun toggleFreelancerFields(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.tilCnpj.visibility = visibility
        binding.tilSpecialization.visibility = visibility
    }

    private fun setupClickableText() {
        val textView = binding.textView21
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)
        val verdeAgua = ContextCompat.getColor(this, R.color.verde_agua)

        val start = fullText.indexOf("Inicie")
        if (start == -1) return
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
        // Validação dos campos...
        binding.tilNome.error = null
        binding.tilEmail.error = null
        binding.tilTelefone.error = null
        binding.tilSenha.error = null
        binding.tilConfirmarSenha.error = null
        binding.tilSpecialization.error = null

        val nome = binding.txtNome.text.toString().trim()
        val email = binding.txtEmail.text.toString().trim()
        val telefone = binding.txtTelefone.text.toString().replace(Regex("[^\\d]"), "")
        val senha = binding.txtSenha.text.toString()
        val confirmarSenha = binding.txtConfirmarSenha.text.toString()
        val politicasAceitas = binding.radioButton.isChecked
        val isFreelancer = binding.rbFreelancer.isChecked

        if (nome.isEmpty() || email.isEmpty() || telefone.length < 13 || senha.isEmpty() || confirmarSenha.isEmpty()) {
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
                    val contratante = Contratante()
                    salvarDadosUsuario(nome, email, telefone, contratante, autonomo)
                } else {
                    showLoading(false)
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso por outra conta."
                        is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. A senha deve ter no mínimo 8 caracteres."
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

        // Monta o objeto Usuario, incluindo a URL da imagem padrão
        val novoUsuario = Usuario(
            uid = userId,
            nome = nome,
            email = email,
            numeroTelefone = telefone,
            fotoUrl = DEFAULT_PROFILE_IMAGE_URL, // <-- USANDO A URL DO FIREBASE STORAGE
            contratante = contratante,
            autonomo = autonomo
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
