package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by lawren on 31/10/17.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)

public class PollServiceScheduler extends JobService {
    private static final String TAG = "PollServiceScheduler";
    private PollTask mCurrentTask = new PollTask();
    private static final int JOB_ID = 1;

    @Override
    public boolean onStartJob(JobParameters parameters){
        Log.i(TAG, "Starting job");
        mCurrentTask.execute(parameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters){
        Log.i(TAG, "Stopping job");
        if(mCurrentTask != null){
            mCurrentTask.cancel(true);
        }
        return false;
    }

    public static boolean hasBeenScheduled(Context context){
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean isScheduled = false;
        for(JobInfo jobInfo : scheduler.getAllPendingJobs()){
            if(jobInfo.getId() == JOB_ID){
                isScheduled = true;
                break;
            }
        }
        return isScheduled;
    }

    public static void scheduleJob(Context context, boolean startSchedule){
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        //schedule job to run once every 15 minutes
        if(startSchedule){
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, PollServiceScheduler.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(1000*60*15)
                    .setPersisted(true)
                    .build();
                    scheduler.schedule(jobInfo);
                    Log.i(TAG, "Scheduler.scheduled " + jobInfo);
        }else{
            scheduler.cancel(JOB_ID);
            Log.i(TAG, "Scheduler.canceled");
        }
    }

    private class PollTask extends AsyncTask<JobParameters, Void, Void>{

        @Override
        protected Void doInBackground(JobParameters... parameters){
            JobParameters jobParams = parameters[0];
            Log.i(TAG, "Polling Flickr for new images");
            try{
                pollImages();
                jobFinished(jobParams, false);
            }catch(Exception e){
                jobFinished(jobParams, true);
            }

            return null;
        }
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    private void pollImages(){
        if(!isNetworkAvailableAndConnected()){
            return;
        }

        List <GalleryItem> items;
        String query = QueryPreferences.getStoredQuery(PollServiceScheduler.this);

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
