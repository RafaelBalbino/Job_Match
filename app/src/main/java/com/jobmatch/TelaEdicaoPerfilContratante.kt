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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.jobmatch.databinding.ActivityTelaEdicaoPerfilContratanteBinding
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

    private val ibgeService: IbgeService by lazy {
        Retrofit.Builder()
            .baseUrl("https://servicodados.ibge.gov.br/api/v1/localidades/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IbgeService::class.java)
    }

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

        setupStateDropdown()
        configurarBotoesETextos()
        carregarDadosUsuario()
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
                    binding.actvEstado.setText(endereco.estado, false)
                    fetchCidadesPorEstado(endereco.estado ?: "") { 
                        binding.txtCidade.setText(endereco.cidade)
                    }
                } else {
                    binding.actvEstado.setText("", false)
                    binding.txtCidade.setText("")
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Falha ao carregar endereço para edição.")
            }
    }

    private fun limparNumeroTelefone(numero: String?, removerPrefixo: Boolean = false): String {
        var digitos = numero?.replace(Regex("[^0-9]"), "") ?: ""
        if (removerPrefixo && digitos.startsWith("55")) {
            digitos = digitos.substring(2)
        }
        return digitos
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSalvar.text = ""
            binding.progressBarSalvar.visibility = View.VISIBLE
            binding.btnSalvar.isEnabled = false
        } else {
            binding.btnSalvar.text = getString(R.string.botao_salvar)
            binding.progressBarSalvar.visibility = View.GONE
            binding.btnSalvar.isEnabled = true
        }
    }

    private fun salvarDados() {
        if (userId == null) return
        showLoading(true)

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
                    showLoading(false)
                    Toast.makeText(this, "Falha ao obter URL da imagem: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Falha no upload da imagem: ${e.message}", Toast.LENGTH_LONG).show()
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
            showLoading(false)
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
                atualizarEnderecoFirestore(userIdFinal, cidade, estado)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erro ao atualizar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun atualizarEnderecoFirestore(userId: String, cidade: String, estado: String) {
        val enderecoDocRef = db.collection("enderecos").document(userId)
        val atualizacoesEndereco = mapOf("cidade" to cidade, "estado" to estado, "uidUsuario" to userId)

        enderecoDocRef.set(atualizacoesEndereco, SetOptions.merge())
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erro ao atualizar endereço: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupStateDropdown() {
        val states = resources.getStringArray(R.array.brazilian_states)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, states)
        binding.actvEstado.setAdapter(adapter)

        binding.tilCidade.isEnabled = false

        binding.actvEstado.setOnItemClickListener { parent, view, position, id ->
            val selectedState = parent.getItemAtPosition(position).toString()
            binding.txtCidade.setText("")
            fetchCidadesPorEstado(selectedState)
        }

        binding.actvEstado.setOnClickListener {
            binding.actvEstado.showDropDown()
        }
    }

    private fun fetchCidadesPorEstado(uf: String, onComplete: (() -> Unit)? = null) {
        binding.tilCidade.isEnabled = false

        ibgeService.buscarCidadesPorEstado(uf).enqueue(object : Callback<List<Municipio>> {
            override fun onResponse(call: Call<List<Municipio>>, response: Response<List<Municipio>>) {
                if (response.isSuccessful) {
                    val cidades = response.body()?.map { it.nome } ?: emptyList()
                    val cityAdapter = ArrayAdapter(this@TelaEdicaoPerfilContratante, android.R.layout.simple_dropdown_item_1line, cidades)
                    binding.txtCidade.setAdapter(cityAdapter)
                    binding.tilCidade.isEnabled = true
                    onComplete?.invoke()
                } else {
                    Toast.makeText(this@TelaEdicaoPerfilContratante, "Erro ao carregar cidades.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Municipio>>, t: Throwable) {
                Toast.makeText(this@TelaEdicaoPerfilContratante, "Falha de rede ao carregar cidades.", Toast.LENGTH_SHORT).show()
            }
        })
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