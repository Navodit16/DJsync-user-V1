package com.sushii.djsync_user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class songAdapter(private val onItemClick: (songList) -> Unit) : RecyclerView.Adapter<songAdapter.SongViewHolder>() {

    private val songList = mutableListOf<songList>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.spotify_search_list, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songList[position])
    }

    override fun getItemCount(): Int = songList.size

    fun setSongList(newSongs: List<songList>) {
        songList.clear()
        songList.addAll(newSongs)
        notifyDataSetChanged()
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(song: songList) {
            itemView.findViewById<TextView>(R.id.songName).text = song.songName
            itemView.findViewById<TextView>(R.id.artistName).text = song.artistName
            val albumImageView = itemView.findViewById<ImageView>(R.id.albumImage)
            Glide.with(albumImageView.context).load(song.albumImageUrl).into(albumImageView)

            // Set onClickListener to pass song details when clicked
            itemView.setOnClickListener {
                onItemClick(song)
            }
        }
    }
}
