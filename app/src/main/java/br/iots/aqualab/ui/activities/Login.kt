package br.iots.aqualab.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import br.iots.aqualab.databinding.ActivityLoginBinding
import br.iots.aqualab.ui.viewmodel.AuthUIState
import br.iots.aqualab.ui.viewmodel.LoginViewModel


class Login : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginViewModel.checkIfUserIsLoggedIn()

        binding.linkSignUp.setOnClickListener { irTelaCadastro() }

        binding.botaoEntrar.setOnClickListener {
            val email = binding.campoEmailLogin.text.toString().trim()
            val senha = binding.campoSenhaLogin.text.toString().trim()
            loginViewModel.login(email, senha)
        }

        binding.linkEsqueciSenha.setOnClickListener { irTelaEsqueciSenha() }
        observeLoginState()

    }

    private fun observeLoginState() {
        loginViewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthUIState.Loading -> {
                    setLoadingState(true)
                }
                is AuthUIState.Success -> {
                    setLoadingState(false)
                    Toast.makeText(
                        this,
                        "Login bem-sucedido! Bem-vindo, ${state.userProfile.displayName ?: state.userProfile.email}",
                        Toast.LENGTH_LONG
                    ).show()
                    irTelaHome()
                }
                is AuthUIState.Error -> {
                    setLoadingState(false)
                    Toast.makeText(this, "Erro: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is AuthUIState.Idle -> {
                    setLoadingState(false)
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.campoEmailLogin.isEnabled = !isLoading
        binding.campoSenhaLogin.isEnabled = !isLoading
        binding.botaoEntrar.isEnabled = !isLoading
        binding.linkSignUp.isEnabled = !isLoading
        binding.linkEsqueciSenha.isEnabled = !isLoading

        if (isLoading) {
            binding.botaoEntrar.text = "Entrando..."
        } else {
            binding.botaoEntrar.text = "Entrar"
        }
    }

    private fun irTelaCadastro()
    {
        val intent = Intent(this, CriarConta::class.java) // Assume que CriarConta é uma Activity
        startActivity(intent)
    }

    private fun irTelaHome()
    {
        val intent = Intent(this, Home::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irTelaEsqueciSenha()
    {
        val intent = Intent(this, EsqueciSenha::class.java) // Assume que EsqueciSenha é uma Activity
        startActivity(intent)
    }
}
