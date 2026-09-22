package com.luc4n3x.levyra.ui.i18n

internal val localLibraryKeys = setOf(
    "localOnDevice",
    "localOnDeviceSubtitle",
    "localFolders",
    "localScanQuick",
    "localScanFull",
    "localRebuildLevyra",
    "localScanning",
    "localScanUpToDate",
    "localScanSummary",
    "localEmpty",
    "localPermissionRequired",
    "localGrantPermission",
    "localUnknownArtist",
    "localUnknownAlbum",
    "localHideFolder",
    "localShowFolder",
    "localFileUnavailable",
    "localDuplicatesHidden"
)

private fun localLibrary(
    onDevice: String,
    onDeviceSubtitle: String,
    folders: String,
    scanQuick: String,
    scanFull: String,
    rebuildLevyra: String,
    scanning: String,
    scanUpToDate: String,
    scanSummary: String,
    empty: String,
    permissionRequired: String,
    grantPermission: String,
    unknownArtist: String,
    unknownAlbum: String,
    hideFolder: String,
    showFolder: String,
    fileUnavailable: String,
    duplicatesHidden: String
): Map<String, String> = mapOf(
    "localOnDevice" to onDevice,
    "localOnDeviceSubtitle" to onDeviceSubtitle,
    "localFolders" to folders,
    "localScanQuick" to scanQuick,
    "localScanFull" to scanFull,
    "localRebuildLevyra" to rebuildLevyra,
    "localScanning" to scanning,
    "localScanUpToDate" to scanUpToDate,
    "localScanSummary" to scanSummary,
    "localEmpty" to empty,
    "localPermissionRequired" to permissionRequired,
    "localGrantPermission" to grantPermission,
    "localUnknownArtist" to unknownArtist,
    "localUnknownAlbum" to unknownAlbum,
    "localHideFolder" to hideFolder,
    "localShowFolder" to showFolder,
    "localFileUnavailable" to fileUnavailable,
    "localDuplicatesHidden" to duplicatesHidden
)

private val localLibraryBundles: Map<String, Map<String, String>> = mapOf(
    "en" to localLibrary(
        "On device", "The music saved on this phone, indexed by Levyra.", "Folders", "Quick rescan", "Full rescan",
        "Rebuild Levyra downloads", "Scanning your music…", "Library already up to date",
        "%1\$d new · %2\$d updated · %3\$d unavailable", "No music found on this device yet.",
        "Levyra needs access to your audio files to build the local library.", "Allow access",
        "Unknown artist", "Unknown album", "Hide this folder", "Show this folder",
        "This file is not available right now.", "Duplicate copies hidden"
    ),
    "it" to localLibrary(
        "Sul dispositivo", "La musica salvata su questo telefono, indicizzata da Levyra.", "Cartelle",
        "Scansione rapida", "Scansione completa", "Ricostruisci i download Levyra", "Sto analizzando la musica…",
        "Libreria già aggiornata", "%1\$d nuovi · %2\$d aggiornati · %3\$d non disponibili",
        "Nessuna musica trovata su questo dispositivo.",
        "Levyra ha bisogno dell'accesso ai file audio per creare la libreria locale.", "Consenti l'accesso",
        "Artista sconosciuto", "Album sconosciuto", "Nascondi questa cartella", "Mostra questa cartella",
        "Questo file non è disponibile al momento.", "Copie duplicate nascoste"
    ),
    "es" to localLibrary(
        "En el dispositivo", "La música guardada en este teléfono, indexada por Levyra.", "Carpetas",
        "Análisis rápido", "Análisis completo", "Reconstruir las descargas de Levyra", "Analizando tu música…",
        "La biblioteca ya está al día", "%1\$d nuevas · %2\$d actualizadas · %3\$d no disponibles",
        "Todavía no hay música en este dispositivo.",
        "Levyra necesita acceso a tus archivos de audio para crear la biblioteca local.", "Permitir el acceso",
        "Artista desconocido", "Álbum desconocido", "Ocultar esta carpeta", "Mostrar esta carpeta",
        "Este archivo no está disponible ahora mismo.", "Copias duplicadas ocultas"
    ),
    "fr" to localLibrary(
        "Sur l'appareil", "La musique enregistrée sur ce téléphone, indexée par Levyra.", "Dossiers",
        "Analyse rapide", "Analyse complète", "Reconstruire les téléchargements Levyra",
        "Analyse de votre musique…", "Bibliothèque déjà à jour",
        "%1\$d nouveaux · %2\$d mis à jour · %3\$d indisponibles", "Aucune musique trouvée sur cet appareil.",
        "Levyra a besoin d'accéder à vos fichiers audio pour créer la bibliothèque locale.", "Autoriser l'accès",
        "Artiste inconnu", "Album inconnu", "Masquer ce dossier", "Afficher ce dossier",
        "Ce fichier n'est pas disponible pour le moment.", "Copies en double masquées"
    ),
    "de" to localLibrary(
        "Auf dem Gerät", "Die Musik auf diesem Telefon, von Levyra indexiert.", "Ordner", "Schnell aktualisieren",
        "Vollständig neu einlesen", "Levyra-Downloads neu aufbauen", "Deine Musik wird gelesen…",
        "Bibliothek ist aktuell", "%1\$d neu · %2\$d aktualisiert · %3\$d nicht verfügbar",
        "Auf diesem Gerät wurde noch keine Musik gefunden.",
        "Levyra braucht Zugriff auf deine Audiodateien für die lokale Bibliothek.", "Zugriff erlauben",
        "Unbekannter Künstler", "Unbekanntes Album", "Diesen Ordner ausblenden", "Diesen Ordner einblenden",
        "Diese Datei ist gerade nicht verfügbar.", "Doppelte Kopien ausgeblendet"
    ),
    "pt" to localLibrary(
        "No dispositivo", "A música guardada neste telefone, indexada pelo Levyra.", "Pastas", "Análise rápida",
        "Análise completa", "Reconstruir as transferências Levyra", "A analisar a tua música…",
        "Biblioteca já atualizada", "%1\$d novas · %2\$d atualizadas · %3\$d indisponíveis",
        "Ainda não há música neste dispositivo.",
        "O Levyra precisa de acesso aos teus ficheiros de áudio para criar a biblioteca local.", "Permitir acesso",
        "Artista desconhecido", "Álbum desconhecido", "Ocultar esta pasta", "Mostrar esta pasta",
        "Este ficheiro não está disponível agora.", "Cópias duplicadas ocultas"
    ),
    "nl" to localLibrary(
        "Op dit apparaat", "De muziek op deze telefoon, geïndexeerd door Levyra.", "Mappen",
        "Snel opnieuw scannen", "Volledig opnieuw scannen", "Levyra-downloads opnieuw opbouwen",
        "Je muziek wordt gescand…", "Bibliotheek is al bijgewerkt",
        "%1\$d nieuw · %2\$d bijgewerkt · %3\$d niet beschikbaar", "Nog geen muziek gevonden op dit apparaat.",
        "Levyra heeft toegang tot je audiobestanden nodig voor de lokale bibliotheek.", "Toegang toestaan",
        "Onbekende artiest", "Onbekend album", "Deze map verbergen", "Deze map weergeven",
        "Dit bestand is nu niet beschikbaar.", "Dubbele kopieën verborgen"
    ),
    "pl" to localLibrary(
        "W urządzeniu", "Muzyka zapisana w tym telefonie, zindeksowana przez Levyrę.", "Foldery",
        "Szybkie skanowanie", "Pełne skanowanie", "Odbuduj pobrane pliki Levyry", "Skanuję twoją muzykę…",
        "Biblioteka jest już aktualna", "%1\$d nowych · %2\$d zaktualizowanych · %3\$d niedostępnych",
        "Nie znaleziono jeszcze muzyki w tym urządzeniu.",
        "Levyra potrzebuje dostępu do plików audio, aby zbudować bibliotekę lokalną.", "Zezwól na dostęp",
        "Nieznany wykonawca", "Nieznany album", "Ukryj ten folder", "Pokaż ten folder",
        "Ten plik jest teraz niedostępny.", "Ukryto duplikaty"
    ),
    "ro" to localLibrary(
        "Pe dispozitiv", "Muzica salvată pe acest telefon, indexată de Levyra.", "Foldere", "Scanare rapidă",
        "Scanare completă", "Reconstruiește descărcările Levyra", "Îți scanez muzica…",
        "Biblioteca este deja actualizată", "%1\$d noi · %2\$d actualizate · %3\$d indisponibile",
        "Încă nu am găsit muzică pe acest dispozitiv.",
        "Levyra are nevoie de acces la fișierele audio pentru biblioteca locală.", "Permite accesul",
        "Artist necunoscut", "Album necunoscut", "Ascunde acest folder", "Arată acest folder",
        "Acest fișier nu este disponibil acum.", "Copii duplicate ascunse"
    ),
    "el" to localLibrary(
        "Στη συσκευή", "Η μουσική που είναι αποθηκευμένη σε αυτό το τηλέφωνο, με ευρετήριο από το Levyra.",
        "Φάκελοι", "Γρήγορη σάρωση", "Πλήρης σάρωση", "Ανακατασκευή λήψεων Levyra", "Σαρώνω τη μουσική σου…",
        "Η βιβλιοθήκη είναι ενημερωμένη", "%1\$d νέα · %2\$d ενημερωμένα · %3\$d μη διαθέσιμα",
        "Δεν βρέθηκε ακόμη μουσική σε αυτή τη συσκευή.",
        "Το Levyra χρειάζεται πρόσβαση στα αρχεία ήχου για την τοπική βιβλιοθήκη.", "Να επιτραπεί η πρόσβαση",
        "Άγνωστος καλλιτέχνης", "Άγνωστο άλμπουμ", "Απόκρυψη αυτού του φακέλου", "Εμφάνιση αυτού του φακέλου",
        "Αυτό το αρχείο δεν είναι διαθέσιμο τώρα.", "Τα διπλότυπα αποκρύφτηκαν"
    ),
    "sv" to localLibrary(
        "På enheten", "Musiken som finns i den här telefonen, indexerad av Levyra.", "Mappar",
        "Snabb genomsökning", "Fullständig genomsökning", "Bygg om Levyra-nedladdningar", "Söker igenom din musik…",
        "Biblioteket är redan uppdaterat", "%1\$d nya · %2\$d uppdaterade · %3\$d otillgängliga",
        "Ingen musik hittades på den här enheten än.",
        "Levyra behöver åtkomst till dina ljudfiler för det lokala biblioteket.", "Tillåt åtkomst",
        "Okänd artist", "Okänt album", "Dölj den här mappen", "Visa den här mappen",
        "Den här filen är inte tillgänglig just nu.", "Dubbletter dolda"
    ),
    "da" to localLibrary(
        "På enheden", "Musikken på denne telefon, indekseret af Levyra.", "Mapper", "Hurtig scanning",
        "Fuld scanning", "Genopbyg Levyra-downloads", "Scanner din musik…", "Biblioteket er allerede opdateret",
        "%1\$d nye · %2\$d opdaterede · %3\$d utilgængelige", "Der blev ikke fundet musik på denne enhed endnu.",
        "Levyra skal have adgang til dine lydfiler for at bygge det lokale bibliotek.", "Tillad adgang",
        "Ukendt kunstner", "Ukendt album", "Skjul denne mappe", "Vis denne mappe",
        "Denne fil er ikke tilgængelig lige nu.", "Dubletter skjult"
    ),
    "cs" to localLibrary(
        "V zařízení", "Hudba uložená v tomto telefonu, indexovaná Levyrou.", "Složky", "Rychlá kontrola",
        "Úplná kontrola", "Znovu sestavit stažené soubory Levyry", "Prohledávám tvou hudbu…",
        "Knihovna je už aktuální", "%1\$d nových · %2\$d aktualizovaných · %3\$d nedostupných",
        "V tomto zařízení zatím nebyla nalezena hudba.",
        "Levyra potřebuje přístup ke zvukovým souborům, aby vytvořila místní knihovnu.", "Povolit přístup",
        "Neznámý interpret", "Neznámé album", "Skrýt tuto složku", "Zobrazit tuto složku",
        "Tento soubor teď není dostupný.", "Duplikáty skryty"
    ),
    "sk" to localLibrary(
        "V zariadení", "Hudba uložená v tomto telefóne, indexovaná Levyrou.", "Priečinky", "Rýchla kontrola",
        "Úplná kontrola", "Znovu vytvoriť stiahnuté súbory Levyry", "Prehľadávam tvoju hudbu…",
        "Knižnica je už aktuálna", "%1\$d nových · %2\$d aktualizovaných · %3\$d nedostupných",
        "V tomto zariadení sa zatiaľ nenašla hudba.",
        "Levyra potrebuje prístup k zvukovým súborom, aby vytvorila lokálnu knižnicu.", "Povoliť prístup",
        "Neznámy interpret", "Neznámy album", "Skryť tento priečinok", "Zobraziť tento priečinok",
        "Tento súbor teraz nie je dostupný.", "Duplikáty skryté"
    ),
    "hr" to localLibrary(
        "Na uređaju", "Glazba spremljena na ovom telefonu, indeksirana Levyrom.", "Mape", "Brzo pretraživanje",
        "Potpuno pretraživanje", "Obnovi Levyra preuzimanja", "Pretražujem tvoju glazbu…",
        "Knjižnica je već ažurna", "%1\$d novih · %2\$d ažuriranih · %3\$d nedostupnih",
        "Na ovom uređaju još nema glazbe.",
        "Levyra treba pristup tvojim zvučnim datotekama za lokalnu knjižnicu.", "Dopusti pristup",
        "Nepoznati izvođač", "Nepoznati album", "Sakrij ovu mapu", "Prikaži ovu mapu",
        "Ova datoteka trenutačno nije dostupna.", "Duplikati su skriveni"
    ),
    "bg" to localLibrary(
        "На устройството", "Музиката, запазена на този телефон, индексирана от Levyra.", "Папки",
        "Бързо сканиране", "Пълно сканиране", "Възстановяване на изтеглянията на Levyra",
        "Сканирам твоята музика…", "Библиотеката вече е актуална",
        "%1\$d нови · %2\$d обновени · %3\$d недостъпни", "На това устройство още няма намерена музика.",
        "Levyra има нужда от достъп до аудиофайловете ти, за да създаде локалната библиотека.",
        "Разреши достъпа", "Неизвестен изпълнител", "Неизвестен албум", "Скрий тази папка", "Покажи тази папка",
        "Този файл не е достъпен в момента.", "Дублиращите копия са скрити"
    ),
    "hu" to localLibrary(
        "Az eszközön", "A telefonon tárolt zene, a Levyra indexével.", "Mappák", "Gyors újraolvasás",
        "Teljes újraolvasás", "Levyra-letöltések újraépítése", "Zenéd átvizsgálása…",
        "A könyvtár már naprakész", "%1\$d új · %2\$d frissített · %3\$d nem elérhető",
        "Ezen az eszközön még nem találtam zenét.",
        "A Levyrának hozzáférés kell a hangfájlokhoz a helyi könyvtár felépítéséhez.", "Hozzáférés engedélyezése",
        "Ismeretlen előadó", "Ismeretlen album", "Mappa elrejtése", "Mappa megjelenítése",
        "Ez a fájl most nem elérhető.", "Duplikált példányok elrejtve"
    ),
    "fi" to localLibrary(
        "Laitteella", "Tähän puhelimeen tallennettu musiikki, Levyran indeksoima.", "Kansiot", "Nopea haku",
        "Täysi haku", "Rakenna Levyra-lataukset uudelleen", "Käyn musiikkiasi läpi…",
        "Kirjasto on jo ajan tasalla", "%1\$d uutta · %2\$d päivitettyä · %3\$d ei saatavilla",
        "Tältä laitteelta ei löytynyt vielä musiikkia.",
        "Levyra tarvitsee käyttöoikeuden äänitiedostoihin paikallista kirjastoa varten.", "Salli käyttöoikeus",
        "Tuntematon esittäjä", "Tuntematon albumi", "Piilota tämä kansio", "Näytä tämä kansio",
        "Tämä tiedosto ei ole juuri nyt saatavilla.", "Kaksoiskappaleet piilotettu"
    ),
    "et" to localLibrary(
        "Seadmes", "Sellesse telefoni salvestatud muusika, indekseeritud Levyra poolt.", "Kaustad", "Kiire skannimine",
        "Täielik skannimine", "Taasta Levyra allalaadimised", "Muusika skannimine…",
        "Kogu on juba ajakohane", "%1\$d uut · %2\$d uuendatud · %3\$d pole saadaval",
        "Sellest seadmest ei leitud veel muusikat.",
        "Levyra vajab kohaliku kogu loomiseks juurdepääsu helifailidele.", "Luba juurdepääs",
        "Tundmatu esitaja", "Tundmatu album", "Peida see kaust", "Kuva see kaust",
        "See fail pole praegu saadaval.", "Duplikaadid peidetud"
    ),
    "nb" to localLibrary(
        "På enheten", "Musikken som er lagret på denne telefonen, indeksert av Levyra.", "Mapper",
        "Rask gjennomgang", "Full gjennomgang", "Bygg opp Levyra-nedlastinger på nytt", "Skanner musikken din…",
        "Biblioteket er allerede oppdatert", "%1\$d nye · %2\$d oppdaterte · %3\$d utilgjengelige",
        "Fant ingen musikk på denne enheten ennå.",
        "Levyra trenger tilgang til lydfilene dine for å bygge det lokale biblioteket.", "Tillat tilgang",
        "Ukjent artist", "Ukjent album", "Skjul denne mappen", "Vis denne mappen",
        "Denne filen er ikke tilgjengelig nå.", "Duplikater skjult"
    ),
    "ca" to localLibrary(
        "Al dispositiu", "La música desada en aquest telèfon, indexada per Levyra.", "Carpetes", "Anàlisi ràpida",
        "Anàlisi completa", "Reconstrueix les baixades de Levyra", "Analitzant la teva música…",
        "La biblioteca ja està actualitzada", "%1\$d noves · %2\$d actualitzades · %3\$d no disponibles",
        "Encara no s'ha trobat música en aquest dispositiu.",
        "Levyra necessita accés als teus fitxers d'àudio per crear la biblioteca local.", "Permet l'accés",
        "Artista desconegut", "Àlbum desconegut", "Amaga aquesta carpeta", "Mostra aquesta carpeta",
        "Aquest fitxer no està disponible ara mateix.", "Còpies duplicades amagades"
    ),
    "uk" to localLibrary(
        "На пристрої", "Музика, збережена на цьому телефоні, проіндексована Levyra.", "Папки",
        "Швидке сканування", "Повне сканування", "Відновити завантаження Levyra", "Сканую твою музику…",
        "Бібліотека вже актуальна", "%1\$d нових · %2\$d оновлених · %3\$d недоступних",
        "На цьому пристрої ще немає музики.",
        "Levyra потребує доступу до твоїх аудіофайлів, щоб створити локальну бібліотеку.", "Дозволити доступ",
        "Невідомий виконавець", "Невідомий альбом", "Сховати цю папку", "Показати цю папку",
        "Цей файл зараз недоступний.", "Дублікати приховано"
    ),
    "ru" to localLibrary(
        "На устройстве", "Музыка, сохранённая на этом телефоне, проиндексированная Levyra.", "Папки",
        "Быстрое сканирование", "Полное сканирование", "Пересобрать загрузки Levyra", "Сканирую твою музыку…",
        "Медиатека уже обновлена", "%1\$d новых · %2\$d обновлённых · %3\$d недоступных",
        "На этом устройстве музыка пока не найдена.",
        "Levyra нужен доступ к аудиофайлам, чтобы собрать локальную медиатеку.", "Разрешить доступ",
        "Неизвестный исполнитель", "Неизвестный альбом", "Скрыть эту папку", "Показать эту папку",
        "Этот файл сейчас недоступен.", "Дубликаты скрыты"
    ),
    "tr" to localLibrary(
        "Cihazda", "Bu telefonda saklanan müzik, Levyra tarafından dizinlendi.", "Klasörler", "Hızlı tarama",
        "Tam tarama", "Levyra indirmelerini yeniden oluştur", "Müziğin taranıyor…", "Kitaplık zaten güncel",
        "%1\$d yeni · %2\$d güncellendi · %3\$d kullanılamıyor", "Bu cihazda henüz müzik bulunamadı.",
        "Levyra'nın yerel kitaplığı oluşturmak için ses dosyalarına erişmesi gerekiyor.", "Erişime izin ver",
        "Bilinmeyen sanatçı", "Bilinmeyen albüm", "Bu klasörü gizle", "Bu klasörü göster",
        "Bu dosya şu anda kullanılamıyor.", "Yinelenen kopyalar gizlendi"
    ),
    "ar" to localLibrary(
        "على الجهاز", "الموسيقى المحفوظة على هذا الهاتف، مفهرسة بواسطة Levyra.", "المجلدات", "فحص سريع",
        "فحص كامل", "إعادة بناء تنزيلات Levyra", "يجري فحص موسيقاك…", "المكتبة محدَّثة بالفعل",
        "%1\$d جديدة · %2\$d محدَّثة · %3\$d غير متوفرة", "لم نجد موسيقى على هذا الجهاز بعد.",
        "يحتاج Levyra إلى الوصول إلى ملفاتك الصوتية لبناء المكتبة المحلية.", "السماح بالوصول",
        "فنان غير معروف", "ألبوم غير معروف", "إخفاء هذا المجلد", "إظهار هذا المجلد",
        "هذا الملف غير متوفر حاليًا.", "تم إخفاء النسخ المكررة"
    ),
    "fa" to localLibrary(
        "روی دستگاه", "موسیقی ذخیره‌شده در این گوشی، فهرست‌شده توسط Levyra.", "پوشه‌ها", "بررسی سریع",
        "بررسی کامل", "بازسازی دانلودهای Levyra", "موسیقی‌ات بررسی می‌شود…", "کتابخانه از قبل به‌روز است",
        "%1\$d تازه · %2\$d به‌روزشده · %3\$d ناموجود", "هنوز موسیقی‌ای روی این دستگاه پیدا نشد.",
        "Levyra برای ساختن کتابخانه محلی به دسترسی فایل‌های صوتی نیاز دارد.", "اجازه دسترسی",
        "هنرمند ناشناس", "آلبوم ناشناس", "پنهان کردن این پوشه", "نمایش این پوشه",
        "این فایل در حال حاضر در دسترس نیست.", "نسخه‌های تکراری پنهان شدند"
    ),
    "zh" to localLibrary(
        "本机音乐", "保存在这台手机上的音乐，由 Levyra 编入索引。", "文件夹", "快速扫描", "完整扫描",
        "重建 Levyra 下载", "正在扫描你的音乐…", "音乐库已是最新",
        "新增 %1\$d · 更新 %2\$d · 不可用 %3\$d", "这台设备上还没有找到音乐。",
        "Levyra 需要访问你的音频文件才能建立本地音乐库。", "允许访问", "未知艺人", "未知专辑",
        "隐藏此文件夹", "显示此文件夹", "该文件目前无法使用。", "已隐藏重复副本"
    ),
    "zh-Hant" to localLibrary(
        "本機音樂", "儲存在這支手機上的音樂，由 Levyra 建立索引。", "資料夾", "快速掃描", "完整掃描",
        "重建 Levyra 下載", "正在掃描你的音樂…", "音樂庫已是最新",
        "新增 %1\$d · 更新 %2\$d · 無法使用 %3\$d", "這台裝置上還沒有找到音樂。",
        "Levyra 需要存取你的音訊檔案才能建立本機音樂庫。", "允許存取", "未知藝人", "未知專輯",
        "隱藏這個資料夾", "顯示這個資料夾", "這個檔案目前無法使用。", "已隱藏重複複本"
    ),
    "ja" to localLibrary(
        "この端末の音楽", "この端末に保存された音楽を Levyra が索引します。", "フォルダ", "クイック再スキャン",
        "フル再スキャン", "Levyra のダウンロードを再構築", "音楽をスキャン中…", "ライブラリは最新です",
        "新規 %1\$d 件 · 更新 %2\$d 件 · 利用不可 %3\$d 件", "この端末にはまだ音楽が見つかりません。",
        "ローカルライブラリを作るには音声ファイルへのアクセスが必要です。", "アクセスを許可",
        "不明なアーティスト", "不明なアルバム", "このフォルダを隠す", "このフォルダを表示",
        "このファイルは現在利用できません。", "重複コピーを非表示にしました"
    ),
    "ko" to localLibrary(
        "이 기기", "이 휴대전화에 저장된 음악을 Levyra가 색인합니다.", "폴더", "빠른 다시 검색",
        "전체 다시 검색", "Levyra 다운로드 다시 만들기", "음악을 검색하는 중…",
        "라이브러리가 이미 최신입니다", "새로 %1\$d · 업데이트 %2\$d · 사용 불가 %3\$d",
        "이 기기에서 음악을 찾지 못했습니다.",
        "로컬 라이브러리를 만들려면 오디오 파일 접근 권한이 필요합니다.", "접근 허용",
        "알 수 없는 아티스트", "알 수 없는 앨범", "이 폴더 숨기기", "이 폴더 표시",
        "이 파일은 지금 사용할 수 없습니다.", "중복 사본 숨김"
    ),
    "hi" to localLibrary(
        "इस डिवाइस पर", "इस फ़ोन में सेव संगीत, Levyra द्वारा अनुक्रमित।", "फ़ोल्डर", "तेज़ स्कैन",
        "पूरा स्कैन", "Levyra डाउनलोड फिर बनाएं", "आपका संगीत स्कैन हो रहा है…",
        "लाइब्रेरी पहले से अपडेट है", "%1\$d नए · %2\$d अपडेट · %3\$d अनुपलब्ध",
        "इस डिवाइस पर अभी कोई संगीत नहीं मिला।",
        "स्थानीय लाइब्रेरी बनाने के लिए Levyra को ऑडियो फ़ाइलों की अनुमति चाहिए।", "पहुँच दें",
        "अज्ञात कलाकार", "अज्ञात एल्बम", "यह फ़ोल्डर छिपाएं", "यह फ़ोल्डर दिखाएं",
        "यह फ़ाइल अभी उपलब्ध नहीं है।", "डुप्लिकेट प्रतियां छिपाई गईं"
    ),
    "id" to localLibrary(
        "Di perangkat", "Musik yang tersimpan di ponsel ini, diindeks oleh Levyra.", "Folder", "Pindai cepat",
        "Pindai penuh", "Bangun ulang unduhan Levyra", "Memindai musikmu…", "Pustaka sudah terbaru",
        "%1\$d baru · %2\$d diperbarui · %3\$d tidak tersedia",
        "Belum ada musik yang ditemukan di perangkat ini.",
        "Levyra perlu akses ke berkas audiomu untuk membangun pustaka lokal.", "Izinkan akses",
        "Artis tidak diketahui", "Album tidak diketahui", "Sembunyikan folder ini", "Tampilkan folder ini",
        "Berkas ini tidak tersedia sekarang.", "Salinan duplikat disembunyikan"
    ),
    "ms" to localLibrary(
        "Pada peranti", "Muzik yang disimpan dalam telefon ini, diindeks oleh Levyra.", "Folder",
        "Imbasan pantas", "Imbasan penuh", "Bina semula muat turun Levyra", "Mengimbas muzik anda…",
        "Pustaka sudah terkini", "%1\$d baharu · %2\$d dikemas kini · %3\$d tidak tersedia",
        "Belum ada muzik ditemui pada peranti ini.",
        "Levyra perlukan akses kepada fail audio anda untuk membina pustaka tempatan.", "Benarkan akses",
        "Artis tidak diketahui", "Album tidak diketahui", "Sembunyikan folder ini", "Tunjukkan folder ini",
        "Fail ini tidak tersedia sekarang.", "Salinan pendua disembunyikan"
    ),
    "vi" to localLibrary(
        "Trên thiết bị", "Nhạc lưu trong điện thoại này, do Levyra lập chỉ mục.", "Thư mục", "Quét nhanh",
        "Quét toàn bộ", "Dựng lại các bản tải Levyra", "Đang quét nhạc của bạn…",
        "Thư viện đã được cập nhật", "%1\$d mới · %2\$d đã cập nhật · %3\$d không khả dụng",
        "Chưa tìm thấy nhạc trên thiết bị này.",
        "Levyra cần quyền truy cập tệp âm thanh để tạo thư viện cục bộ.", "Cho phép truy cập",
        "Nghệ sĩ không rõ", "Album không rõ", "Ẩn thư mục này", "Hiện thư mục này",
        "Tệp này hiện không khả dụng.", "Đã ẩn các bản sao trùng"
    ),
    "th" to localLibrary(
        "ในอุปกรณ์", "เพลงที่เก็บไว้ในโทรศัพท์นี้ จัดทำดัชนีโดย Levyra", "โฟลเดอร์", "สแกนเร็ว",
        "สแกนทั้งหมด", "สร้างรายการดาวน์โหลด Levyra ใหม่", "กำลังสแกนเพลงของคุณ…",
        "คลังเพลงเป็นข้อมูลล่าสุดแล้ว", "ใหม่ %1\$d · อัปเดต %2\$d · ใช้ไม่ได้ %3\$d",
        "ยังไม่พบเพลงในอุปกรณ์นี้",
        "Levyra ต้องเข้าถึงไฟล์เสียงของคุณเพื่อสร้างคลังเพลงในเครื่อง", "อนุญาตการเข้าถึง",
        "ไม่ทราบศิลปิน", "ไม่ทราบอัลบั้ม", "ซ่อนโฟลเดอร์นี้", "แสดงโฟลเดอร์นี้",
        "ไฟล์นี้ใช้ไม่ได้ในขณะนี้", "ซ่อนไฟล์ที่ซ้ำกันแล้ว"
    ),
    "fil" to localLibrary(
        "Sa device", "Ang musikang nasa teleponong ito, na iniindex ng Levyra.", "Mga folder",
        "Mabilis na scan", "Buong scan", "Muling buuin ang mga download ng Levyra", "Sinusuri ang musika mo…",
        "Updated na ang library", "%1\$d bago · %2\$d na-update · %3\$d hindi available",
        "Wala pa kaming nahanap na musika sa device na ito.",
        "Kailangan ng Levyra ng access sa audio files mo para buuin ang lokal na library.", "Payagan ang access",
        "Hindi kilalang artist", "Hindi kilalang album", "Itago ang folder na ito", "Ipakita ang folder na ito",
        "Hindi available ang file na ito ngayon.", "Nakatago ang mga duplicate na kopya"
    ),
    "he" to localLibrary(
        "במכשיר", "המוזיקה ששמורה בטלפון הזה, ממופה על ידי Levyra.", "תיקיות", "סריקה מהירה", "סריקה מלאה",
        "בנייה מחדש של הורדות Levyra", "סורק את המוזיקה שלך…", "הספרייה כבר מעודכנת",
        "%1\$d חדשים · %2\$d עודכנו · %3\$d לא זמינים", "עדיין לא נמצאה מוזיקה במכשיר הזה.",
        "Levyra צריכה גישה לקובצי האודיו שלך כדי לבנות את הספרייה המקומית.", "אישור גישה",
        "אמן לא ידוע", "אלבום לא ידוע", "הסתרת התיקייה הזו", "הצגת התיקייה הזו",
        "הקובץ הזה לא זמין כרגע.", "עותקים כפולים הוסתרו"
    )
)

private val localScanFailedMessages = mapOf(
    "en" to "Couldn't scan your music.",
    "it" to "Impossibile analizzare la musica.",
    "es" to "No se pudo analizar tu música.",
    "fr" to "Impossible d’analyser votre musique.",
    "de" to "Deine Musik konnte nicht gescannt werden.",
    "pt" to "Não foi possível analisar a tua música.",
    "nl" to "Je muziek kon niet worden gescand.",
    "pl" to "Nie udało się przeskanować muzyki.",
    "ro" to "Muzica nu a putut fi scanată.",
    "el" to "Δεν ήταν δυνατή η σάρωση της μουσικής σου.",
    "sv" to "Det gick inte att skanna din musik.",
    "da" to "Din musik kunne ikke scannes.",
    "cs" to "Hudbu se nepodařilo prohledat.",
    "sk" to "Hudbu sa nepodarilo prehľadať.",
    "hr" to "Skeniranje glazbe nije uspjelo.",
    "bg" to "Музиката не можа да бъде сканирана.",
    "hu" to "A zene beolvasása nem sikerült.",
    "fi" to "Musiikin skannaus epäonnistui.",
    "et" to "Muusika skannimine ebaõnnestus.",
    "nb" to "Kunne ikke skanne musikken din.",
    "ca" to "No s'ha pogut analitzar la música.",
    "uk" to "Не вдалося просканувати музику.",
    "ru" to "Не удалось просканировать музыку.",
    "tr" to "Müziğin taranamadı.",
    "ar" to "تعذّر فحص الموسيقى.",
    "fa" to "اسکن موسیقی انجام نشد.",
    "zh" to "无法扫描你的音乐。",
    "zh-Hant" to "無法掃描你的音樂。",
    "ja" to "音楽をスキャンできませんでした。",
    "ko" to "음악을 검색하지 못했습니다.",
    "hi" to "आपके संगीत को स्कैन नहीं किया जा सका।",
    "id" to "Musikmu tidak dapat dipindai.",
    "ms" to "Muzik anda tidak dapat diimbas.",
    "vi" to "Không thể quét nhạc của bạn.",
    "th" to "ไม่สามารถสแกนเพลงของคุณได้",
    "fil" to "Hindi ma-scan ang musika mo.",
    "he" to "לא ניתן היה לסרוק את המוזיקה שלך."
)

internal fun localScanFailedLocalization(code: String): String =
    localScanFailedMessages[code] ?: localScanFailedMessages.getValue("en")

internal fun localLibraryLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(localLibraryBundles, code)

internal fun localLibraryLocalizationCodes(): Set<String> = supportedLocalizationCodes()
