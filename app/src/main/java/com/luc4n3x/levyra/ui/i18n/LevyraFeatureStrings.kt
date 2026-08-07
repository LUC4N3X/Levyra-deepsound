package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraAudioPresets

data class PlaylistImportCopy(
    val title: String,
    val subtitle: String,
    val action: String,
    val body: String,
    val placeholder: String,
    val note: String,
    val cancel: String
)

fun LevyraStrings.playlistImportCopy(): PlaylistImportCopy = when (code) {
    "it" -> PlaylistImportCopy(
        "Importa playlist",
        "Da servizi musicali supportati e backup compatibili",
        "Importa",
        "Incolla il link di una playlist da un servizio musicale supportato oppure il contenuto di un backup compatibile. Levyra riconoscerà i brani e creerà una playlist nella tua libreria.",
        "Incolla link o contenuto del backup…",
        "I brani importati vengono associati al catalogo Levyra per mantenere la riproduzione affidabile.",
        "Annulla"
    )
    "es" -> PlaylistImportCopy(
        "Importar playlist",
        "Desde servicios de música compatibles y copias de seguridad compatibles",
        "Importar",
        "Pega el enlace de una playlist de un servicio de música compatible o el contenido de una copia de seguridad compatible. Levyra identificará las canciones y creará una playlist en tu biblioteca.",
        "Pega el enlace o el contenido de la copia…",
        "Las canciones importadas se vinculan con el catálogo de Levyra para mantener una reproducción fiable.",
        "Cancelar"
    )
    "fr" -> PlaylistImportCopy(
        "Importer une playlist",
        "Depuis les services musicaux pris en charge et les sauvegardes compatibles",
        "Importer",
        "Collez le lien d’une playlist provenant d’un service musical pris en charge ou le contenu d’une sauvegarde compatible. Levyra identifiera les titres et créera une playlist dans votre bibliothèque.",
        "Collez le lien ou le contenu de la sauvegarde…",
        "Les titres importés sont associés au catalogue Levyra afin de garantir une lecture fiable.",
        "Annuler"
    )
    "de" -> PlaylistImportCopy(
        "Playlist importieren",
        "Aus unterstützten Musikdiensten und kompatiblen Sicherungen",
        "Importieren",
        "Füge den Link zu einer Playlist aus einem unterstützten Musikdienst oder den Inhalt einer kompatiblen Sicherung ein. Levyra erkennt die Titel und erstellt eine Playlist in deiner Mediathek.",
        "Link oder Sicherungsinhalt einfügen…",
        "Importierte Titel werden dem Levyra-Katalog zugeordnet, damit die Wiedergabe zuverlässig bleibt.",
        "Abbrechen"
    )
    "pt" -> PlaylistImportCopy(
        "Importar playlist",
        "A partir de serviços de música suportados e cópias de segurança compatíveis",
        "Importar",
        "Cole o link de uma playlist de um serviço de música suportado ou o conteúdo de uma cópia de segurança compatível. A Levyra irá identificar as faixas e criar uma playlist na sua biblioteca.",
        "Cole o link ou o conteúdo da cópia…",
        "As faixas importadas são associadas ao catálogo da Levyra para manter uma reprodução fiável.",
        "Cancelar"
    )
    "nl" -> PlaylistImportCopy(
        "Playlist importeren",
        "Van ondersteunde muziekdiensten en compatibele back-ups",
        "Importeren",
        "Plak de link naar een playlist van een ondersteunde muziekdienst of de inhoud van een compatibele back-up. Levyra herkent de nummers en maakt een playlist in je bibliotheek.",
        "Plak een link of back-upinhoud…",
        "Geïmporteerde nummers worden gekoppeld aan de Levyra-catalogus voor betrouwbare weergave.",
        "Annuleren"
    )
    "pl" -> PlaylistImportCopy(
        "Importuj playlistę",
        "Z obsługiwanych serwisów muzycznych i zgodnych kopii zapasowych",
        "Importuj",
        "Wklej link do playlisty z obsługiwanego serwisu muzycznego albo zawartość zgodnej kopii zapasowej. Levyra rozpozna utwory i utworzy playlistę w Twojej bibliotece.",
        "Wklej link lub zawartość kopii…",
        "Zaimportowane utwory są dopasowywane do katalogu Levyra, aby zapewnić niezawodne odtwarzanie.",
        "Anuluj"
    )
    "ro" -> PlaylistImportCopy(
        "Importă playlistul",
        "Din servicii muzicale acceptate și copii de siguranță compatibile",
        "Importă",
        "Lipește linkul unui playlist dintr-un serviciu muzical acceptat sau conținutul unei copii de siguranță compatibile. Levyra va identifica piesele și va crea un playlist în biblioteca ta.",
        "Lipește linkul sau conținutul copiei…",
        "Piesele importate sunt asociate cu catalogul Levyra pentru o redare fiabilă.",
        "Anulează"
    )
    "el" -> PlaylistImportCopy(
        "Εισαγωγή playlist",
        "Από υποστηριζόμενες υπηρεσίες μουσικής και συμβατά αντίγραφα ασφαλείας",
        "Εισαγωγή",
        "Επικόλλησε τον σύνδεσμο μιας playlist από υποστηριζόμενη υπηρεσία μουσικής ή το περιεχόμενο ενός συμβατού αντιγράφου ασφαλείας. Το Levyra θα αναγνωρίσει τα κομμάτια και θα δημιουργήσει μια playlist στη βιβλιοθήκη σου.",
        "Επικόλλησε σύνδεσμο ή περιεχόμενο αντιγράφου…",
        "Τα εισαγόμενα κομμάτια αντιστοιχίζονται στον κατάλογο του Levyra για αξιόπιστη αναπαραγωγή.",
        "Ακύρωση"
    )
    "sv" -> PlaylistImportCopy(
        "Importera spellista",
        "Från stödda musiktjänster och kompatibla säkerhetskopior",
        "Importera",
        "Klistra in länken till en spellista från en stödd musiktjänst eller innehållet i en kompatibel säkerhetskopia. Levyra identifierar låtarna och skapar en spellista i ditt bibliotek.",
        "Klistra in länk eller säkerhetskopia…",
        "Importerade låtar matchas mot Levyras katalog för tillförlitlig uppspelning.",
        "Avbryt"
    )
    "da" -> PlaylistImportCopy(
        "Importér playliste",
        "Fra understøttede musiktjenester og kompatible sikkerhedskopier",
        "Importér",
        "Indsæt linket til en playliste fra en understøttet musiktjeneste eller indholdet af en kompatibel sikkerhedskopi. Levyra genkender numrene og opretter en playliste i dit bibliotek.",
        "Indsæt link eller sikkerhedskopi…",
        "Importerede numre matches med Levyra-kataloget for pålidelig afspilning.",
        "Annuller"
    )
    "cs" -> PlaylistImportCopy(
        "Importovat playlist",
        "Z podporovaných hudebních služeb a kompatibilních záloh",
        "Importovat",
        "Vložte odkaz na playlist z podporované hudební služby nebo obsah kompatibilní zálohy. Levyra rozpozná skladby a vytvoří playlist ve vaší knihovně.",
        "Vložte odkaz nebo obsah zálohy…",
        "Importované skladby se přiřadí ke katalogu Levyra pro spolehlivé přehrávání.",
        "Zrušit"
    )
    "uk" -> PlaylistImportCopy(
        "Імпортувати плейлист",
        "З підтримуваних музичних сервісів і сумісних резервних копій",
        "Імпортувати",
        "Вставте посилання на плейлист із підтримуваного музичного сервісу або вміст сумісної резервної копії. Levyra розпізнає композиції та створить плейлист у вашій бібліотеці.",
        "Вставте посилання або вміст резервної копії…",
        "Імпортовані композиції зіставляються з каталогом Levyra для надійного відтворення.",
        "Скасувати"
    )
    "ru" -> PlaylistImportCopy(
        "Импортировать плейлист",
        "Из поддерживаемых музыкальных сервисов и совместимых резервных копий",
        "Импортировать",
        "Вставьте ссылку на плейлист из поддерживаемого музыкального сервиса или содержимое совместимой резервной копии. Levyra распознает треки и создаст плейлист в вашей медиатеке.",
        "Вставьте ссылку или содержимое резервной копии…",
        "Импортированные треки сопоставляются с каталогом Levyra для надежного воспроизведения.",
        "Отмена"
    )
    "tr" -> PlaylistImportCopy(
        "Çalma listesi içe aktar",
        "Desteklenen müzik servislerinden ve uyumlu yedeklerden",
        "İçe aktar",
        "Desteklenen bir müzik servisindeki çalma listesinin bağlantısını veya uyumlu bir yedeğin içeriğini yapıştırın. Levyra parçaları tanıyıp kitaplığınızda bir çalma listesi oluşturur.",
        "Bağlantıyı veya yedek içeriğini yapıştırın…",
        "İçe aktarılan parçalar güvenilir oynatma için Levyra kataloğuyla eşleştirilir.",
        "İptal"
    )
    "ar" -> PlaylistImportCopy(
        "استيراد قائمة تشغيل",
        "من خدمات الموسيقى المدعومة والنسخ الاحتياطية المتوافقة",
        "استيراد",
        "الصق رابط قائمة تشغيل من خدمة موسيقى مدعومة أو محتوى نسخة احتياطية متوافقة. سيتعرّف Levyra على المقاطع وينشئ قائمة تشغيل في مكتبتك.",
        "الصق الرابط أو محتوى النسخة الاحتياطية…",
        "تتم مطابقة المقاطع المستوردة مع كتالوج Levyra لضمان تشغيل موثوق.",
        "إلغاء"
    )
    "zh" -> PlaylistImportCopy(
        "导入播放列表",
        "支持的音乐服务与兼容备份均可导入",
        "导入",
        "粘贴来自受支持音乐服务的播放列表链接，或粘贴兼容备份的内容。Levyra 会识别歌曲并在你的音乐库中创建播放列表。",
        "粘贴链接或备份内容…",
        "导入的歌曲会与 Levyra 曲库进行匹配，以确保稳定播放。",
        "取消"
    )
    "ja" -> PlaylistImportCopy(
        "プレイリストをインポート",
        "対応する音楽サービスと互換バックアップから取り込み",
        "インポート",
        "対応する音楽サービスのプレイリストリンク、または互換バックアップの内容を貼り付けてください。Levyra が曲を識別し、ライブラリにプレイリストを作成します。",
        "リンクまたはバックアップ内容を貼り付け…",
        "インポートした曲は、安定した再生のため Levyra のカタログと照合されます。",
        "キャンセル"
    )
    "ko" -> PlaylistImportCopy(
        "플레이리스트 가져오기",
        "지원되는 음악 서비스와 호환 백업에서 가져오기",
        "가져오기",
        "지원되는 음악 서비스의 플레이리스트 링크 또는 호환 백업 내용을 붙여 넣으세요. Levyra가 곡을 식별해 보관함에 플레이리스트를 만듭니다.",
        "링크 또는 백업 내용을 붙여 넣으세요…",
        "가져온 곡은 안정적인 재생을 위해 Levyra 카탈로그와 매칭됩니다.",
        "취소"
    )
    "hi" -> PlaylistImportCopy(
        "प्लेलिस्ट आयात करें",
        "समर्थित संगीत सेवाओं और संगत बैकअप से",
        "आयात करें",
        "किसी समर्थित संगीत सेवा की प्लेलिस्ट का लिंक या संगत बैकअप की सामग्री पेस्ट करें। Levyra गानों को पहचानकर आपकी लाइब्रेरी में एक प्लेलिस्ट बनाएगा।",
        "लिंक या बैकअप सामग्री पेस्ट करें…",
        "विश्वसनीय प्लेबैक के लिए आयात किए गए गानों का Levyra कैटलॉग से मिलान किया जाता है।",
        "रद्द करें"
    )
    "id" -> PlaylistImportCopy(
        "Impor playlist",
        "Dari layanan musik yang didukung dan cadangan yang kompatibel",
        "Impor",
        "Tempel tautan playlist dari layanan musik yang didukung atau isi cadangan yang kompatibel. Levyra akan mengenali lagu dan membuat playlist di koleksi Anda.",
        "Tempel tautan atau isi cadangan…",
        "Lagu yang diimpor dicocokkan dengan katalog Levyra agar pemutaran tetap andal.",
        "Batal"
    )
    "vi" -> PlaylistImportCopy(
        "Nhập playlist",
        "Từ các dịch vụ nhạc được hỗ trợ và bản sao lưu tương thích",
        "Nhập",
        "Dán liên kết playlist từ một dịch vụ nhạc được hỗ trợ hoặc nội dung của bản sao lưu tương thích. Levyra sẽ nhận diện các bài hát và tạo playlist trong thư viện của bạn.",
        "Dán liên kết hoặc nội dung bản sao lưu…",
        "Các bài hát đã nhập được đối chiếu với danh mục Levyra để đảm bảo phát ổn định.",
        "Hủy"
    )
    "th" -> PlaylistImportCopy(
        "นำเข้าเพลย์ลิสต์",
        "จากบริการเพลงที่รองรับและข้อมูลสำรองที่เข้ากันได้",
        "นำเข้า",
        "วางลิงก์เพลย์ลิสต์จากบริการเพลงที่รองรับ หรือวางเนื้อหาจากข้อมูลสำรองที่เข้ากันได้ Levyra จะระบุเพลงและสร้างเพลย์ลิสต์ในคลังของคุณ",
        "วางลิงก์หรือเนื้อหาข้อมูลสำรอง…",
        "เพลงที่นำเข้าจะถูกจับคู่กับแคตตาล็อกของ Levyra เพื่อให้เล่นได้อย่างเสถียร",
        "ยกเลิก"
    )
    "fil" -> PlaylistImportCopy(
        "Mag-import ng playlist",
        "Mula sa mga suportadong music service at compatible na backup",
        "I-import",
        "I-paste ang link ng playlist mula sa suportadong music service o ang nilalaman ng compatible na backup. Kikilalanin ng Levyra ang mga kanta at gagawa ng playlist sa library mo.",
        "I-paste ang link o laman ng backup…",
        "Itinutugma ang mga na-import na kanta sa catalog ng Levyra para sa maaasahang playback.",
        "Kanselahin"
    )
    "he" -> PlaylistImportCopy(
        "ייבוא פלייליסט",
        "משירותי מוזיקה נתמכים ומגיבויים תואמים",
        "ייבוא",
        "הדביקו קישור לפלייליסט משירות מוזיקה נתמך או את התוכן של גיבוי תואם. Levyra יזהה את השירים ויצור פלייליסט בספרייה שלכם.",
        "הדביקו קישור או תוכן גיבוי…",
        "השירים המיובאים מותאמים לקטלוג של Levyra כדי לשמור על הפעלה אמינה.",
        "ביטול"
    )
    else -> PlaylistImportCopy(
        "Import playlist",
        "From supported music services and compatible backups",
        "Import",
        "Paste a playlist link from a supported music service or the contents of a compatible backup. Levyra will identify the tracks and create a playlist in your library.",
        "Paste a link or backup content…",
        "Imported tracks are matched to Levyra's catalog for reliable playback.",
        "Cancel"
    )
}

fun LevyraStrings.localizedAudioPresetLabel(presetId: String, fallback: String): String {
    val labels = localizedNewAudioPresetLabels(code)
    return when (presetId) {
        LevyraAudioPresets.ROCK -> labels.rock
        LevyraAudioPresets.POP -> labels.pop
        LevyraAudioPresets.ELECTRONIC -> labels.electronic
        LevyraAudioPresets.JAZZ -> labels.jazz
        LevyraAudioPresets.ACOUSTIC -> labels.acoustic
        LevyraAudioPresets.CLASSICAL -> labels.classical
        LevyraAudioPresets.AIRPODS_PRO -> "AirPods Pro · ${labels.deviceTune}"
        LevyraAudioPresets.SONY_XM4 -> "Sony WH-1000XM4 · ${labels.deviceTune}"
        LevyraAudioPresets.SONY_XM5 -> "Sony WH-1000XM5 · ${labels.deviceTune}"
        LevyraAudioPresets.SENNHEISER_HD600 -> "Sennheiser HD600 · ${labels.deviceTune}"
        else -> fallback
    }
}

private data class NewAudioPresetLabels(
    val rock: String,
    val pop: String,
    val electronic: String,
    val jazz: String,
    val acoustic: String,
    val classical: String,
    val deviceTune: String
)

private fun localizedNewAudioPresetLabels(code: String): NewAudioPresetLabels = when (code) {
    "it" -> NewAudioPresetLabels("Rock", "Pop", "Elettronica", "Jazz", "Acustica", "Classica", "Profilo dispositivo")
    "es" -> NewAudioPresetLabels("Rock", "Pop", "Electrónica", "Jazz", "Acústica", "Clásica", "Ajuste de dispositivo")
    "fr" -> NewAudioPresetLabels("Rock", "Pop", "Électronique", "Jazz", "Acoustique", "Classique", "Réglage appareil")
    "de" -> NewAudioPresetLabels("Rock", "Pop", "Elektronisch", "Jazz", "Akustik", "Klassik", "Geräteabstimmung")
    "pt" -> NewAudioPresetLabels("Rock", "Pop", "Eletrónica", "Jazz", "Acústica", "Clássica", "Ajuste do dispositivo")
    "nl" -> NewAudioPresetLabels("Rock", "Pop", "Elektronisch", "Jazz", "Akoestisch", "Klassiek", "Apparaatafstemming")
    "pl" -> NewAudioPresetLabels("Rock", "Pop", "Elektroniczna", "Jazz", "Akustyczna", "Klasyczna", "Strojenie urządzenia")
    "ro" -> NewAudioPresetLabels("Rock", "Pop", "Electronică", "Jazz", "Acustică", "Clasică", "Reglaj dispozitiv")
    "el" -> NewAudioPresetLabels("Ροκ", "Ποπ", "Ηλεκτρονική", "Τζαζ", "Ακουστική", "Κλασική", "Ρύθμιση συσκευής")
    "sv" -> NewAudioPresetLabels("Rock", "Pop", "Elektroniskt", "Jazz", "Akustiskt", "Klassiskt", "Enhetsanpassning")
    "da" -> NewAudioPresetLabels("Rock", "Pop", "Elektronisk", "Jazz", "Akustisk", "Klassisk", "Enhedsjustering")
    "cs" -> NewAudioPresetLabels("Rock", "Pop", "Elektronika", "Jazz", "Akustika", "Klasika", "Ladění zařízení")
    "uk" -> NewAudioPresetLabels("Рок", "Поп", "Електроніка", "Джаз", "Акустика", "Класика", "Налаштування пристрою")
    "ru" -> NewAudioPresetLabels("Рок", "Поп", "Электроника", "Джаз", "Акустика", "Классика", "Настройка устройства")
    "tr" -> NewAudioPresetLabels("Rock", "Pop", "Elektronik", "Caz", "Akustik", "Klasik", "Cihaz ayarı")
    "ar" -> NewAudioPresetLabels("روك", "بوب", "إلكتروني", "جاز", "أكوستيك", "كلاسيكي", "ضبط الجهاز")
    "zh" -> NewAudioPresetLabels("摇滚", "流行", "电子", "爵士", "原声", "古典", "设备调音")
    "ja" -> NewAudioPresetLabels("ロック", "ポップ", "エレクトロニック", "ジャズ", "アコースティック", "クラシック", "デバイス調整")
    "ko" -> NewAudioPresetLabels("록", "팝", "일렉트로닉", "재즈", "어쿠스틱", "클래식", "기기 튜닝")
    "hi" -> NewAudioPresetLabels("रॉक", "पॉप", "इलेक्ट्रॉनिक", "जैज़", "अकूस्टिक", "शास्त्रीय", "डिवाइस ट्यूनिंग")
    "id" -> NewAudioPresetLabels("Rock", "Pop", "Elektronik", "Jazz", "Akustik", "Klasik", "Penyetelan perangkat")
    "vi" -> NewAudioPresetLabels("Rock", "Pop", "Điện tử", "Jazz", "Mộc", "Cổ điển", "Tinh chỉnh thiết bị")
    "th" -> NewAudioPresetLabels("ร็อก", "ป๊อป", "อิเล็กทรอนิกส์", "แจ๊ส", "อะคูสติก", "คลาสสิก", "ปรับจูนอุปกรณ์")
    "fil" -> NewAudioPresetLabels("Rock", "Pop", "Electronic", "Jazz", "Acoustic", "Classical", "Tuning ng device")
    "he" -> NewAudioPresetLabels("רוק", "פופ", "אלקטרוני", "ג׳אז", "אקוסטי", "קלאסי", "כיוון למכשיר")
    else -> NewAudioPresetLabels("Rock", "Pop", "Electronic", "Jazz", "Acoustic", "Classical", "Device tune")
}

fun playlistImportStarted(code: String): String = when (code) {
    "it" -> "Importazione playlist in corso…"
    "es" -> "Importando playlist…"
    "fr" -> "Importation de la playlist…"
    "de" -> "Playlist wird importiert…"
    "pt" -> "A importar playlist…"
    "nl" -> "Playlist wordt geïmporteerd…"
    "pl" -> "Importowanie playlisty…"
    "ro" -> "Se importă playlistul…"
    "el" -> "Γίνεται εισαγωγή της playlist…"
    "sv" -> "Importerar spellista…"
    "da" -> "Importerer playliste…"
    "cs" -> "Importuje se playlist…"
    "uk" -> "Імпорт плейлиста…"
    "ru" -> "Импорт плейлиста…"
    "tr" -> "Çalma listesi içe aktarılıyor…"
    "ar" -> "جارٍ استيراد قائمة التشغيل…"
    "zh" -> "正在导入播放列表…"
    "ja" -> "プレイリストをインポート中…"
    "ko" -> "플레이리스트를 가져오는 중…"
    "hi" -> "प्लेलिस्ट आयात की जा रही है…"
    "id" -> "Mengimpor playlist…"
    "vi" -> "Đang nhập playlist…"
    "th" -> "กำลังนำเข้าเพลย์ลิสต์…"
    "fil" -> "Ini-import ang playlist…"
    "he" -> "מייבא פלייליסט…"
    else -> "Importing playlist…"
}

fun playlistImportSuccess(code: String, count: Int, playlistName: String): String = when (code) {
    "it" -> "Importati $count brani in $playlistName"
    "es" -> "Se importaron $count canciones a $playlistName"
    "fr" -> "$count titres importés dans $playlistName"
    "de" -> "$count Titel in $playlistName importiert"
    "pt" -> "$count faixas importadas para $playlistName"
    "nl" -> "$count nummers geïmporteerd in $playlistName"
    "pl" -> "Zaimportowano $count utworów do $playlistName"
    "ro" -> "Au fost importate $count piese în $playlistName"
    "el" -> "Εισήχθησαν $count κομμάτια στο $playlistName"
    "sv" -> "$count låtar importerades till $playlistName"
    "da" -> "$count numre importeret til $playlistName"
    "cs" -> "Do $playlistName bylo importováno $count skladeb"
    "uk" -> "Імпортовано $count композицій у $playlistName"
    "ru" -> "Импортировано $count треков в $playlistName"
    "tr" -> "$playlistName içine $count parça aktarıldı"
    "ar" -> "تم استيراد $count مقطعًا إلى $playlistName"
    "zh" -> "已将 $count 首歌曲导入 $playlistName"
    "ja" -> "$playlistName に $count 曲をインポートしました"
    "ko" -> "$playlistName에 $count곡을 가져왔습니다"
    "hi" -> "$playlistName में $count गाने आयात किए गए"
    "id" -> "$count lagu diimpor ke $playlistName"
    "vi" -> "Đã nhập $count bài hát vào $playlistName"
    "th" -> "นำเข้า $count เพลงไปยัง $playlistName แล้ว"
    "fil" -> "Na-import ang $count kanta sa $playlistName"
    "he" -> "יובאו $count שירים אל $playlistName"
    else -> "Imported $count tracks into $playlistName"
}

fun playlistImportFailure(code: String, reason: String): String = when (code) {
    "it" -> "Importazione non riuscita: $reason"
    "es" -> "No se pudo importar: $reason"
    "fr" -> "Échec de l’importation : $reason"
    "de" -> "Import fehlgeschlagen: $reason"
    "pt" -> "Falha na importação: $reason"
    "nl" -> "Importeren mislukt: $reason"
    "pl" -> "Import nie powiódł się: $reason"
    "ro" -> "Importul a eșuat: $reason"
    "el" -> "Η εισαγωγή απέτυχε: $reason"
    "sv" -> "Importen misslyckades: $reason"
    "da" -> "Importen mislykkedes: $reason"
    "cs" -> "Import se nezdařil: $reason"
    "uk" -> "Не вдалося імпортувати: $reason"
    "ru" -> "Не удалось импортировать: $reason"
    "tr" -> "İçe aktarma başarısız: $reason"
    "ar" -> "تعذّر الاستيراد: $reason"
    "zh" -> "导入失败：$reason"
    "ja" -> "インポートできませんでした: $reason"
    "ko" -> "가져오기에 실패했습니다: $reason"
    "hi" -> "आयात नहीं हो सका: $reason"
    "id" -> "Impor gagal: $reason"
    "vi" -> "Nhập không thành công: $reason"
    "th" -> "นำเข้าไม่สำเร็จ: $reason"
    "fil" -> "Hindi na-import: $reason"
    "he" -> "הייבוא נכשל: $reason"
    else -> "Import failed: $reason"
}
