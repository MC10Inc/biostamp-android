package com.mc10inc.biostamp3.sdk.recording;

public class DownloadStatus {
    private int numPages;
    private boolean inProgress;
    private int downloadedPages;

    public DownloadStatus(int numPages, boolean inProgress, int downloadedPages) {
        this.numPages = numPages;
        this.inProgress = inProgress;
        this.downloadedPages = downloadedPages;
    }

    public int getDownloadedPages() {
        return downloadedPages;
    }

    public int getNumPages() {
        return numPages;
    }

    public boolean isComplete() {
        return !inProgress && downloadedPages == numPages;
    }
}