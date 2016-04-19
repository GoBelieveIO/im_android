package com.beetle.bauhinia.gallery;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by hillwind
 */
public class GalleryImage implements Parcelable {

    public String path;

    public GalleryImage(String path) {
        this.path = path;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.path);
    }

    protected GalleryImage(Parcel in) {
        this.path = in.readString();
    }

    public static final Creator<GalleryImage> CREATOR = new Creator<GalleryImage>() {
        @Override
        public GalleryImage createFromParcel(Parcel source) {
            return new GalleryImage(source);
        }

        @Override
        public GalleryImage[] newArray(int size) {
            return new GalleryImage[size];
        }
    };

    @Override
    public String toString() {
        return "Image{" +
                "path='" + path + '\'' +
                '}';
    }
}
