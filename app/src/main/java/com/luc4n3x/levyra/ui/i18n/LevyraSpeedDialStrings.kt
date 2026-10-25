package com.luc4n3x.levyra.ui.i18n

data class SpeedDialCopy(
    val title: String,
    val addToHome: String,
    val removeFromHome: String,
    val moveEarlier: String,
    val moveLater: String,
    val song: String,
    val album: String,
    val artist: String,
    val playlist: String,
    val reorderHint: String
)

private fun speedDial(vararg value: String): SpeedDialCopy {
    require(value.size == 10)
    return SpeedDialCopy(
        value[0], value[1], value[2], value[3], value[4],
        value[5], value[6], value[7], value[8], value[9]
    )
}

private val speedDialCopies = mapOf(
    "en" to speedDial(
        "Pinned", "Pin to Home", "Unpin from Home",
        "Move earlier", "Move later", "Song", "Album", "Artist", "Playlist", "Hold and drag to reorder"
    ),
    "it" to speedDial(
        "Fissati", "Fissa in Home", "Rimuovi dalla Home",
        "Sposta prima", "Sposta dopo", "Brano", "Album", "Artista", "Playlist", "Tieni premuto e trascina per riordinare"
    ),
    "es" to speedDial(
        "Fijados", "Fijar en Inicio", "Quitar de Inicio",
        "Mover antes", "Mover después", "Canción", "Álbum", "Artista", "Playlist", "Mantén pulsado y arrastra para reordenar"
    ),
    "fr" to speedDial(
        "Épinglés", "Épingler à l’accueil", "Retirer de l’accueil",
        "Déplacer avant", "Déplacer après", "Titre", "Album", "Artiste", "Playlist", "Maintenez puis faites glisser pour réorganiser"
    ),
    "de" to speedDial(
        "Angeheftet", "An Startseite anheften", "Von Startseite lösen",
        "Nach vorne", "Nach hinten", "Song", "Album", "Künstler", "Playlist", "Halten und ziehen zum Sortieren"
    ),
    "pt" to speedDial(
        "Fixados", "Fixar no Início", "Remover do Início",
        "Mover para antes", "Mover para depois", "Música", "Álbum", "Artista", "Playlist", "Toque e segure, depois arraste para reordenar"
    ),
    "nl" to speedDial(
        "Vastgezet", "Vastzetten op Home", "Losmaken van Home",
        "Naar voren", "Naar achteren", "Nummer", "Album", "Artiest", "Playlist", "Houd vast en sleep om te ordenen"
    ),
    "pl" to speedDial(
        "Przypięte", "Przypnij do strony głównej", "Odepnij od strony głównej",
        "Przesuń wcześniej", "Przesuń później", "Utwór", "Album", "Wykonawca", "Playlista", "Przytrzymaj i przeciągnij, aby zmienić kolejność"
    ),
    "ro" to speedDial(
        "Fixate", "Fixează pe Acasă", "Elimină de pe Acasă",
        "Mută mai devreme", "Mută mai târziu", "Melodie", "Album", "Artist", "Playlist", "Ține apăsat și trage pentru a reordona"
    ),
    "el" to speedDial(
        "Καρφιτσωμένα", "Καρφίτσωμα στην Αρχική", "Ξεκαρφίτσωμα από την Αρχική",
        "Μετακίνηση νωρίτερα", "Μετακίνηση αργότερα", "Τραγούδι", "Άλμπουμ", "Καλλιτέχνης", "Playlist", "Πατήστε παρατεταμένα και σύρετε για αναδιάταξη"
    ),
    "sv" to speedDial(
        "Fästa", "Fäst på Hem", "Lossa från Hem",
        "Flytta tidigare", "Flytta senare", "Låt", "Album", "Artist", "Spellista", "Håll ned och dra för att ändra ordning"
    ),
    "da" to speedDial(
        "Fastgjort", "Fastgør på Hjem", "Frigør fra Hjem",
        "Flyt tidligere", "Flyt senere", "Sang", "Album", "Kunstner", "Playliste", "Hold nede og træk for at ændre rækkefølge"
    ),
    "cs" to speedDial(
        "Připnuté", "Připnout na Domů", "Odepnout z Domů",
        "Posunout dříve", "Posunout později", "Skladba", "Album", "Interpret", "Playlist", "Podržte a přetáhněte pro změnu pořadí"
    ),
    "sk" to speedDial(
        "Pripnuté", "Pripnúť na Domov", "Odopnúť z Domova",
        "Posunúť skôr", "Posunúť neskôr", "Skladba", "Album", "Interpret", "Playlist", "Podržte a potiahnite na zmenu poradia"
    ),
    "bn" to speedDial(
        "পিন করা", "হোমে পিন করুন", "হোম থেকে আনপিন করুন",
        "আগে সরান", "পরে সরান", "গান", "অ্যালবাম", "শিল্পী", "প্লেলিস্ট", "ক্রম বদলাতে চেপে ধরে টানুন"
    ),
    "kk" to speedDial(
        "Бекітілген", "Басты бетке бекіту", "Басты беттен босату",
        "Ертерек жылжыту", "Кейінірек жылжыту", "Ән", "Альбом", "Орындаушы", "Ойнату тізімі", "Ретін өзгерту үшін басып тұрып сүйреңіз"
    ),
    "sr" to speedDial(
        "Закачено", "Закачи на Почетну", "Откачи са Почетне",
        "Помери раније", "Помери касније", "Песма", "Албум", "Извођач", "Плејлиста", "Задржи и превуци за промену редоследа"
    ),
    "sq" to speedDial(
        "Të fiksuara", "Fikso në Kreu", "Hiq nga Kreu",
        "Lëviz më përpara", "Lëviz më pas", "Këngë", "Album", "Artist", "Listë luajtjeje", "Mbaj dhe tërhiq për të ndryshuar renditjen"
    ),
    "hr" to speedDial(
        "Prikvačeno", "Prikvači na Početnu", "Otkvači s Početne",
        "Pomakni ranije", "Pomakni kasnije", "Pjesma", "Album", "Izvođač", "Playlista", "Zadržite i povucite za promjenu redoslijeda"
    ),
    "bg" to speedDial(
        "Закачени", "Закачи на Начало", "Откачи от Начало",
        "Премести по-напред", "Премести по-назад", "Песен", "Албум", "Изпълнител", "Плейлист", "Задръжте и плъзнете, за да пренаредите"
    ),
    "hu" to speedDial(
        "Kitűzve", "Kitűzés a Kezdőlapra", "Levétel a Kezdőlapról",
        "Előrébb", "Hátrébb", "Dal", "Album", "Előadó", "Lejátszási lista", "Tartsd lenyomva és húzd az átrendezéshez"
    ),
    "nb" to speedDial(
        "Festet", "Fest til Hjem", "Løsne fra Hjem",
        "Flytt tidligere", "Flytt senere", "Sang", "Album", "Artist", "Spilleliste", "Hold inne og dra for å endre rekkefølge"
    ),
    "ca" to speedDial(
        "Fixats", "Fixa a l’Inici", "Treu de l’Inici",
        "Mou abans", "Mou després", "Cançó", "Àlbum", "Artista", "Llista", "Mantén premut i arrossega per reordenar"
    ),
    "uk" to speedDial(
        "Закріплені", "Закріпити на головній", "Відкріпити з головної",
        "Перемістити раніше", "Перемістити пізніше", "Трек", "Альбом", "Виконавець", "Плейлист", "Утримуйте й перетягуйте, щоб змінити порядок"
    ),
    "ru" to speedDial(
        "Закреплённые", "Закрепить на главной", "Открепить с главной",
        "Переместить раньше", "Переместить позже", "Трек", "Альбом", "Исполнитель", "Плейлист", "Удерживайте и перетаскивайте, чтобы изменить порядок"
    ),
    "tr" to speedDial(
        "Sabitlenenler", "Ana sayfaya sabitle", "Ana sayfadan kaldır",
        "Öne taşı", "Arkaya taşı", "Şarkı", "Albüm", "Sanatçı", "Çalma listesi", "Sıralamak için basılı tutup sürükleyin"
    ),
    "ar" to speedDial(
        "المثبّتة", "تثبيت في الرئيسية", "إزالة من الرئيسية",
        "نقل للأمام", "نقل للخلف", "أغنية", "ألبوم", "فنان", "قائمة تشغيل", "اضغط مطولًا واسحب لإعادة الترتيب"
    ),
    "fa" to speedDial(
        "سنجاق‌شده", "سنجاق به خانه", "برداشتن از خانه",
        "انتقال به قبل", "انتقال به بعد", "آهنگ", "آلبوم", "هنرمند", "فهرست پخش", "برای تغییر ترتیب، نگه دارید و بکشید"
    ),
    "zh-Hant" to speedDial(
        "已釘選", "釘選到首頁", "從首頁移除",
        "前移", "後移", "歌曲", "專輯", "藝人", "播放清單", "長按並拖曳即可重新排序"
    ),
    "zh" to speedDial(
        "已固定", "固定到主页", "从主页移除",
        "前移", "后移", "歌曲", "专辑", "艺人", "播放列表", "长按并拖动以重新排序"
    ),
    "ja" to speedDial(
        "ピン留め", "ホームにピン留め", "ホームから外す",
        "前へ移動", "後ろへ移動", "曲", "アルバム", "アーティスト", "プレイリスト", "長押ししてドラッグすると並べ替えられます"
    ),
    "ko" to speedDial(
        "고정됨", "홈에 고정", "홈에서 고정 해제",
        "앞으로 이동", "뒤로 이동", "노래", "앨범", "아티스트", "플레이리스트", "길게 누른 채 끌어서 순서 변경"
    ),
    "hi" to speedDial(
        "पिन किए गए", "होम पर पिन करें", "होम से अनपिन करें",
        "पहले ले जाएँ", "बाद में ले जाएँ", "गाना", "एल्बम", "कलाकार", "प्लेलिस्ट", "क्रम बदलने के लिए दबाकर रखें और खींचें"
    ),
    "id" to speedDial(
        "Disematkan", "Sematkan ke Beranda", "Lepas dari Beranda",
        "Pindah lebih awal", "Pindah lebih akhir", "Lagu", "Album", "Artis", "Playlist", "Tahan lalu seret untuk mengurutkan"
    ),
    "ms" to speedDial(
        "Disemat", "Semat ke Laman Utama", "Nyahsemat dari Laman Utama",
        "Alih lebih awal", "Alih lebih lewat", "Lagu", "Album", "Artis", "Senarai main", "Tekan lama dan seret untuk menyusun semula"
    ),
    "vi" to speedDial(
        "Đã ghim", "Ghim vào Trang chủ", "Bỏ ghim khỏi Trang chủ",
        "Di chuyển lên trước", "Di chuyển ra sau", "Bài hát", "Album", "Nghệ sĩ", "Danh sách phát", "Nhấn giữ rồi kéo để sắp xếp lại"
    ),
    "th" to speedDial(
        "ปักหมุดไว้", "ปักหมุดที่หน้าแรก", "เลิกปักหมุดจากหน้าแรก",
        "ย้ายไปก่อนหน้า", "ย้ายไปถัดไป", "เพลง", "อัลบั้ม", "ศิลปิน", "เพลย์ลิสต์", "กดค้างแล้วลากเพื่อจัดลำดับใหม่"
    ),
    "fil" to speedDial(
        "Naka-pin", "I-pin sa Home", "Alisin sa Home",
        "Ilipat pauna", "Ilipat pahuli", "Kanta", "Album", "Artist", "Playlist", "Pindutin nang matagal at i-drag para ayusin"
    ),
    "he" to speedDial(
        "נעוצים", "נעיצה בדף הבית", "ביטול נעיצה מדף הבית",
        "הזזה מוקדם יותר", "הזזה מאוחר יותר", "שיר", "אלבום", "אמן", "פלייליסט", "לחיצה ארוכה וגרירה לשינוי הסדר"
    ),
    "fi" to speedDial(
        "Kiinnitetyt", "Kiinnitä etusivulle", "Irrota etusivulta",
        "Siirrä aiemmaksi", "Siirrä myöhemmäksi", "Kappale", "Albumi", "Artisti", "Soittolista", "Pidä painettuna ja vedä järjestääksesi"
    ),
    "et" to speedDial(
        "Kinnitatud", "Kinnita avalehele", "Eemalda avalehelt",
        "Liiguta ettepoole", "Liiguta tahapoole", "Lugu", "Album", "Artist", "Esitusloend", "Hoia all ja lohista järjestuse muutmiseks"
    )
)

internal fun speedDialLocalizationCodes(): Set<String> = speedDialCopies.keys

fun LevyraStrings.speedDialCopy(): SpeedDialCopy = speedDialCopies[code] ?: speedDialCopies.getValue("en")
