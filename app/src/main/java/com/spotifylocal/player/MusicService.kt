package com.spotifylocal.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "music_playback"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "com.spotifylocal.player.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.spotifylocal.player.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.spotifylocal.player.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.spotifylocal.player.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, MusicService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, MusicService::class.java).apply { this.action = action }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private lateinit var musicPlayer: MusicPlayer
    private var mediaSession: MediaSession? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            progressHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        musicPlayer = MusicPlayer.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        setupMediaSession()
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressRunnable)
        mediaSession?.isActive = false
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                musicPlayer.togglePlayPause()
                updateNotification()
            }
            ACTION_NEXT -> {
                musicPlayer.next()
                updateNotification()
            }
            ACTION_PREVIOUS -> {
                musicPlayer.previous()
                updateNotification()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                if (intent?.action == null) {
                    updateNotification()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @android.annotation.SuppressLint("NewApi")
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reproducción de música",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controla la reproducción de música"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "MusicService")
        mediaSession?.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                musicPlayer.togglePlayPause()
                updateNotification()
            }
            override fun onPause() {
                musicPlayer.togglePlayPause()
                updateNotification()
            }
            override fun onSkipToNext() {
                musicPlayer.next()
                updateNotification()
            }
            override fun onSkipToPrevious() {
                musicPlayer.previous()
                updateNotification()
            }
        })
        mediaSession?.isActive = true
    }

    private fun buildNotification(): Notification {
        val song = musicPlayer.currentSong
        val isPlaying = musicPlayer.isPlaying

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) "Pausar" else "Reproducir"

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE },
            pendingFlags
        )
        val prevIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS },
            pendingFlags
        )
        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).apply { action = ACTION_NEXT },
            pendingFlags
        )
        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicService::class.java).apply { action = ACTION_STOP },
            pendingFlags
        )

        val openAppIntent = PendingIntent.getActivity(
            this, 5,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            pendingFlags
        )

        val style = Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(song?.title ?: "Desconocido")
            .setContentText(song?.artist ?: "Sin reproducción")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent)
            .setStyle(style)
            .addAction(R.drawable.ic_previous, "Anterior", prevIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(R.drawable.ic_next, "Siguiente", nextIntent)
            .setOngoing(isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updatePlaybackState() {
        val state = if (musicPlayer.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val pbState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_STOP
            )
            .setState(state, musicPlayer.currentPosition.toLong(), 1.0f)
            .build()
        mediaSession?.setPlaybackState(pbState)
    }

    private fun updateNotification() {
        if (musicPlayer.isPlaying) {
            updatePlaybackState()
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            progressHandler.removeCallbacks(progressRunnable)
            progressHandler.post(progressRunnable)
        } else {
            progressHandler.removeCallbacks(progressRunnable)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
}
