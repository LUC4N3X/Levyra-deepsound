package com.luc4n3x.levyra.ui.i18n

internal val liveChatKeys = setOf("liveChat")

private fun liveChat(liveChat: String): Map<String, String> = mapOf(
    "liveChat" to liveChat
)

private val liveChatBundles: Map<String, Map<String, String>> = mapOf(
    "en" to liveChat("Live chat"),
    "it" to liveChat("Chat dal vivo"),
    "es" to liveChat("Chat en directo"),
    "fr" to liveChat("Chat en direct"),
    "de" to liveChat("Live-Chat"),
    "pt" to liveChat("Chat ao vivo"),
    "nl" to liveChat("Livechat"),
    "pl" to liveChat("Czat na żywo"),
    "ro" to liveChat("Chat live"),
    "el" to liveChat("Ζωντανή συνομιλία"),
    "sv" to liveChat("Livechatt"),
    "da" to liveChat("Livechat"),
    "cs" to liveChat("Živý chat"),
    "uk" to liveChat("Живий чат"),
    "ru" to liveChat("Живой чат"),
    "tr" to liveChat("Canlı sohbet"),
    "ar" to liveChat("الدردشة المباشرة"),
    "zh" to liveChat("实时聊天"),
    "ja" to liveChat("ライブチャット"),
    "ko" to liveChat("실시간 채팅"),
    "hi" to liveChat("लाइव चैट"),
    "id" to liveChat("Obrolan langsung"),
    "vi" to liveChat("Trò chuyện trực tiếp"),
    "th" to liveChat("แชทสด"),
    "fil" to liveChat("Live chat"),
    "he" to liveChat("צ'אט חי")
)

internal fun liveChatLocalizationEntries(code: String): Map<String, String> =
    liveChatBundles.getValue(code)

internal fun liveChatLocalizationCodes(): Set<String> = liveChatBundles.keys
