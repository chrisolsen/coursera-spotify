package org.chrisolsen.spotify;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {

    Album album;
    String songId, name;

    public Song() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.album, 0);
        dest.writeString(this.songId);
        dest.writeString(this.name);
    }

    protected Song(Parcel in) {
        this.album = in.readParcelable(Album.class.getClassLoader());
        this.songId = in.readString();
        this.name = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
