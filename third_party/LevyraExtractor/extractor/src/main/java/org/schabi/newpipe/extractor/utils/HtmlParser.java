package org.schabi.newpipe.extractor.utils;

public class HtmlParser {

    public static String htmlToString(String html) {
        if (html == null) {
            return null;
        }

        // Replace <br> and <br/> tags with \n
        String withNewLines = html.replaceAll("(?i)<br\\s*/?>", "\n");

        // Remove all other HTML tags

        return withNewLines.replaceAll("<[^>]*>", "");
    }
}
