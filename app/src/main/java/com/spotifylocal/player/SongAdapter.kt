package com.spotifylocal.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spotifylocal.player.databinding.ItemSongBinding
import java.util.concurrent.TimeUnit

class SongAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onSongLongClick: ((Song) -> Unit)? = null
) : ListAdapter<Song, SongAdapter.SongViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SongViewHolder(
        private val binding: ItemSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            binding.tvSongName.text = song.title
            binding.tvArtistName.text = song.artist
            binding.tvDuration.text = formatDuration(song.durationMs)
            binding.root.setOnClickListener { onSongClick(song) }
            binding.root.setOnLongClickListener {
                onSongLongClick?.invoke(song)
                true
            }
        }

        private fun formatDuration(ms: Long): String {
            val min = TimeUnit.MILLISECONDS.toMinutes(ms)
            val sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
            return "%d:%02d".format(min, sec)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }
}
