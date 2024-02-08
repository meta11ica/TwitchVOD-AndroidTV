package meta11ica.tn.twitchvod

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
lateinit var sharedPrefs: SharedPreferences
    lateinit var sTREAMER_ID: List<String>
    lateinit var mapSTREAMER_ID: Map<String,String>


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
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
        this.activity?.finish();
        val intent = Intent(this.activity, SplashActivity::class.java)
        startActivity(intent)
        //this.activity?.getIntent()?.let { startActivity(it) };
    }

    override fun getResultsAdapter(): ObjectAdapter {

        sharedPrefs = activity?.getSharedPreferences("Streamers",  0)!!
        sTREAMER_ID = sharedPrefs.getString("favourite_streamers","[micode]").toString()?.split(",")!!
        mapSTREAMER_ID = sTREAMER_ID.associate { it.uppercase() to it }
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())


        // Create a HeaderItem for the list row
        val header = HeaderItem(0,getString(R.string.active_favourite_streamers))

        // Create a ListRow to hold the search results
        val listRowAdapter = ArrayObjectAdapter(TextPresenter())
        // Use your TextPresenter here
        val sortedSTREAMER_ID = sTREAMER_ID.sorted()

            if (sTREAMER_ID != null && sTREAMER_ID.size>0) {
                for(streamer in sTREAMER_ID!!) {
                    listRowAdapter.add(streamer)
                }
            }


        // Add more search results as needed

        // Add the ListRow to the rowsAdapter
        rowsAdapter.add(ListRow(header, listRowAdapter))

        return rowsAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        //TODO("Not yet implemented")
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        //TODO("Not yet implemented")
        lifecycleScope.launch {
            if (query != null) {
                Log.d("onQueryTextSubmit", query)
                if (query.uppercase() in mapSTREAMER_ID){
                    if (mapSTREAMER_ID.size>1) {
                        sTREAMER_ID = sTREAMER_ID.filter { it != mapSTREAMER_ID[query.uppercase()] }
                        Toast.makeText(
                            requireActivity(),
                            "$query removed successfully!!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else {
                        Toast.makeText(
                            requireActivity(),
                            "$query not removed! List must contain at least 1",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    sTREAMER_ID = sTREAMER_ID.plus(query)!!
                    Toast.makeText(
                        requireActivity(),
                        "$query added successfully!!",
                        Toast.LENGTH_SHORT
                    ).show()

                }
                sharedPrefs.edit()?.remove("favourite_streamers")?.commit()
                sharedPrefs.edit()
                    ?.putString("favourite_streamers", sTREAMER_ID.joinToString(separator = ","))
                    ?.commit();

            }
        }
        return true

    }





}