package com.jobmatch

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityCadastrarServicoBinding
import android.content.Intent

class CadastrarServico : AppCompatActivity() {
    private val binding: ActivityCadastrarServicoBinding by lazy {
        ActivityCadastrarServicoBinding.inflate(layoutInflater)
    }
    //Variável para armazenar URI da foto
    private var fotoUri: String? = null


    //Função para chamar foto da galeria
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null){
            fotoUri = uri.toString() //Armazena URI como string
            Toast.makeText(this, "Foto anexada com sucesso!", Toast.LENGTH_SHORT).show()
        } else{
            Toast.makeText(this, "Seleção de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    //Função para coletar, validar e salvar os dados do serviço
    private fun salvarServico() {
        val nomeServico = binding.txtNomeServico.text.toString().trim()
        val descricaoServico = binding.txtDescricaoNegocio.text.toString().trim()
        val categoria = binding.txtCategoriaServico.text.toString()
        //spinner para Modelo de Cobrança
        val modeloCobranca = binding.ModeloCobranca.selectedItem.toString()
        val precoStr = binding.txtPreco.text.toString()


        //--------------------------------------------------------------------------------
        //Validação dos dados
        if (nomeServico.isEmpty() || descricaoServico.isEmpty() || precoStr.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }


        val descricaoCompleta = "$descricaoServico | Preço: $precoStr | Modelo: $modeloCobranca"

        //Instanciar o Objeto Serviço
        val novoServico = Servico(
            uidUsuario = "uidUsuarioLogado",
            nomeServico = nomeServico,
            descricaoServico = descricaoCompleta,
            categoria = categoria,
            modeloCobranca = modeloCobranca,
            fotoServico = fotoUri,
            preco = precoStr.toDoubleOrNull()
        )

        val preco: Double
        try {
            // Tenta obter o modelo de cobrança (Ponto de falha 1)
            val modeloCobranca = binding.ModeloCobranca.selectedItem.toString()

            // Tenta obter o preço (Ponto de falha 2)
            val preco: Double? = precoStr.toDoubleOrNull()

            if (nomeServico.isEmpty() || descricaoServico.isEmpty() || preco == null || precoStr.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor, preencha todos os campos e use um preço válido.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val mensagem =
                "Serviço '$nomeServico', Preço: R$$preco, Possui foto?: ${if (fotoUri != null) "Sim" else "Não"}, Modelo de Cobrança: '$modeloCobranca'"
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()

            //Enviar o Objeto de volta para Tela de Perfil do Autonomo
            val resultIntent =
                Intent().apply { // CORRIGIDO: Usando 'Intent()' em vez de 'intent.apply'
                    // ... (Seus putExtra's)
                    putExtra("UID_USUARIO", novoServico.uidUsuario)
                    putExtra("NOME_SERVICO", novoServico.nomeServico)
                    putExtra("DESCRICAO_COMPLETA", novoServico.descricaoServico)
                    putExtra("FOTO_URI", novoServico.fotoServico)
                    putExtra("MODELO_COBRANCA", novoServico.modeloCobranca)
                    putExtra("CATEGORIA", novoServico.categoria)
                    putExtra("PRECO_BASE", novoServico.preco.toString()) // Adiciona o preço
                }

            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Serviço salvo com sucesso!", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            // Se houver qualquer falha (incluindo Spinner/View Binding/Conversão)
            Toast.makeText(this, "Erro ao processar os dados: ${e.message}", Toast.LENGTH_LONG)
                .show()
            e.printStackTrace() // Imprime o rastreamento de pilha para o Logcat
        }
    }

    private fun mostrarTipoCobranca() {
        val tipoCobrancas = arrayOf(
            "Selecione o tipo de cobrança",
            "Por Hora",
            "Preço Fixo",
            "Por Diária",
            "Por Unidade",
            "A Combinar"
        )
        val spinnerCobrancas: Spinner = binding.ModeloCobranca

        val adapter = ArrayAdapter(
            this,
            R.layout.custom_spinner_item,
            tipoCobrancas
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCobrancas.adapter = adapter

        spinnerCobrancas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    // Faça algo com o item selecionado
                    Toast.makeText(
                        applicationContext,
                        "Selecionado: $selectedItem",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ação quando nada é selecionado
                // Este método fica vazio, pois nenhuma ação é necessária se a seleção for nula.
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        mostrarTipoCobranca()

        binding.imgSeletor.setOnClickListener {
            binding.ModeloCobranca.performClick()
        }


        //função do botão voltar
        binding.btnVoltarPerfilAutonomo2.setOnClickListener {
            finish()
        }

        //Botão Anexar foto
        binding.btnAnexo.setOnClickListener {
            // Lógica para anexar a foto
            pickImageLauncher.launch("image/*")
        }

        binding.btnSalvarServico.setOnClickListener {
            // Lógica para salvar o serviço
            salvarServico()
            }

        }
}