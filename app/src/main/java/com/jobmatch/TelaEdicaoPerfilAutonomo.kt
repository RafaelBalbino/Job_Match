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
import com.jobmatch.databinding.ActivityTelaEdicaoPerfilAutonomoBinding
import kotlin.math.min

class TelaEdicaoPerfilAutonomo : AppCompatActivity() {

    private lateinit var binding: ActivityTelaEdicaoPerfilAutonomoBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var userId: String? = null
    private var fotoSelecionadaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaEdicaoPerfilAutonomoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        userId = auth.currentUser?.uid

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarBotoesETextos()
        carregarDadosUsuario()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            fotoSelecionadaUri = it
            binding.imgPerfilAutonomo.setImageURI(it)
            binding.imgPerfilAutonomo.visibility = View.VISIBLE
        }
    }

    private fun configurarBotoesETextos() {
        binding.btnVoltarPerfilAutonomo.setOnClickListener { finish() }
        binding.btnSalvarAutonomo.setOnClickListener { salvarDados() }
        binding.btnAnexoAutonomo.setOnClickListener { pickImageLauncher.launch("image/*") }
        // Aplica a nova máscara
        binding.txtTelefoneAutonomo.addTextChangedListener(PhoneMaskWatcher())

        // Adiciona o listener de foco para o campo de endereço
        binding.txtEnderecoAutonomo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilEnderecoAutonomo.helperText = "Use o formato: Cidade - Estado ou Cidade, Estado"
            } else {
                binding.tilEnderecoAutonomo.helperText = null
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
                        binding.txtNomeAutonomo.setText(it.nome)
                        // A máscara será aplicada automaticamente pelo TextWatcher
                        binding.txtTelefoneAutonomo.setText(it.numeroTelefone)
                        val enderecoFmt = listOfNotNull(it.cidade, it.estado).filter { it.isNotBlank() }.joinToString(" - ")
                        binding.txtEnderecoAutonomo.setText(enderecoFmt)

                        it.autonomo?.let { autonomo ->
                            binding.txtEspecializacaoAutonomo.setText(autonomo.especializacao)
                            binding.txtCnpjAutonomo.setText(autonomo.cnpj)
                        }

                        val fotoUrl = it.fotoUrl
                        if (!fotoUrl.isNullOrEmpty()) {
                            binding.imgPerfilAutonomo.load(fotoUrl) { crossfade(true) }
                            binding.imgPerfilAutonomo.visibility = View.VISIBLE
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Função ajustada para limpar QUALQUER máscara, retornando apenas os dígitos.
    private fun limparNumeroTelefone(numero: String?): String {
        return numero?.replace(Regex("[^0-9]"), "") ?: ""
    }

    private fun salvarDados() {
        if (userId == null) return

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
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha no upload da imagem: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarDadosFirestore(novaFotoUrl: String?) {
        val nome = binding.txtNomeAutonomo.text.toString().trim()
        // Limpa o número antes de salvar
        val telefone = limparNumeroTelefone(binding.txtTelefoneAutonomo.text.toString())
        val enderecoStr = binding.txtEnderecoAutonomo.text.toString().trim()
        val especializacao = binding.txtEspecializacaoAutonomo.text.toString().trim()
        val cnpj = binding.txtCnpjAutonomo.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "O nome é obrigatório.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validação estrita do endereço
        if (enderecoStr.isNotEmpty() && !enderecoStr.contains("-") && !enderecoStr.contains(",")) {
            Toast.makeText(this, "Formato de endereço inválido. Use 'Cidade - Estado' ou 'Cidade, Estado'.", Toast.LENGTH_LONG).show()
            return
        }

        val atualizacoes = mutableMapOf<String, Any>()
        atualizacoes["nome"] = nome
        // Garante que o número de telefone completo (com DDI) seja salvo, se presente
        if (telefone.isNotEmpty()) atualizacoes["numeroTelefone"] = telefone

        val (cidade, estado) = parseEndereco(enderecoStr)
        atualizacoes["cidade"] = cidade
        atualizacoes["estado"] = estado

        if (especializacao.isNotEmpty()) atualizacoes["autonomo.especializacao"] = especializacao
        if (cnpj.isNotEmpty()) atualizacoes["autonomo.cnpj"] = cnpj

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

    // NOVA MÁSCARA DE TELEFONE COMPLETA
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
            var formatted = "+55"
            if (str.length > 2) {
                formatted += " (${str.substring(2, min(4, str.length))}"
            }
            if (str.length >= 5) {
                formatted += ") ${str.substring(4, min(9, str.length))}"
            }
            if (str.length >= 10) {
                formatted += "-${str.substring(9, min(13, str.length))}"
            }

            s.replace(0, s.length, formatted)

            old = str
            isUpdating = false

            // Garante que o cursor fique no final do texto
            binding.txtTelefoneAutonomo.setSelection(s.length)
        }
    }
}