package com.bignerdranch.android.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lawren on 22/09/17.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private List<GalleryItem> mItems = new ArrayList<>();
    private FlickrFetcher mFlickrFetcher = new FlickrFetcher();

    private boolean mIsLoading = true;
    public int pastVisibleItem;
    public int mMaxPage = 1;
    public int currentPage;
    public int mItemsPerPage;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mGridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);



        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                pastVisibleItem = mGridLayoutManager.findLastVisibleItemPosition();

                if(!mIsLoading && (pastVisibleItem >= mItems.size()-1) && (currentPage < mMaxPage)){
                    mIsLoading = true;
                    currentPage++;
                    new FetchItemTask().execute();
                }
            }

        });


        setupAdapter();

        return v;
    }


    private class PhotoHolder extends RecyclerView.ViewHolder{
        private TextView mTitleTextView;

        public PhotoHolder(View itemView){
            super(itemView);
            mTitleTextView = (TextView) itemView;
        }

        public void bindGallery(GalleryItem item){
            mTitleTextView.setText(item.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGallery(galleryItem);
        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>>{
        @Override
        protected List<GalleryItem> doInBackground(Void... params){
            return mFlickrFetcher.fetchItems(currentPage);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            //first time querying data
            if(mItems.size() == 0){
                mMaxPage = mFlickrFetcher.getMaxPages();
                mItemsPerPage = mFlickrFetcher.getItemsPerPage();
                mItems.addAll(items);

                mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    //dynamically scale number of columns based on device size
                    public void onGlobalLayout(){
                        mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int width = mPhotoRecyclerView.getMeasuredWidth();
                        float columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getActivity().getResources().getDisplayMetrics());
                        int columnNumber = Math.round(width/columnWidth);
                        mGridLayoutManager = new GridLayoutManager(getActivity(), columnNumber);
                        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);
                        setupAdapter();
                    }
                });
            }else{
                final int prevSize = mItems.size();
                mItems.addAll(items);
                mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    //dynamically scale number of columns based on device size
                    public void onGlobalLayout(){
                        mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int width = mPhotoRecyclerView.getMeasuredWidth();
                        float columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getActivity().getResources().getDisplayMetrics());
                        int columnNumber = Math.round(width/columnWidth);
                        mGridLayoutManager = new GridLayoutManager(getActivity(), columnNumber);
                        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);
                        mPhotoRecyclerView.smoothScrollToPosition(prevSize);
                        setupAdapter();
                    }
                });
            }
            mIsLoading = false;
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }
}
