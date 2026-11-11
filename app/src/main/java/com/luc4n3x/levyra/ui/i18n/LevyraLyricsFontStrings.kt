package com.luc4n3x.levyra.ui.i18n

internal val lyricsFontKeys: Set<String> = linkedSetOf(
    "lyricsFontsTitle",
    "lyricsFontsHint",
    "lyricsFontsImport",
    "lyricsFontsImporting",
    "lyricsFontsImported",
    "lyricsFontsReplaced",
    "lyricsFontsInvalid",
    "lyricsFontsTooLarge",
    "lyricsFontsLibraryFull",
    "lyricsFontsFailed",
    "lyricsFontsUnavailable",
    "lyricsFontsVariable",
    "lyricsFontsRename",
    "lyricsFontsReplace",
    "lyricsFontsDeleteTitle",
    "lyricsFontsDeleteBody",
    "lyricsFontsName",
    "lyricsFontsOptions",
    "lyricsStudioSizeHuge"
)

private fun lyricsFonts(vararg values: String): Map<String, String> {
    require(values.size == lyricsFontKeys.size)
    return lyricsFontKeys.zip(values).toMap()
}

private val lyricsFontBundles: Map<String, Map<String, String>> = mapOf(
    "en" to lyricsFonts(
        "My fonts", "Import TTF or OTF fonts from your phone (max 128 MB)", "Import font", "Importing…",
        "Font imported", "Font replaced", "This file isn't a valid TTF or OTF font", "This font is larger than 128 MB",
        "Font library is full. Delete a font to import another", "Couldn't read the font file",
        "The selected font is unavailable. Using the built-in font", "Variable", "Rename", "Replace file",
        "Delete font?", "Lyrics using this font will switch back to the built-in font.", "Font name", "Font options", "Huge"
    ),
    "it" to lyricsFonts(
        "I miei font", "Importa font TTF o OTF dal telefono (max 128 MB)", "Importa font", "Importazione…",
        "Font importato", "Font sostituito", "Il file non è un font TTF o OTF valido", "Il font supera i 128 MB",
        "Libreria font piena. Elimina un font per importarne un altro", "Impossibile leggere il file del font",
        "Il font selezionato non è disponibile. Uso il font integrato", "Variabile", "Rinomina", "Sostituisci file",
        "Eliminare il font?", "I testi che usano questo font torneranno al font integrato.", "Nome del font", "Opzioni font", "Enorme"
    ),
    "es" to lyricsFonts(
        "Mis fuentes", "Importa fuentes TTF u OTF desde tu teléfono (máx. 128 MB)", "Importar fuente", "Importando…",
        "Fuente importada", "Fuente reemplazada", "Este archivo no es una fuente TTF u OTF válida", "La fuente supera los 128 MB",
        "La biblioteca de fuentes está llena. Elimina una para importar otra", "No se pudo leer el archivo de la fuente",
        "La fuente seleccionada no está disponible. Se usa la fuente integrada", "Variable", "Cambiar nombre", "Reemplazar archivo",
        "¿Eliminar la fuente?", "Las letras con esta fuente volverán a la fuente integrada.", "Nombre de la fuente", "Opciones de fuente", "Enorme"
    ),
    "fr" to lyricsFonts(
        "Mes polices", "Importez des polices TTF ou OTF depuis votre téléphone (128 Mo max)", "Importer une police", "Importation…",
        "Police importée", "Police remplacée", "Ce fichier n'est pas une police TTF ou OTF valide", "Cette police dépasse 128 Mo",
        "Bibliothèque de polices pleine. Supprimez-en une pour en importer une autre", "Impossible de lire le fichier de police",
        "La police choisie est indisponible. Police intégrée utilisée", "Variable", "Renommer", "Remplacer le fichier",
        "Supprimer la police ?", "Les paroles utilisant cette police reviendront à la police intégrée.", "Nom de la police", "Options de police", "Énorme"
    ),
    "de" to lyricsFonts(
        "Meine Schriften", "TTF- oder OTF-Schriften vom Handy importieren (max. 128 MB)", "Schrift importieren", "Wird importiert…",
        "Schrift importiert", "Schrift ersetzt", "Diese Datei ist keine gültige TTF- oder OTF-Schrift", "Diese Schrift ist größer als 128 MB",
        "Schriftbibliothek voll. Lösche eine Schrift, um eine neue zu importieren", "Schriftdatei konnte nicht gelesen werden",
        "Die gewählte Schrift ist nicht verfügbar. Die integrierte Schrift wird verwendet", "Variabel", "Umbenennen", "Datei ersetzen",
        "Schrift löschen?", "Liedtexte mit dieser Schrift wechseln zur integrierten Schrift.", "Name der Schrift", "Schriftoptionen", "Riesig"
    ),
    "pt" to lyricsFonts(
        "Minhas fontes", "Importe fontes TTF ou OTF do telemóvel (máx. 128 MB)", "Importar fonte", "A importar…",
        "Fonte importada", "Fonte substituída", "Este ficheiro não é uma fonte TTF ou OTF válida", "A fonte tem mais de 128 MB",
        "A biblioteca de fontes está cheia. Elimine uma para importar outra", "Não foi possível ler o ficheiro da fonte",
        "A fonte selecionada não está disponível. A usar a fonte integrada", "Variável", "Mudar o nome", "Substituir ficheiro",
        "Eliminar a fonte?", "As letras com esta fonte voltam à fonte integrada.", "Nome da fonte", "Opções da fonte", "Enorme"
    ),
    "nl" to lyricsFonts(
        "Mijn lettertypen", "Importeer TTF- of OTF-lettertypen van je telefoon (max. 128 MB)", "Lettertype importeren", "Importeren…",
        "Lettertype geïmporteerd", "Lettertype vervangen", "Dit bestand is geen geldig TTF- of OTF-lettertype", "Dit lettertype is groter dan 128 MB",
        "Lettertypebibliotheek vol. Verwijder er een om een ander te importeren", "Lettertypebestand kon niet worden gelezen",
        "Het gekozen lettertype is niet beschikbaar. Ingebouwd lettertype wordt gebruikt", "Variabel", "Hernoemen", "Bestand vervangen",
        "Lettertype verwijderen?", "Songteksten met dit lettertype gaan terug naar het ingebouwde lettertype.", "Naam lettertype", "Lettertype-opties", "Enorm"
    ),
    "pl" to lyricsFonts(
        "Moje czcionki", "Importuj czcionki TTF lub OTF z telefonu (maks. 128 MB)", "Importuj czcionkę", "Importowanie…",
        "Czcionka zaimportowana", "Czcionka zastąpiona", "Ten plik nie jest prawidłową czcionką TTF ani OTF", "Czcionka jest większa niż 128 MB",
        "Biblioteka czcionek jest pełna. Usuń jedną, aby zaimportować kolejną", "Nie można odczytać pliku czcionki",
        "Wybrana czcionka jest niedostępna. Używana jest czcionka wbudowana", "Zmienna", "Zmień nazwę", "Zastąp plik",
        "Usunąć czcionkę?", "Teksty z tą czcionką wrócą do czcionki wbudowanej.", "Nazwa czcionki", "Opcje czcionki", "Ogromny"
    ),
    "ro" to lyricsFonts(
        "Fonturile mele", "Importă fonturi TTF sau OTF din telefon (max. 128 MB)", "Importă font", "Se importă…",
        "Font importat", "Font înlocuit", "Fișierul nu este un font TTF sau OTF valid", "Fontul depășește 128 MB",
        "Biblioteca de fonturi este plină. Șterge un font pentru a importa altul", "Fișierul fontului nu a putut fi citit",
        "Fontul selectat nu este disponibil. Se folosește fontul integrat", "Variabil", "Redenumește", "Înlocuiește fișierul",
        "Ștergi fontul?", "Versurile cu acest font vor reveni la fontul integrat.", "Numele fontului", "Opțiuni font", "Uriaș"
    ),
    "el" to lyricsFonts(
        "Οι γραμματοσειρές μου", "Εισαγωγή γραμματοσειρών TTF ή OTF από το τηλέφωνο (έως 128 MB)", "Εισαγωγή γραμματοσειράς", "Εισαγωγή…",
        "Η γραμματοσειρά εισήχθη", "Η γραμματοσειρά αντικαταστάθηκε", "Το αρχείο δεν είναι έγκυρη γραμματοσειρά TTF ή OTF", "Η γραμματοσειρά ξεπερνά τα 128 MB",
        "Η βιβλιοθήκη είναι πλήρης. Διαγράψτε μια γραμματοσειρά για να εισαγάγετε άλλη", "Δεν ήταν δυνατή η ανάγνωση του αρχείου",
        "Η επιλεγμένη γραμματοσειρά δεν είναι διαθέσιμη. Χρησιμοποιείται η ενσωματωμένη", "Μεταβλητή", "Μετονομασία", "Αντικατάσταση αρχείου",
        "Διαγραφή γραμματοσειράς;", "Οι στίχοι με αυτή τη γραμματοσειρά θα επιστρέψουν στην ενσωματωμένη.", "Όνομα γραμματοσειράς", "Επιλογές γραμματοσειράς", "Τεράστιο"
    ),
    "sv" to lyricsFonts(
        "Mina typsnitt", "Importera TTF- eller OTF-typsnitt från telefonen (max 128 MB)", "Importera typsnitt", "Importerar…",
        "Typsnittet importerades", "Typsnittet ersattes", "Filen är inte ett giltigt TTF- eller OTF-typsnitt", "Typsnittet är större än 128 MB",
        "Typsnittsbiblioteket är fullt. Ta bort ett för att importera ett nytt", "Det gick inte att läsa typsnittsfilen",
        "Det valda typsnittet är inte tillgängligt. Det inbyggda används", "Variabelt", "Byt namn", "Ersätt fil",
        "Ta bort typsnittet?", "Låttexter med detta typsnitt går tillbaka till det inbyggda.", "Typsnittets namn", "Typsnittsalternativ", "Enorm"
    ),
    "da" to lyricsFonts(
        "Mine skrifttyper", "Importér TTF- eller OTF-skrifttyper fra telefonen (maks. 128 MB)", "Importér skrifttype", "Importerer…",
        "Skrifttype importeret", "Skrifttype erstattet", "Filen er ikke en gyldig TTF- eller OTF-skrifttype", "Skrifttypen er større end 128 MB",
        "Skrifttypebiblioteket er fuldt. Slet en for at importere en ny", "Skrifttypefilen kunne ikke læses",
        "Den valgte skrifttype er ikke tilgængelig. Den indbyggede bruges", "Variabel", "Omdøb", "Erstat fil",
        "Slet skrifttype?", "Sangtekster med denne skrifttype skifter til den indbyggede.", "Skrifttypens navn", "Skrifttypeindstillinger", "Kæmpe"
    ),
    "cs" to lyricsFonts(
        "Moje písma", "Importujte písma TTF nebo OTF z telefonu (max. 128 MB)", "Importovat písmo", "Importování…",
        "Písmo importováno", "Písmo nahrazeno", "Soubor není platné písmo TTF ani OTF", "Písmo je větší než 128 MB",
        "Knihovna písem je plná. Smažte písmo a importujte další", "Soubor písma nelze přečíst",
        "Vybrané písmo není dostupné. Použije se vestavěné písmo", "Variabilní", "Přejmenovat", "Nahradit soubor",
        "Smazat písmo?", "Texty s tímto písmem se vrátí k vestavěnému písmu.", "Název písma", "Možnosti písma", "Obrovská"
    ),
    "sk" to lyricsFonts(
        "Moje písma", "Importujte písma TTF alebo OTF z telefónu (max. 128 MB)", "Importovať písmo", "Importuje sa…",
        "Písmo importované", "Písmo nahradené", "Súbor nie je platné písmo TTF ani OTF", "Písmo je väčšie ako 128 MB",
        "Knižnica písiem je plná. Odstráňte písmo a importujte ďalšie", "Súbor písma sa nedá prečítať",
        "Vybrané písmo nie je dostupné. Použije sa vstavané písmo", "Variabilné", "Premenovať", "Nahradiť súbor",
        "Odstrániť písmo?", "Texty s týmto písmom sa vrátia k vstavanému písmu.", "Názov písma", "Možnosti písma", "Obrovská"
    ),
    "hr" to lyricsFonts(
        "Moji fontovi", "Uvezite TTF ili OTF fontove s telefona (najviše 128 MB)", "Uvezi font", "Uvoz…",
        "Font je uvezen", "Font je zamijenjen", "Datoteka nije valjan TTF ili OTF font", "Font je veći od 128 MB",
        "Biblioteka fontova je puna. Izbrišite font da biste uvezli drugi", "Datoteku fonta nije moguće pročitati",
        "Odabrani font nije dostupan. Koristi se ugrađeni font", "Varijabilni", "Preimenuj", "Zamijeni datoteku",
        "Izbrisati font?", "Stihovi s ovim fontom vratit će se na ugrađeni font.", "Naziv fonta", "Opcije fonta", "Golem"
    ),
    "sr" to lyricsFonts(
        "Moji fontovi", "Uvezite TTF ili OTF fontove sa telefona (najviše 128 MB)", "Uvezi font", "Uvoz…",
        "Font je uvezen", "Font je zamenjen", "Datoteka nije ispravan TTF ili OTF font", "Font je veći od 128 MB",
        "Biblioteka fontova je puna. Obrišite font da biste uvezli drugi", "Datoteku fonta nije moguće pročitati",
        "Izabrani font nije dostupan. Koristi se ugrađeni font", "Promenljivi", "Preimenuj", "Zameni datoteku",
        "Obrisati font?", "Stihovi sa ovim fontom vratiće se na ugrađeni font.", "Naziv fonta", "Opcije fonta", "Ogroman"
    ),
    "sq" to lyricsFonts(
        "Fontet e mia", "Importo fonte TTF ose OTF nga telefoni (maks. 128 MB)", "Importo font", "Po importohet…",
        "Fonti u importua", "Fonti u zëvendësua", "Skedari nuk është font i vlefshëm TTF ose OTF", "Fonti është më i madh se 128 MB",
        "Biblioteka e fonteve është plot. Fshi një font për të importuar një tjetër", "Skedari i fontit nuk u lexua dot",
        "Fonti i zgjedhur nuk ofrohet. Përdoret fonti i integruar", "I ndryshueshëm", "Riemërto", "Zëvendëso skedarin",
        "Të fshihet fonti?", "Tekstet me këtë font do të kthehen te fonti i integruar.", "Emri i fontit", "Opsionet e fontit", "Shumë i madh"
    ),
    "bg" to lyricsFonts(
        "Моите шрифтове", "Импортирайте шрифтове TTF или OTF от телефона (до 128 MB)", "Импортиране на шрифт", "Импортиране…",
        "Шрифтът е импортиран", "Шрифтът е заменен", "Файлът не е валиден шрифт TTF или OTF", "Шрифтът е по-голям от 128 MB",
        "Библиотеката е пълна. Изтрийте шрифт, за да импортирате друг", "Файлът на шрифта не може да бъде прочетен",
        "Избраният шрифт не е наличен. Използва се вграденият", "Променлив", "Преименуване", "Замяна на файла",
        "Изтриване на шрифта?", "Текстовете с този шрифт ще се върнат към вградения.", "Име на шрифта", "Опции за шрифта", "Огромен"
    ),
    "hu" to lyricsFonts(
        "Saját betűtípusok", "TTF vagy OTF betűtípusok importálása a telefonról (max. 128 MB)", "Betűtípus importálása", "Importálás…",
        "Betűtípus importálva", "Betűtípus lecserélve", "A fájl nem érvényes TTF vagy OTF betűtípus", "A betűtípus nagyobb 128 MB-nál",
        "A betűtípustár megtelt. Törölj egyet, hogy újat importálhass", "A betűtípusfájl nem olvasható",
        "A kiválasztott betűtípus nem érhető el. A beépített betűtípus látható", "Változó", "Átnevezés", "Fájl cseréje",
        "Törlöd a betűtípust?", "Az ezt használó dalszövegek visszaállnak a beépített betűtípusra.", "Betűtípus neve", "Betűtípus beállításai", "Óriási"
    ),
    "fi" to lyricsFonts(
        "Omat fontit", "Tuo TTF- tai OTF-fontteja puhelimesta (enintään 128 Mt)", "Tuo fontti", "Tuodaan…",
        "Fontti tuotu", "Fontti korvattu", "Tiedosto ei ole kelvollinen TTF- tai OTF-fontti", "Fontti on yli 128 Mt",
        "Fonttikirjasto on täynnä. Poista fontti tuodaksesi uuden", "Fonttitiedostoa ei voitu lukea",
        "Valittu fontti ei ole saatavilla. Käytetään sisäänrakennettua fonttia", "Muuttuva", "Nimeä uudelleen", "Korvaa tiedosto",
        "Poistetaanko fontti?", "Tätä fonttia käyttävät sanoitukset palaavat sisäänrakennettuun fonttiin.", "Fontin nimi", "Fonttiasetukset", "Valtava"
    ),
    "et" to lyricsFonts(
        "Minu fondid", "Impordi telefonist TTF- või OTF-fonte (kuni 128 MB)", "Impordi font", "Importimine…",
        "Font imporditi", "Font asendati", "Fail ei ole kehtiv TTF- ega OTF-font", "Font on suurem kui 128 MB",
        "Fondikogu on täis. Kustuta font, et uus importida", "Fondifaili ei saanud lugeda",
        "Valitud font pole saadaval. Kasutatakse sisseehitatud fonti", "Muutuv", "Nimeta ümber", "Asenda fail",
        "Kas kustutada font?", "Seda fonti kasutavad laulusõnad lähevad tagasi sisseehitatud fondile.", "Fondi nimi", "Fondi valikud", "Hiiglaslik"
    ),
    "nb" to lyricsFonts(
        "Mine skrifttyper", "Importer TTF- eller OTF-skrifttyper fra telefonen (maks 128 MB)", "Importer skrifttype", "Importerer…",
        "Skrifttype importert", "Skrifttype erstattet", "Filen er ikke en gyldig TTF- eller OTF-skrifttype", "Skrifttypen er større enn 128 MB",
        "Skrifttypebiblioteket er fullt. Slett en for å importere en ny", "Kunne ikke lese skrifttypefilen",
        "Den valgte skrifttypen er utilgjengelig. Den innebygde brukes", "Variabel", "Gi nytt navn", "Erstatt fil",
        "Slette skrifttypen?", "Sangtekster med denne skrifttypen går tilbake til den innebygde.", "Navn på skrifttype", "Skrifttypevalg", "Enorm"
    ),
    "ca" to lyricsFonts(
        "Les meves fonts", "Importa fonts TTF o OTF del telèfon (màx. 128 MB)", "Importa una font", "S'està important…",
        "Font importada", "Font substituïda", "Aquest fitxer no és una font TTF o OTF vàlida", "La font supera els 128 MB",
        "La biblioteca de fonts és plena. Suprimeix-ne una per importar-ne una altra", "No s'ha pogut llegir el fitxer de la font",
        "La font seleccionada no està disponible. S'utilitza la font integrada", "Variable", "Canvia el nom", "Substitueix el fitxer",
        "Vols suprimir la font?", "Les lletres amb aquesta font tornaran a la font integrada.", "Nom de la font", "Opcions de la font", "Enorme"
    ),
    "uk" to lyricsFonts(
        "Мої шрифти", "Імпортуйте шрифти TTF або OTF з телефона (до 128 МБ)", "Імпортувати шрифт", "Імпорт…",
        "Шрифт імпортовано", "Шрифт замінено", "Файл не є дійсним шрифтом TTF або OTF", "Шрифт більший за 128 МБ",
        "Бібліотека шрифтів заповнена. Видаліть шрифт, щоб імпортувати інший", "Не вдалося прочитати файл шрифту",
        "Вибраний шрифт недоступний. Використовується вбудований", "Змінний", "Перейменувати", "Замінити файл",
        "Видалити шрифт?", "Тексти з цим шрифтом повернуться до вбудованого шрифту.", "Назва шрифту", "Параметри шрифту", "Величезний"
    ),
    "ru" to lyricsFonts(
        "Мои шрифты", "Импортируйте шрифты TTF или OTF с телефона (до 128 МБ)", "Импортировать шрифт", "Импорт…",
        "Шрифт импортирован", "Шрифт заменён", "Файл не является корректным шрифтом TTF или OTF", "Шрифт больше 128 МБ",
        "Библиотека шрифтов заполнена. Удалите шрифт, чтобы импортировать другой", "Не удалось прочитать файл шрифта",
        "Выбранный шрифт недоступен. Используется встроенный", "Вариативный", "Переименовать", "Заменить файл",
        "Удалить шрифт?", "Тексты с этим шрифтом вернутся к встроенному шрифту.", "Название шрифта", "Параметры шрифта", "Огромный"
    ),
    "kk" to lyricsFonts(
        "Менің қаріптерім", "Телефоннан TTF не OTF қаріптерін импорттаңыз (128 МБ дейін)", "Қаріп импорттау", "Импортталуда…",
        "Қаріп импортталды", "Қаріп ауыстырылды", "Файл жарамды TTF не OTF қаріпі емес", "Қаріп 128 МБ-тан үлкен",
        "Қаріптер кітапханасы толы. Басқасын импорттау үшін біреуін жойыңыз", "Қаріп файлын оқу мүмкін болмады",
        "Таңдалған қаріп қолжетімсіз. Кірістірілген қаріп қолданылады", "Айнымалы", "Атын өзгерту", "Файлды ауыстыру",
        "Қаріпті жою керек пе?", "Осы қаріппен көрсетілген мәтіндер кірістірілген қаріпке оралады.", "Қаріп атауы", "Қаріп параметрлері", "Өте үлкен"
    ),
    "tr" to lyricsFonts(
        "Yazı tiplerim", "Telefonundan TTF veya OTF yazı tipi içe aktar (en fazla 128 MB)", "Yazı tipi içe aktar", "İçe aktarılıyor…",
        "Yazı tipi içe aktarıldı", "Yazı tipi değiştirildi", "Bu dosya geçerli bir TTF veya OTF yazı tipi değil", "Yazı tipi 128 MB'den büyük",
        "Yazı tipi kitaplığı dolu. Yenisini eklemek için birini silin", "Yazı tipi dosyası okunamadı",
        "Seçilen yazı tipi kullanılamıyor. Yerleşik yazı tipi kullanılıyor", "Değişken", "Yeniden adlandır", "Dosyayı değiştir",
        "Yazı tipi silinsin mi?", "Bu yazı tipini kullanan sözler yerleşik yazı tipine döner.", "Yazı tipi adı", "Yazı tipi seçenekleri", "Dev"
    ),
    "ar" to lyricsFonts(
        "خطوطي", "استورد خطوط TTF أو OTF من هاتفك (حتى 128 ميغابايت)", "استيراد خط", "جارٍ الاستيراد…",
        "تم استيراد الخط", "تم استبدال الخط", "هذا الملف ليس خط TTF أو OTF صالحاً", "حجم الخط أكبر من 128 ميغابايت",
        "مكتبة الخطوط ممتلئة. احذف خطاً لاستيراد آخر", "تعذّرت قراءة ملف الخط",
        "الخط المحدد غير متاح. يُستخدم الخط المدمج", "متغيّر", "إعادة التسمية", "استبدال الملف",
        "حذف الخط؟", "ستعود الكلمات التي تستخدم هذا الخط إلى الخط المدمج.", "اسم الخط", "خيارات الخط", "ضخم"
    ),
    "fa" to lyricsFonts(
        "قلم‌های من", "قلم‌های TTF یا OTF را از تلفن وارد کنید (حداکثر ۲۰ مگابایت)", "وارد کردن قلم", "در حال وارد کردن…",
        "قلم وارد شد", "قلم جایگزین شد", "این فایل قلم TTF یا OTF معتبری نیست", "حجم قلم بیش از ۲۰ مگابایت است",
        "کتابخانه قلم پر است. برای افزودن قلم جدید یکی را حذف کنید", "خواندن فایل قلم ممکن نشد",
        "قلم انتخاب‌شده در دسترس نیست. از قلم داخلی استفاده می‌شود", "متغیر", "تغییر نام", "جایگزینی فایل",
        "قلم حذف شود؟", "متن‌هایی که از این قلم استفاده می‌کنند به قلم داخلی بازمی‌گردند.", "نام قلم", "گزینه‌های قلم", "بسیار بزرگ"
    ),
    "zh" to lyricsFonts(
        "我的字体", "从手机导入 TTF 或 OTF 字体（最大 128 MB）", "导入字体", "正在导入…",
        "字体已导入", "字体已替换", "此文件不是有效的 TTF 或 OTF 字体", "字体超过 128 MB",
        "字体库已满，请删除一个字体后再导入", "无法读取字体文件",
        "所选字体不可用，正在使用内置字体", "可变", "重命名", "替换文件",
        "删除字体？", "使用此字体的歌词将恢复为内置字体。", "字体名称", "字体选项", "超大"
    ),
    "zh-Hant" to lyricsFonts(
        "我的字型", "從手機匯入 TTF 或 OTF 字型（最大 128 MB）", "匯入字型", "正在匯入…",
        "字型已匯入", "字型已取代", "此檔案不是有效的 TTF 或 OTF 字型", "字型超過 128 MB",
        "字型庫已滿，請刪除一個字型後再匯入", "無法讀取字型檔案",
        "所選字型無法使用，正在使用內建字型", "可變", "重新命名", "取代檔案",
        "刪除字型？", "使用此字型的歌詞將恢復為內建字型。", "字型名稱", "字型選項", "超大"
    ),
    "ja" to lyricsFonts(
        "マイフォント", "スマートフォンから TTF / OTF フォントを読み込みます（最大 128 MB）", "フォントを読み込む", "読み込み中…",
        "フォントを読み込みました", "フォントを置き換えました", "有効な TTF / OTF フォントではありません", "フォントが 128 MB を超えています",
        "フォントライブラリがいっぱいです。削除してから読み込んでください", "フォントファイルを読み取れませんでした",
        "選択したフォントは使用できません。内蔵フォントを使用しています", "可変", "名前を変更", "ファイルを置き換え",
        "フォントを削除しますか？", "このフォントを使う歌詞は内蔵フォントに戻ります。", "フォント名", "フォントのオプション", "超特大"
    ),
    "ko" to lyricsFonts(
        "내 글꼴", "휴대폰에서 TTF 또는 OTF 글꼴 가져오기(최대 128MB)", "글꼴 가져오기", "가져오는 중…",
        "글꼴을 가져왔습니다", "글꼴을 교체했습니다", "유효한 TTF 또는 OTF 글꼴이 아닙니다", "글꼴이 128MB를 초과합니다",
        "글꼴 보관함이 가득 찼습니다. 글꼴을 삭제한 후 가져오세요", "글꼴 파일을 읽을 수 없습니다",
        "선택한 글꼴을 사용할 수 없어 기본 글꼴을 사용합니다", "가변", "이름 바꾸기", "파일 교체",
        "글꼴을 삭제할까요?", "이 글꼴을 쓰는 가사는 기본 글꼴로 돌아갑니다.", "글꼴 이름", "글꼴 옵션", "아주 크게"
    ),
    "hi" to lyricsFonts(
        "मेरे फ़ॉन्ट", "फ़ोन से TTF या OTF फ़ॉन्ट आयात करें (अधिकतम 128 MB)", "फ़ॉन्ट आयात करें", "आयात हो रहा है…",
        "फ़ॉन्ट आयात हुआ", "फ़ॉन्ट बदला गया", "यह फ़ाइल मान्य TTF या OTF फ़ॉन्ट नहीं है", "फ़ॉन्ट 128 MB से बड़ा है",
        "फ़ॉन्ट लाइब्रेरी भर गई है। नया आयात करने के लिए एक हटाएँ", "फ़ॉन्ट फ़ाइल पढ़ी नहीं जा सकी",
        "चुना गया फ़ॉन्ट उपलब्ध नहीं है। अंतर्निहित फ़ॉन्ट उपयोग हो रहा है", "वेरिएबल", "नाम बदलें", "फ़ाइल बदलें",
        "फ़ॉन्ट हटाएँ?", "इस फ़ॉन्ट वाले बोल अंतर्निहित फ़ॉन्ट पर लौट जाएँगे।", "फ़ॉन्ट का नाम", "फ़ॉन्ट विकल्प", "विशाल"
    ),
    "bn" to lyricsFonts(
        "আমার ফন্ট", "ফোন থেকে TTF বা OTF ফন্ট আমদানি করুন (সর্বোচ্চ 128 MB)", "ফন্ট আমদানি করুন", "আমদানি হচ্ছে…",
        "ফন্ট আমদানি হয়েছে", "ফন্ট প্রতিস্থাপিত হয়েছে", "এই ফাইলটি বৈধ TTF বা OTF ফন্ট নয়", "ফন্টটি 128 MB-এর বেশি",
        "ফন্ট লাইব্রেরি পূর্ণ। নতুন আমদানির জন্য একটি মুছুন", "ফন্ট ফাইল পড়া যায়নি",
        "নির্বাচিত ফন্ট পাওয়া যাচ্ছে না। বিল্ট-ইন ফন্ট ব্যবহার হচ্ছে", "ভেরিয়েবল", "নাম বদলান", "ফাইল প্রতিস্থাপন",
        "ফন্ট মুছবেন?", "এই ফন্টের লিরিক্স বিল্ট-ইন ফন্টে ফিরে যাবে।", "ফন্টের নাম", "ফন্ট বিকল্প", "বিশাল"
    ),
    "id" to lyricsFonts(
        "Font saya", "Impor font TTF atau OTF dari ponsel (maks. 128 MB)", "Impor font", "Mengimpor…",
        "Font diimpor", "Font diganti", "File ini bukan font TTF atau OTF yang valid", "Font lebih dari 128 MB",
        "Pustaka font penuh. Hapus satu font untuk mengimpor yang lain", "File font tidak dapat dibaca",
        "Font yang dipilih tidak tersedia. Menggunakan font bawaan", "Variabel", "Ganti nama", "Ganti file",
        "Hapus font?", "Lirik dengan font ini akan kembali ke font bawaan.", "Nama font", "Opsi font", "Raksasa"
    ),
    "ms" to lyricsFonts(
        "Fon saya", "Import fon TTF atau OTF dari telefon (maks. 128 MB)", "Import fon", "Mengimport…",
        "Fon diimport", "Fon diganti", "Fail ini bukan fon TTF atau OTF yang sah", "Fon melebihi 128 MB",
        "Pustaka fon penuh. Padam satu fon untuk mengimport yang lain", "Fail fon tidak dapat dibaca",
        "Fon yang dipilih tidak tersedia. Fon terbina dalam digunakan", "Boleh ubah", "Namakan semula", "Ganti fail",
        "Padam fon?", "Lirik dengan fon ini akan kembali ke fon terbina dalam.", "Nama fon", "Pilihan fon", "Gergasi"
    ),
    "vi" to lyricsFonts(
        "Phông chữ của tôi", "Nhập phông TTF hoặc OTF từ điện thoại (tối đa 128 MB)", "Nhập phông chữ", "Đang nhập…",
        "Đã nhập phông chữ", "Đã thay phông chữ", "Tệp này không phải phông TTF hoặc OTF hợp lệ", "Phông chữ lớn hơn 128 MB",
        "Thư viện phông chữ đã đầy. Hãy xóa một phông để nhập phông khác", "Không đọc được tệp phông chữ",
        "Phông chữ đã chọn không khả dụng. Đang dùng phông tích hợp", "Biến thiên", "Đổi tên", "Thay tệp",
        "Xóa phông chữ?", "Lời bài hát dùng phông này sẽ trở về phông tích hợp.", "Tên phông chữ", "Tùy chọn phông chữ", "Khổng lồ"
    ),
    "th" to lyricsFonts(
        "ฟอนต์ของฉัน", "นำเข้าฟอนต์ TTF หรือ OTF จากโทรศัพท์ (สูงสุด 128 MB)", "นำเข้าฟอนต์", "กำลังนำเข้า…",
        "นำเข้าฟอนต์แล้ว", "แทนที่ฟอนต์แล้ว", "ไฟล์นี้ไม่ใช่ฟอนต์ TTF หรือ OTF ที่ถูกต้อง", "ฟอนต์มีขนาดเกิน 128 MB",
        "คลังฟอนต์เต็มแล้ว ลบฟอนต์หนึ่งเพื่อนำเข้าฟอนต์ใหม่", "อ่านไฟล์ฟอนต์ไม่ได้",
        "ไม่สามารถใช้ฟอนต์ที่เลือก กำลังใช้ฟอนต์ในตัว", "แปรผัน", "เปลี่ยนชื่อ", "แทนที่ไฟล์",
        "ลบฟอนต์ไหม", "เนื้อเพลงที่ใช้ฟอนต์นี้จะกลับไปใช้ฟอนต์ในตัว", "ชื่อฟอนต์", "ตัวเลือกฟอนต์", "ใหญ่พิเศษ"
    ),
    "fil" to lyricsFonts(
        "Aking mga font", "Mag-import ng TTF o OTF na font mula sa phone (max 128 MB)", "Mag-import ng font", "Nag-i-import…",
        "Na-import ang font", "Napalitan ang font", "Hindi valid na TTF o OTF na font ang file na ito", "Lampas sa 128 MB ang font",
        "Puno na ang font library. Magbura ng isa para makapag-import", "Hindi mabasa ang font file",
        "Hindi available ang napiling font. Ginagamit ang built-in na font", "Variable", "Palitan ang pangalan", "Palitan ang file",
        "Burahin ang font?", "Babalik sa built-in na font ang lyrics na gumagamit nito.", "Pangalan ng font", "Mga opsyon sa font", "Napakalaki"
    ),
    "he" to lyricsFonts(
        "הגופנים שלי", "ייבוא גופני TTF או OTF מהטלפון (עד 128MB)", "ייבוא גופן", "מייבא…",
        "הגופן יובא", "הגופן הוחלף", "הקובץ אינו גופן TTF או OTF תקין", "הגופן גדול מ־128MB",
        "ספריית הגופנים מלאה. מחקו גופן כדי לייבא אחר", "לא ניתן לקרוא את קובץ הגופן",
        "הגופן שנבחר אינו זמין. נעשה שימוש בגופן המובנה", "משתנה", "שינוי שם", "החלפת קובץ",
        "למחוק את הגופן?", "מילים שמשתמשות בגופן זה יחזרו לגופן המובנה.", "שם הגופן", "אפשרויות גופן", "ענק"
    )
)

internal fun lyricsFontLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(lyricsFontBundles, code)

internal fun lyricsFontLocalizationCodes(): Set<String> = lyricsFontBundles.keys
