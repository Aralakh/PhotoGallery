package com.bignerdranch.android.photogallery;

/**
 * Created by lawren on 22/09/17.
 */

public class GalleryItem {
    private String title;
    private String id;
    private String url_s;

    public String getCaption() {
        return title;
    }

    public void setCaption(String caption) {
        title = caption;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url_s;
    }

    public void setUrl(String url) {
        this.url_s = url;
    }

    @Override
    public String toString(){
        return title;
    }
}
