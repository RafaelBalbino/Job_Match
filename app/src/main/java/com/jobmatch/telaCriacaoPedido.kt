package com.jobmatch

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityTelaCriacaoPedidoBinding

class TelaCriacaoPedido : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriacaoPedidoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaCriacaoPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajusta o padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.Main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupDropdownMenus()
        setupClickListeners()
        populateFieldsFromIntent() // <- NOVA FUNÇÃO CHAMADA AQUI
    }

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        // Processa o URI do ficheiro selecionado
        uri?.let {
            val fileName = getFileName(it)
            // Por enquanto, exibe um Toast. Você pode atualizar um TextView ou uma lista depois.
            Toast.makeText(this, "Anexado: $fileName", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupToolbar() {
        // Define o listener de clique para o botão de voltar
        binding.toolbar.setNavigationOnClickListener {
            // Finaliza a atividade atual e retorna para a anterior
            finish()
        }
    }

    private fun setupDropdownMenus() {
        // Configura o dropdown de Tipo de Serviço
        val serviceTypes = resources.getStringArray(R.array.service_type_options)
        val serviceTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTypes)
        binding.actvTipoServico.setAdapter(serviceTypeAdapter)

        // Configura o dropdown de Tempo de Serviço
        val serviceTimes = resources.getStringArray(R.array.service_time_options)
        val serviceTimeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTimes)
        binding.actvTempoServico.setAdapter(serviceTimeAdapter)
    }

    // NOVA FUNÇÃO PARA PREENCHER OS CAMPOS
    private fun populateFieldsFromIntent() {
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        val serviceDescription = intent.getStringExtra("SERVICE_DESCRIPTION")

        if (!serviceName.isNullOrEmpty()) {
            binding.actvTipoServico.setText(serviceName, false) // `false` para não filtrar a lista
        }

        if (!serviceDescription.isNullOrEmpty()) {
            binding.txtDescricaoServico.setText(serviceDescription)
        }
    }

    private fun setupClickListeners() {
        binding.btnAnexarFotos.setOnClickListener {
            openFileSelector()
        }

        binding.btnEnviarPedido.setOnClickListener {
            // TODO: Implementar a lógica para validar e enviar o pedido
            Toast.makeText(this, "Pedido enviado (lógica a ser implementada)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileSelector() {
        // Exemplo para imagem ou PDF
        selectFileLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        return result ?: uri.lastPathSegment ?: "Desconhecido"
    }
}