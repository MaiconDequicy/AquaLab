package br.iots.aqualab.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.iots.aqualab.R

class ConfirmacaoLink : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacao_link)

        val botao_continuar = findViewById<Button>(R.id.botaoContinuarConfirmacao)
        botao_continuar.setOnClickListener { irTelaLogin() }
    }

    private fun irTelaLogin()
    {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}