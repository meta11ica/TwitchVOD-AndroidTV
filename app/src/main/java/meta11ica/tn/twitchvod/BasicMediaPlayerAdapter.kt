package meta11ica.tn.twitchvod

import android.content.Context
import android.media.session.PlaybackState.*
import android.net.Uri
import androidx.leanback.media.MediaPlayerAdapter

class BasicMediaPlayerAdapter(context: Context) : MediaPlayerAdapter(context) {

    val playlist = ArrayList<meta11ica.tn.twitchvod.Movie>()
    var playlistPosition = 0
        private set

    override fun next() = loadMovie((playlistPosition + 1).mod(playlist.size))

    override fun previous() = loadMovie((playlistPosition - 1).mod(playlist.size))

    override fun fastForward() = seekTo(currentPosition + 10_000)

    override fun rewind() = seekTo(currentPosition - 10_000)

    override fun getSupportedActions(): Long {
        return (ACTION_SKIP_TO_PREVIOUS xor
                ACTION_REWIND xor
                ACTION_PLAY_PAUSE xor
                ACTION_FAST_FORWARD xor
                ACTION_SKIP_TO_NEXT).toLong()
    }

    fun loadMovie(playlistPosition: Int) {
        this.playlistPosition = playlistPosition
        setDataSource(Uri.parse(playlist[playlistPosition].videoUrl))
    }

}