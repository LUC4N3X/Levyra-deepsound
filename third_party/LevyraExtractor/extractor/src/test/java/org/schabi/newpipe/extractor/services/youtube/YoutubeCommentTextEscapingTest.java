package org.schabi.newpipe.extractor.services.youtube;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.schabi.newpipe.extractor.utils.HtmlParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YoutubeCommentTextEscapingTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "I <3 this song",
            "hello <world>",
            "test <b> literal text",
            "2 < 3 and 5 > 4",
            "AT&T",
            "already escaped &amp; stays literal",
            "plain comment without markup"
    })
    void attributedCommentKeepsLiteralMarkupCharacters(final String text) {
        final JsonObject content = JsonObject.builder().value("content", text).done();

        assertEquals(text, HtmlParser.htmlToString(
                YoutubeDescriptionHelper.attributedDescriptionToHtml(content)));
    }

    @Test
    void attributedCommentStylesAreRenderedButStrippedToPlainText() {
        final String text = "bold <3 & plain";
        final JsonArray styleRuns = new JsonArray();
        styleRuns.add(JsonObject.builder()
                .value("startIndex", 0)
                .value("length", 4)
                .value("weightLabel", "FONT_WEIGHT_MEDIUM")
                .done());
        final JsonObject content = JsonObject.builder()
                .value("content", text)
                .value("styleRuns", styleRuns)
                .done();

        final String html = YoutubeDescriptionHelper.attributedDescriptionToHtml(content);

        assertEquals("<b>bold</b> &lt;3 &amp; plain", html);
        assertEquals(text, HtmlParser.htmlToString(html));
    }

    @Test
    void attributedCommentKeepsLineBreaks() {
        final JsonObject content = JsonObject.builder().value("content", "a <3\nb > c").done();

        assertEquals("a <3\nb > c", HtmlParser.htmlToString(
                YoutubeDescriptionHelper.attributedDescriptionToHtml(content)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"I <3 this song", "hello <world>", "AT&T", "2 < 3 and 5 > 4"})
    void legacySimpleTextCommentKeepsLiteralMarkupCharacters(final String text) throws Exception {
        final JsonObject simpleText = JsonObject.builder().value("simpleText", text).done();

        assertEquals(text, HtmlParser.htmlToString(
                YoutubeParsingHelper.getTextFromObject(simpleText, true)));
        assertEquals(text, YoutubeParsingHelper.getTextFromObject(simpleText, false));
    }

    @Test
    void legacyRunsCommentKeepsLiteralMarkupAndDropsFormattingTags() throws Exception {
        final JsonArray runs = new JsonArray();
        runs.add(JsonObject.builder().value("text", "hello <world> ").value("bold", true).done());
        runs.add(JsonObject.builder().value("text", "AT&T").done());
        final JsonObject textObject = JsonObject.builder().value("runs", runs).done();

        assertEquals("hello <world> AT&T", HtmlParser.htmlToString(
                YoutubeParsingHelper.getTextFromObject(textObject, true)));
    }
}
