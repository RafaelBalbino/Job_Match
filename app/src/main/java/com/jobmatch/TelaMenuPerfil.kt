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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaMenuPerfilBinding

class TelaMenuPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityTelaMenuPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaMenuPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Funções principais chamadas ao criar a tela
        carregarDadosUsuario()
        configurarBotaoLogout()
        configurarBotaoFechar()
    }

    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Se não houver usuário, encerra a sessão e volta para o login
            Log.e("TelaMenuPerfil", "Usuário não autenticado.")
            fazerLogout()
            return
        }

        // Mostra um shimmer/loading aqui se desejar

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    if (usuario != null) {
                        // Preenche os campos da UI com os dados do usuário
                        binding.textView23.text = usuario.nome
                        binding.textView24.text = usuario.email

                        // **LÓGICA CONDICIONAL DO MENU**
                        // Mostra ou esconde opções com base no perfil do usuário
                        if (usuario.autonomo != null) {
                            binding.textView16.visibility = View.VISIBLE // "Projetos"
                        } else {
                            binding.textView16.visibility = View.GONE
                        }

                    } else {
                        Log.e("TelaMenuPerfil", "Falha ao converter o documento para objeto Usuario.")
                    }
                } else {
                    Log.e("TelaMenuPerfil", "Nenhum documento encontrado para o usuário: $userId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TelaMenuPerfil", "Erro ao buscar dados do usuário", exception)
                Toast.makeText(this, "Erro ao carregar dados do perfil.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarBotaoLogout() {
        binding.textView21.setOnClickListener {
            fazerLogout()
        }
    }

    private fun configurarBotaoFechar() {
        binding.imageView4.setOnClickListener {
            finish() // Simplesmente fecha a tela atual, voltando para a anterior (TelaMenuPrincipal)
        }
    }

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, TelaLogin::class.java)
        // Limpa todas as activities anteriores e inicia a TelaLogin como a nova tarefa principal
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
