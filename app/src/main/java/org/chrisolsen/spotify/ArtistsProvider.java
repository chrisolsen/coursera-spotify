package org.chrisolsen.spotify;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class ArtistsProvider extends ContentProvider {

    static final int ARTISTS = 100;
    static final int ARTIST = 101;

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
        matcher.addURI(authority, ArtistsContract.PATH_ARTISTS, ARTISTS);
        matcher.addURI(authority, ArtistsContract.PATH_ARTISTS + "/#", ARTIST);

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

        MatrixCursor c;

        switch(mUriMatcher.match(uri)) {
            case ARTISTS:

                final String[] cols = new String[] {
                        ArtistsContract.ArtistEntry._ID,
                        ArtistsContract.ArtistEntry.COLUMN_ID,
                        ArtistsContract.ArtistEntry.COLUMN_NAME,
                        ArtistsContract.ArtistEntry.COLUMN_IMAGE_URL,
                };

                c = new  MatrixCursor(cols);

                if (mArtists == null) {
                    break;
                }

                for (ContentValues vals : mArtists) {
                    MatrixCursor.RowBuilder rb = c.newRow();

                    // ensures order matches projection order
                    for (String col : cols) {
                        rb.add(vals.get(col));
                    }
                }

                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        switch (mUriMatcher.match(uri)) {
            case ARTISTS:
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
