package org.chrisolsen.spotify;

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

    Menu _menu;
    String mSearchFilter;
    ArtistsCursorAdapter mCursorAdapter;
    private SpotifyService mSpotifyApi;

    public ArtistSearchActivityFragment() {
    }

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
                Artist a = (Artist) listView.getAdapter().getItem(position);
                Intent intent = new Intent(context, ArtistTopSongsActivity.class);
                intent.putExtra("artistId", a.id);
                intent.putExtra("artistName", a.name);

                Bundle imageBundle = new Bundle();
                Image image = a.images.get(0);
                imageBundle.putString("url", image.url);
                imageBundle.putInt("width", image.width);
                imageBundle.putInt("height", image.height);
                intent.putExtra("artistImage", imageBundle);

                startActivity(intent);
            }
        });

        // required within a fragment
        setHasOptionsMenu(true);

        bindSearch(layout);
//        View noResults = a.findViewById(android.R.id.empty);
//        View instructions = a.findViewById(R.id.artists_search_instructions);
//
//        boolean hasResults = items.size() > 0;
//        boolean hasFilter = filter.getText().length() > 0;
//
//        instructions.setVisibility(!hasResults && !hasFilter ? View.VISIBLE : View.GONE);
//        noResults.setVisibility(!hasResults && hasFilter ? View.VISIBLE : View.GONE);
//        listView.setVisibility(hasResults ? View.VISIBLE : View.GONE);

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
        _menu = menu;
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

    private class SearchTask extends AsyncTask<String, Void, ArtistsPager> {
        SpotifyService spotify;

        public SearchTask(SpotifyService spotify) {
            this.spotify = spotify;
        }

        @Override
        protected ArtistsPager doInBackground(String... filter) {
            String f = filter[0];
            if (f.length() == 0) {
                return null;
            }

            return this.spotify.searchArtists(f);
        }

        @Override
        protected void onPostExecute(ArtistsPager query) {
            List<Artist> artists;

            if (query == null) {
                artists = new ArrayList<>();
            } else {
                artists = query.artists.items;
            }

            // convert the artists to ContentValues[]
            ContentValues[] searchResults = new ContentValues[artists.size()];
            int i = 0;
            for (Artist a : artists) {
                ContentValues vals = new ContentValues();

                vals.put(ArtistsContract.ArtistEntry._ID, i);
                vals.put(ArtistsContract.ArtistEntry.COLUMN_ID, a.id);
                vals.put(ArtistsContract.ArtistEntry.COLUMN_NAME, a.name);

                String imageUrl;
                if (a.images.size() >= 2) {
                    imageUrl = a.images.get(1).url; // 200px
                } else if (a.images.size() == 1) {
                    imageUrl = a.images.get(0).url; // 64px
                } else {
                    imageUrl = null;
                }
                vals.put(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL, imageUrl);

                searchResults[i] = vals;
                i++;
            }

            Uri uri = ArtistsContract.ArtistEntry.CONTENT_URI;
            ContentResolver cr = getActivity().getContentResolver();
            cr.bulkInsert(uri, searchResults);
            cr.notifyChange(ArtistsContract.ArtistEntry.CONTENT_URI, null);
        }
    }
}
