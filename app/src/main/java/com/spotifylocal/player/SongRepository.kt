package com.spotifylocal.player

import android.content.Context
import android.media.MediaPlayer

object SongRepository {

    fun loadSongs(context: Context): List<Song> {
        val prefs = context.getSharedPreferences("song_names", Context.MODE_PRIVATE)
        val list = mutableListOf<Song>()

        try {
            val rawClass = Class.forName("${context.packageName}.R\$raw")
            for (field in rawClass.declaredFields) {
                val resId = field.getInt(null)
                val resName = field.name
                val defaultTitle = formatTitle(resName)
                val customName = prefs.getString("name_$resId", null)
                val title = customName ?: defaultTitle

                val mp = MediaPlayer.create(context, resId)
                val duration = mp.duration.toLong()
                mp.release()

                list.add(Song(resId.toLong(), title, "Artista local", resId, duration))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list.sortedBy { it.title }
    }

    fun renameSong(context: Context, resId: Int, newName: String) {
        context.getSharedPreferences("song_names", Context.MODE_PRIVATE)
            .edit()
            .putString("name_$resId", newName.trim())
            .apply()
    }

    private fun formatTitle(name: String): String {
        return name
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .trim()
    }
}
