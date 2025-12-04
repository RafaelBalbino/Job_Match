package com.jobmatch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaCriacaoPedidoBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TelaCriacaoPedido : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriacaoPedidoBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val ibgeService: IbgeService by lazy {
        Retrofit.Builder()
            .baseUrl("https://servicodados.ibge.gov.br/api/v1/localidades/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IbgeService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaCriacaoPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupDropdownMenus()
        setupStateDropdown() // Lógica do endereço
        fetchAndSetupCategories() // Lógica das categorias

        val mode = intent.getStringExtra("MODE")
        if (mode == "VIEW_ONLY") {
            val pedidoId = intent.getStringExtra("PEDIDO_ID")
            if (pedidoId.isNullOrEmpty()) {
                Toast.makeText(this, "ID do pedido não fornecido.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                setupViewOnlyMode(pedidoId)
            }
        } else {
            setupCreateMode()
        }
    }

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(it)
            Toast.makeText(this, "Anexado: $fileName", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCreateMode() {
        setupClickListeners()
        preencherDadosUsuarioLogado()
        preencherCamposComIntentAnterior()
    }

    private fun setupViewOnlyMode(pedidoId: String) {
        binding.toolbar.title = "Informações do Pedido"
        binding.btnEnviarPedido.visibility = View.GONE
        binding.btnAnexarFotos.visibility = View.GONE

        binding.txtNome.isEnabled = false
        binding.txtTelefone.isEnabled = false
        binding.txtEmail.isEnabled = false
        binding.actvTipoServico.isEnabled = false
        binding.txtDescricaoServico.isEnabled = false
        binding.txtCidade.isEnabled = false
        binding.actvEstado.isEnabled = false
        binding.actvTempoServico.isEnabled = false

        binding.progressBar.visibility = View.VISIBLE
        db.collection("pedido").document(pedidoId).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document != null && document.exists()) {
                    val pedido = document.toObject(Pedidos::class.java)
                    if (pedido != null) {
                        populateUiWithPedidoData(pedido)
                    } else {
                        showErrorAndFinish("Erro ao ler os dados do pedido.")
                    }
                } else {
                    showErrorAndFinish("Pedido não encontrado.")
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                showErrorAndFinish("Falha ao carregar o pedido: ${e.message}")
            }
    }
    
    private fun preencherDadosUsuarioLogado(){
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val user = userDoc.toObject(Usuario::class.java)
            binding.txtNome.setText(user?.nome)
            binding.txtTelefone.setText(user?.numeroTelefone)
            binding.txtEmail.setText(user?.email)

            buscarEPreencherEndereco(userId)
        }
    }

    private fun buscarEPreencherEndereco(userId: String) {
        db.collection("enderecos").document(userId).get()
            .addOnSuccessListener { enderecoDoc ->
                val endereco = enderecoDoc.toObject(Endereco::class.java)
                if (endereco != null) {
                    binding.actvEstado.setText(endereco.estado, false)
                    fetchCidadesPorEstado(endereco.estado ?: "") {
                        binding.txtCidade.setText(endereco.cidade)
                    }
                } else {
                    Toast.makeText(this, "Endereço não encontrado. Por favor, preencha a localização.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao carregar endereço: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateUiWithPedidoData(pedido: Pedidos) {
        binding.txtNome.setText(pedido.nomeSolicitacao)
        binding.txtTelefone.setText(pedido.telefoneSolicitante)
        binding.txtEmail.setText(pedido.emailSolicitante)
        binding.actvTipoServico.setText(pedido.tipoServico, false)
        binding.txtDescricaoServico.setText(pedido.descricaoServico)
        binding.txtCidade.setText(pedido.cidade)
        binding.actvEstado.setText(pedido.estado, false)
        binding.actvTempoServico.setText(pedido.tempoServico, false)
    }

    private fun setupDropdownMenus() {
        val serviceTimes = resources.getStringArray(R.array.service_time_options)
        val serviceTimeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTimes)
        binding.actvTempoServico.setAdapter(serviceTimeAdapter)
    }
    
    private fun fetchAndSetupCategories() {
        db.collection("servico").get().addOnSuccessListener {
            val categories = it.documents.mapNotNull { doc -> doc.getString("categoria") }.filter { it.isNotBlank() }.distinct().sorted()
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            binding.actvTipoServico.setAdapter(adapter)
        }
    }

    private fun setupStateDropdown() {
        val states = resources.getStringArray(R.array.brazilian_states)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, states)
        binding.actvEstado.setAdapter(adapter)

        binding.tilCidade.isEnabled = false

        binding.actvEstado.setOnItemClickListener { parent, view, position, id ->
            val selectedState = parent.getItemAtPosition(position).toString()
            binding.txtCidade.setText("")
            fetchCidadesPorEstado(selectedState)
        }
    }

    private fun fetchCidadesPorEstado(uf: String, onComplete: (() -> Unit)? = null) {
        binding.tilCidade.isEnabled = false

        ibgeService.buscarCidadesPorEstado(uf).enqueue(object : Callback<List<Municipio>> {
            override fun onResponse(call: Call<List<Municipio>>, response: Response<List<Municipio>>) {
                if (response.isSuccessful) {
                    val cidades = response.body()?.map { it.nome } ?: emptyList()
                    val cityAdapter = ArrayAdapter(this@TelaCriacaoPedido, android.R.layout.simple_dropdown_item_1line, cidades)
                    binding.txtCidade.setAdapter(cityAdapter)
                    binding.tilCidade.isEnabled = true
                    onComplete?.invoke()
                } else {
                    Toast.makeText(this@TelaCriacaoPedido, "Erro ao carregar cidades.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Municipio>>, t: Throwable) {
                Toast.makeText(this@TelaCriacaoPedido, "Falha de rede ao carregar cidades.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun preencherCamposComIntentAnterior() {
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        val serviceDescription = intent.getStringExtra("SERVICE_DESCRIPTION")

        if (!serviceName.isNullOrEmpty()) {
            binding.actvTipoServico.setText(serviceName, false)
        }

        if (!serviceDescription.isNullOrEmpty()) {
            binding.txtDescricaoServico.setText(serviceDescription)
        }
    }

    private fun setupClickListeners() {
        binding.btnAnexarFotos.setOnClickListener { openFileSelector() }
        binding.btnEnviarPedido.setOnClickListener { enviarPedido() }
    }
    
    private fun enviarPedido() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado. Faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        val nome = binding.txtNome.text.toString().trim()
        val telefone = binding.txtTelefone.text.toString().trim()
        val email = binding.txtEmail.text.toString().trim()
        val tipoServico = binding.actvTipoServico.text.toString().trim()
        val descricao = binding.txtDescricaoServico.text.toString().trim()
        val tempoServico = binding.actvTempoServico.text.toString().trim()
        val cidade = binding.txtCidade.text.toString().trim()
        val estado = binding.actvEstado.text.toString().trim()

        if (nome.isEmpty() || telefone.isEmpty() || email.isEmpty() || tipoServico.isEmpty() || descricao.isEmpty() || tempoServico.isEmpty() || cidade.isEmpty() || estado.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE

        val novoPedido = hashMapOf(
            "nomeSolicitacao" to nome,
            "telefoneSolicitante" to telefone,
            "emailSolicitante" to email,
            "tipoServico" to tipoServico,
            "descricaoServico" to descricao,
            "tempoServico" to tempoServico,
            "cidade" to cidade,
            "estado" to estado,
            "status" to "pendente",
            "contratanteId" to currentUser.uid,
            "autonomo" to (intent.getStringExtra("AUTONOMO_ID") ?: ""),
            "dataHora" to FieldValue.serverTimestamp()
        )

        db.collection("pedido").add(novoPedido)
            .addOnSuccessListener { 
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Pedido criado com sucesso!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, FragmentContainerActivity::class.java).apply {
                    putExtra("FRAGMENT_NAME", "fragmentListaPedidosContratante")
                    putExtra("TIPO_QUERY", "CONTRATANTE")
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao criar o pedido: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun openFileSelector() {
        selectFileLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        return result ?: uri.lastPathSegment ?: "Desconhecido"
    }
}