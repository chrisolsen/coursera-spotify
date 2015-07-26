package org.chrisolsen.spotify;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class SongPlayerActivityFragment extends DialogFragment {

    TextView txtSongName, txtArtistName, txtAlbumName;
    ImageView imgAlbumImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_song_player, container, false);

        if (getArguments() == null) {
            return view;
        }

        txtSongName = (TextView) view.findViewById(R.id.song_name);
        txtArtistName = (TextView) view.findViewById(R.id.artist_name);
        txtAlbumName = (TextView) view.findViewById(R.id.album_name);
        imgAlbumImage = (ImageView) view.findViewById(R.id.album_image);

        Song song = getArguments().getParcelable("data");

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

    // Remove the dialog title bar
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public static SongPlayerActivityFragment newInstance(Song song) {
        SongPlayerActivityFragment f = new SongPlayerActivityFragment();
        Bundle b = new Bundle();
        b.putParcelable("data", song);
        f.setArguments(b);

        return f;
    }
}
