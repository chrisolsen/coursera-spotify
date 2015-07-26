package org.chrisolsen.spotify;

import android.os.Parcel;
import android.os.Parcelable;

public class Album implements Parcelable {
    Artist artist;
    String albumId, name, imageUrl;

    public Album() {
        artist = new Artist();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.artist, 0);
        dest.writeString(this.albumId);
        dest.writeString(this.name);
        dest.writeString(this.imageUrl);
    }

    protected Album(Parcel in) {
        this.artist = in.readParcelable(Artist.class.getClassLoader());
        this.albumId = in.readString();
        this.name = in.readString();
        this.imageUrl = in.readString();
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        public Album createFromParcel(Parcel source) {
            return new Album(source);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };
}
