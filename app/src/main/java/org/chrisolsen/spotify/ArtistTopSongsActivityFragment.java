package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopSongsActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ContentValues>> {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private String mArtistId;
    private ListView mListView;
    private ArtistTopSongsAdapter mAdapter;
    private View mNoResults;

    public ArtistTopSongsActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ContentValues artist;
        Bundle args = getArguments();

        if (args == null) {
            return inflater.inflate(R.layout.artist_top_songs_init, container, false);
        }

        artist = args.getParcelable("data");

        View view = inflater.inflate(R.layout.artist_top_songs_fragment, container, false);
        mListView = (ListView)view.findViewById(android.R.id.list);
        mNoResults = view.findViewById(android.R.id.empty);

        mArtistId = artist.getAsString(ArtistsContract.ArtistEntry.COLUMN_ID);
        String artistName = artist.getAsString(ArtistsContract.ArtistEntry.COLUMN_NAME);

        try {
            AppCompatActivity a = (AppCompatActivity)getActivity();
            a.getSupportActionBar().setTitle(R.string.title_activity_artist_top_songs);
            a.getSupportActionBar().setSubtitle(artistName);
        } catch(NullPointerException ignored) {
            Toast.makeText(this.getActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
            return null;
        }

        mAdapter = new ArtistTopSongsAdapter(getActivity(), R.layout.artist_top_songs_fragment, new ArrayList<ContentValues>());
        mListView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
        setRetainInstance(true);
    }

    @Override
    public Loader<List<ContentValues>> onCreateLoader(int id, Bundle args) {
        if (mArtistId == null) return null;

        try {
            return new ArtistTopSongsLoader(getActivity(), mArtistId);
        } catch (NetworkErrorException e) {
            Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<ContentValues>> loader, List<ContentValues> data) {

        // TODO: holder on to a reference to the returned data to save the instance state in stage 2
        // when we are able to drill into the song

        boolean hasSongs = data.size() > 0;
        mListView.setVisibility(hasSongs ? View.VISIBLE : View.GONE);
        mNoResults.setVisibility(hasSongs ? View.GONE : View.VISIBLE);

        mAdapter.clear();
        mAdapter.addAll(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<ContentValues>> loader) {
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
    }
}
