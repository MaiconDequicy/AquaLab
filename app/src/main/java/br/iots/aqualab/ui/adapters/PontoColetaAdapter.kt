package br.iots.aqualab.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.iots.aqualab.databinding.ItemPontoColetaBinding
import br.iots.aqualab.model.PontoColeta

class PontoColetaAdapter(
    private var pontos: List<PontoColeta>
) : RecyclerView.Adapter<PontoColetaAdapter.PontoColetaViewHolder>() {

    inner class PontoColetaViewHolder(val binding: ItemPontoColetaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ponto: PontoColeta) {
            binding.tvNomePonto.text = ponto.nome
            binding.tvTipoPonto.text = ponto.tipo
            //binding.textViewEndereco.text = ponto.endereco
            //binding.textViewStatus.text = ponto.status
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PontoColetaViewHolder {
        val binding = ItemPontoColetaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PontoColetaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PontoColetaViewHolder, position: Int) {
        holder.bind(pontos[position])
    }

    override fun getItemCount(): Int = pontos.size

    fun atualizarLista(novaLista: List<PontoColeta>) {
        pontos = novaLista
        notifyDataSetChanged()
    }
}
