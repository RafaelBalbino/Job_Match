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

    override fun getItemViewType(position: Int): Int {
        return when (userType) {
            "CONTRATANTE" -> TYPE_CONTRATANTE_VIEW
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
            bindAutonomoView(holder as AutonomoViewHolder, pedido, position)
        }
    }

    override fun getItemCount() = pedidos.size

    fun updateData(newPedidos: List<Pedidos>) {
        pedidos.clear()
        pedidos.addAll(newPedidos)
        notifyDataSetChanged()
    }

    // Lógica para a visão do CONTRATANTE
    private fun bindContratanteView(holder: ContratanteViewHolder, pedido: Pedidos) {
        val db = FirebaseFirestore.getInstance()
        val context = holder.itemView.context
        holder.binding.tvStatusPedido.text = pedido.status.uppercase()
        holder.binding.tvResumoDescricao.text = pedido.descricaoServico

        // CORREÇÃO: Lógica da imagem do problema agora usa o campo 'anexos'
        if (pedido.anexos.isEmpty()) {
            holder.binding.imgProblema.visibility = View.GONE
            holder.binding.tvImagemProblemaLabel.text = "Não há imagem em anexo"
        } else {
            holder.binding.imgProblema.visibility = View.VISIBLE
            holder.binding.tvImagemProblemaLabel.text = "Imagem do Problema"
            // Carrega a primeira imagem da lista
            holder.binding.imgProblema.load(pedido.anexos[0]) {
                error(R.drawable.ic_image_placeholder)
            }
        }

        holder.binding.tvVerMaisContratante.setOnClickListener {
            val intent = Intent(context, TelaCriacaoPedido::class.java).apply {
                putExtra("PEDIDO_ID", pedido.id)
                putExtra("MODE", "VIEW_ONLY")
            }
            context.startActivity(intent)
        }

        holder.binding.tvNomeLabel.text = "Profissional"
        if (pedido.autonomo.isNotEmpty()) {
            holder.binding.tvNomeAutonomo.visibility = View.VISIBLE
            db.collection("users").document(pedido.autonomo).get().addOnSuccessListener { doc ->
                val user = doc.toObject(Usuario::class.java)
                holder.binding.tvNomeAutonomo.text = user?.nome
                holder.binding.imgAutonomoPerfil.load(user?.fotoUrl) { 
                    placeholder(R.drawable.ic_profile_placeholder)
                    error(R.drawable.ic_profile_placeholder)
                }
            }
        } else {
            holder.binding.tvNomeAutonomo.text = "Procurando..."
            holder.binding.imgAutonomoPerfil.setImageResource(R.drawable.ic_profile_placeholder)
        }

        when (pedido.status) {
            "pendente" -> {
                holder.binding.botoesContratanteContainer.visibility = View.VISIBLE
                holder.binding.btnConcluirPedido.visibility = View.GONE
                holder.binding.btnCancelarPedido.visibility = View.VISIBLE
                holder.binding.btnCancelarPedido.setOnClickListener { 
                    showConfirmationDialog(context, "Cancelar Pedido", "Deseja realmente cancelar este pedido?", "cancelado", pedido)
                }
            }
            "aceito" -> {
                holder.binding.botoesContratanteContainer.visibility = View.VISIBLE
                holder.binding.btnConcluirPedido.visibility = View.VISIBLE
                holder.binding.btnCancelarPedido.visibility = View.GONE
                 holder.binding.btnConcluirPedido.setOnClickListener { 
                    showConfirmationDialog(context, "Concluir Serviço", "Deseja marcar este serviço como concluído?", "finalizado", pedido)
                }
            }
            else -> {
                holder.binding.botoesContratanteContainer.visibility = View.GONE
            }
        }
    }

    // Lógica para TODAS as visões do AUTÔNOMO
    private fun bindAutonomoView(holder: AutonomoViewHolder, pedido: Pedidos, position: Int) {
        val context = holder.itemView.context
        holder.binding.tvNomeContratante.text = pedido.nomeSolicitacao
        holder.binding.tvTelefone.text = pedido.telefoneSolicitante
        holder.binding.tvEmailContratante.text = pedido.emailSolicitante
        holder.binding.tvEnderecoContratante.text = "${pedido.cidade} - ${pedido.estado}"
        holder.binding.tvTipoServico.text = pedido.tipoServico
        holder.binding.tvDescricaoCurta.text = pedido.descricaoServico
        
        val statusUpper = pedido.status.uppercase()
        holder.binding.tvStatusPedido.text = statusUpper

        if (userType == "AUTONOMO" && statusUpper == "PENDENTE") {
            holder.binding.botoesAutonomoContainer.visibility = View.VISIBLE
            holder.binding.tvStatusPedido.visibility = View.GONE
        } else {
            holder.binding.botoesAutonomoContainer.visibility = View.GONE
            holder.binding.tvStatusPedido.visibility = View.VISIBLE
        }
        
        holder.binding.btnAceitarPedido.setOnClickListener { showAcceptDialog(pedido, context) }
        holder.binding.btnNegarPedido.setOnClickListener { negarPedido(position) }

        holder.binding.tvVerDetalhes.setOnClickListener {
            val intent = Intent(context, TelaCriacaoPedido::class.java).apply {
                putExtra("PEDIDO_ID", pedido.id)
                putExtra("MODE", "VIEW_ONLY")
            }
            context.startActivity(intent)
        }
    }
    
    private fun negarPedido(position: Int) {
        pedidos.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, pedidos.size)
    }

    private fun showConfirmationDialog(context: Context, title: String, message: String, newStatus: String, pedido: Pedidos) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sim") { dialog, _ ->
                atualizarStatusPedido(context, pedido, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Não") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun atualizarStatusPedido(context: Context, pedido: Pedidos, status: String) {
        FirebaseFirestore.getInstance().collection("pedido").document(pedido.id)
            .update("status", status)
            .addOnSuccessListener {
                Toast.makeText(context, "Pedido atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                if (status == "finalizado") {
                    val intent = Intent(context, TelaAvaliacao::class.java).apply {
                        putExtra("PEDIDO_ID", pedido.id)
                        putExtra("AUTONOMO_ID", pedido.autonomo)
                    }
                    context.startActivity(intent)
                }
            }
            .addOnFailureListener { 
                Toast.makeText(context, "Falha ao atualizar o pedido.", Toast.LENGTH_SHORT).show()
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
            val intent = Intent(context, telaNegocioFechado::class.java)
            context.startActivity(intent)
        }.addOnFailureListener {
            Toast.makeText(context, "Falha ao aceitar o pedido.", Toast.LENGTH_SHORT).show()
        }
    }
}