package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.chrisolsen.spotify.ArtistsContract.ArtistEntry;

import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;

/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistSearchActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private String mSearchFilter;
    private ArtistsCursorAdapter mCursorAdapter;
    private SpotifyService mSpotifyApi;

    private ListView mListView;
    private EditText mSearchText;
    private ImageButton mCancelButton;
    private View mNoResults;
    private View mSearchInstructions;
    private ImageView mSearchIndicator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context context = getActivity();
        View layout = inflater.inflate(R.layout.artist_search_fragment, container, false);

        // views
        mListView = (ListView)layout.findViewById(android.R.id.list);
        mSearchText = (EditText) layout.findViewById(R.id.artists_search_filter);
        mCancelButton = (ImageButton) layout.findViewById(R.id.artist_search_cancel);
        mNoResults = layout.findViewById(android.R.id.empty);
        mSearchInstructions = layout.findViewById(R.id.artists_search_instructions);
        mSearchIndicator = (ImageView) layout.findViewById(R.id.artist_search_indicator);

        mSpotifyApi = new SpotifyApi().getService();
        
        // ListView select event
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor)mListView.getAdapter().getItem(position);

                // FIXME: use pre-set column indices rather than calling getColumnIndex all the time
                String artistId = c.getString(c.getColumnIndex(ArtistEntry.COLUMN_ID));
                String artistName = c.getString(c.getColumnIndex(ArtistEntry.COLUMN_NAME));
                String artistImageUrl = c.getString(c.getColumnIndex(ArtistEntry.COLUMN_IMAGE_URL));

                Intent intent = new Intent(context, ArtistTopSongsActivity.class);
                intent.putExtra(ArtistEntry.COLUMN_ID, artistId);
                intent.putExtra(ArtistEntry.COLUMN_NAME, artistName);
                intent.putExtra(ArtistEntry.COLUMN_IMAGE_URL, artistImageUrl);

                startActivity(intent);
            }
        });

        // required within a fragment
        setHasOptionsMenu(true);

        bindSearch();

        mCursorAdapter = new ArtistsCursorAdapter(context, null, 0);
        mListView.setAdapter(mCursorAdapter);

        return layout;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) return;

        mSearchFilter = savedInstanceState.getString("filter");
        if (mSearchFilter != null) {
            try {
                new SearchTask(mSpotifyApi).execute(mSearchFilter);
            } catch (NetworkErrorException e) {
                resetState();
                Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("filter", mSearchFilter);
    }

    private void bindSearch() {

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isBlankFilter = mSearchText.getText().toString().length() == 0;

                if (isBlankFilter) {
                    mSearchInstructions.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        mSearchText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mSearchFilter = null;  // only save the filter after clicking the search button

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mSearchFilter = String.valueOf(v.getText());
                    try {
                        new SearchTask(mSpotifyApi).execute(mSearchFilter);
                    } catch (NetworkErrorException e) {
                        Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                        resetState();
                    }

                    return true;
                }
                return false;
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetState();
            }
        });
    }

    private void resetState() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
        mSearchIndicator.setImageResource(R.mipmap.ic_search_dark);
        mSearchIndicator.clearAnimation();
        mSearchInstructions.setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(0, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = ArtistsContract.ArtistEntry.CONTENT_URI;

        // https://youtu.be/5AO8DwJ6a4s?t=45
        return new CursorLoader(getActivity(),
                uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private class SearchTask extends AsyncTask<String, Void, ContentValues[]> {
        SpotifyService spotify;

        public SearchTask(SpotifyService spotify) throws NetworkErrorException {
            this.spotify = spotify;

            mSearchIndicator.setImageResource(R.mipmap.ic_spinner);
            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_around_center);
            mSearchIndicator.startAnimation(animation);

            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();

            if (info == null || !info.isConnected()) {
                this.cancel(true);
                throw new NetworkErrorException();
            }
        }

        @Override
        protected ContentValues[] doInBackground(String... filter) {
            String f = filter[0];
            if (f.length() == 0) {
                return null;
            }

            ArtistsPager results = this.spotify.searchArtists(f);

            if (results == null) {
                return new ContentValues[] {};
            }

            List<Artist> artists = results.artists.items;

            // convert the artists to ContentValues[]
            ContentValues[] searchResults = new ContentValues[artists.size()];

            int i = 0;
            for (Artist a : artists) {
                ContentValues vals = new ContentValues();

                vals.put(ArtistsContract.ArtistEntry._ID, i);
                vals.put(ArtistsContract.ArtistEntry.COLUMN_ID, a.id);
                vals.put(ArtistsContract.ArtistEntry.COLUMN_NAME, a.name);

                // get the largest image url
                String imageUrl = null; // default value
                int largestSize = 0;
                for (Image img : a.images) {
                    if (img.width > largestSize) {
                        imageUrl = img.url;
                        largestSize = img.width;
                    }
                }

                vals.put(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL, imageUrl);

                searchResults[i] = vals;
                i++;
            }

            return searchResults;
        }

        @Override
        protected void onPostExecute(ContentValues[] searchResults) {
            Uri uri = ArtistsContract.ArtistEntry.CONTENT_URI;
            ContentResolver cr = getActivity().getContentResolver();
            cr.bulkInsert(uri, searchResults);
            cr.notifyChange(ArtistsContract.ArtistEntry.CONTENT_URI, null);

            boolean hasResults = searchResults.length > 0;
            boolean hasFilter = mSearchFilter.length() > 0;

            resetState();

            mNoResults.setVisibility(!hasResults && hasFilter ? View.VISIBLE : View.GONE);
            mListView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        }
    }
}
