package com.jobmatch

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaServicoAmpliadoBinding

class telaServicoAmpliado : AppCompatActivity() {

    private lateinit var binding: ActivityTelaServicoAmpliadoBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTelaServicoAmpliadoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.ivServicoImagem.setImageResource(R.drawable.ic_image_placeholder)
        binding.btnVoltar.setOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val servico = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("SERVICO", Servico::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Servico>("SERVICO")
        }

        if (servico == null) {
            Toast.makeText(this, "Erro ao carregar dados do serviço.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Preenche a UI com os dados do serviço
        binding.tvTituloServico.text = servico.nomeServico
        binding.tvDescricaoServico.text = servico.descricaoServico
        binding.ivServicoImagem.load(servico.fotoServico) {
            crossfade(true)
            placeholder(R.drawable.ic_image_placeholder)
            error(R.drawable.ic_image_placeholder)
        }

        // Lógica de visibilidade dos botões
        configureButtons(servico)
    }

    private fun configureButtons(servico: Servico) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Usuário deslogado, esconde todos os botões de ação
            binding.btnFazerPedido.visibility = View.GONE
            binding.btnEditarServico.visibility = View.GONE
            return
        }

        val isOwner = currentUser.uid == servico.uidAutonomo

        if (isOwner) {
            // Cenário 1: O usuário é o dono do serviço
            binding.btnFazerPedido.visibility = View.GONE
            binding.btnEditarServico.visibility = View.VISIBLE
            binding.btnEditarServico.setOnClickListener {
                val intent = Intent(this, CadastrarServico::class.java).apply {
                    putExtra("SERVICO_PARA_EDITAR", servico)
                }
                startActivity(intent)
            }
        } else {
            // Cenário 2 e 3: O usuário NÃO é o dono do serviço
            // Precisamos saber se o usuário logado é autônomo ou contratante
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { userDocument ->
                    val usuario = userDocument.toObject(Usuario::class.java)
                    if (usuario?.autonomo != null) {
                        // Cenário 2: Usuário é um autônomo (mas não o dono)
                        binding.btnEditarServico.visibility = View.GONE
                        binding.btnFazerPedido.visibility = View.VISIBLE
                        binding.btnFazerPedido.text = "Adicionar Serviço ao Meu Perfil"
                        binding.btnFazerPedido.setOnClickListener {
                            // Leva para a tela de cadastro, com os dados preenchidos para cópia
                            val intent = Intent(this, CadastrarServico::class.java).apply {
                                putExtra("COPIAR_SERVICO", servico)
                            }
                            startActivity(intent)
                        }
                    } else {
                        // Cenário 3: Usuário é um contratante
                        binding.btnEditarServico.visibility = View.GONE
                        binding.btnFazerPedido.visibility = View.VISIBLE
                        binding.btnFazerPedido.text = "Fazer Pedido"
                        binding.btnFazerPedido.setOnClickListener {
                            val intent = Intent(this, TelaCriacaoPedido::class.java).apply {
                                putExtra("SERVICE_NAME", servico.nomeServico)
                                putExtra("SERVICE_DESCRIPTION", servico.descricaoServico)
                                putExtra("AUTONOMO_ID", servico.uidAutonomo)
                            }
                            startActivity(intent)
                        }
                    }
                }
                .addOnFailureListener {
                    // Em caso de falha ao buscar o perfil, esconde os botões por segurança
                    binding.btnFazerPedido.visibility = View.GONE
                    binding.btnEditarServico.visibility = View.GONE
                }
        }
    }
}