package com.luc4n3x.levyra.ui.i18n

import java.util.Locale

private val trendingSongsForYouTitles = setOf(
    "Trending songs for you", "Brani di tendenza per te", "Canciones en tendencia para ti", "Músicas em alta para você",
    "Titres tendance pour vous", "Angesagte Songs für dich", "Trending nummers voor jou", "Melodii în tendințe pentru tine",
    "Ανερχόμενα τραγούδια για εσάς", "Trendande låtar för dig", "Populære sange til dig", "Populární skladby pro vás",
    "Populárne skladby pre vás", "Aktualne pjesme za vas", "Песме у тренду за вас", "Këngët në tendencë për ty",
    "Felkapott zeneszámok neked", "Trendaavia kappaleita sinulle", "Populaarsust koguvad lood teile", "Sanger på vei opp – for deg",
    "Tendències en cançons per a tu", "Сізге арналған танымал әндер", "Sizin için seçilen trend şarkılar", "أغانٍ رائجة مقترَحة لك",
    "آهنگ‌های پرطرفدار برای شما", "为你推荐的热门歌曲", "おすすめの話題の曲", "나만을 위한 인기 급상승 곡", "आपके लिए ट्रेंडिंग गाने",
    "আপনার জন্য জনপ্রিয় গান", "Lagu trending untuk kamu", "Lagu sohor kini untuk anda", "Bài hát thịnh hành dành cho bạn",
    "เพลงมาแรงสำหรับคุณ", "שירים פופולריים בשבילך", "Mga trending na kanta para sa iyo"
).mapTo(HashSet()) { it.lowercase(Locale.ROOT) }

internal fun isTrendingSongsForYouTitle(title: String): Boolean = title.trim().lowercase(Locale.ROOT) in trendingSongsForYouTitles
