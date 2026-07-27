package com.spotifylocal.player

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.spotifylocal.player.databinding.ActivityPlayerBinding
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var musicPlayer: MusicPlayer
    private var isSeeking = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateSeekBar()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        musicPlayer = MusicPlayer.getInstance(this)
        setupViews()
        updateUI()
        updateRepeatIcon()
        handler.post(updateProgressRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateProgressRunnable)
        super.onDestroy()
    }

    private fun applyTheme() {
        val isDark = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("theme", "dark") == "dark"
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        binding.tvPlayerTitle.setOnClickListener {
            val song = musicPlayer.currentSong ?: return@setOnClickListener
            val input = android.widget.EditText(this).apply {
                setText(song.title)
                setSelection(song.title.length)
            }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Renombrar canción")
                .setView(input)
                .setPositiveButton("Guardar") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        SongRepository.renameSong(this, song.rawResId, newName)
                        musicPlayer.refreshSongList(this)
                        updateUI()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnPlayerPlayPause.setOnClickListener {
            musicPlayer.togglePlayPause()
            updatePlayPauseIcon()
            MusicService.startService(this)
        }

        binding.btnPlayerNext.setOnClickListener {
            musicPlayer.next()
            MusicService.startService(this)
            updateUI()
        }

        binding.btnPlayerPrevious.setOnClickListener {
            musicPlayer.previous()
            MusicService.startService(this)
            updateUI()
        }

        binding.btnRepeat.setOnClickListener {
            musicPlayer.repeatMode = (musicPlayer.repeatMode + 1) % 3
            musicPlayer.applyRepeatMode()
            updateRepeatIcon()
        }

        binding.btnPlaylist.setOnClickListener {
            showPlaylistDialog()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar) {
                isSeeking = false
                musicPlayer.seekTo(seekBar.progress)
            }
        })
    }

    private fun showPlaylistDialog() {
        val songs = SongRepository.loadSongs(this)
        val bgColor = ContextCompat.getColor(this, R.color.spotify_dark)
        val textColor = ContextCompat.getColor(this, R.color.spotify_white)
        val cardBg = ContextCompat.getColor(this, R.color.spotify_dark_card)
        val accentColor = ContextCompat.getColor(this, R.color.spotify_green)
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp12 = (12 * resources.displayMetrics.density).toInt()
        val dp16 = (16 * resources.displayMetrics.density).toInt()
        val dp24 = (24 * resources.displayMetrics.density).toInt()

        val dialog = BottomSheetDialog(this)
        val scrollView = android.widget.ScrollView(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp16)
            setBackgroundColor(bgColor)
        }

        val titleView = android.widget.TextView(this).apply {
            text = "Lista de canciones"
            setTextColor(textColor)
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp4, dp4, dp4, dp16)
        }
        layout.addView(titleView)

        for (i in songs.indices) {
            val song = songs[i]
            val isCurrent = i == musicPlayer.currentIndex

            val card = MaterialCardView(this).apply {
                radius = dp8.toFloat()
                cardElevation = 0f
                setCardBackgroundColor(cardBg)
                setContentPadding(dp12, dp12, dp12, dp12)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp8) }
                setOnClickListener {
                    musicPlayer.setSongs(songs)
                    musicPlayer.play(i)
                    MusicService.startService(this@PlayerActivity)
                    updateUI()
                    dialog.dismiss()
                }
            }

            val innerLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val numberView = android.widget.TextView(this).apply {
                text = "${i + 1}"
                setTextColor(if (isCurrent) accentColor else ContextCompat.getColor(this@PlayerActivity, R.color.spotify_gray))
                textSize = 12f
                setPadding(0, 0, dp12, 0)
            }
            innerLayout.addView(numberView)

            val textLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
            }

            val title = android.widget.TextView(this).apply {
                text = song.title
                setTextColor(if (isCurrent) accentColor else textColor)
                textSize = 14f
                typeface = if (isCurrent) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                maxLines = 1
            }
            textLayout.addView(title)

            val artist = android.widget.TextView(this).apply {
                text = song.artist
                setTextColor(ContextCompat.getColor(this@PlayerActivity, R.color.spotify_gray))
                textSize = 12f
                maxLines = 1
            }
            textLayout.addView(artist)
            innerLayout.addView(textLayout)

            if (isCurrent) {
                val playingBadge = android.widget.TextView(this).apply {
                    text = "▶"
                    setTextColor(accentColor)
                    textSize = 14f
                    setPadding(dp8, 0, 0, 0)
                }
                innerLayout.addView(playingBadge)
            }

            card.addView(innerLayout)
            layout.addView(card)
        }

        scrollView.addView(layout)
        dialog.setContentView(scrollView)
        dialog.show()
    }

    private fun updateUI() {
        val song = musicPlayer.currentSong
        if (song != null) {
            binding.tvPlayerTitle.text = song.title
            binding.tvPlayerArtist.text = song.artist
            binding.seekBar.max = musicPlayer.duration.coerceAtLeast(1)
            binding.tvTotalTime.text = formatTime(musicPlayer.duration.toLong())
        }
        updateNextUp()
        updatePlayPauseIcon()
    }

    private fun updateNextUp() {
        val songs = musicPlayer.songList
        if (songs.isEmpty() || musicPlayer.currentIndex < 0) {
            binding.tvNextUp.text = ""
            return
        }
        val nextIndex = (musicPlayer.currentIndex + 1) % songs.size
        val nextSong = songs.getOrNull(nextIndex)
        val currentSong = musicPlayer.currentSong
        binding.tvNextUp.text = if (nextSong != null && nextSong != currentSong) {
            "Siguiente: ${nextSong.title}"
        } else {
            ""
        }
    }

    private fun updateSeekBar() {
        if (!isSeeking) {
            binding.seekBar.progress = musicPlayer.currentPosition
            binding.tvCurrentTime.text = formatTime(musicPlayer.currentPosition.toLong())
        }
    }

    private fun updatePlayPauseIcon() {
        val icon = if (musicPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.btnPlayerPlayPause.setImageResource(icon)
    }

    private fun updateRepeatIcon() {
        val icon = when (musicPlayer.repeatMode) {
            0 -> R.drawable.ic_repeat
            2 -> R.drawable.ic_repeat_one
            else -> R.drawable.ic_repeat
        }
        binding.btnRepeat.setImageResource(icon)
        val alpha = if (musicPlayer.repeatMode == 0) 0.4f else 1.0f
        binding.btnRepeat.alpha = alpha
    }

    private fun formatTime(ms: Long): String {
        val min = TimeUnit.MILLISECONDS.toMinutes(ms)
        val sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return "%d:%02d".format(min, sec)
    }
}
