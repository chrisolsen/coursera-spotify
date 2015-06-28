package org.chrisolsen.spotify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

    public ArtistSearchActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SpotifyApi api = new SpotifyApi();
        final Context context = getActivity();
        View layout = inflater.inflate(R.layout.artist_search_fragment, container, false);
        final SpotifyService spotify = api.getService();
        final ListView listView = (ListView)layout.findViewById(android.R.id.list);

        // ListView select event
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist a = (Artist)listView.getAdapter().getItem(position);
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

        return layout;
    }

    private void bindList(List<Artist> items) {

        Activity a = getActivity();
        ListView listView = (ListView)a.findViewById(android.R.id.list);
        View noResults = a.findViewById(android.R.id.empty);
        View instructions = a.findViewById(R.id.artists_search_instructions);
        TextView filter = (TextView)a.findViewById(R.id.filter);

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
