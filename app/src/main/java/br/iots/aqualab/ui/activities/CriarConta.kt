package br.iots.aqualab.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.iots.aqualab.databinding.ActivityCriarContaBinding
import br.iots.aqualab.ui.viewmodel.AuthUIState
import br.iots.aqualab.ui.viewmodel.RegisterViewModel

class CriarConta : AppCompatActivity() {

    private val registerViewModel: RegisterViewModel by viewModels()
    private lateinit var binding: ActivityCriarContaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCriarContaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.botaoCadastrar.setOnClickListener {
            val nome = binding.campoNomeCadastro.text.toString().trim()
            val email = binding.campoEmailCadastro.text.toString().trim()
            val senha = binding.campoSenhaCadastro.text.toString().trim()
            val confirmarSenha = binding.campoSenhaCadastroConfirmar.text.toString().trim()

            if (nome.isBlank() || email.isBlank() || senha.isBlank() || confirmarSenha.isBlank()) {
                Toast.makeText(this, "Todos os campos são obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (senha != confirmarSenha) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                binding.campoSenhaCadastroConfirmar.error = "As senhas não coincidem"
                return@setOnClickListener
            } else {
                binding.campoSenhaCadastroConfirmar.error = null
            }
            Log.d("CriarContaActivity", "Chamando registerViewModel.register com email: $email") // LOG
            registerViewModel.register(email, senha, nome)
        }

        binding.linkLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        observeRegistrationState()
    }

    private fun observeRegistrationState() {
        Log.d("CriarContaActivity", "Configurando observer para registrationState.") // LOG
        registerViewModel.registrationState.observe(this) { state ->
            Log.d("CriarContaActivity", "Estado recebido no observer: ${state::class.java.simpleName}") // LOG
            when (state) {
                is AuthUIState.Loading -> {
                    Log.d("CriarContaActivity", "Processando estado Loading.")
                    setLoadingState(true)
                }
                is AuthUIState.Success -> {
                    Log.d("CriarContaActivity", "Processando estado Success. Usuário: ${state.userProfile.displayName}") // LOG
                    try {
                        setLoadingState(false)
                        Toast.makeText(
                            this,
                            "Cadastro realizado com sucesso! Bem-vindo, ${state.userProfile.displayName}. Faça o login.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d("CriarContaActivity", "Toast de sucesso mostrado. Preparando para redirecionar.") // LOG

                        val intent = Intent(this, Login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        Log.d("CriarContaActivity", "Redirecionamento para Login iniciado.") // LOG
                        finish()
                    } catch (e: Exception) {
                        Log.e("CriarContaActivity", "ERRO DENTRO DO BLOCO SUCCESS UI: ", e) // LOG DE ERRO CRÍTICO
                    }
                }
                is AuthUIState.Error -> {
                    Log.d("CriarContaActivity", "Processando estado Error. Mensagem: ${state.message}") // LOG
                    try {
                        setLoadingState(false)
                        Toast.makeText(this, "Erro no cadastro: ${state.message}", Toast.LENGTH_LONG).show()
                        Log.d("CriarContaActivity", "Toast de erro mostrado.") // LOG
                    } catch (e: Exception) {
                        Log.e("CriarContaActivity", "ERRO DENTRO DO BLOCO ERROR UI: ", e)                  }
                }
                is AuthUIState.Idle -> {
                    Log.d("CriarContaActivity", "Processando estado Idle.")
                    setLoadingState(false)
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        Log.d("CriarContaActivity", "setLoadingState chamado com isLoading: $isLoading")
        try {
            binding.campoNomeCadastro.isEnabled = !isLoading
            binding.campoEmailCadastro.isEnabled = !isLoading
            binding.campoSenhaCadastro.isEnabled = !isLoading
            binding.campoSenhaCadastroConfirmar.isEnabled = !isLoading
            binding.botaoCadastrar.isEnabled = !isLoading
            binding.linkLogin.isEnabled = !isLoading

            if (isLoading) {
                binding.botaoCadastrar.text = "Cadastrando..."
            } else {
                binding.botaoCadastrar.text = "Criar Conta"
            }
        } catch (e: Exception) {
            Log.e("CriarContaActivity", "ERRO DENTRO DE setLoadingState: ", e)
        }
    }
}
