package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeChannelHelperResolutionTest {

    @Test
    void channelPathReturnsNonEmptyChannelId() throws Exception {
        final String channelId = "UCabcdefghijklmnopqrstuv";
        assertEquals(channelId, YoutubeChannelHelper.resolveChannelId("channel/" + channelId));
    }

    @Test
    void emptyChannelPathSegmentIsRejected() {
        assertThrows(ExtractionException.class,
                () -> YoutubeChannelHelper.resolveChannelId("channel/"));
        assertThrows(ExtractionException.class,
                () -> YoutubeChannelHelper.resolveChannelId("channel//ignored"));
    }
}
