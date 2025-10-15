package br.iots.aqualab.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.iots.aqualab.R
import br.iots.aqualab.databinding.FragmentPerfilBinding
import br.iots.aqualab.model.RequestStatus
import br.iots.aqualab.model.UserProfile
import br.iots.aqualab.model.UserRole
import br.iots.aqualab.ui.activities.Login
import br.iots.aqualab.ui.viewmodel.PerfilUIState
import br.iots.aqualab.ui.viewmodel.PerfilViewModel
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Perfil : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val perfilViewModel: PerfilViewModel by viewModels()

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestStoragePermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var pickImageFromGalleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    private var currentPhotoUri: Uri? = null

    companion object {
        private const val TAG = "PerfilFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Para o menu de logout
        initializeActivityResults()
    }

    private fun initializeActivityResults() {
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d(TAG, "Permissão da Câmera concedida.")
                    openCamera()
                } else {
                    Log.d(TAG, "Permissão da Câmera negada.")
                    Toast.makeText(requireContext(), "Permissão da câmera negada.", Toast.LENGTH_SHORT).show()
                }
            }

        requestStoragePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.entries.all { it.value }
                if (allGranted) {
                    Log.d(TAG, "Permissões de Armazenamento concedidas.")
                    openGallery()
                } else {
                    Log.d(TAG, "Uma ou mais permissões de Armazenamento foram negadas.")
                    Toast.makeText(requireContext(), "Permissão para acessar a galeria negada.", Toast.LENGTH_SHORT).show()
                }
            }

        pickImageFromGalleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        Log.d(TAG, "Imagem selecionada da galeria: $uri")
                        currentPhotoUri = uri

                        perfilViewModel.uploadProfileImage(uri)

                    }
                } else {
                    Log.d(TAG, "Seleção de imagem da galeria cancelada ou falhou.")
                }
            }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
                if (success) {
                    val photoUri = currentPhotoUri
                    if (photoUri != null) {
                        Log.d(TAG, "Foto tirada com sucesso. URI: $photoUri")

                        perfilViewModel.uploadProfileImage(photoUri)

                    } else {
                        Log.e(TAG, "currentPhotoUri é null após tirar foto, mesmo com sucesso.")
                        Toast.makeText(requireContext(), "Erro ao obter a foto capturada.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "Captura de foto cancelada ou falhou.")
                }
            }
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
            parentFragmentManager.popBackStack()
        }

        binding.fabEditarImagem.setOnClickListener {
            Log.d(TAG, "FAB Editar Imagem clicado.")
            showImageSourceDialog()
        }

        binding.botaoSolicitarAcesso.setOnClickListener {
            perfilViewModel.requestResearcherRole()
        }

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
                    .into(binding.imageViewPerfil)
            }
        }

        perfilViewModel.perfilState.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "perfilState OBSERVED. Novo estado: ${state::class.java.simpleName}")


            when (state) {
                is PerfilUIState.LogoutSuccess -> {
                    Log.d(TAG, "Estado LogoutSuccess recebido. Navegando para login...")
                    Toast.makeText(requireContext(), "Você saiu.", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
                is PerfilUIState.Error -> {
                    Log.d(TAG, "Estado Error recebido: ${state.message}")
                    Toast.makeText(requireContext(), "Erro: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is PerfilUIState.UserProfileLoaded -> {
                    Log.d(TAG, "Estado UserProfileLoaded recebido (via perfilState): ${state.userProfile}")

                    updateUIWithUserProfile(state.userProfile)
                    Toast.makeText(requireContext(), "Foto de perfil atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                }
                is PerfilUIState.RoleRequestSuccess -> {
                    Log.d(TAG, "Estado RoleRequestSuccess recebido.")
                    Toast.makeText(requireContext(), "Solicitação de acesso enviada com sucesso!", Toast.LENGTH_SHORT).show()
                }
                is PerfilUIState.Idle -> {
                    Log.d(TAG, "Estado Idle recebido")
                }

                is PerfilUIState.Loading -> {
                    Log.d(TAG, "Estado Loading recebido")
                }

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
            UserRole.ADMIN -> "Administrador"
        }
        binding.textoTipoUsuario.text = userTypeString

        if (!userProfile.photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(userProfile.photoUrl)
                .circleCrop()
                .placeholder(R.drawable.perfil)
                .error(R.drawable.perfil)
                .into(binding.imageViewPerfil)
        } else {
            Glide.with(this)
                .load(R.drawable.perfil)
                .circleCrop()
                .into(binding.imageViewPerfil)
        }

        when {
            userProfile.role == UserRole.RESEARCHER || userProfile.role == UserRole.ADMIN -> {
                binding.cardAcessoPesquisador.visibility = View.GONE
            }
            userProfile.roleRequestStatus == RequestStatus.PENDING -> {
                binding.cardAcessoPesquisador.visibility = View.VISIBLE
                binding.botaoSolicitarAcesso.isEnabled = false
                binding.botaoSolicitarAcesso.text = "Solicitação Pendente"
            }
            userProfile.roleRequestStatus == RequestStatus.REJECTED -> {
                binding.cardAcessoPesquisador.visibility = View.VISIBLE
                binding.botaoSolicitarAcesso.isEnabled = true
                binding.botaoSolicitarAcesso.text = "Solicitar Novamente"
            }
            else -> { // COMMON user, no pending/rejected request
                binding.cardAcessoPesquisador.visibility = View.VISIBLE
                binding.botaoSolicitarAcesso.isEnabled = true
                binding.botaoSolicitarAcesso.text = "Solicitar"
            }
        }
    }


    private fun showImageSourceDialog() {
        val options = arrayOf("Tirar Foto", "Escolher da Galeria", "Cancelar")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Alterar Foto de Perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Tirar Foto
                        Log.d(TAG, "Opção 'Tirar Foto' selecionada.")
                        checkCameraPermissionAndOpenCamera()
                    }
                    1 -> { // Escolher da Galeria
                        Log.d(TAG, "Opção 'Escolher da Galeria' selecionada.")
                        checkStoragePermissionAndOpenGallery()
                    }
                    2 -> { // Cancelar
                        Log.d(TAG, "Opção 'Cancelar' selecionada.")
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Permissão da Câmera já concedida. Abrindo câmera.")
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {

                Log.d(TAG, "Mostrando rationale para permissão da Câmera.")

                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                Log.d(TAG, "Solicitando permissão da Câmera.")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermissionAndOpenGallery() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            Log.d(TAG, "Permissões de armazenamento já concedidas. Abrindo galeria.")
            openGallery()
        } else {
            Log.d(TAG, "Solicitando permissões de armazenamento.")
            requestStoragePermissionLauncher.launch(permissionsToRequest)
        }
    }


    private fun openGallery() {
        Log.d(TAG, "Abrindo a galeria.")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        try {
            pickImageFromGalleryLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Nenhuma aplicação de galeria encontrada.", e)
            Toast.makeText(requireContext(), "Não foi possível abrir a galeria.", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir == null) {
            throw IOException("Diretório de armazenamento externo não está disponível.")
        }

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("createImageFile", "Falha ao criar diretório: $storageDir")
                throw IOException("Falha ao criar diretório de armazenamento: $storageDir")
            }
        }

        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefixo */
            ".jpg",               /* sufixo */
            storageDir            /* diretório */
        )
    }


    private fun openCamera() {
        Log.d(TAG, "Tentando abrir a câmera.")
        val context = requireContext()
        try {
            val photoFile: File = createImageFile(context)

            val photoURI: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            currentPhotoUri = photoURI

            takePictureLauncher.launch(photoURI)
            Log.d(TAG, "URI do arquivo da foto para o launcher da câmera: $photoURI")

        } catch (ex: IOException) {
            Log.e(TAG, "Erro ao criar arquivo para foto.", ex)
            Toast.makeText(context, "Erro ao preparar câmera.", Toast.LENGTH_SHORT).show()
            currentPhotoUri = null
        } catch (ex: ActivityNotFoundException) {
            Log.e(TAG, "Nenhuma aplicação de câmera encontrada.", ex)
            Toast.makeText(context, "Câmera não disponível.", Toast.LENGTH_SHORT).show()
            currentPhotoUri = null
        } catch (ex: IllegalArgumentException) {
            Log.e(TAG, "Erro ao obter URI para o arquivo (FileProvider?). Verifique a configuração.", ex)
            Toast.makeText(context, "Erro ao preparar câmera (URI).", Toast.LENGTH_SHORT).show()
            currentPhotoUri = null
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
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
                perfilViewModel.logout()
            }
            .setCancelable(true)
            .show()
    }

    private fun navigateToLogin() {
        Log.d(TAG, "navigateToLogin: Tentando navegar para LoginActivity.")
        activity?.let {
            val intent = Intent(it, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            it.finish()
            Log.d(TAG, "navigateToLogin: LoginActivity iniciada e Activity atual finalizada.")
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