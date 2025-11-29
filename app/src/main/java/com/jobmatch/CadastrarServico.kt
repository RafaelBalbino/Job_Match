package com.jobmatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jobmatch.databinding.ActivityCadastrarServicoBinding
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Verifica se a tela foi aberta em modo de edição
        servicoParaEditar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("SERVICO_PARA_EDITAR", Servico::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Servico>("SERVICO_PARA_EDITAR")
        }

        configurarDropdownCategorias()
        mostrarTipoCobranca()
        configurarCliques()

        if (servicoParaEditar != null) {
            configurarModoEdicao()
        } else {
            binding.textView10.text = "Cadastrar Serviço"
        }
    }

    private fun configurarModoEdicao() {
        binding.textView10.text = "Editar Serviço"
        servicoParaEditar?.let { preencherFormularioComServico(it) }
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

    private fun levenshteinDistance(a: String, b: String): Int { /* ... */ return 0 }
    private fun calculateSimilarity(a: String, b: String): Double { /* ... */ return 0.0 }

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
        if (nomeServico.isEmpty() || descricaoServico.isEmpty() || categoria.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.ModeloCobranca.selectedItemPosition == 0) {
            Toast.makeText(this, "Por favor, selecione um modelo de cobrança.", Toast.LENGTH_SHORT).show()
            return
        }

        if (servicoParaEditar != null) {
            prosseguirComSalvamento(userId, nomeServico, descricaoServico, categoria, modeloCobranca, fotoSelecionadaUri)
            return
        }

        showLoading(true)
        db.collection("servico").whereEqualTo("uidUsuario", userId).get()
            .addOnSuccessListener { querySnapshot ->
                val similarServices = mutableListOf<String>()
                // ... (lógica de similaridade) ...

                if (similarServices.isNotEmpty()) {
                    showLoading(false)
                    // ... (AlertDialog para confirmar) ...
                } else {
                    prosseguirComSalvamento(userId, nomeServico, descricaoServico, categoria, modeloCobranca, fotoSelecionadaUri)
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

    @Suppress("UNCHECKED_CAST")
    private fun prosseguirComSalvamento(
        userId: String,
        nomeServico: String,
        descricaoServico: String,
        categoria: String,
        modeloCobranca: String,
        fotoUri: Uri?
    ) {
        showLoading(true)

        val fotoOriginalUrl = servicoParaEditar?.fotoServico

        // Verifica se é necessário fazer o upload de uma nova foto
        val precisaUpload = fotoUri != null &&
                // Compara a nova URI com a URL antiga para saber se mudou
                (servicoParaEditar == null || fotoUri.toString() != fotoOriginalUrl)

        if (precisaUpload) {
            uploadFotoParaStorage(userId, nomeServico, descricaoServico, categoria, modeloCobranca, fotoUri!!)
        } else {
            // Mantém a URL pública antiga (se for edição), ou passa null
            val urlPublica = fotoOriginalUrl

            salvarDadosNoFirestore(userId, nomeServico, descricaoServico, categoria, modeloCobranca, urlPublica)
        }
    }

    //Onde vamos colocar as fotos dos serviços
    private fun uploadFotoParaStorage(
        userId: String,
        nomeServico: String,
        descricaoServico: String,
        categoria: String,
        modeloCobranca: String,
        fotoUri: Uri
    ) {
        val filename = UUID.randomUUID().toString() + ".jpg"
        val ref = storage.reference.child("servicos/${userId}/$filename")

        ref.putFile(fotoUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uriPublica ->
                    // 3. Sucesso! Agora salva no Firestore com a URL pública
                    salvarDadosNoFirestore(
                        userId,
                        nomeServico,
                        descricaoServico,
                        categoria,
                        modeloCobranca,
                        uriPublica.toString()
                    )
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Falha no upload da foto: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    //Onde vamos salvar os dados
    @Suppress("UNCHECKED_CAST")
    private fun salvarDadosNoFirestore(
        userId: String,
        nomeServico: String,
        descricaoServico: String,
        categoria: String,
        modeloCobranca: String,
        fotoUrlPublica: String?
    ) {
        val idServico = servicoParaEditar?.id ?: db.collection("servico").document().id

        val servicoData = hashMapOf(
            "id" to idServico,
            "uidUsuario" to userId,
            "nomeServico" to nomeServico,
            "descricaoServico" to descricaoServico,
            "categoria" to categoria,
            "modeloCobranca" to modeloCobranca,
            "fotoServico" to fotoUrlPublica
        )

        val task = if (servicoParaEditar != null) {
            // MODO EDIÇÃO: Atualiza o documento
            db.collection("servico").document(servicoParaEditar!!.id).update(servicoData as Map<String, Any>)
        } else {
            // MODO CRIAÇÃO: Cria um novo documento
            db.collection("servico").document(idServico).set(servicoData as Map<String, Any>)
        }

        task.addOnSuccessListener {
            showLoading(false)
            val mensagem = if (servicoParaEditar != null) "Serviço atualizado com sucesso!" else "Serviço cadastrado com sucesso!"
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()

            val newOrUpdatedServico = Servico(
                id = idServico,
                uidUsuario = userId,
                nomeServico = nomeServico,
                descricaoServico = descricaoServico,
                categoria = categoria,
                modeloCobranca = modeloCobranca,
                fotoServico = fotoUrlPublica
            )

            val resultIntent = Intent().apply { putExtra("SERVICO_ATUALIZADO", newOrUpdatedServico) }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erro ao salvar serviço: ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun configurarDropdownCategorias() {
        val categorias = arrayOf(
            "Ajudante Geral", "Babá", "Barbeiro", "Cabelereiro(a)", "Carpinteiro", "Chaveiro",
            "Cuidador(a) de Idosos", "Cuidador(a) de Pets", "Decorador(a)", "Dedetizador", "Desenvolvedor(a) de Sites",
            "Diarista", "Eletricista", "Encanador(a)", "Entregador(a)", "Faxineiro(a)", "Fotógrafo(a)",
            "Garçom/Garçonete", "Jardineiro(a)", "Manicure e Pedicure", "Marceneiro", "Maquiador(a)",
            "Montador(a) de Móveis", "Motorista Particular", "Motoboy", "Organizador(a) de Eventos",
            "Pedreiro", "Personal Trainer", "Pintor(a)", "Técnico(a) de Informática"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        binding.txtCategoriaServico.setAdapter(adapter)
    }
}