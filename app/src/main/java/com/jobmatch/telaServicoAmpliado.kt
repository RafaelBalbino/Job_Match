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
import com.jobmatch.databinding.ActivityTelaServicoAmpliadoBinding

class telaServicoAmpliado : AppCompatActivity() {

    private lateinit var binding: ActivityTelaServicoAmpliadoBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTelaServicoAmpliadoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance() // Inicializa o Firebase Auth

        // Define o placeholder e o botão de voltar imediatamente
        binding.ivServicoImagem.setImageResource(R.drawable.ic_image_placeholder)
        binding.btnVoltar.setOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. CORREÇÃO: Recebe o objeto Servico de forma segura e compatível
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

        // 2. Popular a tela com os dados recebidos
        binding.tvTituloServico.text = servico.nomeServico
        binding.tvDescricaoServico.text = servico.descricaoServico
        binding.ivServicoImagem.load(servico.fotoServico) {
            crossfade(true)
            placeholder(R.drawable.ic_image_placeholder)
            error(R.drawable.ic_image_placeholder)
        }

        // 3. CORREÇÃO: Usa uidAutonomo para verificar o dono do serviço
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.uid == servico.uidAutonomo) {
            // Cenário 1: O usuário é o dono do serviço
            binding.btnFazerPedido.visibility = View.GONE // Esconde o botão de fazer pedido
            binding.btnEditarServico.visibility = View.VISIBLE // Mostra o botão de editar
            
            binding.btnEditarServico.setOnClickListener {
                val intent = Intent(this, CadastrarServico::class.java).apply {
                    putExtra("SERVICO_PARA_EDITAR", servico)
                }
                startActivity(intent)
            }

        } else {
            // Cenário 2: O usuário é um contratante vendo o serviço de outra pessoa
            binding.btnEditarServico.visibility = View.GONE // Esconde o botão de editar
            binding.btnFazerPedido.visibility = View.VISIBLE // Mostra o botão de fazer pedido

            binding.btnFazerPedido.setOnClickListener {
                val intent = Intent(this, TelaCriacaoPedido::class.java).apply {
                    putExtra("SERVICE_NAME", servico.nomeServico)
                    putExtra("SERVICE_DESCRIPTION", servico.descricaoServico)
                    putExtra("AUTONOMO_ID", servico.uidAutonomo) // Passa o ID do autônomo para a próxima tela
                }
                startActivity(intent)
            }
        }
    }
}