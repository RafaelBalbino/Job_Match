package com.jobmatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.jobmatch.databinding.ActivityTelaServicoAmpliadoBinding

class telaServicoAmpliado : AppCompatActivity() {

    private lateinit var binding: ActivityTelaServicoAmpliadoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTelaServicoAmpliadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Receber os dados enviados da tela anterior
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        val serviceDescription = intent.getStringExtra("SERVICE_DESCRIPTION")
        val serviceImageUrl = intent.getStringExtra("SERVICE_IMAGE_URL")

        // 2. Popular a tela com os dados recebidos
        binding.tvTituloServico.text = serviceName
        binding.tvDescricaoServico.text = serviceDescription
        binding.ivServicoImagem.load(serviceImageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_profile_placeholder) // Imagem temporária enquanto carrega
            error(R.drawable.ic_profile_placeholder)       // Imagem para caso de erro
        }

        // 3. Configurar o botão de voltar
        binding.btnVoltar.setOnClickListener {
            finish() // Fecha a tela atual e volta
        }

        // 4. Configurar o botão "Fazer Pedido"
        binding.btnFazerPedido.setOnClickListener {
            val intent = Intent(this, TelaCriacaoPedido::class.java).apply {
                // Envia o nome e a descrição para a próxima tela
                putExtra("SERVICE_NAME", serviceName)
                putExtra("SERVICE_DESCRIPTION", serviceDescription)
            }
            startActivity(intent)
        }
    }
}