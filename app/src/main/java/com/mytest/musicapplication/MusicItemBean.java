package com.mytest.musicapplication;

import androidx.annotation.NonNull;

public class MusicItemBean {
    private static final String TAG = "[MusicApplication] " + MusicItemBean.class.getSimpleName();

    private String id;
    private String name;
    private String singer;
    private String album;
    private String time;
    private String path;

    public MusicItemBean(String id, String name, String singer, String album, String time, String path) {
        this.id = id;
        this.name = name;
        this.singer = singer;
        this.album = album;
        this.time = time;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @NonNull
    @Override
    public String toString() {
        return "MusicItemBean{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", singer='" + singer + '\'' +
                ", album='" + album + '\'' +
                ", time='" + time + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
