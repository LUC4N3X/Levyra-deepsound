package com.luc4n3x.levyra.ui.i18n

data class SpeedDialCopy(
    val title: String,
    val addToHome: String,
    val removeFromHome: String,
    val homeFull: String,
    val moveEarlier: String,
    val moveLater: String,
    val song: String,
    val album: String,
    val artist: String,
    val playlist: String,
    val reorderHint: String
)

private fun speedDial(vararg value: String): SpeedDialCopy {
    require(value.size == 11)
    return SpeedDialCopy(
        value[0], value[1], value[2], value[3], value[4], value[5],
        value[6], value[7], value[8], value[9], value[10]
    )
}

private val speedDialCopies = mapOf(
    "en" to speedDial(
        "Pinned", "Pin to Home", "Unpin from Home", "Home can hold up to 24 pins",
        "Move earlier", "Move later", "Song", "Album", "Artist", "Playlist", "Hold and drag to reorder"
    ),
    "it" to speedDial(
        "Fissati", "Fissa in Home", "Rimuovi dalla Home", "La Home può contenere fino a 24 elementi fissati",
        "Sposta prima", "Sposta dopo", "Brano", "Album", "Artista", "Playlist", "Tieni premuto e trascina per riordinare"
    ),
    "es" to speedDial(
        "Fijados", "Fijar en Inicio", "Quitar de Inicio", "Inicio admite hasta 24 elementos fijados",
        "Mover antes", "Mover después", "Canción", "Álbum", "Artista", "Playlist", "Mantén pulsado y arrastra para reordenar"
    ),
    "fr" to speedDial(
        "Épinglés", "Épingler à l’accueil", "Retirer de l’accueil", "L’accueil accepte jusqu’à 24 éléments épinglés",
        "Déplacer avant", "Déplacer après", "Titre", "Album", "Artiste", "Playlist", "Maintenez puis faites glisser pour réorganiser"
    ),
    "de" to speedDial(
        "Angeheftet", "An Startseite anheften", "Von Startseite lösen", "Die Startseite fasst bis zu 24 angeheftete Elemente",
        "Nach vorne", "Nach hinten", "Song", "Album", "Künstler", "Playlist", "Halten und ziehen zum Sortieren"
    ),
    "pt" to speedDial(
        "Fixados", "Fixar no Início", "Remover do Início", "O Início aceita até 24 itens fixados",
        "Mover para antes", "Mover para depois", "Música", "Álbum", "Artista", "Playlist", "Toque e segure, depois arraste para reordenar"
    ),
    "nl" to speedDial(
        "Vastgezet", "Vastzetten op Home", "Losmaken van Home", "Home kan maximaal 24 vastgezette items bevatten",
        "Naar voren", "Naar achteren", "Nummer", "Album", "Artiest", "Playlist", "Houd vast en sleep om te ordenen"
    ),
    "pl" to speedDial(
        "Przypięte", "Przypnij do strony głównej", "Odepnij od strony głównej", "Strona główna mieści do 24 przypiętych elementów",
        "Przesuń wcześniej", "Przesuń później", "Utwór", "Album", "Wykonawca", "Playlista", "Przytrzymaj i przeciągnij, aby zmienić kolejność"
    ),
    "ro" to speedDial(
        "Fixate", "Fixează pe Acasă", "Elimină de pe Acasă", "Acasă poate avea cel mult 24 de elemente fixate",
        "Mută mai devreme", "Mută mai târziu", "Melodie", "Album", "Artist", "Playlist", "Ține apăsat și trage pentru a reordona"
    ),
    "el" to speedDial(
        "Καρφιτσωμένα", "Καρφίτσωμα στην Αρχική", "Ξεκαρφίτσωμα από την Αρχική", "Η Αρχική χωράει έως 24 καρφιτσωμένα στοιχεία",
        "Μετακίνηση νωρίτερα", "Μετακίνηση αργότερα", "Τραγούδι", "Άλμπουμ", "Καλλιτέχνης", "Playlist", "Πατήστε παρατεταμένα και σύρετε για αναδιάταξη"
    ),
    "sv" to speedDial(
        "Fästa", "Fäst på Hem", "Lossa från Hem", "Hem rymmer upp till 24 fästa objekt",
        "Flytta tidigare", "Flytta senare", "Låt", "Album", "Artist", "Spellista", "Håll ned och dra för att ändra ordning"
    ),
    "da" to speedDial(
        "Fastgjort", "Fastgør på Hjem", "Frigør fra Hjem", "Hjem kan rumme op til 24 fastgjorte elementer",
        "Flyt tidligere", "Flyt senere", "Sang", "Album", "Kunstner", "Playliste", "Hold nede og træk for at ændre rækkefølge"
    ),
    "cs" to speedDial(
        "Připnuté", "Připnout na Domů", "Odepnout z Domů", "Na Domů lze připnout nejvýše 24 položek",
        "Posunout dříve", "Posunout později", "Skladba", "Album", "Interpret", "Playlist", "Podržte a přetáhněte pro změnu pořadí"
    ),
    "sk" to speedDial(
        "Pripnuté", "Pripnúť na Domov", "Odopnúť z Domova", "Na Domov môžete pripnúť najviac 24 položiek",
        "Posunúť skôr", "Posunúť neskôr", "Skladba", "Album", "Interpret", "Playlist", "Podržte a potiahnite na zmenu poradia"
    ),
    "hr" to speedDial(
        "Prikvačeno", "Prikvači na Početnu", "Otkvači s Početne", "Na Početnu možete prikvačiti najviše 24 stavke",
        "Pomakni ranije", "Pomakni kasnije", "Pjesma", "Album", "Izvođač", "Playlista", "Zadržite i povucite za promjenu redoslijeda"
    ),
    "bg" to speedDial(
        "Закачени", "Закачи на Начало", "Откачи от Начало", "В Начало могат да се закачат до 24 елемента",
        "Премести по-напред", "Премести по-назад", "Песен", "Албум", "Изпълнител", "Плейлист", "Задръжте и плъзнете, за да пренаредите"
    ),
    "hu" to speedDial(
        "Kitűzve", "Kitűzés a Kezdőlapra", "Levétel a Kezdőlapról", "A Kezdőlapra legfeljebb 24 elem tűzhető ki",
        "Előrébb", "Hátrébb", "Dal", "Album", "Előadó", "Lejátszási lista", "Tartsd lenyomva és húzd az átrendezéshez"
    ),
    "nb" to speedDial(
        "Festet", "Fest til Hjem", "Løsne fra Hjem", "Hjem har plass til opptil 24 festede elementer",
        "Flytt tidligere", "Flytt senere", "Sang", "Album", "Artist", "Spilleliste", "Hold inne og dra for å endre rekkefølge"
    ),
    "ca" to speedDial(
        "Fixats", "Fixa a l’Inici", "Treu de l’Inici", "L’Inici admet fins a 24 elements fixats",
        "Mou abans", "Mou després", "Cançó", "Àlbum", "Artista", "Llista", "Mantén premut i arrossega per reordenar"
    ),
    "uk" to speedDial(
        "Закріплені", "Закріпити на головній", "Відкріпити з головної", "На головній можна закріпити до 24 елементів",
        "Перемістити раніше", "Перемістити пізніше", "Трек", "Альбом", "Виконавець", "Плейлист", "Утримуйте й перетягуйте, щоб змінити порядок"
    ),
    "ru" to speedDial(
        "Закреплённые", "Закрепить на главной", "Открепить с главной", "На главной можно закрепить до 24 элементов",
        "Переместить раньше", "Переместить позже", "Трек", "Альбом", "Исполнитель", "Плейлист", "Удерживайте и перетаскивайте, чтобы изменить порядок"
    ),
    "tr" to speedDial(
        "Sabitlenenler", "Ana sayfaya sabitle", "Ana sayfadan kaldır", "Ana sayfaya en fazla 24 öğe sabitlenebilir",
        "Öne taşı", "Arkaya taşı", "Şarkı", "Albüm", "Sanatçı", "Çalma listesi", "Sıralamak için basılı tutup sürükleyin"
    ),
    "ar" to speedDial(
        "المثبّتة", "تثبيت في الرئيسية", "إزالة من الرئيسية", "تتسع الرئيسية لـ 24 عنصرًا مثبّتًا كحد أقصى",
        "نقل للأمام", "نقل للخلف", "أغنية", "ألبوم", "فنان", "قائمة تشغيل", "اضغط مطولًا واسحب لإعادة الترتيب"
    ),
    "fa" to speedDial(
        "سنجاق‌شده", "سنجاق به خانه", "برداشتن از خانه", "در خانه حداکثر ۲۴ مورد را می‌توان سنجاق کرد",
        "انتقال به قبل", "انتقال به بعد", "آهنگ", "آلبوم", "هنرمند", "فهرست پخش", "برای تغییر ترتیب، نگه دارید و بکشید"
    ),
    "zh-Hant" to speedDial(
        "已釘選", "釘選到首頁", "從首頁移除", "首頁最多可釘選 24 項",
        "前移", "後移", "歌曲", "專輯", "藝人", "播放清單", "長按並拖曳即可重新排序"
    ),
    "zh" to speedDial(
        "已固定", "固定到主页", "从主页移除", "主页最多可固定 24 项",
        "前移", "后移", "歌曲", "专辑", "艺人", "播放列表", "长按并拖动以重新排序"
    ),
    "ja" to speedDial(
        "ピン留め", "ホームにピン留め", "ホームから外す", "ホームにピン留めできるのは 24 件までです",
        "前へ移動", "後ろへ移動", "曲", "アルバム", "アーティスト", "プレイリスト", "長押ししてドラッグすると並べ替えられます"
    ),
    "ko" to speedDial(
        "고정됨", "홈에 고정", "홈에서 고정 해제", "홈에는 최대 24개까지 고정할 수 있어요",
        "앞으로 이동", "뒤로 이동", "노래", "앨범", "아티스트", "플레이리스트", "길게 누른 채 끌어서 순서 변경"
    ),
    "hi" to speedDial(
        "पिन किए गए", "होम पर पिन करें", "होम से अनपिन करें", "होम पर अधिकतम 24 आइटम पिन किए जा सकते हैं",
        "पहले ले जाएँ", "बाद में ले जाएँ", "गाना", "एल्बम", "कलाकार", "प्लेलिस्ट", "क्रम बदलने के लिए दबाकर रखें और खींचें"
    ),
    "id" to speedDial(
        "Disematkan", "Sematkan ke Beranda", "Lepas dari Beranda", "Beranda bisa memuat hingga 24 item tersemat",
        "Pindah lebih awal", "Pindah lebih akhir", "Lagu", "Album", "Artis", "Playlist", "Tahan lalu seret untuk mengurutkan"
    ),
    "ms" to speedDial(
        "Disemat", "Semat ke Laman Utama", "Nyahsemat dari Laman Utama", "Laman Utama boleh memuatkan sehingga 24 item disemat",
        "Alih lebih awal", "Alih lebih lewat", "Lagu", "Album", "Artis", "Senarai main", "Tekan lama dan seret untuk menyusun semula"
    ),
    "vi" to speedDial(
        "Đã ghim", "Ghim vào Trang chủ", "Bỏ ghim khỏi Trang chủ", "Trang chủ chứa tối đa 24 mục đã ghim",
        "Di chuyển lên trước", "Di chuyển ra sau", "Bài hát", "Album", "Nghệ sĩ", "Danh sách phát", "Nhấn giữ rồi kéo để sắp xếp lại"
    ),
    "th" to speedDial(
        "ปักหมุดไว้", "ปักหมุดที่หน้าแรก", "เลิกปักหมุดจากหน้าแรก", "หน้าแรกปักหมุดได้สูงสุด 24 รายการ",
        "ย้ายไปก่อนหน้า", "ย้ายไปถัดไป", "เพลง", "อัลบั้ม", "ศิลปิน", "เพลย์ลิสต์", "กดค้างแล้วลากเพื่อจัดลำดับใหม่"
    ),
    "fil" to speedDial(
        "Naka-pin", "I-pin sa Home", "Alisin sa Home", "Hanggang 24 na naka-pin ang kasya sa Home",
        "Ilipat pauna", "Ilipat pahuli", "Kanta", "Album", "Artist", "Playlist", "Pindutin nang matagal at i-drag para ayusin"
    ),
    "he" to speedDial(
        "נעוצים", "נעיצה בדף הבית", "ביטול נעיצה מדף הבית", "אפשר לנעוץ עד 24 פריטים בדף הבית",
        "הזזה מוקדם יותר", "הזזה מאוחר יותר", "שיר", "אלבום", "אמן", "פלייליסט", "לחיצה ארוכה וגרירה לשינוי הסדר"
    ),
    "fi" to speedDial(
        "Kiinnitetyt", "Kiinnitä etusivulle", "Irrota etusivulta", "Etusivulle voi kiinnittää enintään 24 kohdetta",
        "Siirrä aiemmaksi", "Siirrä myöhemmäksi", "Kappale", "Albumi", "Artisti", "Soittolista", "Pidä painettuna ja vedä järjestääksesi"
    ),
    "et" to speedDial(
        "Kinnitatud", "Kinnita avalehele", "Eemalda avalehelt", "Avalehele saab kinnitada kuni 24 üksust",
        "Liiguta ettepoole", "Liiguta tahapoole", "Lugu", "Album", "Artist", "Esitusloend", "Hoia all ja lohista järjestuse muutmiseks"
    )
)

internal fun speedDialLocalizationCodes(): Set<String> = speedDialCopies.keys

fun LevyraStrings.speedDialCopy(): SpeedDialCopy = speedDialCopies[code] ?: speedDialCopies.getValue("en")
