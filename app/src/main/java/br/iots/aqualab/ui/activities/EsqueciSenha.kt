package br.iots.aqualab.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.iots.aqualab.R
import com.google.android.material.appbar.MaterialToolbar

class EsqueciSenha : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esqueci_senha)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarEsqueciSenha)
        toolbar.setNavigationOnClickListener { finish() }

        val botao_gerar_link = findViewById<Button>(R.id.botaoGerarLink)
        botao_gerar_link.setOnClickListener { irTelaConfirmacaoLink() }

    }

    private fun irTelaConfirmacaoLink()
    {
        val intent = Intent(this, ConfirmacaoLink::class.java)
        startActivity(intent)
    }


}