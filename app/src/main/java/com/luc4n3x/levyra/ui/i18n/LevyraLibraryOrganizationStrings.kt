package com.luc4n3x.levyra.ui.i18n

private val organizationKeyList = listOf(
    "ambientMode",
    "ambientModeSubtitle",
    "ambientOpen",
    "ambientExit",
    "ambientSettingsTitle",
    "ambientBrightness",
    "ambientAutoDim",
    "ambientPixelShift",
    "ambientProximityBlackout",
    "ambientShowLyrics",
    "ambientShowCanvas",
    "ambientNothingPlaying",
    "forgottenFavorites",
    "forgottenFavoritesSubtitle",
    "excludeArtist",
    "includeArtist",
    "excludedArtists",
    "excludedArtistsEmpty",
    "playlistTags",
    "newPlaylistTag",
    "playlistTagName",
    "editPlaylistTags",
    "filterByTag",
    "playlistTagLimitReached",
    "hidePlaylist",
    "unhidePlaylist",
    "hiddenPlaylists",
    "hiddenPlaylistsEmpty"
)

internal val organizationKeys: Set<String> = organizationKeyList.toSet()

private fun organization(vararg values: String): Map<String, String> {
    require(values.size == organizationKeyList.size) {
        "Expected ${organizationKeyList.size} organization strings, received ${values.size}"
    }
    return organizationKeyList.zip(values.asList()).toMap()
}

private val organizationBundles: Map<String, Map<String, String>> = mapOf(
    "en" to organization(
        "Ambient", "OLED-friendly screen for quiet listening", "Open Ambient", "Exit Ambient",
        "Ambient screen", "Brightness", "Auto dimming", "Pixel shift", "Proximity blackout",
        "Show lyrics", "Show Canvas", "Nothing is playing",
        "Rediscover", "Favourites you have not played in a while",
        "Do not recommend this artist", "Recommend again", "Excluded artists",
        "No excluded artists yet.",
        "Tags", "New tag", "Tag name", "Edit tags", "Filter by tag",
        "You can assign up to 8 tags.",
        "Hide from library", "Show in library", "Hidden playlists", "No hidden playlists."
    ),
    "it" to organization(
        "Ambient", "Schermata OLED-friendly per l'ascolto tranquillo", "Apri Ambient", "Esci da Ambient",
        "Schermata Ambient", "Luminosità", "Attenuazione automatica", "Spostamento pixel",
        "Blackout di prossimità",
        "Mostra i testi", "Mostra Canvas", "Nessun brano in riproduzione",
        "Riscoprili", "Preferiti che non ascolti da un po'",
        "Non consigliarmi questo artista", "Consiglia di nuovo", "Artisti esclusi",
        "Nessun artista escluso.",
        "Tag", "Nuovo tag", "Nome del tag", "Modifica tag", "Filtra per tag",
        "Puoi assegnare fino a 8 tag.",
        "Nascondi dalla libreria", "Mostra nella libreria", "Playlist nascoste",
        "Nessuna playlist nascosta."
    ),
    "es" to organization(
        "Ambient", "Pantalla apta para OLED para escuchar con calma", "Abrir Ambient", "Salir de Ambient",
        "Pantalla Ambient", "Brillo", "Atenuación automática", "Desplazamiento de píxeles",
        "Apagado por proximidad",
        "Mostrar la letra", "Mostrar Canvas", "No hay nada en reproducción",
        "Redescubre", "Favoritos que no escuchas desde hace tiempo",
        "No recomendar este artista", "Volver a recomendar", "Artistas excluidos",
        "Todavía no hay artistas excluidos.",
        "Etiquetas", "Nueva etiqueta", "Nombre de la etiqueta", "Editar etiquetas",
        "Filtrar por etiqueta",
        "Puedes asignar hasta 8 etiquetas.",
        "Ocultar de la biblioteca", "Mostrar en la biblioteca", "Listas ocultas",
        "No hay listas ocultas."
    ),
    "fr" to organization(
        "Ambient", "Écran adapté aux OLED pour une écoute calme", "Ouvrir Ambient", "Quitter Ambient",
        "Écran Ambient", "Luminosité", "Atténuation automatique", "Décalage des pixels",
        "Extinction de proximité",
        "Afficher les paroles", "Afficher Canvas", "Aucune lecture en cours",
        "Redécouvrir", "Des favoris que vous n'avez pas écoutés depuis un moment",
        "Ne plus me recommander cet artiste", "Recommander à nouveau", "Artistes exclus",
        "Aucun artiste exclu pour l'instant.",
        "Tags", "Nouveau tag", "Nom du tag", "Modifier les tags", "Filtrer par tag",
        "Vous pouvez attribuer jusqu'à 8 tags.",
        "Masquer de la bibliothèque", "Afficher dans la bibliothèque", "Playlists masquées",
        "Aucune playlist masquée."
    ),
    "de" to organization(
        "Ambient", "OLED-freundlicher Bildschirm für ruhiges Hören", "Ambient öffnen", "Ambient verlassen",
        "Ambient-Bildschirm", "Helligkeit", "Automatisches Abdunkeln", "Pixelverschiebung",
        "Näherungs-Blackout",
        "Songtext anzeigen", "Canvas anzeigen", "Es wird nichts abgespielt",
        "Wiederentdecken", "Favoriten, die du länger nicht gehört hast",
        "Diesen Künstler nicht empfehlen", "Wieder empfehlen", "Ausgeschlossene Künstler",
        "Noch keine ausgeschlossenen Künstler.",
        "Tags", "Neuer Tag", "Tag-Name", "Tags bearbeiten", "Nach Tag filtern",
        "Du kannst bis zu 8 Tags vergeben.",
        "Aus der Bibliothek ausblenden", "In der Bibliothek anzeigen", "Ausgeblendete Playlists",
        "Keine ausgeblendeten Playlists."
    ),
    "pt" to organization(
        "Ambient", "Ecrã amigo do OLED para ouvir com calma", "Abrir Ambient", "Sair do Ambient",
        "Ecrã Ambient", "Brilho", "Escurecimento automático", "Deslocação de píxeis",
        "Blackout de proximidade",
        "Mostrar a letra", "Mostrar Canvas", "Nada em reprodução",
        "Redescobrir", "Favoritos que não ouves há algum tempo",
        "Não recomendar este artista", "Recomendar novamente", "Artistas excluídos",
        "Ainda não há artistas excluídos.",
        "Etiquetas", "Nova etiqueta", "Nome da etiqueta", "Editar etiquetas", "Filtrar por etiqueta",
        "Podes atribuir até 8 etiquetas.",
        "Ocultar da biblioteca", "Mostrar na biblioteca", "Playlists ocultas",
        "Não há playlists ocultas."
    ),
    "nl" to organization(
        "Ambient", "OLED-vriendelijk scherm om rustig te luisteren", "Ambient openen", "Ambient sluiten",
        "Ambient-scherm", "Helderheid", "Automatisch dimmen", "Pixelverschuiving", "Nabijheidsblackout",
        "Songtekst tonen", "Canvas tonen", "Er speelt niets",
        "Herontdekken", "Favorieten die je al even niet hebt geluisterd",
        "Deze artiest niet aanbevelen", "Weer aanbevelen", "Uitgesloten artiesten",
        "Nog geen uitgesloten artiesten.",
        "Tags", "Nieuwe tag", "Tagnaam", "Tags bewerken", "Filteren op tag",
        "Je kunt maximaal 8 tags toewijzen.",
        "Verbergen in bibliotheek", "Tonen in bibliotheek", "Verborgen afspeellijsten",
        "Geen verborgen afspeellijsten."
    ),
    "pl" to organization(
        "Ambient", "Ekran przyjazny OLED do spokojnego słuchania", "Otwórz Ambient", "Zamknij Ambient",
        "Ekran Ambient", "Jasność", "Automatyczne przyciemnianie", "Przesuwanie pikseli",
        "Wygaszanie zbliżeniowe",
        "Pokaż tekst", "Pokaż Canvas", "Nic nie jest odtwarzane",
        "Odkryj na nowo", "Ulubione, których dawno nie słuchasz",
        "Nie polecaj mi tego wykonawcy", "Polecaj ponownie", "Wykluczeni wykonawcy",
        "Brak wykluczonych wykonawców.",
        "Tagi", "Nowy tag", "Nazwa tagu", "Edytuj tagi", "Filtruj według tagu",
        "Możesz przypisać maksymalnie 8 tagów.",
        "Ukryj w bibliotece", "Pokaż w bibliotece", "Ukryte playlisty", "Brak ukrytych playlist."
    ),
    "ro" to organization(
        "Ambient", "Ecran prietenos cu OLED pentru ascultare liniștită", "Deschide Ambient",
        "Ieși din Ambient",
        "Ecran Ambient", "Luminozitate", "Estompare automată", "Deplasare pixeli",
        "Stingere la proximitate",
        "Afișează versurile", "Afișează Canvas", "Nu se redă nimic",
        "Redescoperă", "Favorite pe care nu le-ai ascultat de mult",
        "Nu îmi recomanda acest artist", "Recomandă din nou", "Artiști excluși",
        "Niciun artist exclus.",
        "Etichete", "Etichetă nouă", "Numele etichetei", "Editează etichetele",
        "Filtrează după etichetă",
        "Poți atribui până la 8 etichete.",
        "Ascunde din bibliotecă", "Afișează în bibliotecă", "Playlisturi ascunse",
        "Niciun playlist ascuns."
    ),
    "el" to organization(
        "Ambient", "Οθόνη φιλική προς OLED για ήρεμη ακρόαση", "Άνοιγμα Ambient", "Έξοδος από Ambient",
        "Οθόνη Ambient", "Φωτεινότητα", "Αυτόματη μείωση φωτεινότητας", "Μετατόπιση pixel",
        "Σβήσιμο με εγγύτητα",
        "Εμφάνιση στίχων", "Εμφάνιση Canvas", "Δεν αναπαράγεται τίποτα",
        "Ανακαλύψτε ξανά", "Αγαπημένα που έχετε καιρό να ακούσετε",
        "Να μην προτείνεται αυτός ο καλλιτέχνης", "Πρότεινε ξανά", "Εξαιρεμένοι καλλιτέχνες",
        "Δεν υπάρχουν εξαιρεμένοι καλλιτέχνες.",
        "Ετικέτες", "Νέα ετικέτα", "Όνομα ετικέτας", "Επεξεργασία ετικετών",
        "Φιλτράρισμα κατά ετικέτα",
        "Μπορείτε να αντιστοιχίσετε έως 8 ετικέτες.",
        "Απόκρυψη από τη βιβλιοθήκη", "Εμφάνιση στη βιβλιοθήκη", "Κρυφές λίστες",
        "Δεν υπάρχουν κρυφές λίστες."
    ),
    "sv" to organization(
        "Ambient", "OLED-vänlig skärm för lugnt lyssnande", "Öppna Ambient", "Avsluta Ambient",
        "Ambient-skärm", "Ljusstyrka", "Automatisk nedtoning", "Pixelförskjutning", "Närhetssläckning",
        "Visa låttext", "Visa Canvas", "Inget spelas",
        "Återupptäck", "Favoriter du inte lyssnat på på ett tag",
        "Rekommendera inte den här artisten", "Rekommendera igen", "Uteslutna artister",
        "Inga uteslutna artister ännu.",
        "Taggar", "Ny tagg", "Taggnamn", "Redigera taggar", "Filtrera efter tagg",
        "Du kan tilldela upp till 8 taggar.",
        "Dölj i biblioteket", "Visa i biblioteket", "Dolda spellistor", "Inga dolda spellistor."
    ),
    "da" to organization(
        "Ambient", "OLED-venlig skærm til rolig lytning", "Åbn Ambient", "Forlad Ambient",
        "Ambient-skærm", "Lysstyrke", "Automatisk dæmpning", "Pixelforskydning", "Nærhedsslukning",
        "Vis sangtekst", "Vis Canvas", "Der afspilles intet",
        "Genopdag", "Favoritter du ikke har hørt i et stykke tid",
        "Anbefal ikke denne kunstner", "Anbefal igen", "Udelukkede kunstnere",
        "Ingen udelukkede kunstnere endnu.",
        "Tags", "Nyt tag", "Tagnavn", "Rediger tags", "Filtrer efter tag",
        "Du kan tildele op til 8 tags.",
        "Skjul i biblioteket", "Vis i biblioteket", "Skjulte playlister", "Ingen skjulte playlister."
    ),
    "cs" to organization(
        "Ambient", "Obrazovka šetrná k OLED pro klidný poslech", "Otevřít Ambient", "Ukončit Ambient",
        "Obrazovka Ambient", "Jas", "Automatické ztlumení", "Posun pixelů", "Zhasnutí při přiblížení",
        "Zobrazit text", "Zobrazit Canvas", "Nic se nepřehrává",
        "Znovu objevit", "Oblíbené, které jste dlouho neposlouchali",
        "Nedoporučovat tohoto interpreta", "Znovu doporučovat", "Vyloučení interpreti",
        "Zatím žádní vyloučení interpreti.",
        "Štítky", "Nový štítek", "Název štítku", "Upravit štítky", "Filtrovat podle štítku",
        "Můžete přiřadit až 8 štítků.",
        "Skrýt v knihovně", "Zobrazit v knihovně", "Skryté playlisty", "Žádné skryté playlisty."
    ),
    "uk" to organization(
        "Ambient", "Екран, дружній до OLED, для спокійного прослуховування", "Відкрити Ambient",
        "Вийти з Ambient",
        "Екран Ambient", "Яскравість", "Автоматичне затемнення", "Зсув пікселів",
        "Затемнення за наближенням",
        "Показувати текст", "Показувати Canvas", "Нічого не відтворюється",
        "Відкрийте знову", "Улюблене, яке ви давно не слухали",
        "Не рекомендувати цього виконавця", "Рекомендувати знову", "Виключені виконавці",
        "Виключених виконавців немає.",
        "Теги", "Новий тег", "Назва тега", "Редагувати теги", "Фільтрувати за тегом",
        "Можна призначити до 8 тегів.",
        "Сховати з бібліотеки", "Показати в бібліотеці", "Приховані плейлисти",
        "Прихованих плейлистів немає."
    ),
    "ru" to organization(
        "Ambient", "Экран, щадящий OLED, для спокойного прослушивания", "Открыть Ambient",
        "Выйти из Ambient",
        "Экран Ambient", "Яркость", "Автоматическое затемнение", "Сдвиг пикселей",
        "Затемнение по датчику",
        "Показывать текст", "Показывать Canvas", "Ничего не воспроизводится",
        "Открыть заново", "Избранное, которое вы давно не слушали",
        "Не рекомендовать этого исполнителя", "Рекомендовать снова", "Исключённые исполнители",
        "Исключённых исполнителей нет.",
        "Теги", "Новый тег", "Название тега", "Изменить теги", "Фильтр по тегу",
        "Можно назначить до 8 тегов.",
        "Скрыть из библиотеки", "Показать в библиотеке", "Скрытые плейлисты",
        "Скрытых плейлистов нет."
    ),
    "tr" to organization(
        "Ambient", "Sakin dinleme için OLED dostu ekran", "Ambient'i aç", "Ambient'ten çık",
        "Ambient ekranı", "Parlaklık", "Otomatik karartma", "Piksel kaydırma", "Yakınlıkta karartma",
        "Şarkı sözlerini göster", "Canvas'ı göster", "Şu anda bir şey çalmıyor",
        "Yeniden keşfet", "Bir süredir dinlemediğin favoriler",
        "Bu sanatçıyı önerme", "Yeniden öner", "Hariç tutulan sanatçılar",
        "Henüz hariç tutulan sanatçı yok.",
        "Etiketler", "Yeni etiket", "Etiket adı", "Etiketleri düzenle", "Etikete göre filtrele",
        "En fazla 8 etiket atayabilirsin.",
        "Kitaplıktan gizle", "Kitaplıkta göster", "Gizli çalma listeleri",
        "Gizli çalma listesi yok."
    ),
    "ar" to organization(
        "Ambient", "شاشة ملائمة لشاشات OLED للاستماع الهادئ", "فتح Ambient", "الخروج من Ambient",
        "شاشة Ambient", "السطوع", "تعتيم تلقائي", "إزاحة البكسل", "إطفاء عند الاقتراب",
        "عرض الكلمات", "عرض Canvas", "لا يوجد تشغيل حاليًا",
        "أعد الاكتشاف", "مفضلات لم تستمع إليها منذ فترة",
        "لا توصِ بهذا الفنان", "أوصِ به مجددًا", "الفنانون المستبعدون",
        "لا يوجد فنانون مستبعدون.",
        "الوسوم", "وسم جديد", "اسم الوسم", "تعديل الوسوم", "تصفية حسب الوسم",
        "يمكنك تعيين حتى 8 وسوم.",
        "إخفاء من المكتبة", "إظهار في المكتبة", "قوائم التشغيل المخفية",
        "لا توجد قوائم تشغيل مخفية."
    ),
    "zh" to organization(
        "Ambient", "适合 OLED 的安静聆听界面", "打开 Ambient", "退出 Ambient",
        "Ambient 界面", "亮度", "自动变暗", "像素偏移", "接近熄屏",
        "显示歌词", "显示 Canvas", "当前没有播放内容",
        "重新发现", "很久没听的收藏",
        "不再推荐该艺人", "重新推荐", "已排除的艺人",
        "暂无已排除的艺人。",
        "标签", "新建标签", "标签名称", "编辑标签", "按标签筛选",
        "最多可分配 8 个标签。",
        "从音乐库隐藏", "在音乐库中显示", "隐藏的播放列表", "没有隐藏的播放列表。"
    ),
    "ja" to organization(
        "Ambient", "静かに聴くためのOLEDにやさしい画面", "Ambientを開く", "Ambientを終了",
        "Ambient画面", "明るさ", "自動減光", "ピクセルシフト", "近接ブラックアウト",
        "歌詞を表示", "Canvasを表示", "再生中の曲はありません",
        "再発見", "しばらく聴いていないお気に入り",
        "このアーティストをおすすめしない", "再びおすすめする", "除外したアーティスト",
        "除外したアーティストはありません。",
        "タグ", "新しいタグ", "タグ名", "タグを編集", "タグで絞り込む",
        "タグは8個まで設定できます。",
        "ライブラリから隠す", "ライブラリに表示", "非表示のプレイリスト",
        "非表示のプレイリストはありません。"
    ),
    "ko" to organization(
        "Ambient", "조용한 감상을 위한 OLED 친화 화면", "Ambient 열기", "Ambient 종료",
        "Ambient 화면", "밝기", "자동 어둡게", "픽셀 시프트", "근접 블랙아웃",
        "가사 표시", "Canvas 표시", "재생 중인 곡이 없습니다",
        "다시 발견하기", "한동안 듣지 않은 즐겨찾기",
        "이 아티스트 추천하지 않기", "다시 추천하기", "제외한 아티스트",
        "제외한 아티스트가 없습니다.",
        "태그", "새 태그", "태그 이름", "태그 편집", "태그로 필터링",
        "태그는 최대 8개까지 지정할 수 있습니다.",
        "라이브러리에서 숨기기", "라이브러리에 표시", "숨긴 재생목록",
        "숨긴 재생목록이 없습니다."
    ),
    "hi" to organization(
        "Ambient", "शांत सुनने के लिए OLED-अनुकूल स्क्रीन", "Ambient खोलें", "Ambient से बाहर निकलें",
        "Ambient स्क्रीन", "चमक", "स्वचालित मंद", "पिक्सेल शिफ्ट", "निकटता पर काली स्क्रीन",
        "बोल दिखाएँ", "Canvas दिखाएँ", "अभी कुछ नहीं चल रहा",
        "फिर से खोजें", "पसंदीदा जिन्हें आपने काफी समय से नहीं सुना",
        "इस कलाकार की सिफारिश न करें", "फिर से सिफारिश करें", "बाहर रखे गए कलाकार",
        "कोई बाहर रखा गया कलाकार नहीं।",
        "टैग", "नया टैग", "टैग का नाम", "टैग संपादित करें", "टैग से फ़िल्टर करें",
        "आप अधिकतम 8 टैग जोड़ सकते हैं।",
        "लाइब्रेरी से छिपाएँ", "लाइब्रेरी में दिखाएँ", "छिपी हुई प्लेलिस्ट",
        "कोई छिपी हुई प्लेलिस्ट नहीं।"
    ),
    "id" to organization(
        "Ambient", "Layar ramah OLED untuk mendengarkan dengan tenang", "Buka Ambient",
        "Keluar dari Ambient",
        "Layar Ambient", "Kecerahan", "Peredupan otomatis", "Pergeseran piksel",
        "Layar gelap saat dekat",
        "Tampilkan lirik", "Tampilkan Canvas", "Tidak ada yang diputar",
        "Temukan lagi", "Favorit yang sudah lama tidak kamu dengar",
        "Jangan rekomendasikan artis ini", "Rekomendasikan lagi", "Artis yang dikecualikan",
        "Belum ada artis yang dikecualikan.",
        "Tag", "Tag baru", "Nama tag", "Edit tag", "Filter menurut tag",
        "Kamu bisa menetapkan hingga 8 tag.",
        "Sembunyikan dari pustaka", "Tampilkan di pustaka", "Playlist tersembunyi",
        "Tidak ada playlist tersembunyi."
    ),
    "vi" to organization(
        "Ambient", "Màn hình thân thiện với OLED để nghe nhạc thư giãn", "Mở Ambient", "Thoát Ambient",
        "Màn hình Ambient", "Độ sáng", "Tự động giảm sáng", "Dịch chuyển điểm ảnh",
        "Tắt màn hình khi che",
        "Hiện lời bài hát", "Hiện Canvas", "Hiện không phát gì",
        "Khám phá lại", "Những bài yêu thích bạn đã lâu không nghe",
        "Không đề xuất nghệ sĩ này", "Đề xuất lại", "Nghệ sĩ đã loại trừ",
        "Chưa có nghệ sĩ nào bị loại trừ.",
        "Thẻ", "Thẻ mới", "Tên thẻ", "Sửa thẻ", "Lọc theo thẻ",
        "Bạn có thể gán tối đa 8 thẻ.",
        "Ẩn khỏi thư viện", "Hiện trong thư viện", "Danh sách phát đã ẩn",
        "Không có danh sách phát nào bị ẩn."
    ),
    "th" to organization(
        "Ambient", "หน้าจอที่เป็นมิตรกับ OLED สำหรับการฟังอย่างสงบ", "เปิด Ambient", "ออกจาก Ambient",
        "หน้าจอ Ambient", "ความสว่าง", "หรี่แสงอัตโนมัติ", "การเลื่อนพิกเซล", "ดับจอเมื่อเข้าใกล้",
        "แสดงเนื้อเพลง", "แสดง Canvas", "ยังไม่มีการเล่นเพลง",
        "ค้นพบอีกครั้ง", "รายการโปรดที่คุณไม่ได้ฟังมานาน",
        "ไม่ต้องแนะนำศิลปินนี้", "แนะนำอีกครั้ง", "ศิลปินที่ยกเว้น",
        "ยังไม่มีศิลปินที่ยกเว้น",
        "แท็ก", "แท็กใหม่", "ชื่อแท็ก", "แก้ไขแท็ก", "กรองตามแท็ก",
        "คุณกำหนดได้สูงสุด 8 แท็ก",
        "ซ่อนจากคลัง", "แสดงในคลัง", "เพลย์ลิสต์ที่ซ่อน", "ไม่มีเพลย์ลิสต์ที่ซ่อน"
    ),
    "fil" to organization(
        "Ambient", "Screen na OLED-friendly para sa tahimik na pakikinig", "Buksan ang Ambient",
        "Lumabas sa Ambient",
        "Ambient screen", "Liwanag", "Awtomatikong pagdilim", "Pixel shift", "Blackout kapag malapit",
        "Ipakita ang lyrics", "Ipakita ang Canvas", "Walang tumutugtog",
        "Tuklasin muli", "Mga paborito na matagal mo nang hindi napapakinggan",
        "Huwag imungkahi ang artist na ito", "Imungkahi muli", "Mga hindi isinasamang artist",
        "Wala pang hindi isinasamang artist.",
        "Mga tag", "Bagong tag", "Pangalan ng tag", "I-edit ang mga tag", "I-filter ayon sa tag",
        "Hanggang 8 tag ang puwede mong itakda.",
        "Itago sa library", "Ipakita sa library", "Mga nakatagong playlist",
        "Walang nakatagong playlist."
    ),
    "he" to organization(
        "Ambient", "מסך ידידותי ל-OLED להאזנה רגועה", "פתיחת Ambient", "יציאה מ-Ambient",
        "מסך Ambient", "בהירות", "עמעום אוטומטי", "הסטת פיקסלים", "כיבוי בקרבה",
        "הצגת מילים", "הצגת Canvas", "לא מתנגן כלום",
        "לגלות מחדש", "מועדפים שלא האזנתם להם מזמן",
        "אל תמליצו לי על האמן הזה", "להמליץ שוב", "אמנים שהוחרגו",
        "אין אמנים שהוחרגו.",
        "תגיות", "תגית חדשה", "שם התגית", "עריכת תגיות", "סינון לפי תגית",
        "אפשר לשייך עד 8 תגיות.",
        "הסתרה מהספרייה", "הצגה בספרייה", "פלייליסטים מוסתרים", "אין פלייליסטים מוסתרים."
    )
)

internal fun organizationLocalizationEntries(code: String): Map<String, String> =
    organizationBundles[code] ?: organizationBundles.getValue("en")

internal fun organizationLocalizationCodes(): Set<String> = organizationBundles.keys
