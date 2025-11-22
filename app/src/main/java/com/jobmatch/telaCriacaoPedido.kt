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
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaCriacaoPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Ajusta o padding para as barras do sistema (comum a ambos os modos)
        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupDropdownMenus()

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
        setupClickListeners()
        preencherDadosUsuarioLogado()
        preencherCamposComIntentAnterior()
    }

    // Configura a tela para o modo de VISUALIZAÇÃO de pedido
    private fun setupViewOnlyMode(pedidoId: String) {
        // 1. Altera a UI para modo de visualização
        binding.toolbar.title = "Informações do Pedido"
        binding.btnEnviarPedido.visibility = View.GONE
        binding.btnAnexarFotos.visibility = View.GONE

        // Desabilita todos os campos de entrada
        binding.txtNome.isEnabled = false
        binding.txtTelefone.isEnabled = false
        binding.txtEmail.isEnabled = false
        binding.actvTipoServico.isEnabled = false
        binding.txtDescricaoServico.isEnabled = false
        binding.txtCidade.isEnabled = false
        binding.actvEstado.isEnabled = false
        binding.actvTempoServico.isEnabled = false

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
    
    // Preenche a UI com os dados do usuário para conveniência
    private fun preencherDadosUsuarioLogado(){
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val user = userDoc.toObject(Usuario::class.java)
            binding.txtNome.setText(user?.nome)
            binding.txtTelefone.setText(user?.numeroTelefone)
            binding.txtEmail.setText(user?.email)
            binding.txtCidade.setText(user?.cidade ?: "")
            binding.actvEstado.setText(user?.estado ?: "", false)
        }
    }

    // Preenche a UI com os dados de um pedido existente
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
        val serviceTypes = resources.getStringArray(R.array.service_type_options)
        val serviceTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTypes)
        binding.actvTipoServico.setAdapter(serviceTypeAdapter)

        val serviceTimes = resources.getStringArray(R.array.service_time_options)
        val serviceTimeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTimes)
        binding.actvTempoServico.setAdapter(serviceTimeAdapter)
        
        val states = resources.getStringArray(R.array.brazilian_states)
        val stateAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, states)
        binding.actvEstado.setAdapter(stateAdapter)
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
        binding.btnAnexarFotos.setOnClickListener { openFileSelector() }
        binding.btnEnviarPedido.setOnClickListener { enviarPedido() }
    }
    
    // NOVA FUNÇÃO para lidar com o envio do pedido
    private fun enviarPedido() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado. Faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Validação dos campos do formulário
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

        // 2. Cria o objeto Pedido
        val novoPedido = Pedidos(
            nomeSolicitacao = nome,
            telefoneSolicitante = telefone,
            emailSolicitante = email,
            tipoServico = tipoServico,
            descricaoServico = descricao,
            tempoServico = tempoServico,
            cidade = cidade, // Usa a cidade do formulário
            estado = estado, // Usa o estado do formulário
            status = "pendente", // Status inicial
            contratanteId = currentUser.uid,
            autonomo = "" // Autônomo ainda não foi definido
        )

        // 3. Salva o pedido no Firestore
        db.collection("pedido").add(novoPedido)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Pedido criado com sucesso!", Toast.LENGTH_SHORT).show()

                // 4. Navega para a lista de pedidos, como você sugeriu
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