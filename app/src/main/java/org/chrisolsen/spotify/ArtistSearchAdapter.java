package org.chrisolsen.spotify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.List;

public class ArtistSearchAdapter extends ArrayAdapter<Artist> {

    private final Context mContext;

    public ArtistSearchAdapter(Context context, List<Artist> artists) {
        super(context, 0, artists);
        mContext = context;
    }

    private class ViewHolder {
        public TextView name;
        public ImageView image;
    }

    /**
     * {@inheritDoc}
     *
     * @param position
     * @param convertView
     * @param parent
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView nameView;
        ImageView imageView;
        ViewHolder holder;

        View view = convertView;

        if (view == null) {
            view = LayoutInflater
                    .from(mContext)
                    .inflate(R.layout.artist_search_listitem, parent, false);

            nameView = (TextView) view.findViewById(R.id.artist_name);
            imageView = (ImageView) view.findViewById(R.id.artist_image);

            holder = new ViewHolder();
            holder.image = imageView;
            holder.name = nameView;

            view.setTag(holder);
        }

        Picasso p = Picasso.with(mContext);
        RequestCreator rc;
        holder = (ViewHolder)view.getTag();
        Artist artist = getItem(position);

        // bind
        holder.name.setText(artist.name);
        rc = artist.imageUrl == null ? p.load(R.mipmap.ic_no_photo) : p.load(artist.imageUrl);
        rc.into(holder.image);

        return view;
    }
}