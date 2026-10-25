package com.luc4n3x.levyra.ui.i18n

internal val musicVideoPreferenceKeys = setOf(
    "musicVideoPreference",
    "musicVideoPreferenceSubtitle",
    "musicVideoAutomatic",
    "musicVideoPreferAudio",
    "musicVideoPreferVideo",
    "musicVideoRememberLast"
)

private fun musicVideo(vararg value: String): Map<String, String> {
    require(value.size == 6)
    return musicVideoPreferenceKeys.toList().zip(value.toList()).toMap()
}

private val musicVideoPreferenceBundles = mapOf(
    "en" to musicVideo(
        "Music videos", "Song version only when YouTube pairs it with the video; otherwise the video's own audio",
        "Automatic", "Prefer audio", "Prefer video", "Remember last mode"
    ),
    "it" to musicVideo(
        "Video musicali", "Versione brano solo se YouTube la abbina al video; altrimenti l'audio del video stesso",
        "Automatico", "Preferisci audio", "Preferisci video", "Ricorda l'ultima modalità"
    ),
    "es" to musicVideo(
        "Videos musicales", "Versión de canción solo si YouTube la vincula al video; si no, el audio del propio video",
        "Automático", "Preferir audio", "Preferir video", "Recordar el último modo"
    ),
    "fr" to musicVideo(
        "Clips musicaux", "Version titre seulement si YouTube l’associe au clip ; sinon l’audio du clip lui-même",
        "Automatique", "Préférer l’audio", "Préférer la vidéo", "Mémoriser le dernier mode"
    ),
    "de" to musicVideo(
        "Musikvideos", "Songversion nur, wenn YouTube sie dem Video zuordnet; sonst der Ton des Videos selbst",
        "Automatisch", "Audio bevorzugen", "Video bevorzugen", "Letzten Modus merken"
    ),
    "pt" to musicVideo(
        "Videoclipes", "Versão da música só quando o YouTube a associa ao vídeo; caso contrário, o áudio do próprio vídeo",
        "Automático", "Preferir áudio", "Preferir vídeo", "Lembrar o último modo"
    ),
    "nl" to musicVideo(
        "Muziekvideo's", "Nummerversie alleen als YouTube die aan de video koppelt; anders het geluid van de video zelf",
        "Automatisch", "Audio voorkeur", "Video voorkeur", "Laatste modus onthouden"
    ),
    "pl" to musicVideo(
        "Teledyski", "Wersja utworu tylko, gdy YouTube łączy ją z teledyskiem; w przeciwnym razie dźwięk samego wideo",
        "Automatycznie", "Preferuj audio", "Preferuj wideo", "Zapamiętaj ostatni tryb"
    ),
    "ro" to musicVideo(
        "Videoclipuri", "Versiunea piesei doar când YouTube o asociază cu videoclipul; altfel sunetul videoclipului",
        "Automat", "Preferă audio", "Preferă video", "Ține minte ultimul mod"
    ),
    "el" to musicVideo(
        "Μουσικά βίντεο", "Έκδοση τραγουδιού μόνο όταν το YouTube τη συνδέει με το βίντεο· αλλιώς ο ήχος του ίδιου του βίντεο",
        "Αυτόματο", "Προτίμηση ήχου", "Προτίμηση βίντεο", "Απομνημόνευση τελευταίας λειτουργίας"
    ),
    "sv" to musicVideo(
        "Musikvideor", "Låtversion bara när YouTube kopplar den till videon; annars videons eget ljud",
        "Automatiskt", "Föredra ljud", "Föredra video", "Kom ihåg senaste läget"
    ),
    "da" to musicVideo(
        "Musikvideoer", "Sangversion kun når YouTube parrer den med videoen; ellers videoens egen lyd",
        "Automatisk", "Foretræk lyd", "Foretræk video", "Husk seneste tilstand"
    ),
    "cs" to musicVideo(
        "Hudební videa", "Verze skladby jen tehdy, když ji YouTube spáruje s videem; jinak zvuk samotného videa",
        "Automaticky", "Upřednostnit zvuk", "Upřednostnit video", "Pamatovat poslední režim"
    ),
    "uk" to musicVideo(
        "Музичні кліпи", "Версія пісні лише тоді, коли YouTube пов’язує її з кліпом; інакше звук самого відео",
        "Автоматично", "Віддавати перевагу аудіо", "Віддавати перевагу відео", "Пам’ятати останній режим"
    ),
    "ru" to musicVideo(
        "Музыкальные клипы", "Версия песни, только если YouTube связывает её с клипом; иначе звук самого видео",
        "Автоматически", "Предпочитать аудио", "Предпочитать видео", "Запоминать последний режим"
    ),
    "tr" to musicVideo(
        "Müzik videoları", "Şarkı sürümü yalnızca YouTube onu videoyla eşleştirdiğinde; aksi halde videonun kendi sesi",
        "Otomatik", "Sesi tercih et", "Videoyu tercih et", "Son modu hatırla"
    ),
    "ar" to musicVideo(
        "الفيديوهات الموسيقية", "نسخة الأغنية فقط عندما يربطها YouTube بالفيديو؛ وإلا فصوت الفيديو نفسه",
        "تلقائي", "تفضيل الصوت", "تفضيل الفيديو", "تذكر آخر وضع"
    ),
    "zh" to musicVideo(
        "音乐视频", "仅当 YouTube 将歌曲版本与视频配对时使用歌曲版本；否则使用视频本身的音频",
        "自动", "优先音频", "优先视频", "记住上次模式"
    ),
    "ja" to musicVideo(
        "ミュージックビデオ", "YouTube が動画と曲版を関連付けている場合のみ曲版を使用し、それ以外は動画自体の音声を使用します",
        "自動", "音声を優先", "動画を優先", "前回のモードを記憶"
    ),
    "ko" to musicVideo(
        "뮤직비디오", "YouTube가 영상과 곡 버전을 연결한 경우에만 곡 버전을, 그렇지 않으면 영상 자체의 오디오를 사용합니다",
        "자동", "오디오 우선", "동영상 우선", "마지막 모드 기억"
    ),
    "hi" to musicVideo(
        "म्यूज़िक वीडियो", "गाने का संस्करण तभी जब YouTube उसे वीडियो से जोड़े; अन्यथा वीडियो का अपना ऑडियो",
        "स्वचालित", "ऑडियो को प्राथमिकता", "वीडियो को प्राथमिकता", "पिछला मोड याद रखें"
    ),
    "id" to musicVideo(
        "Video musik", "Versi lagu hanya jika YouTube memasangkannya dengan video; jika tidak, audio video itu sendiri",
        "Otomatis", "Utamakan audio", "Utamakan video", "Ingat mode terakhir"
    ),
    "vi" to musicVideo(
        "Video âm nhạc", "Chỉ dùng bản bài hát khi YouTube ghép nó với video; nếu không, dùng âm thanh của chính video",
        "Tự động", "Ưu tiên âm thanh", "Ưu tiên video", "Nhớ chế độ gần nhất"
    ),
    "th" to musicVideo(
        "มิวสิกวิดีโอ", "ใช้เวอร์ชันเพลงเฉพาะเมื่อ YouTube จับคู่กับวิดีโอ มิฉะนั้นใช้เสียงของวิดีโอเอง",
        "อัตโนมัติ", "เน้นเสียง", "เน้นวิดีโอ", "จำโหมดล่าสุด"
    ),
    "fil" to musicVideo(
        "Mga music video", "Bersyon ng kanta lang kapag ipinares ito ng YouTube sa video; kung hindi, ang audio ng mismong video",
        "Awtomatiko", "Mas gusto ang audio", "Mas gusto ang video", "Tandaan ang huling mode"
    ),
    "he" to musicVideo(
        "קליפים", "גרסת השיר רק כש-YouTube מצמידה אותה לסרטון; אחרת השמע של הסרטון עצמו",
        "אוטומטי", "העדפת שמע", "העדפת וידאו", "זכירת המצב האחרון"
    ),
    "sk" to musicVideo(
        "Hudobné videá", "Verzia skladby len vtedy, keď ju YouTube spáruje s videom; inak zvuk samotného videa",
        "Automaticky", "Uprednostniť zvuk", "Uprednostniť video", "Zapamätať posledný režim"
    ),
    "bn" to musicVideo(
        "মিউজিক ভিডিও", "YouTube ভিডিওর সঙ্গে গানের সংস্করণ জুড়লে তবেই সেটি; নইলে ভিডিওর নিজের অডিও",
        "স্বয়ংক্রিয়", "অডিও অগ্রাধিকার", "ভিডিও অগ্রাধিকার", "শেষ মোড মনে রাখুন"
    ),
    "kk" to musicVideo(
        "Музыкалық бейнелер", "Ән нұсқасы YouTube оны бейнемен байланыстырғанда ғана; әйтпесе бейненің өз дыбысы",
        "Автоматты", "Аудиоға басымдық", "Бейнеге басымдық", "Соңғы режимді есте сақтау"
    ),
    "sr" to musicVideo(
        "Muzički spotovi", "Verzija pesme samo kada je YouTube upari sa spotom; inače zvuk samog videa",
        "Automatski", "Prednost zvuku", "Prednost videu", "Zapamti poslednji režim"
    ),
    "sq" to musicVideo(
        "Videoklipe", "Versioni i këngës vetëm kur YouTube e çifton me videon; përndryshe audioja e vetë videos",
        "Automatik", "Prefero audion", "Prefero videon", "Mbaj mend modalitetin e fundit"
    ),
    "hr" to musicVideo(
        "Glazbeni spotovi", "Verzija pjesme samo kad je YouTube upari sa spotom; inače zvuk samog videa",
        "Automatski", "Prednost zvuku", "Prednost videu", "Zapamti zadnji način"
    ),
    "bg" to musicVideo(
        "Музикални клипове", "Версия на песента само когато YouTube я свързва с клипа; иначе звукът на самото видео",
        "Автоматично", "Предпочитане на аудио", "Предпочитане на видео", "Запомняне на последния режим"
    ),
    "hu" to musicVideo(
        "Videoklipek", "Dalverzió csak akkor, ha a YouTube összepárosítja a videóval; különben a videó saját hangja",
        "Automatikus", "Hang előnyben", "Videó előnyben", "Utolsó mód megjegyzése"
    ),
    "fi" to musicVideo(
        "Musiikkivideot", "Kappaleversio vain, kun YouTube yhdistää sen videoon; muuten videon oma ääni",
        "Automaattinen", "Suosi ääntä", "Suosi videota", "Muista viimeisin tila"
    ),
    "et" to musicVideo(
        "Muusikavideod", "Looversioon ainult siis, kui YouTube seob selle videoga; muidu video enda heli",
        "Automaatne", "Eelista heli", "Eelista videot", "Jäta viimane režiim meelde"
    ),
    "nb" to musicVideo(
        "Musikkvideoer", "Sangversjon bare når YouTube kobler den til videoen; ellers videoens egen lyd",
        "Automatisk", "Foretrekk lyd", "Foretrekk video", "Husk siste modus"
    ),
    "ca" to musicVideo(
        "Videoclips", "Versió de la cançó només si YouTube l'associa al vídeo; si no, l'àudio del mateix vídeo",
        "Automàtic", "Prefereix l'àudio", "Prefereix el vídeo", "Recorda l'últim mode"
    ),
    "ms" to musicVideo(
        "Video muzik", "Versi lagu hanya apabila YouTube memadankannya dengan video; jika tidak, audio video itu sendiri",
        "Automatik", "Utamakan audio", "Utamakan video", "Ingat mod terakhir"
    ),
    "fa" to musicVideo(
        "موزیک‌ویدیوها", "نسخه آهنگ فقط وقتی YouTube آن را با ویدیو جفت کند؛ در غیر این صورت صدای خود ویدیو",
        "خودکار", "ترجیح صدا", "ترجیح ویدیو", "به خاطر سپردن آخرین حالت"
    ),
    "zh-Hant" to musicVideo(
        "音樂影片", "僅在 YouTube 將歌曲版本與影片配對時使用歌曲版本；否則使用影片本身的音訊",
        "自動", "優先音訊", "優先影片", "記住上次模式"
    )
)

internal fun musicVideoPreferenceLocalizationCodes(): Set<String> = musicVideoPreferenceBundles.keys

internal fun musicVideoPreferenceLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(musicVideoPreferenceBundles, code)
