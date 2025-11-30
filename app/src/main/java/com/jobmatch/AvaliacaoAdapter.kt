package com.jobmatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jobmatch.databinding.FragmentItemAvaliacaoBinding
import java.text.SimpleDateFormat
import java.util.Locale

class AvaliacaoAdapter(
    private var avaliacoes: List<Avaliacao>
) : RecyclerView.Adapter<AvaliacaoAdapter.AvaliacaoViewHolder>() {

    inner class AvaliacaoViewHolder(val binding: FragmentItemAvaliacaoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvaliacaoViewHolder {
        val binding = FragmentItemAvaliacaoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AvaliacaoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvaliacaoViewHolder, position: Int) {
        val avaliacao = avaliacoes[position]
        holder.binding.apply {
            tvNomeContratante.text = avaliacao.contratanteNome
            rbNotaAvaliacao.rating = avaliacao.nota.toFloat()
            tvComentario.text = avaliacao.comentario

            // Carrega a imagem do contratante
            ivAvatarContratante.load(avaliacao.contratanteFotoUrl) {
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
            }

            // Formata e exibe a data
            avaliacao.dataHora?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvDataAvaliacao.text = sdf.format(it)
            }
        }
    }

    override fun getItemCount() = avaliacoes.size

    fun updateData(newAvaliacoes: List<Avaliacao>) {
        avaliacoes = newAvaliacoes
        notifyDataSetChanged()
    }
}