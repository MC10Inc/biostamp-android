package com.mc10inc.biostamp3.sdk.recording;

/**
 * Status of a downloaded recording in the database.
 */
public class DownloadStatus {
    private int numPages;
    private boolean inProgress;
    private int downloadedPages;

    public DownloadStatus(int numPages, boolean inProgress, int downloadedPages) {
        this.numPages = numPages;
        this.inProgress = inProgress;
        this.downloadedPages = downloadedPages;
    }

    /**
     * Get the number of pages downloaded so far.
     * <p/>
     * Divide by {@link #getNumPages()} to get the fraction of the recording that has been
     * downloaded.
     *
     * @return Number of pages from this recording in the database
     */
    public int getDownloadedPages() {
        return downloadedPages;
    }

    /**
     * Get the size of the recording.
     * <p/>
     * The size of the recording is given in units of 'pages' which correspond to a fixed amount of
     * space in the sensor's flash memory.
     *
     * @return Size of the recording in pages
     */
    public int getNumPages() {
        return numPages;
    }

    /**
     * Check if the download is complete.
     * <p/>
     * If the download is complete then the recording may be decoded. If the download was
     * interrupted and is not complete, it is possible to restart the download which will
     * automatically resume where it had stopped.
     *
     * @return True if download is complete.
     */
    public boolean isComplete() {
        return !inProgress && downloadedPages == numPages;
    }
}