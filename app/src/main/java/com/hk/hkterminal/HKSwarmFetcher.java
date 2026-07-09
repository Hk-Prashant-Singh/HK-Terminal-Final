package com.hk.hkterminal;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HK-OPERATION : SWARM FETCHER (PARALLEL DOWNLOAD ENGINE)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Multi-threaded payload acquisition with zero latency.
 */
public class HKSwarmFetcher {

    // 4 parallel threads for maximum aggressive fetching
    private static final ExecutorService swarmPool = Executors.newFixedThreadPool(4);

    public interface FetchCallback {
        void onProgress(String msg);
        void onComplete(boolean success);
    }

    public static void executeSwarmFetch(List<String> urls, File cacheDir, FetchCallback callback) {
        AtomicInteger completedTasks = new AtomicInteger(0);
        AtomicInteger failedTasks = new AtomicInteger(0);
        int totalTasks = urls.size();

        for (String targetUrl : urls) {
            swarmPool.execute(() -> {
                try {
                    String fileName = targetUrl.substring(targetUrl.lastIndexOf('/') + 1);
                    File outputFile = new File(cacheDir, fileName);
                    
                    URL url = new URL(targetUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "HK-Matrix-Swarm/1.0");

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream input = new BufferedInputStream(conn.getInputStream());
                        FileOutputStream output = new FileOutputStream(outputFile);
                        byte[] data = new byte[8192];
                        int count;
                        while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                        }
                        output.flush();
                        output.close();
                        input.close();
                        
                        postUpdate(callback, "[+] Swarm Locked: Payload '" + fileName + "' secured.");
                    } else {
                        failedTasks.incrementAndGet();
                        postUpdate(callback, "[-] Swarm Error: " + fileName + " unreachable.");
                    }
                } catch (Exception e) {
                    failedTasks.incrementAndGet();
                    postUpdate(callback, "[-] Swarm Exception on " + targetUrl + " : " + e.getMessage());
                } finally {
                    int done = completedTasks.incrementAndGet();
                    if (done == totalTasks) {
                        new Handler(Looper.getMainLooper()).post(() -> 
                            callback.onComplete(failedTasks.get() == 0)
                        );
                    }
                }
            });
        }
    }

    private static void postUpdate(FetchCallback cb, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onProgress(msg));
    }
}
