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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.jobmatch.databinding.FragmentListaPedidosAutonomoBinding

class fragmentListaPedidosAutonomo : Fragment() {

    private var _binding: FragmentListaPedidosAutonomoBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var pedidoAdapter: PedidoAdapter
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaPedidosAutonomoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val tipoQuery = arguments?.getString(ARG_TIPO_QUERY) ?: "BUSCA"

        setupRecyclerView(tipoQuery)

        val query = when (tipoQuery) {
            "PROJETOS" -> {
                binding.tvTituloPedidosAutonomo.text = "Meus Projetos"
                buscarPedidosDoAutonomo()
            }
            else -> { // "BUSCA"
                binding.tvTituloPedidosAutonomo.text = "Buscar Pedidos"
                buscarPedidosParaAutonomo()
            }
        }

        attachPedidosListener(query)

        binding.btnVoltarListaPediAutonomo.setOnClickListener {
            activity?.finish()
        }
    }

    private fun setupRecyclerView(tipoLista: String) {
        val userType = if(tipoLista == "PROJETOS") "AUTONOMO_ACEITOS" else "AUTONOMO"
        pedidoAdapter = PedidoAdapter(mutableListOf(), userType)
        binding.rvListaPedidosAutonomo.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pedidoAdapter
        }
    }

    private fun buscarPedidosParaAutonomo(): Query {
        return db.collection("pedido").whereEqualTo("status", "pendente")
            .orderBy("dataHora", Query.Direction.DESCENDING)
    }

    private fun buscarPedidosDoAutonomo(): Query {
        val autonomoId = auth.currentUser?.uid ?: return db.collection("__non_existent__")
        return db.collection("pedido").whereEqualTo("autonomo", autonomoId)
            .orderBy("dataHora", Query.Direction.DESCENDING)
    }

    private fun attachPedidosListener(query: Query) {
        binding.progressBarAutonomo.visibility = View.VISIBLE
        firestoreListener = query.addSnapshotListener { snapshots, e ->
            binding.progressBarAutonomo.visibility = View.GONE

            if (e != null) {
                Log.e("ListaPedidosAutonomo", "Erro ao ouvir por atualizações. VERIFIQUE O ÍNDICE NO FIREBASE", e)
                Toast.makeText(context, "Falha ao carregar lista. Verifique o Logcat para o link do índice.", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val listaPedidos = snapshots.toObjects(Pedidos::class.java)
                binding.rvListaPedidosAutonomo.visibility = View.VISIBLE
                binding.tvNoPedidosAutonomo.visibility = View.GONE
                pedidoAdapter.updateData(listaPedidos)
            } else {
                binding.rvListaPedidosAutonomo.visibility = View.GONE
                binding.tvNoPedidosAutonomo.text = getEmptyListMessage()
                binding.tvNoPedidosAutonomo.visibility = View.VISIBLE
            }
        }
    }

    private fun getEmptyListMessage(): String {
        return when (arguments?.getString(ARG_TIPO_QUERY)) {
            "PROJETOS" -> "Você ainda não aceitou nenhum projeto."
            else -> "Nenhum pedido de serviço disponível no momento."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }

    companion object {
        private const val ARG_TIPO_QUERY = "TIPO_QUERY"

        @JvmStatic
        fun newInstance(tipoQuery: String) =
            fragmentListaPedidosAutonomo().apply {
                arguments = Bundle().apply {
                    putString(ARG_TIPO_QUERY, tipoQuery)
                }
            }
    }
}