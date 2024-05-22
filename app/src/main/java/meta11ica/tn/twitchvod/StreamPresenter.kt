package meta11ica.tn.twitchvod

import android.graphics.drawable.Drawable
import androidx.leanback.widget.Presenter
import androidx.core.content.ContextCompat
import android.view.ViewGroup

import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains a CustomImageCardView.
 */
class StreamPresenter(cardWidth: Int = DEFAULT_CARD_WIDTH, cardHeight: Int = DEFAULT_CARD_HEIGHT)
    : Presenter() {
    private val mCardWidth = cardWidth
    private val mCardHeight = cardHeight

    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor = ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)

        val cardView = object : CustomImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this, selected)
                super.setSelected(selected)
            }
        }
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        updateCardBackgroundColor(cardView, false)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val stream = item as Movie
        val cardView = viewHolder.view as CustomImageCardView

        if (stream.cardImageUrl != null) {
            if (stream.progress != null) {
                cardView.setProgress(stream.progress!!)
            }
            else {
                cardView.setProgress(0)
            }
            cardView.setTitle(stream.title!!)
            if(stream.description!=null) {
                cardView.setContent(stream.description!!)
            }
            else cardView.setContent(stream.title!!)

            if (stream.duration!! >0){
                val hours = stream.duration!! / 3600
                val minutes = (stream.duration!! % 3600) / 60
                val seconds = stream.duration!! % 60
                cardView.setDuration(String.format("%02d:%02d:%02d", hours, minutes, seconds))

            } else {cardView.removeDuration()
            }
            cardView.setCardDimensions(mCardWidth, mCardHeight)
            cardView.loadMainImage(stream.cardImageUrl!!, mDefaultCardImage!!)
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val cardView = viewHolder.view as CustomImageCardView
        // Clear references to images to free up memory
        cardView.clearMainImage()
    }

    private fun updateCardBackgroundColor(view: CustomImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Set background color
        //view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)

    }

    companion object {
        private val TAG = "StreamPresenter"

        private const val DEFAULT_CARD_WIDTH = 274
        private const val DEFAULT_CARD_HEIGHT = 210
    }
}
