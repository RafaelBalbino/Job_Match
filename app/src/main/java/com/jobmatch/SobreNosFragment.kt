package com.jobmatch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.jobmatch.databinding.FragmentSobreNosBinding
import com.jobmatch.databinding.ItemIntegranteBinding

class SobreNosFragment : Fragment() {

    private var _binding: FragmentSobreNosBinding? = null
    private val binding get() = _binding!!

    // Crie uma data class para organizar os dados de cada integrante
    private data class Integrante(
        val nome: String,
        val linkedinUrl: String,
        val fotoUrl: String? = null, // Para carregar da internet
        val fotoDrawable: Int? = null // Para carregar do projeto
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSobreNosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botão para voltar
        binding.btnVoltarSobreNos.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // PREENCHA AQUI: Lista com os dados dos integrantes
        val integrantes = listOf(
            // Documentação
            Integrante("Nome Documentação 1", "https://www.linkedin.com/in/seu-usuario-1", fotoDrawable = R.drawable.ic_profile_placeholder),
            Integrante("Nome Documentação 2", "https://www.linkedin.com/in/seu-usuario-2", fotoDrawable = R.drawable.ic_profile_placeholder),
            // Desenvolvimento
            Integrante("Nome Dev 1", "https://www.linkedin.com/in/seu-usuario-3", fotoUrl = "URL_DA_SUA_IMAGEM_AQUI"),
            Integrante("Nome Dev 2", "https://www.linkedin.com/in/seu-usuario-4", fotoDrawable = R.drawable.ic_profile_placeholder),
            Integrante("Nome Dev 3", "https://www.linkedin.com/in/seu-usuario-5", fotoDrawable = R.drawable.ic_profile_placeholder)
        )

        // Mapeia os dados da lista para os cards no layout
        setupIntegranteCard(binding.integrante1, integrantes[0])
        setupIntegranteCard(binding.integrante2, integrantes[1])
        setupIntegranteCard(binding.integrante3, integrantes[2])
        setupIntegranteCard(binding.integrante4, integrantes[3])
        setupIntegranteCard(binding.integrante5, integrantes[4])
    }

    /**
     * Preenche um card de integrante com os dados e configura seus cliques.
     */
    private fun setupIntegranteCard(cardBinding: ItemIntegranteBinding, integrante: Integrante) {
        cardBinding.apply {
            // Preenche o nome
            tvNomeIntegrante.text = integrante.nome

            // Configura o clique do LinkedIn para abrir o navegador
            tvLinkedinIntegrante.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(integrante.linkedinUrl))
                startActivity(intent)
            }

            // Exemplo de como carregar a foto de uma URL da internet (descomente para usar)
            // ivFotoIntegrante.load(integrante.fotoUrl) {
            //     crossfade(true)
            //     placeholder(R.drawable.ic_profile_placeholder)
            //     error(R.drawable.ic_profile_placeholder)
            // }

            // Exemplo de como carregar a foto de um recurso drawable do projeto
             integrante.fotoDrawable?.let {
                 ivFotoIntegrante.setImageResource(it)
             }

            // Se estiver usando URL, pode descomentar o bloco acima e comentar o de baixo
             integrante.fotoUrl?.let {
                 ivFotoIntegrante.load(it) {
                     crossfade(true)
                     placeholder(R.drawable.ic_profile_placeholder)
                     error(R.drawable.ic_profile_placeholder)
                 }
             }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}