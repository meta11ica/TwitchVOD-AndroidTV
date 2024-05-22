package meta11ica.tn.twitchvod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

class ContinueOrReplayFragment : DialogFragment() {
    private var continueOrReplayListener: ContinueOrReplayListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_continueorreplay, container, false)

        view.findViewById<Button>(R.id.button_continue).setOnClickListener {
            continueOrReplayListener?.onOptionSelected(true) // Option 1: Continue
            dismiss()
        }

        view.findViewById<Button>(R.id.button_replay).setOnClickListener {
            continueOrReplayListener?.onOptionSelected(false) // Option 2: Replay
            dismiss()
        }
        return view
    }

    fun setContinueOrReplayListener(listener: ContinueOrReplayListener) {
        continueOrReplayListener = listener
    }

    interface ContinueOrReplayListener {
        fun onOptionSelected(continuePlayback: Boolean)
    }
}
