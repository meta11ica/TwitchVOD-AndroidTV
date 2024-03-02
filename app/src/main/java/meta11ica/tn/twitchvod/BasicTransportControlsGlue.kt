package meta11ica.tn.twitchvod

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.annotation.OptIn
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.leanback.LeanbackPlayerAdapter

class BasicTransportControlsGlue(
    context: Context?,
    playerAdapter: LeanbackPlayerAdapter,movie: Movie,isVod: Boolean
) : PlaybackTransportControlGlue<LeanbackPlayerAdapter>(context, playerAdapter) {

    // Primary actions
    private val forwardAction = PlaybackControlsRow.FastForwardAction(context)
    private val rewindAction = PlaybackControlsRow.RewindAction(context)
    private val nextAction = PlaybackControlsRow.SkipNextAction(context)
    private val previousAction = PlaybackControlsRow.SkipPreviousAction(context)
    private val streamIsVod = isVod

    init {
        if (streamIsVod) {
            isSeekEnabled = true // Enables scrubbing on the seekbar
        }
    }

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        primaryActionsAdapter.add(previousAction)
        if(streamIsVod) {
            primaryActionsAdapter.add(rewindAction)
        }
        super.onCreatePrimaryActions(primaryActionsAdapter) // Adds play/pause action
        if(streamIsVod) {
            primaryActionsAdapter.add(forwardAction)
        }
        primaryActionsAdapter.add(nextAction)

    }

        override fun onActionClicked(action: Action) {
        when (action) {
            forwardAction -> {
                if(streamIsVod) {
                    seekTo(currentPosition + 60_000)
                }
            }
            rewindAction -> {
                if(streamIsVod) {
                    seekTo(currentPosition - 60_000)
                }
            }
            else -> super.onActionClicked(action)
        }
        onUpdateProgress() // Updates seekbar progress
    }

    val currentMovie = movie

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
