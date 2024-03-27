package com.mytest.musicapplication;

import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {
    private static final String TAG = MusicService.class.getSimpleName();
    private static final String MEDIA_ID_ROOT = "FANXIANGZI_MUSIC_PLAYER";

    private List<MusicItemBean> musicList;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat mPlaybackState;

    @Override
    public void onCreate() {
        super.onCreate();
        musicList = MusicManager.getInstance().data;
        mPlaybackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE,0,1.0f)
                .build();

        mediaSession = new MediaSessionCompat(this,"MusicService");
        mediaSession.setCallback(sessionCallback);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | FLAG_HANDLES_MEDIA_BUTTONS );
        mediaSession.setPlaybackState(mPlaybackState);

        //设置token后会触发MediaBrowserCompat.ConnectionCallback的回调方法
        //表示MediaBrowser与MediaBrowserService连接成功
        setSessionToken(mediaSession.getSessionToken());
        mediaSession.setActive(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.setActive(false);
        mediaSession.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(TAG,"onGetRoot-----------");
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG,"onLoadChildren-----------");
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (MusicItemBean musicItem:musicList) {
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

    /**
     * 响应控制器指令的回调
     */
    private final MediaSessionCompat.Callback sessionCallback = new MediaSessionCompat.Callback(){
        /**
         * 响应MediaController.getTransportControls().play
         */
        @Override
        public void onPlay() {
            Log.e(TAG,"onPlay() -> mPlaybackState = " + mPlaybackState.getState());
            if(mPlaybackState.getState() == PlaybackStateCompat.STATE_PAUSED){
                MusicManager.getInstance().playOrPause();
                mPlaybackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING,0,1.0f)
                        .build();
                mediaSession.setPlaybackState(mPlaybackState);
            }
        }

        /**
         * 响应MediaController.getTransportControls().onPause
         */
        @Override
        public void onPause() {
            Log.e(TAG,"onPause() -> mPlaybackState = " + mPlaybackState.getState());
            if(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING){
                MusicManager.getInstance().playOrPause();
                mPlaybackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED,0,1.0f)
                        .build();
                mediaSession.setPlaybackState(mPlaybackState);
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.e(TAG,"onSkipToNext() -> mPlaybackState = " + mPlaybackState.getState());
            if(mPlaybackState.getState() == PlaybackStateCompat.STATE_PAUSED){
                MusicManager.getInstance().playNext();
                mPlaybackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING,0,1.0f)
                        .build();
                mediaSession.setPlaybackState(mPlaybackState);
            }
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.e(TAG,"onMediaButtonEvent() -> mPlaybackState = " + mPlaybackState.getState());
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        /**
         * 响应MediaController.getTransportControls().playFromUri
         * @param uri
         * @param extras
         */
        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            Log.e(TAG,"onPlayFromUri");
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

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
        }
    };
}
