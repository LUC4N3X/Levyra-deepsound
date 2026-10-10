package org.schabi.newpipe.extractor.exceptions;

public class VideoNotReleaseException extends ContentNotAvailableException{
    public VideoNotReleaseException(String message) {
        super(message);
    }

    public VideoNotReleaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
