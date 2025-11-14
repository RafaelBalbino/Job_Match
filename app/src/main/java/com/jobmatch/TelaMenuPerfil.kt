package com.jobmatch // Mantenha seu pacote original aqui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.dismiss
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaMenuPerfilBinding



class TelaMenuPerfil : AppCompatActivity() {

    // A variável 'binding' acessa os componentes do XML com os nomes corretos
    private lateinit var binding: ActivityTelaMenuPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUser: Usuario? = null // Variável para guardar os dados do usuário

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaMenuPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Usa o ID do layout raiz: 'layout_root_menu'
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutRootMenu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        carregarDadosUsuario()
        configurarCliquesDoMenu()
    }

    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("TelaMenuPerfil", "Usuário não autenticado.")
            fazerLogout()
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    this.currentUser = usuario // Salva o usuário atual para uso posterior

                    if (usuario != null) {
                        // Usa os IDs corretos para nome, email e imagem
                        binding.txtNomeUsuario.text = usuario.nome
                        binding.txtEmailUsuario.text = usuario.email

                        // CORREÇÃO: Carregando a imagem de forma explícita com Coil
                        val imageView = binding.imgPerfilUsuario
                        imageView.load(usuario.fotoUrl) {
                            crossfade(true) // Adiciona uma transição suave
                            placeholder(R.drawable.ic_profile_placeholder)
                            error(R.drawable.ic_profile_placeholder)
                        }

                        if (usuario.autonomo != null) {
                            binding.btnProjetos.visibility = View.VISIBLE
                        } else {
                            binding.btnProjetos.visibility = View.GONE
                        }

                    } else {
                        Log.e("TelaMenuPerfil", "Falha ao converter o documento para objeto Usuario.")
                    }
                } else {
                    Log.e("TelaMenuPerfil", "Nenhum documento encontrado para o usuário: $userId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TelaMenuPerfil", "Erro ao buscar dados do usuário", exception)
                Toast.makeText(this, "Erro ao carregar dados do perfil.", Toast.LENGTH_SHORT).show()
            }
    }

    // Função que centraliza todos os cliques do menu com a LÓGICA ATUALIZADA
    private fun configurarCliquesDoMenu() {
        // Botão de fechar (X) no canto superior direito
        binding.btnFecharMenu.setOnClickListener {
            finish() // Fecha a tela atual e volta para a anterior
        }

        // --- CLIQUES DOS ITENS DE MENU ---
        binding.btnMeuPerfil.setOnClickListener {
            val intent = Intent(this, TelaMeuPerfil::class.java)
            startActivity(intent)
        }

        // --- LÓGICA ATUALIZADA PARA 'MEUS PEDIDOS' ---
        binding.btnMeusPedidos.setOnClickListener {
            if (currentUser != null) {
                // Se o campo 'autonomo' for nulo, o usuário é um contratante
                if (currentUser?.autonomo == null) {
                    val intent = Intent(this, TelaRealizarPedidos::class.java)
                    startActivity(intent)
                } else {
                    // Se for um autônomo, exibe uma mensagem
                    showToast("Esta seção é para contratantes. Veja seus projetos em 'Meus Projetos'.")
                }
            } else {
                // Caso os dados ainda não tenham sido carregados
                showToast("Aguarde, carregando dados do usuário.")
            }
        }

        binding.btnProjetos.setOnClickListener {
            showToast("Será implementado no futuro")
        }

        binding.btnPagamentos.setOnClickListener {
            showToast("Será implementado no futuro")
        }

        // Botão Sobre Nós
        binding.btnSobreNos.setOnClickListener {
            Toast.makeText(this, "Em Andamento", Toast.LENGTH_SHORT).show()
        }

        // Botão Termos e Condições
        binding.btnTermos.setOnClickListener {
            mostrarPopupTermos()
        }

        binding.btnConfiguracoes.setOnClickListener {
            showToast("Será implementado no futuro")
        }

        // Botão para encerrar a sessão
        binding.btnEncerrarSessao.setOnClickListener {
            fazerLogout()
        }
    }

    /**
     * Exibe um AlertDialog com o texto dos termos e condições.
     */
    private fun mostrarPopupTermos() {
        // Usa o AlertDialog do sistema de Views (appcompat), que é o correto para esta tela
        AlertDialog.Builder(this)
            .setTitle("Termos e Condições")
            .setMessage("Nossa política de privacidade segue as diretrizes da LGPD, garantindo a proteção e o uso consciente dos seus dados. Ao se cadastrar, você concorda com a coleta e o tratamento de suas informações para os fins descritos em nossos termos.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss() // Fecha o pop-up
            }
            .show()
    }


    // Função para fazer o logout do usuário e levá-lo à tela de login
    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, TelaLogin::class.java)
        // Limpa a pilha de telas para que o usuário não possa "voltar" para a área logada
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Função auxiliar para exibir mensagens rápidas
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
