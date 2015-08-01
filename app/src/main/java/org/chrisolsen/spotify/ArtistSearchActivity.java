package org.chrisolsen.spotify;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

// TODO: Add accessability to app https://www.udacity.com/course/viewer#!/c-ud853-nd/l-1623168625/e-1667758627/m-1667758629

public class ArtistSearchActivity
        extends AppCompatActivity
        implements ArtistSearchActivityFragment.ArtistSelectionHandler,
        ArtistTopSongsActivityFragment.SongSelectHandler {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private String mCurrentArtistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_search_activity);

        if (isTwoPane() && savedInstanceState == null) {
            loadDetailsFragment(null);
        }
    }

    /**
     * Whether the details pane is also visible on the screen
     * @return boolean
     */
    private boolean isTwoPane() {
        return findViewById(R.id.top_songs_container) != null;
    }

    /**
     * Method called by a child fragment to load the details based on the current screen.
     * @param artist
     */
    public void handleArtistSelection(Artist artist) {
        if (isTwoPane()) {
            loadDetailsFragment(artist);
        } else {
            openDetailsActivity(artist);
        }
    }

    @Override
    public void handleSongSelection(Song[] songs, int playIndex) {
        FragmentManager mgr = getSupportFragmentManager();
        SongPlayerActivityFragment frag = SongPlayerActivityFragment.newInstance(songs, playIndex);

        if (isTwoPane()) {
            frag.show(mgr, "song");
        } else {
            Intent i = new Intent(this, SongPlayerActivity.class);
            i.putExtra("songs", songs);
            i.putExtra("playIndex", playIndex);
            startActivity(i);
        }
    }

    /**
     * Loads the artist details fragment into the current activity
     */
    private void loadDetailsFragment(Artist artist) {
        if (artist == null || mCurrentArtistId == artist.artistId) {
            return;
        }

        mCurrentArtistId = artist.artistId;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ArtistTopSongsActivityFragment frag = new ArtistTopSongsActivityFragment();

        if (artist != null) {
            Bundle args = new Bundle();
            args.putParcelable("data", artist);
            frag.setArguments(args);
        }

        ft.replace(R.id.top_songs_container, frag);
        ft.commit();
    }

    /**
     * Opens the details activity
     */
    private void openDetailsActivity(Artist artist) {
        Intent intent = new Intent(this, ArtistTopSongsActivity.class);
        intent.putExtra("data", artist);
        startActivity(intent);
    }
}
