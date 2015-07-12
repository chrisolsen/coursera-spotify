package org.chrisolsen.spotify;

import android.accounts.NetworkErrorException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class ArtistSearchActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ContentValues>> {

    private final String LOG_TAG = this.getClass().getSimpleName();
    private static final int LOADER_ARTIST_SEARCH = 0;

    private ArtistSearchAdapter mArtistSearchAdapter;

    private ListView mListView;
    private EditText mSearchText;
    private ImageButton mCancelButton;
    private View mNoResults;
    private View mSearchInstructions;
    private ImageView mSearchIndicator;
    private InputMethodManager mImm;

    private ContentValues[] mSearchResults;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context context = getActivity();
        View layout = inflater.inflate(R.layout.artist_search_fragment, container, false);

        // views
        mListView = (ListView)layout.findViewById(android.R.id.list);
        mSearchText = (EditText) layout.findViewById(R.id.artists_search_filter);
        mCancelButton = (ImageButton) layout.findViewById(R.id.artist_search_cancel);
        mNoResults = layout.findViewById(android.R.id.empty);
        mSearchInstructions = layout.findViewById(R.id.artists_search_instructions);
        mSearchIndicator = (ImageView) layout.findViewById(R.id.artist_search_indicator);
        mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        
        // ListView select event
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContentValues artist = (ContentValues) mListView.getAdapter().getItem(position);

                Intent intent = new Intent(context, ArtistTopSongsActivity.class);
                intent.putExtra("data", artist);

                startActivity(intent);
            }
        });

        bindSearch();

        mArtistSearchAdapter = new ArtistSearchAdapter(getActivity(), new ArrayList<ContentValues>());
        mListView.setAdapter(mArtistSearchAdapter);

        return layout;
    }

    /**
     * Bind any saved search results to the search adapter to retain any previous scroll position
     * and ensure any previous search results remain.
     * @param savedInstanceState
     */
    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) return;

        Parcelable[] data = savedInstanceState.getParcelableArray("searchResults");
        if (data == null) return;

        // convert to simple array
        mSearchResults = new ContentValues[data.length];
        for (int i = 0; i < data.length; i++) {
            mSearchResults[i] = (ContentValues)data[i];
        }

        // bind data
        mArtistSearchAdapter.clear();
        mArtistSearchAdapter.addAll(mSearchResults);
        mArtistSearchAdapter.notifyDataSetChanged();
        mSearchInstructions.setVisibility(View.GONE);
    }

    /**
     * Save any existing search data. Since the data is stored in ContentValues, which are already
     * parcelable, there is no additional work required.
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArray("searchResults", mSearchResults);
    }

    /**
     * Bind the search *like* view controls
     */
    private void bindSearch() {

        final LoaderManager.LoaderCallbacks<List<ContentValues>> self = this;

        mSearchText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    getLoaderManager().restartLoader(LOADER_ARTIST_SEARCH, null, self);
                    return true;
                }
                return false;
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchText.setText("");
                mSearchIndicator.setImageResource(R.mipmap.ic_search_dark);
                mSearchIndicator.clearAnimation();
                mSearchInstructions.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Initialize the loader
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LoaderManager lm = getLoaderManager();

        // no idea why this *if* is needed, but if this check is not added
        if (lm.getLoader(LOADER_ARTIST_SEARCH) != null) {
            lm.initLoader(LOADER_ARTIST_SEARCH, null, this);
        }

        setRetainInstance(true);
    }

    @Override
    public Loader<List<ContentValues>> onCreateLoader(int id, Bundle args) {
        try {
            String filter = mSearchText.getText().toString();
            mImm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);

            if (filter.length() > 0) {
                Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_around_center);
                mSearchIndicator.startAnimation(animation);
                mSearchIndicator.setImageResource(R.mipmap.ic_spinner);
            }
            return new ArtistSearchLoader(getActivity(), filter);
        } catch (NetworkErrorException e) {
            Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<ContentValues>> loader, List<ContentValues> data) {
        boolean hasFilter = mSearchText.getText().toString().length() > 0;
        boolean hasData = data.size() > 0;

        if (!hasFilter && !hasData) {
            mSearchInstructions.setVisibility(View.VISIBLE);
            mSearchInstructions.clearAnimation();
            mSearchIndicator.setImageResource(R.mipmap.ic_search_dark);
            mListView.setVisibility(View.GONE);
            mNoResults.setVisibility(View.GONE);
        } else
        if (hasFilter && hasData) {
            mSearchInstructions.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            mNoResults.setVisibility(View.GONE);
        } else
        if (hasFilter && !hasData) {
            mSearchInstructions.setVisibility(View.GONE);
            mListView.setVisibility(View.GONE);
            mNoResults.setVisibility(View.VISIBLE);
        }

        mSearchResults = new ContentValues[data.size()];
        data.toArray(mSearchResults);

        mArtistSearchAdapter.clear();
        mArtistSearchAdapter.addAll(data);
        mArtistSearchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<ContentValues>> loader) {
        mArtistSearchAdapter.clear();
        mArtistSearchAdapter.notifyDataSetChanged();
    }

}
