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
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaCriacaoPedidoBinding

class TelaCriacaoPedido : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriacaoPedidoBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth // Adicionado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaCriacaoPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance() // Adicionado

        // Ajusta o padding para as barras do sistema (comum a ambos os modos)
        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()

        // Verifica o modo de operação (Criação vs. Visualização)
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

    // Configura a tela para o modo de CRIAÇÃO de pedido
    private fun setupCreateMode() {
        setupDropdownMenus()
        setupClickListeners()
        // A função abaixo foi renomeada para maior clareza
        preencherCamposComIntentAnterior()
    }

    // Configura a tela para o modo de VISUALIZAÇÃO de pedido
    private fun setupViewOnlyMode(pedidoId: String) {
        // 1. Altera a UI para modo de visualização
        binding.toolbar.title = "Informações do Pedido"
        binding.btnEnviarPedido.visibility = View.GONE
        binding.btnAnexarFotos.visibility = View.GONE

        // Desabilita todos os campos de entrada
        binding.actvTipoServico.isEnabled = false
        binding.actvTempoServico.isEnabled = false
        binding.txtDescricaoServico.isEnabled = false
        // Adicionar outros campos aqui se existirem no layout (ex: data, hora, etc.)

        // 2. Carrega e exibe os dados do pedido
        binding.progressBar.visibility = View.VISIBLE
        db.collection("pedido").document(pedidoId).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document != null && document.exists()) {
                    val pedido = document.toObject(Pedidos::class.java)
                    if (pedido != null) {
                        populateUiWithPedidoData(pedido)
                    } else {
                        Toast.makeText(this, "Erro ao ler os dados do pedido.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Pedido não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Falha ao carregar o pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    // Preenche a UI com os dados de um pedido existente
    private fun populateUiWithPedidoData(pedido: Pedidos) {
        binding.actvTipoServico.setText(pedido.tipoServico, false)
        binding.actvTempoServico.setText(pedido.tempoServico, false)
        binding.txtDescricaoServico.setText(pedido.descricaoServico)
        // Preencher outros campos como data e hora, se os IDs correspondentes existirem no layout
        // Ex: binding.etData.setText(pedido.data)
    }

    private fun setupDropdownMenus() {
        val serviceTypes = resources.getStringArray(R.array.service_type_options)
        val serviceTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTypes)
        binding.actvTipoServico.setAdapter(serviceTypeAdapter)

        val serviceTimes = resources.getStringArray(R.array.service_time_options)
        val serviceTimeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTimes)
        binding.actvTempoServico.setAdapter(serviceTimeAdapter)
    }

    // Preenche os campos se vierem de uma tela anterior (apenas no modo de criação)
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
        binding.btnAnexarFotos.setOnClickListener {
            openFileSelector()
        }

        binding.btnEnviarPedido.setOnClickListener {
            // A lógica de envio agora está em uma função separada
            enviarPedido()
        }
    }
    
    // NOVA FUNÇÃO para lidar com o envio do pedido
    private fun enviarPedido() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado. Faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Validação dos campos do formulário
        val tipoServico = binding.actvTipoServico.text.toString().trim()
        val descricao = binding.txtDescricaoServico.text.toString().trim()
        val tempoServico = binding.actvTempoServico.text.toString().trim()

        if (tipoServico.isEmpty() || descricao.isEmpty() || tempoServico.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos de serviço.", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE

        // 2. Busca os dados do usuário para complementar o pedido
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument == null || !userDocument.exists()) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Dados do usuário não encontrados.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val user = userDocument.toObject(Usuario::class.java)

                // 3. Cria o objeto Pedido
                val novoPedido = Pedidos(
                    nomeSolicitacao = user?.nome ?: "",
                    telefoneSolicitante = user?.numeroTelefone ?: "",
                    emailSolicitante = user?.email ?: "",
                    tipoServico = tipoServico,
                    descricaoServico = descricao,
                    tempoServico = tempoServico,
                    cidade = user?.cidade ?: "",
                    estado = user?.estado ?: "", // O campo estado pode ser adicionado ao formulário no futuro
                    status = "pendente", // Status inicial
                    contratanteId = currentUser.uid,
                    autonomo = "" // Autônomo ainda não foi definido
                )

                // 4. Salva o pedido no Firestore
                db.collection("pedido").add(novoPedido)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Pedido criado com sucesso!", Toast.LENGTH_SHORT).show()

                        // 5. Navega para a lista de pedidos, como você sugeriu
                        val intent = Intent(this, FragmentContainerActivity::class.java).apply {
                            putExtra("FRAGMENT_TYPE", "CONTRATANTE")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Erro ao criar o pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao buscar dados do usuário: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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