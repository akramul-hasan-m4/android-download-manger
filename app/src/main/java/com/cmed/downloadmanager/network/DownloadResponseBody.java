package com.cmed.downloadmanager.network;

import android.util.Log;

import com.cmed.downloadmanager.listener.DownloadListener;
import com.cmed.downloadmanager.thred.MainThreadExecutor;

import java.io.IOException;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class DownloadResponseBody extends ResponseBody {

    private final ResponseBody responseBody;
    private final DownloadListener downloadListener;
    private BufferedSource bufferedSource;
    private Executor uiExecutor;

    public DownloadResponseBody(ResponseBody responseBody, DownloadListener downloadListener) {
        this.responseBody = responseBody;
        this.downloadListener = downloadListener;
        uiExecutor = new MainThreadExecutor();
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @NonNull
    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;
            @Override
            public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                final long bytesRead = super.read(sink, byteCount);
                if (null != downloadListener) {
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    Log.d("DownloadUtil", "Already downloaded :" + totalBytesRead + " Total length : " + responseBody.contentLength());
                    final int progress = (int) (totalBytesRead * 100 / responseBody.contentLength());
                    if (uiExecutor == null) {
                        uiExecutor = new MainThreadExecutor();
                    }
                    uiExecutor.execute(() -> downloadListener.onProgress(progress,totalBytesRead/1024,responseBody.contentLength()/1024));
                }
                return bytesRead;
            }
        };
    }
}
