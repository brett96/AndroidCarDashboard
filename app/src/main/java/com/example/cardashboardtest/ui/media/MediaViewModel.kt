package com.example.cardashboardtest.ui.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cardashboardtest.model.Song

class MediaViewModel : ViewModel() {
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    private val _currentSong = MutableLiveData<Song>()
    val currentSong: LiveData<Song> = _currentSong

    init {
        // Load sample songs for testing
        _songs.value = Song.getSampleSongs()
        _currentSong.value = Song.getSampleSongs().firstOrNull()
    }
}
