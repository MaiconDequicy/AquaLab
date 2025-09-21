package br.iots.aqualab.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.* // NECESSÁRIO para Menu, MenuInflater, MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.iots.aqualab.R // Importe o R do seu projeto
import br.iots.aqualab.databinding.FragmentPerfilBinding
import br.iots.aqualab.ui.activities.Login
import br.iots.aqualab.ui.viewmodel.PerfilUIState
import br.iots.aqualab.ui.viewmodel.PerfilViewModel

class Perfil : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val perfilViewModel: PerfilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        perfilViewModel.perfilState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PerfilUIState.LogoutSuccess -> {
                    Toast.makeText(requireContext(), "Você saiu.", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
                is PerfilUIState.Error -> {
                    Toast.makeText(requireContext(), "Erro: ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {  }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.acoes_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.acao_sair -> {
                perfilViewModel.logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToLogin() {
        activity?.let {
            val intent = Intent(it, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            it.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
