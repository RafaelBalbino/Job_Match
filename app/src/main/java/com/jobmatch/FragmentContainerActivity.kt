package com.jobmatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class FragmentContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_container)

        if (savedInstanceState == null) {
            val fragmentType = intent.getStringExtra("FRAGMENT_TYPE")
            val fragment = when (fragmentType) {
                "CONTRATANTE" -> fragmentListaPedidosContratante()
                "AUTONOMO" -> fragmentListaPedidosAutonomo()
                else -> null // Ou um fragmento de erro padrão
            }

            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
            }
        }
    }
}