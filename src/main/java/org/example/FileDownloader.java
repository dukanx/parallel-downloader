package org.example;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileDownloader {

    private final OkHttpClient client = new OkHttpClient();
    private final int numChunks;

    public FileDownloader(int numChunks) {
        this.numChunks = numChunks;
    }

    public long getFileSize(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HEAD request failed: " + response.code());
            }
            String contentLength = response.header("Content-Length");
            if (contentLength == null) {
                throw new IOException("Server did not return Content-Length");
            }
            return Long.parseLong(contentLength);
        }
    }

    public List<Chunk> buildChunks(long fileSize) {
        List<Chunk> chunks = new ArrayList<>();
        long chunkSize = fileSize / numChunks;

        for (int i = 0; i < numChunks; i++) {
            long start = (long) i * chunkSize;
            long end = (i == numChunks - 1) ? fileSize - 1 : start + chunkSize - 1;
            chunks.add(new Chunk(i, start, end));
        }
        return chunks;
    }

    public byte[] downloadChunk(String url, Chunk chunk) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Range", "bytes=" + chunk.start() + "-" + chunk.end())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 206) {
                throw new IOException("Chunk download failed: " + response.code());
            }
            return response.body().bytes();
        }
    }

    public void download(String url, String outputPath) throws IOException, InterruptedException, ExecutionException {
        long fileSize = getFileSize(url);
        List<Chunk> chunks = buildChunks(fileSize);

        ExecutorService executor = Executors.newFixedThreadPool(numChunks);
        List<Future<byte[]>> futures = new ArrayList<>();

        for (Chunk chunk : chunks) {
            Future<byte[]> future = executor.submit(() -> downloadChunk(url, chunk));
            futures.add(future);
        }

        try (RandomAccessFile raf = new RandomAccessFile(outputPath, "rw")) {
            raf.setLength(fileSize);

            for (int i = 0; i < chunks.size(); i++) {
                byte[] data = futures.get(i).get();
                raf.seek(chunks.get(i).start());
                raf.write(data);
            }
        }

        executor.shutdown();
    }
}