package org.chrisolsen.spotify;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context context = getActivity();
        final View layout = inflater.inflate(R.layout.artist_search_fragment, container, false);
        final ListView listView = (ListView)layout.findViewById(android.R.id.list);

        mSpotifyApi = new SpotifyApi().getService();
        
        // ListView select event
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor)listView.getAdapter().getItem(position);

                // FIXME: use pre-set column indices rather than calling getColumnIndex all the time
                String artistId = c.getString(c.getColumnIndex(ArtistsContract.ArtistEntry.COLUMN_ID));
                String artistName = c.getString(c.getColumnIndex(ArtistsContract.ArtistEntry.COLUMN_NAME));
                String artistImageUrl = c.getString(c.getColumnIndex(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL));

                Intent intent = new Intent(context, ArtistTopSongsActivity.class);
                intent.putExtra(ArtistsContract.ArtistEntry.COLUMN_ID, artistId);
                intent.putExtra(ArtistsContract.ArtistEntry.COLUMN_NAME, artistName);
                intent.putExtra(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL, artistImageUrl);

                startActivity(intent);
            }
        });

        // required within a fragment
        setHasOptionsMenu(true);

        bindSearch(layout);

        mCursorAdapter = new ArtistsCursorAdapter(context, null, 0);
        listView.setAdapter(mCursorAdapter);

        return layout;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) return;

        // TODO: do I still need to do this now that a loader is used?
        String filter = savedInstanceState.getString("filter");
        if (filter != null) {
            new SearchTask(mSpotifyApi).execute(filter);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("filter", mSearchFilter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_artist_search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO: allow user to save their location
        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindSearch(View parent) {

        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        final EditText filter = (EditText) parent.findViewById(R.id.artists_search_filter);
        final ImageButton cancel = (ImageButton) parent.findViewById(R.id.artist_search_cancel);
        final Context context = getActivity();

        final View instructions = parent.findViewById(R.id.artists_search_instructions);

        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isBlankFilter = filter.getText().toString().length() == 0;

                if (isBlankFilter) {
                    instructions.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        filter.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mSearchFilter = null;  // only save the filter after clicking the search button

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mSearchFilter = String.valueOf(v.getText());
                    new SearchTask(mSpotifyApi).execute(mSearchFilter);
                    imm.hideSoftInputFromWindow(filter.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter.setText("");

                // hide the keyboard
                imm.hideSoftInputFromWindow(filter.getWindowToken(), 0);
            }
        });
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

        public SearchTask(SpotifyService spotify) {
            this.spotify = spotify;
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

            // show/hide controls
            Activity a = getActivity();
            View noResults = a.findViewById(android.R.id.empty);
            View instructions = a.findViewById(R.id.artists_search_instructions);
            View listView = a.findViewById(android.R.id.list);

            boolean hasResults = searchResults.length > 0;
            boolean hasFilter = mSearchFilter.length() > 0;

            instructions.setVisibility(View.GONE);
            noResults.setVisibility(!hasResults && hasFilter ? View.VISIBLE : View.GONE);
            listView.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        }
    }
}
