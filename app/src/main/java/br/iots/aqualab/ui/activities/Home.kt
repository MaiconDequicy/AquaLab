package br.iots.aqualab.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import br.iots.aqualab.R
import br.iots.aqualab.model.UserRole
import br.iots.aqualab.ui.fragments.Artigos
import br.iots.aqualab.ui.fragments.Inicio
import br.iots.aqualab.ui.fragments.Mapa
import br.iots.aqualab.ui.fragments.Perfil
import br.iots.aqualab.ui.fragments.Admin
import br.iots.aqualab.ui.fragments.CriacaoPontosColeta
import br.iots.aqualab.ui.fragments.InicioResearcher
import br.iots.aqualab.ui.viewmodel.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var bottomNavigation: BottomNavigationView

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigation = findViewById(R.id.bottomNavigationViewHome)

        homeViewModel.userProfile.observe(this) { userProfile ->
            val menuResId = when (userProfile?.role) {
                UserRole.ADMIN -> R.menu.bottom_nav_menu_admin
                UserRole.RESEARCHER -> R.menu.bottom_nav_menu_researcher
                else -> R.menu.bottom_nav_menu
            }
            bottomNavigation.menu.clear()
            bottomNavigation.inflateMenu(menuResId)
            setupBottomNavigationListener()

            if (savedInstanceState == null) {

                bottomNavigation.selectedItemId = R.id.nav_home
            }
        }
    }

    private fun setupBottomNavigationListener() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            Log.d(TAG, "BottomNavigation item selecionado: ${menuItem.title}")
            val fragment = when (menuItem.itemId) {
                R.id.nav_home -> Inicio()
                // Itens do menu comum
                R.id.nav_inicio_research -> InicioResearcher()
                R.id.nav_lupa -> Artigos()
                R.id.nav_mapa -> Mapa()
                // Item exclusivo do menu de admin
                R.id.nav_admin -> Admin()
                // Item comum a todos
                R.id.nav_usuario -> Perfil()

                R.id.nav_pontosColeta -> CriacaoPontosColeta()

                else -> {
                    Log.w(TAG, "Item de menu não tratado: ${menuItem.itemId}")
                    null
                }
            }
            fragment?.let { replaceFragment(it) }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        Log.d(TAG, "replaceFragment chamado para: ${fragment.javaClass.simpleName}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
