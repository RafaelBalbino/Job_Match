package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class telaNegocioFechado : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_negocio_fechado)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handler para fechar a tela e navegar para a lista de pedidos aceitos
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, FragmentContainerActivity::class.java).apply {
                putExtra("FRAGMENT_TYPE", "AUTONOMO_ACEITOS")
                // Limpa a pilha de telas para que o usuário não volte para a tela de "Negócio Fechado"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish() // Fecha a activity atual
        }, 3000) // 3000 milissegundos = 3 segundos
    }
}