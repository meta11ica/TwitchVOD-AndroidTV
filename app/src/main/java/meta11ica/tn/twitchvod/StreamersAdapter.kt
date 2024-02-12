package meta11ica.tn.twitchvod

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class StreamersAdapter(private val streamers: List<String>, private val listener: (String) -> Unit) : RecyclerView.Adapter<StreamersAdapter.MyView>() {


    inner class MyView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.settings_textview)
        var cardView: MaterialCardView = itemView.findViewById(R.id.cardview)

        init {
            // Set click listener on the CardView
            cardView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val streamer = streamers[position]
                    listener(streamer)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyView {

        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_settings_text, parent, false)
        return MyView(itemView)
    }

    override fun onBindViewHolder(holder: MyView, position: Int) {
        val streamer = streamers[position]

        holder.textView.text = streamer

    }

    override fun getItemCount(): Int {
        return streamers.size
    }


    }
