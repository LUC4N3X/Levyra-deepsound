package com.luc4n3x.levyra.ui.i18n

internal data class OnboardingArtistCopy(val title: String, val body: String, val empty: String)

internal object LevyraOnboardingArtistStrings {
    private val copies = mapOf(
        "en" to OnboardingArtistCopy("Your music starts here", "Choose at least 3 artists you love. We'll shape your discoveries around them.", "No artists found in these suggestions."),
        "it" to OnboardingArtistCopy("La tua musica, da qui", "Scegli almeno 3 artisti che ami. Le tue scoperte partiranno da loro.", "Nessun artista trovato tra questi suggerimenti."),
        "es" to OnboardingArtistCopy("Tu música empieza aquí", "Elige al menos 3 artistas que te gusten. Tus descubrimientos empezarán con ellos.", "No se encontraron artistas entre estas sugerencias."),
        "fr" to OnboardingArtistCopy("Ta musique commence ici", "Choisis au moins 3 artistes que tu aimes. Ils guideront tes découvertes.", "Aucun artiste trouvé parmi ces suggestions."),
        "de" to OnboardingArtistCopy("Hier beginnt deine Musik", "Wähle mindestens 3 Künstler, die du liebst. Sie prägen deine Entdeckungen.", "Keine Künstler in diesen Vorschlägen gefunden."),
        "pt" to OnboardingArtistCopy("A tua música começa aqui", "Escolhe pelo menos 3 artistas de que gostas. As tuas descobertas começam com eles.", "Nenhum artista encontrado nestas sugestões."),
        "pt-BR" to OnboardingArtistCopy("Sua música começa aqui", "Escolha pelo menos 3 artistas que você ama. Suas descobertas começam com eles.", "Nenhum artista encontrado nestas sugestões."),
        "nl" to OnboardingArtistCopy("Jouw muziek begint hier", "Kies minstens 3 artiesten die je leuk vindt. Zij vormen je ontdekkingen.", "Geen artiesten gevonden in deze suggesties."),
        "pl" to OnboardingArtistCopy("Tu zaczyna się twoja muzyka", "Wybierz co najmniej 3 ulubionych artystów. Od nich zaczną się twoje odkrycia.", "Nie znaleziono artystów w tych propozycjach."),
        "ro" to OnboardingArtistCopy("Muzica ta începe aici", "Alege cel puțin 3 artiști preferați. Ei îți vor ghida descoperirile.", "Niciun artist găsit în aceste sugestii."),
        "el" to OnboardingArtistCopy("Η μουσική σου ξεκινά εδώ", "Διάλεξε τουλάχιστον 3 αγαπημένους καλλιτέχνες. Θα καθοδηγήσουν τις ανακαλύψεις σου.", "Δεν βρέθηκαν καλλιτέχνες σε αυτές τις προτάσεις."),
        "sv" to OnboardingArtistCopy("Din musik börjar här", "Välj minst 3 artister du gillar. De formar dina upptäckter.", "Inga artister hittades bland dessa förslag."),
        "da" to OnboardingArtistCopy("Din musik starter her", "Vælg mindst 3 kunstnere, du elsker. De former dine opdagelser.", "Ingen kunstnere fundet i disse forslag."),
        "cs" to OnboardingArtistCopy("Tvoje hudba začíná tady", "Vyber alespoň 3 oblíbené interprety. Podle nich ti nabídneme nové objevy.", "V těchto návrzích nebyli nalezeni žádní interpreti."),
        "sk" to OnboardingArtistCopy("Tvoja hudba začína tu", "Vyber aspoň 3 obľúbených interpretov. Podľa nich ti ponúkneme nové objavy.", "V týchto návrhoch sa nenašli žiadni interpreti."),
        "hr" to OnboardingArtistCopy("Tvoja glazba počinje ovdje", "Odaberi barem 3 omiljena izvođača. Oni će oblikovati tvoja otkrića.", "Nema izvođača među ovim prijedlozima."),
        "sr" to OnboardingArtistCopy("Твоја музика почиње овде", "Изабери бар 3 омиљена извођача. Они ће обликовати твоја открића.", "Нема извођача међу овим предлозима."),
        "sq" to OnboardingArtistCopy("Muzika jote fillon këtu", "Zgjidh të paktën 3 artistë që të pëlqejnë. Ata do të udhëheqin zbulimet e tua.", "Nuk u gjetën artistë në këto sugjerime."),
        "bg" to OnboardingArtistCopy("Твоята музика започва тук", "Избери поне 3 любими изпълнители. Те ще оформят твоите открития.", "Няма намерени изпълнители сред тези предложения."),
        "hu" to OnboardingArtistCopy("Itt kezdődik a zenéd", "Válassz legalább 3 kedvenc előadót. Ők alakítják majd a felfedezéseidet.", "Nem található előadó a javaslatok között."),
        "fi" to OnboardingArtistCopy("Musiikkisi alkaa tästä", "Valitse vähintään 3 suosikkiartistia. He ohjaavat uusia löytöjäsi.", "Näistä ehdotuksista ei löytynyt artisteja."),
        "et" to OnboardingArtistCopy("Sinu muusika algab siit", "Vali vähemalt 3 lemmikartisti. Nemad suunavad sinu avastusi.", "Nendest soovitustest ei leitud artiste."),
        "nb" to OnboardingArtistCopy("Musikken din starter her", "Velg minst 3 artister du liker. De former oppdagelsene dine.", "Ingen artister funnet blant disse forslagene."),
        "ca" to OnboardingArtistCopy("La teva música comença aquí", "Tria almenys 3 artistes que t'agradin. Els teus descobriments començaran amb ells.", "No s'han trobat artistes entre aquests suggeriments."),
        "uk" to OnboardingArtistCopy("Твоя музика починається тут", "Обери щонайменше 3 улюблених виконавців. Вони визначать твої відкриття.", "Серед цих пропозицій виконавців не знайдено."),
        "ru" to OnboardingArtistCopy("Твоя музыка начинается здесь", "Выбери хотя бы 3 любимых исполнителей. Они определят твои открытия.", "Среди этих предложений исполнители не найдены."),
        "kk" to OnboardingArtistCopy("Музыкаң осы жерден басталады", "Кемінде 3 сүйікті орындаушыңды таңда. Жаңа музыканы соларға қарай ұсынамыз.", "Бұл ұсыныстардан орындаушылар табылмады."),
        "tr" to OnboardingArtistCopy("Müziğin burada başlıyor", "Sevdiğin en az 3 sanatçıyı seç. Keşiflerini onlara göre şekillendirelim.", "Bu önerilerde sanatçı bulunamadı."),
        "ar" to OnboardingArtistCopy("موسيقاك تبدأ هنا", "اختر 3 فنانين تحبهم على الأقل. سنبني اكتشافاتك الموسيقية حولهم.", "لم يتم العثور على فنانين ضمن هذه الاقتراحات."),
        "fa" to OnboardingArtistCopy("موسیقی تو از اینجا شروع می‌شود", "حداقل ۳ هنرمند مورد علاقه‌ات را انتخاب کن. کشف‌های تازه‌ات را بر پایه آن‌ها می‌سازیم.", "هنرمندی در این پیشنهادها پیدا نشد."),
        "he" to OnboardingArtistCopy("המוזיקה שלך מתחילה כאן", "בחר לפחות 3 אמנים אהובים. הם יעצבו את התגליות שלך.", "לא נמצאו אמנים בהצעות האלה."),
        "zh" to OnboardingArtistCopy("你的音乐，从这里开始", "选择至少3位你喜欢的艺人，我们将据此为你推荐音乐。", "这些推荐中未找到艺人。"),
        "zh-Hant" to OnboardingArtistCopy("你的音樂，從這裡開始", "選擇至少3位你喜歡的藝人，我們將據此為你推薦音樂。", "這些推薦中未找到藝人。"),
        "ja" to OnboardingArtistCopy("あなたの音楽はここから", "好きなアーティストを3組以上選んでください。新しい音楽との出会いにつながります。", "この候補に該当するアーティストはいません。"),
        "ko" to OnboardingArtistCopy("당신의 음악이 시작되는 곳", "좋아하는 아티스트를 3명 이상 선택하세요. 취향에 맞는 새로운 음악을 추천해 드릴게요.", "이 추천 목록에서 아티스트를 찾을 수 없습니다."),
        "hi" to OnboardingArtistCopy("आपका संगीत यहीं से शुरू होता है", "कम से कम 3 पसंदीदा कलाकार चुनें। हम उनके आधार पर नया संगीत सुझाएँगे।", "इन सुझावों में कोई कलाकार नहीं मिला।"),
        "bn" to OnboardingArtistCopy("আপনার সংগীত শুরু এখানেই", "অন্তত ৩ জন প্রিয় শিল্পী বেছে নিন। তাঁদের ভিত্তিতে নতুন গান সুপারিশ করব।", "এই পরামর্শগুলিতে কোনো শিল্পী পাওয়া যায়নি।"),
        "id" to OnboardingArtistCopy("Musikmu dimulai di sini", "Pilih minimal 3 artis favorit. Kami akan menyesuaikan penemuan musikmu.", "Tidak ada artis dalam saran ini."),
        "ms" to OnboardingArtistCopy("Muzik anda bermula di sini", "Pilih sekurang-kurangnya 3 artis kegemaran. Kami akan membentuk penemuan muzik anda.", "Tiada artis ditemui dalam cadangan ini."),
        "vi" to OnboardingArtistCopy("Âm nhạc của bạn bắt đầu từ đây", "Chọn ít nhất 3 nghệ sĩ yêu thích. Chúng tôi sẽ gợi ý nhạc dựa trên họ.", "Không tìm thấy nghệ sĩ trong các gợi ý này."),
        "th" to OnboardingArtistCopy("เพลงของคุณเริ่มต้นที่นี่", "เลือกศิลปินที่ชอบอย่างน้อย 3 คน เราจะแนะนำเพลงใหม่จากรสนิยมของคุณ", "ไม่พบศิลปินในคำแนะนำเหล่านี้"),
        "fil" to OnboardingArtistCopy("Dito nagsisimula ang musika mo", "Pumili ng kahit 3 paboritong artist. Iaangkop namin ang mga bagong tuklas mo sa kanila.", "Walang artist sa mga mungkahing ito.")
    )

    fun forCode(code: String): OnboardingArtistCopy = localizedValueOrEnglish(copies, code)
}
