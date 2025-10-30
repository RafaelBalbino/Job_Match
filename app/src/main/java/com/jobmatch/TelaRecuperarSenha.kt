package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.jobmatch.databinding.ActivityTelaRecuperarSenhaBinding
import java.util.concurrent.TimeUnit

class TelaRecuperarSenha : AppCompatActivity() {

    // Declara o objeto de binding para acessar as views do XML
    private lateinit var binding: ActivityTelaRecuperarSenhaBinding
    // Declara a variável do Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla o layout usando o View Binding
        binding = ActivityTelaRecuperarSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configura o padding para não sobrepor as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configura o listener de clique para o botão de enviar
        binding.button.setOnClickListener {
            iniciarRecuperacao()
        }

        // Configura o botão de voltar
        binding.btnVoltarInicio.setOnClickListener {
            finish() // Fecha a tela atual e volta para a anterior na pilha de atividades.
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.button.text = ""
            binding.recoverProgressBar.visibility = View.VISIBLE
            binding.button.isEnabled = false
        } else {
            binding.button.text = "Enviar"
            binding.recoverProgressBar.visibility = View.GONE
            binding.button.isEnabled = true
        }
    }

    private fun iniciarRecuperacao() {
        binding.tilEndereco.error = null
        val endereco = binding.txtEndereco.text.toString().trim()

        if (endereco.isEmpty()) {
            binding.tilEndereco.error = "Campo obrigatório."
            return
        }

        showLoading(true)

        // Verifica se é um e-mail ou um número de telefone
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(endereco).matches()) {
            enviarEmailRecuperacao(endereco)
        } else {
            val digitosApenas = endereco.filter { it.isDigit() }
            val numeroCompleto = "+55$digitosApenas"
            enviarSmsRecuperacao(numeroCompleto)
        }
    }

    private fun enviarEmailRecuperacao(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "E-mail de recuperação enviado.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, TelaMudancaSucesso::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "Nenhum usuário encontrado com este e-mail."
                        else -> "Falha ao enviar e-mail. Verifique a conexão."
                    }
                    binding.tilEndereco.error = errorMessage
                }
            }
    }

    private fun enviarSmsRecuperacao(numeroTelefone: String) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                showLoading(false)
                Log.d("Firebase", "onVerificationCompleted:$credential")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                showLoading(false)
                Log.w("Firebase", "onVerificationFailed", e)
                binding.tilEndereco.error = "Falha ao verificar telefone: ${e.localizedMessage}"
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                showLoading(false)
                Log.d("Firebase", "onCodeSent:$verificationId")
                Toast.makeText(baseContext, "Código SMS enviado.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@TelaRecuperarSenha, TelaCodigoSenha::class.java).apply {
                    putExtra("VERIFICATION_ID", verificationId)
                }
                startActivity(intent)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(numeroTelefone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
