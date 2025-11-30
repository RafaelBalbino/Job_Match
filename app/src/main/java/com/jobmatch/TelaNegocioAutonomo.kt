package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaNegocioAutonomoBinding
import java.util.Locale

class TelaNegocioAutonomo : AppCompatActivity() {

    // ViewBinding para acesso seguro aos componentes de UI
    private lateinit var binding: ActivityTelaNegocioAutonomoBinding
    // Instância do Firestore
    private lateinit var db: FirebaseFirestore
    // ID do autônomo cujo perfil está sendo exibido
    private var autonomoId: String? = null
    // Adapter para a lista de serviços
    private lateinit var servicoAdapter: ServicoAdapter
    // Lista para armazenar os serviços do autônomo
    private val servicosList = mutableListOf<Servico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaNegocioAutonomoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()

        // 1. Pega o ID do autônomo passado pela tela anterior (ex: ServicoAdapter)
        autonomoId = intent.getStringExtra("AUTONOMO_ID")

        // Validação para garantir que o ID foi recebido
        if (autonomoId == null) {
            Toast.makeText(this, "Erro: ID do autônomo não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Configura os componentes da tela
        setupRecyclerView()
        carregarDadosAutonomo()
        configurarBotoes()
        carregarServicos()
    }

    /**
     * Configura o RecyclerView para a lista de serviços.
     * Define o layout como horizontal.
     */
    private fun setupRecyclerView() {
        servicoAdapter = ServicoAdapter(servicosList)
        binding.rvServicosOferecidos.apply {
            layoutManager = LinearLayoutManager(this@TelaNegocioAutonomo, LinearLayoutManager.HORIZONTAL, false)
            adapter = servicoAdapter
        }
    }

    /**
     * Busca os dados principais do perfil do usuário autônomo no Firestore.
     */
    private fun carregarDadosAutonomo() {
        db.collection("users").document(autonomoId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    if (usuario != null) {
                        // 2. Preenche a UI com os dados encontrados
                        preencherDados(usuario)
                    } else {
                        Toast.makeText(this, "Falha ao processar dados do perfil.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Perfil não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao carregar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Preenche os componentes da UI com os dados do autônomo.
     * Inclui a lógica para exibir ou esconder a seção de avaliações.
     */
    private fun preencherDados(usuario: Usuario) {
        // Preenche nome, endereço e foto
        binding.tvNomeAutonomo.text = usuario.nome
        binding.tvEndereco.text = "${usuario.cidade} - ${usuario.estado}"
        binding.ivAutonomoAvatar.load(usuario.fotoUrl) {
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
        }

        // 3. Lógica para a seção de avaliações
        val perfilAutonomo = usuario.autonomo
        if (perfilAutonomo != null && perfilAutonomo.totalAvaliacoes > 0) {
            // Se houver avaliações, mostra e popula os componentes
            binding.rbMediaAvaliacoes.visibility = View.VISIBLE
            binding.tvNumeroAvaliacoes.visibility = View.VISIBLE
            binding.btnVerAvaliacoes.visibility = View.VISIBLE

            binding.rbMediaAvaliacoes.rating = perfilAutonomo.mediaAvaliacoes.toFloat()
            binding.tvNumeroAvaliacoes.text = String.format(Locale.getDefault(), "(%d)", perfilAutonomo.totalAvaliacoes)
        } else {
            // Caso contrário, esconde a seção de avaliações
            binding.rbMediaAvaliacoes.visibility = View.GONE
            binding.tvNumeroAvaliacoes.visibility = View.GONE
            binding.btnVerAvaliacoes.visibility = View.GONE
        }

        // Popula as categorias (especializações) como Chips
        perfilAutonomo?.especializacao?.let { especializacoes ->
            binding.chipGroupCategorias.removeAllViews()
            val categorias = especializacoes.split(",").map { it.trim() }
            for (categoria in categorias) {
                if (categoria.isNotEmpty()){
                    val chip = Chip(this)
                    chip.text = categoria
                    binding.chipGroupCategorias.addView(chip)
                }
            }
        }
    }

    /**
     * Busca na coleção 'servico' todos os serviços oferecidos pelo autônomo.
     */
    private fun carregarServicos() {
        db.collection("servico")
            .whereEqualTo("autonomoId", autonomoId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val novosServicos = documents.toObjects(Servico::class.java)
                    servicosList.clear()
                    servicosList.addAll(novosServicos)
                    servicoAdapter.notifyDataSetChanged()
                    // Garante que a seção de serviços só aparece se houver serviços
                    binding.tvServicosLabel.visibility = View.VISIBLE
                    binding.rvServicosOferecidos.visibility = View.VISIBLE
                } else {
                    // Esconde a seção de serviços se o autônomo não tiver nenhum
                    binding.tvServicosLabel.visibility = View.GONE
                    binding.rvServicosOferecidos.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Falha ao carregar serviços.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Configura os listeners de clique para os botões da tela.
     */
    private fun configurarBotoes() {
        binding.btnVoltarPesquisa.setOnClickListener {
            finish()
        }

        // Botão para ver a lista de avaliações
        binding.btnVerAvaliacoes.setOnClickListener {
            val intent = Intent(this, FragmentContainerActivity::class.java).apply {
                putExtra("FRAGMENT_NAME", "fragmentListaAvaliacoes")
                putExtra("autonomo_id", autonomoId) // Passa o ID para o fragmento de lista
            }
            startActivity(intent)
        }

        // Botão para iniciar a criação de um pedido para este autônomo
        binding.btnFazerPedido.setOnClickListener {
            val intent = Intent(this, TelaCriacaoPedido::class.java).apply {
                putExtra("AUTONOMO_ID", autonomoId) // Passa o ID para a tela de pedido
                putExtra("MODE", "CREATE")
            }
            startActivity(intent)
        }
    }
}