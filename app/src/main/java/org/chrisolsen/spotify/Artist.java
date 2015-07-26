package org.chrisolsen.spotify;

import android.os.Parcel;
import android.os.Parcelable;

public class Artist implements Parcelable {
    String artistId, name, imageUrl;

    public Artist() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.artistId);
        dest.writeString(this.name);
        dest.writeString(this.imageUrl);
    }

    protected Artist(Parcel in) {
        this.artistId = in.readString();
        this.name = in.readString();
        this.imageUrl = in.readString();
    }

    public static final Creator<Artist> CREATOR = new Creator<Artist>() {
        public Artist createFromParcel(Parcel source) {
            return new Artist(source);
        }

        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };
}
