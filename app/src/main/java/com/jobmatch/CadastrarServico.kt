package com.jobmatch

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jobmatch.databinding.ActivityCadastrarServicoBinding

class CadastrarServico : AppCompatActivity() {
    private val binding: ActivityCadastrarServicoBinding by lazy {
        ActivityCadastrarServicoBinding.inflate(layoutInflater)
    }
    //Variável para armazenar URI da foto
    private var fotoUri: String? = null


    //Função para chamar foto da galeria
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null){
            fotoUri = uri.toString() //Armazena URI como string
            Toast.makeText(this, "Foto anexada com sucesso!", Toast.LENGTH_SHORT).show()
        } else{
            Toast.makeText(this, "Seleção de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    //Função para coletar, validar e salvar os dados do serviço
    private fun salvarServico() {
        val nomeServico = binding.txtNomeServico.text.toString().trim()
        val descricaoServico = binding.txtDescricaoNegocio.text.toString().trim()
        val categoria = binding.txtCategoriaServico.text.toString()
        //spinner para Modelo de Cobrança
        val modeloCobranca = binding.spinnerModeloCobranca.selectedItem.toString()
        val precoStr = binding.txtPreco.text.toString()
        //Já está armazenado na variável global da Activity
        val fotoUri = fotoUri ?: "" //Se for nulo, envia string vazia

        //--------------------------------------------------------------------------------
        //Validação dos dados
        if (nomeServico.isEmpty() || descricaoServico.isEmpty() || precoStr.isEmpty()){
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        val descricaoCompleta = "$descricaoServico | Preço: $precoStr | Modelo: $modeloCobranca"

        //Instanciar o Objeto Serviço
        val novoServico = Servico(
            nomeServico = nomeServico,
            descricaoServico = descricaoCompleta,
            categoria = categoria,
            modeloCobranca = modeloCobranca,
            fotoServico = fotoUri
        )

        val preco: Double
        try {
            preco = precoStr.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Por favor, insira um preço válido.", Toast.LENGTH_SHORT).show()
            return
        }

        val mensagem = "Serviço '$nomeServico', Preço: R$$preco, Possui foto?: ${if(fotoUri != null) "Sim" else "Não"}, Modelo de Cobrança: '$modeloCobranca'"
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()

        //Enviar o Objeto de volta para Tela de Perfil do Autonomo
        val resultIntent = Intent().apply {
            putExtra("NOVO_SERVICO", novoServico)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun mostrarTipoCobranca(){
        val tipoCobrancas = arrayOf(
            "Selecione o tipo de cobrança",
            "Por Hora",
            "Preço Fixo",
            "Por Diária",
            "Por Unidade",
            "A Combinar"
        )
        val spinnerCobrancas: Spinner = findViewById(R.id.spnModeloCobranca)
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_personalizado,
            tipoCobrancas
        )

        adapter.setDropDownViewResource(R.layout.spinner_item_personalizado)
        spinnerCobrancas.adapter = adapter

        spinnerCobrancas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    // Faça algo com o item selecionado
                    Toast.makeText(applicationContext, "Selecionado: $selectedItem", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ação quando nada é selecionado
                // Este método fica vazio, pois nenhuma ação é necessária se a seleção for nula.
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)


        //função do botão voltar
        binding.btnVoltarPerfilAutonomo2.setOnClickListener {
            finish()
        }

        //Botão Anexar foto
        binding.btnAnexo.setOnClickListener {
            // Lógica para anexar a foto
            pickImageLauncher.launch("image/*")
        }

        binding.btnSalvarServico.setOnClickListener {
            // Lógica para salvar o serviço
            salvarServico()
            }
        //Aplica os insets da barra de status
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.spnModeloCobranca)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        }
}