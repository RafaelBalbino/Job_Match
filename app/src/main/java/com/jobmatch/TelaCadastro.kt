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
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaCadastroBinding // Importe a classe de binding gerada

class TelaCadastro : AppCompatActivity() {

    // Declare a variável para o view binding
    private lateinit var binding: ActivityTelaCadastroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla o layout usando o view binding e define como o conteúdo da activity
        binding = ActivityTelaCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A lógica do WindowInsets agora usa a view raiz do binding
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- LÓGICA ORIGINAL PARA O TEXTO CLICÁVEL (ADAPTADA PARA VIEW BINDING) ---
        // Nenhuma mudança na lógica, apenas na forma de acessar o TextView.
        val textView = binding.textView21
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)
        val verdeAgua = ContextCompat.getColor(this, R.color.verde_agua)

        val start = fullText.indexOf("Inicie")
        val end = fullText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Navega para TelaLogin
                val intent = Intent(this@TelaCadastro, TelaLogin::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = verdeAgua
                ds.typeface = Typeface.create(androidx.compose.ui.text.font.Typeface.DEFAULT,
                    androidx.compose.ui.text.font.Typeface.BOLD)
            }
        }

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
        // --- FIM DA LÓGICA ORIGINAL ---


        // --- NOVA LÓGICA PARA O BOTÃO DE CADASTRO ---
        binding.btnEnviaCadastro.setOnClickListener {
            val nome = binding.txtNome.text.toString().trim()
            val email = binding.txtEmail.text.toString().trim()
            val telefone = binding.txtTelefone.text.toString().trim()
            val senha = binding.txtSenha.text.toString()
            val confirmarSenha = binding.txtConfirmarSenha.text.toString()
            val politicasAceitas = binding.radioButton.isChecked

            var hasError = false

            // 1. Checar se cada campo de texto foi preenchido
            if (nome.isEmpty()) {
                binding.txtNome.error = "Campo obrigatório"
                hasError = true
            }
            if (email.isEmpty()) {
                binding.txtEmail.error = "Campo obrigatório"
                hasError = true
            }
            if (telefone.isEmpty()) {
                binding.txtTelefone.error = "Campo obrigatório"
                hasError = true
            }
            if (senha.isEmpty()) {
                binding.txtSenha.error = "Campo obrigatório"
                hasError = true
            }
            if (confirmarSenha.isEmpty()) {
                binding.txtConfirmarSenha.error = "Campo obrigatório"
                hasError = true
            }

            // Se já houver erro de campo vazio, não continuar com outras validações
            if (hasError) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Checar se as senhas são iguais
            if (senha != confirmarSenha) {
                binding.txtConfirmarSenha.error = "As senhas não coincidem"
                Toast.makeText(this, "As senhas não coincidem!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Checar se o RadioButton foi marcado
            if (!politicasAceitas) {
                Toast.makeText(this, "Você deve aceitar as políticas de privacidade.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Se todas as validações passaram:
            // - Guardar as informações
            // - Mudar de tela

            val intent = Intent(this, TelaMenuPrincipal::class.java)
            val bundle = Bundle().apply {
                putString("USER_NOME", nome)
                putString("USER_EMAIL", email)
                putString("USER_TELEFONE", telefone)
                // Nota: Não é uma boa prática passar senhas entre activities, mas seguindo o requisito.
            }
            intent.putExtras(bundle)

            // Limpa as activities anteriores da pilha e inicia a nova
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish() // Finaliza a TelaCadastro para que o usuário não volte para ela ao pressionar "voltar"
        }
    }
}
