package org.schabi.newpipe.extractor;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StreamingServiceFetchDislikeTest {
    @Test
    void dislikeFetchingIsDisabledByDefault() {
        assertFalse(new YoutubeService(0).isFetchDislike());
    }

    @Test
    void dislikeFetchingRemainsAvailableAsExplicitOptIn() {
        final YoutubeService service = new YoutubeService(0);

        service.setFetchDislike(true);

        assertTrue(service.isFetchDislike());
    }
}
