package br.iots.aqualab.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.iots.aqualab.R
import br.iots.aqualab.model.PontoColeta
import br.iots.aqualab.model.PontoDetalhadoInfo
import br.iots.aqualab.ui.viewmodel.MapaViewModel
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

class Mapa : Fragment(), OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    private var gMap: GoogleMap? = null
    private val viewModel: MapaViewModel by viewModels()
    private var detalhesDialog: Dialog? = null
    private var legendaDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carrega o mapa dinamicamente dentro do card
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)

        // Botão legenda
        view.findViewById<ImageButton>(R.id.btn_legenda).setOnClickListener {
            showDialogoLegenda()
        }

        setupObservers()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap?.uiSettings?.isZoomControlsEnabled = true

        gMap?.setInfoWindowAdapter(this)
        gMap?.setOnInfoWindowClickListener { marker ->
            val ponto = marker.tag as? PontoColeta
            ponto?.let { viewModel.onDetalhesRequested(it) }
        }

        viewModel.pontosPublicos.value?.let { addMarkersToMap(it) }
    }

    private fun setupObservers() {
        viewModel.pontosPublicos.observe(viewLifecycleOwner) { pontos ->
            if (gMap != null) addMarkersToMap(pontos)
        }

        viewModel.isLoadingDetalhes.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showDialogoDetalhes(null)
        }

        viewModel.detalhesDoPonto.observe(viewLifecycleOwner) { detalhes ->
            detalhes?.let { updateDialogoDetalhes(it) }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun addMarkersToMap(pontos: List<PontoColeta>) {
        gMap?.clear()

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

            gMap?.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(BitmapDescriptorFactory.defaultMarker(cor))
            )?.also { it.tag = ponto }
        }
    }

    override fun getInfoWindow(marker: Marker): View? = null

    override fun getInfoContents(marker: Marker): View {
        val view = layoutInflater.inflate(R.layout.info_window_layout, null)
        val ponto = marker.tag as? PontoColeta

        view.findViewById<TextView>(R.id.tv_info_nome_ponto).text =
            "Ponto de Coleta: ${ponto?.nome ?: ""}"

        view.findViewById<TextView>(R.id.tv_info_qualidade).text =
            "Qualidade: ${ponto?.classificacao ?: ""}"

        return view
    }

    private fun showDialogoDetalhes(detalhes: PontoDetalhadoInfo?) {
        if (detalhesDialog?.isShowing == true) {
            updateDialogoDetalhes(detalhes)
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_ponto_detalhes, null)
        detalhesDialog = Dialog(requireContext()).apply {
            setContentView(dialogView)
            setOnDismissListener { viewModel.onDialogDetalhesDismissed() }
        }

        updateDialogoDetalhes(detalhes)
        detalhesDialog?.show()
    }

    private fun updateDialogoDetalhes(detalhes: PontoDetalhadoInfo?) {
        val dialog = detalhesDialog ?: return

        val progress = dialog.findViewById<ProgressBar>(R.id.dialog_progressBar)
        val content = dialog.findViewById<LinearLayout>(R.id.dialog_content)

        if (detalhes != null) {
            progress.visibility = View.GONE
            content.visibility = View.VISIBLE

            dialog.findViewById<TextView>(R.id.dialog_tv_nome_estacao).text =
                detalhes.nomeEstacao

            dialog.findViewById<TextView>(R.id.dialog_tv_clima).text =
                "Clima: ${detalhes.condicoesAtuais}, ${detalhes.temperatura}, ${detalhes.umidade} umidade"

            dialog.findViewById<TextView>(R.id.dialog_tv_analise).text =
                detalhes.analiseQualidade

        } else {
            progress.visibility = View.VISIBLE
            content.visibility = View.GONE
        }
    }

    private fun showDialogoLegenda() {
        if (legendaDialog == null) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_legenda_qualidade, null)

            legendaDialog = Dialog(requireContext()).apply {
                setContentView(dialogView)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

            setupLegendaItem(dialogView.findViewById(R.id.legenda_otima),
                R.drawable.circle_blue,
                "Ótima",
                "Água em condições ideais, própria para consumo e lazer.")

            setupLegendaItem(dialogView.findViewById(R.id.legenda_boa),
                R.drawable.circle_green,
                "Boa",
                "Condições favoráveis para a vida aquática e para a maioria dos usos. Baixo risco.")

            setupLegendaItem(dialogView.findViewById(R.id.legenda_regular),
                R.drawable.circle_yellow,
                "Regular",
                "Sinais de alteração. Requer atenção e monitoramento.")

            setupLegendaItem(dialogView.findViewById(R.id.legenda_ruim),
                R.drawable.circle_orange,
                "Ruim",
                "Qualidade comprometida por poluição. Não recomendada para consumo ou contato direto.")

            setupLegendaItem(dialogView.findViewById(R.id.legenda_pessima),
                R.drawable.circle_red,
                "Péssima",
                "Níveis críticos de contaminação. Evite qualquer tipo de contato.")

            dialogView.findViewById<Button>(R.id.btn_entendi).setOnClickListener {
                legendaDialog?.dismiss()
            }

        }

        legendaDialog?.show()
    }

    private fun setupLegendaItem(view: View, colorRes: Int, title: String, description: String) {
        view.findViewById<View>(R.id.legenda_cor).setBackgroundResource(colorRes)
        view.findViewById<TextView>(R.id.legenda_titulo).text = title
        view.findViewById<TextView>(R.id.legenda_descricao).text = description
    }
}
