package org.schabi.newpipe.extractor.exceptions;

public class WebViewUnavailableException extends ExtractionException {
    public WebViewUnavailableException(final String message) {
        super(message);
    }

    public WebViewUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
