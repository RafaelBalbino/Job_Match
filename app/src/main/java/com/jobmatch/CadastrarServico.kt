package com.jobmatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityCadastrarServicoBinding

class CadastrarServico : AppCompatActivity() {
    private val binding: ActivityCadastrarServicoBinding by lazy {
        ActivityCadastrarServicoBinding.inflate(layoutInflater)
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //Variável para armazenar URI da foto
    private var fotoSelecionadaUri: String? = null


    //Função para chamar foto da galeria
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoSelecionadaUri = uri.toString()
            binding.imgAnexo.setImageURI(uri)
            binding.imgAnexo.visibility = View.VISIBLE // Torna a imagem visível
            Toast.makeText(this, "Foto anexada com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Seleção de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    //Função para coletar, validar e salvar os dados do serviço
    private fun salvarServico() {
        // 1. Obter dados dos campos
        val nomeServico = binding.txtNomeServico.text.toString().trim()
        val descricaoServico = binding.txtDescricaoNegocio.text.toString().trim()
        val categoria = binding.txtCategoriaServico.text.toString().trim()
        val modeloCobranca = binding.ModeloCobranca.selectedItem.toString()
        val precoStr = binding.txtPreco.text.toString().trim()
        val userId = auth.currentUser?.uid

        //--------------------------------------------------------------------------------
        // 2. Validação dos dados
        if (userId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado. Faça o login novamente.", Toast.LENGTH_LONG).show()
            return
        }
        if (nomeServico.isEmpty() || descricaoServico.isEmpty() || precoStr.isEmpty() || categoria.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.ModeloCobranca.selectedItemPosition == 0) {
            Toast.makeText(this, "Por favor, selecione um modelo de cobrança.", Toast.LENGTH_SHORT).show()
            return
        }

        val preco = try {
            precoStr.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Por favor, insira um preço válido.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // 3. Instanciar o Objeto Serviço com os dados corretos e validados
        val novoServico = Servico(
            uidUsuario = userId, // O ID real do usuário logado
            nomeServico = nomeServico,
            descricaoServico = descricaoServico,
            categoria = categoria,
            modeloCobranca = modeloCobranca,
            fotoServico = fotoSelecionadaUri, // A URI da imagem (pode ser nula)
            precoBase = preco // O preço convertido para Double
        )

        // 4. Salvar o objeto no Firestore
        db.collection("servicos").add(novoServico)
            .addOnSuccessListener { documentReference ->
                showLoading(false)
                Toast.makeText(this, "Serviço '${novoServico.nomeServico}' cadastrado com sucesso!", Toast.LENGTH_LONG).show()
                
                // Opcional: Enviar o serviço de volta se a tela anterior precisar dele
                val resultIntent = Intent().apply {
                    putExtra("NOVO_SERVICO", novoServico)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Fecha a tela e volta para a anterior
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erro ao salvar serviço: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnSalvarServico.isEnabled = !isLoading
        // Adicione um ProgressBar ao seu layout e controle a visibilidade aqui
        // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
                parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // A lógica pode ser adicionada aqui se necessário
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ação quando nada é selecionado
                // Este método fica vazio, pois nenhuma ação é necessária se a seleção for nula.
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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
