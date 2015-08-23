package org.chrisolsen.spotify;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class SongPlayerActivityFragment extends DialogFragment implements View.OnClickListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private TextView mSongNameText, mArtistNameText, mAlbumNameTest, mSongDuration, mSongPosition;
    private ImageView mAlbumImage;
    private ImageButton mPlayButton;

    private SeekBar mSeekBar;
    private ValueAnimator mProgressAnim;

    // allow service public methods to be called
    private PlayService mService;

    // allow for later disconnection
    private ServiceConnection mPlayServiceConnection;

    private String mNewFingerprint;
    private String mOldFingerprint;

    /**
     * Helper method allowing this fragment to be initialized with some required data
     *
     * @param songs List of songs
     * @param playIndex Index of the song to be played
     * @return Generate fragment
     */
    public static SongPlayerActivityFragment newInstance(Song[] songs, int playIndex) {
        SongPlayerActivityFragment f = new SongPlayerActivityFragment();
        Bundle b = new Bundle();
        b.putParcelableArray("songs", songs);
        b.putInt("playIndex", playIndex);
        f.setArguments(b);

        return f;
    }

    private static int convertToTimePlayed(int duration, int percent) {
        return (int) (duration * percent / 100f);
    }

    private static int convertToPercentPlayed(int duration, int time) {
        return (int) ((time * 1.0f) / duration * 100);
    }

    private static String toTime(long seconds) {
        String min = Long.toString(seconds / 60);
        String sec = Long.toString(seconds % 60);

        // ensure seconds is in two digits
        while (sec.length() < 2) {
            sec = "0" + sec;
        }

        return min + ":" + sec;
    }

    /**
     * Initialize all components
     * @return The create view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOG_TAG, "in the onCreateView");

        View view = inflater.inflate(R.layout.song_player_fragment, container, false);
        if (getArguments() == null) {
            return view;
        }

        mSongNameText = (TextView) view.findViewById(R.id.song_name);
        mArtistNameText = (TextView) view.findViewById(R.id.artist_name);
        mAlbumNameTest = (TextView) view.findViewById(R.id.album_name);
        mSongDuration = (TextView) view.findViewById(R.id.song_duration);
        mSongPosition = (TextView) view.findViewById(R.id.song_position);
        mAlbumImage = (ImageView) view.findViewById(R.id.album_image);
        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        mPlayButton = (ImageButton) view.findViewById(R.id.btn_play);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int time = convertToTimePlayed(mService.getPreviewDuration(), seekBar.getProgress());

                mService.seekTo(time);
                setProgressBarPosition(time);
            }
        });

        ImageButton prevButton = (ImageButton) view.findViewById(R.id.btn_previous_song);
        ImageButton nextButton = (ImageButton) view.findViewById(R.id.btn_next_song);

        prevButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);

        // list of songs to allow skipping between songs
        Parcelable[] plist = getArguments().getParcelableArray("songs");

        int playListLength = plist != null ? plist.length : 0;
        Song[] songs = new Song[playListLength];
        for (int i = 0; i < playListLength; i++) {
            songs[i] = (Song) plist[i];
        }

        int songIndex = getArguments().getInt("playIndex");

        if (savedInstanceState != null) {
            mOldFingerprint = savedInstanceState.getString("fingerprint");
        } else {
            mOldFingerprint = "";
        }

        mNewFingerprint = Integer.toString(songs.hashCode()) + "-" + Integer.toString(songIndex);

        initPlayer(songs, songIndex);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // on first run mService will be null
        if (mService != null) {
            mService.removeNotification();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("fingerprint", mNewFingerprint);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        Log.d(LOG_TAG, "onStop");
        super.onStop();

        // if no song was started, mService will be null
        if (mService != null) {
            mService.disconnect();
            mService.showNotification();
        }
    }

    /**
     * Called when the fragment is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();

        // prevent dangling service connections
        getActivity().unbindService(mPlayServiceConnection);
        mPlayServiceConnection = null;
    }

    /**
     * Sets up and binds the player and service connection
     *
     * @param songs
     * @param songIndex
     */
    private void initPlayer(final Song[] songs, final int songIndex) {
        Log.d(LOG_TAG, "initPlayer");

        Intent intent = new Intent(getActivity(), PlayService.class);

        mPlayServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(LOG_TAG, "onServiceConnected");
                mService = ((PlayService.SongBinder) binder).getService();

                // ensure no notifications exist while the activity is active
                mService.removeNotification();

                // bind for initial song
                resetProgressBar();
                bindSongDetails(songs[songIndex]);

                mService.connect(songs, songIndex,
                        new PlayService.TrackReceivedListener() {
                            @Override
                            public void onReceived() {
                                setProgressBarPosition(0);
                            }
                        },
                        new PlayService.SongChangedListener() {
                            @Override
                            public void onChanged(Song song) {
                                bindSongDetails(song);
                            }
                        }
                );
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(LOG_TAG, "onServiceDisconnected");
            }
        };

        getActivity().startService(intent);
        getActivity().bindService(intent, mPlayServiceConnection, 0);
    }

    /**
     * Removes the dialog title bar when displayed as a floating fragment
     *
     * @param savedInstanceState Any previously saved state
     * @return Reference to the dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /**
     * Handler for all the ui buttons
     *
     * @param v Reference to the button clicked
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        boolean isNewPlaylist = mNewFingerprint != mOldFingerprint;

        if (id == R.id.btn_play && mService.isStopped()) {
            // cancel it here, doing it inside `setPlayerPosition` results in the animation
            // event from continually making reference to the a no longer valid mMediaPlayer
            if (mProgressAnim != null) {
                mProgressAnim.cancel();
            }
            mService.play();
        } else if (id == R.id.btn_play && isNewPlaylist) {
            mService.play();
        } else if (id == R.id.btn_play && mService.isPlaying()) {
            mService.pause();
        } else if (id == R.id.btn_play && mService.isPaused()) {
            mService.resume();
        } else if (id == R.id.btn_previous_song) {
            playPrevious();
        } else if (id == R.id.btn_next_song) {
            playNext();
        }

        // update the state after the service calls
        if (id == R.id.btn_play) {
            updatePlayIcon();
            mOldFingerprint = mNewFingerprint; // reset the old fingerprint now that we are playing the list
        }
    }

    /**
     * Binds the current song's details with the image and titles
     */
    private void bindSongDetails(Song song) {
        mSongNameText.setText(song.name);

        // artist / album details
        mArtistNameText.setText(song.album.artist.name);
        mAlbumNameTest.setText(song.album.name);

        // album image || artist image
        String imageUrl = song.album.imageUrl == null ? song.album.artist.imageUrl : song.album.imageUrl;

        // song time details
        mSongDuration.setText(toTime(song.duration / 1000));
        mSongPosition.setText(toTime(0));

        Picasso.with(getActivity())
                .load(imageUrl)
                .into(mAlbumImage);
    }

    /**
     * Play the next track
     */
    public void playNext() {
        mProgressAnim.cancel();
        if (mService.playNext()) {
            updatePlayIcon();
        }
    }

    /**
     * Play the previous track
     */
    public void playPrevious() {
        mProgressAnim.cancel();
        if (mService.playPrevious()) {
            updatePlayIcon();
        }
    }

    private void resetProgressBar() {
        setProgressBarPosition(-1);
    }

    private void setProgressBarPosition(int time) {
        final int duration = mService.getPreviewDuration();

        mProgressAnim = ValueAnimator.ofInt(0, duration);
        mProgressAnim.setDuration(duration);
        mProgressAnim.setInterpolator(new DecelerateInterpolator());
        mProgressAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int time = mService.getCurrentPosition();
                int percent = convertToPercentPlayed(mService.getPreviewDuration(), time);

                mSongPosition.setText(toTime(time / 1000));
                mSeekBar.setProgress(percent);
            }
        });

        if (time >= 0) {
            mProgressAnim.start();
            mProgressAnim.setCurrentPlayTime(time); // must be set after start
        } else {
            mProgressAnim.cancel();
        }
    }

    /**
     * Updates the play icon based on the current state of the media player
     */
    private void updatePlayIcon() {
        int resId = 0;

        if (mService.isPlaying()) {
            resId = android.R.drawable.ic_media_pause;
        } else if (mService.isStopped() || mService.isPaused()) {
            resId = android.R.drawable.ic_media_play;
        }

        mPlayButton.setImageResource(resId);
    }
}
