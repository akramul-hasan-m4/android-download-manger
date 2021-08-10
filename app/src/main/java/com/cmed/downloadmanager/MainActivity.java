package com.cmed.downloadmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cmed.downloadmanager.service.DownloadService;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private String desFilePath;
    private MaterialButton download;
    private TextView tvProgress;
    private TextView tvFileLocation;
    private ProgressBar progressBar;

    private ServiceConnection mServiceConnection = null;
    private DownloadService mService = null;
    private boolean mBound = false;
    private DataReceiver myReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        if(mServiceConnection != null){
            bindService(new Intent(this, DownloadService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setClickListener();
        serviceSettings();
    }

    private void setClickListener(){
        download.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                if(mService != null){
                    download.setEnabled(false);
                    mService.startDownload(desFilePath);
                }
            }
        });
    }

    private void init(){
        tvProgress = findViewById(R.id.tv_progess);
        tvFileLocation = findViewById(R.id.tv_file_location);
        download = findViewById(R.id.btn_download);
        progressBar = findViewById(R.id.progressBar);
        desFilePath = getExternalFilesDir(null).getAbsolutePath() + "/SampleVideo_1280x720_30mb.mp4";
    }

    private void serviceSettings(){
        myReceiver = new DataReceiver();
        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DownloadService.LocalBinder binder = (DownloadService.LocalBinder) service;
                mService = binder.getService();
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
                mBound = false;
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter(DownloadService.ACTION_BROADCAST));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindLocService();
    }

    public class DataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra(DownloadService.EXTRA_PROGRESS, 0);
            long downloadedLengthKb = intent.getLongExtra(DownloadService.EXTRA_DOWNLOADED, 0);
            long totalLengthKb = intent.getLongExtra(DownloadService.EXTRA_TOTAL, 0);
            int finish = intent.getIntExtra(DownloadService.EXTRA_FINISH, 0);
            int failed = intent.getIntExtra(DownloadService.EXTRA_FAILED, 0);
            String fileLocation = intent.getStringExtra(DownloadService.EXTRA_FILE_LOC);

            if(finish == 1){
                download.setEnabled(true);
                tvFileLocation.setText(String.format("Download Location :\n%s", fileLocation));
            }
            if(failed == 1){
                download.setEnabled(true);
            }

            setValue(progress, downloadedLengthKb, totalLengthKb);
        }
    }

    public void setValue(int progress, long downloadedLengthKb, long totalLengthKb){
        progressBar.setProgress(progress);
        tvProgress.setText(String.format(Locale.US,"File download progress：%d%s \n\nDownloaded:%sKB | Total length:%sKB", progress,"%", downloadedLengthKb + "", totalLengthKb + ""));
    }

    private void unbindLocService(){
        if (mBound) {
            if(mServiceConnection != null){
                unbindService(mServiceConnection);
            }
            mBound = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PERMISSION_REQUEST_CODE != requestCode) {
            return;
        }
        if ((permissions.length > 0) && (grantResults.length > 0) && Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0]) &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if(mService != null){
                download.setEnabled(false);
                mService.startDownload(desFilePath);
            }
        } else {
            Toast.makeText(this, "Please grant storage permissions", Toast.LENGTH_LONG).show();
        }
    }

}