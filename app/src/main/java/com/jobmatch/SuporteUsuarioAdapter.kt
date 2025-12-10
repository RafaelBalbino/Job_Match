package com.jobmatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jobmatch.databinding.ItemUsuarioSuporteBinding

class SuporteUsuarioAdapter(
    private var userList: MutableList<Usuario> = mutableListOf(),
    private val listener: OnUserActionListener
) : RecyclerView.Adapter<SuporteUsuarioAdapter.UserViewHolder>() {

    // Interface para comunicar os cliques dos botões de volta para a Activity
    interface OnUserActionListener {
        fun onBlockUser(user: Usuario)
        fun onDeleteUser(user: Usuario)
        fun onEditUser(user: Usuario)
    }

    inner class UserViewHolder(val binding: ItemUsuarioSuporteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUsuarioSuporteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.binding.apply {
            // Preenche os dados do usuário
            tvUserName.text = user.nome
            tvUserEmail.text = user.email
            ivUserAvatar.load(user.fotoUrl) {
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
            }

            // Altera a cor do ícone de bloqueio com base no status do usuário
            val context = holder.itemView.context
            if (user.isBlocked) {
                // Se estiver bloqueado, o ícone fica verde (indicando ação de "desbloquear")
                btnBlockUser.setColorFilter(ContextCompat.getColor(context, R.color.status_aceito))
            } else {
                // Se não estiver bloqueado, o ícone fica amarelo (indicando ação de "bloquear")
                btnBlockUser.setColorFilter(ContextCompat.getColor(context, R.color.status_pendente))
            }

            // Configura os cliques dos botões, repassando para a Activity
            btnEditUser.setOnClickListener { listener.onEditUser(user) }
            btnBlockUser.setOnClickListener { listener.onBlockUser(user) }
            btnDeleteUser.setOnClickListener { listener.onDeleteUser(user) }
        }
    }

    override fun getItemCount() = userList.size

    // Função para atualizar a lista de usuários de forma segura
    fun submitList(newUsers: List<Usuario>) {
        userList.clear()
        userList.addAll(newUsers)
        notifyDataSetChanged()
    }
}