package com.spotifylocal.player

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class MusicPlayer private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var songList: List<Song> = emptyList()
        private set
    var currentIndex: Int = -1
    var repeatMode: Int = 1
    var currentlyPlayingResId: Int = -1
        private set

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    val currentSong: Song?
        get() = if (currentlyPlayingResId >= 0) {
            songList.find { it.rawResId == currentlyPlayingResId }
        } else if (currentIndex in songList.indices) {
            songList[currentIndex]
        } else null

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition ?: 0

    val currentDuration: Int
        get() {
            val d = mediaPlayer?.duration ?: -1
            if (d > 0) return d
            return currentSong?.durationMs?.toInt() ?: 1
        }

    fun setSongs(songs: List<Song>) {
        Log.d("MusicPlayer", "setSongs: ${songs.size} canciones")
        songList = songs
    }

    fun playByResId(rawResId: Int) {
        Log.d("MusicPlayer", "playByResId: buscando rawResId=$rawResId en songList (${songList.size} items)")
        val index = songList.indexOfFirst { it.rawResId == rawResId }
        Log.d("MusicPlayer", "playByResId: encontrado en index=$index, song=${songList.getOrNull(index)?.title}")
        if (index >= 0) play(index)
    }

    fun play(index: Int) {
        if (index !in songList.indices) {
            Log.w("MusicPlayer", "play: index $index fuera de rango (tamaño=${songList.size})")
            return
        }
        release()
        currentIndex = index
        val song = songList[index]
        Log.d("MusicPlayer", "play: index=$index, title=${song.title}, rawResId=${song.rawResId}")
        playSong(song)
    }

    private fun playSong(song: Song) {
        Log.d("MusicPlayer", "playSong: ${song.title}, rawResId=${song.rawResId}")
        currentlyPlayingResId = song.rawResId
        mediaPlayer = MediaPlayer.create(context, song.rawResId)
        mediaPlayer?.isLooping = repeatMode == 2
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            Log.d("MusicPlayer", "onCompletion: repeatMode=$repeatMode")
            if (repeatMode == 1) next()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) mp.pause() else mp.start()
        Log.d("MusicPlayer", "togglePlayPause: isPlaying=${mp.isPlaying}")
    }

    fun next() {
        if (songList.isEmpty()) {
            Log.w("MusicPlayer", "next: songList vacía")
            return
        }
        val nextIndex = (currentIndex + 1) % songList.size
        Log.d("MusicPlayer", "next: currentIndex=$currentIndex -> nextIndex=$nextIndex, song=${songList.getOrNull(nextIndex)?.title}")
        play(nextIndex)
    }

    fun previous() {
        if (songList.isEmpty()) {
            Log.w("MusicPlayer", "previous: songList vacía")
            return
        }
        if (currentPosition > 5000) {
            Log.d("MusicPlayer", "previous: restart current song (>5s)")
            seekTo(0)
        } else {
            val prevIndex = if (currentIndex <= 0) songList.size - 1 else currentIndex - 1
            Log.d("MusicPlayer", "previous: currentIndex=$currentIndex -> prevIndex=$prevIndex, song=${songList.getOrNull(prevIndex)?.title}")
            play(prevIndex)
        }
    }

    fun seekTo(msec: Int) {
        mediaPlayer?.seekTo(msec)
    }

    fun applyRepeatMode() {
        mediaPlayer?.isLooping = repeatMode == 2
        mediaPlayer?.setOnCompletionListener {
            if (repeatMode == 1) next()
        }
    }

    fun refreshSongList(context: Context) {
        if (currentlyPlayingResId < 0) return
        val songs = SongRepository.loadSongs(context)
        val newIndex = songs.indexOfFirst { it.rawResId == currentlyPlayingResId }
        if (newIndex >= 0) {
            songList = songs
            currentIndex = newIndex
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        @Volatile
        private var instance: MusicPlayer? = null

        fun getInstance(context: Context): MusicPlayer {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayer(context.applicationContext).also { instance = it }
            }
        }
    }
}
