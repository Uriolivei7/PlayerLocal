package com.spotifylocal.player

import android.content.Context
import android.media.MediaPlayer

class MusicPlayer private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var songList: List<Song> = emptyList()
        private set
    var currentIndex: Int = -1
    var repeatMode: Int = 1

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    val currentSong: Song?
        get() = if (currentIndex in songList.indices) songList[currentIndex] else null

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition ?: 0

    val duration: Int
        get() = mediaPlayer?.duration ?: 0

    fun setSongs(songs: List<Song>) {
        songList = songs
    }

    fun play(index: Int) {
        if (index !in songList.indices) return
        release()
        currentIndex = index
        val song = songList[index]
        mediaPlayer = MediaPlayer.create(context, song.rawResId)
        mediaPlayer?.isLooping = repeatMode == 2
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            if (repeatMode == 1) next()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) mp.pause() else mp.start()
    }

    fun next() {
        if (songList.isEmpty()) return
        val nextIndex = (currentIndex + 1) % songList.size
        play(nextIndex)
    }

    fun previous() {
        if (songList.isEmpty()) return
        if (currentPosition > 5000) {
            seekTo(0)
        } else {
            val prevIndex = if (currentIndex <= 0) songList.size - 1 else currentIndex - 1
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
        val currentRawResId = currentSong?.rawResId ?: return
        val songs = SongRepository.loadSongs(context)
        val newIndex = songs.indexOfFirst { it.rawResId == currentRawResId }
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
