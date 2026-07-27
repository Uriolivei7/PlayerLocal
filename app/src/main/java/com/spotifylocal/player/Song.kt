package com.spotifylocal.player

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val rawResId: Int,
    val durationMs: Long = 0L
)
