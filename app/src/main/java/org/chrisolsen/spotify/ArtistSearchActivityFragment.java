package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ArtistSearchActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Artist>> {

    protected interface ArtistSelectionHandler {
        void handleArtistSelection(Artist artist);
    }

    private final String LOG_TAG = this.getClass().getSimpleName();
    private static final int LOADER_ARTIST_SEARCH = 0;

    private ArtistSearchAdapter mArtistSearchAdapter;

    private ListView mListView;
    private SearchView mSearchText;
    private View mNoResults;
    private View mSearchInstructions;
    private InputMethodManager mImm;

    private Artist[] mSearchResults;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Activity context = getActivity();
        final LoaderManager.LoaderCallbacks<List<Artist>> loaderCallback = this;
        View layout = inflater.inflate(R.layout.artist_search_fragment, container, false);

        // TODO: move the search box into the activity and actionbar

        // views
        mListView = (ListView)layout.findViewById(android.R.id.list);
        mNoResults = layout.findViewById(android.R.id.empty);
        mSearchInstructions = layout.findViewById(R.id.artists_search_instructions);

        mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mSearchText = (SearchView) layout.findViewById(R.id.searchText);

        // is always spinning, just shown or hidden
        ImageView searchSpinner = (ImageView) layout.findViewById(R.id.artist_search_indicator);
        Animation spin = AnimationUtils.loadAnimation(context, R.anim.rotate_around_center);
        searchSpinner.startAnimation(spin);

        mSearchText.setIconifiedByDefault(false);
        mSearchText.setQueryHint(getResources().getString(R.string.artists_search_instructions));
        mSearchText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (hasNetworkAccess()) {
                    getLoaderManager().restartLoader(LOADER_ARTIST_SEARCH, null, loaderCallback);
                } else {
                    Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        
        // ListView select event
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist artist = (Artist) mListView.getAdapter().getItem(position);
                ArtistSelectionHandler handler = (ArtistSelectionHandler) getActivity();
                handler.handleArtistSelection(artist);
            }
        });

        mArtistSearchAdapter = new ArtistSearchAdapter(getActivity(), new ArrayList<Artist>());
        mListView.setAdapter(mArtistSearchAdapter);

        return layout;
    }

    /**
     * Save any existing search data
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArray("searchResults", mSearchResults);
    }

    /**
     * Bind any saved search results to the search adapter to retain any previous scroll position
     * and ensure any previous search results remain.
     */
    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) return;

        Parcelable[] data = savedInstanceState.getParcelableArray("searchResults");
        if (data == null) return;

        mSearchResults = (Artist[]) data;
//        // convert to simple array
//        mSearchResults = new Artist[data.length];
//        for (int i = 0; i < data.length; i++) {
//            mSearchResults[i] = (Artist)data[i];
//        }

        // bind data
        mArtistSearchAdapter.clear();
        mArtistSearchAdapter.addAll(mSearchResults);
        mArtistSearchAdapter.notifyDataSetChanged();

        mSearchInstructions.setVisibility(View.GONE);
    }

    /**
     * Setup after activity is available
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initLoaderManager();

        // prevent this fragment from having to be recreated when coming back from artist details
        setRetainInstance(true);
    }

    @Override
    public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
        try {
            String filter = mSearchText.getQuery().toString();
            mImm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);

            if (filter.length() > 0) {
                mSearchInstructions.setVisibility(View.VISIBLE);
            }

            return new ArtistSearchLoader(getActivity(), filter);

        } catch (NetworkErrorException e) {
            Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            mSearchInstructions.setVisibility(View.GONE);

            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Artist>> loader, List<Artist> data) {
        boolean hasFilter = mSearchText.getQuery().length() > 0;
        boolean hasData = data.size() > 0;

        mSearchInstructions.setVisibility(View.GONE);

        if (!hasFilter && !hasData) {
            mListView.setVisibility(View.GONE);
            mNoResults.setVisibility(View.GONE);
        } else
        if (hasFilter && hasData) {
            mListView.setVisibility(View.VISIBLE);
            mNoResults.setVisibility(View.GONE);
        } else
        if (hasFilter && !hasData) {
            mListView.setVisibility(View.GONE);
            mNoResults.setVisibility(View.VISIBLE);
        }

        // save a reference to the data to allow saving of the state
        mSearchResults = new Artist[data.size()];
        data.toArray(mSearchResults);

        // make new data visible in the list
        mArtistSearchAdapter.clear();
        mArtistSearchAdapter.addAll(data);
        mArtistSearchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<Artist>> loader) {
        mArtistSearchAdapter.clear();
        mArtistSearchAdapter.notifyDataSetChanged();
    }

    /**
     * Helper method to whether a network is available
     * @return boolean
     */
    private boolean hasNetworkAccess() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        return info != null && info.isConnected();
    }

    /**
     * Initialize the loader manager
     */
    private void initLoaderManager() {
        LoaderManager lm = getLoaderManager();

        // no idea why this *if* is needed, but if this check is not added coming back to this
        // fragment from the artist detail will not bind the existing list data.
        if (lm.getLoader(LOADER_ARTIST_SEARCH) != null) {
            lm.initLoader(LOADER_ARTIST_SEARCH, null, this);
        }
    }

}
