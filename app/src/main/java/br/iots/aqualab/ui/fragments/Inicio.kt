package br.iots.aqualab.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.iots.aqualab.R
import br.iots.aqualab.databinding.FragmentInicioBinding
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.ui.viewmodel.InicioViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView

class Inicio : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private var googleMapHome: GoogleMap? = null
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

        inicioViewModel.loadUserProfile()
        inicioViewModel.carregarArtigosRecentes()

        inicioViewModel.carregarPontosDoMapa()

        setupObservers()
        setupListeners()

        setupMap()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }
    override fun onMapReady(map: GoogleMap) {
        googleMapHome = map

        map.uiSettings.apply {
            isScrollGesturesEnabled = false
            isZoomGesturesEnabled = false
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = false
            isMapToolbarEnabled = false
        }

        val manaus = LatLng(-3.1190275, -60.0217314)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(manaus, 10f))

        map.setOnMapClickListener {
            trocarParaAbaMapas()
        }
        inicioViewModel.pontosMapaHome.value?.let { pontos ->
            adicionarMarcadores(pontos)
        }
    }

    private fun setupObservers() {
        inicioViewModel.welcomeMessage.observe(viewLifecycleOwner) { message ->
            binding.boasVindas.text = message
        }

        inicioViewModel.artigosRecentes.observe(viewLifecycleOwner) { artigos ->
            binding.itemPraSubstituirArtigo.isVisible = false
            binding.itemPraSubstituirArtigo2.isVisible = false

            if (artigos.isNotEmpty()) {
                binding.itemPraSubstituirArtigo.text = "• ${artigos[0].titulo}"
                binding.itemPraSubstituirArtigo.isVisible = true

                if (artigos.size >= 2) {
                    binding.itemPraSubstituirArtigo2.text = "• ${artigos[1].titulo}"
                    binding.itemPraSubstituirArtigo2.isVisible = true
                }
            } else {
                binding.itemPraSubstituirArtigo.text = "Nenhuma notícia recente no momento."
                binding.itemPraSubstituirArtigo.isVisible = true
            }
        }

        inicioViewModel.pontosMapaHome.observe(viewLifecycleOwner) { pontos ->
            if (googleMapHome != null) {
                adicionarMarcadores(pontos)
            }
        }
    }

    private fun adicionarMarcadores(pontos: List<PontoColeta>) {
        googleMapHome?.clear()

        pontos.forEach { ponto ->
            val position = LatLng(ponto.latitude, ponto.longitude)

            val cor = when (ponto.classificacao) {
                "Ótima" -> BitmapDescriptorFactory.HUE_BLUE
                "Boa" -> BitmapDescriptorFactory.HUE_GREEN
                "Regular" -> BitmapDescriptorFactory.HUE_YELLOW
                "Ruim" -> BitmapDescriptorFactory.HUE_ORANGE
                "Péssima" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_VIOLET
            }

            googleMapHome?.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(BitmapDescriptorFactory.defaultMarker(cor))
            )
        }
    }

    private fun setupListeners() {
        binding.btnDetalhesArtigos.setOnClickListener {
            trocarParaAbaArtigos()
        }
        binding.cardMapas.setOnClickListener {
            trocarParaAbaMapas()
        }
    }

    private fun trocarParaAbaArtigos() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationViewHome)
        bottomNav?.selectedItemId = R.id.nav_lupa // ID da aba de Artigos
    }

    private fun trocarParaAbaMapas() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationViewHome)
        bottomNav?.selectedItemId = R.id.nav_mapa
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}