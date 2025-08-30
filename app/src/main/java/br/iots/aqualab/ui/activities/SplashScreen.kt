package br.iots.aqualab.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import br.iots.aqualab.R

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val handler = android.os.Handler(Looper.getMainLooper())

        handler.postDelayed(
            {
                irTelaLogin()
            }, 2000)
    }

    private fun irTelaLogin()
    {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}