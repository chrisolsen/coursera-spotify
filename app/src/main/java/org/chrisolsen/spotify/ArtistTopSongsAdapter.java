package org.chrisolsen.spotify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Provides the binding of the data and custom view
 */
public class ArtistTopSongsAdapter extends ArrayAdapter<Track> {
    public ArtistTopSongsAdapter(Context context, int resource, List<Track> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        TextView albumNameView;
        TextView songNameView;
        String imageUrl;

        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.artist_top_songs_listitem, parent, false);
        }

        Track track = getItem(position);

        imageView = (ImageView)view.findViewById(R.id.album_image);
        albumNameView = (TextView)view.findViewById(R.id.album_name);
        songNameView = (TextView)view.findViewById(R.id.song_name);

        albumNameView.setText(track.album.name);
        songNameView.setText(track.name);

        List<Image> images = track.album.images;

        if (images.size() >= 2) {
            imageUrl = images.get(1).url; // 200px
        } else if (images.size() == 1) {
            imageUrl = images.get(0).url; // 64px
        } else {
            imageUrl = null;
        }

        if (imageUrl != null) {
            Picasso p = Picasso.with(getContext());
            p.load(imageUrl).into(imageView);
        }

        return view;
    }
}
