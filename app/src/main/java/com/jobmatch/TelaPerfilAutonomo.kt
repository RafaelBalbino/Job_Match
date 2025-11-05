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
import com.jobmatch.databinding.ActivityTelaPerfilAutonomoBinding
import com.jobmatch.databinding.ItemServicoButtonBinding
import androidx.recyclerview.widget.GridLayoutManager

class TelaPerfilAutonomo : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaPerfilAutonomoBinding.inflate(layoutInflater)
    }
    private val listaServico = mutableListOf<Servico>()
    private lateinit var servicosAdapter: ServicosAdapter

    //Lista para armaazenar e gerenciar serviços
    private val listaServicos = mutableListOf<Servico>()

    //Criação do Launcher para receber o resultado
    private val cadastrarServicoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        //ferificar se a operação foi bem sucedida
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data

            //Extrai dados do Intent
            val uidUsuario = data?.getStringExtra("UID_USUARIO")
            val nomeServico = data?.getStringExtra("NOME_SERVICO")
            val descricaoCompleta =
                data?.getStringExtra("DESCRICAO_COMPLETA") // Mapeado para descricaoServico
            val fotoUri = data?.getStringExtra("FOTO_URI") // Mapeado para fotoServico
            val modeloCobranca = data?.getStringExtra("MODELO_COBRANCA")
            val categoria = data?.getStringExtra("CATEGORIA")
            val precoStr = data?.getStringExtra("PRECO_BASE")

            val preco = precoStr?.toDoubleOrNull()

            //Criar Objeto Serviço
            val novoServico = Servico(
                uidUsuario = uidUsuario,
                nomeServico = nomeServico,
                descricaoServico = descricaoCompleta,
                fotoServico = fotoUri,
                modeloCobranca = modeloCobranca,
                categoria = categoria,
                precoBase = preco
            )

            //Adicionar e Atualiza
            adicionarServicoAoPerfil(novoServico)
        } else if (result.resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Operação cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    //Função para adicionar e exibir o serviço
    private fun adicionarServicoAoPerfil(novoServico: Servico) {
        listaServicos.add(novoServico)
        Toast.makeText(this, "Serviço adicionado com sucesso!", Toast.LENGTH_SHORT).show()


        //Cria o item de visualização
        val itemServicoView = criarCardViewServico(novoServico)

        //Adiciona ao container
        binding.categoryContainer.addView(itemServicoView)

    }

    // Função que cria e configura o CardView
    private fun criarCardViewServico(servico: Servico): CardView {
        // 1. Infla o layout do CardView
        val binding = ItemServicoButtonBinding.inflate(LayoutInflater.from(this))
        val cardView = binding.cardServicoRoot


        // 2. Encontra os elementos internos usando findViewById no CardView inflado

        // Título do Serviço (em cima)
        val tvNomeServico = binding.txtNameCardServico
        val ivFundo = binding.imageServicoFundo
        val ivEditar = binding.iconEditar
        val ivExcluir = binding.iconExcluir

        // 3. Define os dados dinâmicos do Serviço
        tvNomeServico.text = servico.nomeServico ?: "Serviço Desconhecido"

        val fotoUrl = servico.fotoServico

        val dadosCompletos = servico.nomeServico.isNullOrBlank() && !servico.fotoServico.isNullOrBlank()

        // Lógica para carregar a imagem (assumindo que você usa Coil/Glide)
        if (!fotoUrl.isNullOrBlank()) {
            // Exemplo usando a extensão load() do Coil
            ivFundo.load(fotoUrl) {
                crossfade(true)
                // placeholder(R.drawable.ic_placeholder_loading) // Se você tiver um placeholder
            }
            // Remove o fundo de cor (se for usado pelo placeholder)
            ivFundo.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))

        } else {
            // Se não houver foto, mostra um ícone de placeholder e cor de fundo
            ivFundo.setImageResource(R.drawable.ic_alerta_24) // Use um ícone de placeholder aqui
            ivFundo.setBackgroundColor(ContextCompat.getColor(this, R.color.cinza_claro))

        }


        // 4. Define as ações de clique

        // Ação principal (clicar no card)
    cardView.setOnClickListener {
        Toast.makeText(this@TelaPerfilAutonomo, "Abrir detalhes de: ${servico.nomeServico}", Toast.LENGTH_SHORT).show()
        // Implemente aqui a navegação para a tela de detalhes
    }

        // Ação do botão de edição
        ivEditar.setOnClickListener {
            Toast.makeText(this@TelaPerfilAutonomo, "Editar serviço: ${servico.nomeServico}", Toast.LENGTH_SHORT).show()
            // Implemente aqui a lógica para editar
        }
        // Ação do botão de exclusão
        ivExcluir.setOnClickListener {
            Toast.makeText(this@TelaPerfilAutonomo, "Excluir serviço: ${servico.nomeServico}", Toast.LENGTH_SHORT).show()
            // Implemente aqui a lógica para exclusão (ex: exclusão no Firebase e remoção da lista)
        }

        // 5. Retorna o CardView preenchido
        return cardView
    }

    private fun setupRecyclerView() {
        // 1. Inicializa o Adapter
        // O último parâmetro (onServiceClick) é o que acontece ao clicar em um Card
        servicosAdapter = ServicosAdapter(listaServicos) { servicoClicado ->
            // Exemplo: mostrar um Toast
            Toast.makeText(this, "Você clicou no serviço: ${servicoClicado.nomeServico}", Toast.LENGTH_SHORT).show()
            // Implemente aqui a navegação para a tela de detalhes do serviço
        }

        // 2. Configura o RecyclerView
        // O ID do seu RecyclerView no XML é 'containerServicos'
        binding.containerServicos.apply {

            // ** CONFIGURAÇÃO DO LAYOUT EM GRID **
            // spanCount = 2 significa 2 colunas
            layoutManager = GridLayoutManager(context, 2)

            adapter = servicosAdapter

            // 3. Otimização para NestedScrollView:
            // Desativa a rolagem interna do RecyclerView, deixando a rolagem para o NestedScrollView
            isNestedScrollingEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupRecyclerView()

        // botão Editar Perfil
        binding.btnEditarPerfil.setOnClickListener {
            // Abrir tela Edição Perfil Autonomo
            startActivity(
                Intent(
                    this,
                    TelaEdicaoPerfilAutonomo::class.java
                )
            )
        }


        binding.btnCadastrarServico.setOnClickListener {
            val intent = Intent(this, CadastrarServico::class.java)
            cadastrarServicoLauncher.launch(intent)
        }
    }
}
