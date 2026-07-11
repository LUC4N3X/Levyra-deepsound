package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.*;

public final class NiconicoDMCPayloadBuilder {
    private NiconicoDMCPayloadBuilder() {

    }

    public static String buildJSON(final JsonObject obj, final JsonObject encryption, String quality) throws JsonParserException {
        JsonStringWriter temp = JsonWriter.string()
                .object()
                .object("session")
                .value("recipe_id", obj.getString("recipeId"))
                .value("content_id", obj.getString("contentId"))
                .value("content_type", "movie")
                .array("content_src_id_sets")
                .object()
                .array("content_src_ids")
                .object()
                .object("src_id_to_mux")
                .array("video_src_ids", obj.getArray("videos")
                        .subList(obj.getArray("videos").indexOf(quality), obj.getArray("videos").size()))
                .array("audio_src_ids", obj.getArray("audios"))
                .end()
                .end()
                .end()
                .end()
                .end()
                .value("timing_constraint", "unlimited")
                .object("keep_method")
                .object("heartbeat")
                .value("lifetime", obj.getLong("heartbeatLifetime"))
                .end()
                .end()
                .object("protocol")
                .value("name", "http")
                .object("parameters")
                .object("http_parameters")
                .object("parameters")
                .object(obj.getArray("protocols").getString(0).equals("hls") ? "hls_parameters" : "http_output_download_parameters")
                .value("use_well_known_port", "yes")
                .value("use_ssl", "yes")
                .value("transfer_preset", "")
                .value("segment_duration", 6000);
        final JsonObject parsedToken = JsonParser.object().from(obj.getString("token"));
        if (parsedToken.containsKey("hls_encryption") && encryption != null) {
            temp = temp.object("encryption")
                    .object(parsedToken.getString("hls_encryption"))
                    .value("encrypted_key", encryption.getString("encryptedKey"))
                    .value("key_uri", encryption.getString("keyUri"))
                    .end().end();
        }
        return temp
                .end()
                .end()
                .end()
                .end()
                .end()
                .value("content_uri", "")
                .object("session_operation_auth")
                .object("session_operation_auth_by_signature")
                .value("token", obj.getString("token"))
                .value("signature", obj.getString("signature"))
                .end()
                .end()
                .object("content_auth")
                .value("auth_type",
                        obj.getObject("authTypes").getString(obj.getArray("protocols")
                                .getString(0)))
                .value("content_key_timeout", obj.getLong("contentKeyTimeout"))
                .value("service_id", "nicovideo")
                .value("service_user_id", obj.getString("serviceUserId"))
                .end()
                .object("client_info")
                .value("player_id", obj.getString("playerId"))
                .end()
                .value("priority", obj.getDouble("priority"))
                .end()
                .end()
                .done();
    }
}
