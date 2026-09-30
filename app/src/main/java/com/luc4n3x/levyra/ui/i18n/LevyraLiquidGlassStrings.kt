package com.luc4n3x.levyra.ui.i18n

internal val liquidGlassKeys = setOf(
    "liquidGlass",
    "liquidGlassSubtitle"
)

private fun liquidGlass(title: String, subtitle: String): Map<String, String> = mapOf(
    "liquidGlass" to title,
    "liquidGlassSubtitle" to subtitle
)

private val liquidGlassBundles = mapOf(
    "en" to liquidGlass("Liquid Glass", "Translucent blurred surfaces on player controls"),
    "it" to liquidGlass("Liquid Glass", "Superfici traslucide e sfocate sui controlli del player"),
    "es" to liquidGlass("Liquid Glass", "Superficies translúcidas y difuminadas en los controles del reproductor"),
    "fr" to liquidGlass("Liquid Glass", "Surfaces translucides et floutées sur les commandes du lecteur"),
    "de" to liquidGlass("Liquid Glass", "Durchscheinende, weichgezeichnete Flächen für die Player-Steuerung"),
    "pt" to liquidGlass("Liquid Glass", "Superfícies translúcidas e desfocadas nos controlos do leitor"),
    "nl" to liquidGlass("Liquid Glass", "Doorschijnende, vervaagde vlakken voor de spelerbediening"),
    "pl" to liquidGlass("Liquid Glass", "Półprzezroczyste, rozmyte powierzchnie przycisków odtwarzacza"),
    "ro" to liquidGlass("Liquid Glass", "Suprafețe translucide și estompate pe comenzile playerului"),
    "el" to liquidGlass("Liquid Glass", "Ημιδιαφανείς, θολές επιφάνειες στα χειριστήρια αναπαραγωγής"),
    "sv" to liquidGlass("Liquid Glass", "Genomskinliga, suddiga ytor på spelarens kontroller"),
    "da" to liquidGlass("Liquid Glass", "Gennemsigtige, slørede flader på afspillerens knapper"),
    "cs" to liquidGlass("Liquid Glass", "Průsvitné rozmazané plochy ovládacích prvků přehrávače"),
    "uk" to liquidGlass("Liquid Glass", "Напівпрозорі розмиті поверхні елементів керування плеєром"),
    "ru" to liquidGlass("Liquid Glass", "Полупрозрачные размытые поверхности элементов управления плеером"),
    "tr" to liquidGlass("Liquid Glass", "Oynatıcı kontrollerinde yarı saydam, bulanık yüzeyler"),
    "ar" to liquidGlass("Liquid Glass", "أسطح شفافة وضبابية لعناصر التحكم في المشغل"),
    "zh" to liquidGlass("Liquid Glass", "播放器控件使用半透明模糊表面"),
    "ja" to liquidGlass("Liquid Glass", "プレーヤーの操作ボタンに半透明のぼかし効果を使用"),
    "ko" to liquidGlass("Liquid Glass", "플레이어 컨트롤에 반투명 블러 표면 사용"),
    "hi" to liquidGlass("Liquid Glass", "प्लेयर कंट्रोल पर पारभासी धुंधली सतहें"),
    "id" to liquidGlass("Liquid Glass", "Permukaan tembus pandang dan buram pada kontrol pemutar"),
    "vi" to liquidGlass("Liquid Glass", "Bề mặt mờ trong suốt cho các nút điều khiển trình phát"),
    "th" to liquidGlass("Liquid Glass", "พื้นผิวโปร่งแสงแบบเบลอบนปุ่มควบคุมเครื่องเล่น"),
    "fil" to liquidGlass("Liquid Glass", "Malabo at translucent na surface sa mga kontrol ng player"),
    "he" to liquidGlass("Liquid Glass", "משטחים שקופים למחצה ומטושטשים בפקדי הנגן"),
    "sk" to liquidGlass("Liquid Glass", "Priesvitné rozmazané plochy ovládacích prvkov prehrávača"),
    "hr" to liquidGlass("Liquid Glass", "Prozirne zamućene površine na kontrolama playera"),
    "bg" to liquidGlass("Liquid Glass", "Полупрозрачни размазани повърхности на контролите на плейъра"),
    "hu" to liquidGlass("Liquid Glass", "Áttetsző, elmosott felületek a lejátszó vezérlőin"),
    "fi" to liquidGlass("Liquid Glass", "Läpikuultavat, sumeat pinnat soittimen säätimissä"),
    "et" to liquidGlass("Liquid Glass", "Poolläbipaistvad hägusad pinnad pleieri nuppudel"),
    "nb" to liquidGlass("Liquid Glass", "Gjennomsiktige, uskarpe flater på spillerkontrollene"),
    "ca" to liquidGlass("Liquid Glass", "Superfícies translúcides i difuminades als controls del reproductor"),
    "ms" to liquidGlass("Liquid Glass", "Permukaan lut sinar dan kabur pada kawalan pemain"),
    "fa" to liquidGlass("Liquid Glass", "سطوح نیمه‌شفاف و مات روی کنترل‌های پخش‌کننده"),
    "zh-Hant" to liquidGlass("Liquid Glass", "播放器控制項使用半透明模糊表面")
)

internal fun liquidGlassLocalizationCodes(): Set<String> = liquidGlassBundles.keys

internal fun liquidGlassLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(liquidGlassBundles, code)
