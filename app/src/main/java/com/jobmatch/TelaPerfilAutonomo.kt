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

class TelaPerfilAutonomo : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaPerfilAutonomoBinding.inflate(layoutInflater)
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var servicosAdapter: ServicosAdapter
    private val listaServicos = mutableListOf<Servico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()

        // Lógica para determinar qual perfil carregar
        val autonomoId = intent.getStringExtra("AUTONOMO_ID")
        if (!autonomoId.isNullOrBlank()) {
            // Modo visitante: Carrega perfil do ID recebido
            carregarDadosAutonomo(autonomoId)
            configurarModoVisitante()
        } else {
            // Modo proprietário: Carrega perfil do usuário logado
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
        // Adicionado para feedback visual
        binding.progressBar.visibility = View.VISIBLE

        // Carrega os dados do usuário (nome, foto, etc.)
        db.collection("users").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        binding.txtViewNomeAutonomo.text = it.nome // Ex: Nome do autônomo
                        binding.txtViewTelefone.text = formatarTelefone(it.numeroTelefone)
                        
                        if (!it.fotoUrl.isNullOrEmpty()) {
                            binding.imgViewBGPerfil.load(it.fotoUrl) { 
                                crossfade(true)
                            }
                            binding.imgViewPerfilIcon.visibility = View.GONE
                        } else {
                            binding.imgViewPerfilIcon.visibility = View.VISIBLE
                        }
                    }
                }
            }

        // Carrega os serviços daquele autônomo
        db.collection("servico").whereEqualTo("uidUsuario", id).get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE // Esconde o progresso
                val servicos = documents.toObjects(Servico::class.java)
                servicosAdapter.updateData(servicos)
            }
            .addOnFailureListener { 
                binding.progressBar.visibility = View.GONE // Esconde o progresso em caso de falha
                Toast.makeText(this, "Erro ao carregar os serviços.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarModoVisitante() {
        binding.btnEditarPerfil.visibility = View.GONE
        binding.btnCadastrarServico.visibility = View.GONE
        // Adicionar botão de "Contratar" ou similar, se necessário
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
            // Se um serviço foi criado ou editado, recarrega a lista para mostrar as mudanças
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
            layoutManager = GridLayoutManager(context, 2)
            adapter = servicosAdapter
            isNestedScrollingEnabled = false
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
                db.collection("servico").document(servico.id).delete()
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
            }
            .setNegativeButton("Não", null)
            .show()
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
