package com.luc4n3x.levyra.data.youtubemusic

import org.json.JSONArray
import org.json.JSONObject

internal class YoutubeMusicSession(val account: YoutubeMusicAccount, val cookie: String) {
    override fun toString(): String = "YoutubeMusicSession(<redacted>)"
}

internal data class YoutubeMusicSavedSessions(
    val sessions: List<YoutubeMusicSession> = emptyList(),
    val activeId: String? = null
) {
    val active: YoutubeMusicSession? get() = sessions.firstOrNull { it.account.id == activeId }
    fun without(id: String) = copy(sessions = sessions.filterNot { it.account.id == id }, activeId = activeId.takeUnless { it == id })
    fun withLogin(incoming: List<YoutubeMusicSession>, active: String): YoutubeMusicSavedSessions {
        val ids = incoming.mapTo(hashSetOf()) { it.account.id }
        return YoutubeMusicSavedSessions((sessions.filterNot { it.account.id in ids } + incoming).takeLast(20), active)
    }
}

internal object YoutubeMusicSessionCodec {
    fun encode(state: YoutubeMusicSavedSessions): String = JSONObject().put("version", 1)
        .put("activeId", state.activeId).put("sessions", JSONArray().apply {
            state.sessions.forEach { session ->
                val account = session.account
                put(JSONObject().put("id", account.id).put("name", account.name).put("handle", account.handle)
                    .put("avatar", account.avatarUrl).put("pageId", account.pageId).put("authUser", account.authUser)
                    .put("selected", account.selected).put("cookie", session.cookie))
            }
        }).toString()

    fun decode(raw: String): YoutubeMusicSavedSessions {
        val root = JSONObject(raw)
        if (root.optInt("version") != 1) throw YoutubeMusicProtocolException()
        val array = root.getJSONArray("sessions")
        if (array.length() > 20) throw YoutubeMusicProtocolException()
        val sessions = (0 until array.length()).map { index ->
            val value = array.getJSONObject(index)
            val id = value.getString("id").takeIf(String::isNotBlank) ?: throw YoutubeMusicProtocolException()
            YoutubeMusicSession(
                YoutubeMusicAccount(id, value.getString("name"), value.optString("handle"), value.optString("avatar"),
                    value.optString("pageId").takeIf { it.isNotBlank() && it != "null" }, value.getInt("authUser"), value.optBoolean("selected")),
                value.getString("cookie")
            )
        }.distinctBy { it.account.id }
        val active = root.optString("activeId").takeIf { id -> sessions.any { it.account.id == id } }
        return YoutubeMusicSavedSessions(sessions, active)
    }
}
