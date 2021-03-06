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

/**
 * Provides the binding of the data and custom view
 */
public class ArtistTopSongsAdapter extends ArrayAdapter<Song> {
    public ArtistTopSongsAdapter(Context context, int resource, List<Song> objects) {
        super(context, resource, objects);
    }

    private class ViewHolder {
        public ImageView imageView;
        public TextView albumNameView;
        public TextView songNameView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String imageUrl;
        ViewHolder holder;

        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.artist_top_songs_listitem, parent, false);

            holder = new ViewHolder();
            holder.albumNameView = (TextView)view.findViewById(R.id.album_name);
            holder.imageView = (ImageView)view.findViewById(R.id.album_image);
            holder.songNameView = (TextView)view.findViewById(R.id.song_name);

            view.setTag(holder);
        }

        Song song = getItem(position);

        holder = (ViewHolder) view.getTag();
        holder.albumNameView.setText(song.album.name);
        holder.songNameView.setText(song.name);

        imageUrl = song.album.imageUrl;
        if (imageUrl != null) {
            Picasso p = Picasso.with(getContext());
            p.load(imageUrl).into(holder.imageView);
        }

        return view;
    }
}
