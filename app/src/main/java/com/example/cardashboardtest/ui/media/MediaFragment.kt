package com.example.cardashboardtest.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cardashboardtest.databinding.FragmentMediaBinding
import com.example.cardashboardtest.model.Song

class MediaFragment : Fragment() {

    private lateinit var mediaViewModel: MediaViewModel
    private var _binding: FragmentMediaBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mediaViewModel = ViewModelProvider(this).get(MediaViewModel::class.java)

        // Observe songs from the ViewModel
        mediaViewModel.songs.observe(viewLifecycleOwner) { songs ->
            // Update UI with songs when data changes
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
