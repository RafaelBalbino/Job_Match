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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityCadastrarServicoBinding
import kotlin.math.max
import kotlin.math.min

class CadastrarServico : AppCompatActivity() {
    private val binding: ActivityCadastrarServicoBinding by lazy {
        ActivityCadastrarServicoBinding.inflate(layoutInflater)
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //Variável para armazenar URI da foto
    private var fotoSelecionadaUri: Uri? = null


    //Função para chamar foto da galeria
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoSelecionadaUri = uri
            binding.imgAnexo.setImageURI(uri)
            binding.imgAnexo.visibility = View.VISIBLE // Torna a imagem visível
            Toast.makeText(this, "Foto anexada com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Seleção de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    //Função para calcular a distância de Levenshtein
    private fun levenshteinDistance(a: String, b: String): Int {
        val costs = IntArray(b.length + 1)
        for (j in 0..b.length) {
            costs[j] = j
        }
        for (i in 1..a.length) {
            costs[0] = i
            var newValue = i - 1
            for (j in 1..b.length) {
                val match = if (a[i - 1] == b[j - 1]) 0 else 1
                val costReplace = costs[j - 1] + match
                val costInsert = costs[j] + 1
                val costDelete = newValue + 1
                costs[j - 1] = newValue
                newValue = min(min(costInsert, costDelete), costReplace)
            }
        }
        return costs[b.length]
    }

    private fun calculateSimilarity(a: String, b: String): Double {
        val longerLength = max(a.length, b.length)
        if (longerLength == 0) {
            return 1.0 // Ambos estão vazios, 100% de similaridade
        }
        val distance = levenshteinDistance(a.lowercase(), b.lowercase())
        return (longerLength - distance) / longerLength.toDouble()
    }


    //Função para coletar, validar e salvar os dados do serviço
    private fun salvarServico() {
        // 1. Obter dados dos campos
        val nomeServico = binding.txtNomeServico.text.toString().trim()
        val descricaoServico = binding.txtDescricaoNegocio.text.toString().trim()
        val categoria = binding.txtCategoriaServico.text.toString().trim() // A forma de ler continua a mesma
        val modeloCobranca = binding.ModeloCobranca.selectedItem.toString()
        val userId = auth.currentUser?.uid

        //--------------------------------------------------------------------------------
        // 2. Validação dos dados
        if (userId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado. Faça o login novamente.", Toast.LENGTH_LONG).show()
            return
        }
        if (nomeServico.isEmpty() || descricaoServico.isEmpty() || categoria.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.ModeloCobranca.selectedItemPosition == 0) {
            Toast.makeText(this, "Por favor, selecione um modelo de cobrança.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // 3. Verificar serviços existentes
        db.collection("servicos")
            .whereEqualTo("uidUsuario", userId) // Busca apenas nos serviços do usuário atual
            .get()
            .addOnSuccessListener { querySnapshot ->
                val similarServices = mutableListOf<String>()
                for (document in querySnapshot.documents) {
                    val nomeServicoExistente = document.getString("nomeServico") ?: ""
                    if(nomeServicoExistente.isNotBlank()){
                        val similarity = calculateSimilarity(nomeServico, nomeServicoExistente)
                        if (similarity >= 0.8) {
                            similarServices.add(nomeServicoExistente)
                        }
                    }
                }

                if (similarServices.isNotEmpty()) {
                    showLoading(false)
                    val similarServicesText = "- " + similarServices.joinToString("\n- ")
                    val dialogMessage = "Foram encontrados os seguintes serviços com nomes parecidos no seu perfil:\n\n$similarServicesText\n\nDeseja continuar mesmo assim?"

                    AlertDialog.Builder(this)
                        .setTitle("Atenção: Serviço Similar Encontrado")
                        .setMessage(dialogMessage)
                        .setPositiveButton("Sim, Continuar") { _, _ ->
                            prosseguirComSalvamento(
                                userId,
                                nomeServico,
                                descricaoServico,
                                categoria,
                                modeloCobranca,
                                fotoSelecionadaUri
                            )
                        }
                        .setNegativeButton("Cancelar") { dialog, _ ->
                            resetarCampos()
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    prosseguirComSalvamento(
                        userId,
                        nomeServico,
                        descricaoServico,
                        categoria,
                        modeloCobranca,
                        fotoSelecionadaUri
                    )
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erro ao verificar serviços: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun resetarCampos() {
        binding.txtNomeServico.text?.clear()
        binding.txtCategoriaServico.text?.clear()
        binding.txtDescricaoNegocio.text?.clear()
        binding.ModeloCobranca.setSelection(0)
        binding.imgAnexo.setImageURI(null)
        binding.imgAnexo.visibility = View.GONE
        fotoSelecionadaUri = null
        Toast.makeText(this, "Operação cancelada.", Toast.LENGTH_SHORT).show()
    }


    private fun prosseguirComSalvamento(
        userId: String,
        nomeServico: String,
        descricaoServico: String,
        categoria: String,
        modeloCobranca: String,
        fotoUri: Uri?
    ) {
        showLoading(true)

        // 3. Instanciar o Objeto Serviço com os dados corretos e validados
        val novoServico = Servico(
            uidUsuario = userId,
            nomeServico = nomeServico,
            descricaoServico = descricaoServico,
            categoria = categoria,
            modeloCobranca = modeloCobranca,
            fotoServico = fotoUri?.toString() // A URI da imagem como String (pode ser nula)
        )

        // 4. Salvar o objeto no Firestore
        db.collection("servicos").add(novoServico)
            .addOnSuccessListener { documentReference ->
                // Atualiza o ID do objeto com o ID gerado pelo Firestore
                val servicoId = documentReference.id
                db.collection("servicos").document(servicoId).update("id", servicoId)

                showLoading(false)
                Toast.makeText(this, "Serviço '${novoServico.nomeServico}' cadastrado com sucesso!", Toast.LENGTH_LONG).show()

                // Enviar o serviço de volta se a tela anterior precisar dele
                val resultIntent = Intent().apply {
                    putExtra("NOVO_SERVICO", novoServico.copy(id = servicoId))
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

    // NOVA FUNÇÃO PARA CONFIGURAR O DROPDOWN DE CATEGORIAS
    private fun configurarDropdownCategorias() {
        val categorias = arrayOf(
            "Ajudante Geral", "Babá", "Barbeiro", "Cabelereiro(a)", "Carpinteiro",
            "Chaveiro", "Cuidador(a) de Idosos", "Cuidador(a) de Pets", "Decorador(a)", "Dedetizador",
            "Desenvolvedor(a) de Sites", "Diarista", "Eletricista", "Encanador(a)", "Entregador(a)",
            "Faxineiro(a)", "Fotógrafo(a)", "Garçom/Garçonete", "Jardineiro(a)", "Manicure e Pedicure",
            "Marceneiro", "Maquiador(a)", "Montador(a) de Móveis", "Motorista Particular", "Motoboy",
            "Organizador(a) de Eventos", "Pedreiro", "Personal Trainer", "Pintor(a)", "Técnico(a) de Informática"
        )

        // O ArrayAdapter conecta a lista de strings ao AutoCompleteTextView
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        binding.txtCategoriaServico.setAdapter(adapter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- CONFIGURA OS DROPDOWNS ---
        configurarDropdownCategorias() // <- CHAMADA DA NOVA FUNÇÃO
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

