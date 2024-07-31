package meta11ica.tn.twitchvod

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.Util
import androidx.leanback.widget.VerticalGridPresenter
import khttp.get
import org.json.JSONArray
import org.json.JSONObject
import java.util.Timer


/**
 * Loads a grid of cards with movies to browse.
 */
class ChannelFragment : VerticalGridSupportFragment() {

    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null
    private lateinit var mSelectedChannel: Channel
    private lateinit var allStreams: MutableList<String>
    var screenWidth: Int = 0
    var screenHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        mSelectedChannel = requireActivity().intent.getSerializableExtra(ChannelActivity.CHANNEL) as Channel
        prepareBackgroundManager()
        loadRows()
        setupEventListeners()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        val backBtn = inflater.inflate(R.layout.button_back, view, false)
        backBtn.setOnClickListener(View.OnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        })
        view.addView(backBtn)
        return view
    }


    override fun onDestroy() {
        super.onDestroy()
        mBackgroundTimer?.cancel()
    }

    private fun prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        mDefaultBackground = ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
        mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun loadRows() {


        val items = ArrayObjectAdapter(StreamPresenter(screenWidth/(NUM_COLS +1),screenHeight*7/24))
        allStreams = mutableListOf()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)
        val queryResult = Utils.makeTwitchPostRequest(listOf(mSelectedChannel.login!!))


        val strResponse = queryResult.get(0)
        val strLiveResponse = queryResult.get(1)

        (0 until JSONArray(strResponse).length()).forEach {
            val userJsonObject = JSONArray(strResponse).getJSONObject(it)
            val userLiveJsonObject = JSONArray(strLiveResponse).getJSONObject(it)

            val title: MutableList<String> = ArrayList()
            val duration: MutableList<Long> = ArrayList()
            val id: MutableList<Long> = ArrayList()
            val description: MutableList<String> = ArrayList()
            val studio: MutableList<String> = ArrayList()
            val bgImageUrl: MutableList<String> = ArrayList()
            val videoUrl: MutableList<String> = ArrayList()
            val cardImageUrl: MutableList<String> = ArrayList()

            try {
                //val jsonedges = userJsonObject.getJSONObject("data").getJSONObject("user")
                //    .getJSONObject("videos").getJSONArray("edges")
                val jsonedges = userJsonObject.getJSONObject("data")
                    .getJSONObject("user")
                    .getJSONObject("videoShelves").getJSONArray("edges")

                if (!userLiveJsonObject.toString().contains("\"stream\":null")) {
                    val stream = Movie()
                    val liveUrl = Utils.getLiveURL(listOf(mSelectedChannel.login!!)[it])
                    stream.id = userLiveJsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("broadcastSettings").getLong("id")
                    stream.title = userLiveJsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("broadcastSettings").getString("title")
                    stream.description = userLiveJsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("broadcastSettings").getString("title")
                    stream.duration = 0
                    stream.studio = "Twitch"
                    stream.videoUrl = liveUrl
                    stream.backgroundImageUrl = "data:image/png;base64,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"
                    stream.cardImageUrl = "data:image/png;base64,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"
                    allStreams.add(stream.toString())
                    items.add(stream)
                }

                for (i in 0 until jsonedges.length()) {
                    val videoNode = jsonedges.getJSONObject(i).getJSONObject("node")
                    if(videoNode.getString("type").equals("LATEST_BROADCASTS")) {
                        for (j in 0 until videoNode.getJSONArray("items").length()) {
                            val item = videoNode.getJSONArray("items").getJSONObject(j)

                            val desc = if (!item.isNull("game")) {
                                item.getJSONObject("game").getString("name")
                            } else {
                                "Untitled"
                            }
                            val stream = Movie()
                            stream.description = desc
                            stream.studio = "Twitch"
                            stream.id = item.getLong("id")
                            stream.videoUrl = "https://www.twitch.tv/videos/${item.getString("id")}"
                            stream.title = item.getString("title")
                            if (item.getString("previewThumbnailURL")
                                    .contains("_404/404")
                            ) {
                                stream.backgroundImageUrl = "https://assets.twitch.tv/assets/mobile_iphone-526a4005c7c0760cb83f.png"
                                stream.cardImageUrl = "https://assets.twitch.tv/assets/mobile_iphone-526a4005c7c0760cb83f.png"

                            } else {
                                stream.backgroundImageUrl =
                                    item.getString("previewThumbnailURL")
                                        .replace("320x180", "1920x1080")

                                stream.cardImageUrl =
                                    item.getString("previewThumbnailURL")
                            }
                            allStreams.add(stream.toString())
                            items.add(stream)


                        }
                    }

                }
            }
            catch(e: Exception) {(Log.e("error","user problem"))}

        }



        title = mSelectedChannel.displayName


        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = NUM_COLS // Set the number of columns for the grid
        setGridPresenter(gridPresenter)
        adapter = items

    }

    private fun setupEventListeners() {
        onItemViewClickedListener = ItemViewClickedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row?
        ) {

            if (item is Movie) {
                val intent = Intent(activity!!, PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.MOVIE, item)
                intent.putExtra(PlaybackActivity.CONFIRMATION_PROMPT, false)
                startActivity(intent)

            }

        }
    }



    fun resetBackground(activity: FragmentActivity?)
    {

        BackgroundManager.getInstance(activity)
        val backgroundDrawable: Drawable? = requireContext().getDrawable(R.drawable.default_background)
        val myView: View? = view?.rootView
        if (myView != null) {
            myView.background = backgroundDrawable
        }
    }

    companion object {
        private val TAG = "ChannelFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
        private val NUM_COLS = 4
    }
}