package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaMenuPrincipalBinding

class TelaMenuPrincipal : AppCompatActivity() {

    private lateinit var binding: ActivityTelaMenuPrincipalBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaMenuPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid

        // Ajusta o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarListeners()
        carregarDadosDoCabecalho()
    }

    private fun configurarListeners() {
        binding.imgPerfil.setOnClickListener {
            navegarParaEdicaoDePerfil()
        }
    }

    private fun carregarDadosDoCabecalho() {
        if (userId == null) return

        db.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        // Determina qual URL de foto usar
                        val fotoUrl = if (it.autonomo != null) it.autonomo?.fotoUrl else it.contratante?.fotoUrl
                        if (!fotoUrl.isNullOrEmpty()) {
                            binding.imgPerfil.load(fotoUrl) {
                                crossfade(true)
                                error(R.drawable.circle_white) // Imagem de fallback
                            }
                        }
                    }
                }
            }
            // Falha ao carregar a foto não é um erro crítico, então não mostramos Toast
    }

    private fun navegarParaEdicaoDePerfil() {
        if (userId == null) {
            Toast.makeText(this, "Usuário não encontrado, faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Verifica se o campo 'autonomo' não é nulo
                    if (document.get("autonomo") != null) {
                        val intent = Intent(this, TelaEdicaoPerfilAutonomo::class.java)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, TelaEdicaoPerfilContratante::class.java)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Perfil de usuário não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}