package com.jobmatch

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ServicoAdapter(
    private var servicos: MutableList<Servico>,
    private val showFreelancerName: Boolean = true // Parâmetro para controlar a visibilidade
) : RecyclerView.Adapter<ServicoAdapter.ServicoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_servico_encontrado, parent, false)
        return ServicoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServicoViewHolder, position: Int) {
        val servico = servicos[position]
        holder.bind(servico, showFreelancerName) // Passa a flag para o ViewHolder

        // CORREÇÃO: Lógica de clique condicional
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            // Quando a busca é por autônomo, o nome do serviço é "Ver Perfil"
            if (servico.nomeServico == "Ver Perfil") {
                // Navega para a tela de perfil do autônomo
                val intent = Intent(context, TelaNegocioAutonomo::class.java).apply {
                    putExtra("AUTONOMO_ID", servico.uidAutonomo)
                }
                context.startActivity(intent)
            } else {
                // Navega para a tela de detalhes do serviço (comportamento padrão)
                val intent = Intent(context, telaServicoAmpliado::class.java).apply {
                    putExtra("SERVICO", servico)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = servicos.size

    fun updateData(newServicos: List<Servico>) {
        servicos.clear()
        servicos.addAll(newServicos)
        notifyDataSetChanged()
    }

    class ServicoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val servicoImage: ImageView = itemView.findViewById(R.id.servico_image)
        private val servicoName: TextView = itemView.findViewById(R.id.servico_name)
        private val freelancerName: TextView = itemView.findViewById(R.id.freelancer_name)

        fun bind(servico: Servico, showName: Boolean) {
            servicoName.text = servico.nomeServico

            if (showName) {
                freelancerName.visibility = View.VISIBLE
                freelancerName.text = servico.nomeAutonomo
            } else {
                freelancerName.visibility = View.GONE
            }

            if (!servico.fotoServico.isNullOrEmpty()) {
                servicoImage.load(servico.fotoServico) {
                    crossfade(true)
                    placeholder(R.drawable.ic_image_placeholder)
                    error(R.drawable.ic_image_placeholder)
                }
            } else {
                servicoImage.load(R.drawable.ic_image_placeholder)
            }
        }
    }
}