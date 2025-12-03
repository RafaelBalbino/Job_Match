package com.jobmatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class FragmentContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_container)

        if (savedInstanceState == null) {
            // 1. Lê o NOME do fragmento a ser carregado, enviado pela tela anterior
            val fragmentName = intent.getStringExtra("FRAGMENT_NAME")
            
            // 2. Decide qual classe de fragmento instanciar com base no nome
            val fragment: Fragment? = when (fragmentName) {
                "fragmentListaPedidosContratante" -> fragmentListaPedidosContratante()
                "fragmentListaPedidosAutonomo" -> fragmentListaPedidosAutonomo()
                "fragmentListaAvaliacoes" -> fragmentListaAvaliacoes()
                else -> null // Se o nome for desconhecido, não carrega nada
            }

            fragment?.let { frag ->
                // 3. Pega TODOS os extras do Intent da Activity...
                val arguments = intent.extras
                // ...e os define como os argumentos do Fragmento.
                // Isso passa o "TIPO_QUERY" e o "autonomo_id" para o fragmento correto.
                frag.arguments = arguments

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .commit()
            }
        }
    }
}