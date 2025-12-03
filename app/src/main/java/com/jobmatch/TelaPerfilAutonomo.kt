package com.jobmatch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaPerfilAutonomoBinding
import com.google.firebase.storage.FirebaseStorage

class TelaPerfilAutonomo : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaPerfilAutonomoBinding.inflate(layoutInflater)
    }

    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var servicosAdapter: ServicosAdapter
    private val listaServicos = mutableListOf<Servico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.imgViewVoltar.setOnClickListener { finish() }
        binding.imgViewPerfilIcon.visibility = View.VISIBLE

        setupRecyclerView()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            carregarDadosAutonomo(userId)
            
            binding.btnEditarPerfil.visibility = View.VISIBLE
            binding.btnCadastrarServico.visibility = View.VISIBLE

            binding.btnEditarPerfil.setOnClickListener {
                startActivity(Intent(this, TelaEdicaoPerfilAutonomo::class.java))
            }

            binding.btnCadastrarServico.setOnClickListener {
                activityResultLauncher.launch(Intent(this, CadastrarServico::class.java))
            }

            // ADICIONADO: Lógica dos novos botões
            binding.btnBuscarPedidos.setOnClickListener {
                abrirListaDePedidos("fragmentListaPedidosAutonomo", "BUSCA")
            }
            binding.btnMeusProjetos.setOnClickListener {
                abrirListaDePedidos("fragmentListaPedidosAutonomo", "PROJETOS")
            }

        } else {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun carregarDadosAutonomo(id: String) {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("users").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        binding.txtViewNomeAutonomo.text = it.nome
                        binding.txtViewTelefone.text = formatarTelefone(it.numeroTelefone)

                        if (!it.fotoUrl.isNullOrEmpty()) {
                            binding.imgViewBGPerfil.load(it.fotoUrl) { crossfade(true) }
                            binding.imgViewPerfilIcon.visibility = View.GONE
                        } else {
                            binding.imgViewBGPerfil.setImageDrawable(null)
                            binding.imgViewPerfilIcon.visibility = View.VISIBLE
                        }
                        carregarEExibirEndereco(id)
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Perfil não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("TelaPerfilAutonomo", "Erro ao carregar dados do usuário", e)
                Toast.makeText(this, "Erro ao carregar perfil.", Toast.LENGTH_SHORT).show()
            }

        db.collection("servico").whereEqualTo("uidAutonomo", id).get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                val servicos = documents.toObjects(Servico::class.java)
                servicosAdapter.updateData(servicos)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("TelaPerfilAutonomo", "Erro ao carregar serviços", e)
                Toast.makeText(this, "Erro ao carregar os serviços.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarEExibirEndereco(userId: String) {
        db.collection("enderecos").document(userId).get()
            .addOnSuccessListener { document ->
                val endereco = document.toObject(Endereco::class.java)
                val enderecoFmt = if (endereco != null && (endereco.cidade?.isNotBlank() == true || endereco.estado?.isNotBlank() == true)) {
                    "${endereco.cidade} - ${endereco.estado}"
                } else {
                    ""
                }
                binding.txtViewEndereco.text = enderecoFmt
            }
            .addOnFailureListener { e ->
                Log.e("TelaPerfilAutonomo", "Falha ao buscar endereço.", e)
                binding.txtViewEndereco.text = ""
            }
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            auth.currentUser?.uid?.let { carregarDadosAutonomo(it) }
        }
    }

    private fun setupRecyclerView() {
        servicosAdapter = ServicosAdapter(
            listaServicos,
            onServiceClick = { servico -> verDetalhesServico(servico) },
            onEditClick = { servico -> editarServico(servico) },
            onDeleteClick = { servico -> excluirServico(servico) }
        )
        binding.containerServicos.apply {
            layoutManager = NonScrollingGridLayoutManager(context, 2)
            adapter = servicosAdapter
        }
    }
    
    private fun abrirListaDePedidos(fragmentName: String, tipoQuery: String) {
        val intent = Intent(this, FragmentContainerActivity::class.java).apply {
            putExtra("FRAGMENT_NAME", fragmentName)
            putExtra("TIPO_QUERY", tipoQuery)
        }
        startActivity(intent)
    }

    private fun verDetalhesServico(servico: Servico) {
        val intent = Intent(this, telaServicoAmpliado::class.java).apply {
            putExtra("SERVICO", servico)
        }
        startActivity(intent)
    }

    private fun editarServico(servico: Servico) {
        val intent = Intent(this, CadastrarServico::class.java).apply {
            putExtra("SERVICO_PARA_EDITAR", servico)
        }
        activityResultLauncher.launch(intent)
    }

    private fun excluirServico(servico: Servico) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Serviço")
            .setMessage("Tem certeza de que deseja excluir o serviço '${servico.nomeServico}'? Esta ação não pode ser desfeita.")
            .setPositiveButton("Sim, excluir") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                val fotoUrl = servico.fotoServico
                if (!fotoUrl.isNullOrBlank()) {
                    try {
                        val fotoRef = storage.getReferenceFromUrl(fotoUrl)
                        fotoRef.delete()
                            .addOnSuccessListener { excluirDocumentoFirestore(servico) }
                            .addOnFailureListener { 
                                Toast.makeText(this, "Aviso: Falha ao excluir imagem do Storage. Prosseguindo.", Toast.LENGTH_LONG).show()
                                excluirDocumentoFirestore(servico) 
                            }
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(this, "Erro de URL inválida. Prosseguindo com o documento.", Toast.LENGTH_LONG).show()
                        excluirDocumentoFirestore(servico)
                    }
                } else {
                    excluirDocumentoFirestore(servico)
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun excluirDocumentoFirestore(servico: Servico) {
        servico.id?.let { id ->
            db.collection("servico").document(id).delete()
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Serviço excluído com sucesso.", Toast.LENGTH_SHORT).show()
                    auth.currentUser?.uid?.let { carregarDadosAutonomo(it) }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erro ao excluir o serviço: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Erro: ID do serviço não encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatarTelefone(numero: String?): String {
        if (numero.isNullOrBlank()) {
            return "Telefone não informado"
        }
        var digitos = numero.filter { it.isDigit() }
        if (digitos.startsWith("55") && digitos.length > 11) {
            digitos = digitos.substring(2)
        }
        return when (digitos.length) {
            10 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 6)}-${digitos.substring(6)}"
            11 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 7)}-${digitos.substring(7)}"
            else -> numero
        }
    }
}