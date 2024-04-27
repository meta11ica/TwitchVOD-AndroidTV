package meta11ica.tn.twitchvod

import org.json.JSONObject
import java.io.Serializable

/**
 * Movie class represents video entity with title, description, image thumbs and video url.
 */
data class Movie(
    var id: Long = 0,
    var title: String? = null,
    var description: String? = null,
    var backgroundImageUrl: String? = null,
    var cardImageUrl: String? = null,
    var videoUrl: String? = null,
    var studio: String? = null,
    var duration: Long?= 0,
    var progress: Int? = 0
) : Serializable {

    override fun toString(): String {
        val jsonStream = JSONObject()
        jsonStream.put("id",id)
        jsonStream.put("title",title)
        jsonStream.put("videoUrl",videoUrl)
        jsonStream.put("backgroundImageUrl",backgroundImageUrl)
        jsonStream.put("cardImageUrl",cardImageUrl)
        jsonStream.put("progress",progress)
        jsonStream.put("duration",duration)
        return "Stream$jsonStream"
    }


    companion object {
        internal const val serialVersionUID = 727566175075960653L

        fun stringToMovie(strMovie: String): Movie {
            val movie = Movie()
            val jsonStream = JSONObject(strMovie.removePrefix("Stream"))
            movie.id = jsonStream.getLong("id")
            movie.title = jsonStream.getString("title")
            movie.videoUrl = jsonStream.getString("videoUrl")
            movie.backgroundImageUrl = jsonStream.getString("backgroundImageUrl")
            movie.cardImageUrl = jsonStream.getString("cardImageUrl")
            if(jsonStream.has("duration")) movie.duration = jsonStream.getLong("duration")

            if (jsonStream.has("progress")) movie.progress = jsonStream.getInt("progress")
            return movie
        }


    }
}