package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

public class ArtistTopSongsLoader extends AsyncTaskLoader<List<ContentValues>> {

    String mArtistId;

    public ArtistTopSongsLoader(Context context, String artistId) throws NetworkErrorException {
        super(context);

        mArtistId = artistId;

        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
            throw new NetworkErrorException();
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public List<ContentValues> loadInBackground() {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        Map<String, Object> query = new HashMap<>();
        query.put("country", "ca");  // TODO: make this a preference setting
        Tracks tracks = spotify.getArtistTopTrack(mArtistId, query);

        List<ContentValues> data = new ArrayList<>(tracks.tracks.size());
        for (Track t : tracks.tracks) {
            ContentValues vals = new ContentValues();

            vals.put("albumName", t.album.name);
            vals.put("songName", t.name);

            String imageUrl;
            List<Image> images = t.album.images;
            if (images.size() >= 2) {
                imageUrl = images.get(1).url; // 200px
            } else if (images.size() == 1) {
                imageUrl = images.get(0).url; // 64px
            } else {
                imageUrl = null;
            }
            vals.put("imageUrl", imageUrl);

            data.add(vals);
        }

        return data;
    }
}
