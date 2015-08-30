package org.chrisolsen.spotify;


import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class PlayerState {
    private static final String STATE_LIST_KEY = "stateListKey";
    private static final String STATE_INDEX_KEY = "stateIndexKey";
    private static final String PLAYER_STATE_SETTINGS_KEY = "playerStateSettingsKey";

    private Context mContext;

    public PlayerState(Context c) {
        mContext = c;
    }

    public Song[] getPlayList() {
        String json = getState(STATE_LIST_KEY);
        return new Gson().fromJson(json, Song[].class);
    }

    public void setPlayList(Song[] playList) {
        String json = new Gson().toJson(playList);
        putState(STATE_LIST_KEY, json);
    }

    public int getPlayListIndex() {
        String index = getState(STATE_INDEX_KEY);
        return Integer.parseInt(index);
    }

    public void setPlayListIndex(int playListIndex) {
        String index = Integer.toString(playListIndex);
        putState(STATE_INDEX_KEY, index);
    }

    private void putState(String key, String value) {
        SharedPreferences.Editor e = mContext.getSharedPreferences(PLAYER_STATE_SETTINGS_KEY, 0).edit();
        e.putString(key, value);
        e.commit();
    }

    private String getState(String key) {
        SharedPreferences p = mContext.getSharedPreferences(PLAYER_STATE_SETTINGS_KEY, 0);
        return p.getString(key, null);
    }

    public boolean equals(PlayerState ps) {
        return this.hashCode() == ps.hashCode();
    }

    @Override
    public int hashCode() {
        StringBuilder b = new StringBuilder();

        for (Song song : getPlayList()) {
            b.append(song.songId);
        }
        b.append("-");
        b.append(getPlayListIndex());

        return b.toString().hashCode();
    }

}
