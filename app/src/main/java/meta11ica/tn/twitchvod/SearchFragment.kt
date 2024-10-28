package meta11ica.tn.twitchvod

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import org.json.JSONArray


class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private lateinit var adapter: ArrayObjectAdapter
    var screenWidth: Int = 0
    var screenHeight: Int = 0
    private lateinit var sharedPrefs: SharedPreferences



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        setupEventListeners()
        setSearchResultProvider(this)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Navigate back to MainFragment
            navigateToMainFragment()
        }
    }
    private fun navigateToMainFragment() {
        // Replace the current fragment with MainFragment
        this.activity?.finish()
        val intent = Intent(this.activity, MainActivity::class.java)
        startActivity(intent)
    }
    private fun refreshMainFragment() {
        // Replace the current fragment with MainFragment
        this.activity?.finish()
        val intent = Intent(this.activity, SplashActivity::class.java)
        startActivity(intent)
    }

    override fun getResultsAdapter(): ObjectAdapter {
            adapter = ArrayObjectAdapter(ListRowPresenter())
            return adapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        adapter.clear()
        // Perform search based on the new query
        searchTwitch(newQuery.orEmpty())
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }


    private fun searchTwitch(query: String) {
        val listStreamsRowAdapter = ArrayObjectAdapter(StreamPresenter(screenWidth/(NUM_COLS +1),screenHeight*7/24))
        val listChannelsRowAdapter = ArrayObjectAdapter(ChannelPresenter(screenWidth/(NUM_COLS +1),screenHeight*7/24) { channel ->
            // Handle long-press action here
            showLongPressOptions(channel)
        })

        // Iterate over the map and add items to the adapter
        if (query.isNotEmpty()) {

            // Search Movies
            val searchResult = Utils.fetchGQL(Utils.buildSearchObjectRequest(query))
            val videosJsonEdge = JSONArray(searchResult).getJSONObject(0)
                .getJSONObject("data").getJSONObject("searchFor")
                .getJSONObject("videos").getJSONArray("edges")
            val channelsJsonEdge = JSONArray(searchResult).getJSONObject(0)
                .getJSONObject("data").getJSONObject("searchFor")
                .getJSONObject("channels").getJSONArray("edges")
            for (i in 0 until channelsJsonEdge.length()) {
                val channel = Channel()
                val channelItem = channelsJsonEdge.getJSONObject(i)
                channel.id = channelItem.getJSONObject("item").getLong("id")
                channel.login = channelItem.getJSONObject("item").getString("login")
                channel.displayName = channelItem.getJSONObject("item").getString("displayName")
                channel.backgroundImageUrl = channelItem.getJSONObject("item").getString("profileImageURL")
                channel.profileImageURL = channelItem.getJSONObject("item").getString("profileImageURL")
                channel.followersCount = channelItem.getJSONObject("item").getJSONObject("followers").getLong("totalCount")
                listChannelsRowAdapter.add(channel)
            }

            for (i in 0 until videosJsonEdge.length()) {
                val stream = Movie()
                val streamItem = videosJsonEdge.getJSONObject(i)
                stream.id = streamItem.getJSONObject("item").getLong("id")
                stream.title = streamItem.getJSONObject("item").getString("title")
                stream.studio = "Twitch"
                stream.duration = streamItem.getJSONObject("item").getLong("lengthSeconds")
                stream.backgroundImageUrl = streamItem.getJSONObject("item").getString("previewThumbnailURL")
                stream.cardImageUrl = streamItem.getJSONObject("item").getString("previewThumbnailURL")
                stream.videoUrl = "https://www.twitch.tv/videos/${streamItem.getJSONObject("item").getLong("id")}"

                listStreamsRowAdapter.add(stream)
            }

            if (listChannelsRowAdapter.size() > 0) {
                val channelsHeader = HeaderItem(0, "Channels")
                val channelsRow = ListRow(channelsHeader, listChannelsRowAdapter)
                adapter.add(channelsRow)
            }
            if (listStreamsRowAdapter.size() > 0) {
                val streamsHeader = HeaderItem(0, "Streams")
                val streamsRow = ListRow(streamsHeader, listStreamsRowAdapter)
                adapter.add(streamsRow)
            }
        }
    }


    private fun setupEventListeners() {
        setOnItemViewClickedListener(ItemViewClickedListener())
    }
    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is Movie) {
                val intent = Intent(activity!!, PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.MOVIE, item)
                intent.putExtra(PlaybackActivity.CONFIRMATION_PROMPT, false)
                startActivity(intent)
            }
            else if (item is Channel) {
                val intent = Intent(activity!!, ChannelActivity::class.java)
                intent.putExtra(ChannelActivity.CHANNEL, item)
                startActivity(intent)
            }
        }
    }

    private fun showLongPressOptions(channel: Channel) {
        // Display options for the channel on long-press, e.g., show a dialog
        // For example:
        sharedPrefs = requireActivity().getSharedPreferences("Streamers",  0)!!
        val streamerId:MutableList<String> = sharedPrefs.getString("favourite_streamers", "micode")?.split(",")!!.toMutableList()

        // Find the first matching element, ignoring case
        val streamerToRemove = streamerId.firstOrNull { it.equals(channel.login, ignoreCase = true) }

        if (streamerToRemove != null) {
            if(streamerId.size < 2) {
                Toast.makeText(context,"${channel.login} ${resources.getString(R.string.error_removed_from_favourites)}", Toast.LENGTH_SHORT).show()
            }
            else {
                streamerId.remove(streamerToRemove)
                Toast.makeText(context, "${channel.login} ${resources.getString(R.string.removed_from_favourites)}", Toast.LENGTH_SHORT).show()
            }
        }
        else {
            streamerId.add(channel.login!!)
            Toast.makeText(context, "${channel.login} ${resources.getString(R.string.added_to_favourites)}", Toast.LENGTH_SHORT).show()
        }

        sharedPrefs.edit().putString("favourite_streamers",streamerId.joinToString(separator=",")).apply()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Navigate back to MainFragment
            refreshMainFragment()
        }
    }


    companion object {
        private val TAG = "SearchFragment"

        private val GRID_ITEM_WIDTH = 300
        private val GRID_ITEM_HEIGHT = 120
        private val NUM_ROWS = 6
        private val NUM_COLS = 4
    }






}