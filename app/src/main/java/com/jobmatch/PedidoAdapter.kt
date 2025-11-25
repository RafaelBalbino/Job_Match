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
import com.jobmatch.databinding.ItemPedidoContratanteBinding

class PedidoAdapter(
    private var pedidos: MutableList<Pedidos>,
    private val userType: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Tipos de View para cada layout
    companion object {
        private const val TYPE_CONTRATANTE_VIEW = 0
        private const val TYPE_AUTONOMO_VIEW = 1
    }

    // ViewHolders para cada layout
    inner class ContratanteViewHolder(val binding: ItemPedidoContratanteBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AutonomoViewHolder(val binding: ItemPedidoAutonomoBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Define qual layout será usado. A lógica foi restaurada e corrigida:
     * - Listas de CONTRATANTE e AUTONOMO_ACEITOS usam o layout de contratante.
     * - Lista de AUTONOMO (pedidos disponíveis) usa o layout de autônomo.
     */
    override fun getItemViewType(position: Int): Int {
        return when (userType) {
            "CONTRATANTE", "AUTONOMO_ACEITOS" -> TYPE_CONTRATANTE_VIEW
            else -> TYPE_AUTONOMO_VIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_CONTRATANTE_VIEW) {
            val binding = ItemPedidoContratanteBinding.inflate(inflater, parent, false)
            ContratanteViewHolder(binding)
        } else {
            val binding = ItemPedidoAutonomoBinding.inflate(inflater, parent, false)
            AutonomoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pedido = pedidos[position]
        if (holder.itemViewType == TYPE_CONTRATANTE_VIEW) {
            bindContratanteView(holder as ContratanteViewHolder, pedido)
        } else {
            bindAutonomoView(holder as AutonomoViewHolder, pedido)
        }
    }

    override fun getItemCount() = pedidos.size

    fun updateData(newPedidos: List<Pedidos>) {
        pedidos.clear()
        pedidos.addAll(newPedidos)
        notifyDataSetChanged()
    }

    /**
     * Preenche o layout item_pedido_contratante. Esta função agora trata
     * tanto a visão do contratante quanto a do autônomo vendo seus projetos.
     */
    private fun bindContratanteView(holder: ContratanteViewHolder, pedido: Pedidos) {
        val db = FirebaseFirestore.getInstance()
        holder.binding.tvStatusPedido.text = pedido.status.uppercase()

        // Lógica para preencher os dados da outra parte envolvida
        when(userType) {
            "CONTRATANTE" -> {
                holder.binding.tvNomeLabel.text = "Profissional"
                if (pedido.autonomo.isNotEmpty()) {
                    holder.binding.tvNomeAutonomo.visibility = View.VISIBLE
                    db.collection("users").document(pedido.autonomo).get().addOnSuccessListener { doc ->
                        holder.binding.tvNomeAutonomo.text = doc.getString("nome")
                    }
                } else {
                    holder.binding.tvNomeAutonomo.text = "Procurando..."
                }
            }
            "AUTONOMO_ACEITOS" -> {
                holder.binding.tvNomeLabel.text = "Contratante"
                 db.collection("users").document(pedido.contratanteId).get().addOnSuccessListener { doc ->
                    holder.binding.tvNomeAutonomo.text = doc.getString("nome")
                }
            }
        }
    }

    /**
     * Preenche o layout item_pedido_autonomo. Usado para autônomos vendo pedidos disponíveis.
     */
    private fun bindAutonomoView(holder: AutonomoViewHolder, pedido: Pedidos) {
        val context = holder.itemView.context
        holder.binding.tvNomeContratante.text = pedido.nomeSolicitacao
        holder.binding.tvTelefone.text = pedido.telefoneSolicitante
        holder.binding.tvEmailContratante.text = pedido.emailSolicitante
        holder.binding.tvEnderecoContratante.text = "${pedido.cidade} - ${pedido.estado}"
        holder.binding.tvTipoServico.text = pedido.tipoServico
        holder.binding.tvDescricaoCurta.text = pedido.descricaoServico
        
        val statusUpper = pedido.status.uppercase()
        holder.binding.tvStatusPedido.text = statusUpper
        when (statusUpper) {
            "DISPONIVEL" -> {
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.sucesso))
                holder.binding.tvStatusPedido.isClickable = true
                holder.binding.tvStatusPedido.setOnClickListener { showAcceptDialog(pedido, context) }
            }
            else -> {
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.cinza_escuro))
                holder.binding.tvStatusPedido.isClickable = false
            }
        }

        holder.binding.tvVerDetalhes.setOnClickListener {
            val intent = Intent(context, TelaCriacaoPedido::class.java).apply {
                putExtra("PEDIDO_ID", pedido.id)
                putExtra("MODE", "VIEW_ONLY")
            }
            context.startActivity(intent)
        }
    }

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
            Toast.makeText(context, "Pedido aceito!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Falha ao aceitar o pedido.", Toast.LENGTH_SHORT).show()
        }
    }
}