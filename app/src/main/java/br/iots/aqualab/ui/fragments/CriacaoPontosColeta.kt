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
import br.iots.aqualab.R
import br.iots.aqualab.databinding.FragmentCriacaoPontosColetaBinding
import br.iots.aqualab.ui.activities.IntegracaoPontoColeta
import br.iots.aqualab.ui.adapter.PontoColetaAdapter
import br.iots.aqualab.ui.viewmodel.CriacaoPontosColetaViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CriacaoPontosColeta : Fragment() {

    private var _binding: FragmentCriacaoPontosColetaBinding? = null
    private val binding get() = _binding!!
    private val pontosviewModel: CriacaoPontosColetaViewModel by viewModels()

    private lateinit var fabPrincipal: FloatingActionButton
    private lateinit var fabAddManual: FloatingActionButton
    private lateinit var fabImportar: FloatingActionButton

    private var isAllFabsVisible: Boolean = false

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

        fabPrincipal = binding.fabPrincipal
        fabAddManual = binding.fabAddManual
        fabImportar = binding.fabImportar

        fabAddManual.visibility = View.GONE
        fabImportar.visibility = View.GONE
        isAllFabsVisible = false

        fabPrincipal.setOnClickListener {
            if (!isAllFabsVisible) {
                fabPrincipal.setImageResource(R.drawable.close_24)
            } else {
                fabPrincipal.setImageResource(R.drawable.add_24)
            }
            toggleFabMenu()
        }

        fabAddManual.setOnClickListener {
            Toast.makeText(context, "Adicionar manualmente clicado", Toast.LENGTH_SHORT).show()
            toggleFabMenu()
        }

        fabImportar.setOnClickListener {
            val intent = Intent(activity, IntegracaoPontoColeta::class.java)

            startActivity(intent)
        }
    }

    private fun toggleFabMenu() {
        isAllFabsVisible = if (!isAllFabsVisible) {
            fabAddManual.show()
            fabImportar.show()
            true
        } else {
            fabAddManual.hide()
            fabImportar.hide()
            false
        }
    }

    private fun configurarRecyclerView() {
        pontoColetaAdapter = PontoColetaAdapter(emptyList())

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
