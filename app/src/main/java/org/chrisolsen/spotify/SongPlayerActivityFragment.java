package org.chrisolsen.spotify;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class SongPlayerActivityFragment extends DialogFragment implements View.OnClickListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    TextView txtSongName, txtArtistName, txtAlbumName;
    ImageButton btnPrev, btnPlay, btnNext;
    ImageView imgAlbumImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.song_player_fragment, container, false);

        if (getArguments() == null) {
            return view;
        }

        txtSongName = (TextView) view.findViewById(R.id.song_name);
        txtArtistName = (TextView) view.findViewById(R.id.artist_name);
        txtAlbumName = (TextView) view.findViewById(R.id.album_name);
        imgAlbumImage = (ImageView) view.findViewById(R.id.album_image);

        btnPrev = (ImageButton) view.findViewById(R.id.btn_previous_song);
        btnPlay = (ImageButton) view.findViewById(R.id.btn_play);
        btnNext = (ImageButton) view.findViewById(R.id.btn_next_song);

        btnPrev.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnNext.setOnClickListener(this);

        // list of songs to allow skipping between songs
        Parcelable[] plist = getArguments().getParcelableArray("songs");
        Song[] songs = new Song[plist.length];
        for (int i = 0; i < plist.length; i++) {
            songs[i] = (Song)plist[i];
        }

        // song to play
        int playIndex = getArguments().getInt("playIndex");
        Song song = songs[playIndex];

        // song details
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

    public static SongPlayerActivityFragment newInstance(Song[] songs, int playIndex) {
        SongPlayerActivityFragment f = new SongPlayerActivityFragment();
        Bundle b = new Bundle();
        b.putParcelableArray("songs", songs);
        b.putInt("playIndex", playIndex);
        f.setArguments(b);

        return f;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_play) {
            play(v);
        } else
        if (id == R.id.btn_previous_song) {
            playPrevious(v);
        } else
        if (id == R.id.btn_next_song) {
            playNext(v);
        }
    }

    public void playPrevious(View v) {
        Log.d(LOG_TAG, "previous");
    }

    public void play(View v) {
        Log.d(LOG_TAG, "play");

    }

    public void playNext(View v) {
        Log.d(LOG_TAG, "next");

    }
}
