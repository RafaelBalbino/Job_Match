package com.jobmatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaRealizarPedidoBinding

class TelaRealizarPedidos : AppCompatActivity() {

    private lateinit var binding: ActivityTelaRealizarPedidoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Infla o layout usando View Binding
        binding = ActivityTelaRealizarPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configura o listener de clique para o botão "Realizar Pedido"
        binding.btnRealizarPedido.setOnClickListener {
            val intent = Intent(this, TelaCriacaoPedido::class.java)
            startActivity(intent)
        }

        // Configura o listener de clique para o botão de voltar (ícone)
        binding.imageView3.setOnClickListener {
            finish() // Fecha a tela atual e volta para a anterior
        }
    }
}