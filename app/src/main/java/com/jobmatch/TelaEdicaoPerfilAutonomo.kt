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
        binding.txtTelefoneAutonomo.addTextChangedListener(PhoneMaskWatcher())
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

                        val numeroLimpo = limparNumeroTelefone(it.numeroTelefone)
                        binding.txtTelefoneAutonomo.setText(numeroLimpo)

                        // --- CORREÇÃO APLICADA AQUI ---
                        // Preenche o campo de endereço com os dados do Firestore
                        binding.txtEnderecoAutonomo.setText(formatarEnderecoParaEdicao(it.endereco))

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

    /**
     * Formata o objeto Endereco em uma única String para exibição no EditText.
     */
    private fun formatarEnderecoParaEdicao(endereco: Endereco?): String {
        if (endereco == null) return ""
        // Concatena os campos do endereço que não são nulos para formar uma string única
        return listOfNotNull(endereco.rua, endereco.cidade, endereco.estado, endereco.cep)
            .joinToString(separator = ", ")
    }

    private fun limparNumeroTelefone(numero: String?): String {
        if (numero.isNullOrBlank()) {
            return ""
        }
        var digitos = numero.filter { it.isDigit() }
        if (digitos.startsWith("55") && digitos.length > 11) {
            digitos = digitos.substring(2)
        }
        return digitos
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
        val telefone = limparNumeroTelefone(binding.txtTelefoneAutonomo.text.toString())
        val enderecoStr = binding.txtEnderecoAutonomo.text.toString().trim() // Pega a string do EditText
        val especializacao = binding.txtEspecializacaoAutonomo.text.toString().trim()
        val cnpj = binding.txtCnpjAutonomo.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "O nome é obrigatório.", Toast.LENGTH_SHORT).show()
            return
        }

        val atualizacoes = mutableMapOf<String, Any>()
        atualizacoes["nome"] = nome
        if (telefone.isNotEmpty()) atualizacoes["numeroTelefone"] = telefone

        // --- CORREÇÃO APLICADA AQUI ---
        // Assume-se, por simplicidade, que o texto completo vai para o campo "rua".
        // O ideal seria ter campos separados para cada parte do endereço.
        if (enderecoStr.isNotEmpty()) {
            val enderecoObj = Endereco(rua = enderecoStr, cidade = null, estado = null, cep = null, pais = null)
            atualizacoes["endereco"] = enderecoObj
        }

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

    // Máscara para o campo de telefone
    inner class PhoneMaskWatcher : TextWatcher {
        private var isUpdating = false
        private var oldText = ""

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val str = s.toString().filter { it.isDigit() }
            if (isUpdating || str == oldText) {
                return
            }

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

            isUpdating = true
            oldText = str
            binding.txtTelefoneAutonomo.setText(formatted)
            binding.txtTelefoneAutonomo.setSelection(formatted.length)
            isUpdating = false
        }

        override fun afterTextChanged(s: Editable) {}
    }
}
