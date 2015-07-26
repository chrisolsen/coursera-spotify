package org.chrisolsen.spotify;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ArtistTopSongsActivity
        extends AppCompatActivity
        implements ArtistTopSongsActivityFragment.SongSelectHandler {

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

    @Override
    public void handleSongSelection(Song song) {
        if (isTwoPane()) {
            SongPlayerActivityFragment f = SongPlayerActivityFragment.newInstance(song);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.song_player_fragment, f)
                    .addToBackStack(null)
                    .commit();
            return;
        }

        Intent i = new Intent(this, SongPlayerActivity.class);
        i.putExtra("data", song);
        startActivity(i);
    }

    private boolean isTwoPane() {
        return findViewById(R.id.song_player_fragment) != null;
    }
}
