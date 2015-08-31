package org.chrisolsen.spotify;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

public class SongPlayerActivityFragment extends DialogFragment implements View.OnClickListener {

    private final String TAG = this.getClass().getSimpleName();

    private TextView mSongNameText, mArtistNameText, mAlbumNameTest, mSongDuration, mSongPosition;
    private ImageView mAlbumImage;
    private ImageButton mPlayButton;

    private SeekBar mSeekBar;
    private ValueAnimator mProgressAnim;

    // allow service public methods to be called
    private PlayService mService;

    // allow for later disconnection
    private ServiceConnection mPlayServiceConnection;
    private PlayerState mPlayerState;
    private ShareActionProvider mShareActionProvider;

    /**
     * Helper method allowing this fragment to be initialized with some required data
     *
     * @return Generate fragment
     */
    public static SongPlayerActivityFragment newInstance() {
        SongPlayerActivityFragment f = new SongPlayerActivityFragment();
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
        Log.d(TAG, "in the onCreateView");

        View view = inflater.inflate(R.layout.song_player_fragment, container, false);

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

        this.setHasOptionsMenu(true);

        return view;
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link #setHasOptionsMenu}.  See
     * {@link Activity#onCreateOptionsMenu(Menu) Activity.onCreateOptionsMenu}
     * for more information.
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater
     * @see #setHasOptionsMenu
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_song_player, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
    }

    /**
     * Allow for sharing of the song
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Song song = mService.getCurrentSong();

        switch (item.getItemId()) {
            case R.id.menu_item_share:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, song.href);
                intent.putExtra(Intent.EXTRA_SUBJECT, song.name);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_title)));

                return true;
        }

        return false;
    }


    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        mPlayerState = new PlayerState(getActivity());
        bindToService();
    }

    private void bindToService() {
        Intent intent = new Intent(getActivity(), PlayService.class);

        // prevent user from clicking play button, while app is waiting for the track
        // mPlayButton.setEnabled(false);

        mPlayServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(TAG, "onServiceConnected");

                if (mProgressAnim != null) {
                    mProgressAnim.cancel();
                }

                mService = ((PlayService.SongBinder) binder).getService();
                mService.removeNotification();

                Song playerSong = mService.getCurrentSong();
                Song stateSong = mPlayerState.getPlayList()[mPlayerState.getPlayListIndex()];

                boolean newSong = !stateSong.equals(playerSong);

                mService.load(mPlayerState.getPlayList(), mPlayerState.getPlayListIndex(),
                        new PlayService.TrackReceivedListener() {
                            @Override
                            public void onReceived() {
                                Log.d(TAG, "TrackReceivedListener.onReceived");
                                mPlayButton.setEnabled(true);
                            }
                        },
                        new PlayService.SongChangedListener() {
                            @Override
                            public void onChanged(Song song) {
                                Log.d(TAG, "SongChangedListener.onChanged");
                                bindSongDetails(song); // needed for songs other than the first
                            }
                        }
                );

                if (newSong) {
                    Log.d(TAG, "onServiceConnected: mService.play()");
                    mService.play();
                }

                setProgressBarPosition(mService.getCurrentPosition());
                updatePlayerControls();
                bindSongDetails(mService.getCurrentSong()); // needed for songs other than the first
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
            }
        };

        getActivity().startService(intent);
        getActivity().bindService(intent, mPlayServiceConnection, 0);
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
    public void onStop() {
        Log.d(TAG, "onStop");
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
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        // prevent dangling service connections
        getActivity().unbindService(mPlayServiceConnection);
        mPlayServiceConnection = null;
    }

    /**
     * Sets up and binds the player and service connection
     */
    private void updatePlayerControls() {
        Log.d(TAG, "updatePlayerControls - playing: " + Boolean.toString(mService.isPlaying()));

        setProgressBarPosition(mService.getCurrentPosition());

        // button icon
        if (mService.isPlaying()) {
            mPlayButton.setImageResource(R.drawable.pause_button_selector);
        } else {
            mPlayButton.setImageResource(R.drawable.play_button_selector);
        }
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
        Log.d(TAG, "onClick");

        if (id == R.id.btn_play && mService.isStopped()) {
            play();
        } else if (id == R.id.btn_play && mService.isPlaying()) {
            pause();
        } else if (id == R.id.btn_play && mService.isPaused()) {
            resume();
        } else if (id == R.id.btn_previous_song && (mService.isPaused() || mService.isStopped())) {
            skipToPrevious();
        } else if (id == R.id.btn_previous_song && mService.isPlaying()) {
            playPrevious();
        } else if (id == R.id.btn_next_song && (mService.isPaused() || mService.isStopped())) {
            skipToNext();
        } else if (id == R.id.btn_next_song && mService.isPlaying()) {
            playNext();
        } else {
            Toast.makeText(getActivity(), "Unhandled click event yo!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Binds the current song's details with the image and titles
     */
    private void bindSongDetails(Song song) {
        Log.d("bindSongDetails", song.name);

        // save the new song
        mPlayerState.setPlayListIndex(mService.getPlayListIndex());

        mSongNameText.setText(song.name);

        // artist / album details
        mArtistNameText.setText(song.album.artist.name);
        mAlbumNameTest.setText(song.album.name);

        // album image || artist image
        String imageUrl = song.album.imageUrl == null ? song.album.artist.imageUrl : song.album.imageUrl;

        // song time details
        mSongDuration.setText(toTime(30));
        mSongPosition.setText(toTime(0));

        Picasso.with(getActivity())
                .load(imageUrl)
                .into(mAlbumImage);
    }

    private void pause() {
        mService.pause();
        updatePlayerControls();
    }

    private void resume() {
        mService.resume();
        updatePlayerControls();
    }

    public void play() {
        Log.d(TAG, "play");
        mProgressAnim.cancel();
        mPlayButton.setEnabled(false);  // will be re-enabled once the song is retrieved
        mService.play();
        bindSongDetails(mService.getCurrentSong());
    }

    /**
     * Play the next track
     */
    public void playNext() {
        Log.d(TAG, "playNext");
        mProgressAnim.cancel();
        mPlayButton.setEnabled(false);  // will be re-enabled once the song is retrieved
        if (mService.playNext()) {
            bindSongDetails(mService.getCurrentSong());
        }
    }

    /**
     * Play the previous track
     */
    public void playPrevious() {
        Log.d(TAG, "playPrevious");
        mProgressAnim.cancel();
        mPlayButton.setEnabled(false);  // will be re-enabled once the song is retrieved
        if (mService.playPrevious()) {
            bindSongDetails(mService.getCurrentSong());
        }
    }

    public void skipToPrevious() {
        Log.d(TAG, "skipToPrevious");
        mProgressAnim.cancel();
        if (mService.skipToPrevious()) {
            setProgressBarPosition(0);
            bindSongDetails(mService.getCurrentSong());
        }
    }

    public void skipToNext() {
        Log.d(TAG, "skipToNext");
        mProgressAnim.cancel();
        if (mService.skipToNext()) {
            setProgressBarPosition(0);
            bindSongDetails(mService.getCurrentSong());
        }
    }

    private void setProgressBarPosition(int time) {
        final int duration = mService.getPreviewDuration();

        mProgressAnim = ValueAnimator.ofInt(0, duration);
        mProgressAnim.setDuration(duration);
        mProgressAnim.setRepeatCount(ValueAnimator.INFINITE);  // HACK: without this the progress bar will unexpectedly stop
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
}
