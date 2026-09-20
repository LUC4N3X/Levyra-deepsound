package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.ReplayGainMode

internal data class ReplayGainCopy(
    val track: String,
    val album: String,
    val smart: String,
    val clippingProtection: String,
    val peakAware: String
) {
    fun modeLabel(mode: ReplayGainMode): String = when (mode) {
        ReplayGainMode.TRACK -> track
        ReplayGainMode.ALBUM -> album
        ReplayGainMode.SMART -> smart
        ReplayGainMode.OFF -> ""
    }
}

internal fun LevyraStrings.replayGainCopy(): ReplayGainCopy = when (code) {
    "it" -> ReplayGainCopy("Traccia", "Album", "Smart", "Protezione clipping", "Usa i picchi ReplayGain")
    "es" -> ReplayGainCopy("Pista", "Álbum", "Inteligente", "Protección contra clipping", "Usa los picos ReplayGain")
    "fr" -> ReplayGainCopy("Piste", "Album", "Intelligent", "Protection contre l’écrêtage", "Utilise les pics ReplayGain")
    "de" -> ReplayGainCopy("Titel", "Album", "Smart", "Übersteuerungsschutz", "Verwendet ReplayGain-Spitzenwerte")
    "pt" -> ReplayGainCopy("Faixa", "Álbum", "Inteligente", "Proteção contra clipping", "Usa os picos ReplayGain")
    "nl" -> ReplayGainCopy("Nummer", "Album", "Slim", "Clippingbeveiliging", "Gebruikt ReplayGain-piekwaarden")
    "pl" -> ReplayGainCopy("Utwór", "Album", "Inteligentny", "Ochrona przed przesterowaniem", "Używa wartości szczytowych ReplayGain")
    "ro" -> ReplayGainCopy("Piesă", "Album", "Inteligent", "Protecție la clipping", "Folosește valorile de vârf ReplayGain")
    "el" -> ReplayGainCopy("Κομμάτι", "Άλμπουμ", "Έξυπνο", "Προστασία από clipping", "Χρησιμοποιεί τις κορυφές ReplayGain")
    "sv" -> ReplayGainCopy("Spår", "Album", "Smart", "Klippskydd", "Använder ReplayGain-toppvärden")
    "da" -> ReplayGainCopy("Nummer", "Album", "Smart", "Beskyttelse mod clipping", "Bruger ReplayGain-spidsværdier")
    "cs" -> ReplayGainCopy("Skladba", "Album", "Chytré", "Ochrana proti přebuzení", "Používá špičkové hodnoty ReplayGain")
    "uk" -> ReplayGainCopy("Трек", "Альбом", "Розумний", "Захист від кліпінгу", "Використовує пікові значення ReplayGain")
    "ru" -> ReplayGainCopy("Трек", "Альбом", "Умный", "Защита от клиппинга", "Использует пиковые значения ReplayGain")
    "tr" -> ReplayGainCopy("Parça", "Albüm", "Akıllı", "Kırpılma koruması", "ReplayGain tepe değerlerini kullanır")
    "ar" -> ReplayGainCopy("المقطع", "الألبوم", "ذكي", "حماية من التشويش", "يستخدم قيم الذروة من ReplayGain")
    "zh" -> ReplayGainCopy("单曲", "专辑", "智能", "削波保护", "使用 ReplayGain 峰值数据")
    "ja" -> ReplayGainCopy("トラック", "アルバム", "スマート", "クリッピング保護", "ReplayGain のピーク値を使用")
    "ko" -> ReplayGainCopy("트랙", "앨범", "스마트", "클리핑 보호", "ReplayGain 피크 값을 사용")
    "hi" -> ReplayGainCopy("ट्रैक", "एल्बम", "स्मार्ट", "क्लिपिंग सुरक्षा", "ReplayGain पीक मान का उपयोग")
    "id" -> ReplayGainCopy("Lagu", "Album", "Cerdas", "Perlindungan clipping", "Menggunakan nilai puncak ReplayGain")
    "vi" -> ReplayGainCopy("Bài hát", "Album", "Thông minh", "Chống clipping", "Dùng giá trị đỉnh ReplayGain")
    "th" -> ReplayGainCopy("แทร็ก", "อัลบั้ม", "อัจฉริยะ", "ป้องกันคลิป", "ใช้ค่าพีก ReplayGain")
    "fil" -> ReplayGainCopy("Track", "Album", "Smart", "Proteksyon sa clipping", "Gumagamit ng ReplayGain peak values")
    "he" -> ReplayGainCopy("רצועה", "אלבום", "חכם", "הגנת clipping", "משתמש בערכי השיא של ReplayGain")
    else -> ReplayGainCopy("Track", "Album", "Smart", "Clipping protection", "Uses ReplayGain peak values")
}
