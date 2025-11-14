package com.jobmatch

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    inner class ContratanteViewHolder(val binding: ItemPedidoContratanteBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AutonomoViewHolder(val binding: ItemPedidoAutonomoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (userType == "CONTRATANTE" || userType == "AUTONOMO_ACEITOS") TYPE_CONTRATANTE else TYPE_AUTONOMO
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
            val contratanteHolder = holder as ContratanteViewHolder
            bindContratanteView(contratanteHolder, pedido)
        } else {
            val autonomoHolder = holder as AutonomoViewHolder
            bindAutonomoView(autonomoHolder, pedido)
        }
    }

    private fun bindContratanteView(holder: ContratanteViewHolder, pedido: Pedidos) {
        val db = FirebaseFirestore.getInstance()
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

        holder.itemView.setOnClickListener {
            val intent = if (userType == "AUTONOMO_ACEITOS") {
                Intent(holder.itemView.context, TelaMeuPerfil::class.java).apply {
                    putExtra("USER_ID", pedido.contratanteId)
                }
            } else {
                Intent(holder.itemView.context, TelaPerfilAutonomo::class.java).apply {
                    putExtra("AUTONOMO_ID", pedido.autonomo)
                }
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    private fun bindAutonomoView(holder: AutonomoViewHolder, pedido: Pedidos) {
        holder.binding.tvNomeContratante.text = pedido.nomeSolicitacao
        holder.binding.tvTipoServico.text = pedido.tipoServico
        holder.binding.tvDescricaoCurta.text = pedido.descricaoServico
        holder.binding.tvTelefone.text = pedido.telefoneSolicitante
        holder.binding.tvCidadeEstado.text = "${pedido.cidade} - ${pedido.estado}"

        holder.binding.btnAceitarPedido.visibility = View.VISIBLE
        holder.binding.btnAceitarPedido.setOnClickListener {
            aceitarPedido(pedido, holder.itemView.context)
        }
    }

    private fun aceitarPedido(pedido: Pedidos, context: Context) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val autonomoId = auth.currentUser?.uid ?: return

        db.collection("pedidos").document(pedido.id).update(
            mapOf("status" to "aceito", "autonomo" to autonomoId)
        ).addOnSuccessListener {
            val intent = Intent(context, telaNegocioFechado::class.java)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = pedidos.size

    fun updateData(newPedidos: List<Pedidos>) {
        pedidos = newPedidos.toMutableList()
        notifyDataSetChanged()
    }
}