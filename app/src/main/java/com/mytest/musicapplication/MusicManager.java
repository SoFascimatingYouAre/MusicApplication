package com.mytest.musicapplication;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MusicManager {
    private static final String TAG = MusicManager.class.getSimpleName();

    private static volatile MusicManager musicManager;

    private MusicManager() {}


    public static MusicManager getInstance() {
        if (musicManager == null) {
            synchronized (MusicManager.class) {
                if (musicManager == null) {
                    musicManager = new MusicManager();
                }
                return musicManager;
            }
        }
        return musicManager;
    }

    /**
     * 播放器
     */
    private MediaPlayer mediaPlayer;


    /**
     * 播放列表
     */
    public ArrayList<MusicItemBean> data;

    /**
     * 当前正在播放的音乐在list中的下标
     */
    public int currentPlayPosition = -1;

    //记录暂停音乐时进度条的位置
    private int currentPausePositionInSong = 0;

    private MusicDataListener musicDataListener;

    public void createPlayerAndData(ContentResolver resolver) {
        mediaPlayer = new MediaPlayer();
        data = new ArrayList<>();
        initMusicData(resolver);
    }

    public void setMusicDataListener(MusicDataListener musicDataListener) {
        this.musicDataListener = musicDataListener;
    }

    /**
     * 加载本地存储当中的音乐文件到集合当中
     */
    @SuppressLint("Range")
    private void initMusicData(ContentResolver resolver) {
        Log.d(TAG, "initMusicData");
        //同时获取外部和内部的媒体文件
        Uri externalUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri internalUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

        Uri[] uris = new Uri[]{externalUri, internalUri};
        int id = 1;
        for (Uri uri : uris) {
            //开始查询地址
            Cursor cursor = resolver.query(uri, null, null, null, null);
            //遍历Cursor
            while (cursor.moveToNext()) {
                String songName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                if (duration <= 30000) {
                    Log.v(TAG, "duration = " + duration + " , drop this");
                    continue;
                }
                String sid = String.valueOf(id);
                Log.d(TAG, "sid = " + sid);
                id++;
                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
                String time = sdf.format(new Date(duration));
                Log.v(TAG, "duration = " + duration + " time = " + time);
                //将一行当中的数据封装到对象当中
                MusicItemBean bean = new MusicItemBean(sid, songName, singer, album, time, path);
                data.add(bean);
            }
            //cursor使用后要及时关闭避免内存泄漏，视频中未提出！
            cursor.close();
        }

    }

    /**
     * 上一曲
     */
    public void playLast() {
        Log.d(TAG, "playLast");
        MusicItemBean lastBean = data.get(--currentPlayPosition);
        playMusicInPosition(lastBean);
    }

    /**
     * 下一曲
     */
    public void playNext() {
        Log.d(TAG, "playNext");
        if (MusicManager.getInstance().currentPlayPosition == MusicManager.getInstance().data.size()-1) {
            return;
        }
        MusicItemBean nextBean = data.get(++currentPlayPosition);
        playMusicInPosition(nextBean);
    }

    /**
     * 播放/暂停
     */
    public void playOrPause() {
        Log.d(TAG, "playOrPause()-> currentPlayPosition = " + currentPlayPosition);
        if (mediaPlayer.isPlaying()) {
            //此时处于播放状态，需要暂停音乐
            pauseMusic();
        } else {
            //此时没有播放音乐，点击开始播放音乐
            playMusic();
        }

    }

    /**
     * 点击列表歌曲，准备播放
     */
    public void playNewSong(int position) {
        currentPlayPosition = position;
        MusicItemBean bean = data.get(position);
        playMusicInPosition(bean);
    }

    /**
     * 根据传入对象播放音乐
     * 最后一个视频中重新封装了播放指定下标的音乐方法
     * @param bean 音乐信息实体类
     */
    private void playMusicInPosition(MusicItemBean bean) {
        //打印歌曲信息
        Log.d(TAG, bean.toString());

        musicDataListener.onSingerChanged(bean.getSinger());
        musicDataListener.onNameChanged(bean.getName());
        stopMusic();

        //重置多媒体播放器
        mediaPlayer.reset();

        //设置新的路径
        try {
            mediaPlayer.setDataSource(bean.getPath());
            //设置路径成功后播放新音乐
            playMusic();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    /**
     * 播放新音乐时暂停后，播放音乐的函数
     * 点击播放按钮播放音乐，或者暂停音乐从新播放
     * 1，从暂停到播放
     * 2，从停止到播放
     */
    private void playMusic() {
        if (mediaPlayer != null) {
            //判空和状态判断应该分开处理
            if (!mediaPlayer.isPlaying()) {

                //最后一个视频中标记当前播放时长的标志位，在前面视频中先看这个判断内部的东西
                if (currentPausePositionInSong == 0) {

                    //从停止到播放
                    try {
                        mediaPlayer.prepare();
                        mediaPlayer.start();

                    } catch (IOException e) {
                        Log.d(TAG, e.toString());
                    }
                } else {
                    //从暂停到播放
                    mediaPlayer.seekTo(currentPausePositionInSong);
                    mediaPlayer.start();
                }

                //开始播放之后把标志位设置为播放状态
                musicDataListener.onPlayStatusChanged(true);
            }
        } else {
            //判空应该增加异常打印
            Log.e(TAG, "stopMusic()-> mediaPlayer is NULL!");
        }
    }

    /**
     * 暂停音乐
     */
    private void pauseMusic() {
        if (mediaPlayer == null){
            Log.e(TAG, "pauseMusic()-> mediaPlayer is NULL!");
            return;
        }
        if (mediaPlayer.isPlaying()){
            currentPausePositionInSong = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();

            //设置为false是暂停状态，也就是需要显示播放按钮
            musicDataListener.onPlayStatusChanged(false);
        }
    }

    /**
     * 播放新音乐之前先停止音乐，View销毁时需要停止播放，此处改为public修饰
     */
    public void stopMusic() {
        if (mediaPlayer != null) {
            currentPausePositionInSong = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            musicDataListener.onPlayStatusChanged(false);
        } else {
            //判空应该增加异常打印
            Log.e(TAG, "stopMusic()-> mediaPlayer is NULL!");
        }
    }

    public interface MusicDataListener {
        void onNameChanged(String newName);

        void onSingerChanged(String newSinger);

        void onPlayStatusChanged(Boolean isPlaying);
    }
}