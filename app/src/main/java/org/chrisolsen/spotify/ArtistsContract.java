package org.chrisolsen.spotify;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class ArtistsContract {

    // application's content provider indentifier
    public static final String CONTENT_AUTHORITY = "org.chrisolsen.spotify";

    // Uri shared by all content providers for this app
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // paths for this app's content providers
    public static final String PATH_ARTISTS = "artists";

    public static class ArtistEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_ARTISTS).build();

        // This is similar to mimetype => application/json
        //  application: android data type
        //  json: artist(s)
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_ARTISTS;

        // public static final String CONTENT_ITEM_TYPE = ... for a single artist

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_IMAGE_URL = "image_url";

    }
}
