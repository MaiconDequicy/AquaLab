package br.iots.aqualab.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.iots.aqualab.databinding.ItemRequestBinding
import br.iots.aqualab.model.UserProfile

class AdminAdapter(
    private val onApproveClicked: (UserProfile) -> Unit,
    private val onRejectClicked: (UserProfile) -> Unit
) : ListAdapter<UserProfile, AdminAdapter.RequestViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, onApproveClicked, onRejectClicked)
    }

    class RequestViewHolder(private val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserProfile, onApprove: (UserProfile) -> Unit, onReject: (UserProfile) -> Unit) {
            binding.tvUserName.text = user.displayName
            binding.tvUserEmail.text = user.email
            binding.btnApprove.setOnClickListener { onApprove(user) }
            binding.btnReject.setOnClickListener { onReject(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem == newItem
        }
    }
}