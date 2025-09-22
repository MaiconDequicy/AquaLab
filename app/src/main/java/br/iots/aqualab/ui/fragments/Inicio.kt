package br.iots.aqualab.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Para by viewModels()
import br.iots.aqualab.R // Ainda necessário para o layout R.layout.fragment_inicio
import br.iots.aqualab.databinding.FragmentInicioBinding // Import para ViewBinding
import br.iots.aqualab.ui.viewmodel.InicioViewModel

class Inicio : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val inicioViewModel: InicioViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicioViewModel.welcomeMessage.observe(viewLifecycleOwner) { message ->
            binding.boasVindas.text = message
        }

        inicioViewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

