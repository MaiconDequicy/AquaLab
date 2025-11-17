package br.iots.aqualab.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.iots.aqualab.R
import com.google.android.material.appbar.MaterialToolbar

class LancamentoManual : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lancamento_manual)


        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarLancamentoManual)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
}