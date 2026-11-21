package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal class LibraryEmptyCopy(private val values: List<String>) {
    fun brazilian(): LibraryEmptyCopy = LibraryEmptyCopy(values.map(LevyraBrazilianPortuguese::adapt))

    init { require(values.size == 4 && values.all(String::isNotBlank)) }
    val albumsTitle get() = values[0]
    val albumsDetail get() = values[1]
    val artistsTitle get() = values[2]
    val artistsDetail get() = values[3]
}

internal fun libraryEmptyCopy(code: String): LibraryEmptyCopy =
    if (isBrazilianPortuguese(code)) brazilianLibraryEmptyCopy else libraryEmptyCopies.getValue(translationKey(code))

private val brazilianLibraryEmptyCopy: LibraryEmptyCopy by lazy(LazyThreadSafetyMode.PUBLICATION) {
    libraryEmptyCopies.getValue("pt").brazilian()
}

private val libraryEmptyCopies = mapOf(
    "en" to listOf("No albums yet", "Save an album and it lands here.", "No artists yet", "Follow an artist to keep them close."),
    "it" to listOf("Ancora nessun album", "Salva un album e lo ritrovi qui.", "Ancora nessun artista", "Segui un artista per averlo sempre a portata."),
    "es" to listOf("Aún no hay álbumes", "Guarda un álbum y aparecerá aquí.", "Aún no hay artistas", "Sigue a un artista para tenerlo cerca."),
    "fr" to listOf("Aucun album pour l’instant", "Enregistre un album et il apparaîtra ici.", "Aucun artiste pour l’instant", "Suis un artiste pour le garder à portée."),
    "de" to listOf("Noch keine Alben", "Speichere ein Album und es landet hier.", "Noch keine Künstler", "Folge Künstlern, um sie hier zu haben."),
    "pt" to listOf("Ainda não há álbuns", "Guarde um álbum e ele aparece aqui.", "Ainda não há artistas", "Siga um artista para o ter sempre por perto."),
    "nl" to listOf("Nog geen albums", "Bewaar een album en het verschijnt hier.", "Nog geen artiesten", "Volg een artiest om hem dichtbij te houden."),
    "pl" to listOf("Brak albumów", "Zapisz album, a pojawi się tutaj.", "Brak wykonawców", "Obserwuj wykonawcę, aby mieć go pod ręką."),
    "ro" to listOf("Încă nu ai albume", "Salvează un album și apare aici.", "Încă nu ai artiști", "Urmărește un artist ca să-l ai aproape."),
    "el" to listOf("Δεν υπάρχουν ακόμη άλμπουμ", "Αποθήκευσε ένα άλμπουμ και θα εμφανιστεί εδώ.", "Δεν υπάρχουν ακόμη καλλιτέχνες", "Ακολούθησε έναν καλλιτέχνη για να τον έχεις κοντά."),
    "sv" to listOf("Inga album än", "Spara ett album så hamnar det här.", "Inga artister än", "Följ en artist för att ha den nära."),
    "da" to listOf("Ingen album endnu", "Gem et album, så lander det her.", "Ingen kunstnere endnu", "Følg en kunstner for at have den tæt på."),
    "cs" to listOf("Zatím žádná alba", "Ulož si album a objeví se tady.", "Zatím žádní interpreti", "Sleduj interpreta a měj ho po ruce."),
    "sk" to listOf("Zatiaľ žiadne albumy", "Ulož si album a objaví sa tu.", "Zatiaľ žiadni interpreti", "Sleduj interpreta a maj ho poruke."),
    "hr" to listOf("Još nema albuma", "Spremi album i pojavit će se ovdje.", "Još nema izvođača", "Prati izvođača da ti bude pri ruci."),
    "sr" to listOf("Још нема албума", "Сачувај албум и појавиће се овде.", "Још нема извођача", "Прати извођача да ти буде при руци."),
    "sq" to listOf("Ende nuk ka albume", "Ruaj një album dhe do të shfaqet këtu.", "Ende nuk ka artistë", "Ndiq një artist për ta pasur pranë."),
    "bg" to listOf("Все още няма албуми", "Запази албум и той ще се появи тук.", "Все още няма изпълнители", "Последвай изпълнител, за да е наблизо."),
    "hu" to listOf("Még nincsenek albumok", "Ments el egy albumot, és itt lesz.", "Még nincsenek előadók", "Kövess egy előadót, hogy kéznél legyen."),
    "fi" to listOf("Ei vielä albumeja", "Tallenna albumi, niin se näkyy täällä.", "Ei vielä artisteja", "Seuraa artistia, niin pidät sen lähellä."),
    "et" to listOf("Albumeid veel pole", "Salvesta album ja see ilmub siia.", "Esitajaid veel pole", "Jälgi esitajat, et ta oleks käepärast."),
    "nb" to listOf("Ingen album ennå", "Lagre et album, så havner det her.", "Ingen artister ennå", "Følg en artist for å ha den nær."),
    "ca" to listOf("Encara no hi ha àlbums", "Desa un àlbum i apareixerà aquí.", "Encara no hi ha artistes", "Segueix un artista per tenir-lo a prop."),
    "uk" to listOf("Ще немає альбомів", "Збережіть альбом, і він з’явиться тут.", "Ще немає виконавців", "Стежте за виконавцем, щоб він був поруч."),
    "ru" to listOf("Альбомов пока нет", "Сохраните альбом, и он появится здесь.", "Исполнителей пока нет", "Подпишитесь на исполнителя, чтобы он был под рукой."),
    "kk" to listOf("Әзірге альбом жоқ", "Альбомды сақтаңыз, ол осында пайда болады.", "Әзірге орындаушы жоқ", "Орындаушыны жақын ұстау үшін оған жазылыңыз."),
    "tr" to listOf("Henüz albüm yok", "Bir albümü kaydedin, burada görünsün.", "Henüz sanatçı yok", "Yakında tutmak için bir sanatçıyı takip edin."),
    "ar" to listOf("لا توجد ألبومات بعد", "احفظ ألبومًا وسيظهر هنا.", "لا يوجد فنانون بعد", "تابع فنانًا لتبقيه قريبًا منك."),
    "fa" to listOf("هنوز آلبومی نیست", "یک آلبوم ذخیره کنید تا اینجا بیاید.", "هنوز هنرمندی نیست", "هنرمندی را دنبال کنید تا همیشه دم دست باشد."),
    "zh" to listOf("还没有专辑", "保存专辑后会显示在这里。", "还没有艺人", "关注艺人，随时找到他们。"),
    "zh-Hant" to listOf("還沒有專輯", "儲存專輯後會顯示在這裡。", "還沒有藝人", "追蹤藝人，隨時找到他們。"),
    "ja" to listOf("アルバムはまだありません", "アルバムを保存するとここに表示されます。", "アーティストはまだいません", "アーティストをフォローするとすぐに見つかります。"),
    "ko" to listOf("아직 앨범이 없습니다", "앨범을 저장하면 여기에 표시됩니다.", "아직 아티스트가 없습니다", "아티스트를 팔로우하면 가까이 둘 수 있어요."),
    "hi" to listOf("अभी कोई एल्बम नहीं", "कोई एल्बम सहेजें और वह यहाँ दिखेगा।", "अभी कोई कलाकार नहीं", "किसी कलाकार को फ़ॉलो करें ताकि वह पास रहे।"),
    "bn" to listOf("এখনও কোনো অ্যালবাম নেই", "একটি অ্যালবাম সংরক্ষণ করুন, এখানে দেখা যাবে।", "এখনও কোনো শিল্পী নেই", "কাছে রাখতে একজন শিল্পীকে অনুসরণ করুন।"),
    "id" to listOf("Belum ada album", "Simpan album dan album itu akan muncul di sini.", "Belum ada artis", "Ikuti artis agar selalu dekat."),
    "ms" to listOf("Belum ada album", "Simpan album dan ia akan muncul di sini.", "Belum ada artis", "Ikuti artis supaya sentiasa dekat."),
    "vi" to listOf("Chưa có album nào", "Lưu một album và nó sẽ xuất hiện ở đây.", "Chưa có nghệ sĩ nào", "Theo dõi nghệ sĩ để luôn ở gần họ."),
    "th" to listOf("ยังไม่มีอัลบั้ม", "บันทึกอัลบั้มแล้วจะแสดงที่นี่", "ยังไม่มีศิลปิน", "ติดตามศิลปินเพื่อเก็บไว้ใกล้ตัว"),
    "fil" to listOf("Wala pang album", "Mag-save ng album at lalabas ito rito.", "Wala pang artist", "Sundan ang isang artist para laging malapit."),
    "he" to listOf("עדיין אין אלבומים", "שמרו אלבום והוא יופיע כאן.", "עדיין אין אמנים", "עקבו אחרי אמן כדי שיהיה קרוב.")
).mapValues { LibraryEmptyCopy(it.value) }.also { copies ->
    check(copies.keys == LevyraLanguageCatalog.languages.map { it.code }.toSet())
}
