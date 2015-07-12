package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;

public class ArtistSearchLoader extends AsyncTaskLoader<List<ContentValues>> {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private final String mSearchText;

    private SpotifyService mSpotifyApi;

    public ArtistSearchLoader(Context context, String searchText) throws NetworkErrorException {
        super(context);

        mSearchText = searchText;
        mSpotifyApi = new SpotifyApi().getService();

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
             throw new NetworkErrorException();
        }
    }

    /**
     * Subclasses must implement this to take care of loading their data,
     * as per {@link #startLoading()}.  This is not called by clients directly,
     * but as a result of a call to {@link #startLoading()}.
     */
    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public List<ContentValues> loadInBackground() {
        Vector<ContentValues> artists = new Vector<>();
        ArtistsPager results;

        // return empty list for empty search
        if (mSearchText.length() == 0) {
            return new ArrayList<>();
        }

        results = mSpotifyApi.searchArtists(mSearchText);

        if (results == null) {
            return new ArrayList<>();
        }

        for (Artist a : results.artists.items) {
            ContentValues vals = new ContentValues();

            vals.put(ArtistsContract.ArtistEntry.COLUMN_ID, a.id);
            vals.put(ArtistsContract.ArtistEntry.COLUMN_NAME, a.name);

            // get the largest image url
            String imageUrl = null; // default value
            int largestSize = 0;
            for (Image img : a.images) {
                if (img.width > largestSize) {
                    imageUrl = img.url;
                    largestSize = img.width;
                }
            }

            vals.put(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL, imageUrl);

            artists.add(vals);
        }

        return new ArrayList<>(artists);
    }
}
