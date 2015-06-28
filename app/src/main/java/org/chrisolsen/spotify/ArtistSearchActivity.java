package org.chrisolsen.spotify;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;


public class ArtistSearchActivity extends ActionBarActivity {

    Menu _menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_search_activity);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_artist_search:
                bindSearchActionBar();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindSearchActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setCustomView(R.layout.actionbar_search);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);

        // hide all the existing menu items
        for (int i = 0; i < _menu.size(); i++) {
            MenuItem item = _menu.getItem(i);
            item.setVisible(false);
        }

        View bar = actionBar.getCustomView();
        ImageButton cancel = (ImageButton) bar.findViewById(R.id.action_artist_search_cancel);
        final EditText filter = (EditText) bar.findViewById(R.id.action_artist_search_filter);
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // focus and show keyboard
        filter.requestFocus();
        imm.showSoftInput(filter, InputMethodManager.SHOW_IMPLICIT);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter.setText("");

                // hide the keyboard
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
                imm.hideSoftInputFromWindow(filter.getWindowToken(), 0);

                // re-show the menuitems
                for (int i = 0; i < _menu.size(); i++) {
                    MenuItem item = _menu.getItem(i);
                    item.setVisible(true);
                }
            }
        });
    }
}
