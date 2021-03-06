package org.chrisolsen.spotify;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
    Album album;
    String songId, name, href, previewUrl;
    long duration;
    int popularity;
    boolean explicit;

    public Song() {
        album = new Album();
    }

    protected Song(Parcel in) {
        this.album = in.readParcelable(Album.class.getClassLoader());
        this.songId = in.readString();
        this.name = in.readString();
        this.href = in.readString();
        this.previewUrl = in.readString();
        this.duration = in.readLong();
        this.popularity = in.readInt();
        this.explicit = in.readByte() != 0;
    }

    public boolean equals(Song s) {
        if (s == null) {
            return false;
        }
        return this.name.equals(s.name);
    }

    @Override
    public int hashCode() {
        return href.hashCode();
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
        dest.writeString(this.href);
        dest.writeString(this.previewUrl);
        dest.writeLong(this.duration);
        dest.writeInt(this.popularity);
        dest.writeByte(explicit ? (byte) 1 : (byte) 0);
    }
}
