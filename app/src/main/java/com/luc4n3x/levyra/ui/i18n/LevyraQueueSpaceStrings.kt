package com.luc4n3x.levyra.ui.i18n

internal val queueSpaceKeys = setOf(
    "queueSpaces",
    "queueSpacesSubtitle",
    "queueSpaceDefaultName",
    "queueSpaceNew",
    "queueSpaceNameHint",
    "queueSpaceRename",
    "queueSpaceDuplicate",
    "queueSpaceClear",
    "queueSpaceDelete",
    "queueSpaceDeleteConfirm",
    "queueSpaceDeleteLast",
    "queueSpaceAddTo",
    "queueSpaceSwitched",
    "queueSpaceEmpty"
)

private fun queueSpace(
    title: String,
    subtitle: String,
    defaultName: String,
    new: String,
    nameHint: String,
    rename: String,
    duplicate: String,
    clear: String,
    delete: String,
    deleteConfirm: String,
    deleteLast: String,
    addTo: String,
    switched: String,
    empty: String
): Map<String, String> = mapOf(
    "queueSpaces" to title,
    "queueSpacesSubtitle" to subtitle,
    "queueSpaceDefaultName" to defaultName,
    "queueSpaceNew" to new,
    "queueSpaceNameHint" to nameHint,
    "queueSpaceRename" to rename,
    "queueSpaceDuplicate" to duplicate,
    "queueSpaceClear" to clear,
    "queueSpaceDelete" to delete,
    "queueSpaceDeleteConfirm" to deleteConfirm,
    "queueSpaceDeleteLast" to deleteLast,
    "queueSpaceAddTo" to addTo,
    "queueSpaceSwitched" to switched,
    "queueSpaceEmpty" to empty
)

private val queueSpaceBundles: Map<String, Map<String, String>> = mapOf(
    "en" to queueSpace(
        "Queues", "Every queue keeps its own order and position.", "Current", "New queue", "Queue name",
        "Rename queue", "Duplicate queue", "Empty queue", "Delete queue",
        "Delete this queue? Your songs stay in the library.",
        "Levyra always keeps one queue. Empty it instead.", "Add to queue", "Now playing from %1\$s", "Still empty"
    ),
    "it" to queueSpace(
        "Code", "Ogni coda mantiene il suo ordine e la sua posizione.", "Attuale", "Nuova coda", "Nome della coda",
        "Rinomina coda", "Duplica coda", "Svuota coda", "Elimina coda",
        "Eliminare questa coda? I brani restano nella libreria.",
        "Levyra tiene sempre una coda. Puoi svuotarla.", "Aggiungi alla coda", "Ora ascolti da %1\$s", "Ancora vuota"
    ),
    "es" to queueSpace(
        "Colas", "Cada cola guarda su orden y su posición.", "Actual", "Nueva cola", "Nombre de la cola",
        "Cambiar nombre", "Duplicar cola", "Vaciar cola", "Eliminar cola",
        "¿Eliminar esta cola? Tus canciones siguen en la biblioteca.",
        "Levyra siempre mantiene una cola. Vacíala en su lugar.", "Añadir a la cola", "Ahora suena %1\$s",
        "Aún vacía"
    ),
    "fr" to queueSpace(
        "Files", "Chaque file garde son ordre et sa position.", "Actuelle", "Nouvelle file", "Nom de la file",
        "Renommer la file", "Dupliquer la file", "Vider la file", "Supprimer la file",
        "Supprimer cette file ? Vos morceaux restent dans la bibliothèque.",
        "Levyra garde toujours une file. Videz-la plutôt.", "Ajouter à la file", "Lecture depuis %1\$s",
        "Encore vide"
    ),
    "de" to queueSpace(
        "Warteschlangen", "Jede Warteschlange behält Reihenfolge und Position.", "Aktuell", "Neue Warteschlange",
        "Name der Warteschlange", "Umbenennen", "Duplizieren", "Leeren", "Löschen",
        "Diese Warteschlange löschen? Deine Titel bleiben in der Bibliothek.",
        "Levyra behält immer eine Warteschlange. Leere sie stattdessen.", "Zur Warteschlange",
        "Läuft jetzt aus %1\$s", "Noch leer"
    ),
    "pt" to queueSpace(
        "Filas", "Cada fila guarda a sua ordem e posição.", "Atual", "Nova fila", "Nome da fila",
        "Mudar o nome", "Duplicar fila", "Esvaziar fila", "Eliminar fila",
        "Eliminar esta fila? As músicas ficam na biblioteca.",
        "O Levyra mantém sempre uma fila. Esvazia-a.", "Adicionar à fila", "A tocar de %1\$s", "Ainda vazia"
    ),
    "nl" to queueSpace(
        "Wachtrijen", "Elke wachtrij houdt eigen volgorde en positie bij.", "Huidige", "Nieuwe wachtrij",
        "Naam van de wachtrij", "Naam wijzigen", "Wachtrij dupliceren", "Wachtrij leegmaken",
        "Wachtrij verwijderen", "Deze wachtrij verwijderen? Je nummers blijven in de bibliotheek.",
        "Levyra houdt altijd één wachtrij. Maak deze leeg.", "Aan wachtrij toevoegen", "Speelt nu uit %1\$s",
        "Nog leeg"
    ),
    "pl" to queueSpace(
        "Kolejki", "Każda kolejka zachowuje własną kolejność i pozycję.", "Bieżąca", "Nowa kolejka", "Nazwa kolejki",
        "Zmień nazwę", "Duplikuj kolejkę", "Opróżnij kolejkę", "Usuń kolejkę",
        "Usunąć tę kolejkę? Utwory zostaną w bibliotece.",
        "Levyra zawsze zachowuje jedną kolejkę. Opróżnij ją.", "Dodaj do kolejki", "Teraz odtwarzasz z %1\$s",
        "Nadal pusta"
    ),
    "ro" to queueSpace(
        "Cozi", "Fiecare coadă își păstrează ordinea și poziția.", "Curentă", "Coadă nouă", "Numele cozii",
        "Redenumește coada", "Duplică coada", "Golește coada", "Șterge coada",
        "Ștergi această coadă? Piesele rămân în bibliotecă.",
        "Levyra păstrează mereu o coadă. Golește-o în schimb.", "Adaugă în coadă", "Acum asculți din %1\$s",
        "Încă goală"
    ),
    "el" to queueSpace(
        "Ουρές", "Κάθε ουρά κρατά τη σειρά και τη θέση της.", "Τρέχουσα", "Νέα ουρά", "Όνομα ουράς",
        "Μετονομασία ουράς", "Αντιγραφή ουράς", "Άδειασμα ουράς", "Διαγραφή ουράς",
        "Να διαγραφεί αυτή η ουρά; Τα κομμάτια παραμένουν στη βιβλιοθήκη.",
        "Το Levyra κρατά πάντα μία ουρά. Άδειασέ την.", "Προσθήκη στην ουρά", "Τώρα ακούς από %1\$s", "Ακόμη κενή"
    ),
    "sv" to queueSpace(
        "Köer", "Varje kö behåller sin ordning och position.", "Aktuell", "Ny kö", "Könamn",
        "Byt namn på kön", "Duplicera kön", "Töm kön", "Ta bort kön",
        "Ta bort den här kön? Låtarna finns kvar i biblioteket.",
        "Levyra behåller alltid en kö. Töm den i stället.", "Lägg till i kön", "Spelar nu från %1\$s",
        "Fortfarande tom"
    ),
    "da" to queueSpace(
        "Køer", "Hver kø husker sin egen rækkefølge og position.", "Aktuel", "Ny kø", "Navn på kø",
        "Omdøb kø", "Dupliker kø", "Tøm kø", "Slet kø",
        "Slet denne kø? Dine numre forbliver i biblioteket.",
        "Levyra beholder altid én kø. Tøm den i stedet.", "Tilføj til kø", "Spiller nu fra %1\$s", "Stadig tom"
    ),
    "cs" to queueSpace(
        "Fronty", "Každá fronta si drží vlastní pořadí a pozici.", "Aktuální", "Nová fronta",
        "Název fronty", "Přejmenovat frontu", "Duplikovat frontu", "Vyprázdnit frontu", "Odstranit frontu",
        "Odstranit tuto frontu? Skladby zůstanou v knihovně.",
        "Levyra si vždy nechá jednu frontu. Vyprázdni ji.", "Přidat do fronty", "Teď přehráváš z %1\$s",
        "Stále prázdná"
    ),
    "uk" to queueSpace(
        "Черги", "Кожна черга зберігає власний порядок і позицію.", "Поточна", "Нова черга", "Назва черги",
        "Перейменувати чергу", "Дублювати чергу", "Очистити чергу", "Видалити чергу",
        "Видалити цю чергу? Треки залишаться в бібліотеці.",
        "Levyra завжди залишає одну чергу. Краще очисти її.", "Додати до черги", "Зараз грає з %1\$s",
        "Досі порожня"
    ),
    "ru" to queueSpace(
        "Очереди", "Каждая очередь хранит свой порядок и позицию.", "Текущая", "Новая очередь", "Название очереди",
        "Переименовать очередь", "Дублировать очередь", "Очистить очередь", "Удалить очередь",
        "Удалить эту очередь? Треки останутся в библиотеке.",
        "Levyra всегда оставляет одну очередь. Лучше очисти её.", "Добавить в очередь", "Сейчас играет из %1\$s",
        "Пока пусто"
    ),
    "tr" to queueSpace(
        "Kuyruklar", "Her kuyruk kendi sırasını ve konumunu korur.", "Geçerli", "Yeni kuyruk", "Kuyruk adı",
        "Kuyruğu yeniden adlandır", "Kuyruğu çoğalt", "Kuyruğu boşalt", "Kuyruğu sil",
        "Bu kuyruk silinsin mi? Parçalar kitaplıkta kalır.",
        "Levyra her zaman bir kuyruk tutar. Bunun yerine boşalt.", "Kuyruğa ekle",
        "Şimdi %1\$s kuyruğundan çalıyor", "Hâlâ boş"
    ),
    "ar" to queueSpace(
        "قوائم الانتظار", "كل قائمة تحفظ ترتيبها وموضعها.", "الحالية", "قائمة جديدة", "اسم القائمة",
        "إعادة تسمية القائمة", "تكرار القائمة", "إفراغ القائمة", "حذف القائمة",
        "حذف هذه القائمة؟ ستبقى المقاطع في مكتبتك.",
        "يحتفظ Levyra دائمًا بقائمة واحدة. أفرغها بدلاً من ذلك.", "إضافة إلى القائمة", "يُشغّل الآن من %1\$s",
        "ما زالت فارغة"
    ),
    "zh" to queueSpace(
        "播放队列", "每个队列都会保留自己的顺序和进度。", "当前", "新建队列", "队列名称",
        "重命名队列", "复制队列", "清空队列", "删除队列",
        "删除这个队列？歌曲仍会保留在音乐库中。",
        "Levyra 始终保留一个队列，可以先清空它。", "添加到队列", "正在播放 %1\$s", "仍然是空的"
    ),
    "zh-Hant" to queueSpace(
        "播放佇列", "每個佇列都會保留自己的順序與進度。", "目前", "新增佇列", "佇列名稱",
        "重新命名佇列", "複製佇列", "清空佇列", "刪除佇列",
        "要刪除這個佇列嗎？歌曲仍會留在音樂庫中。",
        "Levyra 一定會保留一個佇列，可以先清空它。", "加入佇列", "正在播放 %1\$s", "仍然是空的"
    ),
    "ja" to queueSpace(
        "キュー", "キューごとに並び順と再生位置を保ちます。", "現在", "新しいキュー", "キュー名",
        "キューの名前を変更", "キューを複製", "キューを空にする", "キューを削除",
        "このキューを削除しますか？曲はライブラリに残ります。",
        "Levyra は常に 1 つのキューを残します。空にしてください。", "キューに追加", "%1\$s から再生中", "まだ空です"
    ),
    "ko" to queueSpace(
        "대기열", "각 대기열은 순서와 재생 위치를 따로 기억합니다.", "현재", "새 대기열", "대기열 이름",
        "대기열 이름 변경", "대기열 복제", "대기열 비우기", "대기열 삭제",
        "이 대기열을 삭제할까요? 곡은 라이브러리에 남습니다.",
        "Levyra는 항상 대기열 하나를 유지합니다. 대신 비워 주세요.", "대기열에 추가", "%1\$s에서 재생 중",
        "아직 비어 있음"
    ),
    "hi" to queueSpace(
        "क़तारें", "हर क़तार अपना क्रम और स्थिति याद रखती है।", "वर्तमान", "नई क़तार", "क़तार का नाम",
        "क़तार का नाम बदलें", "क़तार की नकल बनाएं", "क़तार खाली करें", "क़तार हटाएं",
        "यह क़तार हटाएं? गाने लाइब्रेरी में रहेंगे।",
        "Levyra हमेशा एक क़तार रखता है। इसे खाली करें।", "क़तार में जोड़ें", "अब %1\$s से चल रहा है", "अभी खाली है"
    ),
    "id" to queueSpace(
        "Antrean", "Setiap antrean menyimpan urutan dan posisinya sendiri.", "Saat ini", "Antrean baru",
        "Nama antrean", "Ganti nama antrean", "Duplikat antrean", "Kosongkan antrean", "Hapus antrean",
        "Hapus antrean ini? Lagu tetap ada di pustaka.",
        "Levyra selalu menyimpan satu antrean. Kosongkan saja.", "Tambahkan ke antrean", "Kini diputar dari %1\$s",
        "Masih kosong"
    ),
    "vi" to queueSpace(
        "Hàng đợi", "Mỗi hàng đợi giữ thứ tự và vị trí riêng.", "Hiện tại", "Hàng đợi mới", "Tên hàng đợi",
        "Đổi tên hàng đợi", "Nhân bản hàng đợi", "Xóa nội dung hàng đợi", "Xóa hàng đợi",
        "Xóa hàng đợi này? Các bài hát vẫn ở trong thư viện.",
        "Levyra luôn giữ một hàng đợi. Hãy làm trống nó.", "Thêm vào hàng đợi", "Đang phát từ %1\$s", "Vẫn trống"
    ),
    "th" to queueSpace(
        "คิวเพลง", "แต่ละคิวจะจำลำดับและตำแหน่งของตัวเอง", "ปัจจุบัน", "คิวใหม่", "ชื่อคิว",
        "เปลี่ยนชื่อคิว", "ทำสำเนาคิว", "ล้างคิว", "ลบคิว",
        "ลบคิวนี้ไหม เพลงยังอยู่ในคลัง",
        "Levyra จะเก็บคิวไว้หนึ่งคิวเสมอ ล้างคิวแทนได้", "เพิ่มในคิว", "กำลังเล่นจาก %1\$s", "ยังว่างอยู่"
    ),
    "fil" to queueSpace(
        "Mga pila", "Bawat pila ay may sariling pagkakasunod at posisyon.", "Kasalukuyan", "Bagong pila",
        "Pangalan ng pila", "Palitan ang pangalan", "Kopyahin ang pila", "Alisan ng laman", "Tanggalin ang pila",
        "Tanggalin ang pilang ito? Nananatili sa library ang mga kanta.",
        "Laging may isang pila ang Levyra. Alisan na lang ng laman.", "Idagdag sa pila", "Tumutugtog mula sa %1\$s",
        "Wala pa ring laman"
    ),
    "he" to queueSpace(
        "תורים", "כל תור שומר את הסדר והמקום שלו.", "נוכחי", "תור חדש", "שם התור",
        "שינוי שם התור", "שכפול התור", "ריקון התור", "מחיקת התור",
        "למחוק את התור הזה? השירים יישארו בספרייה.",
        "Levyra תמיד משאירה תור אחד. אפשר לרוקן אותו.", "הוספה לתור", "מתנגן כעת מתוך %1\$s", "עדיין ריק"
    ),
    "sk" to queueSpace(
        "Frontové zoznamy", "Každý front si drží vlastné poradie a pozíciu.", "Aktuálny", "Nový front",
        "Názov frontu", "Premenovať front", "Duplikovať front", "Vyprázdniť front", "Odstrániť front",
        "Odstrániť tento front? Skladby zostanú v knižnici.",
        "Levyra si vždy ponechá jeden front. Vyprázdni ho.", "Pridať do frontu", "Teraz prehrávaš z %1\$s",
        "Stále prázdny"
    ),
    "hr" to queueSpace(
        "Redovi", "Svaki red pamti vlastiti poredak i položaj.", "Trenutačni", "Novi red", "Naziv reda",
        "Preimenuj red", "Dupliciraj red", "Isprazni red", "Izbriši red",
        "Izbrisati ovaj red? Pjesme ostaju u tvojoj knjižnici.",
        "Levyra uvijek zadrži jedan red. Radije ga isprazni.", "Dodaj u red", "Sada slušaš iz %1\$s",
        "Još je prazan"
    ),
    "bg" to queueSpace(
        "Опашки", "Всяка опашка помни своя ред и позиция.", "Текуща", "Нова опашка", "Име на опашката",
        "Преименуване на опашката", "Дублиране на опашката", "Изчистване на опашката", "Изтриване на опашката",
        "Да се изтрие ли тази опашка? Песните остават в библиотеката.",
        "Levyra винаги пази една опашка. Изчисти я вместо това.", "Добавяне в опашката", "Сега слушаш от %1\$s",
        "Все още е празна"
    ),
    "hu" to queueSpace(
        "Várólisták", "Minden várólista megőrzi a saját sorrendjét és pozícióját.", "Jelenlegi", "Új várólista",
        "Várólista neve", "Várólista átnevezése", "Várólista duplikálása", "Várólista kiürítése",
        "Várólista törlése", "Törlöd ezt a várólistát? A számok a könyvtárban maradnak.",
        "A Levyra mindig megtart egy várólistát. Inkább ürítsd ki.", "Hozzáadás a várólistához",
        "Most a %1\$s listából szól", "Még üres"
    ),
    "fi" to queueSpace(
        "Jonot", "Jokainen jono muistaa oman järjestyksensä ja kohtansa.", "Nykyinen", "Uusi jono", "Jonon nimi",
        "Nimeä jono uudelleen", "Kopioi jono", "Tyhjennä jono", "Poista jono",
        "Poistetaanko tämä jono? Kappaleet jäävät kirjastoon.",
        "Levyra säilyttää aina yhden jonon. Tyhjennä se sen sijaan.", "Lisää jonoon", "Soittaa nyt jonosta %1\$s",
        "Vielä tyhjä"
    ),
    "nb" to queueSpace(
        "Køer", "Hver kø husker sin egen rekkefølge og posisjon.", "Gjeldende", "Ny kø", "Navn på kø",
        "Gi køen nytt navn", "Dupliser køen", "Tøm køen", "Slett køen",
        "Slette denne køen? Sporene blir liggende i biblioteket.",
        "Levyra beholder alltid én kø. Tøm den i stedet.", "Legg til i køen", "Spiller nå fra %1\$s", "Fortsatt tom"
    ),
    "ca" to queueSpace(
        "Cues", "Cada cua manté el seu ordre i la seva posició.", "Actual", "Cua nova", "Nom de la cua",
        "Canvia el nom de la cua", "Duplica la cua", "Buida la cua", "Elimina la cua",
        "Vols eliminar aquesta cua? Les cançons es queden a la biblioteca.",
        "Levyra manté sempre una cua. Buida-la millor.", "Afegeix a la cua", "Ara sona des de %1\$s", "Encara buida"
    ),
    "fa" to queueSpace(
        "صف‌ها", "هر صف ترتیب و جایگاه خودش را نگه می‌دارد.", "کنونی", "صف تازه", "نام صف",
        "تغییر نام صف", "تکثیر صف", "خالی کردن صف", "حذف صف",
        "این صف حذف شود؟ آهنگ‌ها در کتابخانه می‌مانند.",
        "Levyra همیشه یک صف نگه می‌دارد. به جایش آن را خالی کن.", "افزودن به صف", "اکنون از %1\$s پخش می‌شود",
        "هنوز خالی است"
    ),
    "ms" to queueSpace(
        "Baris gilir", "Setiap baris gilir menyimpan susunan dan posisinya sendiri.", "Semasa", "Baris gilir baharu",
        "Nama baris gilir", "Namakan semula", "Duplikasi baris gilir", "Kosongkan baris gilir",
        "Hapus baris gilir", "Hapus baris gilir ini? Lagu anda kekal dalam pustaka.",
        "Levyra sentiasa menyimpan satu baris gilir. Kosongkan sahaja.", "Tambah ke baris gilir",
        "Kini dimainkan dari %1\$s", "Masih kosong"
    )
)

internal fun queueSpaceLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(queueSpaceBundles, code)

internal fun queueSpaceLocalizationCodes(): Set<String> = supportedLocalizationCodes()
