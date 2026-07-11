package org.schabi.newpipe.extractor.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import static java.util.Collections.singletonList;
import static org.schabi.newpipe.extractor.NewPipe.getDownloader;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.*;

public class SubtitleDeduplicator {

    private static final Pattern pattern = Pattern.compile(
            "<p[^>]*begin=\"([^\"]+)\"[^>]*end=\"([^\"]+)\"[^>]*>(.*?)</p>",
            Pattern.DOTALL
    );

    public static boolean hasDuplicateEntries(String remoteSubtitleUrl) {
        return containsDuplicatedEntries(downloadRemoteText(remoteSubtitleUrl));
    }

    public static boolean containsDuplicatedEntries(String subtitleContent) {
        if (StringUtils.isEmpty(subtitleContent)) {
            return false;
        }

        Matcher matcher = pattern.matcher(subtitleContent);
        Set<String> seen = new HashSet<>();
        int counter = 0;
        final int MAX_CHECK = 10;
        while (matcher.find() && counter < MAX_CHECK) {
            String key = getSubtitleKeyOfTtml(matcher);

            if (seen.contains(key)) {
                return true;
            }
            seen.add(key);
            counter++;
        }

        return false;
    }

    public static String getDeduplicatedContent(String remoteSubtitleUrl) {
        String subtitleContent = downloadRemoteText(remoteSubtitleUrl);
        if (StringUtils.isEmpty(subtitleContent)) {
            return subtitleContent;
        }

        Matcher matcher = pattern.matcher(subtitleContent);

        Set<String> seen = new HashSet<>();
        StringBuilder result = new StringBuilder();

        int lastIndex = 0;
        while (matcher.find()) {
            result.append(subtitleContent, lastIndex, matcher.start());

            String key = getSubtitleKeyOfTtml(matcher);

            if (!seen.contains(key)) {
                result.append(matcher.group(0));
                seen.add(key);
            }

            lastIndex = matcher.end();
        }

        result.append(subtitleContent.substring(lastIndex));
        return result.toString();
    }

    private static String downloadRemoteText(String urlStr) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept", singletonList("text/*"));
        headers.put("Accept-Language", singletonList("en-US,en;q=0.9"));
        try {
            if (urlStr.contains("tlang=")) { // auto_translated subtitles have risk control
                addLoggedInHeaders(headers);
            }
            return getDownloader().get(urlStr, headers).responseBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getSubtitleKeyOfTtml(Matcher matcher) {
        String begin = matcher.group(1).trim();
        String end = matcher.group(2).trim();
        String content = matcher.group(3).trim().replaceAll("\\s+", " ");
        return begin + "|" + end + "|" + content;
    }

    public static String getValidSubtitleContent(String remoteSubtitleUrl) {
        String subtitleContent = downloadRemoteText(remoteSubtitleUrl);
        if (StringUtils.isEmpty(subtitleContent)) {
            return null;
        }

        Matcher matcher = pattern.matcher(subtitleContent);
        int pTagCount = 0;

        while (matcher.find()) {
            pTagCount++;
            if (pTagCount > 1) {
                return subtitleContent;
            }
        }
        return null;
    }
}
