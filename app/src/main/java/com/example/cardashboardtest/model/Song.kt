package com.example.cardashboardtest.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: String? = null,
    val filePath: String? = null
) {
    companion object {
        // Sample data for testing
        fun getSampleSongs(): List<Song> {
            return listOf(
                Song(1, "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354000),
                Song(2, "Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", 482000),
                Song(3, "Hotel California", "Eagles", "Hotel California", 390000),
                Song(4, "Sweet Child O' Mine", "Guns N' Roses", "Appetite for Destruction", 356000),
                Song(5, "Smells Like Teen Spirit", "Nirvana", "Nevermind", 301000)
            )
        }
    }
}
