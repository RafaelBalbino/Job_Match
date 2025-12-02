package com.jobmatch

import android.content.Intent
import android.net.Uri
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

    // ViewBinding para acesso seguro e nulo aos componentes da UI
    private lateinit var binding: ActivityTelaNegocioAutonomoBinding
    // Instância do banco de dados Firestore
    private lateinit var db: FirebaseFirestore
    // ID do autônomo cujo perfil está sendo exibido, recebido da tela anterior
    private var autonomoId: String? = null
    // Armazena o objeto do usuário carregado para evitar releituras
    private var usuarioAtual: Usuario? = null 
    // Adapter para a lista de serviços oferecidos
    private lateinit var servicoAdapter: ServicoAdapter
    // Lista mutável que armazena os serviços para o adapter
    private val servicosList = mutableListOf<Servico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla o layout usando ViewBinding
        binding = ActivityTelaNegocioAutonomoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()

        // 1. Pega o ID do autônomo passado pela tela anterior (ex: ServicoAdapter)
        autonomoId = intent.getStringExtra("AUTONOMO_ID")

        // 2. Validação de segurança: se o ID não for fornecido, fecha a tela
        if (autonomoId == null) {
            Toast.makeText(this, "Erro: ID do autônomo não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 3. Chama as funções para configurar a tela
        setupRecyclerView()
        carregarDadosAutonomo()
        configurarBotoes()
        carregarServicos()
    }

    /**
     * Configura o RecyclerView que exibirá os serviços.
     * Define seu layout como horizontal para a rolagem lateral.
     */
    private fun setupRecyclerView() {
        // Cria o adapter, passando 'false' para não mostrar o nome do freelancer nos cards
        servicoAdapter = ServicoAdapter(servicosList, showFreelancerName = false)
        binding.rvServicosOferecidos.apply {
            layoutManager = LinearLayoutManager(this@TelaNegocioAutonomo, LinearLayoutManager.HORIZONTAL, false)
            adapter = servicoAdapter
        }
    }

    /**
     * Busca no Firestore o documento do usuário (autônomo) e chama a função para preencher a UI.
     */
    private fun carregarDadosAutonomo() {
        db.collection("users").document(autonomoId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    if (usuario != null) {
                        usuarioAtual = usuario // Armazena para uso no botão do WhatsApp
                        preencherDados(usuario) // Chama a função que preenche a tela


                        carregarEPreencherEndereco(autonomoId!!)
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
     * Preenche todos os componentes visuais da tela com os dados do objeto Usuario.
     * @param usuario O objeto Usuario contendo as informações do autônomo.
     */
    private fun preencherDados(usuario: Usuario) {
        // Preenche nome, telefone, e o endereço formatado como "Cidade - UF"
        binding.tvNomeAutonomo.text = usuario.nome
        binding.tvTelefone.text = usuario.numeroTelefone

        binding.ivAutonomoAvatar.load(usuario.fotoUrl) {
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
        }

        // Lógica para a seção de avaliações: só exibe se houverem avaliações
        val perfilAutonomo = usuario.autonomo
        if (perfilAutonomo != null && perfilAutonomo.totalAvaliacoes > 0) {
            binding.rbMediaAvaliacoes.visibility = View.VISIBLE
            binding.tvNumeroAvaliacoes.visibility = View.VISIBLE
            binding.btnVerAvaliacoes.visibility = View.VISIBLE

            // Preenche a RatingBar e o texto com a contagem
            binding.rbMediaAvaliacoes.rating = perfilAutonomo.mediaAvaliacoes.toFloat()
            binding.tvNumeroAvaliacoes.text = String.format(Locale.getDefault(), "(%d)", perfilAutonomo.totalAvaliacoes)
        } else {
            // Se não há avaliações, esconde toda a seção
            binding.rbMediaAvaliacoes.visibility = View.GONE
            binding.tvNumeroAvaliacoes.visibility = View.GONE
            binding.btnVerAvaliacoes.visibility = View.GONE
        }
        
        // Popula as especializações do autônomo como Chips
      //  perfilAutonomo?.especializacao?.let { especializacoes ->
          //  binding.chipGroupCategorias.removeAllViews() // Limpa chips antigos
           // val categorias = especializacoes.split(",").map { it.trim() }
           // for (categoria in categorias) {
              //  if (categoria.isNotEmpty()){
                //    val chip = Chip(this)
              //      chip.text = categoria
            //        binding.chipGroupCategorias.addView(chip)
          //      }
        //    }
      //  }
    }

    private fun carregarEPreencherEndereco(userId: String) {
        db.collection("enderecos").document(userId).get()
            .addOnSuccessListener { document ->
                val endereco = document.toObject(Endereco::class.java)
                val cidade = endereco?.cidade ?: ""
                val estado = endereco?.estado ?: ""

                // Preenche o campo tvEndereco com o formato "Cidade - UF"
                binding.tvEndereco.text = if (cidade.isNotBlank() || estado.isNotBlank()) {
                    "$cidade - $estado"
                } else {
                    "Localização não informada"
                }
            }
            .addOnFailureListener {
                binding.tvEndereco.text = "Erro ao carregar endereço."
            }
    }

    /**
     * Busca na coleção 'servico' todos os serviços que pertencem a este autônomo.
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
     * Configura os listeners de clique para todos os botões da tela.
     */
    private fun configurarBotoes() {
        // Botão para voltar à tela anterior
        binding.btnVoltarPesquisa.setOnClickListener {
            finish()
        }

        // Botão para abrir a lista detalhada de avaliações
        binding.btnVerAvaliacoes.setOnClickListener {
            val intent = Intent(this, FragmentContainerActivity::class.java).apply {
                putExtra("FRAGMENT_NAME", "fragmentListaAvaliacoes")
                putExtra("autonomo_id", autonomoId)
            }
            startActivity(intent)
        }

        // Botão para iniciar o fluxo de criação de um pedido para este autônomo
        binding.btnFazerPedido.setOnClickListener {
            val intent = Intent(this, TelaCriacaoPedido::class.java).apply {
                putExtra("AUTONOMO_ID", autonomoId)
                putExtra("MODE", "CREATE")
            }
            startActivity(intent)
        }

        // Botão para abrir a conversa no WhatsApp
        binding.btnWhatsapp.setOnClickListener {
            usuarioAtual?.numeroTelefone?.let { numero ->
                // Limpa o número para conter apenas dígitos
                val numeroLimpo = numero.replace(Regex("[^0-9]"), "")
                val url = "https://api.whatsapp.com/send?phone=$numeroLimpo"
                
                val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                // Tenta abrir o WhatsApp, com um tratamento de erro caso não esteja instalado
                try {
                    startActivity(whatsappIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "WhatsApp não instalado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}