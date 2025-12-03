package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jobmatch.databinding.ActivityTelaMenuPrincipalBinding

class TelaMenuPrincipal : AppCompatActivity() {

    private lateinit var binding: ActivityTelaMenuPrincipalBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private lateinit var servicoAdapter: ServicoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaMenuPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid

        // Define o placeholder da imagem de perfil
        binding.imgPerfil.setImageResource(R.drawable.ic_profile_placeholder)

        // Ajusta o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        configurarListeners()
        carregarDadosDoCabecalho()
        carregarCategorias()
        carregarESetarServicos(null) // Carrega todos os serviços inicialmente
    }
    
    private fun setupRecyclerView(){
        // A lista de serviços é passada diretamente para o adapter
        servicoAdapter = ServicoAdapter(emptyList())
        binding.rvServicosPrincipal.apply {
            layoutManager = LinearLayoutManager(this@TelaMenuPrincipal, LinearLayoutManager.HORIZONTAL, false)
            adapter = servicoAdapter
        }
    }

    private fun configurarListeners() {
        binding.imgPerfil.setOnClickListener {
            navegarParaEdicaoDePerfil()
        }

        binding.imgNavegacaoMenu.setOnClickListener {
            navegarParaMenuPerfil()
        }

        binding.scViewPesquisa.setOnClickListener {
            val intent = Intent(this, TelaPesquisa::class.java)
            startActivity(intent)
        }

        binding.imgFiltro.setOnClickListener {
            val intent = Intent(this, TelaPesquisa::class.java)
            startActivity(intent)
        }

        binding.tvVerTodos.setOnClickListener {
            val intent = Intent(this, TelaPesquisa::class.java).apply {
                putExtra("SHOW_ALL", true)
            }
            startActivity(intent)
        }
    }

    private fun carregarCategorias() {
        db.collection("servico").get().addOnSuccessListener { documents ->
            val categories = documents.toObjects(Servico::class.java)
                .mapNotNull { it.categoria }.filter { it.isNotBlank() }.distinct()

            if (categories.isNotEmpty()) {
                val spinnerItems = mutableListOf("Todas as categorias")
                spinnerItems.addAll(categories)

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCategoriasPrincipal.adapter = adapter

                binding.spinnerCategoriasPrincipal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedCategory = parent.getItemAtPosition(position).toString()
                        val filter = if (selectedCategory == "Todas as categorias") null else selectedCategory
                        carregarESetarServicos(filter)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
                binding.spinnerCategoriasPrincipal.visibility = View.VISIBLE
            } else {
                binding.spinnerCategoriasPrincipal.visibility = View.GONE
            }
        }
    }

    private fun carregarESetarServicos(categoriaFiltro: String?) {
        var query: Query = db.collection("servico")

        if (categoriaFiltro != null) {
            query = query.whereEqualTo("categoria", categoriaFiltro)
        } else {
            // Ordena por nome quando não há filtro, para consistência
            query = query.orderBy("nomeServico", Query.Direction.ASCENDING)
        }

        query.limit(10).get() // Limite para o RecyclerView
            .addOnSuccessListener { documents ->
                val novosServicos = documents.toObjects(Servico::class.java)
                
                // Atualiza o adapter com a nova lista de serviços
                servicoAdapter = ServicoAdapter(novosServicos)
                binding.rvServicosPrincipal.adapter = servicoAdapter

                if (documents.isEmpty) {
                    Log.d("Firestore", "Nenhum serviço encontrado para o filtro: $categoriaFiltro")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Erro ao buscar serviços com filtro $categoriaFiltro: ", exception)
                Toast.makeText(this, "Erro ao carregar serviços.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navegarParaMenuPerfil() {
        val intent = Intent(this, TelaMenuPerfil::class.java)
        startActivity(intent)
    }

    private fun carregarDadosDoCabecalho() {
        if (userId == null) return

        db.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        val fotoUrl = it.fotoUrl
                        if (!fotoUrl.isNullOrEmpty()) {
                            binding.imgPerfil.load(fotoUrl) {
                                crossfade(true)
                                placeholder(R.drawable.ic_profile_placeholder)
                                error(R.drawable.ic_profile_placeholder)
                                // CORREÇÃO: Adiciona a transformação para círculo
                                transformations(CircleCropTransformation())
                            }
                        } else {
                            binding.imgPerfil.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
            }
    }

    private fun navegarParaEdicaoDePerfil() {
        if (userId == null) {
            Toast.makeText(this, "Usuário não encontrado, faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    if (document.get("autonomo") != null) {
                        val intent = Intent(this, TelaEdicaoPerfilAutonomo::class.java)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, TelaEdicaoPerfilContratante::class.java)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Perfil de usuário não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}