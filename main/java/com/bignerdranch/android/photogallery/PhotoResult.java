package com.bignerdranch.android.photogallery;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lawren on 25/09/17.
 */

public class PhotoResult {
    private int page;
    private int pages;
    private int perpage;
    private int total;

    @SerializedName("photo")
    List<GalleryItem> mGalleryItemsList;

    public void setPage(int page) {
        this.page = page;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public void setPerpage(int perpage) {
        this.perpage = perpage;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public int getMaxPages() {
        return pages;
    }

    public int getItemsPerPage() {
        return perpage;
    }

    public int getTotal() {
        return total;
    }

    public List<GalleryItem> getGalleryItemsList() {
        return mGalleryItemsList;
    }
}