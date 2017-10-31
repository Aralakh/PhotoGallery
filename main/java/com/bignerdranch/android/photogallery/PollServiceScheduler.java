package com.bignerdranch.android.photogallery;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by lawren on 31/10/17.
 */

public class PollServiceScheduler extends JobService {
    private static final String TAG = "PollServiceScheduler";
    private PollTask mCurrentTask;
    private static boolean mIsRunning;

    public static boolean getIsRunning(){
        return mIsRunning;
    }

    @Override
    public boolean onStartJob(JobParameters parameters){
        mCurrentTask = new PollTask();
        mCurrentTask.execute(parameters);
        mIsRunning = true;
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters){
        if(mCurrentTask != null){
            mCurrentTask.cancel(true);
            mIsRunning =false;
        }
        return true;
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, List<GalleryItem>>{

        @Override
        protected List<GalleryItem> doInBackground(JobParameters... parameters){
            JobParameters jobParams = parameters[0];
            Log.i(TAG, "Polling Flickr for new images");
            String query = QueryPreferences.getStoredQuery(PollServiceScheduler.this);
            List<GalleryItem> items;

            if(query == null){
                FlickrFetcher flickrFetcher = new FlickrFetcher();
                int currentPage = flickrFetcher.getCurrentPage();
                items = flickrFetcher.fetchRecentPhotos(currentPage);
            }else{
                items = new FlickrFetcher().searchPhotos(query);
            }

            jobFinished(jobParams, false);
            return items;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            if(!isNetworkAvailableAndConnected()){
               return;
            }

            if(items.size() == 0){
                return;
            }

            String lastResultId = QueryPreferences.getLastResultId(PollServiceScheduler.this);
            String resultId = items.get(0).getId();

            if(resultId.equals(lastResultId)){
                Log.i(TAG, "Got an old result: " + resultId);
            }else{
                Log.i(TAG, "Got a new result: " + resultId);

                Resources resources = getResources();
                Intent photoIntent = PhotoGalleryActivity.newIntent(PollServiceScheduler.this);
                PendingIntent pendingIntent = PendingIntent.getActivity(PollServiceScheduler.this, 0, photoIntent, 0);

                Notification notification = new NotificationCompat.Builder(PollServiceScheduler.this)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(PollServiceScheduler.this);
                notificationManager.notify(0, notification);

            }

            QueryPreferences.setLastResultId(PollServiceScheduler.this, resultId);

        }
    }
}
