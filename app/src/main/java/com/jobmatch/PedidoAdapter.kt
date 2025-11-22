package com.jobmatch

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    companion object {
        private const val TYPE_CONTRATANTE = 0
        private const val TYPE_AUTONOMO = 1
    }

    // ViewHolder para a visão do Contratante
    inner class ContratanteViewHolder(val binding: ItemPedidoContratanteBinding) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder para a visão do Autônomo
    inner class AutonomoViewHolder(val binding: ItemPedidoAutonomoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (userType) {
            "CONTRATANTE", "AUTONOMO_ACEITOS" -> TYPE_CONTRATANTE
            else -> TYPE_AUTONOMO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CONTRATANTE) {
            val binding = ItemPedidoContratanteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ContratanteViewHolder(binding)
        } else {
            val binding = ItemPedidoAutonomoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AutonomoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pedido = pedidos[position]
        if (holder.itemViewType == TYPE_CONTRATANTE) {
            bindContratanteView(holder as ContratanteViewHolder, pedido)
        } else {
            bindAutonomoView(holder as AutonomoViewHolder, pedido)
        }
    }

    override fun getItemCount() = pedidos.size

    // Atualiza os dados do adapter
    fun updateData(newPedidos: List<Pedidos>) {
        pedidos.clear()
        pedidos.addAll(newPedidos)
        notifyDataSetChanged()
    }

    // Preenche a view do Contratante
    private fun bindContratanteView(holder: ContratanteViewHolder, pedido: Pedidos) {
        val db = FirebaseFirestore.getInstance()
        // A coleção de usuários está correta, não precisa mexer aqui
        val targetUserId = if (userType == "AUTONOMO_ACEITOS") pedido.contratanteId else pedido.autonomo

        if (targetUserId.isNotEmpty()) {
            db.collection("users").document(targetUserId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val user = document.toObject(Usuario::class.java)
                        holder.binding.tvNomeAutonomo.text = user?.nome
                        holder.binding.tvNumeroAutonomo.text = user?.numeroTelefone
                        holder.binding.tvEmailAutonomo.text = user?.email
                        holder.binding.imgAutonomoPerfil.load(user?.fotoUrl) {
                            error(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
        }

        holder.binding.tvStatusPedido.text = pedido.status.uppercase()
        if (pedido.anexos.isNotEmpty()) {
            holder.binding.imgProblema.load(pedido.anexos[0]) { error(R.drawable.ic_image_placeholder) }
        }

        // Listener para abrir o perfil (lógica mantida)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = if (userType == "AUTONOMO_ACEITOS") {
                Intent(context, TelaMeuPerfil::class.java).apply { putExtra("USER_ID", pedido.contratanteId) }
            } else {
                Intent(context, TelaPerfilAutonomo::class.java).apply { putExtra("AUTONOMO_ID", pedido.autonomo) }
            }
            context.startActivity(intent)
        }
    }

    // Preenche a view do Autônomo com a nova lógica
    private fun bindAutonomoView(holder: AutonomoViewHolder, pedido: Pedidos) {
        // Preenchendo os dados do contratante
        holder.binding.tvNomeContratante.text = pedido.nomeSolicitacao
        holder.binding.tvTelefone.text = pedido.telefoneSolicitante
        holder.binding.tvEmailContratante.text = pedido.emailSolicitante
        // CORRIGIDO: Formata a localização como "Cidade - UF"
        holder.binding.tvEnderecoContratante.text = "${pedido.cidade} - ${pedido.estado}"

        // Preenchendo os detalhes do serviço
        holder.binding.tvTipoServico.text = pedido.tipoServico
        holder.binding.tvDescricaoCurta.text = pedido.descricaoServico

        // Configurando o status e o clique
        val statusUpper = pedido.status.uppercase()
        holder.binding.tvStatusPedido.text = statusUpper

        // Muda a cor de fundo do status e controla o clique
        val context = holder.itemView.context
        when (statusUpper) {
            "PENDENTE" -> {
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.atenção))
                holder.binding.tvStatusPedido.isClickable = true
                holder.binding.tvStatusPedido.setOnClickListener {
                    showAcceptDialog(pedido, context)
                }
            }
            "ACEITO" -> {
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.sucesso))
                holder.binding.tvStatusPedido.isClickable = false // Não pode aceitar de novo
            }
            else -> { // Finalizado, Cancelado, etc.
                holder.binding.tvStatusPedido.background.setTint(ContextCompat.getColor(context, R.color.cinza_escuro))
                holder.binding.tvStatusPedido.isClickable = false
            }
        }

        // Listener para ver mais detalhes
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
            .setNegativeButton("Não") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // Lógica para aceitar o pedido (reaproveitada e melhorada)
    private fun aceitarPedido(pedido: Pedidos, context: Context) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val autonomoId = auth.currentUser?.uid

        if (autonomoId.isNullOrEmpty()) {
            // Tratar caso onde o autônomo não está logado
            return
        }

        db.collection("pedido").document(pedido.id).update(
            mapOf(
                "status" to "aceito",
                "autonomo" to autonomoId
            )
        ).addOnSuccessListener {
            // Navega para a tela de sucesso, informando para onde voltar
            val intent = Intent(context, telaNegocioFechado::class.java).apply {
                putExtra("TARGET_FRAGMENT", "AUTONOMO_ACEITOS")
            }
            context.startActivity(intent)
        }.addOnFailureListener {
            // Tratar falha na atualização do banco de dados
        }
    }
}