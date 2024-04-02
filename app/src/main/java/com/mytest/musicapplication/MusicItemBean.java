package com.mytest.musicapplication;

import androidx.annotation.NonNull;

public class MusicItemBean {
    private static final String TAG = "[MusicApplication] " + MusicItemBean.class.getSimpleName();
    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private String singer;
    @NonNull
    private String album;
    @NonNull
    private String time;
    @NonNull
    private String path;
    private long duration;

    public MusicItemBean(@NonNull String id, @NonNull String name, @NonNull String singer, @NonNull String album, @NonNull String time, @NonNull String path, long duration) {
        this.id = id;
        this.name = name;
        this.singer = singer;
        this.album = album;
        this.time = time;
        this.path = path;
        this.duration = duration;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getSinger() {
        return singer;
    }

    @NonNull
    public String getAlbum() {
        return album;
    }

    @NonNull
    public String getTime() {
        return time;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
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
                ", duration=" + duration +
                '}';
    }
}
