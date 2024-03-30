package com.mytest.musicapplication;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel层
 */
public class MainViewModel {
    private static final String TAG = "[MusicApplication] " + MainViewModel.class.getSimpleName();

    private MediaBrowserCompat mBrowser;
    private MediaControllerCompat mController;

    /**
     * 通知View层(MainActivity)修改UI界面的接口
     */
    private ViewModelListener listener;

    public void setListener(ViewModelListener listener) {
        Log.d(TAG, "setListener");
        this.listener = listener;
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
     * TODO: 当前是直接修改状态并变更UI，应该修改为先修改状态，再根据状态变更的回调再更新UI
     */
    public ObservableBoolean playOrPauseFlag = new ObservableBoolean(false);

    public void onDestroy() {
        Log.d(TAG, "disconnect()");
        mBrowser.disconnect();
        stopMusic();
        setListener(null);
        MusicManager.getInstance().unRegisterMusicDataListener(musicDataListener);
    }

    /**
     * 加载本地存储当中的音乐文件到集合当中
     */
    @SuppressLint("Range")
    public void createPlayerAndData(ContentResolver resolver) {
        MusicManager.getInstance().createPlayerAndData(resolver);
        MusicManager.getInstance().registerMusicDataListener(musicDataListener);
        if (listener != null) {
            listener.updateData(MusicManager.getInstance().data);
        } else {
            Log.e(TAG, "initMusicData()-> listener is NULL!");
        }
    }

    private final MusicManager.MusicDataListener musicDataListener = new MusicManager.MusicDataListener() {
        @Override
        public void onMusicDataChanged(String newName, String newSinger) {
            name.set(newName);
            singer.set(newSinger);
        }

        @Override
        public void onPlayStatusChanged(Boolean isPlaying) {
            playOrPauseFlag.set(isPlaying);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat status) {

        }
    };

    public void initMediaBrowser(Context context) {
        mBrowser = new MediaBrowserCompat(context, new ComponentName(context, MusicService.class), mediaBrowserCallback, null);
        //Browser发送连接请求
        Log.d(TAG, "connect()");
        mBrowser.connect();
    }

    private final MediaBrowserCompat.ConnectionCallback mediaBrowserCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
//            super.onConnected();
            /*作者：Anlia
            链接：https://juejin.cn/post/6844903575814930439*/
            Log.d(TAG, "onConnected");
            if (mBrowser.isConnected()) {
                //mediaId即为MediaBrowserService.onGetRoot的返回值
                //若Service允许客户端连接，则返回结果不为null，其值为数据内容层次结构的根ID
                //若拒绝连接，则返回null
                String mediaId = mBrowser.getRoot();

                //Browser通过订阅的方式向Service请求数据，发起订阅请求需要两个参数，其一为mediaId
                //而如果该mediaId已经被其他Browser实例订阅，则需要在订阅之前取消mediaId的订阅者
                //虽然订阅一个 已被订阅的mediaId 时会取代原Browser的订阅回调，但却无法触发onChildrenLoaded回调

                //ps：虽然基本的概念是这样的，但是Google在官方demo中有这么一段注释...
                // This is temporary: A bug is being fixed that will make subscribe
                // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
                // subscriber or not. Currently this only happens if the mediaID has no previous
                // subscriber or if the media content changes on the service side, so we need to
                // unsubscribe first.
                //大概的意思就是现在这里还有BUG，即只要发送订阅请求就会触发onChildrenLoaded回调
                //所以无论怎样我们发起订阅请求之前都需要先取消订阅
                mBrowser.unsubscribe(mediaId);
                //之前说到订阅的方法还需要一个参数，即设置订阅回调SubscriptionCallback
                //当Service获取数据后会将数据发送回来，此时会触发SubscriptionCallback.onChildrenLoaded回调
                mBrowser.subscribe(mediaId, browserSubscriptionCallback);

                try {
                    mController = new MediaControllerCompat(listener.getContext(), mBrowser.getSessionToken());
                    mController.registerCallback(controllerCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onConnectionFailed() {
//            super.onConnectionFailed();
            Log.d(TAG, "onConnectionFailed");
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback browserSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded(parentId, children);
            Log.d(TAG, "onChildrenLoaded------");
            List<MediaBrowserCompat.MediaItem> list = new ArrayList<>();
            //children 即为Service发送回来的媒体数据集合
            for (MediaBrowserCompat.MediaItem item : children) {
                item.getDescription().getTitle().toString();
                Log.v(TAG, item.getDescription().getTitle().toString());
                list.add(item);
            }
            //在onChildrenLoaded可以执行刷新列表UI的操作
//            listener.updateMediaItemData(list);
        }
    };

    //    TODO: 暂时无法修改状态，此位置无法接收到回调数据
    private final MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                /***
                 * 音乐播放状态改变的回调
                 * @param state 播放状态
                 */
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_NONE://无任何状态
                            name.set("");
                            playOrPauseFlag.set(false);
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                            playOrPauseFlag.set(false);
                            break;
                        case PlaybackStateCompat.STATE_PLAYING:
                            playOrPauseFlag.set(true);
                            break;
                    }
                }

                /**
                 * 播放音乐改变的回调
                 * @param metadata 音乐信息
                 */
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    name.set(metadata.getDescription().getTitle().toString());
                }
            };

    /**
     * 上一曲
     */
    public void playLast() {
        Log.d(TAG, "playLast");
        if (MusicManager.getInstance().currentPlayPosition == 0) {
            if (listener != null) {
                listener.makeMyToast("已经是第一首了，没有上一曲辣");
            } else {
                Log.e(TAG, "playBefore()-> listener is NULL!");
            }
            return;
        }
        MusicManager.getInstance().playLast();
    }

    /**
     * 下一曲
     */
    public void playNext() {
        Log.d(TAG, "playNext");
        if (MusicManager.getInstance().currentPlayPosition == MusicManager.getInstance().data.size() - 1) {
            if (listener != null) {
                listener.makeMyToast("已经是最后一首了，没有下一曲辣");
            } else {
                Log.e(TAG, "playBefore()-> listener is NULL!");
            }
            return;
        }
        MusicManager.getInstance().playNext();
    }

    /**
     * 播放/暂停
     */
    public void playOrPause() {
        Log.d(TAG, "playOrPause");
        if (MusicManager.getInstance().currentPlayPosition == -1) {
            if (listener != null) {
                listener.makeMyToast("请选择想要播放的音乐");
            } else {
                Log.e(TAG, "playOrPause()-> listener is NULL!");
            }
            return;
        }
        MusicManager.getInstance().playOrPause();
    }

    /**
     * 点击列表歌曲，准备播放
     */
    public void playNewSong(int position) {
        Log.d(TAG, "playNewSong");
        MusicManager.getInstance().playNewSong(position);
    }

    /**
     * 播放新音乐之前先停止音乐，View销毁时需要停止播放，此处改为public修饰
     */
    public void stopMusic() {
        Log.d(TAG, "stopMusic");
        MusicManager.getInstance().stopMusic();
    }

    public interface ViewModelListener {

        /**
         * /数据源变化，提示adapter更新，adapter在View层，通过接口通知View更新数据
         *
         * @param data 歌曲信息
         */
        void updateData(ArrayList<MusicItemBean> data);

        void updateMediaItemData(List<MediaBrowserCompat.MediaItem> data);

        void makeMyToast(String msg);

        default Context getContext() {
            return null;
        }
    }
}
