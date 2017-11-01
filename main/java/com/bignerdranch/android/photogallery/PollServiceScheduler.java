package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
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
        //schedule job to run once every 30 minutes
        if(startSchedule){
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, PollServiceScheduler.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(1000*60*30)
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
                PollingUtil.pollImages(PollServiceScheduler.this);
                jobFinished(jobParams, false);
            }catch(Exception e){
                jobFinished(jobParams, true);
            }

            return null;
        }
    }
}
