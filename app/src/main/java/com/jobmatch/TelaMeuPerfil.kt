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

        // Configura o botão Voltar e o placeholder da imagem imediatamente
        binding.btnVoltar.setOnClickListener { finish() }
        binding.imgFotoMeuPerfil.setImageResource(R.drawable.ic_profile_placeholder)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        carregarDadosDoFirestore()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega os dados toda vez que a tela volta ao foco,
        // garantindo que as edições feitas em outra tela sejam refletidas.
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

        // 2. Preenche as informações pessoais
        binding.txtEmailInfo.text = usuario.email ?: "E-mail não informado"
        binding.txtTelefoneMeuPerfil.text = formatarTelefone(usuario.numeroTelefone)
        val enderecoFmt = listOfNotNull(usuario.cidade, usuario.estado).filter { it.isNotBlank() }.joinToString(" - ")
        binding.txtEnderecoInfo.text = if (enderecoFmt.isNotBlank()) enderecoFmt else "Endereço não informado"

        // 3. Lógica para exibir os blocos de perfil com base no tipo de usuário
        if (usuario.autonomo != null) {
            // É autônomo
            binding.blocoAcoesAutonomo.visibility = View.VISIBLE
            binding.blocoAutonomoContratante.visibility = View.GONE
            
            binding.itemBuscarPedidos.setOnClickListener {
                val intent = Intent(this, FragmentContainerActivity::class.java)
                intent.putExtra("FRAGMENT_NAME", "fragmentListaPedidosContratante")
                intent.putExtra("FRAGMENT_TYPE", "AUTONOMO")
                startActivity(intent)
            }
            binding.itemProjetos.setOnClickListener {
                val intent = Intent(this, FragmentContainerActivity::class.java)
                intent.putExtra("FRAGMENT_NAME", "fragmentListaPedidosContratante")
                intent.putExtra("FRAGMENT_TYPE", "AUTONOMO_ACEITOS")
                startActivity(intent)
            }
        } else {
            // É contratante
            binding.blocoAutonomoContratante.visibility = View.VISIBLE
            binding.blocoAcoesAutonomo.visibility = View.GONE
            binding.lbPerfilAutonomoContratante.text = "Perfil Contratante"
        }

        // 4. Configura os cliques dos outros botões
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
     * Corrigido para lidar com o código do país (55).
     */
    private fun formatarTelefone(numero: String?): String {
        if (numero.isNullOrBlank()) {
            return "Telefone não informado"
        }

        var digitos = numero.filter { it.isDigit() }

        if (digitos.startsWith("55") && digitos.length > 11) {
            digitos = digitos.substring(2)
        }

        return when (digitos.length) {
            10 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 6)}-${digitos.substring(6)}" // Fixo
            11 -> "(${digitos.substring(0, 2)}) ${digitos.substring(2, 7)}-${digitos.substring(7)}" // Celular
            else -> numero // Formato inesperado, retorna o original.
        }
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