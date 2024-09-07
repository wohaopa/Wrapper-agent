package com.github.wohaopa.wrapper.ui.window;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class MultiThreadedDownloader {

    Proxy proxy = null;
    private final ListeningExecutorService executorService;

    private final JPanel panel;

    public MultiThreadedDownloader(int numberOfThreads) {
        this.executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numberOfThreads));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    }

    public void setProxy(String proxyHost, int proxyPort) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    }

    private ListenableFuture<File> downloadFile(String url, File destinationFile, JProgressBar progressBar) {
        return executorService.submit(() -> {
            HttpURLConnection connection = null;
            AtomicLong totalBytesRead = new AtomicLong(0);
            try {
                URL downloadUrl = new URL(url);
                if (proxy != null) connection = (HttpURLConnection) downloadUrl.openConnection(proxy);
                else connection = (HttpURLConnection) downloadUrl.openConnection();

                connection.setRequestMethod("GET");

                int fileSize = connection.getContentLength();

                SwingUtilities.invokeLater(() -> progressBar.setMaximum(fileSize));

                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream out = new FileOutputStream(destinationFile)) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead.addAndGet(bytesRead);

                        // 更新进度条
                        SwingUtilities.invokeLater(() -> { progressBar.setValue((int) totalBytesRead.get()); });
                    }
                }

                return destinationFile;
            } catch (IOException e) {
                throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public void addDownloadTask(String url, File destinationFile, CountDownLatch latch,
        Function<String, Void> callback) {
        if (!destinationFile.getParentFile()
            .exists())
            destinationFile.getParentFile()
                .mkdirs();
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        panel.add(new JLabel("Downloading: " + destinationFile.getName()));
        panel.add(progressBar);
        panel.revalidate();

        ListenableFuture<File> future = downloadFile(url, destinationFile, progressBar);

        future.addListener(() -> {
            try {
                File downloadedFile = future.get();
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("Completed");
                    progressBar.setValue(progressBar.getMaximum());
                });
                System.out.println("Download completed: " + downloadedFile.getAbsolutePath());
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("Failed");
                    progressBar.setForeground(Color.RED);
                });
                if (callback != null) callback.apply("Error during download: " + e.getMessage());
                System.err.println("Error during download: " + e.getMessage());
                destinationFile.delete();
            } finally {
                if (latch != null) latch.countDown();
            }
        }, Executors.newSingleThreadExecutor());
    }

    public JComponent getPane() {
        return panel;
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
