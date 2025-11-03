package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import coil.load
import com.jobmatch.databinding.ActivityTelaPerfilAutonomoBinding
import com.jobmatch.databinding.ItemServicoButtonBinding

class TelaPerfilAutonomo : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaPerfilAutonomoBinding.inflate(layoutInflater)
    }


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
                preco = preco
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
        binding.containerServicos.addView(itemServicoView)

    }

    // Função que cria e configura o CardView
    private fun criarCardViewServico(servico: Servico): CardView {
        // 1. Infla o layout do CardView
        val binding = ItemServicoButtonBinding.inflate(LayoutInflater.from(this))
        val cardView = binding.root


        // 2. Encontra os elementos internos usando findViewById no CardView inflado

        // Título do Serviço (em cima)
        val tvNomeServico = binding.txtNomeCard
        // Imagem de Fundo/Principal
        val ivFundo = binding.imageServicoFundo
        // Ícone de Edição (canto superior direito)
        val ivEditar = binding.imgEditar
        // Icone de Alerta/Status (central)
        val ivAlerta = binding.imgAlerta
        // Texto de Alerta/Status (central)
        val layoutAlerta = binding.layoutAlerta
        val tvAlerta = binding.textAbaixoIcone



        // 3. Define os dados dinâmicos do Serviço
        tvNomeServico.text = servico.nomeServico ?: "Novo Serviço"

        val dadosCompletos = servico.nomeServico.isNullOrBlank() && !servico.fotoServico.isNullOrBlank()

        // Lógica para imagem de fundo (exemplo)
        if (dadosCompletos) {
            // 1. Oculta o bloco de alerta
            layoutAlerta.visibility = View.GONE

            // 2. Define a foto do serviço
            ivFundo.load(servico.fotoServico){
                crossfade(true)
            }
        } else {
            // 1. Torna o bloco de alerta VISÍVEL
            layoutAlerta.visibility = View.VISIBLE

            // 2. Define o texto de alerta
            val fotoAusente = servico.fotoServico.isNullOrBlank()
            val nomeAusente = servico.nomeServico.isNullOrBlank()

            tvAlerta.text = when {
                fotoAusente -> "Imagem Pendente"
                nomeAusente -> "Nome Pendente"
                else -> "Dados Incompletos" // Caso não deva acontecer se a lógica de dadosCompletos estiver correta
            }

            // 3. Remove qualquer imagem definida (mostra o fundo cinza de alerta)
            ivFundo.setImageDrawable(null)
        }

        // 4. Define as ações de clique

        // Ação principal (clicar no card)
        cardView.setOnClickListener {
            Toast.makeText(this, "Abrir detalhes de: ${servico.nomeServico}", Toast.LENGTH_SHORT).show()
            // Implemente aqui a navegação para a tela de detalhes
        }

        // Ação do botão de edição
        ivEditar.setOnClickListener {
            Toast.makeText(this, "Editar serviço: ${servico.nomeServico}", Toast.LENGTH_SHORT).show()
            // Implemente aqui a lógica para editar
        }

        // 5. Retorna o CardView preenchido
        return cardView
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


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

        // ✅ BOTÃO CADASTRAR SERVIÇO: Inicia o Launcher
        binding.btnCadastrarServico.setOnClickListener {
            val intent = Intent(this, CadastrarServico::class.java)
            cadastrarServicoLauncher.launch(intent)
        }
    }
}
