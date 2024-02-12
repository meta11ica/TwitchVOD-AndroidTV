package com.example.twitchvod

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import meta11ica.tn.twitchvod.R
import meta11ica.tn.twitchvod.SplashActivity
import meta11ica.tn.twitchvod.StreamersAdapter


class EditFavouriteListFragment : Fragment() {

    private lateinit var editText1: EditText
    private lateinit var buttonSave: Button

    // Get shared preferences
    lateinit var sharedPrefs: SharedPreferences
    lateinit var streamers: MutableList<String>
    lateinit var adapter: StreamersAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_favourite_list, container, false)
        sharedPrefs = requireActivity().getSharedPreferences(
            "Streamers",
            FragmentActivity.MODE_PRIVATE
        )
        streamers = (sharedPrefs.getString("favourite_streamers", "micode")?.split(",")
            ?: emptyList()).toMutableList()
        // Initialize views
        editText1 = view.findViewById(R.id.editText1)
        editText1.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                buttonSave.isEnabled = s.trim().isNotEmpty()
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int, count: Int,
                after: Int
            ) {
                // TODO Auto-generated method stub
            }

            override fun afterTextChanged(s: Editable) {
                // TODO Auto-generated method stub
            }
        })
        editText1.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                saveSettings() // ENTER IS EQUIVALENT TO SAVE
                editText1.text.clear()
                return@OnEditorActionListener true
            }
            false
        })
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonSave.setEnabled(false)

        // Set click listener for the "Save" button
        buttonSave.setOnClickListener {
            saveSettings()
        }


        adapter = StreamersAdapter(streamers) { streamer ->
            removeFromFavourite(streamer)
        }

        val recyclerView: RecyclerView = view.findViewById(R.id.streamersRecyclerView)
        val layoutManager = FlexboxLayoutManager(requireContext())
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.flexWrap = FlexWrap.WRAP
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Navigate back to MainFragment
            navigateToMainFragment()
        }
    }


    private fun navigateToMainFragment() {
        // Replace the current fragment with MainFragment
        this.activity?.finish()
        val intent = Intent(this.activity, SplashActivity::class.java)
        startActivity(intent)
    }

    fun saveSettings() {
        val streamer = editText1.text.toString().trim()
        val streamerExists = streamers.any { it.equals(streamer, ignoreCase = true) }
        if (streamer.isNotEmpty() && !streamerExists) {
            streamers.add(streamer)
            sharedPrefs.edit()?.remove("favourite_streamers")?.commit()
            sharedPrefs.edit()
                ?.putString("favourite_streamers", streamers.joinToString(separator = ","))
                ?.commit()
            Toast.makeText(
                requireContext(),
                "$streamer added to favourites!",
                Toast.LENGTH_SHORT
            ).show()
            // Notify adapter about the added item
            adapter.notifyItemInserted(streamers.size - 1)
        } else {
            if (streamerExists) {
                Toast.makeText(
                    requireContext(),
                    "$streamer already exists in favourites!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Streamer name cannot be empty!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        editText1.text.clear()
    }

    private fun removeFromFavourite(streamer: String) {
        if (streamers.size > 1) {
            val removedIndex = streamers.indexOf(streamer)
            if (removedIndex != -1) {
                streamers.removeAt(removedIndex)
                Log.d("11111111", streamers.toString())
                sharedPrefs.edit()?.remove("favourite_streamers")?.commit()
                sharedPrefs.edit()
                    ?.putString("favourite_streamers", streamers.joinToString(separator = ","))
                    ?.commit();
                Toast.makeText(
                    requireContext(),
                    "$streamer removed from favourites!",
                    Toast.LENGTH_SHORT
                ).show()
                adapter.notifyItemRemoved(removedIndex)
            }
        } else {
            Toast.makeText(
                requireContext(),
                "$streamer not removed! List must contain at least one.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    companion object {
        fun newInstance(): EditFavouriteListFragment {
            return EditFavouriteListFragment()
        }
    }
}

