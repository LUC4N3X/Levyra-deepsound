package com.luc4n3x.levyra.ui.i18n

internal val playerDeckKeys = setOf(
    "playerDeck",
    "playerDeckSubtitle",
    "playerDeckEditorial",
    "playerDeckPulse",
    "playerDeckImmersiveHint",
    "playerDeckCardHint",
    "playerDeckArtworkHint",
    "playerDeckEditorialHint",
    "playerDeckPulseHint",
    "playerDeckLandscapeNote"
)

private fun playerDeck(
    title: String,
    subtitle: String,
    editorial: String,
    pulse: String,
    immersiveHint: String,
    cardHint: String,
    artworkHint: String,
    editorialHint: String,
    pulseHint: String,
    landscapeNote: String
): Map<String, String> = mapOf(
    "playerDeck" to title,
    "playerDeckSubtitle" to subtitle,
    "playerDeckEditorial" to editorial,
    "playerDeckPulse" to pulse,
    "playerDeckImmersiveHint" to immersiveHint,
    "playerDeckCardHint" to cardHint,
    "playerDeckArtworkHint" to artworkHint,
    "playerDeckEditorialHint" to editorialHint,
    "playerDeckPulseHint" to pulseHint,
    "playerDeckLandscapeNote" to landscapeNote
)

private val playerDeckBundles: Map<String, Map<String, String>> = mapOf(
    "en" to playerDeck("Player Deck", "Same music, a different stage. Switching never interrupts playback.", "Editorial", "Pulse", "The signature Levyra stage: artwork and Canvas fill the screen.", "Canvas framed in a floating card with a soft glow.", "Static artwork, calm and focused.", "Magazine cover layout with bold type and track index.", "Live signal line and audio format details.", "Landscape, video and live radio always use the Levyra layout."),
    "it" to playerDeck("Player Deck", "Stessa musica, un palco diverso. Il cambio non interrompe mai la riproduzione.", "Editoriale", "Pulse", "Il palco firmato Levyra: copertina e Canvas riempiono lo schermo.", "Canvas in una scheda sospesa con un bagliore morbido.", "Copertina statica, sobria e concentrata.", "Impaginazione da copertina di rivista con tipografia forte e numero traccia.", "Segnale audio dal vivo e dettagli del formato.", "Orizzontale, video e radio live usano sempre il layout Levyra."),
    "es" to playerDeck("Player Deck", "La misma música, otro escenario. Cambiar nunca interrumpe la reproducción.", "Editorial", "Pulse", "El escenario de Levyra: portada y Canvas llenan la pantalla.", "Canvas en una tarjeta flotante con un brillo suave.", "Portada estática, serena y centrada.", "Diseño de portada de revista con tipografía fuerte e índice de pista.", "Señal de audio en vivo y detalles del formato.", "Horizontal, vídeo y radio en directo usan siempre el diseño Levyra."),
    "fr" to playerDeck("Player Deck", "La même musique, une autre scène. Changer n'interrompt jamais la lecture.", "Éditorial", "Pulse", "La scène signature Levyra : pochette et Canvas occupent l'écran.", "Canvas dans une carte flottante au halo doux.", "Pochette fixe, calme et épurée.", "Mise en page façon couverture de magazine, typographie forte et numéro de piste.", "Signal audio en direct et détails du format.", "Le paysage, la vidéo et la radio en direct utilisent toujours la mise en page Levyra."),
    "de" to playerDeck("Player Deck", "Gleiche Musik, andere Bühne. Der Wechsel unterbricht die Wiedergabe nie.", "Editorial", "Pulse", "Die typische Levyra-Bühne: Cover und Canvas füllen den Bildschirm.", "Canvas in einer schwebenden Karte mit sanftem Schein.", "Statisches Cover, ruhig und fokussiert.", "Magazin-Cover-Layout mit starker Typografie und Titelnummer.", "Live-Signal und Details zum Audioformat.", "Querformat, Video und Live-Radio nutzen immer das Levyra-Layout."),
    "pt" to playerDeck("Player Deck", "A mesma música, outro palco. Mudar nunca interrompe a reprodução.", "Editorial", "Pulse", "O palco característico do Levyra: capa e Canvas ocupam o ecrã.", "Canvas num cartão flutuante com brilho suave.", "Capa estática, calma e focada.", "Layout de capa de revista com tipografia forte e número da faixa.", "Sinal de áudio ao vivo e detalhes do formato.", "Paisagem, vídeo e rádio ao vivo usam sempre o layout Levyra."),
    "nl" to playerDeck("Player Deck", "Dezelfde muziek, een ander podium. Wisselen onderbreekt het afspelen nooit.", "Editorial", "Pulse", "Het kenmerkende Levyra-podium: hoes en Canvas vullen het scherm.", "Canvas in een zwevende kaart met zachte gloed.", "Statische hoes, rustig en gefocust.", "Tijdschriftcover-opmaak met sterke typografie en tracknummer.", "Live signaallijn en details van het audioformaat.", "Liggend, video en live radio gebruiken altijd de Levyra-opmaak."),
    "pl" to playerDeck("Player Deck", "Ta sama muzyka, inna scena. Zmiana nigdy nie przerywa odtwarzania.", "Magazyn", "Pulse", "Charakterystyczna scena Levyra: okładka i Canvas wypełniają ekran.", "Canvas w unoszącej się karcie z delikatną poświatą.", "Statyczna okładka, spokojna i skupiona.", "Układ okładki magazynu z wyrazistą typografią i numerem utworu.", "Sygnał audio na żywo i szczegóły formatu.", "Poziomo, wideo i radio na żywo zawsze używają układu Levyra."),
    "ro" to playerDeck("Player Deck", "Aceeași muzică, altă scenă. Schimbarea nu întrerupe niciodată redarea.", "Editorial", "Pulse", "Scena Levyra: coperta și Canvasul umplu ecranul.", "Canvas într-un card plutitor cu o strălucire discretă.", "Copertă statică, calmă și concentrată.", "Aspect de copertă de revistă cu tipografie puternică și număr de piesă.", "Semnal audio live și detalii despre format.", "Modul peisaj, video și radio live folosesc mereu aspectul Levyra."),
    "el" to playerDeck("Player Deck", "Ίδια μουσική, διαφορετική σκηνή. Η αλλαγή δεν διακόπτει ποτέ την αναπαραγωγή.", "Editorial", "Pulse", "Η χαρακτηριστική σκηνή του Levyra: εξώφυλλο και Canvas γεμίζουν την οθόνη.", "Canvas σε αιωρούμενη κάρτα με απαλή λάμψη.", "Στατικό εξώφυλλο, ήρεμο και εστιασμένο.", "Διάταξη εξωφύλλου περιοδικού με έντονη τυπογραφία και αριθμό κομματιού.", "Ζωντανό σήμα ήχου και λεπτομέρειες μορφής.", "Η οριζόντια προβολή, το βίντεο και το ζωντανό ραδιόφωνο χρησιμοποιούν πάντα τη διάταξη Levyra."),
    "sv" to playerDeck("Player Deck", "Samma musik, en annan scen. Bytet avbryter aldrig uppspelningen.", "Editorial", "Pulse", "Levyras signaturscen: omslag och Canvas fyller skärmen.", "Canvas i ett svävande kort med mjukt sken.", "Statiskt omslag, lugnt och fokuserat.", "Tidningsomslag med stark typografi och spårnummer.", "Livesignal och detaljer om ljudformatet.", "Liggande läge, video och liveradio använder alltid Levyra-layouten."),
    "da" to playerDeck("Player Deck", "Samme musik, en anden scene. Skift afbryder aldrig afspilningen.", "Editorial", "Pulse", "Levyras signaturscene: cover og Canvas fylder skærmen.", "Canvas i et svævende kort med blødt skær.", "Statisk cover, roligt og fokuseret.", "Magasinforside med stærk typografi og spornummer.", "Live-signal og detaljer om lydformatet.", "Liggende, video og liveradio bruger altid Levyra-layoutet."),
    "cs" to playerDeck("Player Deck", "Stejná hudba, jiné pódium. Přepnutí nikdy nepřeruší přehrávání.", "Magazín", "Pulse", "Typické pódium Levyra: obal a Canvas vyplní obrazovku.", "Canvas v plovoucí kartě s jemnou září.", "Statický obal, klidný a soustředěný.", "Rozvržení obálky časopisu s výraznou typografií a číslem skladby.", "Živý zvukový signál a podrobnosti o formátu.", "Na šířku, video a živé rádio vždy používají rozvržení Levyra."),
    "uk" to playerDeck("Player Deck", "Та сама музика, інша сцена. Перемикання ніколи не перериває відтворення.", "Журнал", "Pulse", "Фірмова сцена Levyra: обкладинка та Canvas заповнюють екран.", "Canvas у плаваючій картці з м'яким сяйвом.", "Статична обкладинка, спокійна й зосереджена.", "Макет обкладинки журналу з виразною типографікою та номером треку.", "Живий аудіосигнал і деталі формату.", "Горизонтальний режим, відео та живе радіо завжди використовують макет Levyra."),
    "ru" to playerDeck("Player Deck", "Та же музыка, другая сцена. Переключение никогда не прерывает воспроизведение.", "Журнал", "Pulse", "Фирменная сцена Levyra: обложка и Canvas заполняют экран.", "Canvas в парящей карточке с мягким свечением.", "Статичная обложка, спокойная и сосредоточенная.", "Макет обложки журнала с выразительной типографикой и номером трека.", "Живой аудиосигнал и сведения о формате.", "Горизонтальный режим, видео и живое радио всегда используют макет Levyra."),
    "tr" to playerDeck("Player Deck", "Aynı müzik, farklı bir sahne. Geçiş çalmayı asla kesmez.", "Editoryal", "Pulse", "Levyra'nın imza sahnesi: kapak ve Canvas ekranı doldurur.", "Yumuşak parıltılı, yüzen bir kartta Canvas.", "Sabit kapak, sakin ve odaklı.", "Güçlü tipografi ve parça numarasıyla dergi kapağı düzeni.", "Canlı ses sinyali ve format ayrıntıları.", "Yatay mod, video ve canlı radyo her zaman Levyra düzenini kullanır."),
    "ar" to playerDeck("Player Deck", "الموسيقى نفسها بمشهد مختلف. التبديل لا يوقف التشغيل أبدًا.", "تحريري", "Pulse", "مشهد Levyra المميز: الغلاف وCanvas يملآن الشاشة.", "Canvas داخل بطاقة عائمة بتوهج هادئ.", "غلاف ثابت، هادئ ومركّز.", "تخطيط غلاف مجلة بخط عريض ورقم المقطع.", "إشارة صوتية مباشرة وتفاصيل التنسيق.", "الوضع الأفقي والفيديو والراديو المباشر تستخدم دائمًا تخطيط Levyra."),
    "zh" to playerDeck("Player Deck", "同样的音乐，不同的舞台。切换从不打断播放。", "杂志", "Pulse", "Levyra 标志舞台：封面与 Canvas 铺满屏幕。", "Canvas 置于柔光浮动卡片中。", "静态封面，安静专注。", "杂志封面式排版，醒目字体与曲目序号。", "实时音频信号与格式详情。", "横屏、视频和直播电台始终使用 Levyra 布局。"),
    "ja" to playerDeck("Player Deck", "同じ音楽を、別のステージで。切り替えても再生は途切れません。", "エディトリアル", "Pulse", "Levyra らしいステージ。アートワークと Canvas が画面いっぱいに広がります。", "やわらかな光をまとうフローティングカードの Canvas。", "静止アートワークで落ち着いた表示。", "力強い文字組みとトラック番号の雑誌表紙風レイアウト。", "ライブ信号とオーディオ形式の詳細。", "横向き、動画、ライブラジオでは常に Levyra レイアウトを使用します。"),
    "ko" to playerDeck("Player Deck", "같은 음악, 다른 무대. 전환해도 재생이 끊기지 않습니다.", "에디토리얼", "Pulse", "Levyra 대표 무대: 아트워크와 Canvas가 화면을 채웁니다.", "은은한 빛의 떠 있는 카드 속 Canvas.", "정적인 아트워크로 차분하게.", "굵은 타이포그래피와 트랙 번호의 매거진 커버 레이아웃.", "실시간 오디오 신호와 포맷 정보.", "가로 모드, 동영상, 라이브 라디오는 항상 Levyra 레이아웃을 사용합니다."),
    "hi" to playerDeck("Player Deck", "वही संगीत, नया मंच। बदलने पर प्लेबैक कभी नहीं रुकता।", "एडिटोरियल", "Pulse", "Levyra का खास मंच: आर्टवर्क और Canvas पूरी स्क्रीन भरते हैं।", "हल्की चमक वाले तैरते कार्ड में Canvas।", "स्थिर आर्टवर्क, शांत और केंद्रित।", "बोल्ड टाइपोग्राफी और ट्रैक संख्या वाला पत्रिका कवर लेआउट।", "लाइव ऑडियो सिग्नल और फ़ॉर्मैट विवरण।", "लैंडस्केप, वीडियो और लाइव रेडियो हमेशा Levyra लेआउट का उपयोग करते हैं।"),
    "id" to playerDeck("Player Deck", "Musik yang sama, panggung berbeda. Beralih tidak pernah menghentikan pemutaran.", "Editorial", "Pulse", "Panggung khas Levyra: artwork dan Canvas memenuhi layar.", "Canvas dalam kartu mengambang dengan cahaya lembut.", "Artwork statis, tenang dan fokus.", "Tata letak sampul majalah dengan tipografi tegas dan nomor lagu.", "Sinyal audio langsung dan detail format.", "Lanskap, video, dan radio langsung selalu memakai tata letak Levyra."),
    "vi" to playerDeck("Player Deck", "Cùng âm nhạc, sân khấu khác. Chuyển đổi không bao giờ làm gián đoạn phát nhạc.", "Tạp chí", "Pulse", "Sân khấu đặc trưng của Levyra: ảnh bìa và Canvas phủ kín màn hình.", "Canvas trong thẻ nổi với ánh sáng dịu.", "Ảnh bìa tĩnh, nhẹ nhàng và tập trung.", "Bố cục bìa tạp chí với kiểu chữ mạnh và số thứ tự bài.", "Tín hiệu âm thanh trực tiếp và chi tiết định dạng.", "Chế độ ngang, video và radio trực tiếp luôn dùng bố cục Levyra."),
    "th" to playerDeck("Player Deck", "เพลงเดิม เวทีใหม่ การสลับไม่ขัดจังหวะการเล่นเลย", "นิตยสาร", "Pulse", "เวทีเอกลักษณ์ของ Levyra: หน้าปกและ Canvas เต็มหน้าจอ", "Canvas ในการ์ดลอยพร้อมแสงนุ่ม", "หน้าปกนิ่ง สงบและมีโฟกัส", "เลย์เอาต์แบบปกนิตยสาร ตัวอักษรเด่นพร้อมหมายเลขแทร็ก", "สัญญาณเสียงสดและรายละเอียดรูปแบบ", "แนวนอน วิดีโอ และวิทยุสดจะใช้เลย์เอาต์ Levyra เสมอ"),
    "fil" to playerDeck("Player Deck", "Parehong musika, ibang entablado. Hindi naaabala ang pag-play kapag nagpalit.", "Editorial", "Pulse", "Ang signature na entablado ng Levyra: artwork at Canvas sa buong screen.", "Canvas sa lumulutang na card na may malambot na liwanag.", "Static na artwork, kalmado at nakatuon.", "Layout ng pabalat ng magazine na may matapang na titik at numero ng track.", "Live na audio signal at detalye ng format.", "Ang landscape, video at live radio ay laging gumagamit ng layout ng Levyra."),
    "he" to playerDeck("Player Deck", "אותה מוזיקה, במה אחרת. המעבר אף פעם לא עוצר את ההשמעה.", "מגזין", "Pulse", "הבמה הייחודית של Levyra: העטיפה וה-Canvas ממלאים את המסך.", "Canvas בכרטיס מרחף עם זוהר רך.", "עטיפה סטטית, שקטה וממוקדת.", "פריסת שער מגזין עם טיפוגרפיה בולטת ומספר רצועה.", "אות שמע חי ופרטי הפורמט.", "מצב רוחבי, וידאו ורדיו חי משתמשים תמיד בפריסת Levyra.")
)

internal fun playerDeckLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(playerDeckBundles, code)

internal fun playerDeckLocalizationCodes(): Set<String> = playerDeckBundles.keys
