package com.cmed.downloadmanager.utils;

import android.util.Log;

import com.cmed.downloadmanager.config.InputParameter;
import com.cmed.downloadmanager.listener.DownloadListener;
import com.cmed.downloadmanager.network.APIInterface;
import com.cmed.downloadmanager.network.DownloadInterceptor;
import com.cmed.downloadmanager.network.RetrofitConfig;
import com.cmed.downloadmanager.service.DownloadService;
import com.cmed.downloadmanager.thred.MainThreadExecutor;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

public class DownloadUtil {

    private static final String TAG = "DownloadUtil";
    private static final int DEFAULT_TIMEOUT = 15;
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final MainThreadExecutor uiExecutor = new MainThreadExecutor();
    private OkHttpClient.Builder mBuilder;

    private DownloadUtil() {
    }

    public static DownloadUtil getInstance() {
        return DownloadUtil.SingletonHolder.INSTANCE;
    }

    public void initConfig(OkHttpClient.Builder builder) {
        this.mBuilder = builder;
    }

    public void downloadFile(InputParameter inputParam, final DownloadListener listener) {

        DownloadInterceptor interceptor = new DownloadInterceptor(listener);
        if (mBuilder != null) {
            mBuilder.addInterceptor(interceptor);
        } else {
            mBuilder = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .retryOnConnectionFailure(true)
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        }
        final APIInterface api = new Retrofit.Builder()
                .baseUrl(inputParam.getBaseUrl())
                .client(mBuilder.build())
                .build()
                .create(APIInterface.class);

        mExecutorService.execute(() -> {
            try {
                Response<ResponseBody> result = api.downloadWithDynamicUrl(inputParam.getRelativeUrl()).execute();
                final ResponseBody body = result.body();
                if(body != null) {
                    File file = FileUtil.writeFile(inputParam.getLoadedFilePath(), body.byteStream());
                    if (listener != null) {
                        if (inputParam.isCallbackOnUiThread()) {
                            uiExecutor.execute(() -> listener.onFinish(file));
                        } else {
                            listener.onFinish(file);
                        }
                    }
                }

            } catch (Exception e) {
                if (listener != null) {
                    if (inputParam.isCallbackOnUiThread()) {
                        uiExecutor.execute(() -> listener.onFailed(e.getMessage()));
                    } else {
                        listener.onFailed(e.getMessage());
                    }
                }
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    private static class SingletonHolder {
        private static final DownloadUtil INSTANCE = new DownloadUtil();
    }

}
