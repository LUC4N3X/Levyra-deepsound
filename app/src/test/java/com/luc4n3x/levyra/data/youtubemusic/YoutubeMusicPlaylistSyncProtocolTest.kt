package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicPlaylistSyncProtocolTest {

    @Test
    fun editPlaylistSuccessRecognitionHandlesStatusAndResultShapes() {
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "STATUS_SUCCEEDED")
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "status_succeeded")
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "STATUS_SUCCESS")
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "SUCCESS")
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "STATUS_OK")
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "OK")
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "STATUS_COMPLETED")
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("playlistEditResults", JSONArray())
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("actions", JSONArray().put(JSONObject()))
            )
        )
        assertTrue(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("responseContext", JSONObject())
            )
        )
        assertFalse(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "STATUS_FAILED").put("playlistEditResults", JSONArray())
            )
        )
        assertFalse(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(
                JSONObject().put("status", "STATUS_ERROR")
            )
        )
        assertFalse(
            YoutubeMusicAccountClient.isEditPlaylistSuccessResponse(JSONObject())
        )
    }

    @Test
    fun extractCreatedPlaylistIdExtractsFromRootOrNestedResponse() {
        assertEquals(
            "PL123",
            YoutubeMusicAccountClient.extractCreatedPlaylistId(
                JSONObject().put("playlistId", "PL123")
            )
        )
        assertEquals(
            "PL456",
            YoutubeMusicAccountClient.extractCreatedPlaylistId(
                JSONObject().put("playlist", JSONObject().put("playlistId", "PL456"))
            )
        )
        assertEquals(
            "PL789",
            YoutubeMusicAccountClient.extractCreatedPlaylistId(
                JSONObject().put(
                    "actions",
                    JSONArray().put(
                        JSONObject().put(
                            "browseEndpoint",
                            JSONObject().put("browseId", "VLPL789")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun buildRemoveVideosPayloadEncodesActionRemoveVideoAndSetVideoId() {
        val payload = YoutubeMusicAccountClient.buildRemoveVideosPayload(
            playlistId = "VLPLabc123",
            items = listOf(
                RemotePlaylistRemoval(videoId = "vid1", setVideoId = "set1"),
                RemotePlaylistRemoval(videoId = "vid2", setVideoId = "set2")
            )
        )

        assertEquals("PLabc123", payload.getString("playlistId"))
        val actions = payload.getJSONArray("actions")
        assertEquals(2, actions.length())
        assertEquals("ACTION_REMOVE_VIDEO", actions.getJSONObject(0).getString("action"))
        assertEquals("vid1", actions.getJSONObject(0).getString("removedVideoId"))
        assertEquals("set1", actions.getJSONObject(0).getString("setVideoId"))
        assertEquals("ACTION_REMOVE_VIDEO", actions.getJSONObject(1).getString("action"))
        assertEquals("vid2", actions.getJSONObject(1).getString("removedVideoId"))
        assertEquals("set2", actions.getJSONObject(1).getString("setVideoId"))
    }

    @Test
    fun buildRenamePlaylistPayloadEncodesActionSetPlaylistName() {
        val longName = "A".repeat(200)
        val payload = YoutubeMusicAccountClient.buildRenamePlaylistPayload(
            playlistId = "VLPLxyz",
            title = "  $longName  "
        )

        assertEquals("PLxyz", payload.getString("playlistId"))
        val action = payload.getJSONArray("actions").getJSONObject(0)
        assertEquals("ACTION_SET_PLAYLIST_NAME", action.getString("action"))
        assertEquals(YoutubeMusicAccountClient.MAX_PLAYLIST_TITLE_LENGTH, action.getString("playlistName").length)
    }

    @Test
    fun parsePlaylistPageForSyncExtractsHeaderTitleDirectSetVideoIdAndMenuFallbackSetVideoId() {
        val directItem = JSONObject().put(
            "musicResponsiveListItemRenderer",
            JSONObject()
                .put(
                    "playlistItemData",
                    JSONObject()
                        .put("videoId", "vidDirect11")
                        .put("playlistSetVideoId", "setDirect11")
                )
                .put(
                    "flexColumns",
                    JSONArray()
                        .put(columnWithText("Direct Song"))
                        .put(columnWithText("Artist One"))
                )
        )

        val menuFallbackItem = JSONObject().put(
            "musicResponsiveListItemRenderer",
            JSONObject()
                .put(
                    "playlistItemData",
                    JSONObject().put("videoId", "vidMenu2222")
                )
                .put(
                    "menu",
                    JSONObject().put(
                        "menuRenderer",
                        JSONObject().put(
                            "items",
                            JSONArray().put(
                                JSONObject().put(
                                    "menuServiceItemRenderer",
                                    JSONObject().put(
                                        "serviceEndpoint",
                                        JSONObject().put(
                                            "playlistEditEndpoint",
                                            JSONObject().put(
                                                "actions",
                                                JSONArray().put(
                                                    JSONObject()
                                                        .put("action", "ACTION_REMOVE_VIDEO")
                                                        .put("setVideoId", "setMenu2222")
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                .put(
                    "flexColumns",
                    JSONArray()
                        .put(columnWithText("Fallback Song"))
                        .put(columnWithText("Artist Two"))
                )
        )

        val root = JSONObject()
            .put(
                "header",
                JSONObject().put(
                    "musicDetailHeaderRenderer",
                    JSONObject().put(
                        "title",
                        JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Evening Drive")))
                    )
                )
            )
            .put(
                "contents",
                JSONObject().put(
                    "musicPlaylistShelfRenderer",
                    JSONObject()
                        .put("contents", JSONArray().put(directItem).put(menuFallbackItem))
                        .put(
                            "continuations",
                            JSONArray().put(
                                JSONObject().put(
                                    "nextContinuationData",
                                    JSONObject().put("continuation", "CONT_TOKEN_1")
                                )
                            )
                        )
                )
            )

        val page = YoutubeMusicRepository().parsePlaylistPageForSync(root, "PLxyz")

        assertEquals("Evening Drive", page.title)
        assertEquals("CONT_TOKEN_1", page.continuation)
        assertEquals(2, page.items.size)
        assertEquals("vidDirect11", page.items[0].videoId)
        assertEquals("setDirect11", page.items[0].setVideoId)
        assertEquals("Direct Song", page.items[0].track.title)
        assertEquals("vidMenu2222", page.items[1].videoId)
        assertEquals("setMenu2222", page.items[1].setVideoId)
        assertEquals("Fallback Song", page.items[1].track.title)
    }

    private fun columnWithText(text: String): JSONObject = JSONObject().put(
        "musicResponsiveListItemFlexColumnRenderer",
        JSONObject().put(
            "text",
            JSONObject().put("runs", JSONArray().put(JSONObject().put("text", text)))
        )
    )
}
