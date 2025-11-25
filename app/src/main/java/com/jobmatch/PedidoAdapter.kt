package com.jobmatch

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ItemPedidoAutonomoBinding

class PedidoAdapter(
    private var pedidos: MutableList<Pedidos>,
    private val userType: String
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    // Unificando em um único ViewHolder que usa o layout correto para listas de pedidos.
    inner class PedidoViewHolder(val binding: ItemPedidoAutonomoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        // Sempre infla o item_pedido_autonomo, que é o layout correto e completo.
        val binding = ItemPedidoAutonomoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = pedidos[position]
        bindPedidoView(holder, pedido)
    }

    override fun getItemCount() = pedidos.size

    fun updateData(newPedidos: List<Pedidos>) {
        pedidos.clear()
        pedidos.addAll(newPedidos)
        notifyDataSetChanged()
    }

    /**
     * Lógica centralizada e corrigida para preencher um item da lista.
     * Ela adapta o layout item_pedido_autonomo para mostrar a informação correta
     * dependendo se o usuário é CONTRATANTE ou AUTÔNOMO, e gerencia a visibilidade
     * de todos os campos para evitar bugs de reciclagem.
     */
    private fun bindPedidoView(holder: PedidoViewHolder, pedido: Pedidos) {
        val db = FirebaseFirestore.getInstance()
        val context = holder.itemView.context

        // 1. Preenche os detalhes do SERVIÇO (comum para todas as visões)
        holder.binding.tvTipoServico.text = pedido.tipoServico
        holder.binding.tvDescricaoCurta.text = pedido.descricaoServico
        holder.binding.tvEnderecoContratante.text = "${pedido.cidade} - ${pedido.estado}"

        // 2. Preenche os dados da PESSOA (Contratante ou Autônomo) de forma explícita
        when (userType) {
            "CONTRATANTE" -> {
                // Se sou contratante, quero ver os dados do autônomo (se houver)
                holder.binding.tvNomeContratanteLabel.text = "Profissional"
                if (pedido.autonomo.isNotEmpty()) {
                    holder.binding.tvNomeContratante.visibility = View.VISIBLE
                    holder.binding.tvTelefone.visibility = View.VISIBLE
                    holder.binding.tvEmailContratante.visibility = View.VISIBLE
                    db.collection("users").document(pedido.autonomo).get().addOnSuccessListener { doc ->
                        holder.binding.tvNomeContratante.text = doc.getString("nome")
                        holder.binding.tvTelefone.text = doc.getString("numeroTelefone")
                        holder.binding.tvEmailContratante.text = doc.getString("email")
                    }
                } else {
                    holder.binding.tvNomeContratante.visibility = View.VISIBLE
                    holder.binding.tvNomeContratante.text = "Procurando profissional..."
                    holder.binding.tvTelefone.visibility = View.GONE
                    holder.binding.tvEmailContratante.visibility = View.GONE
                }
            }
            "AUTONOMO_ACEITOS" -> {
                // Se sou autônomo vendo meus projetos, quero ver os dados do contratante
                holder.binding.tvNomeContratanteLabel.text = "Contratante"
                holder.binding.tvNomeContratante.visibility = View.VISIBLE
                holder.binding.tvTelefone.visibility = View.VISIBLE
                holder.binding.tvEmailContratante.visibility = View.VISIBLE
                holder.binding.tvNomeContratante.text = pedido.nomeSolicitacao
                holder.binding.tvTelefone.text = pedido.telefoneSolicitante
                holder.binding.tvEmailContratante.text = pedido.emailSolicitante
            }
            else -> { // AUTONOMO (pedidos disponíveis)
                 holder.binding.tvNomeContratanteLabel.text = "Contratante"
                 holder.binding.tvNomeContratante.visibility = View.VISIBLE
                 holder.binding.tvTelefone.visibility = View.VISIBLE
                 holder.binding.tvEmailContratante.visibility = View.VISIBLE
                 holder.binding.tvNomeContratante.text = pedido.nomeSolicitacao
                 holder.binding.tvTelefone.text = pedido.telefoneSolicitante
                 holder.binding.tvEmailContratante.text = pedido.emailSolicitante
            }
        }

        // 3. Lógica de Status e clique
        val statusUpper = pedido.status.uppercase()
        holder.binding.tvStatusPedido.text = statusUpper
        when (statusUpper) {
            "PENDENTE" -> {
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.atenção))
                // Apenas autônomos podem aceitar pedidos disponíveis
                holder.binding.tvStatusPedido.isClickable = (userType == "AUTONOMO")
                if (userType == "AUTONOMO") {
                    holder.binding.tvStatusPedido.setOnClickListener { showAcceptDialog(pedido, context) }
                } else {
                    holder.binding.tvStatusPedido.isClickable = false
                }
            }
            "ACEITO" -> {
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.sucesso))
                holder.binding.tvStatusPedido.isClickable = false
            }
            else -> {
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.cinza_escuro))
                holder.binding.tvStatusPedido.isClickable = false
            }
        }

        // Listener para ver mais detalhes do pedido
        holder.binding.tvVerDetalhes.setOnClickListener {
            val intent = Intent(context, TelaCriacaoPedido::class.java).apply {
                putExtra("PEDIDO_ID", pedido.id)
                putExtra("MODE", "VIEW_ONLY")
            }
            context.startActivity(intent)
        }
    }

    // Exibe o diálogo de confirmação para aceitar o pedido
    private fun showAcceptDialog(pedido: Pedidos, context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Aceitar Pedido")
            .setMessage("Tem certeza de que deseja aceitar este serviço?")
            .setPositiveButton("Sim, aceitar") { dialog, _ ->
                aceitarPedido(pedido, context)
                dialog.dismiss()
            }
            .setNegativeButton("Não") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Lógica para aceitar o pedido
    private fun aceitarPedido(pedido: Pedidos, context: Context) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val autonomoId = auth.currentUser?.uid

        if (autonomoId.isNullOrEmpty()) {
            Toast.makeText(context, "Erro: Usuário autônomo não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("pedido").document(pedido.id).update(
            mapOf(
                "status" to "aceito",
                "autonomo" to autonomoId
            )
        ).addOnSuccessListener {
            // Sucesso - a lista será atualizada automaticamente pelo snapshot listener
            Toast.makeText(context, "Pedido aceito!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Falha ao aceitar o pedido.", Toast.LENGTH_SHORT).show()
        }
    }
}