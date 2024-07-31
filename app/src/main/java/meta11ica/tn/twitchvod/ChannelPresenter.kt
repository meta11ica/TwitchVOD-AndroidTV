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
class ChannelPresenter(cardWidth: Int = DEFAULT_CARD_WIDTH, cardHeight: Int = DEFAULT_CARD_HEIGHT)
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
        val channel = item as Channel
        val cardView = viewHolder.view as CustomImageCardView
        cardView.setProgress(0)
        cardView.removeDuration()
        if (channel.profileImageURL != null) {
            cardView.setTitle(channel.displayName!!)
            if (channel.description != null) {
                cardView.setContent(channel.description!!)
            } else cardView.setContent(channel.followersCount.toString())

            cardView.setCardDimensions(mCardWidth, mCardHeight)
            cardView.loadMainImage(channel.profileImageURL!!, mDefaultCardImage!!)
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
