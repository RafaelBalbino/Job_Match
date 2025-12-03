package com.jobmatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jobmatch.databinding.FragmentListaAvaliacoesBinding

class fragmentListaAvaliacoes : Fragment() {

    private var _binding: FragmentListaAvaliacoesBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var avaliacaoAdapter: AvaliacaoAdapter
    private var autonomoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            autonomoId = it.getString(ARG_AUTONOMO_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaAvaliacoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        binding.btnVoltarListaAval.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()

        if (autonomoId == null) {
            showErrorState("ID do autônomo não fornecido.")
        } else {
            buscarAvaliacoes()
        }
    }

    private fun setupRecyclerView() {
        avaliacaoAdapter = AvaliacaoAdapter(emptyList())
        binding.rvAvaliacoesList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = avaliacaoAdapter
        }
    }

    private fun buscarAvaliacoes() {
        showLoadingState()
        db.collection("avaliacoes")
            .whereEqualTo("autonomoId", autonomoId)
            .orderBy("dataHora", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showEmptyState()
                } else {
                    val avaliacoes = documents.toObjects(Avaliacao::class.java)
                    avaliacaoAdapter.updateData(avaliacoes)
                    showResultsState()
                }
            }
            .addOnFailureListener { e ->
                showErrorState("Falha ao carregar avaliações: ${e.message}")
            }
    }

    private fun showLoadingState() {
        // Garante que o cabeçalho esteja visível durante o carregamento
        binding.clHeaderAvaliacao.visibility = View.VISIBLE
        // Oculta a lista e as mensagens enquanto carrega
        binding.tvMensagem.visibility = View.GONE
        binding.rvAvaliacoesList.visibility = View.GONE
    }

    private fun showResultsState() {
        binding.clHeaderAvaliacao.visibility = View.VISIBLE
        binding.rvAvaliacoesList.visibility = View.VISIBLE
        binding.tvMensagem.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.clHeaderAvaliacao.visibility = View.VISIBLE
        binding.rvAvaliacoesList.visibility = View.GONE
        binding.tvMensagem.visibility = View.VISIBLE
        binding.tvMensagem.text = "Este profissional ainda não possui avaliações."
    }

    private fun showErrorState(message: String) {
        if (autonomoId == null) {
            binding.clHeaderAvaliacao.visibility = View.GONE
        } else {
            binding.clHeaderAvaliacao.visibility = View.VISIBLE
        }
        binding.rvAvaliacoesList.visibility = View.GONE
        binding.tvMensagem.visibility = View.VISIBLE
        binding.tvMensagem.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_AUTONOMO_ID = "autonomo_id"

        @JvmStatic
        fun newInstance(autonomoId: String) =
            fragmentListaAvaliacoes().apply {
                arguments = Bundle().apply {
                    putString(ARG_AUTONOMO_ID, autonomoId)
                }
            }
    }
}
