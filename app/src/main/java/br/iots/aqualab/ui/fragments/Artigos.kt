package br.iots.aqualab.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import br.iots.aqualab.databinding.FragmentArtigosBinding
import br.iots.aqualab.model.Artigo
import br.iots.aqualab.ui.adapters.ArtigosAdapter
import br.iots.aqualab.ui.viewmodel.ArtigosViewModel

class Artigos : Fragment() {

    private var _binding: FragmentArtigosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArtigosViewModel by viewModels()

    private lateinit var artigosAdapter: ArtigosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtigosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        artigosAdapter = ArtigosAdapter { artigoClicado ->
            viewModel.onArtigoClicado(artigoClicado)
        }

        binding.recyclerViewArtigos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = artigosAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchViewArtigos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    binding.searchViewArtigos.clearFocus()

                    viewModel.buscarArtigos(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    viewModel.buscarArtigos("")
                }
                return false
            }
        })
    }

    private fun observeViewModel() {
        viewModel.listaDeArtigos.observe(viewLifecycleOwner) { listaDeArtigos ->
            artigosAdapter.submitList(listaDeArtigos)
        }

        viewModel.eventoAbrirLink.observe(viewLifecycleOwner) { url ->
            url?.let { link ->
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.data = android.net.Uri.parse(link)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Não foi possível abrir a notícia", Toast.LENGTH_SHORT).show()
                }
                viewModel.onNavegacaoCompleta()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
