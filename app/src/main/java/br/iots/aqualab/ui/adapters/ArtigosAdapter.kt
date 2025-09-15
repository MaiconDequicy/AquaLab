package br.iots.aqualab.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.iots.aqualab.R
import br.iots.aqualab.model.Artigo
import com.google.android.material.button.MaterialButton

class ArtigosAdapter(private val onItemClicked: (Artigo) -> Unit) :
    ListAdapter<Artigo, ArtigosAdapter.ArtigoViewHolder>(ArtigoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtigoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_artigo, parent, false) // Seu layout do item
        return ArtigoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtigoViewHolder, position: Int) {
        val artigo = getItem(position)
        holder.bind(artigo, onItemClicked)
    }

    class ArtigoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tituloTextView: TextView = itemView.findViewById(R.id.textViewArtigoTitulo)
        private val resumoTextView: TextView = itemView.findViewById(R.id.textViewArtigoResumo)
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewArtigo)
        private val leiaMaisButton: MaterialButton = itemView.findViewById(R.id.buttonLeiaMais)

        fun bind(artigo: Artigo, onItemClicked: (Artigo) -> Unit) {
            tituloTextView.text = artigo.titulo
            resumoTextView.text = artigo.resumo
            if (artigo.urlImagem != null) {
                imageView.setBackgroundResource(R.drawable.article)
            } else {
                imageView.setBackgroundResource(R.drawable.article)
            }

            itemView.setOnClickListener { onItemClicked(artigo) }
            leiaMaisButton.setOnClickListener { onItemClicked(artigo) }
        }
    }
}

class ArtigoDiffCallback : DiffUtil.ItemCallback<Artigo>() {
    override fun areItemsTheSame(oldItem: Artigo, newItem: Artigo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Artigo, newItem: Artigo): Boolean {
        return oldItem == newItem
    }
}
