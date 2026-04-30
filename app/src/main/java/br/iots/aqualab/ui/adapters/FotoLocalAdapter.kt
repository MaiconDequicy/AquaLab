package br.iots.aqualab.ui.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.iots.aqualab.R
import br.iots.aqualab.databinding.ItemFotoLocalBinding
import br.iots.aqualab.model.FotoLocal
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Adapter para exibir lista de fotos locais em RecyclerView
 */
class FotoLocalAdapter(
    private val onEditComment: (FotoLocal, String) -> Unit,
    private val onDelete: (FotoLocal) -> Unit,
    private val onClick: (FotoLocal) -> Unit
) : ListAdapter<FotoLocal, FotoLocalAdapter.FotoViewHolder>(FotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val binding = ItemFotoLocalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FotoViewHolder(
        private val binding: ItemFotoLocalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(foto: FotoLocal) {
            // Carregar imagem usando Glide
            Glide.with(binding.root.context)
                .load(foto.getFile())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(binding.ivFoto)

            // Timestamp
            binding.tvTimestamp.text = foto.getFormattedTimestamp()

            // Comentário
            if (foto.comentario.isNotBlank()) {
                binding.tvComentario.text = foto.comentario
            } else {
                binding.tvComentario.text = "Sem comentário"
                binding.tvComentario.setTextColor(
                    binding.root.context.getColor(R.color.light_gray)
                )
            }

            // Click na foto para visualizar em tela cheia
            binding.ivFoto.setOnClickListener {
                onClick(foto)
            }

            // Botão editar comentário
            binding.btnEditComment.setOnClickListener {
                showEditCommentDialog(foto)
            }

            // Botão deletar
            binding.btnDelete.setOnClickListener {
                showDeleteConfirmation(foto)
            }
        }

        private fun showEditCommentDialog(foto: FotoLocal) {
            val context = binding.root.context
            val editText = EditText(context).apply {
                setText(foto.comentario)
                hint = "Digite um comentário sobre a foto..."
                setPadding(40, 40, 40, 40)
            }

            AlertDialog.Builder(context)
                .setTitle("Editar Comentário")
                .setView(editText)
                .setPositiveButton("Salvar") { _, _ ->
                    val newComment = editText.text.toString().trim()
                    onEditComment(foto, newComment)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun showDeleteConfirmation(foto: FotoLocal) {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle("Excluir Foto")
                .setMessage("Deseja realmente excluir esta foto? Esta ação não pode ser desfeita.")
                .setPositiveButton("Excluir") { _, _ ->
                    onDelete(foto)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    class FotoDiffCallback : DiffUtil.ItemCallback<FotoLocal>() {
        override fun areItemsTheSame(oldItem: FotoLocal, newItem: FotoLocal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FotoLocal, newItem: FotoLocal): Boolean {
            return oldItem == newItem
        }
    }
}
