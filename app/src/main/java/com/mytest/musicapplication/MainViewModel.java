package com.mytest.musicapplication;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * ViewModel层
 */
public class MainViewModel {
    private static final String TAG = "[MusicApplication] " + MainViewModel.class.getSimpleName();

    private ArrayList<MusicItemBean> data;

    /**
     * 播放器
     */
    private MediaPlayer mediaPlayer;

    /**
     * 当前正在播放的音乐在list中的下标
     */
    private int currentPlayPosition = -1;

    //记录暂停音乐时进度条的位置
    private int currentPausePositionInSong = 0;

    /**
     * 通知View层(MainActivity)修改UI界面的接口
     */
    private ViewModelListener listener;

    public void setListener(ViewModelListener listener) {
        Log.d(TAG, "setListener");
        this.listener = listener;
    }

    /**
     * 无参构造，用于初始化成员变量
     */
    public MainViewModel() {
        Log.d(TAG, "MainViewModel");
        mediaPlayer = new MediaPlayer();
        data = new ArrayList<>();
    }



    /**
     * 歌曲名
     */
    public ObservableField<String> name = new ObservableField<String>("");

    /**
     * 歌手名
     */
    public ObservableField<String> singer = new ObservableField<>("");

    /**
     * 播放/暂停标志位，根据播放和暂停的状态变化自动变换播放和暂停位置的图片(此项目使用的是字符)
     * true:当前是播放状态，false:当前是暂停状态
     * 播放状态显示暂停按钮，暂停状态显示播放按钮
     */
    public ObservableBoolean playOrPauseFlag = new ObservableBoolean(false);

    /**
     * 加载本地存储当中的音乐文件到集合当中
     */
    public void initMusicData(ContentResolver resolver) {
        Log.d(TAG, "initMusicData");
        //1、获取ContentResolver对象  在View层获取，传递到此方法参数中
//        ContentResolver resolver = getContentResolver();
        //2、获取本地存储的URI地址
        //只能获取到外部存储设备的媒体文件
//        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;


        //此处修改为同时获取外部和内部的媒体文件
        Uri externalUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri internalUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

        Uri[] uris = new Uri[]{externalUri, internalUri};
        int id = 1;
        for (Uri uri : uris) {
            //3开始查询地址
            Cursor cursor = resolver.query(uri, null, null, null, null);
            //4遍历Cursor
            while (cursor.moveToNext()) {
                String songName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                if (duration <= 30000) {
                    Log.d(TAG, "duration = " + duration + " , drop this");
                    continue;
                }
                String sid = String.valueOf(id);
                Log.d(TAG, "sid = " + sid);
                id++;
                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
                String time = sdf.format(new Date(duration));
                Log.d(TAG, "duration = " + duration + " time = " + time);
                //将一行当中的数据封装到对象当中
                MusicItemBean bean = new MusicItemBean(sid, songName, singer, album, time, path);
                data.add(bean);
            }
            //cursor使用后要及时关闭避免内存泄漏，视频中未提出！
            cursor.close();
        }
        //数据源变化，提示adapter更新，adapter在View层，通过接口通知View更新数据
//        adapter.updateData(data);
        if (listener != null) {
            listener.updateData(data);
        } else {
            Log.e(TAG, "initMusicData()-> listener is NULL!");
        }

    }

    /**
     * 上一曲
     */
    public void playBefore() {
        Log.d(TAG, "playBefore");
        if (currentPlayPosition == 0) {
            if (listener != null) {
                listener.makeMyToast("已经是第一首了，没有上一曲辣");
            } else {
                Log.e(TAG, "playBefore()-> listener is NULL!");
            }
            return;
        }
//        currentPlayPosition --;
        //因为在本行就需要使用currentPlayPosition对象，需要先赋值后运算，所以使用--currentPlayPosition而不是 currentPlayPosition--，不信的话自己创建个int值，分两次运行打印i++的值和++i的值就知道了
//        int i = 0; Log.d(TAG, "i = " + ++i);
        MusicItemBean lastBean = data.get(--currentPlayPosition);
        playMusicInPosition(lastBean);
    }

    /**
     * 下一曲
     */
    public void playNext() {
        Log.d(TAG, "playNext");
        if (currentPlayPosition == data.size()-1) {
            if (listener != null) {
                listener.makeMyToast("已经是最后一首了，没有下一曲辣");
            } else {
                Log.e(TAG, "playBefore()-> listener is NULL!");
            }
            return;
        }
//        currentPlayPosition ++;
        //因为在本行就需要使用currentPlayPosition对象，需要先赋值后运算，所以使用++currentPlayPosition而不是 currentPlayPosition++
        MusicItemBean nextBean = data.get(++currentPlayPosition);
        playMusicInPosition(nextBean);
    }

    /**
     * 播放/暂停
     */
    public void playOrPause() {
        Log.d(TAG, "playOrPause()-> currentPlayPosition = " + currentPlayPosition);
        if (currentPlayPosition == -1) {
            //Toast属于View层代码，通过接口传递到Activity处理
            if (listener != null) {
                listener.makeMyToast("请选择想要播放的音乐");
            } else {
                //判空
                Log.e(TAG, "playOrPause()-> listener is NULL!");
            }
            return;
        }
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

//        //打印歌曲信息,最后一个视频中重新封装了播放指定下标的音乐方法，通过将playMusicInPosition()或下方代码分别注释/解注释交替使用
//        Log.d(TAG, bean.toString());
//
//        singer.set(bean.getSinger());
//        name.set(bean.getName());
//        stopMusic();
//
//        //重置多媒体播放器
//        mediaPlayer.reset();
//
//        //设置新的路径
//        try {
//            mediaPlayer.setDataSource(bean.getPath());
//            //设置路径成功后播放新音乐
//            playMusic();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        playMusicInPosition(bean);
    }

    /**
     * 根据传入对象播放音乐
     * 最后一个视频中重新封装了播放指定下标的音乐方法
     * @param bean
     */
    private void playMusicInPosition(MusicItemBean bean) {
        //打印歌曲信息
        Log.d(TAG, bean.toString());

        singer.set(bean.getSinger());
        name.set(bean.getName());
        stopMusic();

        //重置多媒体播放器
        mediaPlayer.reset();

        //设置新的路径
        try {
            mediaPlayer.setDataSource(bean.getPath());
            //设置路径成功后播放新音乐
            playMusic();
        } catch (Exception e) {
            e.printStackTrace();
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
                        e.printStackTrace();
                    }
                } else {
                    //从暂停到播放
                    mediaPlayer.seekTo(currentPausePositionInSong);
                    mediaPlayer.start();
                }

                //开始播放之后把标志位设置为播放状态
                playOrPauseFlag.set(true);
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
        //视频中说mediaPlayer不为空是整个方法的前提，所以此处判空方式为：先判null，再判断播放状态，这两个条件是相互独立的，不要放在同一个if中
        if (mediaPlayer == null){
            Log.e(TAG, "pauseMusic()-> mediaPlayer is NULL!");
            return;
        }
        if (mediaPlayer.isPlaying()){
            currentPausePositionInSong = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();

            //设置为false是暂停状态，也就是需要显示播放按钮
            playOrPauseFlag.set(false);
        }
    }

    /**
     * 播放新音乐之前先停止音乐，View销毁时需要停止播放，此处改为public修饰
     */
    public void stopMusic() {
        if (mediaPlayer != null) {
            //最后一个视频中标记当前播放时长的标志位，在前面视频中先忽略currentPausePositionInSong即可
            currentPausePositionInSong = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();

//      playIv.setImageResourse(xxxxxxxx);此处省略，由于已经使用Databinding实现双向绑定，我们只需要修改playOrPause标志位来控制播放按钮的状态
            playOrPauseFlag.set(false);
        } else {
            //判空应该增加异常打印
            Log.e(TAG, "stopMusic()-> mediaPlayer is NULL!");
        }
    }

    public interface ViewModelListener {

        /**
         * /数据源变化，提示adapter更新，adapter在View层，通过接口通知View更新数据
         * @param data 歌曲信息
         */
        void updateData(ArrayList<MusicItemBean> data);

        void makeMyToast(String msg);
    }
}
