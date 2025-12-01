package com.jobmatch

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jobmatch.databinding.ItemUsuarioSuporteBinding

class SuporteUsuarioAdapter(
    private var userList: List<Usuario>,
    private val listener: OnUserActionListener
) : RecyclerView.Adapter<SuporteUsuarioAdapter.UserViewHolder>() {

    // Interface para comunicar os cliques dos botões de volta para a Activity
    interface OnUserActionListener {
        fun onBlockUser(user: Usuario)
        fun onDeleteUser(user: Usuario)
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
            if (user.isBlocked) {
                // Se estiver bloqueado, o ícone fica verde (indicando ação de "desbloquear")
                btnBlockUser.setColorFilter(Color.GREEN)
            } else {
                // Se não estiver bloqueado, o ícone fica laranja (indicando ação de "bloquear")
                btnBlockUser.setColorFilter(Color.parseColor("#FFA500")) // Laranja
            }

            // Configura os cliques dos botões, repassando para a Activity
            btnBlockUser.setOnClickListener { listener.onBlockUser(user) }
            btnDeleteUser.setOnClickListener { listener.onDeleteUser(user) }
        }
    }

    override fun getItemCount() = userList.size

    // Função para atualizar a lista de usuários quando novos dados forem carregados
    fun updateUsers(newUsers: List<Usuario>) {
        userList = newUsers
        notifyDataSetChanged()
    }
}