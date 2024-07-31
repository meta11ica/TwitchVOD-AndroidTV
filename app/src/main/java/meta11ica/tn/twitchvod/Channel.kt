package meta11ica.tn.twitchvod

import org.json.JSONObject
import java.io.Serializable

/**
 * Channel class represents streamer entity with id, login, description, image url.
 */
data class Channel(
    var id: Long = 0,
    var login: String? = null,
    var displayName: String? = null,
    var description: String? = null,
    var backgroundImageUrl: String? = null,
    var profileImageURL: String? = null,
    var followersCount: Long = 0
) : Serializable {

    override fun toString(): String {
        val jsonStream = JSONObject()
        jsonStream.put("id",id)
        jsonStream.put("login",login)
        jsonStream.put("displayName",displayName)
        jsonStream.put("description",description)
        jsonStream.put("backgroundImageUrl",backgroundImageUrl)
        jsonStream.put("profileImageURL",profileImageURL)
        jsonStream.put("followersCount",followersCount)
        return "Stream$jsonStream"
    }


    companion object {
        internal const val serialVersionUID = 727566175075960653L

        fun stringToChannel(strChannel: String): Channel {
            val channel = Channel()
            val jsonChannel = JSONObject(strChannel.removePrefix("Channel"))
            channel.id = jsonChannel.getLong("id")
            channel.login = jsonChannel.getString("login")
            channel.displayName = jsonChannel.getString("displayName")
            channel.description = jsonChannel.getString("description")
            channel.backgroundImageUrl = jsonChannel.getString("backgroundImageUrl")
            channel.profileImageURL = jsonChannel.getString("profileImageURL")
            channel.followersCount = jsonChannel.getLong("followersCount")

            return channel
        }


    }
}