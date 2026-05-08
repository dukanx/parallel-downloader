package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {
        FileDownloader downloader = new FileDownloader(4);
        downloader.download("http://localhost:8080/test.txt", "output.txt");
        System.out.println("Download complete!");
    }
}