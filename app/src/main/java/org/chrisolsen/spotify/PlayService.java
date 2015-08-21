package org.chrisolsen.spotify;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;

public class PlayService extends Service {

    private final String LOG_TAG = PlayService.class.getSimpleName();
    private TrackReceivedListener mOnReceiveListener;
    private SongChangedListener mSongChangeListener;

    // Interface to allow binding to when the track has been fully received from the api
    public interface TrackReceivedListener {
        void onReceived();
    }

    public interface SongChangedListener {
        void onChanged(Song song);
    }

    private SongBinder mBinder = new SongBinder();

    public class SongBinder extends Binder {

        private

        public boolean play() {

        }

        public boolean playNext() {

        }

        public boolean playPrevious() {

        }

        public void seekTo(int time) {

        }

//        public PlayService getService() {
//            return PlayService.this;
//        }
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

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Clean up
     */
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_NEXT:
                        playNext(true);
                        break;
                    case ACTION_PAUSE:
                        pause();
                        break;
                    case ACTION_PLAY:
                        play(true);
                        break;
                    case ACTION_PREV:
                        playPrevious(true);
                        break;
                    case ACTION_STOP:
                        stop();
                        break;
                }
            }
        }

        return START_STICKY;
    }

    public void init(Song[] playList, int playListIndex, TrackReceivedListener trackReceivedListener, SongChangedListener songChangedListener) {
        mPlaylist = playList;
        mPlaylistIndex = playListIndex;
        mOnReceiveListener = trackReceivedListener;
        mSongChangeListener = songChangedListener;
    }

    public Song getCurrentSong() {
        return mPlaylist[mPlaylistIndex];
    }

    public int getPreviewDuration() {
        return mMediaPlayer.getDuration();
    }

    public int convertToTimePlayed(int percent) {
        int duration = getPreviewDuration();
        return (int) (duration * percent / 100f);
    }

    public int convertToPercentPlayed(int time) {
        return (int) ((time * 1.0f) / getPreviewDuration() * 100);
    }

    public void seekTo(int time) {
        mMediaPlayer.seekTo(time);
    }

    public void play(final boolean showNotification) {
        final Song song;

        try {
            // current song
            song = mPlaylist[mPlaylistIndex];

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(song.previewUrl);
            mMediaPlayer.prepareAsync();

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mPlayerState = PLAYER_STATE_PLAYING;
                    mMediaPlayer.start();
                    mMediaPlayer.seekTo(0);
                    mOnReceiveListener.onReceived();
                    mSongChangeListener.onChanged(song);
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNext(showNotification);
                }
            });

            if (showNotification) {
                showNotification();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), R.string.error_fetching_audio, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean playNext(boolean showNotification) {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (mPlaylistIndex < mPlaylist.length - 1) {
            mPlaylistIndex++;
            play(showNotification);
            return true;
        }
        return false;
    }

    public boolean playPrevious(boolean showNotification) {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (mPlaylistIndex > 0) {
            mPlaylistIndex--;
            play(showNotification);
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

    public void showNotification() {
        Log.d(LOG_TAG, "showNotification");
        Intent intent = new Intent(getApplicationContext(), SongPlayerActivity.class);
        intent.putExtra("songs", mPlaylist);
        intent.putExtra("playIndex", mPlaylistIndex);

        final PendingIntent pi = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final Song song = getCurrentSong();

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap img = null;
                try {
                    img = Picasso.with(getApplicationContext())
                            .load(song.album.imageUrl)
                            .get();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return img;
            }

            @Override
            protected void onPostExecute(Bitmap img) {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(getApplicationContext())
                                .setContentTitle(song.name)
                                .setContentIntent(pi)
                                .setSmallIcon(R.drawable.ic_spotify_notificatiopn)
                                .setLargeIcon(img)
                                .setContentText(song.album.artist.name);

                startForeground(1, builder.build());
            }
        }.execute();
    }

    public int getCurrentPosition() {
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
