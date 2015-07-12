package org.chrisolsen.spotify;

import android.provider.BaseColumns;

public class ArtistsContract {
    public static class ArtistEntry implements BaseColumns {
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_IMAGE_URL = "image_url";
    }
}
