package com.spotifylocal.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.spotifylocal.player.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var musicPlayer: MusicPlayer
    private lateinit var adapter: SongAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updatePlayerUI()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        musicPlayer = MusicPlayer.getInstance(this)
        setupRecyclerView()
        setupPlayerControls()
        loadSongs()
        updateThemeIcon()
    }

    override fun onResume() {
        super.onResume()
        updatePlayerUI()
        handler.post(updateProgressRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateProgressRunnable)
    }

    private fun applyTheme() {
        val isDark = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("theme", "dark") == "dark"
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(
            onSongClick = { song ->
                val index = adapter.currentList.indexOf(song)
                musicPlayer.setSongs(adapter.currentList)
                musicPlayer.play(index)
                MusicService.startService(this)
                startActivity(Intent(this, PlayerActivity::class.java))
            },
            onSongLongClick = { song ->
                showRenameDialog(song)
            }
        )
        binding.rvSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupPlayerControls() {
        binding.btnPlayPause.setOnClickListener {
            musicPlayer.togglePlayPause()
            updatePlayPauseIcon()
            MusicService.startService(this)
        }
        binding.btnNext.setOnClickListener {
            musicPlayer.next()
            MusicService.startService(this)
            updatePlayerUI()
        }
        binding.btnPrevious.setOnClickListener {
            musicPlayer.previous()
            MusicService.startService(this)
            updatePlayerUI()
        }
        binding.playerBackground.setOnClickListener {
            if (musicPlayer.currentSong != null) {
                startActivity(Intent(this, PlayerActivity::class.java))
            }
        }
        binding.btnThemeToggle.setOnClickListener {
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val isDark = prefs.getString("theme", "dark") == "dark"
            prefs.edit().putString("theme", if (isDark) "light" else "dark").apply()
            recreate()
        }
    }

    private fun showRenameDialog(song: Song) {
        val input = EditText(this).apply {
            setText(song.title)
            setSelection(song.title.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Renombrar canción")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    SongRepository.renameSong(this, song.rawResId, newName)
                    loadSongs()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadSongs() {
        musicPlayer.refreshSongList(this)
        val songs = SongRepository.loadSongs(this)
        if (songs.isEmpty()) {
            Toast.makeText(this, R.string.no_songs_warning, Toast.LENGTH_LONG).show()
            return
        }
        adapter.submitList(songs)
        updatePlayerUI()
    }

    private fun updatePlayerUI() {
        val song = musicPlayer.currentSong
        binding.playerBackground.visibility = android.view.View.VISIBLE
        if (song != null) {
            binding.tvNowPlaying.text = song.title
            binding.tvNowPlayingArtist.text = song.artist
        } else {
            binding.tvNowPlaying.text = "Desconocido"
            binding.tvNowPlayingArtist.text = "Sin reproducción"
        }
        updatePlayPauseIcon()
    }

    private fun updatePlayPauseIcon() {
        val icon = if (musicPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.btnPlayPause.setImageResource(icon)
    }

    private fun updateThemeIcon() {
        val isDark = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("theme", "dark") == "dark"
        binding.btnThemeToggle.setImageResource(R.drawable.ic_theme)
    }
}
