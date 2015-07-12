package org.chrisolsen.spotify;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopSongsActivityFragment extends Fragment {

    public ArtistTopSongsActivityFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.artist_top_songs_fragment, container, false);

        // to fetch the data
        Intent intent = getActivity().getIntent();
        ContentValues artist = intent.getParcelableExtra("data");

        String imageUrl = artist.getAsString(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL);
        String artistName = artist.getAsString(ArtistsContract.ArtistEntry.COLUMN_NAME);
        String artistId = artist.getAsString(ArtistsContract.ArtistEntry.COLUMN_ID);

        ImageView bgImage = (ImageView) view.findViewById(R.id.artist_image);

        if (imageUrl != null) {
            Picasso.with(getActivity()).load(imageUrl).into(bgImage);
        } else {
            bgImage.setVisibility(View.GONE);
        }

        try {
            AppCompatActivity a = (AppCompatActivity)getActivity();
            a.getSupportActionBar().setTitle(R.string.title_activity_artist_top_songs);
            a.getSupportActionBar().setSubtitle(artistName);
        } catch(NullPointerException ignored) {
            Toast.makeText(this.getActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
            return null;
        }

        new TopSongRequestTask().execute(artistId);

        return view;
    }

    private void bindList(List<Track> tracks) {
        View parent = getView();
        View noResults = parent.findViewById(android.R.id.empty);
        ListView listView = (ListView)parent.findViewById(android.R.id.list);

        noResults.setVisibility(tracks.size() > 0 ? View.GONE : View.VISIBLE);

        ArtistTopSongsAdapter adapter = new ArtistTopSongsAdapter(getActivity(), R.layout.artist_top_songs_fragment, tracks);
        listView.setAdapter(adapter);
    }

    private class TopSongRequestTask extends AsyncTask<String, Void, Tracks> {
        SpotifyService spotify;

        public TopSongRequestTask() {
            SpotifyApi api = new SpotifyApi();
            this.spotify = api.getService();

            ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();

            if (info == null || !info.isConnected()) {
                Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                this.cancel(true);
            }
        }

        @Override
        protected Tracks doInBackground(String... params) {
            String artistId = params[0];
            Map<String, Object> query = new HashMap<>();
            query.put("country", "ca");
            return spotify.getArtistTopTrack(artistId, query);
        }

        @Override
        protected void onPostExecute(Tracks data) {
            bindList(data.tracks);
        }
    }
}
