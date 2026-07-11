package org.schabi.newpipe.extractor.services.niconico.extractors;

import org.apache.commons.lang3.StringUtils;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.grack.nanojson.JsonObject;

public class NiconicoSearchContentItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject item;

    public NiconicoSearchContentItemExtractor(JsonObject item) {
        this.item = item;
    }

    @Override
    public String getName() throws ParsingException {
        return item.getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return NiconicoService.BASE_URL + "/watch/" + item.getString("id");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        JsonObject thumbnail = item.getObject("thumbnail");
        if (thumbnail != null) {
            String url = thumbnail.getString("url");
            if (url != null && !url.isEmpty()) {
                return url;
            }
            url = thumbnail.getString("listingUrl");
            if (url != null && !url.isEmpty()) {
                return url;
            }
            url = thumbnail.getString("nHdUrl");
            if (url != null && !url.isEmpty()) {
                return url;
            }
        }
        return "";
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public boolean isAd() throws ParsingException {
        return false;
    }

    @Override
    public long getDuration() throws ParsingException {
        return item.getLong("duration");
    }

    @Override
    public long getViewCount() throws ParsingException {
        JsonObject count = item.getObject("count");
        if (count != null) {
            return count.getLong("view");
        }
        return 0;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        JsonObject owner = item.getObject("owner");
        if (owner != null) {
            return owner.getString("name");
        }
        return "";
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        JsonObject owner = item.getObject("owner");
        if (owner != null) {
            String ownerType = owner.getString("ownerType");
            String id = owner.getString("id");
            if (ownerType.equals("user")) {
                return NiconicoService.BASE_URL + "/user/" + id;
            } else if (ownerType.equals("channel")) {
                return NiconicoService.BASE_URL + "/channel/" + id;
            }
        }
        return "";
    }

    @Nullable
    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        JsonObject owner = item.getObject("owner");
        if (owner != null) {
            return owner.getString("iconUrl");
        }
        return null;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        String registeredAt = item.getString("registeredAt");
        if (registeredAt != null) {
            // Format: "2025-08-23T00:10:05+09:00"
            return registeredAt.substring(0, 10).replace("-", "/") +
                    " " + registeredAt.substring(11, 16);
        }
        return null;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        String registeredAt = item.getString("registeredAt");
        if (registeredAt != null) {
            return new DateWrapper(LocalDateTime.parse(
                            registeredAt.substring(0, 19),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    .atOffset(ZoneOffset.ofHours(9)));
        }
        return null;
    }

    @Override
    public boolean requiresMembership() throws ParsingException {
        return item.getBoolean("isPaymentRequired", false);
    }
}
