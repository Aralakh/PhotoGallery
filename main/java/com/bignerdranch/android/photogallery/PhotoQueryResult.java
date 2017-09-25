package com.bignerdranch.android.photogallery;

import java.util.List;

/**
 * Created by lawren on 25/09/17.
 */

public class PhotoQueryResult {
    PhotoResult mPhotos;

    List<GalleryItem> getResult(){
        return mPhotos.getGalleryItemsList();
    }

    int getPageCount(){
        return mPhotos.getMaxPages();
    }

    int getTotalCount(){
        return mPhotos.getTotal();
    }

    int getPerPageCount(){
        return mPhotos.getItemsPerPage();
    }
}
