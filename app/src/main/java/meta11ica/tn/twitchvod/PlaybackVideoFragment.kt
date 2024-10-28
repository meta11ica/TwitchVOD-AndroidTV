package meta11ica.tn.twitchvod

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import khttp.post
import org.json.JSONObject
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import androidx.media3.ui.leanback.LeanbackPlayerAdapter
import org.json.JSONArray


/** Handles video playback with media controls. */
class PlaybackVideoFragment(private val watchFromPosition: Long = 0) : VideoSupportFragment() {

    private lateinit var mTransportControlGlue: BasicTransportControlsGlue
    private lateinit var indicatorView: View
    private lateinit var player: ExoPlayer
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    lateinit var sharedPrefs: SharedPreferences
    private lateinit var movie: Movie


    var isVod: Boolean = false




    @OptIn(UnstableApi::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = requireActivity().getSharedPreferences("Streamers",  0)!!
        if (requireActivity().intent.hasExtra(DetailsActivity.MOVIE)) {
            movie = activity?.intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie
        } else {
            movie = activity?.intent?.getSerializableExtra(PlaybackActivity.MOVIE) as Movie
        }

        val title = movie.title
        val description = movie.description
        val videoUrl = movie.videoUrl

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
            var finalVideoUrl = videoUrl
            if (videoUrl != null && !videoUrl.contains("api/channel/hls")) {
                finalVideoUrl = getFinalVideoUrl(videoUrl)
            }

        player =  ExoPlayer.Builder(requireContext()).build()
        val defaultDataSourceFactory = DefaultDataSource.Factory(requireContext())
        val customDataSourceFactory = TwitchUnmutedDataSourceFactory(defaultDataSourceFactory)
        val playerAdapter = LeanbackPlayerAdapter(requireContext(),player,500)
        mTransportControlGlue = BasicTransportControlsGlue(requireContext(), playerAdapter,movie,isVod)
        mTransportControlGlue.host = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        //movie.videoUrl = finalVideoUrl
        val mediaItem = MediaItem.Builder()
            .setUri(finalVideoUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        val mediaSource = HlsMediaSource.Factory(customDataSourceFactory)
            .createMediaSource(mediaItem)

        player.setMediaSource(mediaSource)
        player.seekTo(0, watchFromPosition)

        player.prepare()

            mTransportControlGlue.title = title
            mTransportControlGlue.subtitle = description
            setOnKeyInterceptListener { view, keyCode, event ->
                if (isControlsOverlayVisible || event.repeatCount > 0) {
                    isShowOrHideControlsOverlayOnUserInteraction = true
                } else when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        isShowOrHideControlsOverlayOnUserInteraction = event.action != KeyEvent.ACTION_DOWN
                        if (event.action == KeyEvent.ACTION_DOWN && isVod) {
                            val indicatorTextView =
                                view.findViewById<TextView>(R.id.indicatorTextView)
                            indicatorTextView.text =
                                formatDuration(
                                    minOf(
                                        playerAdapter.currentPosition + 60_000,
                                        playerAdapter.duration
                                    )
                                ) + "/" + formatDuration(playerAdapter.duration)
                            animateIndicator(indicatorView)
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        isShowOrHideControlsOverlayOnUserInteraction = event.action != KeyEvent.ACTION_DOWN
                        if (event.action == KeyEvent.ACTION_DOWN && isVod) {
                            val indicatorTextView =
                                view.findViewById<TextView>(R.id.indicatorTextView)
                            indicatorTextView.text =
                                formatDuration(
                                    maxOf(
                                        playerAdapter.currentPosition - 60_000,
                                        0
                                    )
                                ) + "/" + formatDuration(playerAdapter.duration)
                            animateIndicator(indicatorView)
                        }
                    }
                }
                mTransportControlGlue.onKey(view, keyCode, event)
            }

    }

    private fun animateIndicator(indicatorView: View) {
        indicatorView.animate()
            .withEndAction {
                indicatorView.isVisible = false
                indicatorView.scaleX = 1F
                indicatorView.scaleY = 1F
            }
            .withStartAction {
                indicatorView.isVisible = true
                indicatorView.alpha = 1F
            }
            .scaleX(2f)
            .scaleY(2f)
            .setDuration(1000) // Adjust the duration as needed
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
        mTransportControlGlue.pause()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup

        indicatorView = inflater.inflate(R.layout.view_playback_indicator, view, false)
        view.addView(indicatorView)
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateWatchList()
        handler = Handler(Looper.getMainLooper())
        // Initialize Runnable
        runnable = Runnable {
            if(isVod) {
                val editor = sharedPrefs.edit()
                val streamUid = movie.id.toString()
                editor.putLong(streamUid, player.currentPosition)
                editor.putInt(
                    "$streamUid-progress",
                    (player.currentPosition * 100 / player.duration).toInt()
                ).apply()

                val intent = Intent("com.example.SHARED_PREF_CHANGED")
                intent.setPackage(context?.packageName)
                activity?.sendBroadcast(intent)
                // Your code to execute every 10 seconds goes here
                // etc.
                // Schedule the next execution
            }
            handler.postDelayed(runnable, 10000) // 10 seconds delay
        }
    }

    override fun onResume() {
        super.onResume()
        // Start executing the code every 10 seconds when the fragment is resumed
        handler.postDelayed(runnable, 10000) // 10 seconds delay
    }
    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }

    fun getFinalVideoUrl(videoUrl: String): String? {
        val regex = Regex("""/videos/(\d+)""")
        val matchResult = videoUrl.let { regex.find(it) }
        val vodId = matchResult?.groupValues?.get(1)
        isVod = true
        val data = vodId?.let { fetchTwitchDataGQL(it) }
        val vodData = data?.getJSONObject("data")?.getJSONObject("video")
        val channelData = vodData?.getJSONObject("owner")
        val resolutions = mapOf(
            "160p30" to mapOf("res" to "284x160", "fps" to 30),
            "360p30" to mapOf("res" to "640x360", "fps" to 30),
            "480p30" to mapOf("res" to "854x480", "fps" to 30),
            "720p60" to mapOf("res" to "1280x720", "fps" to 60),
            "1080p60" to mapOf("res" to "1920x1080", "fps" to 60),
            "chunked" to mapOf("res" to "1920x1080", "fps" to 60)
        )

        val sortedDict = resolutions.keys.sortedDescending()
        val orderedResolutions = sortedDict.associateWith { resolutions[it] }
        val currentURL = URL(vodData?.getString("seekPreviewsURL"))
        val domain = currentURL.host
        val paths = currentURL.path.split("/")
        val vodSpecialID = paths[paths.indexOfFirst { it.contains("storyboards") } - 1]

        var fakePlaylist = """#EXTM3U
#EXT-X-TWITCH-INFO:ORIGIN="s3",B="false",REGION="EU",USER-IP="127.0.0.1",SERVING-ID="${createServingID()}",CLUSTER="cloudfront_vod",USER-COUNTRY="BE",MANIFEST-CLUSTER="cloudfront_vod""""

        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val now: Date = formatter.parse("2023-02-10")
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val created: Date = dateFormat.parse(vodData?.getString("createdAt"))

        val timeDifference = now.time - created.time
        val daysDifference = timeDifference / (1000 * 3600 * 24)

        val broadcastType = vodData?.getString("broadcastType")?.toLowerCase()
        var startQuality = 8534030

        for ((resKey, resValue) in orderedResolutions) {
            var url: String? = null
            when {
                broadcastType == "highlight" -> {
                    url = "https://$domain/$vodSpecialID/$resKey/highlight-${vodId}.m3u8"
                }
                broadcastType == "upload" && daysDifference > 7 -> {
                    url = "https://$domain/${channelData?.getString("login")}/$vodId/$vodSpecialID/$resKey/index-dvr.m3u8"
                }
                else -> {
                    url = "https://$domain/$vodSpecialID/$resKey/index-dvr.m3u8"
                }
            }

            if (isValidQuality(url)) {
                val quality =
                    if (resKey == "chunked") "${resValue?.get("res").toString().split("x")[1]}p" else resKey
                val enabled = if (resKey == "chunked") "YES" else "NO"
                val fps = resValue?.get("fps")

                fakePlaylist += """
#EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="$quality",NAME="$quality",AUTOSELECT=$enabled,DEFAULT=$enabled
#EXT-X-STREAM-INF:BANDWIDTH=$startQuality,CODECS="avc1.64002A,mp4a.40.2",RESOLUTION=${resValue?.get("res")},VIDEO="$quality",FRAME-RATE=$fps
$url"""

                startQuality -= 100
            }
        }

        return Regex("https://[^\\s]+").find(fakePlaylist)?.value.toString()
    }

     fun fetchTwitchDataGQL(vodId: String): JSONObject {
        val url = "https://gql.twitch.tv/gql"

        val headers = mapOf(
            "Client-ID" to "kimne78kx3ncx6brgo4mv6wki5h1ko", // Replace with your Twitch Client ID
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )
        var body = "{\"query\":\"query { video(id: \\\"$vodId\\\") { broadcastType, createdAt, seekPreviewsURL, owner { login } }}\"}"
        return JSONObject(post(url, headers = headers, data = body).text)
    }

    fun createServingID(): String {
        val w = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray()
        val random = java.util.Random()
        val id = StringBuilder()

        repeat(32) {
            id.append(w[random.nextInt(w.size)])
        }

        return id.toString()
    }

    fun isValidQuality(url: String): Boolean {

        val connection = URL(url).openConnection()

        try {
            val response = connection.getInputStream()

            response?.bufferedReader()?.use { reader ->
                val data = reader.readText()
                return data.contains(".ts")
            }
        } catch (e: Exception) {
            // Handle any exceptions if necessary
            e.printStackTrace()
        }

        return false
    }

    fun formatDuration(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (milliseconds % (1000 * 60)) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateWatchList() {
        if (isVod) {
            //val currentStream = allStreams[player.currentMediaItemIndex]
            val currentStream = movie
            val strExistingWatchList = if (sharedPrefs.contains("watchList")) sharedPrefs.getString(
                "watchList",
                ""
            ) else ""
            lateinit var watchList: JSONArray
            if (strExistingWatchList!!.isNotEmpty()) {
                watchList = JSONArray(strExistingWatchList)
                var found = false
                var i = 0

                while (i < watchList.length() && !found) {
                    val streamElement = Movie.stringToMovie(watchList.getString(i))

                    if (streamElement.id == currentStream.id) {
                        found = true
                        watchList.put(streamElement.toString())
                        watchList.remove(i)
                    }
                    i++
                }
                if (!found) {
                    watchList.put(currentStream.toString())
                }
            } else {
                watchList = JSONArray()
                watchList.put(currentStream.toString())
            }
            sharedPrefs.edit().putString("watchList", watchList.toString()).apply()
            val intent = Intent("com.example.SHARED_PREF_CHANGED")
            intent.setPackage(context?.packageName)
            activity?.sendBroadcast(intent)
        }
    }



}
