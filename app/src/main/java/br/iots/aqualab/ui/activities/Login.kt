package br.iots.aqualab.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.iots.aqualab.R
import com.google.firebase.Firebase

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val link_criar_conta = findViewById<TextView>(R.id.linkSignUp)
        link_criar_conta.setOnClickListener { irTelaCadastro() }

        val botao_entrar = findViewById<TextView>(R.id.botaoEntrar)
        botao_entrar.setOnClickListener { irTelaHome() }

        val text_esqueciSenha =  findViewById<TextView>(R.id.linkEsqueciSenha)
        text_esqueciSenha.setOnClickListener { irTelaEsqueciSenha() }

    }
    private fun irTelaCadastro()
    {
        val intent = Intent(this, CriarConta::class.java)
        startActivity(intent)
    }

    private fun irTelaHome()
    {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
    }

    private fun irTelaEsqueciSenha()
    {
        val intent = Intent(this, EsqueciSenha::class.java)
        startActivity(intent)
    }
}