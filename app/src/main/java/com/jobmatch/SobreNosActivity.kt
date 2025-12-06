package com.jobmatch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.jobmatch.databinding.ActivitySobreNosBinding

class SobreNosActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySobreNosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySobreNosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ação do botão voltar
        binding.btnVoltarSobreNos.setOnClickListener { 
            finish() // Simplesmente fecha a activity
        }

        // --- Preenchimento dos Cards ---
        
        // Documentação
        binding.integrante1.tvNomeIntegrante.text = "Adriel de Castro Moura"
        binding.integrante1.ivFotoIntegrante.load(R.drawable.adriel) { transformations(CircleCropTransformation()) }
        binding.integrante1.tvLinkedinIntegrante.setOnClickListener { openLink("https://www.linkedin.com/in/adricastro/") }

        binding.integrante2.tvNomeIntegrante.text = "Letícia Oliveira Gonzalez"
        binding.integrante2.ivFotoIntegrante.load(R.drawable.leticiao) { transformations(CircleCropTransformation()) }
        binding.integrante2.tvLinkedinIntegrante.setOnClickListener { openLink("https://www.linkedin.com/in/leticia-gonzalez/") }
        
        // Desenvolvimento
        binding.integrante3.tvNomeIntegrante.text = "Adriel de Castro Moura"
        binding.integrante3.ivFotoIntegrante.load(R.drawable.adriel) { transformations(CircleCropTransformation()) }
        binding.integrante3.tvLinkedinIntegrante.setOnClickListener { openLink("https://www.linkedin.com/in/adricastro/") }

        binding.integrante4.tvNomeIntegrante.text = "Kaio Freires de Abreu"
        binding.integrante4.ivFotoIntegrante.load(R.drawable.kaio) { transformations(CircleCropTransformation()) }
        binding.integrante4.tvLinkedinIntegrante.setOnClickListener { openLink("https://www.linkedin.com/in/kaio-freires-de-abreu/") }

        binding.integrante5.tvNomeIntegrante.text = "Letícia Silva de Bonis"
        binding.integrante5.ivFotoIntegrante.load(R.drawable.leticias) { transformations(CircleCropTransformation()) }
        binding.integrante5.tvLinkedinIntegrante.setOnClickListener { openLink("https://www.linkedin.com/in/leticia-de-bonis/") }

        binding.integrante6.tvNomeIntegrante.text = "Rafael Ballabinute Balbino"
        binding.integrante6.ivFotoIntegrante.load(R.drawable.rafael) { transformations(CircleCropTransformation()) }
        binding.integrante6.tvLinkedinIntegrante.setOnClickListener { openLink("https://www.linkedin.com/in/rafael-ballabinute-balbino/") }
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}