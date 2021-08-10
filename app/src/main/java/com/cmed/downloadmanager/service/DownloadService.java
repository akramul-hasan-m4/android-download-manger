package com.cmed.downloadmanager.service;


import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.cmed.downloadmanager.MainActivity;
import com.cmed.downloadmanager.R;
import com.cmed.downloadmanager.config.InputParameter;
import com.cmed.downloadmanager.listener.DownloadListener;
import com.cmed.downloadmanager.utils.DownloadUtil;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class DownloadService extends Service {

    private static final String CHANNEL_ID = "channel_01";
    private static final String BASE_URL = "http://dropbox.sandbox2000.com/intrvw/";
    private static final String FILE_URL = "SampleVideo_1280x720_30mb.mp4";
    public static final String EXTRA_PROGRESS = "com.cmed.downloadmanager.service.data";
    public static final String EXTRA_DOWNLOADED = "com.cmed.downloadmanager.service.downloaded";
    public static final String EXTRA_TOTAL = "com.cmed.downloadmanager.service.total";
    public static final String EXTRA_FINISH = "com.cmed.downloadmanager.service.finish";
    public static final String EXTRA_FILE_LOC = "com.cmed.downloadmanager.service.fileloc";
    public static final String EXTRA_FAILED = "com.cmed.downloadmanager.service.failed";
    public static final String ACTION_BROADCAST ="com.cmed.downloadmanager.broadcast";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "com.cmed.downloadmanager.started_from_notification";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
            if (startedFromNotification) {
                stopSelf();
            }
        }
         return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        startForeground(NOTIFICATION_ID, getDownloadNotification("Downloading...",0));
        return true;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        super.onRebind(intent);
    }

    public class LocalBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    public void startDownload(String desFilePath) {
        DownloadUtil.getInstance()
                .downloadFile(new InputParameter.Builder(BASE_URL, FILE_URL, desFilePath)
                        .setCallbackOnUiThread(true)
                        .build(), new DownloadListener() {
                    @Override
                    public void onFinish(final File file) {
                        Intent intent = new Intent(ACTION_BROADCAST);
                        intent.putExtra(EXTRA_FINISH, 1);
                        intent.putExtra(EXTRA_FILE_LOC, file.getAbsolutePath());

                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }

                    @Override
                    public void onProgress(int progress, long downloadedLengthKb, long totalLengthKb) {

                        Intent intent = new Intent(ACTION_BROADCAST);
                        intent.putExtra(EXTRA_PROGRESS, progress);
                        intent.putExtra(EXTRA_DOWNLOADED, downloadedLengthKb);
                        intent.putExtra(EXTRA_TOTAL, totalLengthKb);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                        getDownloadNotification("Downloading...", progress);
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        Intent intent = new Intent(ACTION_BROADCAST);
                        intent.putExtra(EXTRA_FAILED, 1);

                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }
                });
    }

    public Notification getDownloadNotification(String title, int progress) {
        Intent intent2 = new Intent(this, DownloadService.class);

        intent2.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        Intent intent1 = new Intent(this, DownloadService.class);
        intent1.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Ch01";
            String description = "abc";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
        notifyBuilder.setSmallIcon(android.R.mipmap.sym_def_app_icon);
        if(progress == -1){
            notifyBuilder.setSmallIcon(R.drawable.done);
        }else{
            notifyBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        }

        notifyBuilder.addAction(R.drawable.done, "Open ESS", activityPendingIntent);
        notifyBuilder.setContentIntent(pendingIntent);
        notifyBuilder.setContentTitle(title);
        notifyBuilder.setFullScreenIntent(pendingIntent, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifyBuilder.setChannelId(CHANNEL_ID);
        }
        if(progress > 0 && progress < 100) {
            notifyBuilder.setContentText("Download progress " + progress + "%");
            notifyBuilder.setProgress(100, progress, false);
        }

        return notifyBuilder.build();
    }

    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager != null){
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (getClass().getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
