package org.schabi.newpipe.extractor.services.youtube.sabr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SabrNodeFallback {
    static final int MAX_ALTERNATIVE_NODES = 3;
    private static final Pattern NODE_HOST =
            Pattern.compile("^rr(\\d{1,3})---([a-z0-9-]{1,63})\\.googlevideo\\.com$");
    private static final Pattern NODE_NAME = Pattern.compile("^[a-z0-9-]{1,63}$");
    private static final Pattern NODE_INDEX = Pattern.compile("^\\d{1,3}$");

    private SabrNodeFallback() {
    }

    static boolean isNodeUnreachable(@Nonnull final IOException error) {
        return error instanceof UnknownHostException
                || error instanceof ConnectException
                || error instanceof NoRouteToHostException;
    }

    @Nonnull
    static List<String> alternativeUrls(@Nonnull final String streamingUrl) {
        final URI uri;
        try {
            uri = URI.create(streamingUrl);
        } catch (final IllegalArgumentException error) {
            return Collections.emptyList();
        }
        final String host = uri.getHost();
        final String rawQuery = uri.getRawQuery();
        if (!"https".equals(uri.getScheme()) || host == null || rawQuery == null
                || !host.equals(uri.getRawAuthority())) {
            return Collections.emptyList();
        }
        final Matcher hostMatcher = NODE_HOST.matcher(host);
        final String prefix = "https://" + host;
        if (!hostMatcher.matches() || !streamingUrl.startsWith(prefix)) {
            return Collections.emptyList();
        }
        final String nodes = queryParameter(rawQuery, "mn");
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        final String mvi = queryParameter(rawQuery, "mvi");
        final String index = mvi != null && NODE_INDEX.matcher(mvi).matches()
                ? mvi : hostMatcher.group(1);
        final String suffix = streamingUrl.substring(prefix.length());
        final List<String> alternatives = new ArrayList<>();
        for (final String node : nodes.split(",")) {
            final String name = node.trim();
            if (!NODE_NAME.matcher(name).matches()) {
                continue;
            }
            final String candidateHost = "rr" + index + "---" + name + ".googlevideo.com";
            final String candidate = "https://" + candidateHost + suffix;
            if (!candidateHost.equals(host) && !alternatives.contains(candidate)) {
                alternatives.add(candidate);
                if (alternatives.size() == MAX_ALTERNATIVE_NODES) {
                    break;
                }
            }
        }
        return alternatives;
    }

    @Nullable
    static String hostOf(@Nonnull final String url) {
        try {
            return URI.create(url).getHost();
        } catch (final IllegalArgumentException error) {
            return null;
        }
    }

    @Nullable
    private static String queryParameter(@Nonnull final String rawQuery,
                                         @Nonnull final String name) {
        for (final String pair : rawQuery.split("&")) {
            final int separator = pair.indexOf('=');
            if (separator > 0 && name.equals(pair.substring(0, separator))) {
                try {
                    return URLDecoder.decode(pair.substring(separator + 1), "UTF-8");
                } catch (final UnsupportedEncodingException | IllegalArgumentException error) {
                    return null;
                }
            }
        }
        return null;
    }
}
