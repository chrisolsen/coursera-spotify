package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
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

public class ArtistTopSongsLoader extends AsyncTaskLoader<List<Song>> {

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
    public List<Song> loadInBackground() {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        Map<String, Object> query = new HashMap<>();
        query.put("country", "ca");  // TODO: make this a preference setting

        Tracks tracks = spotify.getArtistTopTrack(mArtistId, query);

        List<Song> songs = new ArrayList<>(tracks.tracks.size());
        for (Track t : tracks.tracks) {
            Song song = new Song();
            song.album.artist.name = t.artists.get(0).name;
            song.album.name = t.album.name;
            song.name = t.name;

            String imageUrl = t.album.images.get(0).url;
            int maxSize = 0;
            for(Image image : t.album.images) {
                if (image.width > maxSize) {
                    imageUrl = image.url;
                    maxSize = image.width;
                }
            }

            song.album.imageUrl = imageUrl;

            songs.add(song);
        }

        return songs;
    }
}
