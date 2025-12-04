package com.jobmatch

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaPagamentosBinding

class TelaPagamentos : AppCompatActivity() {

    private lateinit var binding: ActivityTelaPagamentosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Infla o layout usando ViewBinding
        binding = ActivityTelaPagamentosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajusta o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configura o clique do botão de voltar na toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish() // Fecha a atividade e volta para a tela anterior
        }
    }
}