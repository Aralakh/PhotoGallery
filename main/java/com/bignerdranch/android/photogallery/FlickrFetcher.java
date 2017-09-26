package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lawren on 22/09/17.
 */

public class FlickrFetcher {
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY ="17799f23e3aa7e76d8436fb030d88745";

    private int maxPages;
    private int totalItems;
    private int itemsPerPage;
    private int currentPage = 1;

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public int getCurrentPage(){
        return currentPage;
    }

    public void setCurrentPage(int page){
        currentPage = page;
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }

            out.close();
            return out.toByteArray();
        }finally{
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(int currentPage){

        List<GalleryItem> items = new ArrayList<>();

        try{
            String url = Uri.parse("https://api.flickr.com/services/rest")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("page", Integer.toString(currentPage))
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);

            Gson gson = new Gson();
            PhotoResultQuery result = gson.fromJson(jsonString, PhotoResultQuery.class);
            setMaxPages(result.getPhotos().getMaxPages());
            setTotalItems(result.getPhotos().getTotal());
            setItemsPerPage(result.getPhotos().getItemsPerPage());
            items = result.getPhotos().getGalleryItemsList();
        }catch(IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);

        }catch(Exception e){
            Log.e(TAG, "Failed to parse JSON: " + e.getMessage(), e);
        }

        return items;
    }
}
