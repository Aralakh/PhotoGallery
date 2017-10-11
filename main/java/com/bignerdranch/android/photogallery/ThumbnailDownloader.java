package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by lawren on 05/10/17.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRELOAD = 1;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloaderListener;
    private LruCache<String, Bitmap> mMemoryCache;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap bitmap);
    }

    public void setThumbnailDownloaderListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloaderListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;

        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    @Override
    public boolean quit(){
        mHasQuit = true;
        return super.quit();
    }

    @Override
    protected void onLooperPrepared(){
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }else if(msg.what == MESSAGE_PRELOAD){
                    String url = (String) msg.obj;
                    downloadBitmap(url);
                }
            }
        };
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap){
        if(getBitmapFromMemCache(key) == null){
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key){
        return mMemoryCache.get(key);
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a URL: " + url);

        if(url == null){
            mRequestMap.remove(target);
        }else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void preloadImage(String url){
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestHandler.removeMessages(MESSAGE_PRELOAD);
        mRequestMap.clear();
        mMemoryCache.evictAll();
    }

    private void handleRequest(final T target){
//        try {
//            final String url = mRequestMap.get(target);
//            if (url == null) {
//                return;
//            }
//
//            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
//            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
//            Log.i(TAG, "Bitmap created");
//
//            mResponseHandler.post(new Runnable(){
//               public void run(){
//                   if(mRequestMap.get(target) != url || mHasQuit){
//                       return;
//                   }
//
//                   mRequestMap.remove(target);
//                   mThumbnailDownloaderListener.onThumbnailDownloaded(target, bitmap);
//               }
//            });
//        }catch (IOException ioe){
//                Log.e(TAG, "Error downloading image", ioe);
//        }

        final String url = mRequestMap.get(target);
        final Bitmap bitmap;
        if(url == null){
            return;
        }

        bitmap = downloadBitmap(url);
        Log.i(TAG, "Bitmap created.");

        mResponseHandler.post(new Runnable(){
           public void run(){
               if(mRequestMap.get(target) != url || mHasQuit){
                   return;
               }

               mRequestMap.remove(target);
               mThumbnailDownloaderListener.onThumbnailDownloaded(target, bitmap);
           }
        });
    }

    private Bitmap downloadBitmap(String url){
        if(url == null){
            return null;
        }

        Bitmap bitmap = getBitmapFromMemCache(url);

        if(bitmap != null){
            return bitmap;
        }

        try {
            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            addBitmapToMemoryCache(url, bitmap);
            return bitmap;
        } catch (IOException ioe) {
            Log.e(TAG,"Cannot download the image ",ioe);
            return null;
        }
    }
}
