package org.chrisolsen.spotify;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SongPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_player);

        if (savedInstanceState == null) {
            Song song = getIntent().getParcelableExtra("data");
            SongPlayerActivityFragment frag = SongPlayerActivityFragment.newInstance(song);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .commit();
        }
    }
}
