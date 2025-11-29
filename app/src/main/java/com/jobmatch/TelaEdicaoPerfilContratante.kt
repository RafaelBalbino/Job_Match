package com.jobmatch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
import kotlin.math.min

class TelaEdicaoPerfilContratante : AppCompatActivity() {

    private lateinit var binding: ActivityTelaEdicaoPerfilContratanteBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var userId: String? = null
    private var fotoSelecionadaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaEdicaoPerfilContratanteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        userId = auth.currentUser?.uid

        // Ajusta o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarBotoesETextos()
        carregarDadosUsuario()
    }

    // Lançador para buscar imagem da galeria
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
        binding.btnVoltar.setOnClickListener {             val intent = Intent(this, TelaMeuPerfil::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent) 
        }
        binding.btnSalvar.setOnClickListener { salvarDados() }
        binding.btnAnexarImagem.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.txtTelefoneContratante.addTextChangedListener(PhoneMaskWatcher())

        // Adiciona o listener de foco para o campo de endereço
        binding.txtEnderecoContratante.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilEnderecoContratante.helperText = "Use o formato: Cidade - Estado ou Cidade, Estado"
            } else {
                binding.tilEnderecoContratante.helperText = null
            }
        }
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

                        val enderecoFmt = listOfNotNull(it.cidade, it.estado).filter { it.isNotBlank() }.joinToString(" - ")
                        binding.txtEnderecoContratante.setText(enderecoFmt)

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

    private fun limparNumeroTelefone(numero: String?, removerPrefixo: Boolean = false): String {
        var digitos = numero?.replace(Regex("[^0-9]"), "") ?: ""
        if (removerPrefixo && digitos.startsWith("55")) {
            digitos = digitos.substring(2)
        }
        return digitos
    }

    private fun salvarDados() {
        if (userId == null) return
        binding.btnSalvar.isEnabled = false // Desabilita o botão

        if (fotoSelecionadaUri != null) {
            uploadImagemEAtualizarPerfil(fotoSelecionadaUri!!)
        } else {
            atualizarDadosFirestore(null)
        }
    }

    private fun uploadImagemEAtualizarPerfil(uri: Uri) {
        val storageRef = storage.reference.child("profile_images/$userId.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    atualizarDadosFirestore(downloadUrl.toString())
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Falha ao obter URL da imagem: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSalvar.isEnabled = true // Reabilita em caso de falha
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha no upload da imagem: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSalvar.isEnabled = true // Reabilita em caso de falha
            }
    }

    private fun atualizarDadosFirestore(novaFotoUrl: String?) {
        val nome = binding.txtNomeContratante.text.toString().trim()
        val email = binding.txtEmailContratante.text.toString().trim()
        val telefone = limparNumeroTelefone(binding.txtTelefoneContratante.text.toString())
        val enderecoStr = binding.txtEnderecoContratante.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nome e Email são obrigatórios.", Toast.LENGTH_SHORT).show()
            binding.btnSalvar.isEnabled = true // Reabilita em caso de falha
            return
        }

        // Validação estrita do endereço
        if (enderecoStr.isNotEmpty() && !enderecoStr.contains("-") && !enderecoStr.contains(",")) {
            Toast.makeText(this, "Formato de endereço inválido. Use 'Cidade - Estado' ou 'Cidade, Estado'.", Toast.LENGTH_LONG).show()
            binding.btnSalvar.isEnabled = true // Reabilita em caso de falha
            return
        }

        val atualizacoes = mutableMapOf<String, Any>()
        atualizacoes["nome"] = nome
        atualizacoes["email"] = email

        if(telefone.isNotEmpty()) {
            atualizacoes["numeroTelefone"] = "+55$telefone"
        }

        val (cidade, estado) = parseEndereco(enderecoStr)
        atualizacoes["cidade"] = cidade
        atualizacoes["estado"] = estado

        novaFotoUrl?.let {
            atualizacoes["fotoUrl"] = it
        }

        db.collection("users").document(userId!!).update(atualizacoes)
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, TelaMeuPerfil::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar o perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSalvar.isEnabled = true // Reabilita em caso de falha
            }
    }

    private fun parseEndereco(enderecoStr: String): Pair<String, String> {
        val parts = enderecoStr.split("-").map { it.trim() }
        return if (parts.size > 1) {
            Pair(parts[0], parts.drop(1).joinToString("-").trim())
        } else {

            val commaParts = enderecoStr.split(",").map { it.trim() }
            if (commaParts.size > 1) {
                Pair(commaParts[0], commaParts.drop(1).joinToString(",").trim())
            } else {
                Pair(enderecoStr, "")
            }
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
