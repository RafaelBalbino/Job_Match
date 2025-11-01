package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.jobmatch.databinding.ActivityTelaCodigoSenhaBinding

class TelaCodigoSenha : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCodigoSenhaBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private val editTexts: List<EditText> by lazy {
        listOf(binding.etCodigo1, binding.etCodigo2, binding.etCodigo3, binding.etCodigo4, binding.etCodigo5, binding.etCodigo6)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaCodigoSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Recupera o ID de verificação passado pela TelaRecuperarSenha
        verificationId = intent.getStringExtra("VERIFICATION_ID")

        if (verificationId == null) {
            Toast.makeText(this, "Erro: ID de verificação não encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupEditTexts()

        binding.btnVerificarCodigo.setOnClickListener {
            val codigo = getCodigoFromEditTexts()

            if (codigo.length < 6) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = PhoneAuthProvider.getCredential(verificationId!!, codigo)
            signInWithPhoneAuthCredential(credential)
        }

        binding.btnVoltarRecuperacao.setOnClickListener {
            finish()
        }
    }

    private fun getCodigoFromEditTexts(): String {
        return editTexts.joinToString("") { it.text.toString() }
    }

    private fun setupEditTexts() {
        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < editTexts.size - 1) {
                        editTexts[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            editTexts[i].setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (i > 0 && editTexts[i].text.isEmpty()) {
                        editTexts[i - 1].requestFocus()
                    }
                }
                false
            })
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verificação bem-sucedida!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, TelaMudanca::class.java)
                    // Opcional: passar alguma informação, se a tela de mudança precisar
                    // intent.putExtra("USER_UID", task.result?.user?.uid)
                    startActivity(intent)
                    finishAffinity() // Limpa a pilha de recuperação de senha

                } else {
                    Toast.makeText(this, "Código de verificação inválido.", Toast.LENGTH_LONG).show()
                }
            }
    }
}
