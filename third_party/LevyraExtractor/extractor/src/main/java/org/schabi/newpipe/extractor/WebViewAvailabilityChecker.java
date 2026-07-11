package org.schabi.newpipe.extractor;

import org.schabi.newpipe.extractor.exceptions.WebViewUnavailableException;

public interface WebViewAvailabilityChecker {
    void checkWebViewAvailable() throws WebViewUnavailableException;
}
