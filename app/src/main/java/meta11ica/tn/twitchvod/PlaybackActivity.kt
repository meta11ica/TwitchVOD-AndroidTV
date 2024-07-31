package meta11ica.tn.twitchvod

import android.R
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity


/** Loads [PlaybackVideoFragment]. */
class PlaybackActivity : FragmentActivity(),ContinueOrReplayFragment.ContinueOrReplayListener {
    lateinit var stream: Movie
    var savedPosition = 0L
    private var dialogShown = false



    override fun onCreate(savedInstanceState: Bundle?) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            stream = if (intent.hasExtra(DetailsActivity.MOVIE)) {
                intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie
            } else {
                intent?.getSerializableExtra(PlaybackActivity.MOVIE) as Movie
            }
            val confirmationPrompt = intent.extras?.getBoolean(CONFIRMATION_PROMPT, true) ?: true

            savedPosition = returnSavedPositionMs(stream)
            if (!confirmationPrompt) {
                startPlayback()
            }
            else if ( savedPosition > 0 && !dialogShown) {
                // Show the dialog
                showContinueOrReplayDialog()
            } else {
                // Start playback directly
                startPlayback()
            }
        }
    }
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        super.dispatchTouchEvent(ev)
        val fragmentById: Fragment? =
            supportFragmentManager.findFragmentById(R.id.content)
        if (fragmentById is PlaybackVideoFragment) {
        }
        return true

    }

    fun returnSavedPositionMs(stream: Movie): Long {
        var watchTime = 0L
        val sharedPrefs = getSharedPreferences("Streamers", 0)
        val streamUid = stream.id.toString()
        if (sharedPrefs.contains(streamUid)) {
            watchTime = sharedPrefs.getLong(streamUid, 0)
        }
        return watchTime
    }


    private fun showContinueOrReplayDialog() {
        val dialogFragment = ContinueOrReplayFragment()
        dialogFragment.setContinueOrReplayListener(this)
        dialogFragment.show(supportFragmentManager, "ContinueOrReplayDialog")
    }

    override fun onOptionSelected(continuePlayback: Boolean) {
        // Handle the option selected here
        dialogShown = true
        if (!continuePlayback) {
            // Nullify to read from the beginning
            savedPosition = 0L
        }
        startPlayback()
        dialogShown = false
    }

    private fun startPlayback() {
        val sharedPrefs = getSharedPreferences("Streamers",  0)
        val player = sharedPrefs.getString("player","ExoPlayer")
        supportFragmentManager.beginTransaction()
            .replace(R.id.content, PlaybackVideoFragment(savedPosition))
            .commit()
    }

    companion object {
        const val CONFIRMATION_PROMPT = "confirmationPrompt"
        var MOVIE = "Movie"
        const val FEATURE = "Feature"
    }

}