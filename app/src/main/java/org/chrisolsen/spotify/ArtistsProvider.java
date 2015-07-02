package org.chrisolsen.spotify;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class ArtistsProvider extends ContentProvider {

    static final int ARTISTS = 100;

    ContentValues[] mArtists;
    UriMatcher mUriMatcher = buildUriMatcher();

    @Override
    public boolean onCreate() {
        // since we are using a MatrixCursor there is nothing to be done here
        return false;
    }

    static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ArtistsContract.CONTENT_AUTHORITY;

        // /artists
        matcher.addURI(authority, ArtistsContract.PATH_ARTIST, ARTISTS);

        return matcher;
    }

    @Override
    public String getType(Uri uri) {

        final int match = mUriMatcher.match(uri);

        switch(match) {
            case ARTISTS:
                return ArtistsContract.ArtistEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Log.d("ArtistsProvider", "in the query " + uri.toString());

        switch(mUriMatcher.match(uri)) {
            case ARTISTS:

                final String[] cols = new String[] {
                        ArtistsContract.ArtistEntry._ID,
                        ArtistsContract.ArtistEntry.COLUMN_NAME,
                        ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL,
                };

                MatrixCursor c = new  MatrixCursor(cols);

                if (mArtists == null) {
                    return c;
                }

                for (ContentValues vals : mArtists) {
                    MatrixCursor.RowBuilder rb = c.newRow();

                    // ensures order matches projection order
                    for (String col : cols) {
                        rb.add(vals.get(col));
                    }
                }

                return c;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

    }

    /**
     * Override this to handle requests to insert a set of new rows, or the
     * default implementation will iterate over the values and call
     * {@link #insert} on each of them.
     * As a courtesy, call ContentResolver#notifyChange(Uri, ContentObserver) notifyChange()
     * after inserting.
     * This method can be called from multiple threads, as described in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
     * and Threads</a>.
     *
     * @param uri    The content:// URI of the insertion request.
     * @param values An array of sets of column_name/value pairs to add to the database.
     *               This must not be {@code null}.
     * @return The number of values that were inserted.
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        Log.d("ArtistsProvider", "in the bulk insert " + uri.toString() + " " + values.length);
        switch (mUriMatcher.match(uri)) {
            case ARTISTS:
                Log.d("ArtsitsProvider", "in the matcher");
                mArtists = values;
                getContext().getContentResolver().notifyChange(uri, null);
                return values.length;
            default:
                return super.bulkInsert(uri, values);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
