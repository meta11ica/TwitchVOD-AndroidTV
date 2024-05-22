package meta11ica.tn.twitchvod

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.animation.LinearInterpolator
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.leanback.widget.BaseCardView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlin.math.max


open class CustomImageCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseCardView(context, attrs, defStyleAttr) {

    val mainImage: ImageView
    private val titleScrollView: HorizontalScrollView
    private val contentScrollView: HorizontalScrollView
    private val titleTextView: TextView
    private val contentTextView: TextView
    private val durationTextView: TextView
    private val progressBar: ProgressBar
    private val infoField: RelativeLayout
    private var titleAnimator: ObjectAnimator = ObjectAnimator()
    private var contentAnimator: ObjectAnimator = ObjectAnimator()

    init {
        inflate(context, R.layout.view_custom_image_card, this)
        mainImage = findViewById(R.id.main_image)
        infoField = findViewById(R.id.info_field)
        titleTextView = findViewById(R.id.title_text)
        contentTextView = findViewById(R.id.content_text)
        durationTextView = findViewById(R.id.duration_text)
        titleScrollView = findViewById(R.id.title_scroll)
        contentScrollView = findViewById(R.id.content_scroll)
        progressBar = findViewById(R.id.bar_progress)

        // Set up focus change listeners
        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val titleScrollToX = titleTextView.width + titleTextView.text.length
                val contentScrollToX =contentTextView.width + contentTextView.text.length

                    // Smoothly scroll title text view to top
                    titleAnimator =
                        ObjectAnimator.ofInt(titleScrollView, "scrollX", titleScrollToX).apply {
                            interpolator = LinearInterpolator()
                            setDuration(max(titleScrollToX.toLong() * 12,0))
                            repeatMode = ValueAnimator.RESTART
                            repeatCount = 2

                        }

                    contentAnimator =
                        ObjectAnimator.ofInt(contentScrollView, "scrollX", contentScrollToX).apply {
                            interpolator = LinearInterpolator()
                            setDuration(max(contentScrollToX.toLong() * 12,0))
                            repeatMode = ValueAnimator.RESTART
                            repeatCount = 2
                        }
                    titleAnimator.start()
                    contentAnimator.start()

            } else {
                titleAnimator.cancel()
                contentAnimator.cancel()
                // Restore default scroll positions when losing focus
                val defaultTitleScrollPosition =
                    titleScrollView.getTag(R.id.title_scroll) as? Int ?: 0
                val defaultContentScrollPosition =
                    contentScrollView.getTag(R.id.content_scroll) as? Int ?: 0
                titleScrollView.scrollTo(defaultTitleScrollPosition, 0)
                contentScrollView.scrollTo(defaultContentScrollPosition, 0)
            }


        }
    }


    fun setDuration(duration: String) {
        durationTextView.text = duration
    }
    fun removeDuration() {
        durationTextView.visibility = TextView.GONE
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setContent(content: String) {
        contentTextView.text = content
    }

    fun setCardDimensions(width: Int, height: Int) {
        val lpMainImage = mainImage.layoutParams
        val lpInfoField = infoField.layoutParams
        val lpProgressBar = progressBar.layoutParams
        val lpCardLayout = this.layoutParams
        lpProgressBar.width = width
        //progressBar.layoutParams = lpProgressBar
        lpMainImage.width = width
        lpMainImage.height = height - 48
        lpInfoField.width = width
        lpMainImage.width = width
        lpCardLayout.width = width
        this.minimumWidth = width
        this.layoutParams = lpCardLayout

    }

    fun loadMainImage(imageUrl: String, placeholder: Drawable) {
        val mDefaultCardImage = ContextCompat.getDrawable(context, R.drawable.movie)

        Glide.with(context)
            .load(imageUrl)
            .centerCrop()
            .apply(RequestOptions().placeholder(placeholder))
            .error(mDefaultCardImage)
            .into(mainImage)
    }
    fun clearMainImage() {
        mainImage.setImageDrawable(null)
    }

    fun setProgress(progress: Int) {
        progressBar.progress = progress
    }


    fun setInfoAreaBackgroundColor(color: Int) {
        this.setBackgroundColor(color)
    }

}
