package org.chrisolsen.spotify;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * TODOS
 * - make api request for data
 * - create list item view
 * - create the list adapter
 * - create async task
 */

/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopSongsActivityFragment extends Fragment {

    public ArtistTopSongsActivityFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.artist_top_songs_fragment, container, false);

        // perform api request
        Intent intent = getActivity().getIntent();
        String artistId = intent.getStringExtra(ArtistsContract.ArtistEntry.COLUMN_ID);
        String artistName = intent.getStringExtra(ArtistsContract.ArtistEntry.COLUMN_NAME);
        String imageUrl = intent.getStringExtra(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL);

        Log.d("Artist Data", artistId + " " + artistName + " " + imageUrl);

        ImageView bgImage = (ImageView) view.findViewById(R.id.artist_image);
        TextView artistNameView = (TextView) view.findViewById(R.id.artist_name);
        artistNameView.setText(artistName);

        Picasso.with(getActivity()).load(imageUrl).into(bgImage);

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

        TopTenAdapter adapter = new TopTenAdapter(getActivity(), R.layout.artist_top_songs_fragment, tracks);
        listView.setAdapter(adapter);
    }

    private class TopTenAdapter extends ArrayAdapter<Track> {
        public TopTenAdapter(Context context, int resource, List<Track> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            TextView albumNameView;
            TextView songNameView;
            String imageUrl;

            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.artist_top_songs_listitem, parent, false);
            }

            Track track = getItem(position);

            imageView = (ImageView)view.findViewById(R.id.album_image);
            albumNameView = (TextView)view.findViewById(R.id.album_name);
            songNameView = (TextView)view.findViewById(R.id.song_name);

            albumNameView.setText(track.album.name);
            songNameView.setText(track.name);

            List<Image> images = track.album.images;

            if (images.size() >= 2) {
                imageUrl = images.get(1).url; // 200px
            } else if (images.size() == 1) {
                imageUrl = images.get(0).url; // 64px
            } else {
                imageUrl = null;
            }

            Picasso p = Picasso.with(getContext());
            p.load(imageUrl).into(imageView);

            return view;
        }
    }

    private class TopSongRequestTask extends AsyncTask<String, Void, Tracks> {
        SpotifyService spotify;

        public TopSongRequestTask() {
            SpotifyApi api = new SpotifyApi();
            this.spotify = api.getService();
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
