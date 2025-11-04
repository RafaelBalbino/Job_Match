package com.jobmatch



import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.jobmatch.databinding.ItemServicoButtonBinding
import coil.load

// Assumindo que você tem uma classe de dados 'Servico'

class ServicosAdapter(
    // 1. A lista de dados que será exibida
    private val listaServicos: List<Servico>,
    // 2. Um listener opcional para lidar com cliques nos itens
    private val onServiceClick: (Servico) -> Unit
) : RecyclerView.Adapter<ServicosAdapter.ServicoViewHolder>() {

    // --- ViewHolder ---
    // Classe responsável por segurar e vincular as Views de cada item
    inner class ServicoViewHolder(
        private val binding: ItemServicoButtonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(servico: Servico) {
            // 🎯 Aqui você vincula os dados do objeto 'Servico' às Views do CardView
            binding.txtNameCardServico.text = servico.nomeServico ?: "Serviço Desconhecido"

            // Configura o clique no item inteiro
            binding.cardServicoRoot.setOnClickListener {
                onServiceClick(servico)
            }
            // Lógica de Coil/Glide:
            if (!servico.fotoServico.isNullOrBlank()) {
                // Certifique-se de que o import 'coil.load' está no topo
                binding.imageServicoFundo.load(servico.fotoServico) {
                    // crossfade(true) // Descomente se estiver usando Coil/Glide
                    placeholder(R.drawable.ic_alerta_24)
                }
            } else {
                binding.imageServicoFundo.setImageResource(R.drawable.ic_alerta_24)
            }
            binding.iconEditar.setOnClickListener {
                // Adicione a lógica de edição aqui
                Toast.makeText(it.context, "Editar: ${servico.nomeServico}", Toast.LENGTH_SHORT).show()
            }

            binding.iconExcluir.setOnClickListener {
                // Adicione a lógica de exclusão aqui
                Toast.makeText(it.context, "Excluir: ${servico.nomeServico}", Toast.LENGTH_SHORT).show()
            }
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
}