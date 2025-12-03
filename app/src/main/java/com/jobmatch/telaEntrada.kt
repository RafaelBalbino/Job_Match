package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth

class telaEntrada : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_entrada)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa o SDK de anúncios do Google
        MobileAds.initialize(this) {}

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            verificarUsuarioLogado()
        }, 2000) // 2 segundos
    }

    private fun verificarUsuarioLogado() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuário está logado, vai para a tela principal
            val intent = Intent(this, TelaMenuPrincipal::class.java)
            startActivity(intent)
        } else {
            // Usuário não está logado, vai para a tela de login
            val intent = Intent(this, TelaLogin::class.java)
            startActivity(intent)
        }
        finish()
    }
}
