package com.jobmatch

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
        setupSearch()

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

    private fun setupSearch() {
        // Listener para o texto digitado no novo TextInputEditText
        headerBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newText = s.toString()
                if (newText.isNotEmpty()) {
                    search(newText)
                } else {
                    // Se o texto for limpo, volta ao estado inicial ou à lista completa
                    if (intent.getBooleanExtra("SHOW_ALL", false)) {
                        fetchAllServices()
                    } else {
                        showInitialState()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Opcional: Executa a busca ao pressionar o botão de "pesquisar" no teclado
        headerBinding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(headerBinding.etSearch.text.toString())
                true
            } else {
                false
            }
        }
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
            val currentQuery = headerBinding.etSearch.text.toString()
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