package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaMeuPerfilBinding

class TelaMeuPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityTelaMeuPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaMeuPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        carregarDadosDoFirestore()
    }

    private fun carregarDadosDoFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    if (usuario != null) {
                        exibirDadosNaTela(usuario)
                    } else {
                        Toast.makeText(this, "Erro ao processar dados do usuário.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Perfil de usuário não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TelaMeuPerfil", "Erro ao buscar dados do Firestore", exception)
                Toast.makeText(this, "Falha ao carregar o perfil.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Preenche a tela com os dados do objeto Usuario.
     */
    /**
     * Preenche a tela com os dados do objeto Usuario.
     */
    private fun exibirDadosNaTela(usuario: Usuario) {
        // 1. Preenche o cabeçalho
        binding.imgFotoMeuPerfil.load(usuario.fotoUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
            transformations(CircleCropTransformation())
        }
        binding.txtNomeMeuPerfil.text = usuario.nome
        binding.txtEmailMeuPerfil.text = usuario.email

        // 2. Preenche as informações pessoais (SEÇÃO AJUSTADA)
        // Popula o novo campo de email na seção de informações
        binding.txtEmailInfo.text = usuario.email ?: "E-mail não informado"
        binding.txtTelefoneMeuPerfil.text = formatarTelefone(usuario.numeroTelefone)

        // --- ALTERAÇÃO AQUI ---
        // Descomente a linha abaixo para exibir o endereço formatado
        binding.txtEnderecoInfo.text = formatarEndereco(usuario.endereco)

        // 3. Lógica para exibir o bloco de perfil correto
        if (usuario.autonomo != null) {
            // Se for Autônomo
            binding.blocoAutonomoContratante.visibility = View.VISIBLE
            // Renomeei o ID no XML para 'lb_perfil_autonomo_contratante' para ser mais claro
            binding.lbPerfilAutonomoContratante.text = "Perfil Autônomo"
            binding.txtEspecializacao.visibility = View.VISIBLE
            // Assumindo que você tenha um txtFormacao no XML
            // binding.txtFormacao.visibility = View.VISIBLE
            binding.txtEspecializacao.text = usuario.autonomo.especializacao ?: "Não informado"
            // binding.txtFormacao.text = usuario.autonomo.formacao ?: "Não informado"

            // Trata o caso de CNPJ
            if (usuario.autonomo.cnpj.isNullOrBlank()) {
                binding.txtCnpj.visibility = View.GONE
            } else {
                binding.txtCnpj.visibility = View.VISIBLE
                binding.txtCnpj.text = "CNPJ: ${usuario.autonomo.cnpj}"
            }
        } else if (usuario.contratante != null) {
            // Se for Contratante
            binding.blocoAutonomoContratante.visibility = View.VISIBLE
            binding.lbPerfilAutonomoContratante.text = "Perfil Contratante"
            // Esconde os campos específicos de autônomo
            binding.txtEspecializacao.visibility = View.GONE
            // binding.txtFormacao.visibility = View.GONE
            binding.txtCnpj.visibility = View.GONE
        } else {
            // Se não tiver nenhum perfil específico
            binding.blocoAutonomoContratante.visibility = View.GONE
        }

        // 4. Configura os cliques dos botões
        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.itemAlterarSenha.setOnClickListener {
            usuario.email?.let { email ->
                enviarEmailRedefinicaoSenha(email)
            }
        }

        binding.itemSair.setOnClickListener {
            fazerLogout()
        }


        binding.btnEditarPerfil.setOnClickListener {
            navegarParaEdicaoDePerfil(usuario)
        }

    }


    /**
     * Adiciona uma máscara (##) #####-#### a um número de telefone.
     * AGORA CORRIGIDO para lidar com o código do país (55).
     */
    private fun formatarTelefone(numero: String?): String {
        if (numero.isNullOrBlank()) {
            return "Telefone não informado"
        }

        // 1. Remove caracteres não numéricos para garantir que temos apenas dígitos.
        var digitos = numero.filter { it.isDigit() }

        // 2. CORREÇÃO PRINCIPAL: Verifica e remove o código do país "55" se ele estiver no início
        //    e se o número tiver mais de 11 dígitos (55 + DDD + Numero).
        if (digitos.startsWith("55") && digitos.length > 11) {
            digitos = digitos.substring(2) // Pula os dois primeiros caracteres ("55")
        }

        // 3. Aplica a máscara com base no tamanho do número restante (10 para fixo, 11 para celular).
        return when (digitos.length) {
            10 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 6)}-${digitos.substring(6)}" // Fixo: (DD) XXXX-XXXX
            11 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 7)}-${digitos.substring(7)}" // Celular: (DD) XXXXX-XXXX
            else -> numero // Se, mesmo após a limpeza, o número tiver um formato inesperado, retorna o original.
        }
    }

    /**
     * Formata o objeto Endereco em uma única String legível.
     */
    private fun formatarEndereco(endereco: Endereco?): String {
        if (endereco == null) {
            return "Endereço não informado"
        }
        return listOfNotNull(
            endereco.rua,
            endereco.cidade,
            endereco.estado,
            endereco.cep
        ).joinToString(separator = ", ")
    }

    private fun navegarParaEdicaoDePerfil(usuario: Usuario) {
        val intent = if (usuario.autonomo != null) {
            Intent(this, TelaEdicaoPerfilAutonomo::class.java)
        } else {
            Intent(this, TelaEdicaoPerfilContratante::class.java)
        }
        startActivity(intent)
    }

    private fun enviarEmailRedefinicaoSenha(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Link para alterar senha enviado para o seu e-mail.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Falha ao enviar e-mail de redefinição.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, TelaLogin::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
