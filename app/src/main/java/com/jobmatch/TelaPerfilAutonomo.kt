package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaPerfilAutonomoBinding
import com.jobmatch.databinding.ItemServicoButtonBinding
import androidx.recyclerview.widget.GridLayoutManager

class TelaPerfilAutonomo : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaPerfilAutonomoBinding.inflate(layoutInflater)
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var servicosAdapter: ServicosAdapter
    private val listaServicos = mutableListOf<Servico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()

        // Lógica para determinar qual perfil carregar
        val autonomoId = intent.getStringExtra("AUTONOMO_ID")
        if (!autonomoId.isNullOrBlank()) {
            // Modo visitante: Carrega perfil do ID recebido
            carregarDadosAutonomo(autonomoId)
            configurarModoVisitante()
        } else {
            // Modo proprietário: Carrega perfil do usuário logado
            val userId = auth.currentUser?.uid
            if (userId != null) {
                carregarDadosAutonomo(userId)
                configurarModoProprietario()
            } else {
                Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun carregarDadosAutonomo(id: String) {
        // Carrega os dados do usuário (nome, foto, etc.)
        db.collection("users").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        binding.textView.text = it.nome // Ex: Nome do autônomo
                        // binding.ivProfile.load(it.fotoUrl) // Ex: Foto do autônomo
                    }
                }
            }

        // Carrega os serviços daquele autônomo
        db.collection("servicos").whereEqualTo("uidUsuario", id).get()
            .addOnSuccessListener { documents ->
                val servicos = documents.toObjects(Servico::class.java)
                servicosAdapter.updateData(servicos)
            }
    }

    private fun configurarModoVisitante() {
        binding.btnEditarPerfil.visibility = View.GONE
        binding.btnCadastrarServico.visibility = View.GONE
        // Adicionar botão de "Contratar" ou similar, se necessário
    }

    private fun configurarModoProprietario() {
        binding.btnEditarPerfil.visibility = View.VISIBLE
        binding.btnCadastrarServico.visibility = View.VISIBLE

        binding.btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, TelaEdicaoPerfilAutonomo::class.java))
        }

        binding.btnCadastrarServico.setOnClickListener {
            cadastrarServicoLauncher.launch(Intent(this, CadastrarServico::class.java))
        }
    }

    private val cadastrarServicoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Simplesmente recarrega os dados do usuário logado para mostrar o novo serviço
            auth.currentUser?.uid?.let { carregarDadosAutonomo(it) }
        }
    }

    private fun setupRecyclerView() {
        servicosAdapter = ServicosAdapter(listaServicos) { servicoClicado ->
            Toast.makeText(this, "Detalhes de: ${servicoClicado.nomeServico}", Toast.LENGTH_SHORT).show()
        }
        binding.containerServicos.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = servicosAdapter
            isNestedScrollingEnabled = false
        }
    }
}
