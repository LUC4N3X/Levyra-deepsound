package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import java.util.Locale

internal class LevyraStoryPostStrings(val code: String, private val values: List<String>) {
    val tab get() = values[0]
    val importFolder get() = values[1]
    val importZip get() = values[2]
    val emptyDetail get() = values[3]
    val previewTitle get() = values[4]
    val video get() = values[6]
    val music get() = values[7]
    val caption get() = values[8]
    val noCaption get() = values[9]
    val reading get() = values[10]
    val saving get() = values[11]
    val deleteTitle get() = values[12]
    val deleteBody get() = values[13]
    val videoFile get() = values[15]
    val errorTitle get() = values[16]

    fun photos(count: Int) = values[5].replace("{n}", number(count))
    fun photoPosition(index: Int, total: Int) =
        values[14].replace("{n}", number(index)).replace("{total}", number(total))
    fun error(ordinal: Int): String = values[ERROR_OFFSET + ordinal.coerceIn(0, ERROR_COUNT - 1)]

    private fun number(value: Int): String = isolate(String.format(Locale.forLanguageTag(code), "%d", value))

    private fun isolate(value: String): String =
        if (LevyraLanguageCatalog.isRtl(code) && value.isNotBlank()) "⁨$value⁩" else value

    companion object {
        const val KEY_COUNT = 24
        const val ERROR_OFFSET = 17
        const val ERROR_COUNT = 7
    }
}

internal object LevyraStoryPostCatalog {
    private val brazilianPortuguese = listOf(
        "Publicações", "Importar pasta", "Importar ZIP",
        "Recupere publicações salvas: uma pasta ou ZIP com fotos, legenda e a música delas, ou um vídeo.",
        "Prévia da publicação", "{n} fotos", "Vídeo", "Música", "Legenda", "Sem legenda", "Lendo arquivos…", "Salvando…",
        "Excluir esta publicação?", "O Levyra remove a cópia dele. Seus arquivos originais continuam intactos.",
        "Foto {n} de {total}", "Arquivo de vídeo", "Não foi possível importar a publicação",
        "Não dá para abrir este ZIP.", "Este ZIP tem caminhos inseguros, então nada foi extraído.",
        "A publicação é grande demais.", "Nenhuma foto com música nem vídeo foi encontrado.",
        "Há fotos, mas nenhum arquivo de música.", "O Levyra não consegue ler esses arquivos.", "Não há espaço livre suficiente."
    )

    private val values: Map<String, List<String>> = mapOf(
        "en" to listOf(
            "Posts", "Import folder", "Import ZIP",
            "Bring back saved posts: a folder or ZIP with photos, a caption and their music, or a video.",
            "Post preview", "{n} photos", "Video", "Music", "Caption", "No caption", "Reading files…", "Saving…",
            "Delete this post?", "Levyra removes its saved copy. Your original files stay untouched.",
            "Photo {n} of {total}", "Video file", "Couldn’t import this post",
            "This ZIP can’t be opened.", "This ZIP contains unsafe paths, so nothing was extracted.",
            "This post is too large to import.", "No photos with music, and no video, were found.",
            "Photos found, but no music file.", "Levyra couldn’t read these files.", "Not enough free space."
        ),
        "it" to listOf(
            "Post", "Importa cartella", "Importa ZIP",
            "Ritrova i post salvati: una cartella o uno ZIP con foto, didascalia e la loro musica, oppure un video.",
            "Anteprima del post", "{n} foto", "Video", "Musica", "Didascalia", "Nessuna didascalia", "Lettura dei file…", "Salvataggio…",
            "Eliminare questo post?", "Levyra rimuove la sua copia. I file originali restano intatti.",
            "Foto {n} di {total}", "File video", "Impossibile importare il post",
            "Questo ZIP non si può aprire.", "Questo ZIP contiene percorsi non sicuri: non è stato estratto nulla.",
            "Il post è troppo grande da importare.", "Non ci sono foto con musica né un video.",
            "Ci sono foto, ma nessun file musicale.", "Levyra non riesce a leggere questi file.", "Spazio libero insufficiente."
        ),
        "es" to listOf(
            "Publicaciones", "Importar carpeta", "Importar ZIP",
            "Recupera publicaciones guardadas: una carpeta o un ZIP con fotos, descripción y su música, o un vídeo.",
            "Vista previa", "{n} fotos", "Vídeo", "Música", "Descripción", "Sin descripción", "Leyendo archivos…", "Guardando…",
            "¿Eliminar esta publicación?", "Levyra borra su copia. Tus archivos originales no se tocan.",
            "Foto {n} de {total}", "Archivo de vídeo", "No se pudo importar la publicación",
            "Este ZIP no se puede abrir.", "Este ZIP contiene rutas no seguras, así que no se extrajo nada.",
            "La publicación es demasiado grande.", "No se encontraron fotos con música ni un vídeo.",
            "Hay fotos, pero ningún archivo de música.", "Levyra no puede leer estos archivos.", "No hay suficiente espacio libre."
        ),
        "fr" to listOf(
            "Publications", "Importer un dossier", "Importer un ZIP",
            "Retrouvez vos publications : un dossier ou un ZIP avec photos, légende et musique, ou une vidéo.",
            "Aperçu de la publication", "{n} photos", "Vidéo", "Musique", "Légende", "Aucune légende", "Lecture des fichiers…", "Enregistrement…",
            "Supprimer cette publication ?", "Levyra supprime sa copie. Vos fichiers d’origine restent intacts.",
            "Photo {n} sur {total}", "Fichier vidéo", "Impossible d’importer la publication",
            "Ce ZIP ne peut pas être ouvert.", "Ce ZIP contient des chemins dangereux : rien n’a été extrait.",
            "Cette publication est trop volumineuse.", "Aucune photo avec musique ni vidéo trouvée.",
            "Des photos, mais aucun fichier audio.", "Levyra ne peut pas lire ces fichiers.", "Espace libre insuffisant."
        ),
        "de" to listOf(
            "Beiträge", "Ordner importieren", "ZIP importieren",
            "Gespeicherte Beiträge zurückholen: ein Ordner oder ZIP mit Fotos, Text und Musik oder ein Video.",
            "Vorschau", "{n} Fotos", "Video", "Musik", "Beschreibung", "Keine Beschreibung", "Dateien werden gelesen…", "Wird gespeichert…",
            "Diesen Beitrag löschen?", "Levyra entfernt seine Kopie. Deine Originaldateien bleiben unverändert.",
            "Foto {n} von {total}", "Videodatei", "Beitrag konnte nicht importiert werden",
            "Dieses ZIP lässt sich nicht öffnen.", "Dieses ZIP enthält unsichere Pfade, daher wurde nichts entpackt.",
            "Der Beitrag ist zu groß.", "Keine Fotos mit Musik und kein Video gefunden.",
            "Fotos gefunden, aber keine Musikdatei.", "Levyra kann diese Dateien nicht lesen.", "Nicht genug freier Speicher."
        ),
        "pt" to listOf(
            "Publicações", "Importar pasta", "Importar ZIP",
            "Recupera publicações guardadas: uma pasta ou ZIP com fotos, legenda e a sua música, ou um vídeo.",
            "Pré-visualização", "{n} fotos", "Vídeo", "Música", "Legenda", "Sem legenda", "A ler ficheiros…", "A guardar…",
            "Eliminar esta publicação?", "O Levyra remove a sua cópia. Os ficheiros originais ficam intactos.",
            "Foto {n} de {total}", "Ficheiro de vídeo", "Não foi possível importar a publicação",
            "Este ZIP não pode ser aberto.", "Este ZIP contém caminhos inseguros, por isso nada foi extraído.",
            "A publicação é demasiado grande.", "Não foram encontradas fotos com música nem um vídeo.",
            "Há fotos, mas nenhum ficheiro de música.", "O Levyra não consegue ler estes ficheiros.", "Espaço livre insuficiente."
        ),
        "nl" to listOf(
            "Posts", "Map importeren", "ZIP importeren",
            "Haal opgeslagen posts terug: een map of ZIP met foto’s, bijschrift en muziek, of een video.",
            "Voorbeeld", "{n} foto’s", "Video", "Muziek", "Bijschrift", "Geen bijschrift", "Bestanden lezen…", "Opslaan…",
            "Deze post verwijderen?", "Levyra verwijdert zijn kopie. Je originele bestanden blijven ongewijzigd.",
            "Foto {n} van {total}", "Videobestand", "Post kon niet worden geïmporteerd",
            "Deze ZIP kan niet worden geopend.", "Deze ZIP bevat onveilige paden, dus er is niets uitgepakt.",
            "Deze post is te groot.", "Geen foto’s met muziek en geen video gevonden.",
            "Foto’s gevonden, maar geen muziekbestand.", "Levyra kan deze bestanden niet lezen.", "Niet genoeg vrije ruimte."
        ),
        "pl" to listOf(
            "Posty", "Importuj folder", "Importuj ZIP",
            "Odzyskaj zapisane posty: folder lub ZIP ze zdjęciami, opisem i muzyką albo wideo.",
            "Podgląd posta", "Zdjęcia: {n}", "Wideo", "Muzyka", "Opis", "Brak opisu", "Odczytywanie plików…", "Zapisywanie…",
            "Usunąć ten post?", "Levyra usunie swoją kopię. Oryginalne pliki pozostaną nietknięte.",
            "Zdjęcie {n} z {total}", "Plik wideo", "Nie udało się zaimportować posta",
            "Nie można otworzyć tego pliku ZIP.", "Ten ZIP zawiera niebezpieczne ścieżki, więc nic nie rozpakowano.",
            "Post jest zbyt duży.", "Nie znaleziono zdjęć z muzyką ani wideo.",
            "Są zdjęcia, ale brak pliku muzycznego.", "Levyra nie może odczytać tych plików.", "Za mało wolnego miejsca."
        ),
        "ro" to listOf(
            "Postări", "Importă dosar", "Importă ZIP",
            "Recuperează postările salvate: un dosar sau ZIP cu fotografii, descriere și muzica lor, ori un videoclip.",
            "Previzualizare", "{n} fotografii", "Video", "Muzică", "Descriere", "Fără descriere", "Se citesc fișierele…", "Se salvează…",
            "Ștergi această postare?", "Levyra șterge copia sa. Fișierele originale rămân neatinse.",
            "Fotografia {n} din {total}", "Fișier video", "Postarea nu a putut fi importată",
            "Acest ZIP nu poate fi deschis.", "Acest ZIP conține căi nesigure, așa că nu s-a extras nimic.",
            "Postarea este prea mare.", "Nu s-au găsit fotografii cu muzică sau un videoclip.",
            "Există fotografii, dar niciun fișier audio.", "Levyra nu poate citi aceste fișiere.", "Spațiu liber insuficient."
        ),
        "el" to listOf(
            "Αναρτήσεις", "Εισαγωγή φακέλου", "Εισαγωγή ZIP",
            "Επανάφερε αποθηκευμένες αναρτήσεις: φάκελο ή ZIP με φωτογραφίες, λεζάντα και μουσική ή ένα βίντεο.",
            "Προεπισκόπηση", "{n} φωτογραφίες", "Βίντεο", "Μουσική", "Λεζάντα", "Χωρίς λεζάντα", "Ανάγνωση αρχείων…", "Αποθήκευση…",
            "Διαγραφή της ανάρτησης;", "Το Levyra αφαιρεί το αντίγραφό του. Τα αρχικά αρχεία μένουν ανέπαφα.",
            "Φωτογραφία {n} από {total}", "Αρχείο βίντεο", "Η ανάρτηση δεν εισήχθη",
            "Αυτό το ZIP δεν ανοίγει.", "Αυτό το ZIP έχει μη ασφαλείς διαδρομές, οπότε δεν εξήχθη τίποτα.",
            "Η ανάρτηση είναι πολύ μεγάλη.", "Δεν βρέθηκαν φωτογραφίες με μουσική ούτε βίντεο.",
            "Υπάρχουν φωτογραφίες, αλλά όχι αρχείο μουσικής.", "Το Levyra δεν μπορεί να διαβάσει αυτά τα αρχεία.", "Δεν υπάρχει αρκετός χώρος."
        ),
        "sv" to listOf(
            "Inlägg", "Importera mapp", "Importera ZIP",
            "Ta tillbaka sparade inlägg: en mapp eller ZIP med foton, bildtext och musik, eller en video.",
            "Förhandsvisning", "{n} foton", "Video", "Musik", "Bildtext", "Ingen bildtext", "Läser filer…", "Sparar…",
            "Radera inlägget?", "Levyra tar bort sin kopia. Dina originalfiler lämnas orörda.",
            "Foto {n} av {total}", "Videofil", "Inlägget kunde inte importeras",
            "Den här ZIP-filen kan inte öppnas.", "ZIP-filen innehåller osäkra sökvägar, så inget packades upp.",
            "Inlägget är för stort.", "Inga foton med musik och ingen video hittades.",
            "Foton finns, men ingen musikfil.", "Levyra kan inte läsa filerna.", "Inte tillräckligt med ledigt utrymme."
        ),
        "da" to listOf(
            "Opslag", "Importér mappe", "Importér ZIP",
            "Hent gemte opslag frem: en mappe eller ZIP med fotos, billedtekst og musik eller en video.",
            "Forhåndsvisning", "{n} fotos", "Video", "Musik", "Billedtekst", "Ingen billedtekst", "Læser filer…", "Gemmer…",
            "Slet dette opslag?", "Levyra fjerner sin kopi. Dine originale filer forbliver urørte.",
            "Foto {n} af {total}", "Videofil", "Opslaget kunne ikke importeres",
            "Denne ZIP kan ikke åbnes.", "Denne ZIP indeholder usikre stier, så intet blev udpakket.",
            "Opslaget er for stort.", "Ingen fotos med musik og ingen video fundet.",
            "Der er fotos, men ingen musikfil.", "Levyra kan ikke læse filerne.", "Ikke nok ledig plads."
        ),
        "cs" to listOf(
            "Příspěvky", "Importovat složku", "Importovat ZIP",
            "Vraťte se k uloženým příspěvkům: složka nebo ZIP s fotkami, popiskem a hudbou, nebo video.",
            "Náhled příspěvku", "Fotky: {n}", "Video", "Hudba", "Popisek", "Bez popisku", "Čtení souborů…", "Ukládání…",
            "Smazat tento příspěvek?", "Levyra odstraní svou kopii. Původní soubory zůstanou nedotčené.",
            "Fotka {n} z {total}", "Videosoubor", "Příspěvek se nepodařilo importovat",
            "Tento ZIP nelze otevřít.", "Tento ZIP obsahuje nebezpečné cesty, proto se nic nerozbalilo.",
            "Příspěvek je příliš velký.", "Nebyly nalezeny fotky s hudbou ani video.",
            "Fotky jsou, ale chybí hudební soubor.", "Levyra tyto soubory nedokáže přečíst.", "Nedostatek volného místa."
        ),
        "sk" to listOf(
            "Príspevky", "Importovať priečinok", "Importovať ZIP",
            "Vráťte sa k uloženým príspevkom: priečinok alebo ZIP s fotkami, popisom a hudbou, alebo video.",
            "Náhľad príspevku", "Fotky: {n}", "Video", "Hudba", "Popis", "Bez popisu", "Čítanie súborov…", "Ukladá sa…",
            "Odstrániť tento príspevok?", "Levyra odstráni svoju kópiu. Pôvodné súbory zostanú nedotknuté.",
            "Fotka {n} z {total}", "Videosúbor", "Príspevok sa nepodarilo importovať",
            "Tento ZIP sa nedá otvoriť.", "Tento ZIP obsahuje nebezpečné cesty, preto sa nič nerozbalilo.",
            "Príspevok je príliš veľký.", "Nenašli sa fotky s hudbou ani video.",
            "Fotky sú, ale chýba hudobný súbor.", "Levyra tieto súbory nedokáže prečítať.", "Nedostatok voľného miesta."
        ),
        "bn" to listOf(
            "পোস্ট", "ফোল্ডার ইমপোর্ট", "ZIP ইমপোর্ট",
            "সংরক্ষিত পোস্ট ফিরিয়ে আনুন: ছবি, ক্যাপশন ও গানসহ একটি ফোল্ডার বা ZIP, অথবা একটি ভিডিও।",
            "পোস্টের প্রিভিউ", "{n}টি ছবি", "ভিডিও", "গান", "ক্যাপশন", "ক্যাপশন নেই", "ফাইল পড়া হচ্ছে…", "সংরক্ষণ হচ্ছে…",
            "এই পোস্ট মুছবেন?", "Levyra শুধু নিজের কপি সরাবে। আপনার মূল ফাইল অক্ষত থাকবে।",
            "ছবি {n}/{total}", "ভিডিও ফাইল", "পোস্ট ইমপোর্ট করা যায়নি",
            "এই ZIP খোলা যাচ্ছে না।", "এই ZIP-এ অনিরাপদ পাথ আছে, তাই কিছুই বের করা হয়নি।",
            "পোস্টটি ইমপোর্টের জন্য খুব বড়।", "গানসহ ছবি বা কোনো ভিডিও পাওয়া যায়নি।",
            "ছবি আছে, কিন্তু কোনো গানের ফাইল নেই।", "Levyra এই ফাইলগুলো পড়তে পারছে না।", "যথেষ্ট খালি জায়গা নেই।"
        ),
        "kk" to listOf(
            "Жазбалар", "Қалтаны импорттау", "ZIP импорттау",
            "Сақталған жазбаларды қайтарыңыз: суреттері, сипаттамасы мен әуені бар қалта не ZIP немесе бейне.",
            "Жазбаны алдын ала қарау", "{n} сурет", "Бейне", "Әуен", "Сипаттама", "Сипаттама жоқ", "Файлдар оқылуда…", "Сақталуда…",
            "Бұл жазбаны жою керек пе?", "Levyra тек өз көшірмесін жояды. Түпнұсқа файлдар өзгермейді.",
            "{total} суреттің {n}-сі", "Бейне файл", "Жазбаны импорттау мүмкін болмады",
            "Бұл ZIP ашылмайды.", "Бұл ZIP ішінде қауіпті жолдар бар, сондықтан ештеңе шығарылмады.",
            "Жазба тым үлкен.", "Әуені бар суреттер де, бейне де табылмады.",
            "Суреттер бар, бірақ әуен файлы жоқ.", "Levyra бұл файлдарды оқи алмайды.", "Бос орын жеткіліксіз."
        ),
        "sr" to listOf(
            "Објаве", "Увези фасциклу", "Увези ZIP",
            "Врати сачуване објаве: фасцикла или ZIP са фотографијама, описом и музиком, или видео.",
            "Преглед објаве", "Фотографија: {n}", "Видео", "Музика", "Опис", "Без описа", "Читање датотека…", "Чување…",
            "Обрисати ову објаву?", "Levyra брише своју копију. Оригиналне датотеке остају нетакнуте.",
            "Фотографија {n} од {total}", "Видео датотека", "Објава није увезена",
            "Овај ZIP не може да се отвори.", "Овај ZIP садржи небезбедне путање, па ништа није распаковано.",
            "Објава је превелика.", "Нису пронађене фотографије са музиком ни видео.",
            "Има фотографија, али нема музичке датотеке.", "Levyra не може да прочита ове датотеке.", "Нема довољно слободног простора."
        ),
        "sq" to listOf(
            "Postime", "Importo dosje", "Importo ZIP",
            "Rikthe postimet e ruajtura: një dosje ose ZIP me foto, përshkrim dhe muzikën e tyre, ose një video.",
            "Pamje paraprake", "{n} foto", "Video", "Muzikë", "Përshkrimi", "Pa përshkrim", "Po lexohen skedarët…", "Po ruhet…",
            "Ta fshish këtë postim?", "Levyra heq kopjen e vet. Skedarët origjinalë mbeten të paprekur.",
            "Foto {n} nga {total}", "Skedar video", "Postimi nuk u importua",
            "Ky ZIP nuk mund të hapet.", "Ky ZIP ka shtigje të pasigurta, prandaj nuk u nxor asgjë.",
            "Postimi është shumë i madh.", "Nuk u gjetën foto me muzikë apo video.",
            "Ka foto, por asnjë skedar muzike.", "Levyra nuk i lexon dot këta skedarë.", "Nuk ka hapësirë të mjaftueshme."
        ),
        "hr" to listOf(
            "Objave", "Uvezi mapu", "Uvezi ZIP",
            "Vrati spremljene objave: mapa ili ZIP s fotografijama, opisom i glazbom, ili videozapis.",
            "Pregled objave", "Fotografija: {n}", "Video", "Glazba", "Opis", "Bez opisa", "Čitanje datoteka…", "Spremanje…",
            "Izbrisati ovu objavu?", "Levyra briše svoju kopiju. Izvorne datoteke ostaju netaknute.",
            "Fotografija {n} od {total}", "Videodatoteka", "Objava nije uvezena",
            "Ovaj ZIP ne može se otvoriti.", "Ovaj ZIP sadrži nesigurne putanje pa ništa nije raspakirano.",
            "Objava je prevelika.", "Nisu pronađene fotografije s glazbom ni videozapis.",
            "Ima fotografija, ali nema glazbene datoteke.", "Levyra ne može pročitati ove datoteke.", "Nema dovoljno slobodnog prostora."
        ),
        "bg" to listOf(
            "Публикации", "Импорт на папка", "Импорт на ZIP",
            "Върнете запазени публикации: папка или ZIP със снимки, описание и музика, или видео.",
            "Преглед", "{n} снимки", "Видео", "Музика", "Описание", "Без описание", "Четене на файлове…", "Запазване…",
            "Да се изтрие ли публикацията?", "Levyra премахва своето копие. Оригиналните файлове остават непокътнати.",
            "Снимка {n} от {total}", "Видеофайл", "Публикацията не беше импортирана",
            "Този ZIP не може да се отвори.", "Този ZIP съдържа опасни пътища, затова нищо не е извлечено.",
            "Публикацията е твърде голяма.", "Няма снимки с музика, нито видео.",
            "Има снимки, но няма музикален файл.", "Levyra не може да прочете тези файлове.", "Няма достатъчно свободно място."
        ),
        "hu" to listOf(
            "Bejegyzések", "Mappa importálása", "ZIP importálása",
            "Hozd vissza a mentett bejegyzéseket: mappa vagy ZIP fotókkal, leírással és zenével, vagy egy videó.",
            "Előnézet", "{n} fotó", "Videó", "Zene", "Leírás", "Nincs leírás", "Fájlok olvasása…", "Mentés…",
            "Törlöd ezt a bejegyzést?", "A Levyra törli a saját másolatát. Az eredeti fájlok érintetlenek maradnak.",
            "{n}. fotó / {total}", "Videofájl", "A bejegyzést nem sikerült importálni",
            "Ez a ZIP nem nyitható meg.", "Ez a ZIP nem biztonságos útvonalakat tartalmaz, ezért semmi sem lett kicsomagolva.",
            "A bejegyzés túl nagy.", "Nincs fotó zenével, és videó sem.",
            "Vannak fotók, de nincs zenefájl.", "A Levyra nem tudja olvasni ezeket a fájlokat.", "Nincs elég szabad hely."
        ),
        "fi" to listOf(
            "Julkaisut", "Tuo kansio", "Tuo ZIP",
            "Palauta tallennetut julkaisut: kansio tai ZIP, jossa on kuvat, kuvateksti ja musiikki, tai video.",
            "Esikatselu", "{n} kuvaa", "Video", "Musiikki", "Kuvateksti", "Ei kuvatekstiä", "Luetaan tiedostoja…", "Tallennetaan…",
            "Poistetaanko julkaisu?", "Levyra poistaa oman kopionsa. Alkuperäiset tiedostot säilyvät ennallaan.",
            "Kuva {n}/{total}", "Videotiedosto", "Julkaisua ei voitu tuoda",
            "Tätä ZIP-tiedostoa ei voi avata.", "ZIP sisältää vaarallisia polkuja, joten mitään ei purettu.",
            "Julkaisu on liian suuri.", "Kuvia musiikin kanssa tai videota ei löytynyt.",
            "Kuvia löytyi, mutta ei musiikkitiedostoa.", "Levyra ei pysty lukemaan näitä tiedostoja.", "Vapaata tilaa ei ole tarpeeksi."
        ),
        "et" to listOf(
            "Postitused", "Impordi kaust", "Impordi ZIP",
            "Too salvestatud postitused tagasi: kaust või ZIP fotode, kirjelduse ja muusikaga või video.",
            "Eelvaade", "{n} fotot", "Video", "Muusika", "Kirjeldus", "Kirjeldus puudub", "Failide lugemine…", "Salvestamine…",
            "Kas kustutada see postitus?", "Levyra eemaldab oma koopia. Algsed failid jäävad puutumata.",
            "Foto {n}/{total}", "Videofail", "Postitust ei õnnestunud importida",
            "Seda ZIP-faili ei saa avada.", "ZIP sisaldab ohtlikke teid, seega midagi ei pakitud lahti.",
            "Postitus on liiga suur.", "Muusikaga fotosid ega videot ei leitud.",
            "Fotod on olemas, kuid muusikafail puudub.", "Levyra ei saa neid faile lugeda.", "Vaba ruumi pole piisavalt."
        ),
        "nb" to listOf(
            "Innlegg", "Importer mappe", "Importer ZIP",
            "Hent frem lagrede innlegg: en mappe eller ZIP med bilder, bildetekst og musikk, eller en video.",
            "Forhåndsvisning", "{n} bilder", "Video", "Musikk", "Bildetekst", "Ingen bildetekst", "Leser filer…", "Lagrer…",
            "Slette dette innlegget?", "Levyra fjerner sin kopi. Originalfilene dine forblir urørt.",
            "Bilde {n} av {total}", "Videofil", "Kunne ikke importere innlegget",
            "Denne ZIP-filen kan ikke åpnes.", "ZIP-filen inneholder usikre stier, så ingenting ble pakket ut.",
            "Innlegget er for stort.", "Fant ingen bilder med musikk og ingen video.",
            "Fant bilder, men ingen musikkfil.", "Levyra kan ikke lese filene.", "Ikke nok ledig plass."
        ),
        "ca" to listOf(
            "Publicacions", "Importa una carpeta", "Importa un ZIP",
            "Recupera publicacions desades: una carpeta o un ZIP amb fotos, descripció i la seva música, o un vídeo.",
            "Previsualització", "{n} fotos", "Vídeo", "Música", "Descripció", "Sense descripció", "S’estan llegint els fitxers…", "S’està desant…",
            "Vols suprimir aquesta publicació?", "Levyra elimina la seva còpia. Els fitxers originals queden intactes.",
            "Foto {n} de {total}", "Fitxer de vídeo", "No s’ha pogut importar la publicació",
            "Aquest ZIP no es pot obrir.", "Aquest ZIP conté camins no segurs, així que no s’ha extret res.",
            "La publicació és massa gran.", "No s’han trobat fotos amb música ni cap vídeo.",
            "Hi ha fotos, però cap fitxer de música.", "Levyra no pot llegir aquests fitxers.", "No hi ha prou espai lliure."
        ),
        "uk" to listOf(
            "Дописи", "Імпортувати теку", "Імпортувати ZIP",
            "Поверніть збережені дописи: теку або ZIP зі світлинами, підписом і музикою або відео.",
            "Попередній перегляд", "Світлин: {n}", "Відео", "Музика", "Підпис", "Без підпису", "Читання файлів…", "Збереження…",
            "Видалити цей допис?", "Levyra видалить свою копію. Оригінальні файли залишаться недоторканими.",
            "Світлина {n} з {total}", "Відеофайл", "Не вдалося імпортувати допис",
            "Цей ZIP не вдається відкрити.", "Цей ZIP містить небезпечні шляхи, тому нічого не розпаковано.",
            "Допис завеликий.", "Не знайдено світлин із музикою чи відео.",
            "Світлини є, але немає музичного файлу.", "Levyra не може прочитати ці файли.", "Недостатньо вільного місця."
        ),
        "ru" to listOf(
            "Посты", "Импорт папки", "Импорт ZIP",
            "Верните сохранённые посты: папку или ZIP с фото, подписью и музыкой либо видео.",
            "Предпросмотр", "Фото: {n}", "Видео", "Музыка", "Подпись", "Без подписи", "Чтение файлов…", "Сохранение…",
            "Удалить этот пост?", "Levyra удалит свою копию. Исходные файлы останутся нетронутыми.",
            "Фото {n} из {total}", "Видеофайл", "Не удалось импортировать пост",
            "Этот ZIP не открывается.", "В этом ZIP есть небезопасные пути, поэтому ничего не извлечено.",
            "Пост слишком большой.", "Не найдено ни фото с музыкой, ни видео.",
            "Фото есть, но нет музыкального файла.", "Levyra не может прочитать эти файлы.", "Недостаточно свободного места."
        ),
        "tr" to listOf(
            "Gönderiler", "Klasör içe aktar", "ZIP içe aktar",
            "Kayıtlı gönderilerini geri getir: fotoğraf, açıklama ve müziği olan bir klasör ya da ZIP veya bir video.",
            "Önizleme", "{n} fotoğraf", "Video", "Müzik", "Açıklama", "Açıklama yok", "Dosyalar okunuyor…", "Kaydediliyor…",
            "Bu gönderi silinsin mi?", "Levyra kendi kopyasını kaldırır. Orijinal dosyaların olduğu gibi kalır.",
            "Fotoğraf {n}/{total}", "Video dosyası", "Gönderi içe aktarılamadı",
            "Bu ZIP açılamıyor.", "Bu ZIP güvenli olmayan yollar içeriyor, bu yüzden hiçbir şey çıkarılmadı.",
            "Gönderi çok büyük.", "Müzikli fotoğraf veya video bulunamadı.",
            "Fotoğraflar var ama müzik dosyası yok.", "Levyra bu dosyaları okuyamıyor.", "Yeterli boş alan yok."
        ),
        "ar" to listOf(
            "المنشورات", "استيراد مجلد", "استيراد ZIP",
            "استعد المنشورات المحفوظة: مجلد أو ملف ZIP يضم صورًا ووصفًا وموسيقاها، أو فيديو.",
            "معاينة المنشور", "{n} صور", "فيديو", "الموسيقى", "الوصف", "بلا وصف", "جارٍ قراءة الملفات…", "جارٍ الحفظ…",
            "حذف هذا المنشور؟", "يحذف Levyra نسخته فقط. تبقى ملفاتك الأصلية كما هي.",
            "الصورة {n} من {total}", "ملف فيديو", "تعذّر استيراد المنشور",
            "لا يمكن فتح ملف ZIP هذا.", "يحتوي ملف ZIP على مسارات غير آمنة، لذا لم يُستخرج شيء.",
            "المنشور كبير جدًا.", "لم يُعثر على صور مع موسيقى ولا على فيديو.",
            "توجد صور، لكن لا يوجد ملف موسيقى.", "لا يستطيع Levyra قراءة هذه الملفات.", "لا توجد مساحة كافية."
        ),
        "fa" to listOf(
            "پست‌ها", "وارد کردن پوشه", "وارد کردن ZIP",
            "پست‌های ذخیره‌شده را برگردانید: پوشه یا ZIP با عکس، توضیح و موسیقی، یا یک ویدیو.",
            "پیش‌نمایش پست", "{n} عکس", "ویدیو", "موسیقی", "توضیح", "بدون توضیح", "در حال خواندن فایل‌ها…", "در حال ذخیره…",
            "این پست حذف شود؟", "Levyra فقط نسخهٔ خودش را حذف می‌کند. فایل‌های اصلی دست‌نخورده می‌مانند.",
            "عکس {n} از {total}", "فایل ویدیو", "وارد کردن پست ممکن نشد",
            "این ZIP باز نمی‌شود.", "این ZIP مسیرهای ناامن دارد، برای همین چیزی استخراج نشد.",
            "این پست خیلی بزرگ است.", "عکس همراه موسیقی یا ویدیویی پیدا نشد.",
            "عکس هست، اما فایل موسیقی نیست.", "Levyra نمی‌تواند این فایل‌ها را بخواند.", "فضای خالی کافی نیست."
        ),
        "zh" to listOf(
            "图文", "导入文件夹", "导入 ZIP",
            "找回已保存的作品：包含图片、文案和配乐的文件夹或 ZIP，或者一段视频。",
            "作品预览", "{n} 张图片", "视频", "配乐", "文案", "无文案", "正在读取文件…", "正在保存…",
            "删除这条作品？", "Levyra 只会删除自己的副本，原始文件保持不变。",
            "第 {n} 张，共 {total} 张", "视频文件", "无法导入这条作品",
            "无法打开这个 ZIP。", "这个 ZIP 含有不安全的路径，未解压任何内容。",
            "作品太大，无法导入。", "没有找到带配乐的图片，也没有视频。",
            "找到了图片，但没有音乐文件。", "Levyra 无法读取这些文件。", "可用空间不足。"
        ),
        "zh-Hant" to listOf(
            "圖文", "匯入資料夾", "匯入 ZIP",
            "找回已儲存的作品：包含圖片、文案和配樂的資料夾或 ZIP，或者一段影片。",
            "作品預覽", "{n} 張圖片", "影片", "配樂", "文案", "沒有文案", "正在讀取檔案…", "正在儲存…",
            "刪除這則作品？", "Levyra 只會刪除自己的副本，原始檔案保持不變。",
            "第 {n} 張，共 {total} 張", "影片檔案", "無法匯入這則作品",
            "無法開啟這個 ZIP。", "這個 ZIP 含有不安全的路徑，未解壓任何內容。",
            "作品太大，無法匯入。", "找不到附配樂的圖片，也沒有影片。",
            "找到了圖片，但沒有音樂檔案。", "Levyra 無法讀取這些檔案。", "可用空間不足。"
        ),
        "ja" to listOf(
            "投稿", "フォルダを読み込む", "ZIP を読み込む",
            "保存した投稿をもう一度：写真・キャプション・音楽が入ったフォルダや ZIP、または動画。",
            "投稿のプレビュー", "写真 {n} 枚", "動画", "音楽", "キャプション", "キャプションなし", "ファイルを読み込み中…", "保存中…",
            "この投稿を削除しますか？", "Levyra は自分のコピーだけを削除します。元のファイルはそのままです。",
            "写真 {n}/{total}", "動画ファイル", "投稿を読み込めませんでした",
            "この ZIP は開けません。", "この ZIP には安全でないパスがあるため、何も展開していません。",
            "投稿が大きすぎます。", "音楽付きの写真も動画も見つかりません。",
            "写真はありますが、音楽ファイルがありません。", "Levyra はこれらのファイルを読み込めません。", "空き容量が足りません。"
        ),
        "ko" to listOf(
            "게시물", "폴더 가져오기", "ZIP 가져오기",
            "저장한 게시물을 다시 즐기세요: 사진, 캡션, 음악이 담긴 폴더나 ZIP 또는 동영상.",
            "게시물 미리보기", "사진 {n}장", "동영상", "음악", "캡션", "캡션 없음", "파일 읽는 중…", "저장 중…",
            "이 게시물을 삭제할까요?", "Levyra는 자체 사본만 삭제합니다. 원본 파일은 그대로 남습니다.",
            "사진 {n}/{total}", "동영상 파일", "게시물을 가져오지 못했습니다",
            "이 ZIP을 열 수 없습니다.", "이 ZIP에 안전하지 않은 경로가 있어 아무것도 풀지 않았습니다.",
            "게시물이 너무 큽니다.", "음악이 있는 사진이나 동영상을 찾지 못했습니다.",
            "사진은 있지만 음악 파일이 없습니다.", "Levyra가 이 파일을 읽을 수 없습니다.", "여유 공간이 부족합니다."
        ),
        "hi" to listOf(
            "पोस्ट", "फ़ोल्डर इंपोर्ट करें", "ZIP इंपोर्ट करें",
            "सहेजी गई पोस्ट वापस लाएँ: फ़ोटो, कैप्शन और संगीत वाला फ़ोल्डर या ZIP, या कोई वीडियो।",
            "पोस्ट का प्रीव्यू", "{n} फ़ोटो", "वीडियो", "संगीत", "कैप्शन", "कोई कैप्शन नहीं", "फ़ाइलें पढ़ी जा रही हैं…", "सहेजा जा रहा है…",
            "यह पोस्ट हटाएँ?", "Levyra सिर्फ़ अपनी कॉपी हटाता है। आपकी मूल फ़ाइलें वैसी ही रहती हैं।",
            "फ़ोटो {n}/{total}", "वीडियो फ़ाइल", "पोस्ट इंपोर्ट नहीं हो सकी",
            "यह ZIP नहीं खुल रहा।", "इस ZIP में असुरक्षित पाथ हैं, इसलिए कुछ भी नहीं निकाला गया।",
            "पोस्ट बहुत बड़ी है।", "संगीत वाली फ़ोटो या कोई वीडियो नहीं मिला।",
            "फ़ोटो मिलीं, पर कोई संगीत फ़ाइल नहीं।", "Levyra इन फ़ाइलों को नहीं पढ़ पा रहा।", "पर्याप्त खाली जगह नहीं है।"
        ),
        "id" to listOf(
            "Postingan", "Impor folder", "Impor ZIP",
            "Hadirkan lagi postingan tersimpan: folder atau ZIP berisi foto, keterangan, dan musiknya, atau video.",
            "Pratinjau", "{n} foto", "Video", "Musik", "Keterangan", "Tanpa keterangan", "Membaca file…", "Menyimpan…",
            "Hapus postingan ini?", "Levyra menghapus salinannya. File aslimu tetap utuh.",
            "Foto {n} dari {total}", "File video", "Postingan tidak dapat diimpor",
            "ZIP ini tidak dapat dibuka.", "ZIP ini berisi jalur tidak aman, jadi tidak ada yang diekstrak.",
            "Postingan terlalu besar.", "Tidak ada foto bermusik maupun video.",
            "Ada foto, tetapi tidak ada file musik.", "Levyra tidak dapat membaca file ini.", "Ruang kosong tidak cukup."
        ),
        "ms" to listOf(
            "Siaran", "Import folder", "Import ZIP",
            "Kembalikan siaran yang disimpan: folder atau ZIP dengan foto, kapsyen dan muziknya, atau video.",
            "Pratonton", "{n} foto", "Video", "Muzik", "Kapsyen", "Tiada kapsyen", "Membaca fail…", "Menyimpan…",
            "Padam siaran ini?", "Levyra memadam salinannya sahaja. Fail asal anda kekal utuh.",
            "Foto {n} daripada {total}", "Fail video", "Siaran tidak dapat diimport",
            "ZIP ini tidak dapat dibuka.", "ZIP ini mengandungi laluan tidak selamat, jadi tiada apa diekstrak.",
            "Siaran terlalu besar.", "Tiada foto bermuzik atau video ditemui.",
            "Ada foto, tetapi tiada fail muzik.", "Levyra tidak dapat membaca fail ini.", "Ruang kosong tidak mencukupi."
        ),
        "vi" to listOf(
            "Bài đăng", "Nhập thư mục", "Nhập ZIP",
            "Mở lại bài đăng đã lưu: thư mục hoặc ZIP có ảnh, chú thích và nhạc, hoặc một video.",
            "Xem trước bài đăng", "{n} ảnh", "Video", "Nhạc", "Chú thích", "Không có chú thích", "Đang đọc tệp…", "Đang lưu…",
            "Xóa bài đăng này?", "Levyra chỉ xóa bản sao của mình. Tệp gốc vẫn được giữ nguyên.",
            "Ảnh {n}/{total}", "Tệp video", "Không thể nhập bài đăng",
            "Không mở được ZIP này.", "ZIP này chứa đường dẫn không an toàn nên không giải nén gì.",
            "Bài đăng quá lớn.", "Không tìm thấy ảnh kèm nhạc hoặc video.",
            "Có ảnh nhưng không có tệp nhạc.", "Levyra không đọc được các tệp này.", "Không đủ dung lượng trống."
        ),
        "th" to listOf(
            "โพสต์", "นำเข้าโฟลเดอร์", "นำเข้า ZIP",
            "เปิดโพสต์ที่บันทึกไว้อีกครั้ง: โฟลเดอร์หรือ ZIP ที่มีรูป คำบรรยาย และเพลง หรือวิดีโอ",
            "ตัวอย่างโพสต์", "{n} รูป", "วิดีโอ", "เพลง", "คำบรรยาย", "ไม่มีคำบรรยาย", "กำลังอ่านไฟล์…", "กำลังบันทึก…",
            "ลบโพสต์นี้ไหม", "Levyra จะลบเฉพาะสำเนาของตัวเอง ไฟล์ต้นฉบับยังอยู่ครบ",
            "รูปที่ {n} จาก {total}", "ไฟล์วิดีโอ", "นำเข้าโพสต์ไม่ได้",
            "เปิด ZIP นี้ไม่ได้", "ZIP นี้มีพาธที่ไม่ปลอดภัย จึงไม่ได้แตกไฟล์ใดเลย",
            "โพสต์ใหญ่เกินไป", "ไม่พบรูปที่มีเพลงหรือวิดีโอ",
            "มีรูป แต่ไม่มีไฟล์เพลง", "Levyra อ่านไฟล์เหล่านี้ไม่ได้", "พื้นที่ว่างไม่พอ"
        ),
        "fil" to listOf(
            "Mga post", "Mag-import ng folder", "Mag-import ng ZIP",
            "Ibalik ang mga naka-save na post: folder o ZIP na may mga larawan, caption at musika, o isang video.",
            "Preview ng post", "{n} larawan", "Video", "Musika", "Caption", "Walang caption", "Binabasa ang mga file…", "Sine-save…",
            "Burahin ang post na ito?", "Kopya lang ng Levyra ang mabubura. Buo pa rin ang mga orihinal mong file.",
            "Larawan {n} sa {total}", "Video file", "Hindi ma-import ang post",
            "Hindi mabuksan ang ZIP na ito.", "May mga hindi ligtas na path ang ZIP na ito kaya walang na-extract.",
            "Masyadong malaki ang post.", "Walang nakitang larawang may musika o video.",
            "May mga larawan pero walang music file.", "Hindi mabasa ng Levyra ang mga file na ito.", "Kulang ang libreng espasyo."
        ),
        "he" to listOf(
            "פוסטים", "ייבוא תיקייה", "ייבוא ZIP",
            "החזירו פוסטים שמורים: תיקייה או ZIP עם תמונות, כיתוב והמוזיקה שלהם, או סרטון.",
            "תצוגה מקדימה", "{n} תמונות", "סרטון", "מוזיקה", "כיתוב", "אין כיתוב", "קורא קבצים…", "שומר…",
            "למחוק את הפוסט?", "Levyra מוחק רק את העותק שלו. הקבצים המקוריים נשארים כמו שהם.",
            "תמונה {n} מתוך {total}", "קובץ וידאו", "לא ניתן לייבא את הפוסט",
            "אי אפשר לפתוח את קובץ ה-ZIP.", "ב-ZIP יש נתיבים לא בטוחים, ולכן לא חולץ דבר.",
            "הפוסט גדול מדי.", "לא נמצאו תמונות עם מוזיקה או סרטון.",
            "יש תמונות, אבל אין קובץ מוזיקה.", "Levyra לא מצליח לקרוא את הקבצים.", "אין מספיק מקום פנוי."
        )
    )

    init {
        val languageCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        check(values.keys == languageCodes)
        check(values.values.all { it.size == LevyraStoryPostStrings.KEY_COUNT })
        check(brazilianPortuguese.size == LevyraStoryPostStrings.KEY_COUNT)
    }

    fun forCode(code: String): LevyraStoryPostStrings {
        val normalized = LevyraLanguageCatalog.translationCode(code)
        val resolved = if (values.containsKey(normalized)) normalized else "en"
        return LevyraStoryPostStrings(resolved, values.getValue(resolved).forBrazil(code) { brazilianPortuguese })
    }

    internal fun codes(): Set<String> = values.keys

    internal fun rawValues(code: String): List<String> = values.getValue(code)

    internal fun brazilianValues(): List<String> = brazilianPortuguese
}
