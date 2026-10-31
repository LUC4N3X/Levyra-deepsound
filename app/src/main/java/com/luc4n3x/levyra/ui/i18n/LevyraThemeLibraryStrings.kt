package com.luc4n3x.levyra.ui.i18n

enum class ThemeLibraryText {
    DefaultBadge, Selected, RestoreDefault, RestoreConfirm, Reset,
    MyThemes, MyThemesEmpty, CreateTheme, EditorTitle, MoreActions,
    Edit, Duplicate, Rename, Delete, DeleteConfirm,
    Name, Light, Dark, Background, Surface,
    Primary, Secondary, Accent, Hex, Corners,
    Save, CopySuffix, NewThemeName
}

class ThemeLibraryCopy internal constructor(private val texts: Map<ThemeLibraryText, String>) {
    internal val brazilian: ThemeLibraryCopy by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ThemeLibraryCopy(texts.mapValues { (_, text) -> LevyraBrazilianPortuguese.adapt(text) })
    }

    operator fun get(key: ThemeLibraryText): String = texts[key] ?: englishThemeLibrary.getValue(key)
}

private fun themeLibrary(vararg value: String): Map<ThemeLibraryText, String> =
    if (value.size == ThemeLibraryText.entries.size) ThemeLibraryText.entries.zip(value).toMap() else emptyMap()

private val englishThemeLibrary: Map<ThemeLibraryText, String> = themeLibrary(
    "Default", "Selected", "Restore default", "Levyra returns to its original theme and accent. Your saved themes are kept.", "Restore",
    "My themes", "Create a theme to save your own colors and shapes.", "Create theme", "Theme editor", "More actions",
    "Edit", "Duplicate", "Rename", "Delete", "This theme will be deleted. This can't be undone.",
    "Name", "Light", "Dark", "Background", "Surfaces",
    "Primary", "Secondary", "Accent", "HEX code", "Corner roundness",
    "Save", "copy", "My theme"
)

private val themeLibraryCopies: Map<String, Map<ThemeLibraryText, String>> = mapOf(
    "en" to englishThemeLibrary,
    "it" to themeLibrary(
        "Predefinito", "Selezionato", "Ripristina predefinito", "Levyra torna al tema e all'accento originali. I tuoi temi salvati restano disponibili.", "Ripristina",
        "I miei temi", "Crea un tema per salvare i tuoi colori e le tue forme.", "Crea tema", "Editor del tema", "Altre azioni",
        "Modifica", "Duplica", "Rinomina", "Elimina", "Il tema verrà eliminato. L'operazione non si può annullare.",
        "Nome", "Chiaro", "Scuro", "Sfondo", "Superfici",
        "Primario", "Secondario", "Accento", "Codice HEX", "Arrotondamento angoli",
        "Salva", "copia", "Il mio tema"
    ),
    "es" to themeLibrary(
        "Predeterminado", "Seleccionado", "Restaurar predeterminado", "Levyra vuelve a su tema y acento originales. Tus temas guardados se conservan.", "Restaurar",
        "Mis temas", "Crea un tema para guardar tus propios colores y formas.", "Crear tema", "Editor de tema", "Más acciones",
        "Editar", "Duplicar", "Cambiar nombre", "Eliminar", "Este tema se eliminará. No se puede deshacer.",
        "Nombre", "Claro", "Oscuro", "Fondo", "Superficies",
        "Primario", "Secundario", "Acento", "Código HEX", "Redondeo de esquinas",
        "Guardar", "copia", "Mi tema"
    ),
    "fr" to themeLibrary(
        "Par défaut", "Sélectionné", "Restaurer par défaut", "Levyra revient à son thème et à son accent d'origine. Vos thèmes enregistrés sont conservés.", "Restaurer",
        "Mes thèmes", "Créez un thème pour enregistrer vos couleurs et vos formes.", "Créer un thème", "Éditeur de thème", "Plus d'actions",
        "Modifier", "Dupliquer", "Renommer", "Supprimer", "Ce thème sera supprimé. Cette action est irréversible.",
        "Nom", "Clair", "Sombre", "Arrière-plan", "Surfaces",
        "Principal", "Secondaire", "Accent", "Code HEX", "Arrondi des coins",
        "Enregistrer", "copie", "Mon thème"
    ),
    "de" to themeLibrary(
        "Standard", "Ausgewählt", "Standard wiederherstellen", "Levyra kehrt zu Originaldesign und -akzent zurück. Deine gespeicherten Designs bleiben erhalten.", "Wiederherstellen",
        "Meine Designs", "Erstelle ein Design, um eigene Farben und Formen zu speichern.", "Design erstellen", "Design-Editor", "Weitere Aktionen",
        "Bearbeiten", "Duplizieren", "Umbenennen", "Löschen", "Dieses Design wird gelöscht. Das kann nicht rückgängig gemacht werden.",
        "Name", "Hell", "Dunkel", "Hintergrund", "Flächen",
        "Primär", "Sekundär", "Akzent", "HEX-Code", "Eckenrundung",
        "Speichern", "Kopie", "Mein Design"
    ),
    "pt" to themeLibrary(
        "Padrão", "Selecionado", "Restaurar padrão", "O Levyra volta ao tema e à cor de destaque originais. Seus temas salvos são mantidos.", "Restaurar",
        "Meus temas", "Crie um tema para salvar suas próprias cores e formas.", "Criar tema", "Editor de tema", "Mais ações",
        "Editar", "Duplicar", "Renomear", "Excluir", "Este tema será excluído. Não é possível desfazer.",
        "Nome", "Claro", "Escuro", "Fundo", "Superfícies",
        "Primária", "Secundária", "Destaque", "Código HEX", "Arredondamento dos cantos",
        "Salvar", "cópia", "Meu tema"
    ),
    "nl" to themeLibrary(
        "Standaard", "Geselecteerd", "Standaard herstellen", "Levyra keert terug naar het originele thema en accent. Je opgeslagen thema's blijven bewaard.", "Herstellen",
        "Mijn thema's", "Maak een thema om je eigen kleuren en vormen op te slaan.", "Thema maken", "Thema-editor", "Meer acties",
        "Bewerken", "Dupliceren", "Naam wijzigen", "Verwijderen", "Dit thema wordt verwijderd. Dit kan niet ongedaan worden gemaakt.",
        "Naam", "Licht", "Donker", "Achtergrond", "Oppervlakken",
        "Primair", "Secundair", "Accent", "HEX-code", "Hoekafronding",
        "Opslaan", "kopie", "Mijn thema"
    ),
    "pl" to themeLibrary(
        "Domyślny", "Wybrany", "Przywróć domyślny", "Levyra wraca do oryginalnego motywu i akcentu. Zapisane motywy pozostają.", "Przywróć",
        "Moje motywy", "Utwórz motyw, aby zapisać własne kolory i kształty.", "Utwórz motyw", "Edytor motywu", "Więcej działań",
        "Edytuj", "Duplikuj", "Zmień nazwę", "Usuń", "Ten motyw zostanie usunięty. Tej operacji nie można cofnąć.",
        "Nazwa", "Jasny", "Ciemny", "Tło", "Powierzchnie",
        "Podstawowy", "Dodatkowy", "Akcent", "Kod HEX", "Zaokrąglenie narożników",
        "Zapisz", "kopia", "Mój motyw"
    ),
    "ro" to themeLibrary(
        "Implicit", "Selectat", "Restabilește implicit", "Levyra revine la tema și accentul originale. Temele salvate se păstrează.", "Restabilește",
        "Temele mele", "Creează o temă pentru a-ți salva culorile și formele.", "Creează temă", "Editor de temă", "Mai multe acțiuni",
        "Editează", "Duplică", "Redenumește", "Șterge", "Tema va fi ștearsă. Acțiunea nu poate fi anulată.",
        "Nume", "Luminoasă", "Întunecată", "Fundal", "Suprafețe",
        "Primară", "Secundară", "Accent", "Cod HEX", "Rotunjirea colțurilor",
        "Salvează", "copie", "Tema mea"
    ),
    "el" to themeLibrary(
        "Προεπιλογή", "Επιλεγμένο", "Επαναφορά προεπιλογής", "Το Levyra επιστρέφει στο αρχικό θέμα και χρώμα έμφασης. Τα αποθηκευμένα θέματά σου διατηρούνται.", "Επαναφορά",
        "Τα θέματά μου", "Δημιούργησε ένα θέμα για να αποθηκεύσεις τα δικά σου χρώματα και σχήματα.", "Δημιουργία θέματος", "Επεξεργασία θέματος", "Περισσότερες ενέργειες",
        "Επεξεργασία", "Αντιγραφή", "Μετονομασία", "Διαγραφή", "Το θέμα θα διαγραφεί. Η ενέργεια δεν αναιρείται.",
        "Όνομα", "Φωτεινό", "Σκοτεινό", "Φόντο", "Επιφάνειες",
        "Κύριο", "Δευτερεύον", "Έμφαση", "Κωδικός HEX", "Στρογγύλεμα γωνιών",
        "Αποθήκευση", "αντίγραφο", "Το θέμα μου"
    ),
    "sv" to themeLibrary(
        "Standard", "Vald", "Återställ standard", "Levyra återgår till sitt ursprungliga tema och accentfärg. Dina sparade teman finns kvar.", "Återställ",
        "Mina teman", "Skapa ett tema för att spara dina egna färger och former.", "Skapa tema", "Temaredigerare", "Fler åtgärder",
        "Redigera", "Duplicera", "Byt namn", "Radera", "Temat raderas. Det går inte att ångra.",
        "Namn", "Ljust", "Mörkt", "Bakgrund", "Ytor",
        "Primär", "Sekundär", "Accent", "HEX-kod", "Hörnrundning",
        "Spara", "kopia", "Mitt tema"
    ),
    "da" to themeLibrary(
        "Standard", "Valgt", "Gendan standard", "Levyra vender tilbage til det oprindelige tema og den oprindelige accentfarve. Dine gemte temaer bevares.", "Gendan",
        "Mine temaer", "Opret et tema for at gemme dine egne farver og former.", "Opret tema", "Temaeditor", "Flere handlinger",
        "Rediger", "Dupliker", "Omdøb", "Slet", "Temaet slettes. Det kan ikke fortrydes.",
        "Navn", "Lyst", "Mørkt", "Baggrund", "Flader",
        "Primær", "Sekundær", "Accent", "HEX-kode", "Hjørneafrunding",
        "Gem", "kopi", "Mit tema"
    ),
    "cs" to themeLibrary(
        "Výchozí", "Vybráno", "Obnovit výchozí", "Levyra se vrátí k původnímu motivu a zvýraznění. Uložené motivy zůstanou zachovány.", "Obnovit",
        "Moje motivy", "Vytvořte motiv a uložte si vlastní barvy a tvary.", "Vytvořit motiv", "Editor motivu", "Další akce",
        "Upravit", "Duplikovat", "Přejmenovat", "Smazat", "Motiv bude smazán. Akci nelze vrátit.",
        "Název", "Světlý", "Tmavý", "Pozadí", "Plochy",
        "Primární", "Sekundární", "Zvýraznění", "Kód HEX", "Zaoblení rohů",
        "Uložit", "kopie", "Můj motiv"
    ),
    "sk" to themeLibrary(
        "Predvolený", "Vybraný", "Obnoviť predvolený", "Levyra sa vráti k pôvodnému motívu a zvýrazneniu. Uložené motívy zostanú zachované.", "Obnoviť",
        "Moje motívy", "Vytvorte motív a uložte si vlastné farby a tvary.", "Vytvoriť motív", "Editor motívu", "Ďalšie akcie",
        "Upraviť", "Duplikovať", "Premenovať", "Odstrániť", "Motív bude odstránený. Akciu nemožno vrátiť.",
        "Názov", "Svetlý", "Tmavý", "Pozadie", "Plochy",
        "Primárna", "Sekundárna", "Zvýraznenie", "Kód HEX", "Zaoblenie rohov",
        "Uložiť", "kópia", "Môj motív"
    ),
    "bn" to themeLibrary(
        "ডিফল্ট", "নির্বাচিত", "ডিফল্টে ফেরান", "Levyra আসল থিম ও অ্যাকসেন্টে ফিরে যাবে। আপনার সংরক্ষিত থিমগুলো থাকবে।", "ফেরান",
        "আমার থিম", "নিজের রং ও আকার সংরক্ষণ করতে একটি থিম তৈরি করুন।", "থিম তৈরি করুন", "থিম এডিটর", "আরও কাজ",
        "সম্পাদনা", "অনুলিপি", "নাম বদলান", "মুছুন", "থিমটি মুছে যাবে। এটি ফেরানো যাবে না।",
        "নাম", "হালকা", "গাঢ়", "পটভূমি", "সারফেস",
        "প্রাথমিক", "মাধ্যমিক", "অ্যাকসেন্ট", "HEX কোড", "কোণের গোলাকৃতি",
        "সংরক্ষণ", "অনুলিপি", "আমার থিম"
    ),
    "kk" to themeLibrary(
        "Әдепкі", "Таңдалған", "Әдепкіні қалпына келтіру", "Levyra бастапқы тақырып пен акцентке оралады. Сақталған тақырыптарыңыз қалады.", "Қалпына келтіру",
        "Менің тақырыптарым", "Өз түстеріңіз бен пішіндеріңізді сақтау үшін тақырып жасаңыз.", "Тақырып жасау", "Тақырып редакторы", "Басқа әрекеттер",
        "Өңдеу", "Көшірме жасау", "Атын өзгерту", "Жою", "Тақырып жойылады. Бұл әрекетті болдырмау мүмкін емес.",
        "Атауы", "Ашық", "Күңгірт", "Фон", "Беттер",
        "Негізгі", "Қосымша", "Акцент", "HEX коды", "Бұрыштардың дөңгелектігі",
        "Сақтау", "көшірме", "Менің тақырыбым"
    ),
    "sr" to themeLibrary(
        "Подразумевано", "Изабрано", "Врати подразумевано", "Levyra се враћа на оригиналну тему и акценат. Сачуване теме остају.", "Врати",
        "Моје теме", "Направите тему да бисте сачували своје боје и облике.", "Направи тему", "Уређивач теме", "Још радњи",
        "Уреди", "Дуплирај", "Преименуј", "Избриши", "Тема ће бити избрисана. Ова радња се не може опозвати.",
        "Назив", "Светла", "Тамна", "Позадина", "Површине",
        "Примарна", "Секундарна", "Акценат", "HEX код", "Заобљеност углова",
        "Сачувај", "копија", "Моја тема"
    ),
    "sq" to themeLibrary(
        "I parazgjedhur", "I zgjedhur", "Rikthe të parazgjedhurin", "Levyra kthehet te tema dhe theksi origjinal. Temat e ruajtura mbeten.", "Rikthe",
        "Temat e mia", "Krijo një temë për të ruajtur ngjyrat dhe format e tua.", "Krijo temë", "Redaktori i temës", "Më shumë veprime",
        "Redakto", "Dyfisho", "Riemërto", "Fshi", "Tema do të fshihet. Veprimi nuk mund të zhbëhet.",
        "Emri", "E çelët", "E errët", "Sfondi", "Sipërfaqet",
        "Kryesore", "Dytësore", "Theksi", "Kodi HEX", "Rrumbullakimi i qosheve",
        "Ruaj", "kopje", "Tema ime"
    ),
    "hr" to themeLibrary(
        "Zadano", "Odabrano", "Vrati zadano", "Levyra se vraća na izvornu temu i naglasak. Spremljene teme ostaju.", "Vrati",
        "Moje teme", "Izradite temu da biste spremili vlastite boje i oblike.", "Izradi temu", "Uređivač teme", "Više radnji",
        "Uredi", "Dupliciraj", "Preimenuj", "Izbriši", "Tema će biti izbrisana. Radnja se ne može poništiti.",
        "Naziv", "Svijetla", "Tamna", "Pozadina", "Površine",
        "Primarna", "Sekundarna", "Naglasak", "HEX kod", "Zaobljenost kutova",
        "Spremi", "kopija", "Moja tema"
    ),
    "bg" to themeLibrary(
        "По подразбиране", "Избрано", "Възстановяване по подразбиране", "Levyra се връща към оригиналната тема и акцент. Запазените теми остават.", "Възстановяване",
        "Моите теми", "Създайте тема, за да запазите свои цветове и форми.", "Създаване на тема", "Редактор на тема", "Още действия",
        "Редактиране", "Дублиране", "Преименуване", "Изтриване", "Темата ще бъде изтрита. Действието не може да се отмени.",
        "Име", "Светла", "Тъмна", "Фон", "Повърхности",
        "Основен", "Вторичен", "Акцент", "HEX код", "Заобляне на ъглите",
        "Запазване", "копие", "Моята тема"
    ),
    "hu" to themeLibrary(
        "Alapértelmezett", "Kiválasztva", "Alapértelmezett visszaállítása", "A Levyra visszatér az eredeti témához és kiemelőszínhez. A mentett témáid megmaradnak.", "Visszaállítás",
        "Saját témák", "Hozz létre egy témát a saját színeid és formáid mentéséhez.", "Téma létrehozása", "Témaszerkesztő", "További műveletek",
        "Szerkesztés", "Másolat", "Átnevezés", "Törlés", "A téma törlődik. A művelet nem vonható vissza.",
        "Név", "Világos", "Sötét", "Háttér", "Felületek",
        "Elsődleges", "Másodlagos", "Kiemelés", "HEX-kód", "Sarkok kerekítése",
        "Mentés", "másolat", "Saját téma"
    ),
    "fi" to themeLibrary(
        "Oletus", "Valittu", "Palauta oletus", "Levyra palaa alkuperäiseen teemaan ja korostusväriin. Tallennetut teemasi säilyvät.", "Palauta",
        "Omat teemat", "Luo teema tallentaaksesi omat värisi ja muotosi.", "Luo teema", "Teemaeditori", "Lisää toimintoja",
        "Muokkaa", "Monista", "Nimeä uudelleen", "Poista", "Teema poistetaan. Toimintoa ei voi kumota.",
        "Nimi", "Vaalea", "Tumma", "Tausta", "Pinnat",
        "Ensisijainen", "Toissijainen", "Korostus", "HEX-koodi", "Kulmien pyöristys",
        "Tallenna", "kopio", "Oma teema"
    ),
    "et" to themeLibrary(
        "Vaikimisi", "Valitud", "Taasta vaikimisi", "Levyra naaseb algse teema ja rõhuvärvi juurde. Salvestatud teemad jäävad alles.", "Taasta",
        "Minu teemad", "Loo teema, et salvestada oma värvid ja kujud.", "Loo teema", "Teemaredaktor", "Rohkem toiminguid",
        "Muuda", "Dubleeri", "Nimeta ümber", "Kustuta", "Teema kustutatakse. Seda ei saa tagasi võtta.",
        "Nimi", "Hele", "Tume", "Taust", "Pinnad",
        "Peamine", "Teisene", "Rõhuvärv", "HEX-kood", "Nurkade ümarus",
        "Salvesta", "koopia", "Minu teema"
    ),
    "nb" to themeLibrary(
        "Standard", "Valgt", "Gjenopprett standard", "Levyra går tilbake til det opprinnelige temaet og aksentfargen. Lagrede temaer beholdes.", "Gjenopprett",
        "Mine temaer", "Lag et tema for å lagre dine egne farger og former.", "Lag tema", "Temaredigering", "Flere handlinger",
        "Rediger", "Dupliser", "Gi nytt navn", "Slett", "Temaet slettes. Dette kan ikke angres.",
        "Navn", "Lyst", "Mørkt", "Bakgrunn", "Flater",
        "Primær", "Sekundær", "Aksent", "HEX-kode", "Hjørneavrunding",
        "Lagre", "kopi", "Mitt tema"
    ),
    "ca" to themeLibrary(
        "Predeterminat", "Seleccionat", "Restaura el predeterminat", "Levyra torna al tema i l'accent originals. Els teus temes desats es conserven.", "Restaura",
        "Els meus temes", "Crea un tema per desar els teus colors i formes.", "Crea un tema", "Editor de temes", "Més accions",
        "Edita", "Duplica", "Canvia el nom", "Suprimeix", "Aquest tema se suprimirà. No es pot desfer.",
        "Nom", "Clar", "Fosc", "Fons", "Superfícies",
        "Primari", "Secundari", "Accent", "Codi HEX", "Arrodoniment de cantonades",
        "Desa", "còpia", "El meu tema"
    ),
    "uk" to themeLibrary(
        "За замовчуванням", "Вибрано", "Відновити за замовчуванням", "Levyra повернеться до оригінальної теми й акценту. Збережені теми залишаться.", "Відновити",
        "Мої теми", "Створіть тему, щоб зберегти власні кольори та форми.", "Створити тему", "Редактор теми", "Інші дії",
        "Редагувати", "Дублювати", "Перейменувати", "Видалити", "Тему буде видалено. Цю дію не можна скасувати.",
        "Назва", "Світла", "Темна", "Фон", "Поверхні",
        "Основний", "Додатковий", "Акцент", "HEX-код", "Заокруглення кутів",
        "Зберегти", "копія", "Моя тема"
    ),
    "ru" to themeLibrary(
        "По умолчанию", "Выбрано", "Восстановить по умолчанию", "Levyra вернётся к исходной теме и акценту. Сохранённые темы останутся.", "Восстановить",
        "Мои темы", "Создайте тему, чтобы сохранить свои цвета и формы.", "Создать тему", "Редактор темы", "Другие действия",
        "Изменить", "Дублировать", "Переименовать", "Удалить", "Тема будет удалена. Это действие нельзя отменить.",
        "Название", "Светлая", "Тёмная", "Фон", "Поверхности",
        "Основной", "Дополнительный", "Акцент", "HEX-код", "Скругление углов",
        "Сохранить", "копия", "Моя тема"
    ),
    "tr" to themeLibrary(
        "Varsayılan", "Seçili", "Varsayılanı geri yükle", "Levyra orijinal temasına ve vurgu rengine döner. Kaydettiğin temalar korunur.", "Geri yükle",
        "Temalarım", "Kendi renklerini ve şekillerini kaydetmek için bir tema oluştur.", "Tema oluştur", "Tema düzenleyici", "Diğer işlemler",
        "Düzenle", "Çoğalt", "Yeniden adlandır", "Sil", "Bu tema silinecek. Bu işlem geri alınamaz.",
        "Ad", "Açık", "Koyu", "Arka plan", "Yüzeyler",
        "Birincil", "İkincil", "Vurgu", "HEX kodu", "Köşe yuvarlaklığı",
        "Kaydet", "kopya", "Temam"
    ),
    "ar" to themeLibrary(
        "الافتراضي", "المحدد", "استعادة الافتراضي", "يعود Levyra إلى السمة واللون المميز الأصليين. تبقى سماتك المحفوظة.", "استعادة",
        "سماتي", "أنشئ سمة لحفظ ألوانك وأشكالك الخاصة.", "إنشاء سمة", "محرر السمة", "إجراءات أخرى",
        "تعديل", "تكرار", "إعادة تسمية", "حذف", "سيتم حذف هذه السمة. لا يمكن التراجع عن ذلك.",
        "الاسم", "فاتح", "داكن", "الخلفية", "الأسطح",
        "أساسي", "ثانوي", "مميز", "رمز HEX", "استدارة الزوايا",
        "حفظ", "نسخة", "سمتي"
    ),
    "fa" to themeLibrary(
        "پیش‌فرض", "انتخاب‌شده", "بازگرداندن پیش‌فرض", "Levyra به پوسته و رنگ تأکیدی اصلی برمی‌گردد. پوسته‌های ذخیره‌شده شما حفظ می‌شوند.", "بازگرداندن",
        "پوسته‌های من", "برای ذخیره رنگ‌ها و شکل‌های خودتان یک پوسته بسازید.", "ساخت پوسته", "ویرایشگر پوسته", "کارهای بیشتر",
        "ویرایش", "تکثیر", "تغییر نام", "حذف", "این پوسته حذف می‌شود. این کار برگشت‌پذیر نیست.",
        "نام", "روشن", "تیره", "پس‌زمینه", "سطح‌ها",
        "اصلی", "ثانویه", "تأکیدی", "کد HEX", "گردی گوشه‌ها",
        "ذخیره", "کپی", "پوسته من"
    ),
    "zh" to themeLibrary(
        "默认", "已选择", "恢复默认", "Levyra 将恢复为原始主题和强调色。已保存的主题会保留。", "恢复",
        "我的主题", "创建主题来保存你自己的颜色和形状。", "创建主题", "主题编辑器", "更多操作",
        "编辑", "复制", "重命名", "删除", "此主题将被删除，且无法撤销。",
        "名称", "浅色", "深色", "背景", "表面",
        "主色", "辅色", "强调色", "HEX 代码", "圆角程度",
        "保存", "副本", "我的主题"
    ),
    "zh-Hant" to themeLibrary(
        "預設", "已選取", "還原預設", "Levyra 將還原為原始主題與強調色。已儲存的主題會保留。", "還原",
        "我的主題", "建立主題來儲存你自己的顏色與形狀。", "建立主題", "主題編輯器", "更多動作",
        "編輯", "複製", "重新命名", "刪除", "此主題將被刪除，且無法復原。",
        "名稱", "淺色", "深色", "背景", "表面",
        "主色", "輔色", "強調色", "HEX 代碼", "圓角程度",
        "儲存", "副本", "我的主題"
    ),
    "ja" to themeLibrary(
        "デフォルト", "選択中", "デフォルトに戻す", "Levyra は元のテーマとアクセントに戻ります。保存したテーマは残ります。", "戻す",
        "マイテーマ", "テーマを作成して、自分だけの色と形を保存しましょう。", "テーマを作成", "テーマエディタ", "その他の操作",
        "編集", "複製", "名前を変更", "削除", "このテーマは削除されます。元に戻せません。",
        "名前", "ライト", "ダーク", "背景", "サーフェス",
        "プライマリ", "セカンダリ", "アクセント", "HEX コード", "角の丸み",
        "保存", "コピー", "マイテーマ"
    ),
    "ko" to themeLibrary(
        "기본값", "선택됨", "기본값 복원", "Levyra가 원래 테마와 강조 색상으로 돌아갑니다. 저장한 테마는 유지됩니다.", "복원",
        "내 테마", "테마를 만들어 나만의 색상과 모양을 저장하세요.", "테마 만들기", "테마 편집기", "추가 작업",
        "편집", "복제", "이름 바꾸기", "삭제", "이 테마가 삭제됩니다. 되돌릴 수 없습니다.",
        "이름", "라이트", "다크", "배경", "표면",
        "기본", "보조", "강조", "HEX 코드", "모서리 둥글기",
        "저장", "사본", "내 테마"
    ),
    "hi" to themeLibrary(
        "डिफ़ॉल्ट", "चुना गया", "डिफ़ॉल्ट पुनर्स्थापित करें", "Levyra अपनी मूल थीम और एक्सेंट पर लौट आएगा। आपकी सहेजी गई थीम बनी रहेंगी।", "पुनर्स्थापित करें",
        "मेरी थीम", "अपने रंग और आकार सहेजने के लिए एक थीम बनाएँ।", "थीम बनाएँ", "थीम एडिटर", "और कार्य",
        "संपादित करें", "डुप्लिकेट", "नाम बदलें", "हटाएँ", "यह थीम हटा दी जाएगी। इसे पूर्ववत नहीं किया जा सकता।",
        "नाम", "हल्की", "गहरी", "पृष्ठभूमि", "सतहें",
        "प्राथमिक", "द्वितीयक", "एक्सेंट", "HEX कोड", "कोनों की गोलाई",
        "सहेजें", "प्रति", "मेरी थीम"
    ),
    "id" to themeLibrary(
        "Default", "Dipilih", "Pulihkan default", "Levyra kembali ke tema dan aksen aslinya. Tema yang kamu simpan tetap ada.", "Pulihkan",
        "Tema saya", "Buat tema untuk menyimpan warna dan bentuk milikmu sendiri.", "Buat tema", "Editor tema", "Tindakan lainnya",
        "Edit", "Duplikat", "Ganti nama", "Hapus", "Tema ini akan dihapus. Tindakan ini tidak bisa dibatalkan.",
        "Nama", "Terang", "Gelap", "Latar belakang", "Permukaan",
        "Utama", "Sekunder", "Aksen", "Kode HEX", "Kelengkungan sudut",
        "Simpan", "salinan", "Tema saya"
    ),
    "ms" to themeLibrary(
        "Lalai", "Dipilih", "Pulihkan lalai", "Levyra kembali kepada tema dan aksen asal. Tema yang anda simpan dikekalkan.", "Pulihkan",
        "Tema saya", "Cipta tema untuk menyimpan warna dan bentuk anda sendiri.", "Cipta tema", "Editor tema", "Lagi tindakan",
        "Edit", "Pendua", "Namakan semula", "Padam", "Tema ini akan dipadam. Tindakan ini tidak boleh dibuat asal.",
        "Nama", "Cerah", "Gelap", "Latar belakang", "Permukaan",
        "Utama", "Sekunder", "Aksen", "Kod HEX", "Kebulatan sudut",
        "Simpan", "salinan", "Tema saya"
    ),
    "vi" to themeLibrary(
        "Mặc định", "Đã chọn", "Khôi phục mặc định", "Levyra trở về chủ đề và màu nhấn ban đầu. Các chủ đề bạn đã lưu vẫn được giữ.", "Khôi phục",
        "Chủ đề của tôi", "Tạo chủ đề để lưu màu sắc và hình dạng của riêng bạn.", "Tạo chủ đề", "Trình chỉnh sửa chủ đề", "Thao tác khác",
        "Chỉnh sửa", "Nhân bản", "Đổi tên", "Xóa", "Chủ đề này sẽ bị xóa và không thể hoàn tác.",
        "Tên", "Sáng", "Tối", "Nền", "Bề mặt",
        "Chính", "Phụ", "Nhấn", "Mã HEX", "Độ bo góc",
        "Lưu", "bản sao", "Chủ đề của tôi"
    ),
    "th" to themeLibrary(
        "ค่าเริ่มต้น", "เลือกอยู่", "คืนค่าเริ่มต้น", "Levyra จะกลับไปใช้ธีมและสีเน้นดั้งเดิม ธีมที่คุณบันทึกไว้จะยังอยู่", "คืนค่า",
        "ธีมของฉัน", "สร้างธีมเพื่อบันทึกสีและรูปทรงของคุณเอง", "สร้างธีม", "ตัวแก้ไขธีม", "การทำงานเพิ่มเติม",
        "แก้ไข", "ทำสำเนา", "เปลี่ยนชื่อ", "ลบ", "ธีมนี้จะถูกลบและไม่สามารถเรียกคืนได้",
        "ชื่อ", "สว่าง", "มืด", "พื้นหลัง", "พื้นผิว",
        "สีหลัก", "สีรอง", "สีเน้น", "รหัส HEX", "ความโค้งมุม",
        "บันทึก", "สำเนา", "ธีมของฉัน"
    ),
    "fil" to themeLibrary(
        "Default", "Napili", "Ibalik ang default", "Babalik ang Levyra sa orihinal na tema at accent. Mananatili ang mga na-save mong tema.", "Ibalik",
        "Aking mga tema", "Gumawa ng tema para i-save ang sarili mong mga kulay at hugis.", "Gumawa ng tema", "Editor ng tema", "Iba pang aksyon",
        "I-edit", "I-duplicate", "Palitan ang pangalan", "Burahin", "Mabubura ang temang ito. Hindi na ito maibabalik.",
        "Pangalan", "Maliwanag", "Madilim", "Background", "Mga surface",
        "Pangunahin", "Pangalawa", "Accent", "HEX code", "Kurba ng sulok",
        "I-save", "kopya", "Aking tema"
    ),
    "he" to themeLibrary(
        "ברירת מחדל", "נבחר", "שחזור ברירת המחדל", "Levyra תחזור לערכת הנושא ולצבע ההדגשה המקוריים. ערכות הנושא ששמרת יישמרו.", "שחזור",
        "ערכות הנושא שלי", "צור ערכת נושא כדי לשמור צבעים וצורות משלך.", "יצירת ערכת נושא", "עורך ערכת נושא", "פעולות נוספות",
        "עריכה", "שכפול", "שינוי שם", "מחיקה", "ערכת הנושא תימחק. לא ניתן לבטל פעולה זו.",
        "שם", "בהירה", "כהה", "רקע", "משטחים",
        "ראשי", "משני", "הדגשה", "קוד HEX", "עיגול פינות",
        "שמירה", "עותק", "ערכת הנושא שלי"
    )
)

private val themeLibraryCopyObjects: Map<String, ThemeLibraryCopy> by lazy {
    themeLibraryCopies.mapValues { (_, texts) -> ThemeLibraryCopy(texts) }
}

internal fun themeLibraryLocalizationCodes(): Set<String> = themeLibraryCopies.keys

internal fun themeLibraryCompleteLocalizationCodes(): Set<String> =
    themeLibraryCopies.filterValues { it.size == ThemeLibraryText.entries.size }.keys

internal val LevyraStrings.themeLibraryCopy: ThemeLibraryCopy
    get() = localizedValueOrEnglish(themeLibraryCopyObjects, code).forBrazil(code) { brazilian }
