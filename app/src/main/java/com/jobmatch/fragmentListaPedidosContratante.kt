package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.jobmatch.databinding.FragmentListaPedidosContratanteBinding

class fragmentListaPedidosContratante : Fragment() {

    // View Binding para acessar os componentes do XML com segurança
    private var _binding: FragmentListaPedidosContratanteBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // RecyclerView Adapter
    private lateinit var pedidoAdapter: PedidoAdapter

    // Listener do Firestore para atualizações em tempo real
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla o layout e configura o binding
        _binding = FragmentListaPedidosContratanteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa o Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Configura o RecyclerView
        setupRecyclerView()

        // Pega o tipo de lista que foi passado pela Activity
        val tipoLista = arguments?.getString("FRAGMENT_TYPE")

        // Decide qual query do Firestore usar com base no tipo de lista
        val query = when (tipoLista) {
            "AUTONOMO" -> {
                binding.tvTituloPedidos.text = "Pedidos Disponíveis"
                buscarPedidosParaAutonomo()
            }
            "AUTONOMO_ACEITOS" -> {
                binding.tvTituloPedidos.text = "Meus Projetos Aceitos"
                buscarPedidosDoAutonomo()
            }
            else -> { // "CONTRATANTE"
                binding.tvTituloPedidos.text = "Meus Pedidos Realizados"
                buscarPedidosDoContratante()
            }
        }

        // Anexa o listener à query para receber atualizações em tempo real
        attachPedidosListener(query)

        // Configura o botão de voltar para sempre levar ao menu principal
        binding.btnVoltarListaPedi.setOnClickListener {
            val intent = Intent(activity, TelaMenuPrincipal::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        // Inicializa o Adapter. Ele começa com uma lista vazia.
        val tipoLista = arguments?.getString("FRAGMENT_TYPE") ?: "CONTRATANTE"
        pedidoAdapter = PedidoAdapter(mutableListOf(), tipoLista)

        binding.rvListaPedidos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pedidoAdapter
        }
    }

    // Retorna a query para buscar pedidos disponíveis para autônomos
    private fun buscarPedidosParaAutonomo(): Query {
        return db.collection("pedido")
            .whereEqualTo("status", "disponivel")
            .orderBy("dataHora", Query.Direction.DESCENDING)
    }

    // Retorna a query para buscar projetos aceitos por um autônomo
    private fun buscarPedidosDoAutonomo(): Query {
        val autonomoId = auth.currentUser?.uid ?: return db.collection("__non_existent__") // Retorna uma query vazia se o usuário não estiver logado
        return db.collection("pedido")
            .whereEqualTo("autonomo", autonomoId)
            .orderBy("dataHora", Query.Direction.DESCENDING)
    }

    // Retorna a query para buscar pedidos criados por um contratante
    private fun buscarPedidosDoContratante(): Query {
        val contratanteId = auth.currentUser?.uid ?: return db.collection("__non_existent__") // Retorna uma query vazia se o usuário não estiver logado
        return db.collection("pedido")
            .whereEqualTo("contratanteId", contratanteId)
            .orderBy("dataHora", Query.Direction.DESCENDING)
    }

    /**
     * Anexa um listener em tempo real (SnapshotListener) a uma query do Firestore.
     * Ele atualiza a UI com os dados, trata o estado de carregamento e exibe mensagens
     * de lista vazia ou de erro.
     */
    private fun attachPedidosListener(query: Query) {
        binding.progressBar.visibility = View.VISIBLE
        firestoreListener = query.addSnapshotListener { snapshots, e ->
            binding.progressBar.visibility = View.GONE

            if (e != null) {
                Log.e("ListaPedidos", "Erro ao ouvir por atualizações", e)
                Toast.makeText(context, "Falha ao carregar lista. Verifique o índice do Firestore.", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val listaPedidos = snapshots.toObjects(Pedidos::class.java)
                pedidoAdapter.updateData(listaPedidos)
                binding.tvNoPedidos.visibility = View.GONE
            } else {
                pedidoAdapter.updateData(emptyList()) // Limpa a lista
                binding.tvNoPedidos.text = getEmptyListMessage()
                binding.tvNoPedidos.visibility = View.VISIBLE
            }
        }
    }

    // Retorna a mensagem apropriada para quando a lista de pedidos está vazia
    private fun getEmptyListMessage(): String {
        return when (arguments?.getString("FRAGMENT_TYPE")) {
            "AUTONOMO" -> "Nenhum pedido de serviço disponível no momento."
            "AUTONOMO_ACEITOS" -> "Você ainda não aceitou nenhum projeto."
            else -> "Você ainda não realizou nenhum pedido de serviço."
        }
    }

    // Limpa o listener e o binding para evitar vazamentos de memória
    override fun onDestroyView() {
        super.onDestroyView()
        // Remove o listener do Firestore para evitar cobranças e memory leaks
        firestoreListener?.remove()
        _binding = null
    }
}