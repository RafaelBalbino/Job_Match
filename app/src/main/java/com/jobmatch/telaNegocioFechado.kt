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

        // Handler para fechar a tela e navegar para a lista de projetos do autônomo
        Handler(Looper.getMainLooper()).postDelayed({
            // CORREÇÃO: A tela de destino é a lista de projetos, que é um fragmento.
            val intent = Intent(this, FragmentContainerActivity::class.java).apply {
                putExtra("FRAGMENT_NAME", "fragmentListaPedidosAutonomo")
                putExtra("TIPO_QUERY", "PROJETOS")
                // CORREÇÃO: A flag que destruía a pilha foi removida.
            }
            startActivity(intent)
            finish() // Fecha a activity atual, retornando para a pilha anterior que agora está preservada
        }, 3000) // 3000 milissegundos = 3 segundos
    }
}