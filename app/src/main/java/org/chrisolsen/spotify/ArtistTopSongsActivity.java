package org.chrisolsen.spotify;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ArtistTopSongsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_top_songs_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArtistTopSongsActivityFragment topSongsActivityFragment = new ArtistTopSongsActivityFragment();
        topSongsActivityFragment.setArguments(getIntent().getExtras());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.top_songs_container, topSongsActivityFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }


}
