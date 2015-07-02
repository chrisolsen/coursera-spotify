package org.chrisolsen.spotify;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

public class ArtistsCursorAdapter extends CursorAdapter {
    public ArtistsCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater
                .from(context)
                .inflate(R.layout.artist_search_listitem, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nameView;
        ImageView imageView;
        Picasso p = Picasso.with(context);
        RequestCreator rc;

        Log.d("ArtistCursor", "bindView");

        nameView = (TextView)view.findViewById(R.id.artist_name);
        imageView = (ImageView)view.findViewById(R.id.artist_image);

        // cursor indices
        int colName = cursor.getColumnIndex(ArtistsContract.ArtistEntry.COLUMN_NAME);
        int colImageUrl = cursor.getColumnIndex(ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL);

        // cursor values
        String name = cursor.getString(colName);
        String imageUrl = cursor.getString(colImageUrl);

        // bind
        nameView.setText(name);
        rc = imageUrl == null ? p.load(R.mipmap.no_photo) : p.load(imageUrl);
        rc.into(imageView);
    }
}