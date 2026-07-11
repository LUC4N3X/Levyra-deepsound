package org.schabi.newpipe.extractor.services.niconico;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class M3U8Parser {

    public static Map<String, List<String>> parseMasterM3U8(String masterContent, String cookies, long length) {
        cookies = URLEncoder.encode(cookies);
        Map<String, List<String>> parsedContent = new HashMap<>();

        // Pattern to match audio playlist
        Pattern audioPattern = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO.*?URI=\"(.*?)\"");
        // Pattern to match video playlist with resolution
        Pattern videoPattern = Pattern.compile("https://[^\"]+\\.m3u8\\?[^\\s\"]+(?=#)");

        Matcher audioMatcher = audioPattern.matcher(masterContent);
        // Find and add audio playlists
        while (audioMatcher.find()) {
            String audioUrl = audioMatcher.group(1);
            parsedContent.computeIfAbsent("audio", k -> new ArrayList<>()).add(audioUrl + "#cookie=" + cookies + "&length=" + length);
        }

        Matcher videoMatcher = videoPattern.matcher(masterContent);
        // add all the videos to the list
        while (videoMatcher.find()) {
            String videoUrl = videoMatcher.group();
            parsedContent.computeIfAbsent("video", k -> new ArrayList<>()).add(videoUrl + "#cookie=" + cookies + "&length=" + length);
        }
        return parsedContent;
    }
}
