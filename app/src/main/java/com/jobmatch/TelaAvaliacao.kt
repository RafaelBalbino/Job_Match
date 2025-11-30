package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaAvaliacaoBinding
import java.util.UUID

class TelaAvaliacao : AppCompatActivity() {

    private lateinit var binding: ActivityTelaAvaliacaoBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var pedidoId: String? = null
    private var autonomoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaAvaliacaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        pedidoId = intent.getStringExtra("PEDIDO_ID")
        autonomoId = intent.getStringExtra("AUTONOMO_ID")

        if (autonomoId == null || pedidoId == null) {
            Toast.makeText(this, "Erro: IDs não encontrados.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        carregarDadosAutonomo()

        binding.btnEnviarAvaliacao.setOnClickListener {
            iniciarProcessoDeAvaliacao()
        }
    }

    private fun carregarDadosAutonomo() {
        db.collection("users").document(autonomoId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    binding.tvNomeAutonomoAvaliado.text = usuario?.nome
                    binding.imgAutonomoAvaliado.load(usuario?.fotoUrl) {
                        placeholder(R.drawable.ic_profile_placeholder)
                        error(R.drawable.ic_profile_placeholder)
                    }
                } else {
                    Toast.makeText(this, "Autônomo não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Falha ao carregar dados do autônomo.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun iniciarProcessoDeAvaliacao() {
        val nota = binding.ratingBarAvaliacao.rating
        val contratanteId = auth.currentUser?.uid

        if (nota == 0f) {
            Toast.makeText(this, "Por favor, selecione uma nota.", Toast.LENGTH_SHORT).show()
            return
        }

        if (contratanteId == null) {
            Toast.makeText(this, "Erro: Contratante não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // 1. Buscar os dados do contratante (nome e foto)
        db.collection("users").document(contratanteId).get()
            .addOnSuccessListener { document ->
                val contratante = document.toObject(Usuario::class.java)
                if (contratante == null) {
                    Toast.makeText(this, "Falha ao obter dados do contratante.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@addOnSuccessListener
                }
                // 2. Com os dados do contratante, criar e salvar a avaliação
                criarESalvarAvaliacao(contratante, nota.toDouble())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao buscar seu perfil: ${e.message}", Toast.LENGTH_LONG).show()
                showLoading(false)
            }
    }

    private fun criarESalvarAvaliacao(contratante: Usuario, nota: Double) {
        val comentario = binding.etComentario.text.toString().trim()
        val avaliacaoId = UUID.randomUUID().toString() // Gera um ID único

        val novaAvaliacao = Avaliacao(
            id = avaliacaoId,
            pedidoId = pedidoId!!,
            autonomoId = autonomoId!!,
            contratanteId = contratante.uid ?: "",
            contratanteNome = contratante.nome ?: "", // Salva o nome para eficiência
            contratanteFotoUrl = contratante.fotoUrl, // Salva a foto para eficiência
            nota = nota,
            comentario = comentario
        )

        // 3. Usa .document(id).set(objeto) para salvar com o ID controlado
        db.collection("avaliacoes").document(avaliacaoId).set(novaAvaliacao)
            .addOnSuccessListener {
                // 4. Atualiza a média de avaliações do autônomo
                atualizarPerfilAutonomo(nota)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao enviar avaliação: ${e.message}", Toast.LENGTH_LONG).show()
                showLoading(false)
            }
    }

    private fun atualizarPerfilAutonomo(novaNota: Double) {
        val autonomoRef = db.collection("users").document(autonomoId!!)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(autonomoRef)
            val usuario = snapshot.toObject(Usuario::class.java)
            val autonomo = usuario?.autonomo ?: return@runTransaction

            val totalAvaliacoesAntigo = autonomo.totalAvaliacoes
            val mediaAvaliacoesAntiga = autonomo.mediaAvaliacoes

            val novoTotalAvaliacoes = totalAvaliacoesAntigo + 1
            val novaMedia = ((mediaAvaliacoesAntiga * totalAvaliacoesAntigo) + novaNota) / novoTotalAvaliacoes

            transaction.update(autonomoRef, mapOf(
                "autonomo.mediaAvaliacoes" to novaMedia,
                "autonomo.totalAvaliacoes" to novoTotalAvaliacoes
            ))

            null
        }.addOnSuccessListener {
            Toast.makeText(this, "Avaliação enviada com sucesso!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, TelaMenuPrincipal::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Falha ao atualizar o perfil do autônomo: ${e.message}", Toast.LENGTH_LONG).show()
            showLoading(false)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnEnviarAvaliacao.isEnabled = !isLoading
        binding.progressBarAvaliacao.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}