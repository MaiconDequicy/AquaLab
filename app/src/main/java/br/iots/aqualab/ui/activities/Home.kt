package br.iots.aqualab.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import br.iots.aqualab.R
import br.iots.aqualab.ui.fragments.Artigos
import br.iots.aqualab.ui.fragments.Inicio
import br.iots.aqualab.ui.fragments.Mapa
import br.iots.aqualab.ui.fragments.Perfil
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.widget.Toolbar // Para androidx.appcompat.widget.Toolbar


class Home : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val toolbar = findViewById<Toolbar>(R.id.toolbarPerfil)
        setSupportActionBar(toolbar)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationViewHome)

        if (savedInstanceState == null) {
            replaceFragment(Inicio())
        }

        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> replaceFragment(Inicio())
                R.id.nav_lupa -> replaceFragment(Artigos())
                R.id.nav_mapa -> replaceFragment(Mapa())
                R.id.nav_usuario -> replaceFragment(Perfil())
                else -> {  }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

