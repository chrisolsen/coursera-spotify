package org.chrisolsen.spotify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

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
public class ArtistSearchActivityFragment extends Fragment {

    Menu _menu;
    EditText filter;

    public ArtistSearchActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context context = getActivity();
        View layout = inflater.inflate(R.layout.artist_search_fragment, container, false);
        final ListView listView = (ListView)layout.findViewById(android.R.id.list);
        
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

        return layout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        _menu = menu;
        inflater.inflate(R.menu.menu_artist_search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindSearch(View parent) {

        SpotifyApi api = new SpotifyApi();
        final SpotifyService spotify = api.getService();

        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        ImageButton cancel = (ImageButton) parent.findViewById(R.id.artist_search_cancel);

        filter = (EditText) parent.findViewById(R.id.artists_search_filter);
        filter.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchBy = String.valueOf(v.getText());
                    new SearchTask(spotify).execute(searchBy);
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

    private void bindList(List<Artist> items) {

        if (!isAdded()) {
            return;
        }

        Activity a = getActivity();
        ListView listView = (ListView)a.findViewById(android.R.id.list);
        View noResults = a.findViewById(android.R.id.empty);
        View instructions = a.findViewById(R.id.artists_search_instructions);

        boolean hasResults = items.size() > 0;
        boolean hasFilter = filter.getText().length() > 0;

        instructions.setVisibility(!hasResults && !hasFilter ? View.VISIBLE : View.GONE);
        noResults.setVisibility(!hasResults && hasFilter ? View.VISIBLE : View.GONE);
        listView.setVisibility(hasResults ? View.VISIBLE : View.GONE);

        ArtistAdapter adapter = new ArtistAdapter(a, android.R.id.list, items);
        listView.setAdapter(adapter);
    }

    private class ArtistAdapter extends ArrayAdapter<Artist> {
        List<Artist> artists;

        public ArtistAdapter(Context context, int resource, List<Artist> objects) {
            super(context, resource, objects);
            artists = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            TextView t;
            ImageView image;
            String imageUrl;

            if (v == null) {
                v = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.artist_search_listitem, parent, false);
            }

            Artist a = artists.get(position);
            // name
            t = (TextView)v.findViewById(R.id.artist_name);
            t.setText(a.name);

            // image
            // the middle image is the one we want, but will settle for the smallest
            image = (ImageView)v.findViewById(R.id.artist_image);
            if (a.images.size() >= 2) {
                imageUrl = a.images.get(1).url; // 200px
            } else if (a.images.size() == 1) {
                imageUrl = a.images.get(0).url; // 64px
            } else {
                imageUrl = null;
            }

            Picasso p = Picasso.with(getContext());
            RequestCreator rc;

            if (imageUrl == null || imageUrl.length() == 0) {
                rc = p.load(R.mipmap.no_photo);
            } else {
                rc = p.load(imageUrl);
            }

            //rc.noFade(); // required for the circular image view plugin
            rc.into(image);

            return v;
        }
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
            if (query == null) {
                bindList(new ArrayList<Artist>());
                return;
            }
            bindList(query.artists.items);
        }
    }
}
