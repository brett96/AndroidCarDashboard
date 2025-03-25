package com.example.cardashboardtest.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cardashboardtest.databinding.FragmentNavigationBinding

class NavigationFragment : Fragment() {

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NavigationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[NavigationViewModel::class.java]
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up map placeholder
        viewModel.mapImageResource.observe(viewLifecycleOwner) { resource ->
            binding.mapPlaceholder.setImageResource(resource)
        }
        
        // Set up destination text
        viewModel.destination.observe(viewLifecycleOwner) { destination ->
            binding.destinationText.text = destination
        }
        
        // Set up ETA text
        viewModel.eta.observe(viewLifecycleOwner) { eta ->
            binding.etaText.text = eta
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
