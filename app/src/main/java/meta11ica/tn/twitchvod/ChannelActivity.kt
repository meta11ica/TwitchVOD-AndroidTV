package meta11ica.tn.twitchvod
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity


/**
 * Loads [MainFragment].
 */
class ChannelActivity : FragmentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

            setContentView(R.layout.activity_channel)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.channel_fragment, ChannelFragment())
                .commitNow()
        }
    }
    private fun goFullScreen()
    {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsCompat = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsCompat.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsCompat.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    companion object {
        const val SHARED_ELEMENT_NAME = "hero"
        const val CHANNEL = "Channel"
        const val ROW = "0"
    }


}

