package com.jobmatch

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaPesquisaBinding
import com.jobmatch.databinding.LayoutHeaderSearchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.Normalizer

class TelaPesquisa : AppCompatActivity() {

    private lateinit var binding: ActivityTelaPesquisaBinding
    private lateinit var headerBinding: LayoutHeaderSearchBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var servicoAdapter: ServicoAdapter

    private val allDataSource = mutableListOf<Servico>()

    private var searchField = "nomeServico"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaPesquisaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        headerBinding = LayoutHeaderSearchBinding.bind(binding.header.root)
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupSearch()
        loadDataBasedOnFilter()

        headerBinding.backButton.setOnClickListener { finish() }
        headerBinding.filterButton.setOnClickListener { view -> showFilterPopupMenu(view) }
    }

    private fun setupRecyclerView() {
        servicoAdapter = ServicoAdapter(mutableListOf<Servico>(), true)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@TelaPesquisa, 2)
            adapter = servicoAdapter
        }
    }

    private fun setupSearch() {
        headerBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndDisplay(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadDataBasedOnFilter() {
        showLoadingState()
        if (searchField == "nomeAutonomo") {
            loadAllAutonomos()
        } else {
            loadAllServices()
        }
    }

    private fun loadAllServices() {
        firestore.collection("servico").orderBy(searchField).get()
            .addOnSuccessListener { snapshot ->
                hideLoadingState()
                if (snapshot == null || snapshot.isEmpty) {
                    allDataSource.clear()
                    filterAndDisplay("")
                } else {
                    val servicos = snapshot.toObjects(Servico::class.java)
                    allDataSource.clear()
                    allDataSource.addAll(servicos)
                    filterAndDisplay(headerBinding.etSearch.text.toString())
                }
            }
            .addOnFailureListener {
                hideLoadingState()
                showErrorState()
            }
    }

    private fun loadAllAutonomos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usersSnapshot = firestore.collection("users").whereNotEqualTo("autonomo", null).get().await()
                val autonomosAsServices = usersSnapshot.toObjects(Usuario::class.java).map { 
                    Servico(uidAutonomo = it.uid ?: "", nomeAutonomo = it.nome ?: "", fotoServico = it.fotoUrl, nomeServico = "Ver Perfil")
                }
                withContext(Dispatchers.Main) {
                    hideLoadingState()
                    allDataSource.clear()
                    allDataSource.addAll(autonomosAsServices)
                    filterAndDisplay(headerBinding.etSearch.text.toString())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoadingState()
                    showErrorState()
                }
            }
        }
    }

    private fun filterAndDisplay(query: String) {
        val filteredList = if (query.isBlank()) {
            allDataSource
        } else {
            val normalizedQuery = query.normalize()
            allDataSource.filter { servico ->
                val fieldValue = when (searchField) {
                    "nomeServico" -> servico.nomeServico
                    "categoria" -> servico.categoria
                    "nomeAutonomo" -> servico.nomeAutonomo
                    else -> null
                }
                 fieldValue?.let { 
                    it.normalize().contains(normalizedQuery, ignoreCase = true)
                } ?: false
            }
        }

        servicoAdapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            showErrorState()
        } else {
            showResultsState()
        }
    }

    private fun String.normalize(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
    }

    private fun showFilterPopupMenu(view: View) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.search_options_menu, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0f
        }

        val radioGroup = popupView.findViewById<RadioGroup>(R.id.search_options_group)
        when (searchField) {
            "nomeServico" -> radioGroup.check(R.id.search_by_service_name)
            "categoria" -> radioGroup.check(R.id.search_by_category)
            "nomeAutonomo" -> radioGroup.check(R.id.search_by_freelancer_name)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            popupWindow.dismiss()
            val newSearchField = when (checkedId) {
                R.id.search_by_service_name -> "nomeServico"
                R.id.search_by_category -> "categoria"
                R.id.search_by_freelancer_name -> "nomeAutonomo"
                else -> "nomeServico"
            }
            if (searchField != newSearchField) {
                searchField = newSearchField
                headerBinding.etSearch.text?.clear()
                loadDataBasedOnFilter()
            }
        }
        popupWindow.showAsDropDown(view)
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        binding.searchHintText.visibility = View.GONE
    }
    
    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showResultsState() {
        binding.recyclerView.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE
        binding.searchHintText.visibility = View.GONE
    }

    private fun showErrorState() {
        servicoAdapter.updateData(emptyList())
        binding.recyclerView.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
        binding.searchHintText.visibility = View.GONE
    }
}