package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaMudancaSucessoBinding

class TelaMudancaSucesso : AppCompatActivity() {

    private lateinit var binding: ActivityTelaMudancaSucessoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla o layout usando o View Binding
        binding = ActivityTelaMudancaSucessoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajusta o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handler para esperar 3 segundos antes de voltar para a tela de login
        Handler(Looper.getMainLooper()).postDelayed({
            // Cria a intenção para navegar para a TelaLogin
            val intent = Intent(this, TelaLogin::class.java).apply {
                // Limpa as telas anteriores da pilha de navegação
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish() // Finaliza a tela de sucesso para que o usuário não possa voltar para ela
        }, 3000) // 3000 milissegundos = 3 segundos
    }
}
