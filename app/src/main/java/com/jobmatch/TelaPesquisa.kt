package com.jobmatch

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioGroup
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jobmatch.databinding.ActivityTelaPesquisaBinding
import com.jobmatch.databinding.LayoutHeaderSearchBinding
import kotlin.apply
import kotlin.jvm.java
import kotlin.text.isNullOrEmpty

class TelaPesquisa : AppCompatActivity() {

    private lateinit var binding: ActivityTelaPesquisaBinding
    private lateinit var headerBinding: LayoutHeaderSearchBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var servicoAdapter: ServicoAdapter
    private val servicos = mutableListOf<Servico>()
    private var searchField = "nomeServico" // Default search field

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaPesquisaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        headerBinding = LayoutHeaderSearchBinding.bind(binding.header.root)
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        showInitialState()

        headerBinding.backButton.setOnClickListener {
            finish()
        }

        headerBinding.filterButton.setOnClickListener { view ->
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView = inflater.inflate(R.layout.search_options_menu, null)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                popupWindow.elevation = 10.0f
            }

            val radioGroup = popupView.findViewById<RadioGroup>(R.id.search_options_group)
            when (searchField) {
                "nomeServico" -> radioGroup.check(R.id.search_by_service_name)
                "uidServico" -> radioGroup.check(R.id.search_by_category)
                "nomeAutonomo" -> radioGroup.check(R.id.search_by_freelancer_name)
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                searchField = when (checkedId) {
                    R.id.search_by_service_name -> "nomeServico"
                    R.id.search_by_category -> "uidServico"
                    R.id.search_by_freelancer_name -> "nomeAutonomo"
                    else -> "nomeServico"
                }
                popupWindow.dismiss()
            }

            popupWindow.showAsDropDown(view)
        }

        headerBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // We are searching as you type
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    search(newText)
                } else {
                    showInitialState()
                }
                return true
            }
        })
    }

    private fun search(query: String) {
        firestore.collection("servico")
            .orderBy(searchField)
            .whereGreaterThanOrEqualTo(searchField, query)
            .whereLessThanOrEqualTo(searchField, query + '\uf8ff')
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showErrorState()
                } else {
                    val newServicos = documents.toObjects(Servico::class.java)
                    servicos.clear()
                    servicos.addAll(newServicos)
                    servicoAdapter.notifyDataSetChanged()
                    showResultsState()
                }
            }
            .addOnFailureListener {
                showErrorState()
            }
    }

    private fun setupRecyclerView() {
        servicoAdapter = ServicoAdapter(servicos)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@TelaPesquisa, 2)
            adapter = servicoAdapter
        }
    }

    private fun showInitialState() {
        binding.searchHintText.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
    }

    private fun showResultsState() {
        binding.searchHintText.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE
    }

    private fun showErrorState() {
        binding.searchHintText.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
    }
}
