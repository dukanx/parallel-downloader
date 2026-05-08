import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class FileDownloader {

    private final OkHttpClient client = new OkHttpClient();

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
}