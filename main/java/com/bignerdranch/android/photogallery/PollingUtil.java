package com.bignerdranch.android.photogallery;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by lawren on 01/11/17.
 */

public class PollingUtil {

    private static boolean isNetworkAvailableAndConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    public static void pollImages(Context context){
        if(!isNetworkAvailableAndConnected(context)){
            return;
        }

        String TAG = context.getClass().getName();
        List<GalleryItem> items;
        String query = QueryPreferences.getStoredQuery(context);

        if(query == null){
            FlickrFetcher flickrFetcher = new FlickrFetcher();
            int currentPage = flickrFetcher.getCurrentPage();
            items = flickrFetcher.fetchRecentPhotos(currentPage);
        }else{
            items = new FlickrFetcher().searchPhotos(query);
        }

        if(items.size() == 0){
            return;
        }

        String lastResultId = QueryPreferences.getLastResultId(context);
        String resultId = items.get(0).getId();

        if(resultId.equals(lastResultId)){
            Log.i(TAG, "Got an old result: " + resultId);
        }else{
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = context.getResources();
            Intent photoIntent = PhotoGalleryActivity.newIntent(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, photoIntent, 0);

            Notification notification = new NotificationCompat.Builder(context)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(0, notification);

        }

        QueryPreferences.setLastResultId(context, resultId);

    }
}
