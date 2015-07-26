package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ArtistTopSongsActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Song>> {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private String mArtistId;
    private ListView mListView;
    private ArtistTopSongsAdapter mAdapter;
    private View mNoResults;

    private Song[] mSongs;

    public ArtistTopSongsActivityFragment() {}

    public interface SongSelectHandler {
        void handleSongSelection(Song s);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Artist artist;
        Bundle args = getArguments();

        if (args == null) {
            return inflater.inflate(R.layout.artist_top_songs_init, container, false);
        }

        View view = inflater.inflate(R.layout.artist_top_songs_fragment, container, false);

        artist = args.getParcelable("data");
        if (artist == null) {
            return view;
        }

        mListView = (ListView)view.findViewById(android.R.id.list);
        mNoResults = view.findViewById(android.R.id.empty);

        mArtistId = artist.artistId;

        try {
            AppCompatActivity a = (AppCompatActivity)getActivity();
            a.getSupportActionBar().setTitle(R.string.title_activity_artist_top_songs);
            a.getSupportActionBar().setSubtitle(artist.name);
        } catch(NullPointerException ignored) {
            Toast.makeText(this.getActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
            return null;
        }

        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song song = (Song)mListView.getAdapter().getItem(position);
                SongSelectHandler handler = (SongSelectHandler)getActivity();
                handler.handleSongSelection(song);
            }
        });

        mAdapter = new ArtistTopSongsAdapter(getActivity(), R.layout.artist_top_songs_fragment, new ArrayList<Song>());
        mListView.setAdapter(mAdapter);

        // restore the instance state.
        // NOTE: this is done within the onCreateView due to the onViewStateRestored not being
        // called until after the onCreateLoader is called, thereby not allowing for the http
        // request to be skipped.
        if (savedInstanceState != null && savedInstanceState.getParcelableArray("songs") != null) {
            Parcelable[] p = savedInstanceState.getParcelableArray("songs");
            Song[] songs = new Song[p.length];
            for (int i = 0; i < p.length; i++) {
                songs[i] = (Song)p[i];
            }

            bindSongs(songs);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray("songs", mSongs);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
        setRetainInstance(true);
    }

    @Override
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        if (mArtistId == null) return null;
        if (mSongs != null && mSongs.length > 0) return null;

        try {
            return new ArtistTopSongsLoader(getActivity(), mArtistId);
        } catch (NetworkErrorException e) {
            Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Song>> loader, List<Song> songs) {
        if (mSongs != null) return;

        Song[] songArr = new Song[songs.size()];
        songs.toArray(songArr);
        bindSongs(songArr);
    }

    private void bindSongs(Song[] songs) {
        boolean hasSongs;

        mSongs = songs;
        hasSongs = songs.length > 0;
        mListView.setVisibility(hasSongs ? View.VISIBLE : View.GONE);
        mNoResults.setVisibility(hasSongs ? View.GONE : View.VISIBLE);

        mAdapter.clear();
        mAdapter.addAll(songs);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<Song>> loader) {
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
    }
}
