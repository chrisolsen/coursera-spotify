package org.chrisolsen.spotify;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * A placeholder fragment containing a simple view.
 */
public class SongPlayerActivityFragment extends Fragment {

    TextView txtSongName, txtArtistName, txtAlbumName;
    ImageView imgAlbumImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_song_player, container, false);

        txtSongName = (TextView) view.findViewById(R.id.song_name);
        txtArtistName = (TextView) view.findViewById(R.id.artist_name);
        txtAlbumName = (TextView) view.findViewById(R.id.album_name);
        imgAlbumImage = (ImageView) view.findViewById(R.id.album_image);

        Song song = getActivity().getIntent().getParcelableExtra("data");

        txtSongName.setText(song.name);
        txtArtistName.setText(song.album.artist.name);
        txtAlbumName.setText(song.album.name);

        // album image || artist image
        String imageUrl = song.album.imageUrl == null ? song.album.artist.imageUrl : song.album.imageUrl;

        Picasso.with(getActivity())
                .load(imageUrl)
                .into(imgAlbumImage);

        return view;
    }
}
