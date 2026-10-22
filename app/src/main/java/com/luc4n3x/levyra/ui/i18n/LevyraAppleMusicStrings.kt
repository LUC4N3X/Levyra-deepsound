package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal data class AppleMusicSkinCopy(
    val editorialUnavailable: String,
    val editorialRetry: String,
    val editorialOffline: String,
    val updated: String,
    val more: String,
    val explicit: String,
    val songs: (Int) -> String,
    val collectionUnavailable: String
)

internal object LevyraAppleMusicStrings {
    private val copies = mapOf(
        "en" to AppleMusicSkinCopy(
            editorialUnavailable = "Editorial shelves are not available right now.",
            editorialRetry = "Try again",
            editorialOffline = "Showing the last saved editorial shelves.",
            updated = "Updated",
            more = "More",
            explicit = "Explicit",
            songs = { count -> if (count == 1) "1 song" else "${count} songs" },
            collectionUnavailable = "This list could not be loaded."
        ),
        "it" to AppleMusicSkinCopy(
            editorialUnavailable = "Le sezioni editoriali non sono disponibili in questo momento.",
            editorialRetry = "Riprova",
            editorialOffline = "Mostro le ultime sezioni editoriali salvate.",
            updated = "Aggiornato",
            more = "Altro",
            explicit = "Esplicito",
            songs = { count -> if (count == 1) "1 brano" else "${count} brani" },
            collectionUnavailable = "Impossibile caricare questo elenco."
        ),
        "es" to AppleMusicSkinCopy(
            editorialUnavailable = "Las secciones editoriales no están disponibles ahora.",
            editorialRetry = "Reintentar",
            editorialOffline = "Mostrando las últimas secciones editoriales guardadas.",
            updated = "Actualizado",
            more = "Más",
            explicit = "Explícito",
            songs = { count -> if (count == 1) "1 canción" else "${count} canciones" },
            collectionUnavailable = "No se pudo cargar esta lista."
        ),
        "fr" to AppleMusicSkinCopy(
            editorialUnavailable = "Les rubriques éditoriales ne sont pas disponibles pour le moment.",
            editorialRetry = "Réessayer",
            editorialOffline = "Affichage des dernières rubriques éditoriales enregistrées.",
            updated = "Mis à jour",
            more = "Plus",
            explicit = "Explicite",
            songs = { count -> if (count == 1) "1 titre" else "${count} titres" },
            collectionUnavailable = "Impossible de charger cette liste."
        ),
        "de" to AppleMusicSkinCopy(
            editorialUnavailable = "Redaktionelle Bereiche sind gerade nicht verfügbar.",
            editorialRetry = "Erneut versuchen",
            editorialOffline = "Die zuletzt gespeicherten redaktionellen Bereiche werden angezeigt.",
            updated = "Aktualisiert",
            more = "Mehr",
            explicit = "Explizit",
            songs = { count -> if (count == 1) "1 Song" else "${count} Songs" },
            collectionUnavailable = "Diese Liste konnte nicht geladen werden."
        ),
        "pt" to AppleMusicSkinCopy(
            editorialUnavailable = "As seções editoriais não estão disponíveis agora.",
            editorialRetry = "Tentar novamente",
            editorialOffline = "Mostrando as últimas seções editoriais salvas.",
            updated = "Atualizado",
            more = "Mais",
            explicit = "Explícito",
            songs = { count -> if (count == 1) "1 música" else "${count} músicas" },
            collectionUnavailable = "Não foi possível carregar esta lista."
        ),
        "nl" to AppleMusicSkinCopy(
            editorialUnavailable = "Redactionele secties zijn nu niet beschikbaar.",
            editorialRetry = "Opnieuw proberen",
            editorialOffline = "De laatst opgeslagen redactionele secties worden getoond.",
            updated = "Bijgewerkt",
            more = "Meer",
            explicit = "Expliciet",
            songs = { count -> if (count == 1) "1 nummer" else "${count} nummers" },
            collectionUnavailable = "Deze lijst kan niet worden geladen."
        ),
        "pl" to AppleMusicSkinCopy(
            editorialUnavailable = "Sekcje redakcyjne są teraz niedostępne.",
            editorialRetry = "Spróbuj ponownie",
            editorialOffline = "Wyświetlane są ostatnio zapisane sekcje redakcyjne.",
            updated = "Zaktualizowano",
            more = "Więcej",
            explicit = "Treści dla dorosłych",
            songs = { count -> if (count == 1) "Utwory: 1" else "Utwory: ${count}" },
            collectionUnavailable = "Nie udało się wczytać tej listy."
        ),
        "ru" to AppleMusicSkinCopy(
            editorialUnavailable = "Редакционные подборки сейчас недоступны.",
            editorialRetry = "Повторить",
            editorialOffline = "Показаны последние сохранённые редакционные подборки.",
            updated = "Обновлено",
            more = "Ещё",
            explicit = "Нецензурное содержание",
            songs = { count -> if (count == 1) "Треков: 1" else "Треков: ${count}" },
            collectionUnavailable = "Не удалось загрузить этот список."
        ),
        "uk" to AppleMusicSkinCopy(
            editorialUnavailable = "Редакційні добірки зараз недоступні.",
            editorialRetry = "Повторити",
            editorialOffline = "Показано останні збережені редакційні добірки.",
            updated = "Оновлено",
            more = "Ще",
            explicit = "Відвертий вміст",
            songs = { count -> if (count == 1) "Треків: 1" else "Треків: ${count}" },
            collectionUnavailable = "Не вдалося завантажити цей список."
        ),
        "ja" to AppleMusicSkinCopy(
            editorialUnavailable = "現在、編集部のセクションは利用できません。",
            editorialRetry = "再試行",
            editorialOffline = "最後に保存した編集部のセクションを表示しています。",
            updated = "更新",
            more = "その他",
            explicit = "不適切な表現",
            songs = { count -> if (count == 1) "1 曲" else "${count} 曲" },
            collectionUnavailable = "このリストを読み込めませんでした。"
        ),
        "ko" to AppleMusicSkinCopy(
            editorialUnavailable = "지금은 에디토리얼 섹션을 사용할 수 없습니다.",
            editorialRetry = "다시 시도",
            editorialOffline = "마지막으로 저장된 에디토리얼 섹션을 표시합니다.",
            updated = "업데이트됨",
            more = "더보기",
            explicit = "청소년 유해",
            songs = { count -> if (count == 1) "1곡" else "${count}곡" },
            collectionUnavailable = "이 목록을 불러올 수 없습니다."
        ),
        "zh" to AppleMusicSkinCopy(
            editorialUnavailable = "编辑推荐栏目暂时不可用。",
            editorialRetry = "重试",
            editorialOffline = "正在显示上次保存的编辑推荐栏目。",
            updated = "已更新",
            more = "更多",
            explicit = "露骨内容",
            songs = { count -> if (count == 1) "1 首歌曲" else "${count} 首歌曲" },
            collectionUnavailable = "无法加载此列表。"
        ),
        "zh-Hant" to AppleMusicSkinCopy(
            editorialUnavailable = "編輯推薦欄目暫時無法使用。",
            editorialRetry = "重試",
            editorialOffline = "正在顯示上次儲存的編輯推薦欄目。",
            updated = "已更新",
            more = "更多",
            explicit = "露骨內容",
            songs = { count -> if (count == 1) "1 首歌曲" else "${count} 首歌曲" },
            collectionUnavailable = "無法載入此清單。"
        ),
        "ar" to AppleMusicSkinCopy(
            editorialUnavailable = "الأقسام التحريرية غير متاحة الآن.",
            editorialRetry = "إعادة المحاولة",
            editorialOffline = "يتم عرض آخر الأقسام التحريرية المحفوظة.",
            updated = "تم التحديث",
            more = "المزيد",
            explicit = "محتوى فاضح",
            songs = { count -> if (count == 1) "عدد الأغاني: 1" else "عدد الأغاني: ${count}" },
            collectionUnavailable = "تعذّر تحميل هذه القائمة."
        ),
        "ro" to AppleMusicSkinCopy(
            editorialUnavailable = "Secțiunile editoriale nu sunt disponibile acum.",
            editorialRetry = "Încearcă din nou",
            editorialOffline = "Se afișează ultimele secțiuni editoriale salvate.",
            updated = "Actualizat",
            more = "Mai mult",
            explicit = "Explicit",
            songs = { count -> if (count == 1) "1 melodie" else "Melodii: ${count}" },
            collectionUnavailable = "Lista nu a putut fi încărcată."
        ),
        "sq" to AppleMusicSkinCopy(
            editorialUnavailable = "Seksionet editoriale nuk janë të disponueshme tani.",
            editorialRetry = "Provo përsëri",
            editorialOffline = "Po shfaqen seksionet editoriale të ruajtura së fundi.",
            updated = "Përditësuar",
            more = "Më shumë",
            explicit = "Eksplicite",
            songs = { count -> if (count == 1) "1 këngë" else "${count} këngë" },
            collectionUnavailable = "Kjo listë nuk mund të ngarkohej."
        ),
        "bg" to AppleMusicSkinCopy(
            editorialUnavailable = "Редакционните раздели не са достъпни в момента.",
            editorialRetry = "Опитай отново",
            editorialOffline = "Показват се последно запазените редакционни раздели.",
            updated = "Актуализирано",
            more = "Още",
            explicit = "Неприлично съдържание",
            songs = { count -> if (count == 1) "1 песен" else "${count} песни" },
            collectionUnavailable = "Списъкът не можа да се зареди."
        ),
        "el" to AppleMusicSkinCopy(
            editorialUnavailable = "Οι ενότητες επιμέλειας δεν είναι διαθέσιμες αυτή τη στιγμή.",
            editorialRetry = "Δοκιμάστε ξανά",
            editorialOffline = "Εμφανίζονται οι τελευταίες αποθηκευμένες ενότητες επιμέλειας.",
            updated = "Ενημερώθηκε",
            more = "Περισσότερα",
            explicit = "Ακατάλληλο",
            songs = { count -> if (count == 1) "1 τραγούδι" else "${count} τραγούδια" },
            collectionUnavailable = "Δεν ήταν δυνατή η φόρτωση αυτής της λίστας."
        ),
        "sv" to AppleMusicSkinCopy(
            editorialUnavailable = "Redaktionella sektioner är inte tillgängliga just nu.",
            editorialRetry = "Försök igen",
            editorialOffline = "Visar de senast sparade redaktionella sektionerna.",
            updated = "Uppdaterad",
            more = "Mer",
            explicit = "Explicit",
            songs = { count -> if (count == 1) "1 låt" else "${count} låtar" },
            collectionUnavailable = "Listan kunde inte läsas in."
        ),
        "da" to AppleMusicSkinCopy(
            editorialUnavailable = "Redaktionelle sektioner er ikke tilgængelige lige nu.",
            editorialRetry = "Prøv igen",
            editorialOffline = "Viser de senest gemte redaktionelle sektioner.",
            updated = "Opdateret",
            more = "Mere",
            explicit = "Eksplicit",
            songs = { count -> if (count == 1) "1 sang" else "${count} sange" },
            collectionUnavailable = "Listen kunne ikke indlæses."
        ),
        "cs" to AppleMusicSkinCopy(
            editorialUnavailable = "Redakční sekce teď nejsou k dispozici.",
            editorialRetry = "Zkusit znovu",
            editorialOffline = "Zobrazují se naposledy uložené redakční sekce.",
            updated = "Aktualizováno",
            more = "Další",
            explicit = "Explicitní",
            songs = { count -> if (count == 1) "Skladby: 1" else "Skladby: ${count}" },
            collectionUnavailable = "Tento seznam se nepodařilo načíst."
        ),
        "sk" to AppleMusicSkinCopy(
            editorialUnavailable = "Redakčné sekcie momentálne nie sú k dispozícii.",
            editorialRetry = "Skúsiť znova",
            editorialOffline = "Zobrazujú sa naposledy uložené redakčné sekcie.",
            updated = "Aktualizované",
            more = "Viac",
            explicit = "Explicitné",
            songs = { count -> if (count == 1) "Skladby: 1" else "Skladby: ${count}" },
            collectionUnavailable = "Tento zoznam sa nepodarilo načítať."
        ),
        "bn" to AppleMusicSkinCopy(
            editorialUnavailable = "সম্পাদকীয় বিভাগ এখন উপলভ্য নয়।",
            editorialRetry = "আবার চেষ্টা করুন",
            editorialOffline = "সর্বশেষ সংরক্ষিত সম্পাদকীয় বিভাগ দেখানো হচ্ছে।",
            updated = "আপডেট হয়েছে",
            more = "আরও",
            explicit = "অশ্লীল",
            songs = { count -> if (count == 1) "১টি গান" else "${count}টি গান" },
            collectionUnavailable = "এই তালিকা লোড করা যায়নি।"
        ),
        "kk" to AppleMusicSkinCopy(
            editorialUnavailable = "Редакциялық бөлімдер қазір қолжетімсіз.",
            editorialRetry = "Қайталау",
            editorialOffline = "Соңғы сақталған редакциялық бөлімдер көрсетілуде.",
            updated = "Жаңартылды",
            more = "Тағы",
            explicit = "Әдепсіз мазмұн",
            songs = { count -> if (count == 1) "Әндер: 1" else "Әндер: ${count}" },
            collectionUnavailable = "Бұл тізімді жүктеу мүмкін болмады."
        ),
        "sr" to AppleMusicSkinCopy(
            editorialUnavailable = "Уреднички одељци тренутно нису доступни.",
            editorialRetry = "Покушај поново",
            editorialOffline = "Приказују се последњи сачувани уреднички одељци.",
            updated = "Ажурирано",
            more = "Још",
            explicit = "Експлицитно",
            songs = { count -> if (count == 1) "Песме: 1" else "Песме: ${count}" },
            collectionUnavailable = "Ова листа није могла да се учита."
        ),
        "hr" to AppleMusicSkinCopy(
            editorialUnavailable = "Urednički odjeljci trenutačno nisu dostupni.",
            editorialRetry = "Pokušaj ponovno",
            editorialOffline = "Prikazuju se posljednji spremljeni urednički odjeljci.",
            updated = "Ažurirano",
            more = "Više",
            explicit = "Eksplicitno",
            songs = { count -> if (count == 1) "Pjesme: 1" else "Pjesme: ${count}" },
            collectionUnavailable = "Ovaj popis nije se mogao učitati."
        ),
        "hu" to AppleMusicSkinCopy(
            editorialUnavailable = "A szerkesztői szekciók jelenleg nem érhetők el.",
            editorialRetry = "Újra",
            editorialOffline = "A legutóbb mentett szerkesztői szekciók láthatók.",
            updated = "Frissítve",
            more = "Továbbiak",
            explicit = "Szókimondó",
            songs = { count -> if (count == 1) "1 dal" else "${count} dal" },
            collectionUnavailable = "Nem sikerült betölteni ezt a listát."
        ),
        "fi" to AppleMusicSkinCopy(
            editorialUnavailable = "Toimitukselliset osiot eivät ole nyt saatavilla.",
            editorialRetry = "Yritä uudelleen",
            editorialOffline = "Näytetään viimeksi tallennetut toimitukselliset osiot.",
            updated = "Päivitetty",
            more = "Lisää",
            explicit = "Sopimaton",
            songs = { count -> if (count == 1) "1 kappale" else "${count} kappaletta" },
            collectionUnavailable = "Luetteloa ei voitu ladata."
        ),
        "et" to AppleMusicSkinCopy(
            editorialUnavailable = "Toimetuse rubriigid pole praegu saadaval.",
            editorialRetry = "Proovi uuesti",
            editorialOffline = "Kuvatakse viimati salvestatud toimetuse rubriike.",
            updated = "Uuendatud",
            more = "Veel",
            explicit = "Ropp",
            songs = { count -> if (count == 1) "1 lugu" else "${count} lugu" },
            collectionUnavailable = "Seda loendit ei õnnestunud laadida."
        ),
        "nb" to AppleMusicSkinCopy(
            editorialUnavailable = "Redaksjonelle seksjoner er ikke tilgjengelige akkurat nå.",
            editorialRetry = "Prøv igjen",
            editorialOffline = "Viser de sist lagrede redaksjonelle seksjonene.",
            updated = "Oppdatert",
            more = "Mer",
            explicit = "Eksplisitt",
            songs = { count -> if (count == 1) "1 sang" else "${count} sanger" },
            collectionUnavailable = "Listen kunne ikke lastes inn."
        ),
        "ca" to AppleMusicSkinCopy(
            editorialUnavailable = "Les seccions editorials no estan disponibles ara mateix.",
            editorialRetry = "Torna-ho a provar",
            editorialOffline = "Es mostren les últimes seccions editorials desades.",
            updated = "Actualitzat",
            more = "Més",
            explicit = "Explícit",
            songs = { count -> if (count == 1) "1 cançó" else "${count} cançons" },
            collectionUnavailable = "No s'ha pogut carregar aquesta llista."
        ),
        "tr" to AppleMusicSkinCopy(
            editorialUnavailable = "Editoryal bölümler şu anda kullanılamıyor.",
            editorialRetry = "Tekrar dene",
            editorialOffline = "Son kaydedilen editoryal bölümler gösteriliyor.",
            updated = "Güncellendi",
            more = "Diğer",
            explicit = "Müstehcen",
            songs = { count -> if (count == 1) "1 şarkı" else "${count} şarkı" },
            collectionUnavailable = "Bu liste yüklenemedi."
        ),
        "fa" to AppleMusicSkinCopy(
            editorialUnavailable = "بخش‌های گزیده در حال حاضر در دسترس نیستند.",
            editorialRetry = "تلاش دوباره",
            editorialOffline = "آخرین بخش‌های گزیدهٔ ذخیره‌شده نمایش داده می‌شود.",
            updated = "به‌روزرسانی شد",
            more = "بیشتر",
            explicit = "صریح",
            songs = { count -> if (count == 1) "۱ آهنگ" else "${count} آهنگ" },
            collectionUnavailable = "این فهرست بارگیری نشد."
        ),
        "hi" to AppleMusicSkinCopy(
            editorialUnavailable = "संपादकीय सेक्शन अभी उपलब्ध नहीं हैं.",
            editorialRetry = "फिर से कोशिश करें",
            editorialOffline = "आखिरी बार सेव किए गए संपादकीय सेक्शन दिखाए जा रहे हैं.",
            updated = "अपडेट किया गया",
            more = "ज़्यादा",
            explicit = "अश्लील",
            songs = { count -> if (count == 1) "1 गाना" else "${count} गाने" },
            collectionUnavailable = "यह सूची लोड नहीं हो सकी."
        ),
        "id" to AppleMusicSkinCopy(
            editorialUnavailable = "Bagian editorial tidak tersedia saat ini.",
            editorialRetry = "Coba lagi",
            editorialOffline = "Menampilkan bagian editorial terakhir yang disimpan.",
            updated = "Diperbarui",
            more = "Lainnya",
            explicit = "Eksplisit",
            songs = { count -> if (count == 1) "1 lagu" else "${count} lagu" },
            collectionUnavailable = "Daftar ini tidak dapat dimuat."
        ),
        "ms" to AppleMusicSkinCopy(
            editorialUnavailable = "Bahagian editorial tidak tersedia sekarang.",
            editorialRetry = "Cuba lagi",
            editorialOffline = "Menunjukkan bahagian editorial terakhir yang disimpan.",
            updated = "Dikemas kini",
            more = "Lagi",
            explicit = "Eksplisit",
            songs = { count -> if (count == 1) "1 lagu" else "${count} lagu" },
            collectionUnavailable = "Senarai ini tidak dapat dimuatkan."
        ),
        "vi" to AppleMusicSkinCopy(
            editorialUnavailable = "Các mục biên tập hiện không khả dụng.",
            editorialRetry = "Thử lại",
            editorialOffline = "Đang hiển thị các mục biên tập đã lưu gần nhất.",
            updated = "Đã cập nhật",
            more = "Thêm",
            explicit = "Nội dung phản cảm",
            songs = { count -> if (count == 1) "1 bài hát" else "${count} bài hát" },
            collectionUnavailable = "Không thể tải danh sách này."
        ),
        "th" to AppleMusicSkinCopy(
            editorialUnavailable = "ส่วนแนะนำจากบรรณาธิการไม่พร้อมใช้งานในขณะนี้",
            editorialRetry = "ลองอีกครั้ง",
            editorialOffline = "กำลังแสดงส่วนแนะนำที่บันทึกไว้ล่าสุด",
            updated = "อัปเดตแล้ว",
            more = "เพิ่มเติม",
            explicit = "ไม่เหมาะสม",
            songs = { count -> if (count == 1) "1 เพลง" else "${count} เพลง" },
            collectionUnavailable = "โหลดรายการนี้ไม่ได้"
        ),
        "fil" to AppleMusicSkinCopy(
            editorialUnavailable = "Hindi available ngayon ang mga editorial na seksyon.",
            editorialRetry = "Subukan ulit",
            editorialOffline = "Ipinapakita ang mga huling na-save na editorial na seksyon.",
            updated = "Na-update",
            more = "Higit pa",
            explicit = "Explicit",
            songs = { count -> if (count == 1) "1 kanta" else "${count} kanta" },
            collectionUnavailable = "Hindi ma-load ang listahang ito."
        ),
        "he" to AppleMusicSkinCopy(
            editorialUnavailable = "המדורים המערכתיים לא זמינים כרגע.",
            editorialRetry = "ניסיון חוזר",
            editorialOffline = "מוצגים המדורים המערכתיים האחרונים שנשמרו.",
            updated = "עודכן",
            more = "עוד",
            explicit = "בוטה",
            songs = { count -> if (count == 1) "שיר אחד" else "${count} שירים" },
            collectionUnavailable = "לא ניתן היה לטעון את הרשימה הזו."
        )
    )

    fun forCode(code: String): AppleMusicSkinCopy = copies[LevyraLanguageCatalog.normalize(code)] ?: copies.getValue("en")
}
