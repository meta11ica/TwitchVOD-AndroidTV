package meta11ica.tn.twitchvod

import khttp.post
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale


class Utils() : Any() {
    companion object {
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
        fun buildSearchObjectRequest(query: String): JSONArray {
            val queriesArray = JSONArray()
                val variablesObject = JSONObject()
            variablesObject.put("videoSort", "TIME")
            variablesObject.put("query", query)
            variablesObject.put("options", null)
            variablesObject.put("includeIsDJ", false)
            variablesObject.put("broadcastType", null)
            val extensionsObject = JSONObject()
            val persistedQueryObject = JSONObject()
            persistedQueryObject.put("version", 1)
            persistedQueryObject.put(
                "sha256Hash",
                "f6c2575aee4418e8a616e03364d8bcdbf0b10a5c87b59f523569dacc963e8da5"
            )
            extensionsObject.put("persistedQuery", persistedQueryObject)

            val queryObject = JSONObject()
            queryObject.put("operationName", "SearchResultsPage_SearchResults")
            queryObject.put("variables", variablesObject)
            queryObject.put("extensions", extensionsObject)

            queriesArray.put(queryObject)

            return queriesArray
        }

        fun fetchGQL(body: JSONArray): String {

            val url = "https://gql.twitch.tv/gql"

            val headers = mapOf(
                "Client-ID" to "kimne78kx3ncx6brgo4mv6wki5h1ko", // Replace with your Twitch Client ID
                "Content-Type" to "application/json",
                "Accept" to "application/json",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            )
            return (post(url = url, headers = headers, data = body.toString()).text)


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


    }
}