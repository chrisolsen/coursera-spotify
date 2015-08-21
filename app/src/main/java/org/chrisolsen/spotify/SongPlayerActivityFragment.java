package org.chrisolsen.spotify;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
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
    private PlayService mPlayService;

    // allow for later disconnection
    private ServiceConnection mPlayServiceConnection;

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
                int time = mPlayService.convertToTimePlayed(seekBar.getProgress());

                mPlayService.seekTo(time);
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

        // show selected song's details
        int songIndex = getArguments().getInt("playIndex");

        initPlayer(songs, songIndex);

        return view;
    }

    /**
     * Remove dialog.
     */
    @Override
    public void onDestroyView() {
        Log.d(LOG_TAG, "onDestroyView");

        mPlayService.showNotification();
        getActivity().unbindService(mPlayServiceConnection);

        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
    }

    private void initPlayer(final Song[] songs, final int songIndex) {
        Log.d(LOG_TAG, "initPlayer");

        Intent intent = new Intent(getActivity(), PlayService.class);

        mPlayServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                PlayService.SongBinder b = (PlayService.SongBinder) binder;
                mPlayService = b.getService();

                setProgressBarPosition(0);

                mPlayService.init(songs, songIndex,
                        new PlayService.TrackReceivedListener() {
                            @Override
                            public void onReceived() {
                                resetProgressBar();
                            }
                        },
                        new PlayService.SongChangedListener() {
                            @Override
                            public void onChanged(Song song) {
                                bindSongDetails(song);
                            }
                        }
                );
                bindSongDetails(songs[songIndex]);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mPlayService = null;
            }
        };

        getActivity().startService(intent);
        getActivity().bindService(intent, mPlayServiceConnection, Context.BIND_AUTO_CREATE);
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

        if (id == R.id.btn_play) {
            if (mPlayService.isStopped()) {
                play();
            } else if (mPlayService.isPlaying()) {
                mPlayService.pause();
            } else if (mPlayService.isPaused()) {
                mPlayService.resume();
            }
            updatePlayIcon();

        } else if (id == R.id.btn_previous_song) {
            playPrevious();
        } else if (id == R.id.btn_next_song) {
            playNext();
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
     * Plays the song for the current song index
     */
    public void play() {
        // cancel it here, doing it inside `setPlayerPosition` results in the animation
        // event from continually making reference to the a no longer valid mMediaPlayer
        if (mProgressAnim != null) {
            mProgressAnim.cancel();
        }

        mPlayService.play(false);
    }

    /**
     * Play the next track
     */
    public void playNext() {
        mProgressAnim.cancel();
        if (mPlayService.playNext(false)) {
            updatePlayIcon();
        }
    }

    /**
     * Play the previous track
     */
    public void playPrevious() {
        mProgressAnim.cancel();
        if (mPlayService.playPrevious(false)) {
            updatePlayIcon();
        }
    }

    private void resetProgressBar() {
        setProgressBarPosition(0);
    }

    private void setProgressBarPosition(int time) {
        final int duration = mPlayService.getPreviewDuration();

        mProgressAnim = ValueAnimator.ofInt(0, duration);
        mProgressAnim.setDuration(duration);
        mProgressAnim.setInterpolator(new DecelerateInterpolator());
        mProgressAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int time = mPlayService.getCurrentPosition();
                int percent = mPlayService.convertToPercentPlayed(time);

                mSongPosition.setText(toTime(time / 1000));
                mSeekBar.setProgress(percent);
            }
        });

        if (time > 0) {
            mProgressAnim.start();
            mProgressAnim.setCurrentPlayTime(time); // must be set after start
        } else {
            mProgressAnim.cancel();
        }
    }

    private String toTime(long seconds) {
        String min = Long.toString(seconds / 60);
        String sec = Long.toString(seconds % 60);

        return min + ":" + sec;
    }

    /**
     * Updates the play icon based on the current state of the media player
     */
    private void updatePlayIcon() {
        int resId = 0;

        if (mPlayService.isPlaying()) {
            resId = android.R.drawable.ic_media_pause;
        } else if (mPlayService.isStopped() || mPlayService.isPaused()) {
            resId = android.R.drawable.ic_media_play;
        }

        mPlayButton.setImageResource(resId);
    }
}
