package com.jobmatch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.storage.FirebaseStorage // Import necessário

class TelaPerfilAutonomo : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaPerfilAutonomoBinding.inflate(layoutInflater)
    }

    private lateinit var storage: FirebaseStorage // Mantendo a declaração
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var servicosAdapter: ServicosAdapter
    private val listaServicos = mutableListOf<Servico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance() // Mantendo a inicialização

        // Configura ações iniciais e placeholders
        binding.imgViewVoltar.setOnClickListener { finish() }
        binding.imgViewPerfilIcon.visibility = View.VISIBLE

        setupRecyclerView()

        // Lógica para determinar qual perfil carregar
        val autonomoId = intent.getStringExtra("AUTONOMO_ID")
        if (!autonomoId.isNullOrBlank()) {
            carregarDadosAutonomo(autonomoId)
            configurarModoVisitante()
        } else {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                carregarDadosAutonomo(userId)
                configurarModoProprietario()
            } else {
                Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun carregarDadosAutonomo(id: String) {
        binding.progressBar.visibility = View.VISIBLE

        // Carrega os dados do usuário (nome, foto, etc.)
        db.collection("users").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        binding.txtViewNomeAutonomo.text = it.nome
                        binding.txtViewTelefone.text = formatarTelefone(it.numeroTelefone)

                        if (!it.fotoUrl.isNullOrEmpty()) {
                            binding.imgViewBGPerfil.load(it.fotoUrl) {
                                crossfade(true)
                            }
                            binding.imgViewPerfilIcon.visibility = View.GONE
                        } else {
                            binding.imgViewBGPerfil.setImageDrawable(null)
                            binding.imgViewPerfilIcon.visibility = View.VISIBLE
                        }
                    }
                }
            }

        // Carrega os serviços daquele autônomo
        db.collection("servico").whereEqualTo("uidUsuario", id).get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                val servicos = documents.toObjects(Servico::class.java)
                servicosAdapter.updateData(servicos)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar os serviços.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarModoVisitante() {
        binding.btnEditarPerfil.visibility = View.GONE
        binding.btnCadastrarServico.visibility = View.GONE
    }

    private fun configurarModoProprietario() {
        binding.btnEditarPerfil.visibility = View.VISIBLE
        binding.btnCadastrarServico.visibility = View.VISIBLE

        binding.btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, TelaEdicaoPerfilAutonomo::class.java))
        }

        binding.btnCadastrarServico.setOnClickListener {
            activityResultLauncher.launch(Intent(this, CadastrarServico::class.java))
        }
    }

    // Launcher para aguardar o resultado da tela de cadastro/edição
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
            // ✅ Usa o NonScrollingGridLayoutManager para resolver o bug de rolagem
            layoutManager = NonScrollingGridLayoutManager(context, 2)
            adapter = servicosAdapter
            // isNestedScrollingEnabled é desnecessário com NonScrollingGridLayoutManager, mas manter
            // a linha é opcional se não houver conflito. Removida para clareza.
        }
    }

    // --- FUNÇÕES DE AÇÃO PARA OS SERVIÇOS ---

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

                // 1. Tenta excluir a imagem do Storage (se existir e a URL for válida)
                if (!fotoUrl.isNullOrBlank()) {
                    try {
                        val fotoRef = storage.getReferenceFromUrl(fotoUrl)

                        fotoRef.delete()
                            .addOnSuccessListener {
                                // Imagem excluída com sucesso, agora exclui o documento
                                excluirDocumentoFirestore(servico)
                            }
                            .addOnFailureListener { e ->
                                // Trata a falha do Storage (a imagem pode não existir)
                                Toast.makeText(this, "Aviso: Falha ao excluir imagem do Storage. Prosseguindo com documento.", Toast.LENGTH_LONG).show()
                                excluirDocumentoFirestore(servico)
                            }

                    } catch (e: IllegalArgumentException) {
                        // Captura o erro se a URL for local (content://)
                        Toast.makeText(this, "Erro de URL inválida. Prosseguindo com o documento.", Toast.LENGTH_LONG).show()
                        excluirDocumentoFirestore(servico)
                    }
                } else {
                    // Se não há foto, vai direto para o Firestore
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
                    // Recarrega a lista para refletir a exclusão
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
            10 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 6)}-${digitos.substring(6)}" // Fixo
            11 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 7)}-${digitos.substring(7)}" // Celular
            else -> numero // Formato inesperado, retorna o original.
        }
    }
}