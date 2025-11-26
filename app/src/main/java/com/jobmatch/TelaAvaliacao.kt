package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaAvaliacaoBinding

data class Avaliacao(
    val pedidoId: String = "",
    val servicoId: String = "", // Você pode adicionar isso no futuro se necessário
    val autonomoId: String = "",
    val contratanteId: String = "",
    val nota: Float = 0.0f,
    val comentario: String = "",
    val data: FieldValue? = null
)

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

        // Recebe os IDs da tela anterior
        pedidoId = intent.getStringExtra("PEDIDO_ID")
        autonomoId = intent.getStringExtra("AUTONOMO_ID")

        if (pedidoId == null || autonomoId == null) {
            Toast.makeText(this, "Erro: Informações do pedido não encontradas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.btnEnviarAvaliacao.setOnClickListener {
            enviarAvaliacao()
        }
    }

    private fun enviarAvaliacao() {
        val contratanteId = auth.currentUser?.uid
        if (contratanteId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val nota = binding.ratingBarAvaliacao.rating
        val comentario = binding.etComentario.text.toString().trim()

        if (nota == 0.0f) {
            Toast.makeText(this, "Por favor, selecione uma nota.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val novaAvaliacao = Avaliacao(
            pedidoId = pedidoId!!,
            autonomoId = autonomoId!!,
            contratanteId = contratanteId,
            nota = nota,
            comentario = comentario,
            data = FieldValue.serverTimestamp()
        )

        db.collection("avaliacoes").add(novaAvaliacao)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Avaliação enviada com sucesso!", Toast.LENGTH_SHORT).show()
                // Volta para o menu principal
                val intent = Intent(this, TelaMenuPrincipal::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erro ao enviar avaliação: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarAvaliacao.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnEnviarAvaliacao.isEnabled = !isLoading
    }
}