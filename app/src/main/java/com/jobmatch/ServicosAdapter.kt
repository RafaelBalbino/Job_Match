package com.jobmatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jobmatch.databinding.ItemServicoButtonBinding

// Adaptador para a lista de serviços no perfil do autônomo
class ServicosAdapter(
    // 1. A lista de dados que será exibida (mutável para permitir atualizações)
    private var listaServicos: MutableList<Servico>,
    // 2. Listeners para os três tipos de clique possíveis
    private val onServiceClick: (Servico) -> Unit,
    private val onEditClick: (Servico) -> Unit,
    private val onDeleteClick: (Servico) -> Unit
) : RecyclerView.Adapter<ServicosAdapter.ServicoViewHolder>() {

    // --- ViewHolder ---
    // Classe responsável por segurar e vincular as Views de cada item
    inner class ServicoViewHolder(private val binding: ItemServicoButtonBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(servico: Servico) {
            // 🎯 Vincula os dados do objeto 'Servico' às Views do CardView
            binding.txtNameCardServico.text = servico.nomeServico

            // Lógica de carregamento de imagem com Coil
            binding.imageServicoFundo.load(servico.fotoServico) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder) // Imagem padrão enquanto carrega
                error(R.drawable.ic_image_placeholder)       // Imagem para caso de erro
            }

            // --- Configura os cliques para chamar as funções recebidas ---
            binding.cardServicoRoot.setOnClickListener { onServiceClick(servico) }
            binding.iconEditar.setOnClickListener { onEditClick(servico) }
            binding.iconExcluir.setOnClickListener { onDeleteClick(servico) }
        }
    }

    // Cria e infla o layout do item (ItemServicoButtonBinding)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicoViewHolder {
        val binding = ItemServicoButtonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false // Não anexar ao pai imediatamente
        )
        return ServicoViewHolder(binding)
    }

    // Vincula os dados na posição atual com o ViewHolder
    override fun onBindViewHolder(holder: ServicoViewHolder, position: Int) {
        holder.bind(listaServicos[position])
    }

    // Retorna o número total de itens na lista
    override fun getItemCount(): Int = listaServicos.size

    // Função pública para permitir que a Activity atualize a lista de serviços
    fun updateData(newServicos: List<Servico>) {
        listaServicos.clear()
        listaServicos.addAll(newServicos)
        notifyDataSetChanged()
    }
}