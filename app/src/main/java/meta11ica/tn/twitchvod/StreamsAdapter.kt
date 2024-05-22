package meta11ica.tn.twitchvod

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class StreamsAdapter(private val streams: MutableList<Movie>) : RecyclerView.Adapter<StreamsAdapter.StreamViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stream, parent, false)
        return StreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        val stream = streams[position]
        holder.bind(stream)
    }
    fun updateStreamOrder(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(streams, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(streams, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }


    override fun getItemCount(): Int = streams.size

    inner class StreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind stream data to views
        fun bind(stream: Movie) {
            // Implement binding logic
        }
    }
}
