package br.iots.aqualab.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.iots.aqualab.R
import br.iots.aqualab.databinding.FragmentPerfilBinding
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.model.UserRole
import br.iots.aqualab.ui.activities.Login
import br.iots.aqualab.ui.viewmodel.PerfilUIState
import br.iots.aqualab.ui.viewmodel.PerfilViewModel
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Perfil : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val perfilViewModel: PerfilViewModel by viewModels()

    companion object {
        private const val TAG = "PerfilFragment"
    }

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

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbarPerfil)
        (activity as? AppCompatActivity)?.supportActionBar?.title = ""

        binding.toolbarPerfil.setNavigationOnClickListener {
            Log.d(TAG, "Botão de navegação (voltar) da toolbarPerfil clicado.")
            parentFragmentManager.popBackStack()
        }

        perfilViewModel.perfilState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PerfilUIState.LogoutSuccess -> {
                    Toast.makeText(requireContext(), "Você saiu", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
                is PerfilUIState.Error -> {
                    Toast.makeText(requireContext(), "Erro: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is PerfilUIState.UserProfileLoaded -> {
                }
                is PerfilUIState.Idle -> {
                }
            }
        }

        //Observa os dados do perfil do usuário
        perfilViewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            Log.d(TAG, "userProfile LiveData OBSERVED. UserProfile: $userProfile")
            if (userProfile != null) {
                updateUIWithUserProfile(userProfile)
            } else {
                binding.textoNomeUsuario.text = "Usuário Desconhecido"
                binding.textoTipoUsuario.text = "N/A"
                binding.textoEmailUsuario.text = "N/A"
                Glide.with(this)
                    .load(R.drawable.perfil)
                    .circleCrop()
                    .into(binding.framaLTPerfil.findViewById(R.id.imageViewPerfil))
            }
        }
    }

    private fun updateUIWithUserProfile(userProfile: UserProfile) {
        Log.d(TAG, "updateUIWithUserProfile: Atualizando UI com dados: $userProfile")
        binding.textoNomeUsuario.text = userProfile.displayName ?: "Nome não disponível"
        binding.textoEmailUsuario.text = userProfile.email ?: "Email não disponível"

        val userTypeString = when (userProfile.role) {
            UserRole.COMMON -> "Usuário Comum"
            UserRole.RESEARCHER -> "Pesquisador"
            else -> "Tipo Desconhecido"
        }
        binding.textoTipoUsuario.text = userTypeString

        val imageView = binding.framaLTPerfil.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.imageViewPerfil) // Certifique-se que o ID é este ou ajuste

        if (imageView != null) {
            if (!userProfile.photoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(userProfile.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.perfil)
                    .error(R.drawable.perfil)
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(R.drawable.perfil)
                    .circleCrop()
                    .into(imageView)
            }
        } else {
            Log.e(TAG, "updateUIWithUserProfile: ShapeableImageView com ID 'imageViewPerfil' não encontrada dentro de 'framaLTPerfil'")
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "onCreateOptionsMenu chamado. Inflating menu R.menu.acoes_toolbar na toolbarPerfil")
        inflater.inflate(R.menu.acoes_toolbar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected chamado. Item ID: ${item.itemId}, Título: ${item.title}")
        return when (item.itemId) {
            R.id.acao_sair -> {
                Log.d(TAG, "Item 'acao_sair' selecionado. Mostrando diálogo de confirmação")
                showLogoutConfirmationDialog()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        Log.d(TAG, "showLogoutConfirmationDialog: Criando diálogo.")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Saída")
            .setMessage("Você deseja mesmo sair da sua conta?")
            .setNegativeButton("Cancelar") { dialog, _ ->
                Log.d(TAG, "Logout cancelado pelo usuário.")
                dialog.dismiss()
            }
            .setPositiveButton("Sair") { dialog, _ ->
                Log.d(TAG, "Logout confirmado. Chamando perfilViewModel.logout().")
                try {
                    perfilViewModel.logout()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao chamar perfilViewModel.logout() do diálogo", e)
                    Toast.makeText(requireContext(), "Erro ao tentar sair.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun navigateToLogin() {
        Log.d(TAG, "navigateToLogin: Tentando navegar para LoginActivity.")
        activity?.let {
            val intent = Intent(it, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            try {
                startActivity(intent)
                it.finish()
                Log.d(TAG, "navigateToLogin: LoginActivity iniciada e HomeActivity finalizada.")
            } catch (e: Exception) {
                Log.e(TAG, "navigateToLogin: Erro ao iniciar LoginActivity ou finalizar HomeActivity.", e)
                Toast.makeText(requireContext(), "Erro ao navegar para tela de login.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.e(TAG, "navigateToLogin: Activity é null, não foi possível redirecionar.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: Binding zerado.")
    }
}
