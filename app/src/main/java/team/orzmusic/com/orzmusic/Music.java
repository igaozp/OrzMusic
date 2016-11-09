package team.orzmusic.com.orzmusic;

import android.net.Uri;

import java.io.Serializable;

public class Music implements Serializable {
    private String musicName;
    private String artist;
    private String albumName;
    private int duration;
    private String musicData;
    private String albumArtWorkUri;
    private int albumID;
    private Long ID;
    private String simpleTime;

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getAlbumArtWorkUri() {
        return albumArtWorkUri;
    }

    public void setAlbumArtWorkUri(String albumArtWorkUri) {
        this.albumArtWorkUri = albumArtWorkUri;
    }

    public int getAlbumID() {
        return albumID;
    }

    public void setAlbumID(int albumID) {
        this.albumID = albumID;
    }

    public String getMusicData() {
        return musicData;
    }

    public void setMusicData(String musicData) {
        this.musicData = musicData;
    }

    public Long getID() {
        return ID;
    }

    public void setID(Long ID) {
        this.ID = ID;
    }

    public String getSimpleTime() {
        return simpleTime;
    }

    public void setSimpleTime(String simpleTime) {
        this.simpleTime = simpleTime;
    }
}
