package br.iots.aqualab.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import br.iots.aqualab.R
import br.iots.aqualab.ui.fragments.Artigos
import br.iots.aqualab.ui.fragments.Inicio
import br.iots.aqualab.ui.fragments.Mapa
import br.iots.aqualab.ui.fragments.Perfil
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationViewHome)
        if (savedInstanceState == null) {
            replaceFragment(Inicio())
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            Log.d(TAG, "BottomNavigation item selecionado: ${menuItem.title}")
            when (menuItem.itemId) {
                R.id.nav_home -> replaceFragment(Inicio())
                R.id.nav_lupa -> replaceFragment(Artigos())
                R.id.nav_mapa -> replaceFragment(Mapa())
                R.id.nav_usuario -> replaceFragment(Perfil())
                else -> {
                    Log.w(TAG, "Item de menu não tratado no BottomNavigationView: ${menuItem.itemId}")

                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        Log.d(TAG, "replaceFragment chamado para: ${fragment.javaClass.simpleName}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}

