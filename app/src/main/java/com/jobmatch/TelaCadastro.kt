package com.jobmatch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaCadastroBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET




interface IbgeService{

    @GET("municipios")
    fun buscarTodosMunicipios(): Call<List<Municipio>>
}
class TelaCadastro : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore



    // PASSO FINAL: SUBSTITUA ESTA LINHA PELA SUA URL DO FIREBASE STORAGE
    private val DEFAULT_PROFILE_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/jobmatch-3faec.firebasestorage.app/o/avatar-do-usuario.png?alt=media&token=d1d15194-bf59-4a2b-9df3-75c0a23053d1"

    //variavel para a busca de cidades/estados
    private val ibgeService: IbgeService by lazy{
        Retrofit.Builder()
        .baseUrl("https://servicodados.ibge.gov.br/api/v1/localidades/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(IbgeService::class.java)
    }

    //Cache para armazenar os municipios
    private var allMunicipios: List<Municipio>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        fetchIbgeData()

        setupClickableText()
        setupPasswordFocusListener()
        setupUserTypeSelection()
        setupPrivacyPolicyClick()
        binding.txtTelefone.addTextChangedListener(PhoneMaskWatcher())
        setupCityInputWatcher()

        setupCityInputWatcher()

        binding.btnEnviaCadastro.setOnClickListener {
            cadastrarUsuario()
        }
    }

    private fun fetchIbgeData() {
        if (allMunicipios != null) return

        ibgeService.buscarTodosMunicipios().enqueue(object : Callback<List<Municipio>> {
            override fun onResponse(call: Call<List<Municipio>>, response: Response<List<Municipio>>) {
                if (response.isSuccessful) {
                    allMunicipios = response.body()
                    Log.d("IBGE", "Municípios do IBGE carregados: ${allMunicipios?.size}")
                    // Tenta acionar a busca se o usuário já digitou algo
                    binding.txtCidade.text?.let {
                        if (it.isNotEmpty()) setupCityInputWatcher()
                    }
                } else {
                    Log.e("IBGE", "Erro ao carregar municípios: ${response.code()}")
                    Toast.makeText(this@TelaCadastro, "Erro ao carregar dados de localização. Tente novamente.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<Municipio>>, t: Throwable) {
                Log.e("IBGE", "Falha na requisição IBGE", t)
                Toast.makeText(this@TelaCadastro, "Falha de rede ao carregar localizações.", Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun limparNumeroTelefone(numero: String?): String {
        return numero?.replace(Regex("[^0-9]"), "") ?: ""
    }

    private fun setupPrivacyPolicyClick() {
        val cbPoliticas = binding.cbPoliticas
        val fullText = cbPoliticas.text.toString()
        val clickableText = "Políticas de Privacidade"
        val spannableString = SpannableString(fullText)
        val linkColor = MaterialColors.getColor(this, MaterialR.attr.colorSecondary, Color.BLACK)

        val start = fullText.indexOf(clickableText)
        if (start == -1) return

        val end = start + clickableText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                AlertDialog.Builder(this@TelaCadastro)
                    .setTitle("Políticas de Privacidade")
                    .setMessage("Nossa política de privacidade segue as diretrizes da LGPD, como o Princípio da Finalidade e o Princípio da Necessidade, garantindo a proteção e o uso consciente dos seus dados. Ao se cadastrar, você concorda com a coleta e o tratamento de suas informações para os fins descritos em nossos termos.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = linkColor
                ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        cbPoliticas.text = spannableString
        cbPoliticas.movementMethod = LinkMovementMethod.getInstance()
        cbPoliticas.highlightColor = Color.TRANSPARENT
    }

    private fun setupUserTypeSelection() {
        binding.rgUserType.setOnCheckedChangeListener { _, checkedId ->
            val isFreelancer = checkedId == R.id.rb_freelancer
            toggleFreelancerFields(isFreelancer)
        }
    }

    private fun toggleFreelancerFields(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.tilCnpj.visibility = visibility
        binding.tilSpecialization.visibility = visibility
        // A visibilidade do ll_location não é mais controlada pelo RadioButton
    }

    private fun setupStateDropdown() {
        val states = resources.getStringArray(R.array.brazilian_states)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, states)
        binding.actvEstado.setAdapter(adapter)

        // Listener para quando um estado for selecionado
        binding.actvEstado.setOnItemClickListener { parent, view, position, id ->
            // Limpa o campo de cidade sempre que um novo estado for selecionado
            binding.txtCidade.setText("")
        }
    }

    private fun setupClickableText() {
        val textView = binding.textView21
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)
        val linkColor = MaterialColors.getColor(this, MaterialR.attr.colorSecondary, Color.BLACK)

        val start = fullText.indexOf("Inicie")
        if (start == -1) return
        val end = fullText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@TelaCadastro, TelaLogin::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = linkColor
                ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }

    private fun setupPasswordFocusListener() {
        binding.txtSenha.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvPasswordRestrictions.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).setDuration(300).setListener(null)
                }
            } else {
                binding.tvPasswordRestrictions.animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.tvPasswordRestrictions.visibility = View.GONE
                    }
                })
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnEnviaCadastro.text = ""
            binding.cadastroProgressBar.visibility = View.VISIBLE
            binding.btnEnviaCadastro.isEnabled = false
        } else {
            binding.btnEnviaCadastro.text = getString(R.string.register_button_text)
            binding.cadastroProgressBar.visibility = View.GONE
            binding.btnEnviaCadastro.isEnabled = true
        }
    }

    private fun validarSenha(senha: String): String? {
        val erros = mutableListOf<String>()

        if (senha.length < 8) {
            erros.add("mínimo 8 caracteres")
        }
        if (!senha.any { it.isUpperCase() }) {
            erros.add("uma letra maiúscula")
        }
        if (!senha.any { it.isLowerCase() }) {
            erros.add("uma letra minúscula")
        }
        if (!senha.any { it.isDigit() }) {
            erros.add("um número")
        }
        if (!senha.any { !it.isLetterOrDigit() }) {
            erros.add("um caractere especial")
        }

        return if (erros.isEmpty()) null else "A senha deve conter: ${erros.joinToString(", ")}."
    }

    private fun setupCityInputWatcher() {
        binding.txtCidade.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val cidadeDigitada = s.toString().trim()
                // Busca no cache quando o usuário digita pelo menos 3 caracteres
                if (cidadeDigitada.length >= 3 && allMunicipios != null) {
                    buscarEstadosPorCidade(cidadeDigitada)
                } else {
                    // Limpa o dropdown do estado se o texto for muito curto
                    binding.actvEstado.setText("", false)
                    binding.actvEstado.setAdapter(null)
                }
            }
        })
    }


    //Função pra chamar Estados pela cidade
    private fun buscarEstadosPorCidade(cidade: String) {
        // Encontra todos os municípios com o nome digitado (ignora case)
        val municipiosEncontrados = allMunicipios
            ?.filter { it.nome.equals(cidade, ignoreCase = true) }
            ?: emptyList()

        if (municipiosEncontrados.isNotEmpty()) {
            // Mapeia para uma lista de strings no formato "Sigla (Nome do Estado)"
            val estadosUnicos = municipiosEncontrados
                .map {
                    val uf = it.microrregiao.mesorregiao.uf
                    "${uf.sigla} (${uf.nome})"
                }
                .distinct() // Remove duplicatas de estados

            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estadosUnicos)

            // Define o adaptador e mostra a lista
            binding.actvEstado.setAdapter(adapter)
            binding.actvEstado.showDropDown()

            // Se for apenas um, preenche automaticamente
            if (estadosUnicos.size == 1) {
                binding.actvEstado.setText(estadosUnicos.first(), false)
            } else {
                Toast.makeText(this, "Selecione o Estado. Múltiplos resultados encontrados.", Toast.LENGTH_LONG).show()
            }

        } else if (allMunicipios != null) {
            binding.actvEstado.setText("", false)
            binding.actvEstado.setAdapter(null)
            // Opcional: binding.tilCidade.error = "Cidade não encontrada"
        }
    }
    private fun cadastrarUsuario() {
        // Validação dos campos...
        binding.tilNome.error = null
        binding.tilEmail.error = null
        binding.tilTelefone.error = null
        binding.tilCidade.error = null
        binding.tilSenha.error = null
        binding.tilConfirmarSenha.error = null
        binding.tilSpecialization.error = null
        binding.tilEstado.error = null

        val nome = binding.txtNome.text.toString().trim()
        val email = binding.txtEmail.text.toString().trim()
        val telefoneLimpo = limparNumeroTelefone(binding.txtTelefone.text.toString())
        val cidade = binding.txtCidade.text.toString().trim()
        val estado = binding.actvEstado.text.toString().trim()
        val senha = binding.txtSenha.text.toString()
        val confirmarSenha = binding.txtConfirmarSenha.text.toString()
        val politicasAceitas = binding.cbPoliticas.isChecked
        val isFreelancer = binding.rbFreelancer.isChecked

        val estadoSigla = estado.substringBefore("(").trim().substringAfterLast(" ")

        if (cidade.isEmpty() || estadoSigla.isEmpty()) {
            Toast.makeText(this, "Cidade e Estado são obrigatórios.", Toast.LENGTH_SHORT).show()
            if (cidade.isEmpty()) binding.tilCidade.error = "Obrigatório"
            if (estadoSigla.isEmpty()) binding.tilEstado.error = "Selecione o estado na lista"
            return
        }

        if (nome.isEmpty() || email.isEmpty() || telefoneLimpo.length < 10 || cidade.isEmpty() || estado.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            if (cidade.isEmpty()) binding.tilCidade.error = "Obrigatório"
            return
        }

        val erroSenha = validarSenha(senha)
        if (erroSenha != null) {
            binding.tilSenha.error = erroSenha
            return
        }

        if (senha != confirmarSenha) {
            binding.tilConfirmarSenha.error = "As senhas não coincidem"
            return
        }

        var autonomo: Autonomo? = null
        if(isFreelancer) {
            val especializacao = binding.txtSpecialization.text.toString().trim()
            val cnpj = binding.txtCnpj.text.toString().trim()
            if (especializacao.isEmpty()){
                binding.tilSpecialization.error = "Especialização é obrigatória para autônomo"
                return
            }
            autonomo = Autonomo(cnpj = cnpj.ifEmpty { null }, especializacao = especializacao)
        }

        if (!politicasAceitas) {
            Toast.makeText(this, "Você deve aceitar as políticas de privacidade.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val contratante = Contratante()
                    val telefoneFormatado = "+55$telefoneLimpo"
                    salvarDadosUsuario(nome, email, telefoneFormatado, cidade, estadoSigla, contratante, autonomo)
                } else {
                    showLoading(false)
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso por outra conta."
                        is FirebaseAuthWeakPasswordException -> "A senha não atende aos critérios de segurança do Firebase."
                        else -> "Falha no cadastro: ${exception?.message}"
                    }
                    if (exception is FirebaseAuthUserCollisionException) {
                        binding.tilEmail.error = errorMessage
                    } else if (exception is FirebaseAuthWeakPasswordException) {
                        binding.tilSenha.error = errorMessage
                    } else {
                        Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun salvarDadosUsuario(nome: String, email: String, telefone: String, cidade: String, estadoSigla: String, contratante: Contratante, autonomo: Autonomo?) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            Toast.makeText(baseContext, "Erro ao obter ID do usuário.", Toast.LENGTH_SHORT).show()
            return
        }

        // Monta o objeto Usuario, incluindo o status de bloqueio inicial
        val novoUsuario = Usuario(
            uid = userId,
            nome = nome,
            email = email,
            numeroTelefone = telefone,
            fotoUrl = DEFAULT_PROFILE_IMAGE_URL,
            contratante = contratante,
            autonomo = autonomo,
            isBlocked = false // Garante que o usuário seja criado como não bloqueado
        )

        db.collection("users").document(userId)
            .set(novoUsuario)
            .addOnSuccessListener {
                Log.d("Firestore", "Usuário salvo sem endereço. ID: $userId")

                // --- PASSO 2: Salvar o Endereço (Interconectado) ---
                val novoEndereco = Endereco(
                    uidUsuario = userId, // Chave de interconexão
                    cidade = cidade,
                    estado = estadoSigla,
                    // Deixamos CEP, logradouro e bairro como null, pois o cadastro não coleta esses dados
                )

                db.collection("enderecos").document(userId) // Usando o UID como ID do documento de endereço
                    .set(novoEndereco)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Endereço salvo na coleção 'enderecos' e interconectado.")
                        showLoading(false)
                        val intent = Intent(this, TelaMenuPrincipal::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // Se falhar ao salvar endereço, tentamos reverter o usuário no Auth
                        Log.w("Firestore", "Erro ao salvar endereço. Deletando Auth user.", e)
                        auth.currentUser?.delete()
                        showLoading(false)
                        Toast.makeText(baseContext, "Falha ao salvar endereço.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                // Se falhar ao salvar usuário, deletamos o Auth user
                showLoading(false)
                Log.w("Firestore", "Erro ao salvar usuário", e)
                auth.currentUser?.delete()
                Toast.makeText(baseContext, "Falha ao salvar dados do perfil.", Toast.LENGTH_SHORT).show()
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

            binding.txtTelefone.setSelection(s.length)
        }
    }
}