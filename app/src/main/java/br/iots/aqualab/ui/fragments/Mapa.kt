package br.iots.aqualab.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.iots.aqualab.R
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.PontoDetalhadoInfo
import br.iots.aqualab.ui.viewmodel.MapaViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class Mapa : Fragment(), OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    private var gMap: GoogleMap? = null
    private val viewModel: MapaViewModel by viewModels()
    private var detalhesDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.PontosDeColeta) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        setupObservers()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap?.uiSettings?.isZoomControlsEnabled = true

        gMap?.setInfoWindowAdapter(this)
        gMap?.setOnInfoWindowClickListener { marker ->
            val ponto = marker.tag as? PontoColeta
            ponto?.let {
                viewModel.onDetalhesRequested(it)
            }
        }

        viewModel.pontosPublicos.value?.let { addMarkersToMap(it) }
    }

    private fun setupObservers() {
        viewModel.pontosPublicos.observe(viewLifecycleOwner) { pontos ->
            gMap?.let { addMarkersToMap(pontos) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
        }

        viewModel.isLoadingDetalhes.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showDialogoDetalhes(null)
            }
        }

        viewModel.detalhesDoPonto.observe(viewLifecycleOwner) { detalhes ->
            detalhes?.let {
                updateDialogoDetalhes(it)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun addMarkersToMap(pontos: List<PontoColeta>) {
        gMap?.clear()
        pontos.forEach { ponto ->
            val position = LatLng(ponto.latitude, ponto.longitude)
            gMap?.addMarker(MarkerOptions().position(position))?.also { marker ->
                marker.tag = ponto
            }
        }
        pontos.firstOrNull()?.let {
            val initialPosition = LatLng(it.latitude, it.longitude)
            gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 12f))
        }
    }

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    override fun getInfoContents(marker: Marker): View {
        val view = layoutInflater.inflate(R.layout.info_window_layout, null)
        val ponto = marker.tag as? PontoColeta

        val nomePonto = view.findViewById<TextView>(R.id.tv_info_nome_ponto)
        val qualidade = view.findViewById<TextView>(R.id.tv_info_qualidade)

        ponto?.let {
            nomePonto.text = "Ponto de Coleta: ${it.nome}"
            qualidade.text = "Status: ${it.status}"
        }
        return view
    }

    private fun showDialogoDetalhes(detalhes: PontoDetalhadoInfo?) {
        detalhesDialog?.dismiss()

        val dialogView = layoutInflater.inflate(R.layout.dialog_ponto_detalhes, null)
        detalhesDialog = Dialog(requireContext()).apply {
            setContentView(dialogView)
            setOnDismissListener { viewModel.onDialogDetalhesDismissed() }
        }
        updateDialogoDetalhes(detalhes)

        detalhesDialog?.show()
    }

    private fun updateDialogoDetalhes(detalhes: PontoDetalhadoInfo?) {
        val dialogView = detalhesDialog?.findViewById<View>(android.R.id.content) ?: return

        val progressBar = dialogView.findViewById<ProgressBar>(R.id.dialog_progressBar)
        val content = dialogView.findViewById<LinearLayout>(R.id.dialog_content)

        if (detalhes != null) {
            progressBar.visibility = View.GONE
            content.visibility = View.VISIBLE

            content.findViewById<TextView>(R.id.dialog_tv_nome_estacao).text = detalhes.nomeEstacao
            content.findViewById<TextView>(R.id.dialog_tv_clima).text = "Clima: ${detalhes.condicoesAtuais}, ${detalhes.temperatura}, ${detalhes.umidade} umidade"
            content.findViewById<TextView>(R.id.dialog_tv_analise).text = detalhes.analiseQualidade
        } else {
            progressBar.visibility = View.VISIBLE
            content.visibility = View.GONE
        }
    }
}