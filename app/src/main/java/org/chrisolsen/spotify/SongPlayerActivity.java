package org.chrisolsen.spotify;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;

public class SongPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_player_activity);

        if (savedInstanceState == null) {
            Parcelable[] data = getIntent().getParcelableArrayExtra("songs");
            Song[] songs = new Song[data.length];
            for (int i = 0; i < data.length; i++) {
                songs[i] = (Song) data[i];
            }
            int playIndex = getIntent().getIntExtra("playIndex", 0);
            SongPlayerActivityFragment frag = SongPlayerActivityFragment.newInstance(songs, playIndex);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .commit();
        }
    }
}
