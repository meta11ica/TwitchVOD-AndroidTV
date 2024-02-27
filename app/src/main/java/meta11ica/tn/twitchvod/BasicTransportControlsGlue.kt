package meta11ica.tn.twitchvod

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow

class BasicTransportControlsGlue(
    context: Context?,
    playerAdapter: BasicMediaPlayerAdapter,
) : PlaybackTransportControlGlue<BasicMediaPlayerAdapter>(context, playerAdapter) {
    // Primary actions
    private val forwardAction = PlaybackControlsRow.FastForwardAction(context)
    private val rewindAction = PlaybackControlsRow.RewindAction(context)
    private val nextAction = PlaybackControlsRow.SkipNextAction(context)
    private val previousAction = PlaybackControlsRow.SkipPreviousAction(context)

    init {
        isSeekEnabled = true // Enables scrubbing on the seekbar
    }

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        primaryActionsAdapter.add(previousAction)
        primaryActionsAdapter.add(rewindAction)
        super.onCreatePrimaryActions(primaryActionsAdapter) // Adds play/pause action
        primaryActionsAdapter.add(forwardAction)
        primaryActionsAdapter.add(nextAction)
    }

    override fun onActionClicked(action: Action) {
        when (action) {
            forwardAction -> {
                playerAdapter.fastForward()  // Fast forward to the desired position

            }
            rewindAction -> playerAdapter.rewind()
            else -> super.onActionClicked(action)
        }
        onUpdateProgress() // Updates seekbar progress
    }
    val currentMovie: Movie
        get() = playerAdapter.playlist[playerAdapter.playlistPosition]

    // Event when ready state for play changes.
    override fun onPreparedStateChanged() {
        super.onPreparedStateChanged()
        playWhenPrepared()
        updateMovieInfo(currentMovie)

        // Hide the seekbar after playback starts
        host?.isControlsOverlayAutoHideEnabled = true
    }

    private fun updateMovieInfo(movie: Movie?) {
        title = movie?.title
        subtitle = movie?.description
    }

    fun loadMovie(playlistPosition: Int) {
        playerAdapter.loadMovie(playlistPosition)
    }

    fun setPlaylist(movies: List<Movie>) {
        playerAdapter.playlist.clear()
        playerAdapter.playlist.addAll(movies)
    }
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
        if (host?.isControlsOverlayVisible ?: false || event.repeatCount > 0) {
            return super.onKey(v, keyCode, event)
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (event.action != KeyEvent.ACTION_DOWN) false else {
                onActionClicked(forwardAction)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> if (event.action != KeyEvent.ACTION_DOWN) false else {
                onActionClicked(rewindAction)
                true
            }
            else -> super.onKey(v, keyCode, event)
        }
    }


}
