package com.jobmatch

import android.util.Log
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
import com.google.firebase.firestore.SetOptions

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


    private fun carregarEFormatarEndereco(userId: String) {
        // Note que aqui estamos assumindo que o endereço usa o mesmo ID do usuário
        db.collection("enderecos").document(userId).get()
            .addOnSuccessListener { document ->
                val endereco = document.toObject(Endereco::class.java)
                if (endereco != null) {
                    // Formata o endereço a partir do objeto Endereco
                    val enderecoFmt = listOfNotNull(endereco.cidade, endereco.estado)
                        .filter { !it.isNullOrBlank() } // Filtra nulo ou vazio
                        .joinToString(" - ")

                    binding.txtEnderecoContratante.setText(enderecoFmt)
                    // OU binding.txtEnderecoContratante.setText(enderecoFmt) se for o Contratante
                } else {
                    binding.txtEnderecoContratante.setText("")
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Falha ao carregar endereço para edição.")
                // Pode ser útil manter o campo vazio em caso de falha de carregamento
                binding.txtEnderecoContratante.setText("Erro ao carregar endereço.")
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
                    binding.btnSalvar.isEnabled = true // Reabilita em caso de falha
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha no upload da imagem: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSalvar.isEnabled = true // Reabilita em caso de falha
            }
    }

    private fun atualizarEnderecoFirestore(userId: String, enderecoStr: String) {
        val (cidade, estado) = parseEndereco(enderecoStr)

        // O documento de endereço é definido pelo ID do usuário
        val enderecoDocRef = db.collection("enderecos").document(userId)

        val atualizacoesEndereco = mapOf(
            "cidade" to cidade,
            "estado" to estado,
            "uidUsuario" to userId // Garante que a chave de interconexão está presente
            // Outros campos do Endereco (cep, logradouro, bairro) ficam nulos neste caso.
        )

        // Usamos 'set(..., SetOptions.merge())' ou 'set()' para garantir que o documento exista ou seja criado/atualizado
        enderecoDocRef.set(atualizacoesEndereco, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil e Endereço atualizados com sucesso!", Toast.LENGTH_SHORT).show()

                // Navegação final após ambas as atualizações
                val intent = Intent(this, TelaMeuPerfil::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                // Se o endereço falhar, o usuário já foi atualizado (pode ser necessário rollback em um sistema maior)
                Toast.makeText(this, "Erro ao atualizar endereço: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSalvar.isEnabled = true
            }
    }


    private fun atualizarDadosUsuario(novaFotoUrl: String?) {
        val userIdFinal = userId ?: return

        val nome = binding.txtNomeContratante.text.toString().trim()
        val email = binding.txtEmailContratante.text.toString().trim()
        val telefone = limparNumeroTelefone(binding.txtTelefoneContratante.text.toString())
        val enderecoStr = binding.txtEnderecoContratante.text.toString().trim() // Endereço completo

        if (nome.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nome e Email são obrigatórios.", Toast.LENGTH_SHORT).show()
            binding.btnSalvar.isEnabled = true
            return
        }

        // Validação estrita do endereço (mantida)
        if (enderecoStr.isNotEmpty() && !enderecoStr.contains("-") && !enderecoStr.contains(",")) {
            Toast.makeText(this, "Formato de endereço inválido. Use 'Cidade - Estado' ou 'Cidade, Estado'.", Toast.LENGTH_LONG).show()
            binding.btnSalvar.isEnabled = true
            return
        }

        // 1. DADOS PARA A COLEÇÃO 'users'
        val atualizacoesUsuario = mutableMapOf<String, Any>()
        atualizacoesUsuario["nome"] = nome
        atualizacoesUsuario["email"] = email
        if (telefone.isNotEmpty()) {
            atualizacoesUsuario["numeroTelefone"] = "+55$telefone"
        }
        novaFotoUrl?.let {
            atualizacoesUsuario["fotoUrl"] = it
        }

        // 2. ATUALIZA O PERFIL (USUARIO)
        db.collection("users").document(userIdFinal).update(atualizacoesUsuario)
            .addOnSuccessListener {
                Log.d("Firestore", "Dados pessoais (users) atualizados com sucesso.")

                // 3. SE O PERFIL ATUALIZAR, ATUALIZA O ENDEREÇO
                atualizarEnderecoFirestore(userIdFinal, enderecoStr)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar perfil (Dados Pessoais): ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSalvar.isEnabled = true
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
