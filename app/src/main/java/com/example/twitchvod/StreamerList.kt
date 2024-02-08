package com.example.twitchvod
import android.content.SharedPreferences
import khttp.post
import khttp.responses.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject


fun twitchPostRequest(channelOwnerLogin: List<String>?): String {
    val url = "https://gql.twitch.tv/gql"

    val headers = mapOf(
        "Client-ID" to "kimne78kx3ncx6brgo4mv6wki5h1ko", // Replace with your Twitch Client ID
        "Content-Type" to "application/json",
        "Accept" to "application/json"
    )

    val body = """
        [{"operationName":"FilterableVideoTower_Videos","variables":{"limit":30,"channelOwnerLogin":"$channelOwnerLogin","broadcastType":null,"videoSort":"TIME"},"extensions":{"persistedQuery":{"version":1,"sha256Hash":"072ae0f19038145cdf1bbe51c83be73fa15ab553483509a8bb65589f6b9ca279"}}}]
    """.trimIndent()
    return post(url= url, headers = headers,data = body).text


}

object StreamerList {
    val STREAMER_ID = arrayOf(
        //"Category Zero",
        "Category One",
        "Category Two",
        "Category Three",
        "Category Four",
        "Category Five"
    )
    fun getList(preferences: SharedPreferences):  List<Movie>{
        val jsonObject = JSONArray(preferences.getString("movies","empty"))
        val map: Map<String, Any> = jsonObject.getJSONObject(0).toMap()
        return setupMovies(map);

    }

    fun getStreamerStreamList(preferences: SharedPreferences):  List<Movie>{
        val jsonObject = JSONObject(preferences.getString("movies","empty"))
        val map: Map<String, Any> = jsonObject.toMap()
        return setupMovies(map);

    }

    private var count: Long = 0

    private fun setupMovies(map: Map<String, Any>): List<Movie> {
        val STREAMER_ID = map.get("STREAMER_ID").toString().split(',');
        val title = map.get("title").toString().split(',');
        val description =map.get("description").toString();
        val studio = map.get("studio").toString().split(',');
        val videoUrl = map.get("videoUrl").toString().split(',');
        val bgImageUrl = map.get("bgImageUrl").toString().split(',');
        val cardImageUrl = map.get("cardImageUrl").toString().split(',');

        val list = title.indices.map {
            StreamerList.buildMovieInfo(
                title[it],
                description,
                studio[it],
                videoUrl[it],
                cardImageUrl[it],
                bgImageUrl[it]
            )
        }

        return list
    }

    private fun setupStreamerStreams(map: Map<String, Any>): List<Movie> {
        val STREAMER_ID = map.get("STREAMER_ID").toString().split(',');
        val title = map.get("title").toString().split(',');
        val description =map.get("description").toString();
        val studio = map.get("studio").toString().split(',');
        val videoUrl = map.get("videoUrl").toString().split(',');
        val bgImageUrl = map.get("bgImageUrl").toString().split(',');
        val cardImageUrl = map.get("cardImageUrl").toString().split(',');

        val list = title.indices.map {
            StreamerList.buildMovieInfo(
                title[it],
                description,
                studio[it],
                videoUrl[it],
                cardImageUrl[it],
                bgImageUrl[it]
            )
        }

        return list
    }

    private fun buildMovieInfo(
        title: String,
        description: String,
        studio: String,
        videoUrl: String,
        cardImageUrl: String,
        backgroundImageUrl: String
    ): Movie {
        val movie = Movie()
        movie.id = count++
        movie.title = title
        movie.description = description
        movie.studio = studio
        movie.cardImageUrl = cardImageUrl
        movie.backgroundImageUrl = backgroundImageUrl
        movie.videoUrl = videoUrl
        return movie
    }
}
fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keysItr = keys()

    while (keysItr.hasNext()) {
        val key = keysItr.next()
        val value = get(key)

        // Recursively convert nested JSONObjects to maps
        if (value is JSONObject) {
            map[key] = value.toMap()
        } else {
            map[key] = value
        }
    }

    return map
}