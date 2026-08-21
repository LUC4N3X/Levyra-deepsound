package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.Page;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubePlaylistContinuationContextTest {
    @Test
    void pageCarriesCourseAndUploaderContextAcrossFreshExtractorInstances() throws Exception {
        final Page page = new Page(
                "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false",
                "true",
                Arrays.asList("Course Artist", "https://www.youtube.com/@course"),
                null,
                "{}".getBytes(StandardCharsets.UTF_8)
        );
        final Method method = YoutubePlaylistExtractor.class
                .getDeclaredMethod("contextFromPage", Page.class);
        method.setAccessible(true);
        final Object context = method.invoke(null, page);

        assertEquals(true, field(context, "coursePlaylist"));
        assertEquals("Course Artist", field(context, "uploaderName"));
        assertEquals("https://www.youtube.com/@course", field(context, "uploaderUrl"));
    }

    @Test
    void courseMarkerDetectionWorksAtNestedContinuationDepth() throws Exception {
        final JsonObject json = JsonParser.object().from("{\"a\":[{\"b\":{"
                + "\"tag\":\"engagement-panel-course-metadata\"}}]}");
        final Method method = YoutubePlaylistExtractor.class
                .getDeclaredMethod("containsJsonValue", Object.class, String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(
                null, json, "engagement-panel-course-metadata"));
    }

    private static Object field(final Object target, final String name) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
