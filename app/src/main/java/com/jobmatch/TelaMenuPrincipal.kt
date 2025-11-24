package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
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

        // Define os placeholders imediatamente
        binding.imgPerfil.setImageResource(R.drawable.circle_white)
        binding.imgViewAutonomo1.setImageResource(R.drawable.rounded_edittext_background)
        binding.imgViewAutonomo2.setImageResource(R.drawable.rounded_edittext_background)
        binding.imgViewAutonomo3.setImageResource(R.drawable.rounded_edittext_background)
        binding.imgViewAutonomo4.setImageResource(R.drawable.rounded_edittext_background)

        // Ajusta o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarListeners()
        carregarDadosDoCabecalho()
        carregarESetarServicos() // Carrega os serviços do Firestore
    }

    private fun configurarListeners() {
        binding.imgPerfil.setOnClickListener {
            navegarParaEdicaoDePerfil()
        }

        binding.imgNavegacaoMenu.setOnClickListener {
            navegarParaMenuPerfil()
        }
    }

    private fun carregarESetarServicos() {
        val imageViews: List<ImageView> = listOf(
            binding.imgViewAutonomo1,
            binding.imgViewAutonomo2,
            binding.imgViewAutonomo3,
            binding.imgViewAutonomo4
        )

        db.collection("servicos").limit(4).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "Nenhum serviço encontrado.")
                    return@addOnSuccessListener
                }

                for ((index, document) in documents.withIndex()) {
                    if (index >= imageViews.size) break

                    val servico = document.toObject(Servico::class.java)
                    val imageView = imageViews[index]

                    // Carrega a imagem usando Coil com a URL correta
                    if (!servico.fotoServico.isNullOrEmpty()) {
                        imageView.load(servico.fotoServico) {
                            crossfade(true)
                            placeholder(R.drawable.rounded_edittext_background)
                            error(R.drawable.rounded_edittext_background) // Imagem de fallback
                        }
                    } else {
                         imageView.setImageResource(R.drawable.rounded_edittext_background)
                    }

                    // Configura o clique para cada card usando os dados corretos
                    imageView.setOnClickListener {
                        abrirDetalhesDoServico(servico)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Erro ao buscar serviços: ", exception)
                Toast.makeText(this, "Erro ao carregar serviços.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun abrirDetalhesDoServico(servico: Servico) {
        val intent = Intent(this, telaServicoAmpliado::class.java).apply {
            putExtra("SERVICO", servico)
        }
        startActivity(intent)
    }

    private fun navegarParaMenuPerfil() {
        val intent = Intent(this, TelaMenuPerfil::class.java)
        startActivity(intent)
    }

    private fun carregarDadosDoCabecalho() {
        if (userId == null) return

        db.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        val fotoUrl = it.fotoUrl
                        if (!fotoUrl.isNullOrEmpty()) {
                            binding.imgPerfil.load(fotoUrl) {
                                crossfade(true)
                                placeholder(R.drawable.circle_white)
                                error(R.drawable.circle_white) // Imagem de fallback
                            }
                        } else {
                            binding.imgPerfil.setImageResource(R.drawable.circle_white)
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