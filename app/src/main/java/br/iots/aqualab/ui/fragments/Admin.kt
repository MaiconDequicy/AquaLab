package br.iots.aqualab.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import br.iots.aqualab.databinding.FragmentAdminBinding
import br.iots.aqualab.ui.adapter.AdminAdapter
import br.iots.aqualab.ui.viewmodel.AdminUIState
import br.iots.aqualab.ui.viewmodel.AdminViewModel

class Admin : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private val adminViewModel: AdminViewModel by viewModels()
    private lateinit var adminAdapter: AdminAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        adminViewModel.fetchPendingRequests()
    }

    private fun setupRecyclerView() {
        adminAdapter = AdminAdapter(
            onApproveClicked = { user -> adminViewModel.approveRequest(user) },
            onRejectClicked = { user -> adminViewModel.rejectRequest(user) }
        )
        binding.rvRequests.apply {
            adapter = adminAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModel() {
        adminViewModel.adminState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AdminUIState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvNoRequests.visibility = View.GONE
                    binding.rvRequests.visibility = View.GONE
                }
                is AdminUIState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.users.isEmpty()) {
                        binding.tvNoRequests.visibility = View.VISIBLE
                        binding.rvRequests.visibility = View.GONE
                    } else {
                        binding.tvNoRequests.visibility = View.GONE
                        binding.rvRequests.visibility = View.VISIBLE
                        adminAdapter.submitList(state.users)
                    }
                }
                is AdminUIState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoRequests.visibility = View.VISIBLE
                    binding.tvNoRequests.text = "Erro ao carregar solicitações"
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                is AdminUIState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoRequests.visibility = View.GONE
                    binding.rvRequests.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}