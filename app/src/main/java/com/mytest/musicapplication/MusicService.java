package com.mytest.musicapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {
    private static final String TAG = MusicService.class.getSimpleName();
    private static final String MEDIA_ID_ROOT = "FANXIANGZI_MUSIC_PLAYER";

    private static final int NOTIFICATION_ID = 12138;

    private static final String CHANNEL_ID = "music_channel";
    private static final String PLAY_LAST = "LAST";
    private static final String PLAY_PAUSE = "PLAY_PAUSE";
    private static final String PLAY_NEXT = "NEXT";

    private List<MusicItemBean> musicList;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat mPlaybackState;

    private DeviceDisconnectReceiver deviceDisconnectReceiver;

    private String notificationSongName;

    private String notificationSinger;

    private boolean playBackState = false;

    /**
     * 线控耳机连点计数
     * 由于线控耳机不区分准确的功能，需要按照计数的方式执行对应任务
     */
    int count = 0;
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable doControlAction = () -> {
        //由于延迟0.5秒，用户连续点击数可能大于3,不做对应case，算所用户反悔，不想执行任何动作
        switch (count) {
            case 1:
                MusicManager.getInstance().playOrPause();
                break;
            case 2:
                MusicManager.getInstance().playNext();
                break;
            case 3:
                MusicManager.getInstance().playLast();
                break;
        }
        count = 0;
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        musicList = MusicManager.getInstance().data;
        notificationSongName = getResources().getString(R.string.notification_song_name);
        notificationSinger = getResources().getString(R.string.notification_singer);
        initMediaSession();
        initReceiver();
        // 创建通知渠道（仅适用于 Android 8.0 及更高版本）
        createNotificationChannel();
        // 启动通知
        startForeground(NOTIFICATION_ID, createNotification());
        MusicManager.getInstance().registerMusicDataListener(musicDataListener);
    }

    private void initMediaSession() {
        mPlaybackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build();

        //若需要处理媒体按钮事件，需要加入pendingIntent，但是有线耳机的指令也会在onMediaButtonEvent中收到，此位置不再使用
//        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                getBaseContext(),
//                0,
//                mediaButtonIntent,
//                PendingIntent.FLAG_IMMUTABLE
//        );
//        mediaSession = new MediaSessionCompat(this, "MusicService", null, pendingIntent);
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setCallback(sessionCallback);
        mediaSession.setPlaybackState(mPlaybackState);

        //设置token后会触发MediaBrowserCompat.ConnectionCallback的回调方法
        //表示MediaBrowser与MediaBrowserService连接成功
        setSessionToken(mediaSession.getSessionToken());
        mediaSession.setActive(true);
    }

    private void initReceiver() {
        deviceDisconnectReceiver = new DeviceDisconnectReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(deviceDisconnectReceiver, filter);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        // 创建一个PendingIntent，用于启动应用的主活动
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 创建一个PendingIntent，用于响应播放/暂停按钮的点击事件
        PendingIntent lastPendingIntent = PendingIntent.getService(
                this, 0, new Intent(this, MusicService.class).setAction(PLAY_LAST), PendingIntent.FLAG_IMMUTABLE);

        // 创建一个PendingIntent，用于响应播放/暂停按钮的点击事件
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 0, new Intent(this, MusicService.class).setAction(PLAY_PAUSE), PendingIntent.FLAG_IMMUTABLE);

        // 创建一个PendingIntent，用于响应下一曲按钮的点击事件
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this, 0, new Intent(this, MusicService.class).setAction(PLAY_NEXT), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(notificationSongName)
                .setContentText(notificationSinger)
                .setContentIntent(pendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2) // 设置紧凑视图中要显示的操作
                        .setMediaSession(mediaSession.getSessionToken())) // 关联MediaSession
                .addAction(R.drawable.notification_play_last, PLAY_LAST, lastPendingIntent) // 添加播放/暂停按钮
                .addAction(playBackState ? R.drawable.notification_pause : R.drawable.notification_play, PLAY_PAUSE, playPausePendingIntent) // 添加播放/暂停按钮
                .addAction(R.drawable.notification_play_next, PLAY_NEXT, nextPendingIntent) // 添加下一曲按钮
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (PLAY_PAUSE.equals(action)) {
                MusicManager.getInstance().playOrPause();
            } else if (PLAY_LAST.equals(action)) {
                MusicManager.getInstance().playLast();
            } else if (PLAY_NEXT.equals(action)) {
                MusicManager.getInstance().playNext();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        MusicManager.getInstance().unRegisterMusicDataListener(musicDataListener);
        unregisterReceiver(deviceDisconnectReceiver);
        mediaSession.setActive(false);
        mediaSession.release();
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(TAG, "onGetRoot-----------");
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren-----------");
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (MusicItemBean musicItem : musicList) {
            MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                    .setMediaId(musicItem.getPath()) // 设置媒体ID，可以是音频文件的路径等唯一标识
                    .setTitle(musicItem.getName()) // 设置音频文件的标题
                    .setSubtitle(musicItem.getSinger()) // 设置音频文件的艺术家
                    .setDescription(musicItem.getTime()) // 设置音频文件的时长
                    .setMediaUri(Uri.parse(musicItem.getPath())) // 设置音频文件的Uri
                    // 其他可选的设置，如设置音频文件的封面图片等
                    .build();
            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    mediaDescription,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE // 设置MediaItem为可播放的标志
            );
            mediaItems.add(mediaItem);
        }

        result.sendResult(mediaItems);
    }

    private final MusicManager.MusicDataListener musicDataListener = new MusicManager.MusicDataListener() {

        @Override
        public void onMusicDataChanged(String newName, String newSinger) {
            notificationSongName = newName;
            notificationSinger = newSinger;
            startForeground(NOTIFICATION_ID, createNotification());
        }

        @Override
        public void onPlayStatusChanged(Boolean isPlaying) {
            //使用的是同一个mPlaybackState对象，不需要重新setPlaybackState
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                    .build();
            playBackState = isPlaying;
            startForeground(NOTIFICATION_ID, createNotification());
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat status) {

        }
    };

    /**
     * 响应控制器指令的回调
     */
    private final MediaSessionCompat.Callback sessionCallback = new MediaSessionCompat.Callback() {
        /**
         * 响应MediaController.getTransportControls().play
         */
        @Override
        public void onPlay() {
            //在通过语音助手使用相应方法时会收到onPlay()、onPause()、onSkipToNext()、onSkipToPrevious()等回调
            Log.v(TAG, "onPlay() -> mPlaybackState = " + mPlaybackState.getState());
            if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PAUSED) {
                MusicManager.getInstance().playOrPause();
                mPlaybackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                        .build();
            }
        }

        /**
         * 响应MediaController.getTransportControls().onPause
         */
        @Override
        public void onPause() {
            Log.v(TAG, "onPause() -> mPlaybackState = " + mPlaybackState.getState());
            if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                MusicManager.getInstance().playOrPause();
                mPlaybackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                        .build();
            }
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.v(TAG, "onSkipToNext() -> mPlaybackState = " + mPlaybackState.getState());
            MusicManager.getInstance().playLast();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .build();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.v(TAG, "onSkipToNext() -> mPlaybackState = " + mPlaybackState.getState());
            MusicManager.getInstance().playNext();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .build();
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            //在通过蓝牙耳机点击下一曲时，会触发此方法
            //一次下一曲功能的使用会触发两次，KeyEvent中的action分别为ACTION_DOWN / ACTION_UP
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            Log.v(TAG, "onMediaButtonEvent() -> mPlaybackState -> keyEvent = " + keyEvent);
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        MusicManager.getInstance().playOrPause();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        MusicManager.getInstance().playNext();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        MusicManager.getInstance().playLast();
                        break;
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        //线控耳机只会发出KEYCODE_HEADSETHOOK，通过计数方式控制
                        handler.removeCallbacks(doControlAction);
                        count++;
                        handler.postDelayed(doControlAction, 500);

                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        /**
         * 响应MediaController.getTransportControls().playFromUri
         * @param uri 信息
         * @param extras 额外信息
         */
        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            Log.e(TAG, "onPlayFromUri");
//            try {
//                switch (mPlaybackState.getState()){
//                    case PlaybackStateCompat.STATE_PLAYING:
//                    case PlaybackStateCompat.STATE_PAUSED:
//                    case PlaybackStateCompat.STATE_NONE:
//                        mMediaPlayer.reset();
//                        mMediaPlayer.setDataSource(MusicService.this,uri);
//                        mMediaPlayer.prepare();//准备同步
//                        mPlaybackState = new PlaybackStateCompat.Builder()
//                                .setState(PlaybackStateCompat.STATE_CONNECTING,0,1.0f)
//                                .build();
//                        mSession.setPlaybackState(mPlaybackState);
//                        //我们可以保存当前播放音乐的信息，以便客户端刷新UI
//                        mSession.setMetadata(new MediaMetadataCompat.Builder()
//                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,extras.getString("title"))
//                                .build()
//                        );
//                        break;
//                }
//            }catch (IOException e){
//                e.printStackTrace();
//            }
        }
    };
}

class DeviceDisconnectReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.e("HeadsetEventReceiver", "action is NULL!");
            return;
        }
        Log.d("HeadsetEventReceiver", "action = " + action);
        if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
//            来源：https://blog.csdn.net/crazestone0614/article/details/135412491
            if (MusicManager.getInstance().mediaPlayer.isPlaying()) {
                MusicManager.getInstance().playOrPause();
            }
        }

        //以下仅用作学习蓝牙协议的代码，无实际作用
        //因为有线耳机拔出时，蓝牙协议的判断没有作用。所以只需要接收ACTION_AUDIO_BECOMING_NOISY状态即可
//        if (action != null && action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
//            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//            if (audioManager != null && (audioManager.isBluetoothScoOn() || audioManager.isBluetoothA2dpOn())) {
//                if (MusicManager.getInstance().mediaPlayer.isPlaying()) {
//                    MusicManager.getInstance().playOrPause();
//                }
//            }
//        }
    }
}
