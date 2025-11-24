package com.jobmatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ServicoAdapter(private val servicos: List<Servico>) : RecyclerView.Adapter<ServicoAdapter.ServicoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_servico_encontrado, parent, false)
        return ServicoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServicoViewHolder, position: Int) {
        val servico = servicos[position]
        holder.bind(servico)
    }

    override fun getItemCount() = servicos.size

    class ServicoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val servicoImage: ImageView = itemView.findViewById(R.id.servico_image)
        private val servicoName: TextView = itemView.findViewById(R.id.servico_name)
        private val freelancerName: TextView = itemView.findViewById(R.id.freelancer_name) // TextView adicionado

        fun bind(servico: Servico) {
            servicoName.text = servico.nomeServico
            freelancerName.text = servico.nomeAutonomo // Atribuindo o nome do autônomo

            if (!servico.fotoServico.isNullOrEmpty()) {
                servicoImage.load(servico.fotoServico) {
                    crossfade(true)
                    error(R.drawable.rounded_edittext_background) // Imagem de fallback
                }
            }
        }
    }
}
