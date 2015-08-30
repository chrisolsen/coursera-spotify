package org.chrisolsen.spotify;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SongPlayerActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_player_activity);

        if (savedInstanceState == null) {
            SongPlayerActivityFragment frag = SongPlayerActivityFragment.newInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .commit();
        }
    }
}
