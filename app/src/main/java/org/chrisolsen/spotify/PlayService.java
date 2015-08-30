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
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;

public class PlayService extends Service {

    // Notification actions
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_RESUME = "resume";

    // Player States
    private static final int PLAYER_STATE_STOPPED = 0;
    private static final int PLAYER_STATE_PLAYING = 1;
    private static final int PLAYER_STATE_PAUSED = 2;

    private final String LOG_TAG = PlayService.class.getSimpleName();
    private TrackReceivedListener mOnReceiveListener;
    private SongChangedListener mSongChangeListener;
    private SongBinder mBinder = new SongBinder();
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
        Log.d(LOG_TAG, "onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_NEXT:
                        playNext();
                        break;
                    case ACTION_PAUSE:
                        pause();
                        break;
                    case ACTION_RESUME:
                        resume();
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
                }
            }
        }

        return START_STICKY;
    }

    // Exposed service methods

    public void load(Song[] playList, int playListIndex, TrackReceivedListener trackReceivedListener, SongChangedListener songChangedListener) {
        Log.d(LOG_TAG, "load()");
        mPlaylist = playList;
        mPlaylistIndex = playListIndex;
        mOnReceiveListener = trackReceivedListener;
        mSongChangeListener = songChangedListener;
    }

    public void disconnect() {
        Log.d(LOG_TAG, "disconnect()");
        mSongChangeListener = null;
        mOnReceiveListener = null;
    }

    public Song getCurrentSong() {
        if (mPlaylist == null) {
            return null;
        }
        return mPlaylist[mPlaylistIndex];
    }

    public int getCurrentPosition() {
        if (mPlayerState == PLAYER_STATE_STOPPED) {
            return -1;
        }
        return mMediaPlayer.getCurrentPosition();
    }

    public int getPreviewDuration() {
        return 30000; //mMediaPlayer.getDuration();
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

    public void seekTo(int time) {
        mMediaPlayer.seekTo(time);
    }

    public void play() {
        Log.d(LOG_TAG, "play");

        try {
            // current song
            final Song song = mPlaylist[mPlaylistIndex];

            Log.d(LOG_TAG, "song to play: " + song.name);

            // in the case that a song from a different artist is being played
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(song.previewUrl);
            mMediaPlayer.prepareAsync();

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // FIXME: handle this error...properly
                    if (what == -38) {
                        return true;
                    }

                    return true;
                }
            });

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d(LOG_TAG, "about to start the song");
                    mp.start();
                    mp.seekTo(0);

                    showNotification();

                    if (mOnReceiveListener != null) mOnReceiveListener.onReceived();
                    if (mSongChangeListener != null) mSongChangeListener.onChanged(song);
                }
            });

            mPlayerState = PLAYER_STATE_PLAYING;
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
        if (skipToNext()) {
            play();
            return true;
        }
        return false;
    }

    public boolean skipToNext() {
        Log.d(LOG_TAG, "skipToNext");
        stop();
        if (mPlaylistIndex < mPlaylist.length - 1) {
            mPlaylistIndex++;
            return true;
        }
        return false;
    }

    public boolean skipToPrevious() {
        Log.d(LOG_TAG, "skipToPrevious");
        stop();
        if (mPlaylistIndex > 0) {
            mPlaylistIndex--;
            return true;
        }
        return false;
    }

    public boolean playPrevious() {
        if (skipToPrevious()) {
            play();
            return true;
        }
        return false;
    }

    public void pause() {
        Log.d(LOG_TAG, "pause");
        mPlayerState = PLAYER_STATE_PAUSED;
        mMediaPlayer.pause();

        // to allow the pause button to be converted to a play and handled appropriately
        showNotification();
    }

    public void removeNotification() {
        stopForeground(true);
    }

    public void resume() {
        Log.d(LOG_TAG, "resume");
        mPlayerState = PLAYER_STATE_PLAYING;
        mMediaPlayer.start();
        showNotification();
    }

    public void stop() {
        Log.d(LOG_TAG, "stop");
        mPlayerState = PLAYER_STATE_STOPPED;
        mMediaPlayer.stop();
        mMediaPlayer.reset();
    }

    public void showNotification() {
        // if the listeners are not null then the activity is active, so no notification is require
        if (mOnReceiveListener != null && mSongChangeListener != null) {
            Log.d(LOG_TAG, "showNotification abort");
            return;
        }

        Log.d(LOG_TAG, "showNotification");

        Intent intent = new Intent(getApplicationContext(), SongPlayerActivity.class);
        intent.putExtra("songs", mPlaylist);
        intent.putExtra("playIndex", mPlaylistIndex);

        // need to create a task stack to allow the user to navigate up after opening app
        // through a notification
        final TaskStackBuilder taskBuilder = TaskStackBuilder.create(getApplicationContext());
        taskBuilder.addParentStack(SongPlayerActivity.class);
        taskBuilder.addNextIntent(intent);

        final Song song = getCurrentSong();

        // FIXME: not sure if this is the best way to pre-load the image for the notification or not
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

                // Notification actions
                Intent nextIntent = new Intent(getApplicationContext(), PlayService.class);
                nextIntent.setAction(ACTION_NEXT);

                Intent prevIntent = new Intent(getApplicationContext(), PlayService.class);
                prevIntent.setAction(ACTION_PREV);

                Intent pauseIntent = new Intent(getApplicationContext(), PlayService.class);
                pauseIntent.setAction(ACTION_PAUSE);

                Intent resumeEvent = new Intent(getApplicationContext(), PlayService.class);
                resumeEvent.setAction(ACTION_RESUME);

                PendingIntent nextPIntent = PendingIntent.getService(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent prevPIntent = PendingIntent.getService(getApplicationContext(), 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent pausePIntent = PendingIntent.getService(getApplicationContext(), 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent resumePIntent = PendingIntent.getService(getApplicationContext(), 0, resumeEvent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                builder.setContentTitle(song.name)
                        .setShowWhen(false)
                        .setContentIntent(taskBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
                        .setSmallIcon(R.drawable.ic_spotify_notificatiopn)
                        .setLargeIcon(img)
                        .setContentText(song.album.artist.name)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setStyle(new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0, 1, 2));

                builder.addAction(android.R.drawable.ic_media_previous, null, prevPIntent);
                if (mPlayerState == PLAYER_STATE_PLAYING)
                    builder.addAction(android.R.drawable.ic_media_pause, null, pausePIntent);
                else
                    builder.addAction(android.R.drawable.ic_media_play, null, resumePIntent);
                builder.addAction(android.R.drawable.ic_media_next, null, nextPIntent);

                startForeground(1, builder.build());
            }
        }.execute();
    }

    public int getPlayListIndex() {
        return mPlaylistIndex;
    }

    /**
     * Interface to allow binding to when the track has been fully received from the api
     */
    public interface TrackReceivedListener {
        void onReceived();
    }

    // allow an activity/fragment to take action when the song changes
    public interface SongChangedListener {
        void onChanged(Song song);
    }

    /**
     * Provides a reference to the service object
     */
    public class SongBinder extends Binder {

        public PlayService getService() {
            return PlayService.this;
        }
    }
}
