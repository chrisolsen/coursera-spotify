package org.chrisolsen.spotify;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;

public class SongPlayerActivityFragment extends DialogFragment implements View.OnClickListener {

    private static final int PLAYER_STATE_STOPPED = 0;
    private static final int PLAYER_STATE_PLAYING = 1;
    private static final int PLAYER_STATE_PAUSED = 2;

    private final String LOG_TAG = this.getClass().getSimpleName();

    private TextView mSongNameText, mArtistNameText, mAlbumNameTest, mSongDuration, mSongPosition;
    private ImageView mAlbumImage;
    private ImageButton mPlayButton;
    private Song[] mPlaylist;
    private MediaPlayer mMediaPlayer;
    private int mPlaylistIndex;
    private int mPlayerState;
    private SeekBar mSeekBar;
    private ValueAnimator mProgressAnim;

    /**
     * Helper method allowing this fragment to be initialized with some required data
     *
     * @param songs List of songs
     * @param playIndex Index of the song to be played
     * @return Generarte fragment
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

        View view = inflater.inflate(R.layout.song_player_fragment, container, false);

        if (getArguments() == null) {
            return view;
        }

        mMediaPlayer = new MediaPlayer();

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
                setPlayerPosition(seekBar.getProgress());
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
        mPlaylist = new Song[playListLength];
        for (int i = 0; i < playListLength; i++) {
            mPlaylist[i] = (Song) plist[i];
        }

        // show selected song's details
        mPlaylistIndex = getArguments().getInt("playIndex");

        bindSongDetails();

        return view;
    }

    /**
     * Clean up
     * Unreleased MediaPlayer results in a lot of lost memory
     */
    @Override
    public void onStop() {
        super.onStop();

        if (mProgressAnim != null) {
            mProgressAnim.cancel();
        }
        mMediaPlayer.release();
        mMediaPlayer = null;
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
            if (mPlayerState == PLAYER_STATE_STOPPED) {
                play();
            } else if (mPlayerState == PLAYER_STATE_PLAYING) {
                pause();
            } else if (mPlayerState == PLAYER_STATE_PAUSED) {
                resume();
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
    private void bindSongDetails() {
        Song song = mPlaylist[mPlaylistIndex];
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
     * Stops the current track at the current location
     */
    public void pause() {
        mPlayerState = PLAYER_STATE_PAUSED;
        mMediaPlayer.pause();
    }

    /**
     * Restarts a previously paused track from the stopped location
     */
    public void resume() {
        mPlayerState = PLAYER_STATE_PLAYING;
        mMediaPlayer.start();
    }

    /**
     * Plays the song for the current song index
     */
    public void play() {
        Song s = mPlaylist[mPlaylistIndex];

        // cancel it here, doing it inside `setPlayerPosition` results in the animation
        // event from continually making reference to the a no longer valid mMediaPlayer
        if (mProgressAnim != null) {
            mProgressAnim.cancel();
        }

        // play song
        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(s.previewUrl);
            mMediaPlayer.prepareAsync();


            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mPlayerState = PLAYER_STATE_PLAYING;
                    mMediaPlayer.start();
                    setPlayerPosition(0);
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
            Context c = this.getActivity();
            Toast.makeText(c, c.getString(R.string.error_fetching_audio), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Play the next track
     */
    public void playNext() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (mPlaylistIndex < mPlaylist.length - 1) {
            mPlaylistIndex++;
            bindSongDetails();
            updatePlayIcon();
            play();
        }
    }

    /**
     * Play the previous track
     */
    public void playPrevious() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (mPlaylistIndex > 0) {
            mPlaylistIndex--;
            bindSongDetails();
            updatePlayIcon();
            play();
        }
    }

    /**
     * Sets the seekbar and media player's position
     *
     * @param percent Percent of the current song's position
     */
    private void setPlayerPosition(int percent) {
        final int duration = mMediaPlayer.getDuration();
        final int currentTime = (int) (duration * percent / 100f);

        mMediaPlayer.seekTo(currentTime);

        mProgressAnim = ValueAnimator.ofInt(0, duration);
        mProgressAnim.setDuration(duration);
        mProgressAnim.setInterpolator(new DecelerateInterpolator());
        mProgressAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int time = mMediaPlayer.getCurrentPosition();
                float percent = (time * 1.0f) / duration * 100;

                mSongPosition.setText(toTime(time / 1000));
                mSeekBar.setProgress((int) percent);
            }
        });

        mProgressAnim.start();
        mProgressAnim.setCurrentPlayTime(currentTime); // must be set after start
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

        switch (mPlayerState) {
            case PLAYER_STATE_STOPPED:
            case PLAYER_STATE_PAUSED:
                resId = android.R.drawable.ic_media_play;
                break;

            case PLAYER_STATE_PLAYING:
                resId = android.R.drawable.ic_media_pause;
                break;
        }

        mPlayButton.setImageResource(resId);
    }
}
