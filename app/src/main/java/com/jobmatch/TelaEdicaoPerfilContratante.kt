package com.jobmatch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jobmatch.databinding.ActivityTelaEdicaoPerfilContratanteBinding
import com.google.firebase.firestore.SetOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TelaEdicaoPerfilContratante : AppCompatActivity() {

    private lateinit var binding: ActivityTelaEdicaoPerfilContratanteBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var userId: String? = null
    private var fotoSelecionadaUri: Uri? = null
    
    // Variáveis para a API do IBGE
    private val ibgeService: IbgeService by lazy {
        Retrofit.Builder()
            .baseUrl("https://servicodados.ibge.gov.br/api/v1/localidades/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IbgeService::class.java)
    }
    private var allMunicipios: List<Municipio>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaEdicaoPerfilContratanteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        userId = auth.currentUser?.uid

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        fetchIbgeData()
        configurarBotoesETextos()
        carregarDadosUsuario()
        setupCityInputWatcher()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            fotoSelecionadaUri = it
            binding.imgPerfilContratante.setImageURI(it)
            binding.imgPerfilContratante.visibility = View.VISIBLE
        }
    }

    private fun configurarBotoesETextos() {
        binding.btnVoltar.setOnClickListener { finish() }
        binding.btnSalvar.setOnClickListener { salvarDados() }
        binding.btnAnexarImagem.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.txtTelefoneContratante.addTextChangedListener(PhoneMaskWatcher())
    }

    private fun carregarDadosUsuario() {
        if (userId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        binding.txtNomeContratante.setText(it.nome)
                        binding.txtEmailContratante.setText(it.email)
                        binding.txtTelefoneContratante.setText(limparNumeroTelefone(it.numeroTelefone, true))

                        carregarEFormatarEndereco(userId!!)

                        val fotoUrl = it.fotoUrl
                        if (!fotoUrl.isNullOrEmpty()) {
                            binding.imgPerfilContratante.load(fotoUrl) { crossfade(true) }
                            binding.imgPerfilContratante.visibility = View.VISIBLE
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun carregarEFormatarEndereco(userId: String) {
        db.collection("enderecos").document(userId).get()
            .addOnSuccessListener { document ->
                val endereco = document.toObject(Endereco::class.java)
                if (endereco != null) {
                    binding.txtCidade.setText(endereco.cidade)
                    binding.actvEstado.setText(endereco.estado, false)
                } else {
                    binding.txtCidade.setText("")
                    binding.actvEstado.setText("", false)
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Falha ao carregar endereço para edição.")
                binding.txtCidade.setText("Erro ao carregar endereço.")
                binding.actvEstado.setText("", false)
            }
    }
    
    private fun limparNumeroTelefone(numero: String?, removerPrefixo: Boolean = false): String {
        var digitos = numero?.replace(Regex("[^0-9]"), "") ?: ""
        if (removerPrefixo && digitos.startsWith("55")) {
            digitos = digitos.substring(2)
        }
        return digitos
    }

    private fun salvarDados() {
        if (userId == null) return
        binding.btnSalvar.isEnabled = false

        if (fotoSelecionadaUri != null) {
            uploadImagemEAtualizarPerfil(fotoSelecionadaUri!!)
        } else {
            atualizarDadosUsuario(null)
        }
    }

    private fun uploadImagemEAtualizarPerfil(uri: Uri) {
        val storageRef = storage.reference.child("profile_images/$userId.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    atualizarDadosUsuario(downloadUrl.toString())
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Falha ao obter URL da imagem: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSalvar.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha no upload da imagem: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSalvar.isEnabled = true
            }
    }

    private fun atualizarDadosUsuario(novaFotoUrl: String?) {
        val userIdFinal = userId ?: return

        val nome = binding.txtNomeContratante.text.toString().trim()
        val email = binding.txtEmailContratante.text.toString().trim()
        val telefone = limparNumeroTelefone(binding.txtTelefoneContratante.text.toString())
        val cidade = binding.txtCidade.text.toString().trim()
        val estado = binding.actvEstado.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || cidade.isEmpty() || estado.isEmpty()) {
            Toast.makeText(this, "Nome, Email, Cidade e Estado são obrigatórios.", Toast.LENGTH_SHORT).show()
            binding.btnSalvar.isEnabled = true
            return
        }

        val atualizacoesUsuario = mutableMapOf<String, Any>()
        atualizacoesUsuario["nome"] = nome
        atualizacoesUsuario["email"] = email
        if (telefone.isNotEmpty()) {
            atualizacoesUsuario["numeroTelefone"] = "+55$telefone"
        }
        novaFotoUrl?.let {
            atualizacoesUsuario["fotoUrl"] = it
        }

        db.collection("users").document(userIdFinal).update(atualizacoesUsuario)
            .addOnSuccessListener {
                Log.d("Firestore", "Dados pessoais (users) atualizados com sucesso.")
                val estadoSigla = estado.substringBefore("(").trim()
                atualizarEnderecoFirestore(userIdFinal, cidade, estadoSigla)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSalvar.isEnabled = true
            }
    }

    private fun atualizarEnderecoFirestore(userId: String, cidade: String, estado: String) {
        val enderecoDocRef = db.collection("enderecos").document(userId)

        val atualizacoesEndereco = mapOf(
            "cidade" to cidade,
            "estado" to estado,
            "uidUsuario" to userId
        )

        enderecoDocRef.set(atualizacoesEndereco, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "Endereço atualizado com sucesso.")
                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar endereço: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSalvar.isEnabled = true
            }
    }
    
    // --- Lógica do IBGE ---
    private fun fetchIbgeData() {
        if (allMunicipios != null) return

        ibgeService.buscarTodosMunicipios().enqueue(object : Callback<List<Municipio>> {
            override fun onResponse(call: Call<List<Municipio>>, response: Response<List<Municipio>>) {
                if (response.isSuccessful) {
                    allMunicipios = response.body()
                    Log.d("IBGE", "Municípios do IBGE carregados: ${allMunicipios?.size}")
                } else {
                    Log.e("IBGE", "Erro ao carregar municípios: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Municipio>>, t: Throwable) {
                Log.e("IBGE", "Falha na requisição IBGE", t)
            }
        })
    }

    private fun setupCityInputWatcher() {
        binding.txtCidade.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val cidadeDigitada = s.toString().trim()
                if (cidadeDigitada.length >= 3 && allMunicipios != null) {
                    buscarEstadosPorCidade(cidadeDigitada)
                } else {
                    binding.actvEstado.setText("", false)
                    binding.actvEstado.setAdapter(null)
                }
            }
        })
    }

    private fun buscarEstadosPorCidade(cidade: String) {
        val municipiosEncontrados = allMunicipios
            ?.filter { it.nome.equals(cidade, ignoreCase = true) }
            ?: emptyList()

        if (municipiosEncontrados.isNotEmpty()) {
            val estadosUnicos = municipiosEncontrados
                .map { val uf = it.microrregiao.mesorregiao.uf; "${uf.sigla} (${uf.nome})" }
                .distinct()

            val adapter = ArrayAdapter(this@TelaEdicaoPerfilContratante, android.R.layout.simple_dropdown_item_1line, estadosUnicos)
            binding.actvEstado.setAdapter(adapter)
            binding.actvEstado.showDropDown()

            if (estadosUnicos.size == 1) {
                binding.actvEstado.setText(estadosUnicos.first(), false)
            }
        } else {
            binding.actvEstado.setText("", false)
            binding.actvEstado.setAdapter(null)
        }
    }

    inner class PhoneMaskWatcher : TextWatcher {
        private var isUpdating = false
        private var old = ""

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            val str = limparNumeroTelefone(s.toString())
            if (isUpdating || str == old) {
                return
            }

            isUpdating = true
            
            val mask = if (str.length > 10) "(##) #####-####" else "(##) ####-####"
            var formatted = ""
            var i = 0
            for (m in mask.toCharArray()) {
                if (i >= str.length) break
                if (m == '#') {
                    formatted += str[i]
                    i++
                } else {
                    formatted += m
                }
            }

            s.replace(0, s.length, formatted)

            old = str
            isUpdating = false

            binding.txtTelefoneContratante.setSelection(s.length)
        }
    }
}