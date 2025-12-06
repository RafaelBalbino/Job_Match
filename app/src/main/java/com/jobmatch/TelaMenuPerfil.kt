package com.jobmatch // Mantenha seu pacote original aqui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaMenuPerfilBinding

class TelaMenuPerfil : AppCompatActivity() {

    // Binding para acessar as views do layout com segurança
    private lateinit var binding: ActivityTelaMenuPerfilBinding
    // Instâncias do Firebase para autenticação e banco de dados
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    // Armazena os dados do usuário logado para evitar múltiplas leituras do banco
    private var currentUser: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Habilita o modo de tela cheia

        // Infla o layout e o define como o conteúdo da activity
        binding = ActivityTelaMenuPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa as instâncias do Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Define uma imagem de placeholder enquanto a foto do perfil carrega
        binding.imgPerfilUsuario.setImageResource(R.drawable.ic_profile_placeholder)

        // Ajusta o padding da tela para não sobrepor as barras do sistema (status bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutRootMenu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicia o carregamento dos dados do usuário
        carregarDadosUsuario()
        // Configura os cliques dos botões que são iguais para todos os usuários
        configurarCliquesGenericos()
        // Configura o novo seletor de tema
        configurarSeletorDeTema()
    }

    /**
     * Carrega os dados do usuário logado do Firestore e os exibe na tela.
     * Também chama a função para configurar os botões específicos do tipo de usuário.
     */
    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("TelaMenuPerfil", "Usuário não autenticado.")
            fazerLogout() // Se não houver usuário, volta para a tela de login
            return
        }

        // Busca o documento do usuário na coleção "users"
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Converte o documento em um objeto Usuario
                    val usuario = document.toObject(Usuario::class.java)
                    this.currentUser = usuario // Salva o usuário para uso posterior

                    if (usuario != null) {
                        // Preenche os dados do cabeçalho
                        binding.txtNomeUsuario.text = usuario.nome
                        binding.txtEmailUsuario.text = usuario.email

                        // Carrega a foto do perfil usando a biblioteca Coil
                        binding.imgPerfilUsuario.load(usuario.fotoUrl) {
                            crossfade(true) // Efeito de transição suave
                            placeholder(R.drawable.ic_profile_placeholder) // Imagem enquanto carrega
                            error(R.drawable.ic_profile_placeholder) // Imagem em caso de erro
                            transformations(CircleCropTransformation())
                        }

                        // AGORA, configura os botões que dependem do tipo de usuário
                        configurarBotoesDeAcao(usuario)

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

    /**
     * Configura a visibilidade e a ação dos botões que mudam de acordo com o tipo de usuário.
     * @param usuario O objeto Usuario com os dados da pessoa logada.
     */
    private fun configurarBotoesDeAcao(usuario: Usuario) {
        if (usuario.autonomo != null) {
            // LÓGICA PARA AUTÔNOMO
            binding.tvMeusPedidos.text = "Buscar Pedidos"
            binding.btnMeusPedidos.setOnClickListener { 
                abrirListaDePedidos("fragmentListaPedidosAutonomo", "BUSCA") 
            }

            binding.btnProjetos.visibility = View.VISIBLE
            binding.btnProjetos.setOnClickListener { 
                abrirListaDePedidos("fragmentListaPedidosAutonomo", "PROJETOS") 
            }
        } else {
            // LÓGICA PARA CONTRATANTE
            binding.tvMeusPedidos.text = "Meus Pedidos"
            binding.btnMeusPedidos.setOnClickListener { 
                abrirListaDePedidos("fragmentListaPedidosContratante", "CONTRATANTE") 
            }
            binding.btnProjetos.visibility = View.GONE
        }

        // Lógica do botão "Meu Perfil"
        binding.btnMeuPerfil.setOnClickListener {
            if (currentUser?.autonomo != null) {
                // Se for autônomo, leva para a sua vitrine de serviços (TelaPerfilAutonomo)
                val intent = Intent(this, TelaPerfilAutonomo::class.java)
                startActivity(intent)
            } else {
                // Se for contratante, leva para a tela de informações da conta
                startActivity(Intent(this, TelaMeuPerfil::class.java))
            }
        }
    }

    /**
     * Configura os cliques de botões que têm a mesma ação para qualquer tipo de usuário.
     */
    private fun configurarCliquesGenericos() {
        binding.btnFecharMenu.setOnClickListener { finish() } // Fecha a tela
        binding.btnPagamentos.setOnClickListener { startActivity(Intent(this, TelaPagamentos::class.java)) } // CORREÇÃO: Abre a TelaPagamentos
        binding.btnSobreNos.setOnClickListener { startActivity(Intent(this, SobreNosActivity::class.java)) } 
        binding.btnTermos.setOnClickListener { mostrarPopupTermos() }
        binding.btnEncerrarSessao.setOnClickListener { fazerLogout() } // Desloga o usuário
    }
    
    private fun configurarSeletorDeTema() {
        // Define o estado inicial do switch baseado no tema atual do app
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        binding.switchTema.isChecked = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        // Adiciona um listener para mudar o tema quando o switch for clicado
        binding.switchTema.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    /**
     * Abre a tela que contém o fragmento da lista de pedidos.
     * @param fragmentName O nome da classe do fragmento a ser carregado.
     * @param tipoQuery O tipo de query a ser executada no fragmento.
     */
    private fun abrirListaDePedidos(fragmentName: String, tipoQuery: String) {
        val intent = Intent(this, FragmentContainerActivity::class.java).apply {
            putExtra("FRAGMENT_NAME", fragmentName)
            putExtra("TIPO_QUERY", tipoQuery)
        }
        startActivity(intent)
    }
    
    /**
     * Abre a tela que contém um fragmento genérico.
     */
    private fun abrirFragmento(fragmentName: String) {
        val intent = Intent(this, FragmentContainerActivity::class.java).apply {
            putExtra("FRAGMENT_NAME", fragmentName)
        }
        startActivity(intent)
    }

    /**
     * Exibe um AlertDialog com as políticas de privacidade.
     */
    private fun mostrarPopupTermos() {
        AlertDialog.Builder(this)
            .setTitle("Políticas de Privacidade")
            .setMessage("Nossa política de privacidade segue as diretrizes da LGPD, como o Princípio da Finalidade e o Princípio da Necessidade, garantindo a proteção e o uso consciente dos seus dados. Esses são alguns dos termos que você concordou.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Realiza o logout do usuário no Firebase e o redireciona para a tela de login.
     */
    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, TelaLogin::class.java)
        // Limpa a pilha de telas para que o usuário não possa "voltar" para a área logada
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Função auxiliar para exibir uma mensagem rápida (Toast).
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}