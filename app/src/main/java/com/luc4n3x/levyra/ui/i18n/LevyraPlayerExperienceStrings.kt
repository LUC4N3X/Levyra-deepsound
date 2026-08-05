package com.luc4n3x.levyra.ui.i18n

private val playerExperienceKeys = listOf(
    "introHeadline",
    "introBody",
    "introFeatureSound",
    "introFeatureLyrics",
    "introFeatureOffline",
    "introStart",
    "expandPlayer",
    "collapsePlayer",
    "lyricsFocus"
)

private fun playerExperience(vararg values: String): Map<String, String> {
    require(values.size == playerExperienceKeys.size) {
        "Expected ${playerExperienceKeys.size} player experience strings, received ${values.size}"
    }
    return playerExperienceKeys.zip(values.asList()).toMap()
}

private val playerExperienceBundles: Map<String, Map<String, String>> = mapOf(
    "en" to playerExperience("Your sound, without limits.", "Millions of tracks, synced lyrics and offline listening in one player built to stay out of the way.", "Gapless playback and a full audio engine", "Synced lyrics, word by word", "Offline downloads with covers and tags", "Get started", "Expand player", "Collapse player", "Focus"),
    "it" to playerExperience("Il tuo suono, senza limiti.", "Milioni di brani, testi sincronizzati e ascolto offline in un player pensato per non intralciarti.", "Riproduzione gapless e motore audio completo", "Testi sincronizzati, parola per parola", "Download offline con copertine e tag", "Inizia", "Espandi il player", "Riduci il player", "Focus"),
    "es" to playerExperience("Tu sonido, sin límites.", "Millones de canciones, letras sincronizadas y escucha sin conexión en un reproductor que no estorba.", "Reproducción sin pausas y motor de audio completo", "Letras sincronizadas, palabra por palabra", "Descargas sin conexión con portadas y etiquetas", "Empezar", "Ampliar el reproductor", "Reducir el reproductor", "Enfoque"),
    "fr" to playerExperience("Votre son, sans limites.", "Des millions de titres, des paroles synchronisées et l'écoute hors connexion dans un lecteur qui se fait oublier.", "Lecture sans blanc et moteur audio complet", "Paroles synchronisées, mot à mot", "Téléchargements hors connexion avec pochettes et tags", "Commencer", "Agrandir le lecteur", "Réduire le lecteur", "Focus"),
    "de" to playerExperience("Dein Sound, ohne Grenzen.", "Millionen Titel, synchronisierte Liedtexte und Offline-Hören in einem Player, der dir nicht im Weg steht.", "Lückenlose Wiedergabe und vollständige Audio-Engine", "Synchronisierte Liedtexte, Wort für Wort", "Offline-Downloads mit Covern und Tags", "Loslegen", "Player vergrößern", "Player verkleinern", "Fokus"),
    "pt" to playerExperience("O teu som, sem limites.", "Milhões de faixas, letras sincronizadas e audição offline num player feito para não atrapalhar.", "Reprodução sem intervalos e motor de áudio completo", "Letras sincronizadas, palavra a palavra", "Transferências offline com capas e etiquetas", "Começar", "Expandir o player", "Reduzir o player", "Foco"),
    "nl" to playerExperience("Jouw geluid, zonder grenzen.", "Miljoenen nummers, gesynchroniseerde songteksten en offline luisteren in één speler die niet in de weg zit.", "Naadloos afspelen en een volledige audio-engine", "Gesynchroniseerde songteksten, woord voor woord", "Offline downloads met hoezen en tags", "Beginnen", "Speler vergroten", "Speler verkleinen", "Focus"),
    "pl" to playerExperience("Twoje brzmienie, bez ograniczeń.", "Miliony utworów, zsynchronizowane teksty i słuchanie offline w odtwarzaczu, który nie wchodzi w drogę.", "Odtwarzanie bez przerw i pełny silnik audio", "Zsynchronizowane teksty, słowo po słowie", "Pobieranie offline z okładkami i tagami", "Zaczynamy", "Powiększ odtwarzacz", "Zmniejsz odtwarzacz", "Skupienie"),
    "ro" to playerExperience("Sunetul tău, fără limite.", "Milioane de piese, versuri sincronizate și ascultare offline într-un player care nu îți stă în cale.", "Redare fără pauze și motor audio complet", "Versuri sincronizate, cuvânt cu cuvânt", "Descărcări offline cu coperți și etichete", "Începe", "Extinde playerul", "Restrânge playerul", "Focus"),
    "el" to playerExperience("Ο ήχος σου, χωρίς όρια.", "Εκατομμύρια κομμάτια, συγχρονισμένοι στίχοι και ακρόαση εκτός σύνδεσης σε ένα πρόγραμμα αναπαραγωγής που δεν σε εμποδίζει.", "Αναπαραγωγή χωρίς κενά και πλήρης μηχανή ήχου", "Συγχρονισμένοι στίχοι, λέξη προς λέξη", "Λήψεις εκτός σύνδεσης με εξώφυλλα και ετικέτες", "Ξεκίνα", "Μεγέθυνση αναπαραγωγής", "Σμίκρυνση αναπαραγωγής", "Εστίαση"),
    "sv" to playerExperience("Ditt ljud, utan gränser.", "Miljontals låtar, synkade låttexter och offlinelyssning i en spelare som håller sig ur vägen.", "Sömlös uppspelning och en komplett ljudmotor", "Synkade låttexter, ord för ord", "Offlinenedladdningar med omslag och taggar", "Kom igång", "Förstora spelaren", "Förminska spelaren", "Fokus"),
    "da" to playerExperience("Din lyd, uden grænser.", "Millioner af numre, synkroniserede sangtekster og offlinelytning i én afspiller, der holder sig af vejen.", "Sømløs afspilning og en komplet lydmotor", "Synkroniserede sangtekster, ord for ord", "Offlinedownloads med covers og tags", "Kom i gang", "Forstør afspilleren", "Formindsk afspilleren", "Fokus"),
    "cs" to playerExperience("Tvůj zvuk, bez hranic.", "Miliony skladeb, synchronizované texty a poslech offline v přehrávači, který ti nepřekáží.", "Přehrávání bez mezer a plnohodnotný zvukový engine", "Synchronizované texty, slovo po slovu", "Offline stahování s obaly a tagy", "Začít", "Zvětšit přehrávač", "Zmenšit přehrávač", "Soustředění"),
    "uk" to playerExperience("Твій звук, без меж.", "Мільйони треків, синхронізовані тексти й офлайн-прослуховування в плеєрі, який не заважає.", "Відтворення без пауз і повноцінний аудіорушій", "Синхронізовані тексти, слово за словом", "Офлайн-завантаження з обкладинками й тегами", "Почати", "Розгорнути плеєр", "Згорнути плеєр", "Фокус"),
    "ru" to playerExperience("Твой звук, без границ.", "Миллионы треков, синхронизированные тексты и офлайн-прослушивание в плеере, который не мешает.", "Воспроизведение без пауз и полноценный аудиодвижок", "Синхронизированные тексты, слово за словом", "Офлайн-загрузки с обложками и тегами", "Начать", "Развернуть плеер", "Свернуть плеер", "Фокус"),
    "tr" to playerExperience("Sesin, sınır tanımadan.", "Milyonlarca parça, senkron şarkı sözleri ve çevrimdışı dinleme; yolundan çekilmek için tasarlanmış tek bir oynatıcıda.", "Boşluksuz çalma ve eksiksiz ses motoru", "Senkron şarkı sözleri, kelime kelime", "Kapak ve etiketlerle çevrimdışı indirmeler", "Başla", "Oynatıcıyı büyüt", "Oynatıcıyı küçült", "Odak"),
    "ar" to playerExperience("صوتك، بلا حدود.", "ملايين المقطوعات وكلمات متزامنة واستماع دون اتصال في مشغّل صُمم ليبتعد عن طريقك.", "تشغيل بلا فواصل ومحرك صوت متكامل", "كلمات متزامنة، كلمة بكلمة", "تنزيلات دون اتصال مع الأغلفة والوسوم", "لنبدأ", "توسيع المشغّل", "تصغير المشغّل", "تركيز"),
    "zh" to playerExperience("你的声音，没有边界。", "数百万曲目、同步歌词与离线聆听，全部集中在一个不打扰你的播放器里。", "无缝播放与完整音频引擎", "逐字同步歌词", "带封面和标签的离线下载", "开始使用", "展开播放器", "收起播放器", "专注"),
    "ja" to playerExperience("あなたの音に、限界はない。", "数百万曲、同期歌詞、オフライン再生を、邪魔をしないひとつのプレーヤーに。", "ギャップレス再生と本格的なオーディオエンジン", "歌詞を一語ずつ同期表示", "カバーとタグ付きのオフラインダウンロード", "はじめる", "プレーヤーを拡大", "プレーヤーを縮小", "フォーカス"),
    "ko" to playerExperience("당신의 사운드, 한계 없이.", "수백만 곡, 동기화된 가사, 오프라인 감상을 방해 없는 하나의 플레이어에 담았습니다.", "무간격 재생과 완전한 오디오 엔진", "단어 단위로 동기화된 가사", "커버와 태그가 포함된 오프라인 다운로드", "시작하기", "플레이어 확대", "플레이어 축소", "집중"),
    "hi" to playerExperience("आपकी आवाज़, बिना सीमाओं के.", "लाखों ट्रैक, सिंक किए गए बोल और ऑफ़लाइन सुनना, एक ऐसे प्लेयर में जो रास्ते में नहीं आता.", "बिना अंतराल वाला प्लेबैक और पूरा ऑडियो इंजन", "शब्द दर शब्द सिंक किए गए बोल", "कवर और टैग के साथ ऑफ़लाइन डाउनलोड", "शुरू करें", "प्लेयर बड़ा करें", "प्लेयर छोटा करें", "फ़ोकस"),
    "id" to playerExperience("Suaramu, tanpa batas.", "Jutaan lagu, lirik tersinkron, dan mendengarkan offline dalam satu pemutar yang tidak mengganggu.", "Pemutaran tanpa jeda dan mesin audio lengkap", "Lirik tersinkron, kata demi kata", "Unduhan offline dengan sampul dan tag", "Mulai", "Perbesar pemutar", "Perkecil pemutar", "Fokus"),
    "vi" to playerExperience("Âm thanh của bạn, không giới hạn.", "Hàng triệu bài hát, lời bài hát đồng bộ và nghe ngoại tuyến trong một trình phát không làm phiền bạn.", "Phát liền mạch và động cơ âm thanh đầy đủ", "Lời bài hát đồng bộ, từng từ một", "Tải xuống ngoại tuyến kèm ảnh bìa và thẻ", "Bắt đầu", "Mở rộng trình phát", "Thu gọn trình phát", "Tập trung"),
    "th" to playerExperience("เสียงของคุณ ไร้ขีดจำกัด", "เพลงนับล้าน เนื้อเพลงแบบซิงก์ และการฟังออฟไลน์ในโปรแกรมเล่นเดียวที่ไม่รบกวนคุณ", "เล่นต่อเนื่องไร้รอยต่อพร้อมเอนจินเสียงเต็มรูปแบบ", "เนื้อเพลงซิงก์แบบคำต่อคำ", "ดาวน์โหลดออฟไลน์พร้อมปกและแท็ก", "เริ่มต้น", "ขยายโปรแกรมเล่น", "ย่อโปรแกรมเล่น", "โฟกัส"),
    "fil" to playerExperience("Ang tunog mo, walang hangganan.", "Milyon-milyong kanta, naka-sync na liriko, at offline na pakikinig sa isang player na hindi nakakaabala.", "Gapless na pag-playback at buong audio engine", "Naka-sync na liriko, salita bawat salita", "Mga offline na download na may cover at tag", "Magsimula", "Palakihin ang player", "Paliitin ang player", "Focus"),
    "he" to playerExperience("הצליל שלך, בלי גבולות.", "מיליוני רצועות, מילים מסונכרנות והאזנה לא מקוונת בנגן אחד שלא עומד בדרך.", "נגינה רצופה ומנוע שמע מלא", "מילים מסונכרנות, מילה אחר מילה", "הורדות לא מקוונות עם עטיפות ותגיות", "בואו נתחיל", "הגדלת הנגן", "הקטנת הנגן", "מיקוד")
)

internal fun playerExperienceLocalizationEntries(code: String): Map<String, String> =
    playerExperienceBundles.getValue(code)

internal fun playerExperienceLocalizationCodes(): Set<String> = playerExperienceBundles.keys
