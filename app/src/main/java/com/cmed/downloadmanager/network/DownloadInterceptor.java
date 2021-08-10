package com.cmed.downloadmanager.network;

import com.cmed.downloadmanager.listener.DownloadListener;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;

public class DownloadInterceptor implements Interceptor {

        private final DownloadListener listener;

    public DownloadInterceptor(DownloadListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());

            return originalResponse.newBuilder()
                    .body(new DownloadResponseBody(originalResponse.body(), listener))
                    .build();
        }
}
