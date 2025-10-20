package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaCodigoSenhaBinding

class TelaCodigoSenha : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCodigoSenhaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCodigoSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Recupera o endereço (email/celular) da Intent e o exibe.
        val enderecoRecuperacao = intent.getStringExtra("ENDERECO_RECUPERACAO")
        binding.lblEndereco.text = enderecoRecuperacao ?: "Endereço não informado"

        setupCodigoInput()

        binding.btnVerificarCodigo.setOnClickListener {
            verificarCodigo()
        }
    }

    // Configura os listeners para os campos de entrada do código.
    private fun setupCodigoInput() {
        val et1 = binding.etCodigo1
        val et2 = binding.etCodigo2
        val et3 = binding.etCodigo3
        val et4 = binding.etCodigo4

        // Adiciona TextWatcher para mover o foco automaticamente.
        et1.addTextChangedListener(GenericTextWatcher(et1, et2))
        et2.addTextChangedListener(GenericTextWatcher(et2, et3))
        et3.addTextChangedListener(GenericTextWatcher(et3, et4))
        et4.addTextChangedListener(GenericTextWatcher(et4, null))
    }

    private fun verificarCodigo() {
        val codigo1 = binding.etCodigo1.text.toString()
        val codigo2 = binding.etCodigo2.text.toString()
        val codigo3 = binding.etCodigo3.text.toString()
        val codigo4 = binding.etCodigo4.text.toString()

        if (codigo1.isEmpty() || codigo2.isEmpty() || codigo3.isEmpty() || codigo4.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos do código.", Toast.LENGTH_SHORT).show()
            return
        }

        val codigoCompleto = "$codigo1$codigo2$codigo3$codigo4"
        val codigoDoServidor = "1234" // Código de teste.

        // Lógica de verificação do código.
        if (codigoCompleto == codigoDoServidor) {
            Toast.makeText(this, "Código verificado com sucesso!", Toast.LENGTH_SHORT).show()

            // ALTERAÇÃO AQUI: Inicia a activity 'TelaMudanca' após a verificação.
            val intent = Intent(this, TelaMudanca::class.java)
            startActivity(intent)
            finish() // Finaliza a tela de código para que o usuário não possa voltar.
        } else {
            Toast.makeText(this, "Código inválido. Tente novamente.", Toast.LENGTH_LONG).show()
        }
    }

    // Classe interna para gerenciar a mudança de foco entre os EditTexts.
    inner class GenericTextWatcher(private val currentView: EditText, private val nextView: EditText?) : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            if (editable.toString().length == 1) {
                nextView?.requestFocus()
            }
        }
        override fun beforeTextChanged(arg0: CharSequence?, arg1: Int, arg2: Int, arg3: Int) {}
        override fun onTextChanged(arg0: CharSequence?, arg1: Int, arg2: Int, arg3: Int) {}
    }
}
