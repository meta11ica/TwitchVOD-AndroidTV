package meta11ica.tn.twitchvod

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.leanback.widget.Presenter

class TextPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val text = item as String
        val textView = viewHolder.view as TextView
        textView.text = text
        textView.setBackgroundResource(R.color.fastlane_background)
        textView.setOnClickListener {
            // Show a Toast with the text when the TextView is clicked
            Toast.makeText(textView.context, text, Toast.LENGTH_SHORT).show()
        }
        // Enable LinkMovementMethod to make links clickable
        textView.movementMethod = LinkMovementMethod.getInstance()

    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        // Clean up resources if needed
    }

    inner class ViewHolder(view: View) : Presenter.ViewHolder(view)
}