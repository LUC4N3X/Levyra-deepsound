package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubePlayerDiscoveryTest {
    private final Downloader previous = NewPipe.getDownloader();

    @AfterEach
    void restore() {
        if (previous != null) NewPipe.init(previous);
    }

    @Test
    void embedFallbackDownloadsDiscoveredPlayerInsteadOfAnOldHash() throws Exception {
        for (final String variant : List.of("ias", "es6")) {
            final String path = "/s/player/abcdef12/player_" + variant + ".vflset/it_IT/base.js";
            final FixtureDownloader downloader = new FixtureDownloader(
                    "<script src=\"" + path + "\"></script>");
            NewPipe.init(downloader);

            assertEquals("player-code", YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode("video"));
            assertEquals(List.of("https://www.youtube.com/iframe_api",
                    "https://www.youtube.com/embed/video", "https://www.youtube.com" + path),
                    downloader.urls);
        }
    }

    @Test
    void embedJsonAcceptsBothPlayerVariantsAndExtractsTheHash() throws Exception {
        for (final String variant : List.of("ias", "es6")) {
            final FixtureDownloader downloader = new FixtureDownloader(
                    "<script>var config={\"jsUrl\":\"/s/player/abcdef12/player_" + variant
                            + ".vflset/en_US/base.js\"}</script>");
            NewPipe.init(downloader);

            assertEquals("abcdef12", YoutubeJavaScriptExtractor.extractPlayerId("video"));
        }
    }

    @Test
    void embedCannotRedirectPlayerDownloadToAnotherOrigin() {
        for (final String url : List.of("https://example.org/base.js",
                "https://www.youtube.com@127.0.0.1/base.js",
                "http://www.youtube.com/s/player/abcdef12/player_ias.vflset/en_US/base.js")) {
            final FixtureDownloader downloader = new FixtureDownloader(
                    "<script src=\"" + url + "\"></script>");
            NewPipe.init(downloader);

            assertThrows(ParsingException.class,
                    () -> YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode("video"));
            assertEquals(2, downloader.urls.size());
        }
    }

    private static final class FixtureDownloader extends Downloader {
        private final String embed;
        private final List<String> urls = new ArrayList<>();

        private FixtureDownloader(final String embed) {
            this.embed = embed;
        }

        @Override
        public Response execute(final Request request) throws IOException {
            urls.add(request.url());
            if (request.url().endsWith("iframe_api")) throw new IOException("fixture iframe offline");
            final String body = request.url().contains("/embed/") ? embed : "player-code";
            return new Response(200, "OK", Collections.emptyMap(), body, new byte[0], request.url());
        }

        @Override
        public CancellableCall executeAsync(final Request request, final AsyncCallback callback)
                throws IOException {
            throw new IOException("unused");
        }
    }
}
