package com.mytest.musicapplication;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.util.Log;

public class AudioFocusManager {
    private static final String TAG = AudioFocusManager.class.getSimpleName();
    private static volatile AudioFocusManager audioFocusManager;

    private AudioManager audioManager;

    private AudioFocusRequest focusRequest;

    private AudioFocusManager() {
    }

    public static AudioFocusManager getInstance() {
        if (audioFocusManager == null) {
            synchronized (AudioFocusManager.class) {
                if (audioFocusManager == null) {
                    audioFocusManager = new AudioFocusManager();
                }
                return audioFocusManager;
            }
        }
        return audioFocusManager;
    }

    public void initAudioManager(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private AudioFocusRequest makeAudioFocusRequest(AudioManager.OnAudioFocusChangeListener listener) {
        return new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build())
                .setOnAudioFocusChangeListener(listener)
                .setAcceptsDelayedFocusGain(true)
                .build();
    }

    public boolean requestAudioFocus(AudioManager.OnAudioFocusChangeListener listener) {
        Log.d(TAG, "requestAudioFocus");
        focusRequest = makeAudioFocusRequest(listener);
        int result = audioManager.requestAudioFocus(focusRequest);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(TAG, "request was aborted");
            return false;
        }
        return true;
    }

    public void releaseAudioFocus() {
        Log.d(TAG, "releaseAudioFocus");
        int result = audioManager.abandonAudioFocusRequest(focusRequest);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(TAG, "request was aborted");
        }
    }

}
