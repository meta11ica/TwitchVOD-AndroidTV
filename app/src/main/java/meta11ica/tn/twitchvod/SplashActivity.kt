package meta11ica.tn.twitchvod

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import khttp.get
import khttp.post
import kotlinx.coroutines.launch
import meta11ica.tn.twitchvod.databinding.ActivitySplashBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.Locale


class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {

            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()

            StrictMode.setThreadPolicy(policy)
            checkForUpdates()
            initializePrefs()
        }
        binding = ActivitySplashBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        binding.ivGood.alpha = 0f

        binding.ivGood.animate().alpha(1f).setDuration(1000).withEndAction {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
            this.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

    }


    private fun initializePrefs() {
        lateinit var streamerId: List<String>
        val sharedPrefs = getSharedPreferences("Streamers", MODE_PRIVATE)
        //sharedPrefs.edit().clear().commit()
        if (sharedPrefs.getString("favourite_streamers", null)==null) {
            streamerId = listOf(
                "Domingo"
            )

            sharedPrefs.edit().putString("favourite_streamers",streamerId.joinToString(separator=",")).commit()
        }
        else {
            streamerId = sharedPrefs.getString("favourite_streamers", "micode")?.split(",")!!
            // Return the array of values

        }
        val editor = sharedPrefs.edit()
        // the first use of the shared preference will trigger its initialisation
        editor?.putBoolean("initialized", true)
        val streamerArray = JSONArray()

        lifecycleScope.launch {

            val favStreamers = sharedPrefs.getString("favourite_streamers","[micode]")?.split(",")

            val answers = favStreamers?.let { makeTwitchPostRequest(it) }
            val strResponse = answers?.get(0)
            val strLiveResponse = answers?.get(1)

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

        val liveUrl = getLiveURL(favStreamers?.get(it))
        id.add(userLiveJsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("broadcastSettings").getLong("id"))
        title.add(userLiveJsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("broadcastSettings").getString("title"))
        description.add(userLiveJsonObject.getJSONObject("data").getJSONObject("user").getJSONObject("broadcastSettings").getString("title"))
        duration.add(0)
        studio.add(("Twitch"))
        videoUrl.add(liveUrl)
        bgImageUrl.add(
            "data:image/png;base64,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"
        )
        cardImageUrl.add(
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAANgAAADpCAMAAABx2AnXAAABPlBMVEX///8REiQAAADa2tu6GBPUDQrYDAuzHBXQDgy3GhOlIhqtHhbGEw+wHRafJBzbCQiaJRu/FxGpHxfJEQ6XJxzBFg/NEA7lBwbfCAblAADOAACwAAC/AADJCwevGRHhh4fPJCP89vasAAC6AAD47e3Wl5jvyckAABq2VE/Ed3SnEgQAABzcsK4AABWcAAD89/eTAADYoaG5NTCUlJrt1tXyurrlnp7LPjrnp6f339/glZLLMCxnaHGNjZV5eYEpKjhBQUw9PUhTU12ioqfSYl7IUk7ov73cgX/HZ2PUSkmyKCPOioa6TUnBbmneXFvibWz2x8fiKyymQTrmPz3JaGfJmpjqXF3wj4/iISH52tb0sK+yPjnpTU3KfnvAREDicHDpMjHxmJftgIDewL4ZGyrDxchaW2K1trfl5ecsLjnDTNTMAAAMMElEQVR4nO2cC1vaSBeAA1gVvOKlQAIoCRJLioiAEkjCTaFqva1sq21tP12ky///A9+ZBBAsJJCkJnbnfaSiUsnrzJw5czIJQWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAYDAZjNyJRIGn1UZhIMvY+f3hQmPKuM+te98LeUT1Vi1p9UEaJZuuFoMw64AWmvJTMh1Tm1bYev38eAubn13tWMm63e8YNbnt3MasPUQ/ZYzJErq6uzg8VQ7hp+uSUt/o4JyO6QZLk29VVVbGZhYUwTW+/omaL1kly9u3bMcQWFubi9Md/rD7g8UhCa80ir/HE5ubi8e2M1Qc9BlmkNZHY3Eo4fhax+rg14M/JN7MTi62sxFfeW33oqmSXlt7oEgM1Gzda8mJp+o1eMU98xa4jjb+WvfSKeVZ27NkdY9NL04bEPJ6dU6slhgDDa9qomGfnzHYJ5Bc/8jIq5tnZtpkZeJkiBmZWqwyQ9ZsjtuhZtJXZJfIyp8UWF3feWa3Tg5e9zBJb3NmyWqhD5MpcscVdm8xn536TxRZ3bJGD7Ps1xEJy0YNRxLyMXPIIq4p5/rJB0Of96mKhUD0WjfC5Y2bdC1oHqUwkGjulqFFREbG8a4MU5FpdLHTYLbXVvIyXyXW+SN5RKi22vLxreWe896mKheoEEc01DusxaFsvA/9mNhqNFMhuUWpiy1Z3xmggoCYWOobkeF4eY4fwDKJdnWHQKINnZ5Sa2O7f1oqdKw02UownboOrclQMfkevbzByVHTTNSJKhVXElpf/Tv3C1tb+fm5/gFyW/x1lZd7nV+uKoQOCOAh1wj0DrRRjugXTmQhxRo8MHsDabhxBK8ihlOmg1JZRMZaUOa6bXsL7pCGWJzLB7jzGHCsNpsxjVI14/6tYX4utrcHSE4Cfw8vkAqtbnga9XvTrYHqEIQxvMjv7Btyu7k0tLMQCGmI5ItcTW5+C5vP2xE6JmIbYsmymLTYL7wlv/9lEtQuIHBpiWf1iaxOIwVsvTWfN8uIDWmJ5ItY7KcF8RzFxgq4ITTaRmN9/YZLYZ+iJ6mKF/uBRg0msFzxOkkRDS2xtMrGlJf+1ORHSF+gE+9Hh/hbCvXxubJ05RP+l3gn3FAr3o1OqjtjipGL+KzPMLgM+jRbrm6CZRpKopQjilJKzYGi9huoELYstTyzmvzEhY7mQxUa3GEmGQijhILL1Rp5H/RD1Rn6rUX8PAeyUpmB+Uhdbm1zM982wV9Kv2mKz5HkeMoP8Ze/18GW+11MyWyiT+ECHVcUWJxbz+x6MisEkpiI2S9bG+SWZk7Ca2NrkYn7fpfbbqvJZVYwcc1LJ0CrBQ5dY4Mag2Dc1MRICfaaAEjnI6IJduske2jkAh0idQI73Ia7WYp7Ju6IvcG/IK+pTFaujGQxANY/nZQ+3PEWHw+H4AkGkaDWxRT1iS4Yi4y3yGi22QRCbIa1iTjhMQP4xtzIXD48QW9Yh5gt8MSK2b54Y/fVsO74yVGxNl5ihkP9gllh8DpURM3srqmLwn6aUX+D1zvfWY7IYGsnw7n1iASO7Rz6ZJXanLDcy8aFiHlksfPIdceBlmCnv+kGXQgilAbM/8hvn6AC6Ysb64o1JYrLU2T8E8XFltBiV6rwrX2fkkpBCjHxLbirTVvLe3ydmJM0PmCYWvYvH3xHE9lCxRUXsqZyfZ/rFyEJveXnZJ2ZgKovIXmaIpebiK2OJnW6lkEVBLuLVN4AfJIlG0+UDKnpc+HtivoB+Md4sMRTuVcSWn8RoikKVrjoSyzJy+EDTJYH2bZEPn/rGmM9A9Lj9LWK/pFT9YpTbOwWf7pAYnwP250n0lEThfiB4+AK3usViVrQYquARh70xltwkM9ARyefzGIjpr8ddvryYQkYJHhEgKovF/gyxzMw6EqsFUbE0ROZAcZOUy29264oZOj6mWA2t8DrzWFZJPchz+NbtEkne3H4ZENNf+jAteBDJU1plHhsYY2iIFeQWi/AyIWXZd4uCYPYp3AcMhPuoaWIQ1z6gvUaa8xjlZqK9MaYQ6lvQ/ugTu9YvRpjWFVGvyUSI5N7QXFERo++geUDsiM/wDSbHw6fM7S1/S76dJS8uYdrmv1z354pG6h5mpVTvaSUP3FJLghdQyQ7ye7T8Xvf2kns5u1eS+8Ek2EjZ43+midFfa9HMu/jw9ZhHz3rMb6Rset4RG15XnCjcz6ETYcNX0PoWmoZq+J81W2xeW4zuzGMjq1T6xAydB/yi3mLwR6uHFAYrVIyy0QNB0x9gbKkWc/SIGasMdNL70QVTCFXZ/FC2erxPEsTegpoYmsYmFjN24jZyo14w/THerzmlVSvBk5ff/L5zQ16dosfo2j15fBt5AlX6Op+TT9+M8Ge0au1eT8F0yeiZpH11sbckuVnoMg+9o144OSgcQh9273U5oWnV82N6StyGS/eQLWqdH5NDB8RFZfcKmlrX0YrqjpJrwW53OKxxfgydIJv0NJKx+rbMlfaJPyXczwejRLLAoHDvhXQvcqK1ra8n5plYzPBJJEKZycYSC+6j4pIyjzENWH9Q44pNfOLPDC955TKOWLAAi4Fgd4JmYA14RI0nNuGpWr9/3wwvgrjW2jXQEYP1+1FPzLsHmpR7LLEJt0PcmLXz6Ava+aYphnb2ZYNPKRWzJe/pGyr2LCqOt+UIiaEy1YNpW3Oifo0NLIoZRA5vn9gUWi7uucdoscVhYt5hYrBuuTDzMtbP2mLzQVji1oP9STBzBCvL4Vtnn205GhDrNFivxdBv75xrITfvzd3bF9XYJLaq7PTIBAeze7TL40x96ywS2+lu6+vkzL9s6pO39W0e/8iZf83xg7YYvOnxM7GpkyTahqkhthsdi99znWCku3IZJRbKE0Qu+Hw9Rp3COmzYnuC+4GHx3tl7ja2zBWgbZZfYwEKTghngI63aYhbvdk5eqe+lgtF0GPx1BT31HZJh9c3OVl/1fakmRqLkNzisNEDJ+8VGi3nOLPZC59tGi4VykPyGhom56ajqDtNd6+/4kZTDxwixQvZw+G0vZqjt2sfR29NtcT1STG2Modt5DK9S0ZR75Bjbtb4jIu61li0jy28jxDx/2eQi9nOTxexx+RjiylQxyyP9E9EbMy8+tUPg6MKbd7nwrl0uPVVAZqa0mO1uN8Cb0xVtc6nwE/yVGTdRSGm/0YsT+Wb0thcrO/aJhwP8QDeW0S8W/2rbu2/Jt8zRK7Zj45vmEPw1Oa1PLG6r6WsI+8ptqSYUm4tvW79O0YA/l9UmEovv2by5FGIHyq3fxr5FGp2y8ega4BLUxhQL0+HT16KFiB2SJKktRtNfX01rdeHzBeV85igxN0XTZ6/o5op9ZGS30JC6IrrT58xZzQZ37dBLtLZx1Lk9KxPsbWCZ+t5I2WaVrJ8oH8vlNxqHR0dHh427063ab7ntAQaDwWAwGAwGg8FgMBgMBoPBYDAYDAaDwWAwGAwGg8FgMBgMBoPBYDAvjOsPhXD+oRCOPxQs9tpQFWO5ga86j9dBR6wMj2JTed7u/izRqiTa5e5XzQrrKLbajleCIsYJApsQE4mEg004xSabSLDwpApIVWfC6XSwTmfb5XSWxZLFxzs2nRZriommJKUlJzykdDudlkrViqvidAr/ttIuV7HkcpUeS/C5/KJHx/b3/e5T7tmLOPnBohdwDq77045YIu0QBCkhCYLTKXKiwylJLWfp0SUJ6eKjs/Xzsews/utiX3qISazk4Nhi2dFEf3uhzBY5rimCbhF9pwgfggRHWqm2xJIkCmJFFKQK2y/GtlpiKw1uJeiSXDWRgBdxTifnktKVn87yz8dEov2vq/n8j/WbYSWpUk3DoySK6Qp8boGGUBXZklQRJRH1LlYSKkI7LUjw4RSrXLoyKObgqhL8KYrFtKPZEoSWI92qcFVBfCy7ii7pUUr/lFBXTLywWOlnO10RK/CAY25V4S8vpktVsQySoii1wazYSrekFgyeSgvcBUmsCNygmNDkiqLEwiMhOAWxWWyzlbTEJlolLt2CFkwXBadUflkx6HJcu8iV2HaxWeTKTUez7OBKxWaZLTfbzaajWCxxJa5cLrabbbbsKFeK8JNOeOvNY8gTjaEEGrCc3DRcgpW/l4CfsYnBkWxL+o/vv5l5vGaw2Gvj/zImjHWiABHZAAAAAElFTkSuQmCC"
        )
    }

    for (i in 0 until jsonedges.length()) {
        val videoNode = jsonedges.getJSONObject(i).getJSONObject("node")
        if(videoNode.getString("type").equals("LATEST_BROADCASTS")) {
            for (j in 0 until videoNode.getJSONArray("items").length()) {
                val item = videoNode.getJSONArray("items").getJSONObject(j)

                title.add(item.getString("title"))

                val desc = if (!item.isNull("game")) {
                    item.getJSONObject("game").getString("name")
                } else {
                    "Untitled"
                }
                description.add(desc)
                studio.add(("Twitch"))
                id.add(item.getLong("id"))
                videoUrl.add(
                    "https://www.twitch.tv/videos/" + item.getString("id")
                )
                duration.add(item.getLong("lengthSeconds"))
                if (item.getString("previewThumbnailURL")
                        .contains("_404/404")
                ) {
                    bgImageUrl.add("https://assets.twitch.tv/assets/mobile_iphone-526a4005c7c0760cb83f.png"
                    )
                    cardImageUrl.add("https://assets.twitch.tv/assets/mobile_iphone-526a4005c7c0760cb83f.png"
                    )

                } else {
                    bgImageUrl.add(
                        item.getString("previewThumbnailURL")
                            .replace("320x180", "1920x1080")
                    )

                    cardImageUrl.add(
                        item.getString("previewThumbnailURL")
                    )
                }

            }
        }

    }
}
catch(e: Exception) {(Log.e("error","user problem"))}


                    val streamsArray = JSONArray()

                    for (i in studio.indices) {
                        val streamObject = JSONObject()
                        streamObject.put("id", id[i])
                        streamObject.put("title", title[i])
                        streamObject.put("description", description[i])
                        streamObject.put("studio", studio[i])
                        streamObject.put("duration", duration[i])
                        streamObject.put("videoUrl", videoUrl[i])
                        streamObject.put("bgImageUrl", bgImageUrl[i])
                        streamObject.put("cardImageUrl", cardImageUrl[i])
                        streamsArray.put(streamObject)
                    }
                    val streamerObject = JSONObject()
                    streamerObject.put("streamer_id", favStreamers?.get(it))
                    streamerObject.put("streams", streamsArray)
                    streamerArray.put(streamerObject)

                }
                editor?.putString("movies",streamerArray.toString())
                editor.apply()

            }




    }

    fun makeTwitchPostRequest(favStreamers: List<String>): Array<String> {
        val vodResponse = fetchGQL(buildVODStreamsObjectRequest(favStreamers))
        val liveResponse = fetchGQL(buildLiveStreamsObjectRequest(favStreamers))
        return arrayOf(vodResponse,liveResponse)
    }


    fun getLiveURL(streamer: String?): String {
        val query = """
        query PlaybackAccessToken_Template(${"$"}login: String!, ${"$"}isLive: Boolean!, ${"$"}vodID: ID!, ${"$"}isVod: Boolean!, ${"$"}playerType: String!) {  streamPlaybackAccessToken(channelName: ${"$"}login, params: {platform: "web", playerBackend: "mediaplayer", playerType: ${"$"}playerType}) @include(if: ${"$"}isLive) {    value    signature    __typename  } videoPlaybackAccessToken(id: ${"$"}vodID, params: {platform: "web", playerBackend: "mediaplayer", playerType: ${"$"}playerType}) @include(if: ${"$"}isVod) {    value    signature    __typename  }}
    """.trimIndent()
        val variablesObject = JSONObject()
        variablesObject.put("isLive", true)
        variablesObject.put("isVod", false)
        variablesObject.put("login", streamer)
        variablesObject.put("playerType", "site")
        variablesObject.put("vodID", "")

        val queryObject = JSONObject()
        val queryArray = JSONArray()
        queryObject.put("operationName", "PlaybackAccessToken_Template")
        queryObject.put("query",query)
        queryObject.put("variables",variablesObject)
        queryArray.put(queryObject)
        val rawtoken = JSONArray(fetchGQL(queryArray)).getJSONObject(0)
        val token = URLEncoder.encode(rawtoken.getJSONObject("data").getJSONObject("streamPlaybackAccessToken").getString("value"), "UTF-8")
        val signature = rawtoken.getJSONObject("data").getJSONObject("streamPlaybackAccessToken").getString("signature")

        val url = "api/channel/hls/$streamer".lowercase(Locale.ROOT) //fix bug streamer name must be in lowercase
        val liveStreamUrl = "https://usher.ttvnw.net/$url.m3u8?allow_source=true&allow_audio_only=true&fast_bread=true&playlist_include_framerate=true&reassignments_supported=true&sig=$signature&token=$token"
        return liveStreamUrl
    }

    fun buildVODStreamsObjectRequest(favStreamers: List<String>):JSONArray {
        val queriesArray = JSONArray()
        for (channelOwnerLogin in favStreamers) {
            val variablesObject = JSONObject()
            variablesObject.put("videoSort", "TIME")
            variablesObject.put("channelLogin", channelOwnerLogin)
            variablesObject.put("first", 30)
            variablesObject.put("broadcastType", null)

            val extensionsObject = JSONObject()
            val persistedQueryObject = JSONObject()
            persistedQueryObject.put("version", 1)
            persistedQueryObject.put(
                "sha256Hash",
                "f374a282ca470e4cddb42f75c2b04fc277ce220cc1fe257062bc47d6eef48eb9"
            )
            extensionsObject.put("persistedQuery", persistedQueryObject)

            val queryObject = JSONObject()
            queryObject.put("operationName", "ChannelVideoShelvesQuery")
            queryObject.put("variables", variablesObject)
            queryObject.put("extensions", extensionsObject)

            queriesArray.put(queryObject)
        }
        return queriesArray
    }

    fun buildLiveStreamsObjectRequest(favStreamers: List<String>):JSONArray {
        val queriesArray2 = JSONArray()

        for (channelOwnerLogin in favStreamers) {
            val variablesObject2 = JSONObject().apply {
                put("isCollectionContent", false)
                put("isLiveContent", true)
                put("isVODContent", false)
                put("collectionID", "")
                put("login", channelOwnerLogin)
                put("vodID", "")
            }

            // Build extensions object
            val extensionsObject2 = JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", "2dbf505ee929438369e68e72319d1106bb3c142e295332fac157c90638968586")
                })
            }

            // Build the final JSON object
            val jsonObject = JSONObject().apply {
                put("operationName", "NielsenContentMetadata")
                put("variables", variablesObject2)
                put("extensions", extensionsObject2)
            }
            queriesArray2.put(jsonObject)
        }
        return queriesArray2
    }

    fun fetchGQL(body: JSONArray): String {

        val url = "https://gql.twitch.tv/gql"

        val headers = mapOf(
            "Client-ID" to "kimne78kx3ncx6brgo4mv6wki5h1ko", // Replace with your Twitch Client ID
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        )
        return(post(url=url,headers=headers,data=body.toString()).text)


    }
    fun checkForUpdates() {
        try {
            val latestRelease = JSONObject(get(url = getString(R.string.update_check_json)).text).getJSONArray("elements").getJSONObject(0).getInt("versionCode")
            Toast.makeText(
                this,
                "Current version : "+resources.getInteger(R.integer.app_version_code)+", latest release version : "+latestRelease,
                Toast.LENGTH_SHORT
            ).show()

            val aua = AutoUpdateApk(applicationContext, getString(R.string.update_check_json))
            aua.setUpdateInterval(AutoUpdateApk.DAYS * 1);

        }
        catch (e: Exception)
        {
            e.printStackTrace()
            Log.e("Update checks","Couldn't check updates...")
        }
    }
}