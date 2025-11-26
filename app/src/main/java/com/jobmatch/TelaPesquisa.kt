package com.jobmatch

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.jobmatch.databinding.ActivityTelaPesquisaBinding
import com.jobmatch.databinding.LayoutHeaderSearchBinding

class TelaPesquisa : AppCompatActivity() {

    private lateinit var binding: ActivityTelaPesquisaBinding
    private lateinit var headerBinding: LayoutHeaderSearchBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var servicoAdapter: ServicoAdapter
    private val servicos = mutableListOf<Servico>()
    private var searchField = "nomeServico" // Campo de busca padrão
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaPesquisaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        headerBinding = LayoutHeaderSearchBinding.bind(binding.header.root)
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupSearchView()

        // Verifica se a tela foi aberta para mostrar todos os serviços
        if (intent.getBooleanExtra("SHOW_ALL", false)) {
            fetchAllServices()
        } else {
            showInitialState()
        }

        headerBinding.backButton.setOnClickListener {
            finish()
        }

        headerBinding.filterButton.setOnClickListener { view ->
            showFilterPopupMenu(view)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove o listener para evitar memory leaks
        firestoreListener?.remove()
    }

    private fun setupSearchView() {
        // Configura a barra de pesquisa para ser focável e clicável
        headerBinding.searchView.isFocusable = true
        headerBinding.searchView.isIconified = false
        headerBinding.searchView.requestFocusFromTouch()

        // Encontra o ícone de busca dentro da SearchView
        val searchIcon = headerBinding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)

        headerBinding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    search(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Esconde o ícone de busca se houver texto
                searchIcon.visibility = if (newText.isNullOrEmpty()) View.VISIBLE else View.GONE
                
                if (!newText.isNullOrEmpty()) {
                    search(newText)
                } else {
                    // Se o texto for limpo, volta ao estado inicial ou à lista completa
                    if (intent.getBooleanExtra("SHOW_ALL", false)) {
                        fetchAllServices()
                    } else {
                        showInitialState()
                    }
                }
                return true
            }
        })
    }

    private fun fetchAllServices() {
        // Cancela a busca anterior antes de iniciar uma nova
        firestoreListener?.remove()

        firestoreListener = firestore.collection("servico")
            .orderBy("nomeServico", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    showErrorState()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    showErrorState()
                } else {
                    val newServicos = snapshot.toObjects(Servico::class.java)
                    servicos.clear()
                    servicos.addAll(newServicos)
                    servicoAdapter.notifyDataSetChanged()
                    showResultsState()
                }
            }
    }

    private fun showFilterPopupMenu(view: View) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
            "categoria" -> radioGroup.check(R.id.search_by_category)
            "nomeAutonomo" -> radioGroup.check(R.id.search_by_freelancer_name)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            searchField = when (checkedId) {
                R.id.search_by_service_name -> "nomeServico"
                R.id.search_by_category -> "categoria"
                R.id.search_by_freelancer_name -> "nomeAutonomo"
                else -> "nomeServico"
            }
            popupWindow.dismiss()
            val currentQuery = headerBinding.searchView.query.toString()
            if (currentQuery.isNotEmpty()) {
                search(currentQuery)
            }
        }

        popupWindow.showAsDropDown(view)
    }

    private fun search(query: String) {
        // Cancela a busca anterior antes de iniciar uma nova
        firestoreListener?.remove()

        val formattedQuery = query.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        firestoreListener = firestore.collection("servico")
            .orderBy(searchField)
            .whereGreaterThanOrEqualTo(searchField, formattedQuery)
            .whereLessThanOrEqualTo(searchField, formattedQuery + '\uf8ff')
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    showErrorState()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    showErrorState()
                } else {
                    val newServicos = snapshot.toObjects(Servico::class.java)
                    servicos.clear()
                    servicos.addAll(newServicos)
                    servicoAdapter.notifyDataSetChanged()
                    showResultsState()
                }
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
        firestoreListener?.remove()
        servicos.clear()
        servicoAdapter.notifyDataSetChanged()
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
        servicos.clear()
        servicoAdapter.notifyDataSetChanged()
        binding.searchHintText.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
    }
}