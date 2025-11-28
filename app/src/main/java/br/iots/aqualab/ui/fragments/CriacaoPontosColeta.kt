package br.iots.aqualab.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import br.iots.aqualab.data.database.AppDatabase // Importação do Banco
import br.iots.aqualab.databinding.FragmentCriacaoPontosColetaBinding
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.ui.activities.DetalhesPontoColeta
import br.iots.aqualab.ui.activities.IntegracaoPontoColeta
import br.iots.aqualab.ui.adapter.PontoColetaAdapter
import br.iots.aqualab.ui.viewmodel.CriacaoPontosColetaViewModel
import br.iots.aqualab.ui.viewmodel.CriacaoViewModelFactory

class CriacaoPontosColeta : Fragment() {

    private var _binding: FragmentCriacaoPontosColetaBinding? = null
    private val binding get() = _binding!!

    private val pontosviewModel: CriacaoPontosColetaViewModel by viewModels {
        CriacaoViewModelFactory(AppDatabase.getDatabase(requireContext()).medicaoDao())
    }

    private lateinit var pontoColetaAdapter: PontoColetaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCriacaoPontosColetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        observarViewModel()
        configurarFab()
    }

    private fun configurarFab() {
        binding.fabAdicionarPonto.setOnClickListener {
            val intent = Intent(requireContext(), IntegracaoPontoColeta::class.java)
            startActivity(intent)
        }
    }

    private fun configurarRecyclerView() {
        pontoColetaAdapter = PontoColetaAdapter(emptyList()) { pontoSelecionado ->
            abrirDetalhesDoPonto(pontoSelecionado)
        }

        binding.recyclerViewPontosColeta.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pontoColetaAdapter
        }
    }

    private fun observarViewModel() {
        pontosviewModel.pontosColeta.observe(viewLifecycleOwner) { listaPontos ->
            pontoColetaAdapter.atualizarLista(listaPontos)
        }

        pontosviewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
        }

        pontosviewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun abrirDetalhesDoPonto(ponto: PontoColeta) {
        val intent = Intent(requireActivity(), DetalhesPontoColeta::class.java).apply {
            putExtra("PONTO_COLETA_EXTRA", ponto)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}