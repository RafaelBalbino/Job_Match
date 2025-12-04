package com.jobmatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jobmatch.databinding.ActivityCadastrarServicoBinding
import java.util.UUID

class CadastrarServico : AppCompatActivity() {
    private val binding: ActivityCadastrarServicoBinding by lazy {
        ActivityCadastrarServicoBinding.inflate(layoutInflater)
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Variáveis de estado
    private var fotoSelecionadaUri: Uri? = null
    private var servicoParaEditar: Servico? = null
    private var servicoParaCopiar: Servico? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Verifica os modos de operação
        servicoParaEditar = getParcelableExtraCompat("SERVICO_PARA_EDITAR", Servico::class.java)
        servicoParaCopiar = getParcelableExtraCompat("COPIAR_SERVICO", Servico::class.java)


        fetchAndSetupCategories()
        mostrarTipoCobranca()
        configurarCliques()

        // Lógica de configuração da tela com base no modo
        when {
            servicoParaEditar != null -> configurarModoEdicao()
            servicoParaCopiar != null -> configurarModoCopia()
            else -> configurarModoCriacao()
        }
    }

    // Helper para compatibilidade de getParcelableExtra
    private fun <T : Parcelable> getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(key) as? T
        }
    }

    private fun configurarModoCriacao() {
        binding.textView10.text = "Cadastrar Serviço"
    }

    private fun configurarModoEdicao() {
        binding.textView10.text = "Editar Serviço"
        servicoParaEditar?.let { preencherFormularioComServico(it) }
    }

    // NOVA FUNÇÃO: Configura a tela para o modo de cópia
    private fun configurarModoCopia() {
        binding.textView10.text = "Adicionar Serviço"
        servicoParaCopiar?.let { servico ->
            // Preenche os campos
            binding.txtNomeServico.setText(servico.nomeServico)
            binding.txtCategoriaServico.setText(servico.categoria, false)

            // Desabilita a edição dos campos, como solicitado
            binding.tilNomeServico.isEnabled = false
            binding.tilCategoriaServico.isEnabled = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun preencherFormularioComServico(servico: Servico) {
        binding.txtNomeServico.setText(servico.nomeServico)
        binding.txtDescricaoNegocio.setText(servico.descricaoServico)
        binding.txtCategoriaServico.setText(servico.categoria, false)

        val cobrancaAdapter = binding.ModeloCobranca.adapter as ArrayAdapter<String>
        val position = cobrancaAdapter.getPosition(servico.modeloCobranca)
        if (position >= 0) {
            binding.ModeloCobranca.setSelection(position)
        }

        if (!servico.fotoServico.isNullOrEmpty()) {
            fotoSelecionadaUri = Uri.parse(servico.fotoServico)
            binding.imgAnexo.load(fotoSelecionadaUri) {
                crossfade(true)
                error(R.drawable.ic_image_placeholder)
            }
            binding.imgAnexo.visibility = View.VISIBLE
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoSelecionadaUri = uri
            binding.imgAnexo.setImageURI(uri)
            binding.imgAnexo.visibility = View.VISIBLE
            Toast.makeText(this, "Foto anexada com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Seleção de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarCliques(){
        binding.imgSeletor.setOnClickListener { binding.ModeloCobranca.performClick() }
        binding.btnVoltarPerfilAutonomo2.setOnClickListener { finish() }
        binding.btnAnexo.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSalvarServico.setOnClickListener { salvarServico() }
    }

    private fun salvarServico() {
        val nomeServico = binding.txtNomeServico.text.toString().trim()
        val descricaoServico = binding.txtDescricaoNegocio.text.toString().trim()
        val categoria = binding.txtCategoriaServico.text.toString().trim()
        val modeloCobranca = binding.ModeloCobranca.selectedItem.toString()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado.", Toast.LENGTH_LONG).show()
            return
        }

        if (descricaoServico.isEmpty() || nomeServico.isEmpty() || categoria.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos editáveis.", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.ModeloCobranca.selectedItemPosition == 0) {
            Toast.makeText(this, "Por favor, selecione um modelo de cobrança.", Toast.LENGTH_SHORT).show()
            return
        }

        prosseguirComSalvamento(userId, nomeServico, descricaoServico, categoria, modeloCobranca, fotoSelecionadaUri)
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
        val fotoOriginalUrl = if(servicoParaCopiar != null) null else servicoParaEditar?.fotoServico
        
        val precisaUpload = fotoUri != null && (servicoParaEditar == null || fotoUri.toString() != fotoOriginalUrl)

        if (precisaUpload) {
            uploadFotoParaStorage(userId, nomeServico, descricaoServico, categoria, modeloCobranca, fotoUri!!)
        } else {
            salvarDadosNoFirestore(userId, nomeServico, descricaoServico, categoria, modeloCobranca, fotoOriginalUrl)
        }
    }

    private fun uploadFotoParaStorage(
        userId: String, nomeServico: String, descricaoServico: String, categoria: String, modeloCobranca: String, fotoUri: Uri
    ) {
        val filename = UUID.randomUUID().toString() + ".jpg"
        val ref = storage.reference.child("servicos/${userId}/$filename")

        ref.putFile(fotoUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uriPublica ->
                    salvarDadosNoFirestore(userId, nomeServico, descricaoServico, categoria, modeloCobranca, uriPublica.toString())
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Falha no upload da foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun salvarDadosNoFirestore(
        userId: String, nomeServico: String, descricaoServico: String, categoria: String, modeloCobranca: String, fotoUrlPublica: String?
    ) {
        val idServico = servicoParaEditar?.id ?: db.collection("servico").document().id
        
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val user = userDoc.toObject(Usuario::class.java)
            val servicoData = hashMapOf(
                "id" to idServico,
                "uidAutonomo" to userId,
                "nomeAutonomo" to (user?.nome ?: ""),
                "nomeServico" to nomeServico,
                "descricaoServico" to descricaoServico,
                "categoria" to categoria,
                "modeloCobranca" to modeloCobranca,
                "fotoServico" to fotoUrlPublica
            )

            val task = if (servicoParaEditar != null) {
                db.collection("servico").document(servicoParaEditar!!.id).update(servicoData as Map<String, Any>)
            } else {
                db.collection("servico").document(idServico).set(servicoData as Map<String, Any>)
            }

            task.addOnSuccessListener {
                showLoading(false)
                val mensagem = when {
                    servicoParaEditar != null -> "Serviço atualizado com sucesso!"
                    servicoParaCopiar != null -> "Serviço adicionado ao seu perfil!"
                    else -> "Serviço cadastrado com sucesso!"
                }
                Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_OK)
                finish()
            }.addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erro ao salvar serviço: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnSalvarServico.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun mostrarTipoCobranca() {
        val tipoCobrancas = arrayOf("Selecione o tipo de cobrança", "Por Hora", "Preço Fixo", "Por Diária", "Por Unidade", "A Combinar")
        val spinnerCobrancas: Spinner = binding.ModeloCobranca
        val adapter = ArrayAdapter(this, R.layout.custom_spinner_item, tipoCobrancas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCobrancas.adapter = adapter
        spinnerCobrancas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchAndSetupCategories() {
        db.collection("servico").get().addOnSuccessListener {
            val categories = it.documents.mapNotNull { doc -> doc.getString("categoria") }.filter { it.isNotBlank() }.distinct().sorted()
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            binding.txtCategoriaServico.setAdapter(adapter)
        }
    }
}