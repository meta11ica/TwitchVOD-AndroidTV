package com.example.twitchvod

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class StreamersAdapter(private val streamers: List<String>) : RecyclerView.Adapter<StreamersAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val streamer = streamers[position]
        holder.bind(streamer)
    }

    override fun getItemCount(): Int {
        return streamers.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(streamer: String) {
            textView.text = streamer
        }

        override fun onClick(view: View?) {
            view?.let {
                val streamer = streamers[adapterPosition]
                Toast.makeText(it.context, "Clicked on $streamer", Toast.LENGTH_SHORT).show()
            }
        }
    }
}