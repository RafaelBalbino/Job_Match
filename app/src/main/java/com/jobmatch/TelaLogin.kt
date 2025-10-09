package com.jobmatch // Replace com.example.job_match with your actual package name

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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TelaLogin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_login)

        // --- Linkando pra TelaCadastro ---
        val textViewToCadastro: TextView = findViewById(R.id.textView13)
        val fullTextToCadastro = textViewToCadastro.text.toString() // "Não tem cadastro? Cadastre-se aqui"

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
                    ds.isUnderlineText = true // Set to true or false as you prefer
                    ds.color = linkColorCadastro
                    ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
            spannableStringToCadastro.setSpan(clickableSpanCadastro, startCadastro, endCadastro, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textViewToCadastro.text = spannableStringToCadastro
            textViewToCadastro.movementMethod = LinkMovementMethod.getInstance()
            textViewToCadastro.highlightColor = Color.TRANSPARENT
        } else {
            Log.e("TelaLogin", "Clickable text '$clickableTextCadastro' not found in textView13. Current text: '$fullTextToCadastro'")
        }

        // --- NOVO CÓDIGO: Link para TelaRecuperarSenha ---
        val textViewToRecuperarSenha: TextView = findViewById(R.id.lbl_esqueceu_senha)
        val fullTextRecuperarSenha = textViewToRecuperarSenha.text.toString() // "Esqueceu sua senha?"
        val spannableStringRecuperarSenha = SpannableString(fullTextRecuperarSenha)

        // Vamos usar a cor já definida no XML, mas podemos especificá-la aqui para o ClickableSpan
        val linkColorRecuperarSenha = ContextCompat.getColor(this, R.color.texto_secundario)

        // Queremos que o texto inteiro "Esqueceu sua senha?" seja clicável
        val clickableTextRecuperarSenha = fullTextRecuperarSenha // Ou "Esqueceu sua senha?"
        val startRecuperarSenha = 0 // Começo do texto
        val endRecuperarSenha = clickableTextRecuperarSenha.length // Fim do texto

        if (startRecuperarSenha != -1) { // Verificação ainda útil, embora deva sempre ser 0 aqui
            val clickableSpanRecuperarSenha = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@TelaLogin, TelaRecuperarSenha::class.java)
                    startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true // Mude para false se não quiser sublinhado
                    ds.color = linkColorRecuperarSenha // Mantém a cor do XML ou a que definirmos
                    // ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Descomente se quiser negrito
                }
            }

            spannableStringRecuperarSenha.setSpan(clickableSpanRecuperarSenha, startRecuperarSenha, endRecuperarSenha, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textViewToRecuperarSenha.text = spannableStringRecuperarSenha
            textViewToRecuperarSenha.movementMethod = LinkMovementMethod.getInstance()
            textViewToRecuperarSenha.highlightColor = Color.TRANSPARENT // Remove o destaque azul ao clicar
        } else {
            // Este Log.e provavelmente nunca será chamado se fullTextRecuperarSenha for usado
            Log.e("TelaLogin", "Texto clicável '$clickableTextRecuperarSenha' não encontrado em lbl_esqueceu_senha.")
        }
    }
}
