package br.iots.aqualab.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.iots.aqualab.R
import br.iots.aqualab.model.PontoColeta

class PontoColetaAdapter(
    private var pontos: List<PontoColeta>,
    private val onItemClicked: (PontoColeta) -> Unit
) : RecyclerView.Adapter<PontoColetaAdapter.PontoColetaViewHolder>() {

    class PontoColetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nomePonto: TextView = itemView.findViewById(R.id.tv_nome_ponto)
        private val tipoPonto: TextView = itemView.findViewById(R.id.tv_tipo_ponto)

        fun bind(ponto: PontoColeta, onItemClicked: (PontoColeta) -> Unit) {
            nomePonto.text = ponto.nome
            tipoPonto.text = "Tipo: ${ponto.tipo}"

            itemView.setOnClickListener {
                onItemClicked(ponto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PontoColetaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ponto_coleta, parent, false)
        return PontoColetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: PontoColetaViewHolder, position: Int) {
        holder.bind(pontos[position], onItemClicked)
    }
    override fun getItemCount() = pontos.size

    fun atualizarLista(novaLista: List<PontoColeta>) {
        pontos = novaLista
        notifyDataSetChanged()
    }
}
