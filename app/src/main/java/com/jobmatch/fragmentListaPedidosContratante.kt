package com.jobmatch

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
import com.google.firebase.firestore.Query
import com.jobmatch.databinding.FragmentListaPedidosContratanteBinding // O nome do seu XML de layout

class fragmentListaPedidosContratante : Fragment() {

    // View Binding para acessar os componentes do XML com segurança
    private var _binding: FragmentListaPedidosContratanteBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // RecyclerView Adapter
    private lateinit var pedidoAdapter: PedidoAdapter

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

        // Decide qual lista carregar com base no tipo
        when (tipoLista) {
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

        binding.btnVoltarListaPedi.setOnClickListener {
            // Fecha a Activity que contém este fragmento
            activity?.finish()
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

    private fun buscarPedidosParaAutonomo() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("pedidos")
            .whereEqualTo("status", "disponivel")
            .orderBy("dataHora", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    binding.tvNoPedidos.text = "Nenhum pedido de serviço disponível no momento."
                    binding.tvNoPedidos.visibility = View.VISIBLE
                } else {
                    val listaPedidos = documents.toObjects(Pedidos::class.java)
                    pedidoAdapter.updateData(listaPedidos)
                    binding.tvNoPedidos.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("ListaPedidos", "Erro ao buscar pedidos para autônomo", e)
                Toast.makeText(context, "Falha ao carregar pedidos.", Toast.LENGTH_SHORT).show()
            }
    }

    // NOVA FUNÇÃO
    private fun buscarPedidosDoAutonomo() {
        val autonomoId = auth.currentUser?.uid
        if (autonomoId == null) {
            Toast.makeText(context, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        db.collection("pedidos")
            .whereEqualTo("autonomo", autonomoId)
            .orderBy("dataHora", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    binding.tvNoPedidos.text = "Você ainda não aceitou nenhum projeto."
                    binding.tvNoPedidos.visibility = View.VISIBLE
                } else {
                    val listaPedidos = documents.toObjects(Pedidos::class.java)
                    pedidoAdapter.updateData(listaPedidos)
                    binding.tvNoPedidos.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("ListaPedidos", "Erro ao buscar projetos aceitos", e)
                Toast.makeText(context, "Falha ao carregar seus projetos.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buscarPedidosDoContratante() {
        val contratanteId = auth.currentUser?.uid
        if (contratanteId == null) {
            Toast.makeText(context, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        db.collection("pedidos")
            .whereEqualTo("contratanteId", contratanteId)
            .orderBy("dataHora", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    binding.tvNoPedidos.text = "Você ainda não realizou nenhum pedido de serviço."
                    binding.tvNoPedidos.visibility = View.VISIBLE
                } else {
                    val listaPedidos = documents.toObjects(Pedidos::class.java)
                    pedidoAdapter.updateData(listaPedidos)
                    binding.tvNoPedidos.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("ListaPedidos", "Erro ao buscar pedidos do contratante", e)
                Toast.makeText(context, "Falha ao carregar seus pedidos.", Toast.LENGTH_SHORT).show()
            }
    }

    // Limpa o binding para evitar vazamentos de memória quando a view do fragmento é destruída
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}