package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lawren on 22/09/17.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";

    private ProgressBar mProgressBar;
    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private List<GalleryItem> mItems = new ArrayList<>();
    private FlickrFetcher mFlickrFetcher = new FlickrFetcher();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private boolean mIsLoading = true;
    public int mFirstVisibleItem;
    public int mPastVisibleItem;
    public int mMaxPage = 1;
    public int mCurrentPage;
    public int mItemsPerPage;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
       // updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                searchView.onActionViewCollapsed();
                mItems.clear();
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                mItems.clear();
                mCurrentPage = 0;
                mMaxPage = 1;
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems(){
        showProgressBar();
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemTask(query).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mProgressBar = (ProgressBar) v.findViewById(R.id.loading_bar);
        mGridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);
        updateItems();

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mPastVisibleItem = mGridLayoutManager.findLastVisibleItemPosition();
                mFirstVisibleItem = mGridLayoutManager.findFirstVisibleItemPosition();
                if (!mIsLoading && (mPastVisibleItem >= mItems.size() - 1) && (mCurrentPage < mMaxPage)) {
                    mIsLoading = true;
                    mCurrentPage++;
                    updateItems();
                }
            }

        });

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed.");
    }

    public void showProgressBar(){
        mProgressBar.setVisibility(View.VISIBLE);
        mPhotoRecyclerView.setVisibility(View.GONE);
    }

    public void hideProgressBar(){
        mProgressBar.setVisibility(View.GONE);
        mPhotoRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    //preload the 10 previous and 10 next images based on the first visible item
    private void preloadImages(int position) {
        final int imageBufferSize = 10;
        int startPosition = position + 1;
        PhotoAdapter adapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
        int upperLimit = Math.min(startPosition + imageBufferSize, adapter.getItemCount());

        for (int i = startPosition; i < upperLimit; i++) {
            mThumbnailDownloader.preloadImage(adapter.mGalleryItems.get(i).getUrl());
        }

        startPosition = mGridLayoutManager.findFirstVisibleItemPosition() - 1;
        int lowerLimit = Math.max(startPosition - imageBufferSize, 0);
        for (int i = startPosition; i > lowerLimit; i--) {
            mThumbnailDownloader.preloadImage(adapter.mGalleryItems.get(i).getUrl());
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView;
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Bitmap bitmap = mThumbnailDownloader.getBitmapFromMemCache(galleryItem.getUrl());
            if(bitmap == null){
                Drawable placeholder = ContextCompat.getDrawable(getActivity(), R.drawable.blaze);
                photoHolder.bindDrawable((placeholder));
                mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            }else{
                Log.i(TAG, "Loaded cached image");
                Drawable cachedImage = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(cachedImage);
            }
            mFirstVisibleItem = mGridLayoutManager.findFirstVisibleItemPosition();
            preloadImages(mFirstVisibleItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if(mQuery == null){
                return mFlickrFetcher.fetchRecentPhotos(mCurrentPage);
            } else{
                return new FlickrFetcher().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            //first time querying data
            if (mItems.size() == 0) {
                mItems.addAll(items);
                mMaxPage = mFlickrFetcher.getMaxPages();
                mItemsPerPage = mFlickrFetcher.getItemsPerPage();


                mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    //dynamically scale number of columns based on device size
                    public void onGlobalLayout() {
                        int width = mPhotoRecyclerView.getMeasuredWidth();
                        float columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getActivity().getResources().getDisplayMetrics());
                        int columnNumber = Math.round(width / columnWidth);
                        mGridLayoutManager = new GridLayoutManager(getActivity(), columnNumber);
                        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);
                        mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

                setupAdapter();
            } else {
                final int prevSize = mItems.size();
                mItems.addAll(items);

                mPhotoRecyclerView.getAdapter().notifyItemRangeInserted(prevSize, mItemsPerPage);
                mPhotoRecyclerView.smoothScrollToPosition(prevSize);

            }
            hideProgressBar();
            mIsLoading = false;
        }
    }
}
