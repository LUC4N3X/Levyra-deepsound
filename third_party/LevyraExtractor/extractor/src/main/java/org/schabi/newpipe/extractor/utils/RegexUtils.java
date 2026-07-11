package org.schabi.newpipe.extractor.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
    public static String extract(String input, String regex) {
        // Define the pattern to extract the desired string
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // Find the matching pattern
        if (matcher.find()) {
            return matcher.group(0);
        } else {
            return null;
        }
    }
}
