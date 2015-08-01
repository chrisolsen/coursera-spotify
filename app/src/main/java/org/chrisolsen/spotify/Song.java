package org.chrisolsen.spotify;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {

    Album album;
    String songId, name, url, previewUrl;

    public Song() {
        album = new Album();
    }

    protected Song(Parcel in) {
        album = in.readParcelable(Album.class.getClassLoader());
        songId = in.readString();
        name = in.readString();
        url = in.readString();
        previewUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(album, flags);
        dest.writeString(songId);
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(previewUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
