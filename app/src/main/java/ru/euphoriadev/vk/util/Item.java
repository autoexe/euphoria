package ru.euphoriadev.vk.util;

import android.net.Uri;
import android.os.Parcel;

import java.io.Serializable;

/**
 * Created by autoexec on 08.02.2017.
 */

public class Item {
    public static final String VIDEO_ID = "id";
    public static String description;
    private String FirstName;
    private String LastName;
    private String PhotoAvatar;
    private int Views;
    private String access_key;
    private Long albumId;
    private int duration;
    private int id;
    private String idd;
    private String image;
    private boolean isAd;
    private int likes;
    private String mp4_360;
    private String name;
    private int owner_id;
    private String photo_100;
    private String photo_130;
    private String photo_320;
    private String platform;
    private String player;
    private String post_type;
    private int source_id;
    private String title;

    public Item() {
        isAd = false;
    }

    public Item(final int source_id) {
        isAd = false;
        this.source_id = source_id;
    }

    public Item(final int n, final String title, final String player) {
        isAd = false;
        this.title = title;
        this.player = player;
        image = this.image;
        photo_130 = this.photo_130;
    }

    public Item(final String mp4_360) {
        isAd = false;
        this.mp4_360 = mp4_360;
    }

    public Item(final boolean isAd) {
        this.isAd = false;
        this.isAd = isAd;
    }

    public int describeContents() {
        return 0;
    }

    public String getAccess_key() {
        return this.access_key;
    }

    public Long getAlbumId() {
        return this.albumId;
    }

    public int getDuration() {
        return this.duration;
    }

    public String getExternal(final String s) {
        return this.getPlayer();
    }

    public String getFirstName() {
        return this.FirstName;
    }

    public int getId(final String s) {
        return this.id;
    }

    public String getIdd() {
        return this.idd;
    }

    public String getImage() {
        return this.image;
    }

    public String getLastName() {
        return this.LastName;
    }

    public int getLikes() {
        return this.likes;
    }

    public String getMp4_360() {
        return this.mp4_360;
    }

    public String getMp4_720(final String s) {
        return this.mp4_360;
    }

    public String getName() {
        return this.name;
    }

    public int getOwner_id() {
        return this.owner_id;
    }

    public String getPhotoAvatar() {
        return this.PhotoAvatar;
    }

    public String getPhoto_100() {
        return this.photo_100;
    }

    public String getPhoto_130() {
        return this.photo_130;
    }

    public String getPhoto_320() {
        return this.photo_320;
    }

    public String getPlatform() {
        return this.platform;
    }

    public String getPlayer() {
        return this.player;
    }

    public String getPost_type() {
        return this.post_type;
    }

    public int getSource_id() {
        return this.source_id;
    }

    public String getTitle() {
        return this.title;
    }

    public Uri getURI() {
        return null;
    }

    public int getViews() {
        return this.Views;
    }

    public boolean isAd() {
        return this.isAd;
    }

    public void setAccess_key(final String access_key) {
        this.access_key = access_key;
    }

    public void setAd(final boolean isAd) {
        this.isAd = isAd;
    }

    public void setAlbumId(final Long albumId) {
        this.albumId = albumId;
    }

    public void setDuration(final int duration) {
        this.duration = duration;
    }

    public String setFirstName(final String firstName) {
        return this.FirstName = firstName;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setIdd(final String idd) {
        this.idd = idd;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public void setLastName(final String lastName) {
        this.LastName = lastName;
    }

    public void setLikes(final int likes) {
        this.likes = likes;
    }

    public void setMp4_360(final String mp4_360) {
        this.mp4_360 = mp4_360;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setOwner_id(final int owner_id) {
        this.owner_id = owner_id;
    }

    public void setPhotoAvatar(final String photoAvatar) {
        this.PhotoAvatar = photoAvatar;
    }

    public void setPhoto_100(final String photo_100) {
        this.photo_100 = photo_100;
    }

    public void setPhoto_130(final String photo_130) {
        this.photo_130 = photo_130;
    }

    public void setPhoto_320(final String photo_320) {
        this.photo_320 = photo_320;
    }

    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    public void setPlayer(final String player) {
        this.player = player;
    }

    public void setPost_type(final String post_type) {
        this.post_type = post_type;
    }

    public void setSource_id(final int source_id) {
        this.source_id = source_id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setViews(final int views) {
        this.Views = views;
    }

    public void writeToParcel(final Parcel parcel, final int n) {
    }
}
