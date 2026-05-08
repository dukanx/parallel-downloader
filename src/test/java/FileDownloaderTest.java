import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.example.Chunk;
import org.example.FileDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileDownloaderTest {

    private MockWebServer server;
    private FileDownloader downloader;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        downloader = new FileDownloader(4);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void testGetFileSize() throws Exception {
        server.enqueue(new MockResponse()
                .addHeader("Content-Length", "1000")
                .addHeader("Accept-Ranges", "bytes"));

        long size = downloader.getFileSize(server.url("/file.txt").toString());
        assertEquals(1000, size);
    }

    @Test
    void testBuildChunks() {
        List<Chunk> chunks = downloader.buildChunks(1000);
        assertEquals(4, chunks.size());
        assertEquals(0, chunks.get(0).start());
        assertEquals(249, chunks.get(0).end());
        assertEquals(250, chunks.get(1).start());
        assertEquals(999, chunks.get(3).end());
    }

    @Test
    void testFullDownload() throws Exception {
        byte[] content = "Hello, this is a test file content!".getBytes();

        server.enqueue(new MockResponse()
                .addHeader("Content-Length", String.valueOf(content.length))
                .addHeader("Accept-Ranges", "bytes"));

        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse()
                    .setResponseCode(206)
                    .setBody(new String(content)));
        }

        Path output = Files.createTempFile("test-download", ".txt");
        downloader.download(server.url("/file.txt").toString(), output.toString());

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);

        Files.deleteIfExists(output);
    }

    @Test
    void testRangeHeaderIsSent() throws Exception {
        byte[] content = "Test content".getBytes();

        server.enqueue(new MockResponse()
                .addHeader("Content-Length", String.valueOf(content.length))
                .addHeader("Accept-Ranges", "bytes"));

        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse()
                    .setResponseCode(206)
                    .setBody(new String(content)));
        }

        Path output = Files.createTempFile("test-download", ".txt");
        downloader.download(server.url("/file.txt").toString(), output.toString());

        server.takeRequest();

        RecordedRequest firstChunk = server.takeRequest();
        assertNotNull(firstChunk.getHeader("Range"));
        assertTrue(firstChunk.getHeader("Range").startsWith("bytes="));

        Files.deleteIfExists(output);
    }
}