package com.luc4n3x.levyra.ui.i18n

private val playlistStudioKeyOrder = listOf(
    "playlistStudio",
    "playlistStudioNew",
    "playlistStudioEdit",
    "playlistStudioOpen",
    "playlistStudioNameHint",
    "playlistStudioNameRequired",
    "playlistStudioCover",
    "playlistStudioCoverCurrent",
    "playlistStudioCoverAutomatic",
    "playlistStudioCoverArtwork",
    "playlistStudioCoverMosaic",
    "playlistStudioCoverSpotlight",
    "playlistStudioCoverSignal",
    "playlistStudioCoverPhoto",
    "playlistStudioChooseArtwork",
    "playlistStudioAddSongs",
    "playlistStudioSearchLibrary",
    "playlistStudioInPlaylist",
    "playlistStudioStateSaved",
    "playlistStudioStateUnsaved",
    "playlistStudioStateSaving",
    "playlistStudioStateFailed",
    "playlistStudioRetry",
    "playlistStudioRemoved",
    "playlistStudioMoved",
    "playlistStudioUndo",
    "playlistStudioEmptyTitle",
    "playlistStudioEmptyBody",
    "playlistStudioLibraryEmpty",
    "playlistStudioLoading",
    "playlistStudioDiscardTitle",
    "playlistStudioDiscardBody",
    "playlistStudioDiscard",
    "playlistStudioKeepEditing"
)

internal val playlistStudioKeys: Set<String> = playlistStudioKeyOrder.toSet()

private fun studio(vararg values: String): Map<String, String> {
    require(values.size == playlistStudioKeyOrder.size) { "Playlist Studio bundle has ${values.size} values" }
    return playlistStudioKeyOrder.zip(values).toMap()
}

private val playlistStudioBundles: Map<String, Map<String, String>> = mapOf(
    "en" to studio(
        "Playlist Studio", "New playlist", "Edit playlist", "Open in Playlist Studio", "Name your playlist", "Give it a name to save",
        "Cover", "Current", "Automatic", "Artwork", "Mosaic", "Spotlight", "Signal", "Photo", "Choose the artwork",
        "Add songs", "Search your library", "In playlist",
        "All changes saved", "Unsaved changes", "Saving…", "Couldn't save the playlist", "Retry",
        "Removed {title}", "Moved {title}", "Undo",
        "Start with a few songs", "Add songs from your favorites, history and playlists. You can reorder them any time.",
        "Your library is empty for now. Like songs or play some music to fill it.", "Gathering your library…",
        "Discard changes?", "Your edits to this playlist will be lost.", "Discard", "Keep editing"
    ),
    "it" to studio(
        "Playlist Studio", "Nuova playlist", "Modifica playlist", "Apri in Playlist Studio", "Dai un nome alla playlist", "Inserisci un nome per salvare",
        "Copertina", "Attuale", "Automatica", "Artwork", "Mosaico", "Spotlight", "Segnale", "Foto", "Scegli l'artwork",
        "Aggiungi brani", "Cerca nella tua libreria", "Nella playlist",
        "Modifiche salvate", "Modifiche non salvate", "Salvataggio…", "Impossibile salvare la playlist", "Riprova",
        "Rimosso {title}", "Spostato {title}", "Annulla",
        "Inizia con qualche brano", "Aggiungi brani da preferiti, cronologia e playlist. Puoi riordinarli quando vuoi.",
        "La tua libreria è ancora vuota. Metti mi piace o ascolta un po' di musica per riempirla.", "Raccolgo la tua libreria…",
        "Scartare le modifiche?", "Le modifiche a questa playlist andranno perse.", "Scarta", "Continua a modificare"
    ),
    "es" to studio(
        "Playlist Studio", "Nueva playlist", "Editar playlist", "Abrir en Playlist Studio", "Ponle nombre a tu playlist", "Añade un nombre para guardar",
        "Portada", "Actual", "Automática", "Artwork", "Mosaico", "Destacada", "Señal", "Foto", "Elige el artwork",
        "Añadir canciones", "Busca en tu biblioteca", "En la playlist",
        "Cambios guardados", "Cambios sin guardar", "Guardando…", "No se pudo guardar la playlist", "Reintentar",
        "Eliminada {title}", "Movida {title}", "Deshacer",
        "Empieza con algunas canciones", "Añade canciones de favoritos, historial y playlists. Puedes reordenarlas cuando quieras.",
        "Tu biblioteca está vacía. Marca canciones como favoritas o escucha música para llenarla.", "Reuniendo tu biblioteca…",
        "¿Descartar cambios?", "Se perderán los cambios de esta playlist.", "Descartar", "Seguir editando"
    ),
    "fr" to studio(
        "Playlist Studio", "Nouvelle playlist", "Modifier la playlist", "Ouvrir dans Playlist Studio", "Nommez votre playlist", "Ajoutez un nom pour enregistrer",
        "Pochette", "Actuelle", "Automatique", "Artwork", "Mosaïque", "Vedette", "Signal", "Photo", "Choisir l'artwork",
        "Ajouter des titres", "Rechercher dans votre bibliothèque", "Dans la playlist",
        "Modifications enregistrées", "Modifications non enregistrées", "Enregistrement…", "Impossible d'enregistrer la playlist", "Réessayer",
        "{title} retiré", "{title} déplacé", "Annuler",
        "Commencez avec quelques titres", "Ajoutez des titres depuis vos favoris, votre historique et vos playlists. Vous pourrez les réordonner à tout moment.",
        "Votre bibliothèque est vide pour l'instant. Aimez des titres ou écoutez de la musique pour la remplir.", "Préparation de votre bibliothèque…",
        "Abandonner les modifications ?", "Les modifications de cette playlist seront perdues.", "Abandonner", "Continuer"
    ),
    "de" to studio(
        "Playlist Studio", "Neue Playlist", "Playlist bearbeiten", "In Playlist Studio öffnen", "Benenne deine Playlist", "Zum Speichern einen Namen eingeben",
        "Cover", "Aktuell", "Automatisch", "Artwork", "Mosaik", "Spotlight", "Signal", "Foto", "Artwork auswählen",
        "Songs hinzufügen", "Bibliothek durchsuchen", "In der Playlist",
        "Alle Änderungen gespeichert", "Nicht gespeicherte Änderungen", "Wird gespeichert…", "Playlist konnte nicht gespeichert werden", "Erneut versuchen",
        "{title} entfernt", "{title} verschoben", "Rückgängig",
        "Starte mit ein paar Songs", "Füge Songs aus Favoriten, Verlauf und Playlists hinzu. Die Reihenfolge kannst du jederzeit ändern.",
        "Deine Bibliothek ist noch leer. Like Songs oder hör Musik, um sie zu füllen.", "Bibliothek wird gesammelt…",
        "Änderungen verwerfen?", "Deine Änderungen an dieser Playlist gehen verloren.", "Verwerfen", "Weiter bearbeiten"
    ),
    "pt" to studio(
        "Playlist Studio", "Nova playlist", "Editar playlist", "Abrir no Playlist Studio", "Dá um nome à playlist", "Adiciona um nome para guardar",
        "Capa", "Atual", "Automática", "Artwork", "Mosaico", "Destaque", "Sinal", "Foto", "Escolher o artwork",
        "Adicionar músicas", "Pesquisar na biblioteca", "Na playlist",
        "Alterações guardadas", "Alterações por guardar", "A guardar…", "Não foi possível guardar a playlist", "Tentar novamente",
        "{title} removida", "{title} movida", "Anular",
        "Começa com algumas músicas", "Adiciona músicas dos favoritos, histórico e playlists. Podes reordená-las a qualquer momento.",
        "A tua biblioteca ainda está vazia. Marca músicas como favoritas ou ouve música para a preencher.", "A reunir a tua biblioteca…",
        "Descartar alterações?", "As alterações a esta playlist serão perdidas.", "Descartar", "Continuar a editar"
    ),
    "nl" to studio(
        "Playlist Studio", "Nieuwe playlist", "Playlist bewerken", "Openen in Playlist Studio", "Geef je playlist een naam", "Voer een naam in om op te slaan",
        "Hoes", "Huidig", "Automatisch", "Artwork", "Mozaïek", "Spotlight", "Signaal", "Foto", "Kies de artwork",
        "Nummers toevoegen", "Zoek in je bibliotheek", "In playlist",
        "Alle wijzigingen opgeslagen", "Niet-opgeslagen wijzigingen", "Opslaan…", "Playlist kon niet worden opgeslagen", "Opnieuw proberen",
        "{title} verwijderd", "{title} verplaatst", "Ongedaan maken",
        "Begin met een paar nummers", "Voeg nummers toe uit favorieten, geschiedenis en playlists. Je kunt ze altijd herschikken.",
        "Je bibliotheek is nog leeg. Like nummers of luister muziek om hem te vullen.", "Bibliotheek verzamelen…",
        "Wijzigingen verwerpen?", "Je wijzigingen aan deze playlist gaan verloren.", "Verwerpen", "Verder bewerken"
    ),
    "pl" to studio(
        "Playlist Studio", "Nowa playlista", "Edytuj playlistę", "Otwórz w Playlist Studio", "Nazwij swoją playlistę", "Podaj nazwę, aby zapisać",
        "Okładka", "Obecna", "Automatyczna", "Artwork", "Mozaika", "Wyróżnienie", "Sygnał", "Zdjęcie", "Wybierz artwork",
        "Dodaj utwory", "Szukaj w bibliotece", "W playliście",
        "Wszystkie zmiany zapisane", "Niezapisane zmiany", "Zapisywanie…", "Nie udało się zapisać playlisty", "Ponów",
        "Usunięto {title}", "Przeniesiono {title}", "Cofnij",
        "Zacznij od kilku utworów", "Dodaj utwory z ulubionych, historii i playlist. Kolejność zmienisz w każdej chwili.",
        "Twoja biblioteka jest jeszcze pusta. Polub utwory lub posłuchaj muzyki, aby ją wypełnić.", "Zbieranie biblioteki…",
        "Odrzucić zmiany?", "Zmiany w tej playliście zostaną utracone.", "Odrzuć", "Edytuj dalej"
    ),
    "ro" to studio(
        "Playlist Studio", "Playlist nou", "Editează playlistul", "Deschide în Playlist Studio", "Denumește playlistul", "Adaugă un nume pentru a salva",
        "Copertă", "Actuală", "Automată", "Artwork", "Mozaic", "Spotlight", "Semnal", "Fotografie", "Alege artwork-ul",
        "Adaugă melodii", "Caută în bibliotecă", "În playlist",
        "Toate modificările salvate", "Modificări nesalvate", "Se salvează…", "Playlistul nu a putut fi salvat", "Reîncearcă",
        "Eliminat {title}", "Mutat {title}", "Anulează",
        "Începe cu câteva melodii", "Adaugă melodii din favorite, istoric și playlisturi. Le poți reordona oricând.",
        "Biblioteca ta e goală deocamdată. Apreciază melodii sau ascultă muzică pentru a o umple.", "Se adună biblioteca…",
        "Renunți la modificări?", "Modificările acestui playlist se vor pierde.", "Renunță", "Continuă editarea"
    ),
    "el" to studio(
        "Playlist Studio", "Νέα λίστα", "Επεξεργασία λίστας", "Άνοιγμα στο Playlist Studio", "Δώσε όνομα στη λίστα", "Πρόσθεσε όνομα για αποθήκευση",
        "Εξώφυλλο", "Τρέχον", "Αυτόματο", "Artwork", "Μωσαϊκό", "Spotlight", "Σήμα", "Φωτογραφία", "Επίλεξε artwork",
        "Προσθήκη τραγουδιών", "Αναζήτηση στη βιβλιοθήκη", "Στη λίστα",
        "Όλες οι αλλαγές αποθηκεύτηκαν", "Μη αποθηκευμένες αλλαγές", "Αποθήκευση…", "Δεν ήταν δυνατή η αποθήκευση της λίστας", "Επανάληψη",
        "Αφαιρέθηκε: {title}", "Μετακινήθηκε: {title}", "Αναίρεση",
        "Ξεκίνα με μερικά τραγούδια", "Πρόσθεσε τραγούδια από αγαπημένα, ιστορικό και λίστες. Μπορείς να αλλάξεις τη σειρά οποτεδήποτε.",
        "Η βιβλιοθήκη σου είναι άδεια προς το παρόν. Κάνε like ή άκουσε μουσική για να γεμίσει.", "Συγκέντρωση βιβλιοθήκης…",
        "Απόρριψη αλλαγών;", "Οι αλλαγές σε αυτή τη λίστα θα χαθούν.", "Απόρριψη", "Συνέχεια επεξεργασίας"
    ),
    "sv" to studio(
        "Playlist Studio", "Ny spellista", "Redigera spellista", "Öppna i Playlist Studio", "Namnge spellistan", "Ange ett namn för att spara",
        "Omslag", "Nuvarande", "Automatiskt", "Artwork", "Mosaik", "Spotlight", "Signal", "Foto", "Välj artwork",
        "Lägg till låtar", "Sök i biblioteket", "I spellistan",
        "Alla ändringar sparade", "Osparade ändringar", "Sparar…", "Det gick inte att spara spellistan", "Försök igen",
        "Tog bort {title}", "Flyttade {title}", "Ångra",
        "Börja med några låtar", "Lägg till låtar från favoriter, historik och spellistor. Du kan ändra ordningen när som helst.",
        "Ditt bibliotek är tomt än så länge. Gilla låtar eller lyssna på musik för att fylla det.", "Samlar ditt bibliotek…",
        "Ignorera ändringar?", "Dina ändringar i spellistan går förlorade.", "Ignorera", "Fortsätt redigera"
    ),
    "da" to studio(
        "Playlist Studio", "Ny playliste", "Rediger playliste", "Åbn i Playlist Studio", "Giv playlisten et navn", "Angiv et navn for at gemme",
        "Cover", "Nuværende", "Automatisk", "Artwork", "Mosaik", "Spotlight", "Signal", "Foto", "Vælg artwork",
        "Tilføj sange", "Søg i dit bibliotek", "I playlisten",
        "Alle ændringer gemt", "Ikke-gemte ændringer", "Gemmer…", "Playlisten kunne ikke gemmes", "Prøv igen",
        "Fjernede {title}", "Flyttede {title}", "Fortryd",
        "Start med et par sange", "Tilføj sange fra favoritter, historik og playlister. Du kan ændre rækkefølgen når som helst.",
        "Dit bibliotek er tomt indtil videre. Synes godt om sange eller lyt til musik for at fylde det.", "Samler dit bibliotek…",
        "Kassér ændringer?", "Dine ændringer i playlisten går tabt.", "Kassér", "Fortsæt redigering"
    ),
    "cs" to studio(
        "Playlist Studio", "Nový playlist", "Upravit playlist", "Otevřít v Playlist Studio", "Pojmenuj playlist", "Pro uložení zadej název",
        "Obal", "Aktuální", "Automatický", "Artwork", "Mozaika", "Spotlight", "Signál", "Fotka", "Vybrat artwork",
        "Přidat skladby", "Hledat v knihovně", "V playlistu",
        "Všechny změny uloženy", "Neuložené změny", "Ukládání…", "Playlist se nepodařilo uložit", "Zkusit znovu",
        "Odebráno: {title}", "Přesunuto: {title}", "Zpět",
        "Začni několika skladbami", "Přidej skladby z oblíbených, historie a playlistů. Pořadí můžeš kdykoli změnit.",
        "Tvoje knihovna je zatím prázdná. Označ skladby jako oblíbené nebo si pusť hudbu.", "Načítání knihovny…",
        "Zahodit změny?", "Změny v tomto playlistu budou ztraceny.", "Zahodit", "Pokračovat v úpravách"
    ),
    "uk" to studio(
        "Playlist Studio", "Новий плейлист", "Редагувати плейлист", "Відкрити в Playlist Studio", "Назвіть плейлист", "Додайте назву, щоб зберегти",
        "Обкладинка", "Поточна", "Автоматична", "Artwork", "Мозаїка", "Акцент", "Сигнал", "Фото", "Вибрати artwork",
        "Додати пісні", "Пошук у бібліотеці", "У плейлисті",
        "Усі зміни збережено", "Незбережені зміни", "Збереження…", "Не вдалося зберегти плейлист", "Повторити",
        "Видалено: {title}", "Переміщено: {title}", "Скасувати",
        "Почніть із кількох пісень", "Додавайте пісні з улюблених, історії та плейлистів. Порядок можна змінити будь-коли.",
        "Ваша бібліотека поки порожня. Вподобайте пісні або слухайте музику, щоб її наповнити.", "Збираємо бібліотеку…",
        "Скасувати зміни?", "Зміни в цьому плейлисті буде втрачено.", "Скасувати зміни", "Продовжити редагування"
    ),
    "ru" to studio(
        "Playlist Studio", "Новый плейлист", "Редактировать плейлист", "Открыть в Playlist Studio", "Назовите плейлист", "Введите название, чтобы сохранить",
        "Обложка", "Текущая", "Автоматическая", "Artwork", "Мозаика", "Акцент", "Сигнал", "Фото", "Выбрать artwork",
        "Добавить песни", "Поиск в библиотеке", "В плейлисте",
        "Все изменения сохранены", "Несохранённые изменения", "Сохранение…", "Не удалось сохранить плейлист", "Повторить",
        "Удалено: {title}", "Перемещено: {title}", "Отменить",
        "Начните с нескольких песен", "Добавляйте песни из избранного, истории и плейлистов. Порядок можно изменить в любой момент.",
        "Ваша библиотека пока пуста. Отмечайте песни или слушайте музыку, чтобы её наполнить.", "Собираем библиотеку…",
        "Отменить изменения?", "Изменения в этом плейлисте будут потеряны.", "Отменить изменения", "Продолжить"
    ),
    "tr" to studio(
        "Playlist Studio", "Yeni çalma listesi", "Çalma listesini düzenle", "Playlist Studio'da aç", "Çalma listene bir ad ver", "Kaydetmek için bir ad gir",
        "Kapak", "Mevcut", "Otomatik", "Artwork", "Mozaik", "Öne çıkan", "Sinyal", "Fotoğraf", "Artwork seç",
        "Şarkı ekle", "Kitaplığında ara", "Listede",
        "Tüm değişiklikler kaydedildi", "Kaydedilmemiş değişiklikler", "Kaydediliyor…", "Çalma listesi kaydedilemedi", "Tekrar dene",
        "{title} kaldırıldı", "{title} taşındı", "Geri al",
        "Birkaç şarkıyla başla", "Favorilerinden, geçmişinden ve listelerinden şarkı ekle. Sıralamayı istediğin zaman değiştirebilirsin.",
        "Kitaplığın şimdilik boş. Doldurmak için şarkı beğen ya da müzik dinle.", "Kitaplığın toplanıyor…",
        "Değişiklikler silinsin mi?", "Bu listedeki değişikliklerin kaybolacak.", "Sil", "Düzenlemeye devam et"
    ),
    "ar" to studio(
        "Playlist Studio", "قائمة تشغيل جديدة", "تعديل قائمة التشغيل", "فتح في Playlist Studio", "سمِّ قائمة التشغيل", "أضف اسمًا للحفظ",
        "الغلاف", "الحالي", "تلقائي", "Artwork", "فسيفساء", "إبراز", "إشارة", "صورة", "اختر الغلاف",
        "إضافة أغانٍ", "ابحث في مكتبتك", "في القائمة",
        "تم حفظ كل التغييرات", "تغييرات غير محفوظة", "جارٍ الحفظ…", "تعذّر حفظ قائمة التشغيل", "إعادة المحاولة",
        "تمت إزالة {title}", "تم نقل {title}", "تراجع",
        "ابدأ ببعض الأغاني", "أضف أغاني من المفضلة والسجل وقوائم التشغيل. يمكنك إعادة ترتيبها في أي وقت.",
        "مكتبتك فارغة حاليًا. أعجب بالأغاني أو استمع إلى الموسيقى لملئها.", "جارٍ تجميع مكتبتك…",
        "تجاهل التغييرات؟", "ستفقد تعديلاتك على هذه القائمة.", "تجاهل", "متابعة التعديل"
    ),
    "zh" to studio(
        "Playlist Studio", "新建歌单", "编辑歌单", "在 Playlist Studio 中打开", "为歌单命名", "输入名称后即可保存",
        "封面", "当前", "自动", "Artwork", "拼贴", "聚光", "信号", "照片", "选择封面",
        "添加歌曲", "搜索你的音乐库", "已在歌单中",
        "所有更改已保存", "有未保存的更改", "正在保存…", "无法保存歌单", "重试",
        "已移除 {title}", "已移动 {title}", "撤销",
        "先添加几首歌", "从收藏、历史和歌单中添加歌曲，随时可以调整顺序。",
        "你的音乐库暂时是空的。收藏歌曲或播放音乐来充实它。", "正在整理你的音乐库…",
        "放弃更改？", "对此歌单的修改将会丢失。", "放弃", "继续编辑"
    ),
    "ja" to studio(
        "Playlist Studio", "新しいプレイリスト", "プレイリストを編集", "Playlist Studio で開く", "プレイリストに名前を付ける", "保存するには名前を入力してください",
        "カバー", "現在のカバー", "自動", "Artwork", "モザイク", "スポットライト", "シグナル", "写真", "アートワークを選択",
        "曲を追加", "ライブラリを検索", "追加済み",
        "すべての変更を保存しました", "未保存の変更があります", "保存中…", "プレイリストを保存できませんでした", "再試行",
        "{title} を削除しました", "{title} を移動しました", "元に戻す",
        "まずは数曲から", "お気に入り、履歴、プレイリストから曲を追加できます。順番はいつでも変更できます。",
        "ライブラリはまだ空です。曲をお気に入りにしたり再生したりするとここに表示されます。", "ライブラリを準備しています…",
        "変更を破棄しますか？", "このプレイリストへの変更は失われます。", "破棄", "編集を続ける"
    ),
    "ko" to studio(
        "Playlist Studio", "새 플레이리스트", "플레이리스트 편집", "Playlist Studio에서 열기", "플레이리스트 이름 짓기", "저장하려면 이름을 입력하세요",
        "커버", "현재", "자동", "Artwork", "모자이크", "스포트라이트", "시그널", "사진", "아트워크 선택",
        "곡 추가", "보관함에서 검색", "추가됨",
        "모든 변경사항 저장됨", "저장되지 않은 변경사항", "저장 중…", "플레이리스트를 저장할 수 없습니다", "다시 시도",
        "{title} 삭제됨", "{title} 이동됨", "실행 취소",
        "몇 곡으로 시작해 보세요", "좋아요, 기록, 플레이리스트에서 곡을 추가하세요. 순서는 언제든 바꿀 수 있습니다.",
        "보관함이 아직 비어 있습니다. 곡에 좋아요를 누르거나 음악을 재생해 채워 보세요.", "보관함을 불러오는 중…",
        "변경사항을 삭제할까요?", "이 플레이리스트의 수정 내용이 사라집니다.", "삭제", "계속 편집"
    ),
    "hi" to studio(
        "Playlist Studio", "नई प्लेलिस्ट", "प्लेलिस्ट संपादित करें", "Playlist Studio में खोलें", "प्लेलिस्ट को नाम दें", "सेव करने के लिए नाम डालें",
        "कवर", "मौजूदा", "स्वचालित", "Artwork", "मोज़ेक", "स्पॉटलाइट", "सिग्नल", "फ़ोटो", "आर्टवर्क चुनें",
        "गाने जोड़ें", "अपनी लाइब्रेरी में खोजें", "प्लेलिस्ट में",
        "सभी बदलाव सेव हो गए", "बिना सेव किए बदलाव", "सेव हो रहा है…", "प्लेलिस्ट सेव नहीं हो सकी", "फिर से कोशिश करें",
        "{title} हटाया गया", "{title} खिसकाया गया", "पूर्ववत करें",
        "कुछ गानों से शुरुआत करें", "पसंदीदा, इतिहास और प्लेलिस्ट से गाने जोड़ें। क्रम कभी भी बदल सकते हैं।",
        "आपकी लाइब्रेरी अभी खाली है। गाने पसंद करें या संगीत चलाएँ।", "आपकी लाइब्रेरी जुटाई जा रही है…",
        "बदलाव छोड़ें?", "इस प्लेलिस्ट के बदलाव खो जाएँगे।", "छोड़ें", "संपादन जारी रखें"
    ),
    "id" to studio(
        "Playlist Studio", "Playlist baru", "Edit playlist", "Buka di Playlist Studio", "Beri nama playlist", "Masukkan nama untuk menyimpan",
        "Sampul", "Saat ini", "Otomatis", "Artwork", "Mosaik", "Sorotan", "Sinyal", "Foto", "Pilih artwork",
        "Tambah lagu", "Cari di koleksi", "Di playlist",
        "Semua perubahan tersimpan", "Perubahan belum disimpan", "Menyimpan…", "Playlist tidak dapat disimpan", "Coba lagi",
        "{title} dihapus", "{title} dipindahkan", "Urungkan",
        "Mulai dengan beberapa lagu", "Tambahkan lagu dari favorit, riwayat, dan playlist. Urutan bisa diubah kapan saja.",
        "Koleksimu masih kosong. Sukai lagu atau putar musik untuk mengisinya.", "Mengumpulkan koleksimu…",
        "Buang perubahan?", "Perubahan pada playlist ini akan hilang.", "Buang", "Lanjut mengedit"
    ),
    "vi" to studio(
        "Playlist Studio", "Danh sách phát mới", "Chỉnh sửa danh sách phát", "Mở trong Playlist Studio", "Đặt tên danh sách phát", "Nhập tên để lưu",
        "Ảnh bìa", "Hiện tại", "Tự động", "Artwork", "Ghép ảnh", "Tiêu điểm", "Tín hiệu", "Ảnh", "Chọn artwork",
        "Thêm bài hát", "Tìm trong thư viện", "Đã có",
        "Đã lưu mọi thay đổi", "Có thay đổi chưa lưu", "Đang lưu…", "Không thể lưu danh sách phát", "Thử lại",
        "Đã xóa {title}", "Đã di chuyển {title}", "Hoàn tác",
        "Bắt đầu với vài bài hát", "Thêm bài hát từ yêu thích, lịch sử và danh sách phát. Có thể sắp xếp lại bất cứ lúc nào.",
        "Thư viện của bạn đang trống. Hãy thích bài hát hoặc nghe nhạc để lấp đầy.", "Đang tập hợp thư viện…",
        "Bỏ thay đổi?", "Các chỉnh sửa cho danh sách này sẽ mất.", "Bỏ", "Tiếp tục chỉnh sửa"
    ),
    "th" to studio(
        "Playlist Studio", "เพลย์ลิสต์ใหม่", "แก้ไขเพลย์ลิสต์", "เปิดใน Playlist Studio", "ตั้งชื่อเพลย์ลิสต์", "ใส่ชื่อเพื่อบันทึก",
        "หน้าปก", "ปัจจุบัน", "อัตโนมัติ", "Artwork", "โมเสก", "สปอตไลต์", "สัญญาณ", "รูปภาพ", "เลือกอาร์ตเวิร์ก",
        "เพิ่มเพลง", "ค้นหาในคลัง", "อยู่ในเพลย์ลิสต์",
        "บันทึกการเปลี่ยนแปลงแล้ว", "มีการเปลี่ยนแปลงที่ยังไม่บันทึก", "กำลังบันทึก…", "บันทึกเพลย์ลิสต์ไม่สำเร็จ", "ลองอีกครั้ง",
        "นำ {title} ออกแล้ว", "ย้าย {title} แล้ว", "เลิกทำ",
        "เริ่มด้วยเพลงสักสองสามเพลง", "เพิ่มเพลงจากรายการโปรด ประวัติ และเพลย์ลิสต์ จัดลำดับใหม่ได้ทุกเมื่อ",
        "คลังของคุณยังว่างอยู่ กดถูกใจเพลงหรือเปิดเพลงเพื่อเติมคลัง", "กำลังรวบรวมคลังของคุณ…",
        "ทิ้งการเปลี่ยนแปลง?", "การแก้ไขเพลย์ลิสต์นี้จะหายไป", "ทิ้ง", "แก้ไขต่อ"
    ),
    "fil" to studio(
        "Playlist Studio", "Bagong playlist", "I-edit ang playlist", "Buksan sa Playlist Studio", "Pangalanan ang playlist", "Maglagay ng pangalan para i-save",
        "Cover", "Kasalukuyan", "Awtomatiko", "Artwork", "Mosaic", "Spotlight", "Signal", "Larawan", "Pumili ng artwork",
        "Magdagdag ng kanta", "Maghanap sa library", "Nasa playlist",
        "Na-save ang lahat ng pagbabago", "Mga pagbabagong hindi naka-save", "Sine-save…", "Hindi na-save ang playlist", "Subukang muli",
        "Inalis ang {title}", "Inilipat ang {title}", "I-undo",
        "Magsimula sa ilang kanta", "Magdagdag mula sa favorites, history at mga playlist. Maaaring baguhin ang order anumang oras.",
        "Walang laman ang iyong library. Mag-like ng kanta o magpatugtog ng musika.", "Tinitipon ang iyong library…",
        "I-discard ang mga pagbabago?", "Mawawala ang mga edit sa playlist na ito.", "I-discard", "Magpatuloy sa pag-edit"
    ),
    "he" to studio(
        "Playlist Studio", "פלייליסט חדש", "עריכת פלייליסט", "פתיחה ב-Playlist Studio", "תן שם לפלייליסט", "הזן שם כדי לשמור",
        "עטיפה", "נוכחית", "אוטומטית", "Artwork", "פסיפס", "זרקור", "אות", "תמונה", "בחירת עטיפה",
        "הוספת שירים", "חיפוש בספרייה", "בפלייליסט",
        "כל השינויים נשמרו", "שינויים שלא נשמרו", "שומר…", "לא ניתן לשמור את הפלייליסט", "ניסיון חוזר",
        "{title} הוסר", "{title} הועבר", "ביטול",
        "התחל עם כמה שירים", "הוסף שירים מהמועדפים, מההיסטוריה ומהפלייליסטים. אפשר לשנות את הסדר בכל עת.",
        "הספרייה שלך ריקה כרגע. סמן שירים כאהובים או האזן למוזיקה כדי למלא אותה.", "אוסף את הספרייה שלך…",
        "לבטל את השינויים?", "העריכות בפלייליסט הזה יאבדו.", "בטל שינויים", "המשך לערוך"
    )
)

internal fun playlistStudioLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(playlistStudioBundles, code)

internal fun playlistStudioLocalizationCodes(): Set<String> = playlistStudioBundles.keys
