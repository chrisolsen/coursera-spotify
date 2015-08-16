package org.chrisolsen.spotify;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class PlayService extends Service {

    private final String LOG_TAG = PlayService.class.getSimpleName();
    private TrackReceivedListener mOnReceiveListener;

    public interface TrackReceivedListener {
        void onReceived();
    }

    private SongBinder mBinder = new SongBinder();
    public class SongBinder extends Binder {
        public PlayService getService() {
            return PlayService.this;
        }
    }

    // Player States
    private static final int PLAYER_STATE_STOPPED = 0;
    private static final int PLAYER_STATE_PLAYING = 1;
    private static final int PLAYER_STATE_PAUSED = 2;

    // Notification actions
    public static final String ACTION_PLAY = "org.chrisolsen.spotify.actions.play";
    public static final String ACTION_STOP = "org.chrisolsen.spotify.actions.stop";
    public static final String ACTION_PAUSE = "org.chrisolsen.spotify.actions.pause";
    public static final String ACTION_NEXT = "org.chrisolsen.spotify.actions.next";
    public static final String ACTION_PREV = "org.chrisolsen.spotify.actions.prev";

    // vars
    private Song[] mPlaylist;
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private int mPlaylistIndex;
    private int mPlayerState;
    private boolean mIsSetup = false;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Clean up
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        switch (intent.getAction()) {
            case ACTION_NEXT:
                playNext();
                break;
            case ACTION_PAUSE:
                pause();
                break;
            case ACTION_PLAY:
                play();
                break;
            case ACTION_PREV:
                playPrevious();
                break;
            case ACTION_STOP:
                stop();
                break;
            default:
                setup();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void init(Song[] playList, int playListIndex, TrackReceivedListener listener) {
        mPlaylist = playList;
        mPlaylistIndex = playListIndex;
        mOnReceiveListener = listener;
    }

    public Song getCurrentSong() {
        return mPlaylist[mPlaylistIndex];
    }

    public int getPreviewDuration() {
        return mMediaPlayer.getDuration();
    }

    public void seekTo(int time) {
        mMediaPlayer.seekTo(time);
    }

    public void play() {
        Song s;

        if (!mIsSetup) {
            setup();
            mIsSetup = true;
        }

        try {
            // current song
            s = mPlaylist[mPlaylistIndex];

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(s.previewUrl);
            mMediaPlayer.prepareAsync();


            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mPlayerState = PLAYER_STATE_PLAYING;
                    mMediaPlayer.start();
                    mMediaPlayer.seekTo(0);
                    mOnReceiveListener.onReceived();
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNext();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), R.string.error_fetching_audio, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean playNext() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (mPlaylistIndex < mPlaylist.length - 1) {
            mPlaylistIndex++;
            play();
            return true;
        }
        return false;
    }

    public boolean playPrevious() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (mPlaylistIndex > 0) {
            mPlaylistIndex--;
            play();
            return true;
        }
        return false;
    }

    public void pause() {
        mPlayerState = PLAYER_STATE_PAUSED;
        mMediaPlayer.pause();
    }

    public void resume() {
        mPlayerState = PLAYER_STATE_PLAYING;
        mMediaPlayer.start();
    }

    public void stop() {
        mPlayerState = PLAYER_STATE_STOPPED;
        mMediaPlayer.stop();
    }


    private void setup() {
        Log.d(LOG_TAG, "onStartCommand");
        PendingIntent pi = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                new Intent(getApplicationContext(), SongPlayerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("The notification")
                        .setContentIntent(pi)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.notification_template_icon_bg)
                        .setContentText("The notification text");

        Log.d(LOG_TAG, "about to startForeground");
        startForeground(1, builder.build());
    }

    public int getCurrentPosition() {
        Log.d("Current Position", Integer.toString(mMediaPlayer.getCurrentPosition()));
        return mMediaPlayer.getCurrentPosition();
    }

    public boolean isPlaying() {
        return mPlayerState == PLAYER_STATE_PLAYING;
    }

    public boolean isStopped() {
        return mPlayerState == PLAYER_STATE_STOPPED;
    }

    public boolean isPaused() {
        return mPlayerState == PLAYER_STATE_PAUSED;
    }

}
