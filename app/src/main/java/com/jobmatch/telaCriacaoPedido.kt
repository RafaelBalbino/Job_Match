package com.jobmatch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class telaCriacaoPedido : AppCompatActivity() {

    private lateinit var dataServico: EditText
    private lateinit var horarioServico: EditText
    private lateinit var anexoTextView: TextView
    private lateinit var anexoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_tela_criacao_pedido)

        anexoTextView = findViewById(R.id.txt_fotosAnexos)
        anexoButton = findViewById(R.id.btn_anexar)


        anexoButton.setOnClickListener {
            abrirSelecionadorArquivos()
        }


        dataServico = findViewById(R.id.dp_dataServico)

        dataServico.setOnClickListener {
            showDatePicker()
        }

        horarioServico = findViewById(R.id.tp_horarioServico)

        horarioServico.setOnClickListener {
            showTimePicker()
        }

        mostrarTipoServicos()

        mostrarTempoServicos()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        //Processa o URI do ficheiro selecinado
        uri?.let {
            val nomeArquivo = getFileName(it); //Implementa essa função para obter o nome do ficheiro
            anexoTextView.text = nomeArquivo
        }

    }

    private fun abrirSelecionadorArquivos() {
        selectFileLauncher.launch(arrayOf("image/*", "application/pdf")) //Exemplo para imagem ou PDF
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

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val ano = calendar.get(Calendar.YEAR)
        val mes = calendar.get(Calendar.MONTH)
        val dia = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, anoSelecionado, mesSelecionado, diaSelecionado ->
                val dataSelecionada = String.format("%02d/%02d/%d", diaSelecionado, mesSelecionado + 1, anoSelecionado)
                findViewById<EditText>(R.id.dp_dataServico).setText(dataSelecionada)
            },
            ano,
            mes,
            dia
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hora = calendar.get(Calendar.HOUR_OF_DAY)
        val minuto = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, horaSelecionada, minutoSelecionado ->
                val horarioSelecionado = String.format("%02d:%02d", horaSelecionada, minutoSelecionado)
                findViewById<EditText>(R.id.tp_horarioServico).setText(horarioSelecionado)
            },
            hora,
            minuto,
            true //true é para formato 24 horas e false para 12 horas (am/pm)
        )
        timePickerDialog.show()
    }


    private fun mostrarTipoServicos() {
        val tipoServicos = arrayOf(
            "Selecione o tipo de serviço", // Este é o "prompt"
            "Eletricista",
            "Montagem de Computador",
            "Diarista",
            "Professor de Inglês",
            "Montador de Móveis"
        )
        val spinnerTipoServico: Spinner = findViewById(R.id.tipoServico)
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_personalizado,
            tipoServicos
        )

        adapter.setDropDownViewResource(R.layout.spinner_item_personalizado)
        spinnerTipoServico.adapter = adapter

        spinnerTipoServico.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    // Faça algo com o item selecionado
                    Toast.makeText(applicationContext, "Selecionado: $selectedItem", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                TODO("Not yet implemented")
            }
        }
    }

    private fun mostrarTempoServicos() {
        val tempoServicos = arrayOf(
            "Selecione o tempo de serviço", // Este é o "prompt"
            "Turno da manhã (8h - 12h)",
            "Turno da tarde (13h - 17h)",
            "Período integral (8h - 17h)",
            "Horário comercial (9h - 18h)"
        )
        val spinnerTempoServico: Spinner = findViewById(R.id.tempoServico)
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_personalizado,
            tempoServicos
        )

        adapter.setDropDownViewResource(R.layout.spinner_item_personalizado)
        spinnerTempoServico.adapter = adapter

        spinnerTempoServico.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
}
