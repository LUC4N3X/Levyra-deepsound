package com.luc4n3x.levyra.ui.i18n

private val experienceKeyList = listOf(
    "levyraMix",
    "mixCreate",
    "mixStartRadio",
    "mixFamiliarLabel",
    "mixDiscoveryLabel",
    "surpriseMe",
    "saveSelection",
    "mixUnavailable",
    "yourSound",
    "yourSoundSubtitle",
    "dnaPeriodWeek",
    "dnaPeriodMonth",
    "dnaPeriodHalfYear",
    "dnaPeriodAll",
    "dnaDiscovery",
    "dnaRhythm",
    "dnaEmpty",
    "discoverMore"
)

internal val experienceKeys: Set<String> = experienceKeyList.toSet()

private fun experience(vararg values: String): Map<String, String> {
    require(values.size == experienceKeyList.size) {
        "Expected ${experienceKeyList.size} experience strings, received ${values.size}"
    }
    return experienceKeyList.zip(values.asList()).toMap()
}

private val experienceBundles: Map<String, Map<String, String>> = mapOf(
    "en" to experience("Levyra Mix", "Create a mix", "Start radio", "Familiar", "Discovery", "Surprise me", "Save this selection", "No mix could be built right now.", "Your Sound", "How you actually listen", "7 days", "30 days", "6 months", "All time", "New music", "Listening rhythm", "Listen to a few tracks and your sound will appear here.", "Discover more"),
    "it" to experience("Levyra Mix", "Crea un mix", "Avvia radio", "Familiare", "Scoperta", "Sorprendimi", "Salva questa selezione", "Non è stato possibile creare un mix ora.", "Il tuo suono", "Come ascolti davvero", "7 giorni", "30 giorni", "6 mesi", "Sempre", "Musica nuova", "Ritmo d'ascolto", "Ascolta qualche brano e il tuo suono comparirà qui.", "Scopri altro"),
    "es" to experience("Levyra Mix", "Crear una mezcla", "Iniciar radio", "Familiar", "Descubrimiento", "Sorpréndeme", "Guardar esta selección", "No se pudo crear una mezcla ahora.", "Tu sonido", "Cómo escuchas de verdad", "7 días", "30 días", "6 meses", "Siempre", "Música nueva", "Ritmo de escucha", "Escucha algunas canciones y tu sonido aparecerá aquí.", "Descubrir más"),
    "fr" to experience("Levyra Mix", "Créer un mix", "Lancer la radio", "Familier", "Découverte", "Surprends-moi", "Enregistrer cette sélection", "Impossible de créer un mix pour le moment.", "Ton son", "Ta façon d'écouter", "7 jours", "30 jours", "6 mois", "Toujours", "Nouveautés", "Rythme d'écoute", "Écoute quelques titres et ton son apparaîtra ici.", "Découvrir plus"),
    "de" to experience("Levyra Mix", "Mix erstellen", "Radio starten", "Vertraut", "Entdeckung", "Überrasch mich", "Diese Auswahl speichern", "Es konnte gerade kein Mix erstellt werden.", "Dein Sound", "Wie du wirklich hörst", "7 Tage", "30 Tage", "6 Monate", "Gesamt", "Neue Musik", "Hörrhythmus", "Höre ein paar Titel und dein Sound erscheint hier.", "Mehr entdecken"),
    "pt" to experience("Levyra Mix", "Criar uma mistura", "Iniciar rádio", "Familiar", "Descoberta", "Surpreende-me", "Guardar esta seleção", "Não foi possível criar uma mistura agora.", "O teu som", "Como ouves na verdade", "7 dias", "30 dias", "6 meses", "Sempre", "Música nova", "Ritmo de audição", "Ouve algumas faixas e o teu som aparecerá aqui.", "Descobrir mais"),
    "nl" to experience("Levyra Mix", "Mix maken", "Radio starten", "Vertrouwd", "Ontdekking", "Verras me", "Deze selectie opslaan", "Er kon nu geen mix worden gemaakt.", "Jouw geluid", "Hoe je echt luistert", "7 dagen", "30 dagen", "6 maanden", "Altijd", "Nieuwe muziek", "Luisterritme", "Luister naar een paar nummers en je geluid verschijnt hier.", "Meer ontdekken"),
    "pl" to experience("Levyra Mix", "Utwórz miks", "Włącz radio", "Znajome", "Odkrycia", "Zaskocz mnie", "Zapisz ten wybór", "Nie udało się teraz utworzyć miksu.", "Twoje brzmienie", "Jak naprawdę słuchasz", "7 dni", "30 dni", "6 miesięcy", "Cały czas", "Nowa muzyka", "Rytm słuchania", "Posłuchaj kilku utworów, a Twoje brzmienie pojawi się tutaj.", "Odkryj więcej"),
    "ro" to experience("Levyra Mix", "Creează un mix", "Pornește radioul", "Familiar", "Descoperire", "Surprinde-mă", "Salvează această selecție", "Nu s-a putut crea un mix acum.", "Sunetul tău", "Cum asculți cu adevărat", "7 zile", "30 de zile", "6 luni", "Dintotdeauna", "Muzică nouă", "Ritm de ascultare", "Ascultă câteva piese și sunetul tău va apărea aici.", "Descoperă mai mult"),
    "el" to experience("Levyra Mix", "Δημιουργία mix", "Έναρξη ραδιοφώνου", "Οικείο", "Ανακάλυψη", "Έκπληξέ με", "Αποθήκευση επιλογής", "Δεν ήταν δυνατή η δημιουργία mix τώρα.", "Ο ήχος σου", "Πώς ακούς πραγματικά", "7 ημέρες", "30 ημέρες", "6 μήνες", "Πάντα", "Νέα μουσική", "Ρυθμός ακρόασης", "Άκουσε μερικά κομμάτια και ο ήχος σου θα εμφανιστεί εδώ.", "Ανακάλυψε περισσότερα"),
    "sv" to experience("Levyra Mix", "Skapa en mix", "Starta radio", "Bekant", "Upptäckt", "Överraska mig", "Spara det här urvalet", "Det gick inte att skapa en mix just nu.", "Ditt ljud", "Hur du faktiskt lyssnar", "7 dagar", "30 dagar", "6 månader", "Alltid", "Ny musik", "Lyssningsrytm", "Lyssna på några låtar så visas ditt ljud här.", "Upptäck mer"),
    "da" to experience("Levyra Mix", "Opret et mix", "Start radio", "Velkendt", "Opdagelse", "Overrask mig", "Gem dette udvalg", "Der kunne ikke oprettes et mix lige nu.", "Din lyd", "Sådan lytter du faktisk", "7 dage", "30 dage", "6 måneder", "Altid", "Ny musik", "Lytterytme", "Lyt til et par numre, så vises din lyd her.", "Opdag mere"),
    "cs" to experience("Levyra Mix", "Vytvořit mix", "Spustit rádio", "Známé", "Objevování", "Překvap mě", "Uložit tento výběr", "Mix se teď nepodařilo vytvořit.", "Tvůj zvuk", "Jak doopravdy posloucháš", "7 dní", "30 dní", "6 měsíců", "Vždy", "Nová hudba", "Rytmus poslechu", "Poslechni si pár skladeb a tvůj zvuk se objeví tady.", "Objevit více"),
    "uk" to experience("Levyra Mix", "Створити мікс", "Увімкнути радіо", "Знайоме", "Відкриття", "Здивуй мене", "Зберегти цю добірку", "Не вдалося створити мікс зараз.", "Твоє звучання", "Як ти слухаєш насправді", "7 днів", "30 днів", "6 місяців", "Увесь час", "Нова музика", "Ритм прослуховування", "Послухай кілька треків — і твоє звучання з'явиться тут.", "Відкрити більше"),
    "ru" to experience("Levyra Mix", "Создать микс", "Включить радио", "Знакомое", "Открытия", "Удиви меня", "Сохранить эту подборку", "Сейчас не удалось создать микс.", "Твоё звучание", "Как ты слушаешь на самом деле", "7 дней", "30 дней", "6 месяцев", "За всё время", "Новая музыка", "Ритм прослушивания", "Послушай несколько треков — и твоё звучание появится здесь.", "Открыть больше"),
    "tr" to experience("Levyra Mix", "Mix oluştur", "Radyoyu başlat", "Tanıdık", "Keşif", "Beni şaşırt", "Bu seçimi kaydet", "Şu anda bir mix oluşturulamadı.", "Senin sesin", "Gerçekte nasıl dinliyorsun", "7 gün", "30 gün", "6 ay", "Tüm zamanlar", "Yeni müzik", "Dinleme ritmi", "Birkaç parça dinle, sesin burada belirsin.", "Daha fazla keşfet"),
    "ar" to experience("Levyra Mix", "إنشاء مزيج", "تشغيل الراديو", "مألوف", "اكتشاف", "فاجئني", "حفظ هذه المجموعة", "تعذّر إنشاء مزيج الآن.", "صوتك", "كيف تستمع فعلاً", "7 أيام", "30 يومًا", "6 أشهر", "كل الأوقات", "موسيقى جديدة", "إيقاع الاستماع", "استمع إلى بضعة مقاطع وسيظهر صوتك هنا.", "اكتشف المزيد"),
    "zh" to experience("Levyra Mix", "创建混音", "开始电台", "熟悉", "探索", "给我惊喜", "保存此精选", "暂时无法创建混音。", "你的声音", "你真实的聆听方式", "7 天", "30 天", "6 个月", "全部时间", "新音乐", "聆听节奏", "先听几首歌，你的声音就会出现在这里。", "发现更多"),
    "ja" to experience("Levyra Mix", "ミックスを作成", "ラジオを開始", "なじみ", "発見", "おまかせ", "この選曲を保存", "現在ミックスを作成できませんでした。", "あなたのサウンド", "実際の聴き方", "7日間", "30日間", "6か月", "全期間", "新しい音楽", "リスニングのリズム", "数曲聴くと、あなたのサウンドがここに表示されます。", "もっと発見"),
    "ko" to experience("Levyra Mix", "믹스 만들기", "라디오 시작", "익숙함", "발견", "깜짝 추천", "이 선곡 저장", "지금은 믹스를 만들 수 없습니다.", "당신의 사운드", "실제로 듣는 방식", "7일", "30일", "6개월", "전체 기간", "새로운 음악", "청취 리듬", "몇 곡만 들어 보면 당신의 사운드가 여기에 나타납니다.", "더 발견하기"),
    "hi" to experience("Levyra Mix", "मिक्स बनाएँ", "रेडियो शुरू करें", "जाना-पहचाना", "खोज", "मुझे चौंकाएँ", "यह चयन सहेजें", "अभी मिक्स नहीं बनाया जा सका।", "आपकी ध्वनि", "आप असल में कैसे सुनते हैं", "7 दिन", "30 दिन", "6 महीने", "हमेशा", "नया संगीत", "सुनने की लय", "कुछ गाने सुनें और आपकी ध्वनि यहाँ दिखेगी।", "और खोजें"),
    "id" to experience("Levyra Mix", "Buat mix", "Mulai radio", "Familier", "Penemuan", "Kejutkan aku", "Simpan pilihan ini", "Mix tidak dapat dibuat sekarang.", "Suaramu", "Cara kamu benar-benar mendengarkan", "7 hari", "30 hari", "6 bulan", "Sepanjang waktu", "Musik baru", "Ritme mendengarkan", "Dengarkan beberapa lagu dan suaramu akan muncul di sini.", "Temukan lainnya"),
    "vi" to experience("Levyra Mix", "Tạo mix", "Bật radio", "Quen thuộc", "Khám phá", "Gây bất ngờ cho tôi", "Lưu tuyển chọn này", "Hiện chưa thể tạo mix.", "Âm thanh của bạn", "Cách bạn thực sự nghe nhạc", "7 ngày", "30 ngày", "6 tháng", "Mọi lúc", "Nhạc mới", "Nhịp nghe nhạc", "Nghe vài bản nhạc và âm thanh của bạn sẽ hiện ở đây.", "Khám phá thêm"),
    "th" to experience("Levyra Mix", "สร้างมิกซ์", "เริ่มวิทยุ", "คุ้นเคย", "ค้นพบ", "สุ่มให้ฉัน", "บันทึกรายการนี้", "ยังสร้างมิกซ์ไม่ได้ในตอนนี้", "เสียงของคุณ", "วิธีที่คุณฟังจริง ๆ", "7 วัน", "30 วัน", "6 เดือน", "ตลอดเวลา", "เพลงใหม่", "จังหวะการฟัง", "ฟังสักสองสามเพลง แล้วเสียงของคุณจะปรากฏที่นี่", "ค้นพบเพิ่มเติม"),
    "fil" to experience("Levyra Mix", "Gumawa ng mix", "Simulan ang radio", "Pamilyar", "Pagtuklas", "Sorpresahin mo ako", "I-save ang seleksyong ito", "Walang mix na nagawa ngayon.", "Ang tunog mo", "Kung paano ka talaga nakikinig", "7 araw", "30 araw", "6 na buwan", "Lahat ng panahon", "Bagong musika", "Ritmo ng pakikinig", "Makinig ng ilang kanta at lilitaw dito ang tunog mo.", "Tumuklas pa"),
    "he" to experience("Levyra Mix", "יצירת מיקס", "הפעלת רדיו", "מוכר", "גילוי", "הפתיעו אותי", "שמירת הבחירה הזו", "לא ניתן ליצור מיקס כרגע.", "הצליל שלך", "איך אתם באמת מאזינים", "7 ימים", "30 ימים", "6 חודשים", "תמיד", "מוזיקה חדשה", "קצב ההאזנה", "האזינו לכמה רצועות והצליל שלכם יופיע כאן.", "לגלות עוד")
)

internal fun experienceLocalizationEntries(code: String): Map<String, String> = experienceBundles.getValue(code)

internal fun experienceLocalizationCodes(): Set<String> = experienceBundles.keys
