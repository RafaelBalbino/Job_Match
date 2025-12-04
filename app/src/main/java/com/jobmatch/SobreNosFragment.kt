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

    private data class Integrante(
        val nome: String,
        val linkedinUrl: String
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

        binding.btnVoltarSobreNos.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Define os dados de cada integrante para facilitar a reordenação
        val adriel = Integrante("Adriel de Castro Moura", "https://www.linkedin.com/in/adricastro/")
        val leticiaO = Integrante("Letícia Oliveira Gonzalez", "https://www.linkedin.com/in/leticia-gonzalez/")
        val kaio = Integrante("Kaio Freires de Abreu", "https://www.linkedin.com/in/kaio-freires-de-abreu/")
        val leticiaS = Integrante("Letícia Silva de Bonis", "https://www.linkedin.com/in/leticia-de-bonis/")
        val rafael = Integrante("Rafael Ballabinute Balbino", "https://www.linkedin.com/in/rafael-ballabinute-balbino/")

        // Mapeia os dados para os cards na ordem que você pediu
        // Documentação
        setupIntegranteCard(binding.integrante1, adriel)
        setupIntegranteCard(binding.integrante2, leticiaO)
        // Desenvolvimento
        setupIntegranteCard(binding.integrante3, adriel) // Adriel novamente
        setupIntegranteCard(binding.integrante4, kaio)
        setupIntegranteCard(binding.integrante5, leticiaS)
        setupIntegranteCard(binding.integrante6, rafael)
    }

    /**
     * Preenche um card de integrante com os dados e configura seus cliques.
     */
    private fun setupIntegranteCard(cardBinding: ItemIntegranteBinding, integrante: Integrante) {
        cardBinding.apply {
            tvNomeIntegrante.text = integrante.nome

            tvLinkedinIntegrante.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(integrante.linkedinUrl))
                startActivity(intent)
            }

            // Lógica automática para carregar a foto do drawable.
            val resourceName = when (integrante.nome) {
                "Letícia Oliveira Gonzalez" -> "leticiao"
                "Letícia Silva de Bonis" -> "leticias"
                else -> integrante.nome.split(" ")[0].lowercase()
            }
            
            val resourceId = requireContext().resources.getIdentifier(resourceName, "drawable", requireContext().packageName)

            // Se encontrar o arquivo, carrega. Se não, usa o placeholder.
            if (resourceId != 0) {
                ivFotoIntegrante.load(resourceId) {
                    crossfade(true)
                    transformations(CircleCropTransformation()) // Garante a foto circular
                }
            } else {
                ivFotoIntegrante.load(R.drawable.ic_profile_placeholder) {
                    transformations(CircleCropTransformation()) // Garante a foto circular
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}