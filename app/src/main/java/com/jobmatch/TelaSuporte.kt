package com.jobmatch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jobmatch.databinding.ActivityTelaSuporteBinding

class TelaSuporte : AppCompatActivity(), SuporteUsuarioAdapter.OnUserActionListener {

    private lateinit var binding: ActivityTelaSuporteBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SuporteUsuarioAdapter
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaSuporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        setupAuthStateListener()
    }

    override fun onStart() {
        super.onStart()
        // Começa a ouvir o estado de autenticação
        authStateListener?.let { auth.addAuthStateListener(it) }
    }

    override fun onStop() {
        super.onStop()
        // Para de ouvir para evitar memory leaks
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // O usuário está 100% logado, agora podemos carregar os dados
                loadUsers()
            } else {
                // O usuário fez logout, podemos redirecionar para o login se necessário
                Log.w("TelaSuporte", "Usuário deslogado, estado do listener mudou.")
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = SuporteUsuarioAdapter(mutableListOf(), this)
        binding.rvUsuariosSuporte.layoutManager = LinearLayoutManager(this)
        binding.rvUsuariosSuporte.adapter = adapter
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                if (!documents.isEmpty) {
                    val allUsers = documents.toObjects(Usuario::class.java)
                    val filteredUsers = allUsers.filter { it.email != "suportejobmatch@email.com" }
                    adapter.submitList(filteredUsers)
                } else {
                    Toast.makeText(this, "Nenhum usuário encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                // Este erro não deve mais acontecer
                Toast.makeText(this, "Erro ao carregar usuários: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("TelaSuporte", "Erro de permissão do Firestore", e)
            }
    }

    // ... (o resto das suas funções onEditUser, onBlockUser, onDeleteUser permanecem iguais)
    override fun onEditUser(user: Usuario) {
        val targetActivity = if (user.autonomo != null) {
            TelaEdicaoPerfilAutonomo::class.java
        } else {
            TelaEdicaoPerfilContratante::class.java
        }
        val intent = Intent(this, targetActivity).apply {
            putExtra("USER_ID", user.uid)
        }
        startActivity(intent)
    }

    override fun onBlockUser(user: Usuario) {
        val newBlockedStatus = !user.isBlocked
        val actionText = if (newBlockedStatus) "bloquear" else "desbloquear"
        val pastParticiple = if (newBlockedStatus) "bloqueado" else "desbloqueado"

        AlertDialog.Builder(this)
            .setTitle("${actionText.replaceFirstChar { it.uppercase() }} Usuário")
            .setMessage("Tem certeza de que deseja $actionText o usuário ${user.nome}?")
            .setPositiveButton("Sim") { _, _ ->
                user.uid?.let {
                    db.collection("users").document(it)
                        .update("isBlocked", newBlockedStatus)
                        .addOnSuccessListener { 
                            Toast.makeText(this, "Usuário $pastParticiple com sucesso!", Toast.LENGTH_SHORT).show()
                            loadUsers() // Recarrega a lista para refletir a mudança
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Falha ao ${actionText} usuário: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onDeleteUser(user: Usuario) {
        AlertDialog.Builder(this)
            .setTitle("Deletar Usuário")
            .setMessage("Esta ação é IRREVERSÍVEL. Tem certeza de que deseja deletar o usuário ${user.nome}? Todos os seus dados serão perdidos.")
            .setPositiveButton("Sim, deletar") { _, _ ->
                user.uid?.let {
                    db.collection("users").document(it).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Usuário deletado com sucesso!", Toast.LENGTH_SHORT).show()
                            loadUsers() // Recarrega a lista para remover o usuário
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Falha ao deletar usuário: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }
}