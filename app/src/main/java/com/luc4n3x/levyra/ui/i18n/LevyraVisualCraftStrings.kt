package com.luc4n3x.levyra.ui.i18n

enum class VisualCraftText {
    ArtworkCrop, ArtworkCropSubtitle, CropCenter, CropSmart, CropSmartNoZoom,
    ShareStory, StoryTitle, StoryPreview, StoryEyebrow,
    TemplateAura, TemplateVinyl, TemplatePoster, TemplateGlass,
    ToneArtwork, ToneMidnight, ShowProgress, ShowBrand,
    ShareImage, SaveImage, Saved, Preparing, Failed,
    WallpaperColorsUnavailable,
    StartupAnimation, StartupAnimationSubtitle
}

class VisualCraftCopy internal constructor(private val texts: Map<VisualCraftText, String>) {
    internal val brazilian: VisualCraftCopy by lazy(LazyThreadSafetyMode.PUBLICATION) {
        VisualCraftCopy(texts.mapValues { (_, text) -> LevyraBrazilianPortuguese.adapt(text) })
    }

    operator fun get(key: VisualCraftText): String = texts[key] ?: englishVisualCraft.getValue(key)

    internal val complete: Boolean
        get() = texts.size == VisualCraftText.entries.size
}

private fun visualCraft(vararg value: String): Map<VisualCraftText, String> =
    if (value.size == VisualCraftText.entries.size) VisualCraftText.entries.zip(value).toMap() else emptyMap()

private val englishVisualCraft: Map<VisualCraftText, String> = visualCraft(
    "Cover cropping", "How covers that are not square fill square frames", "Center", "Smart with zoom", "Smart without zoom",
    "Share as story", "Share as a story", "Story preview", "Now playing",
    "Aura", "Vinyl", "Poster", "Glass",
    "Artwork colors", "Midnight", "Progress bar", "Levyra badge",
    "Share", "Save", "Saved to Pictures/Levyra", "Preparing…", "Couldn't create the story. Try again.",
    "Wallpaper colors are not available on this device. Levyra theme colors are used instead.",
    "Startup animation", "Animated Levyra logo when the app opens"
)

private val visualCraftCopies: Map<String, VisualCraftCopy> by lazy {
    mapOf(
        "en" to VisualCraftCopy(englishVisualCraft),
        "it" to VisualCraftCopy(visualCraft(
            "Ritaglio copertine", "Come le copertine non quadrate riempiono le cornici quadrate", "Centrale", "Smart con zoom", "Smart senza zoom",
            "Condividi come storia", "Condividi come storia", "Anteprima storia", "In riproduzione",
            "Aura", "Vinile", "Poster", "Vetro",
            "Colori copertina", "Mezzanotte", "Barra di avanzamento", "Badge Levyra",
            "Condividi", "Salva", "Salvata in Immagini/Levyra", "Preparazione…", "Impossibile creare la storia. Riprova.",
            "I colori dello sfondo non sono disponibili su questo dispositivo. Vengono usati i colori del tema Levyra.",
            "Animazione di avvio", "Logo Levyra animato all’apertura dell’app"
        )),
        "es" to VisualCraftCopy(visualCraft(
            "Recorte de portadas", "Cómo llenan los marcos cuadrados las portadas que no son cuadradas", "Centrado", "Inteligente con zoom", "Inteligente sin zoom",
            "Compartir como historia", "Compartir como historia", "Vista previa de la historia", "Reproduciendo",
            "Aura", "Vinilo", "Póster", "Cristal",
            "Colores de la portada", "Medianoche", "Barra de progreso", "Distintivo Levyra",
            "Compartir", "Guardar", "Guardada en Imágenes/Levyra", "Preparando…", "No se pudo crear la historia. Inténtalo de nuevo.",
            "Los colores del fondo de pantalla no están disponibles en este dispositivo. Se usan los colores del tema Levyra.",
            "Animación de inicio", "Logo de Levyra animado al abrir la app"
        )),
        "fr" to VisualCraftCopy(visualCraft(
            "Recadrage des pochettes", "Comment les pochettes non carrées remplissent les cadres carrés", "Centré", "Intelligent avec zoom", "Intelligent sans zoom",
            "Partager en story", "Partager en story", "Aperçu de la story", "En cours de lecture",
            "Aura", "Vinyle", "Affiche", "Verre",
            "Couleurs de la pochette", "Minuit", "Barre de progression", "Badge Levyra",
            "Partager", "Enregistrer", "Enregistrée dans Images/Levyra", "Préparation…", "Impossible de créer la story. Réessayez.",
            "Les couleurs du fond d’écran ne sont pas disponibles sur cet appareil. Les couleurs du thème Levyra sont utilisées.",
            "Animation de démarrage", "Logo Levyra animé à l’ouverture de l’app"
        )),
        "de" to VisualCraftCopy(visualCraft(
            "Cover-Zuschnitt", "Wie nicht quadratische Cover quadratische Rahmen füllen", "Mittig", "Smart mit Zoom", "Smart ohne Zoom",
            "Als Story teilen", "Als Story teilen", "Story-Vorschau", "Läuft gerade",
            "Aura", "Vinyl", "Poster", "Glas",
            "Coverfarben", "Mitternacht", "Fortschrittsbalken", "Levyra-Badge",
            "Teilen", "Speichern", "In Bilder/Levyra gespeichert", "Wird vorbereitet…", "Die Story konnte nicht erstellt werden. Versuche es erneut.",
            "Hintergrundfarben sind auf diesem Gerät nicht verfügbar. Es werden die Farben des Levyra-Themas verwendet.",
            "Startanimation", "Animiertes Levyra-Logo beim Öffnen der App"
        )),
        "pt" to VisualCraftCopy(visualCraft(
            "Recorte de capas", "Como as capas que não são quadradas preenchem molduras quadradas", "Central", "Inteligente com zoom", "Inteligente sem zoom",
            "Partilhar como story", "Partilhar como story", "Pré-visualização da story", "A reproduzir",
            "Aura", "Vinil", "Póster", "Vidro",
            "Cores da capa", "Meia-noite", "Barra de progresso", "Distintivo Levyra",
            "Partilhar", "Guardar", "Guardada em Imagens/Levyra", "A preparar…", "Não foi possível criar a story. Tente novamente.",
            "As cores do fundo não estão disponíveis neste dispositivo. São usadas as cores do tema Levyra.",
            "Animação de abertura", "Logótipo do Levyra animado ao abrir a app"
        )),
        "nl" to VisualCraftCopy(visualCraft(
            "Hoesbijsnijding", "Hoe niet-vierkante hoezen vierkante kaders vullen", "Gecentreerd", "Slim met zoom", "Slim zonder zoom",
            "Delen als story", "Delen als story", "Storyvoorbeeld", "Nu aan het spelen",
            "Aura", "Vinyl", "Poster", "Glas",
            "Hoeskleuren", "Middernacht", "Voortgangsbalk", "Levyra-badge",
            "Delen", "Opslaan", "Opgeslagen in Afbeeldingen/Levyra", "Voorbereiden…", "Kan de story niet maken. Probeer het opnieuw.",
            "Achtergrondkleuren zijn niet beschikbaar op dit apparaat. De kleuren van het Levyra-thema worden gebruikt.",
            "Opstartanimatie", "Geanimeerd Levyra-logo bij het openen van de app"
        )),
        "pl" to VisualCraftCopy(visualCraft(
            "Kadrowanie okładek", "Jak niekwadratowe okładki wypełniają kwadratowe ramki", "Do środka", "Inteligentne z powiększeniem", "Inteligentne bez powiększenia",
            "Udostępnij jako relację", "Udostępnij jako relację", "Podgląd relacji", "Teraz odtwarzane",
            "Aura", "Winyl", "Plakat", "Szkło",
            "Kolory okładki", "Północ", "Pasek postępu", "Odznaka Levyra",
            "Udostępnij", "Zapisz", "Zapisano w Obrazy/Levyra", "Przygotowywanie…", "Nie udało się utworzyć relacji. Spróbuj ponownie.",
            "Kolory tapety są niedostępne na tym urządzeniu. Używane są kolory motywu Levyra.",
            "Animacja startowa", "Animowane logo Levyra przy otwieraniu aplikacji"
        )),
        "ro" to VisualCraftCopy(visualCraft(
            "Decuparea copertelor", "Cum umplu copertele care nu sunt pătrate cadrele pătrate", "Centrat", "Inteligent cu zoom", "Inteligent fără zoom",
            "Distribuie ca story", "Distribuie ca story", "Previzualizare story", "Se redă acum",
            "Aura", "Vinil", "Poster", "Sticlă",
            "Culorile copertei", "Miezul nopții", "Bară de progres", "Insignă Levyra",
            "Distribuie", "Salvează", "Salvată în Imagini/Levyra", "Se pregătește…", "Nu s-a putut crea story-ul. Încearcă din nou.",
            "Culorile imaginii de fundal nu sunt disponibile pe acest dispozitiv. Se folosesc culorile temei Levyra.",
            "Animație la pornire", "Logo Levyra animat la deschiderea aplicației"
        )),
        "el" to VisualCraftCopy(visualCraft(
            "Περικοπή εξωφύλλων", "Πώς τα μη τετράγωνα εξώφυλλα γεμίζουν τετράγωνα πλαίσια", "Στο κέντρο", "Έξυπνη με ζουμ", "Έξυπνη χωρίς ζουμ",
            "Κοινοποίηση ως ιστορία", "Κοινοποίηση ως ιστορία", "Προεπισκόπηση ιστορίας", "Αναπαράγεται τώρα",
            "Aura", "Βινύλιο", "Αφίσα", "Γυαλί",
            "Χρώματα εξωφύλλου", "Μεσάνυχτα", "Γραμμή προόδου", "Σήμα Levyra",
            "Κοινοποίηση", "Αποθήκευση", "Αποθηκεύτηκε στις Εικόνες/Levyra", "Προετοιμασία…", "Δεν ήταν δυνατή η δημιουργία της ιστορίας. Δοκίμασε ξανά.",
            "Τα χρώματα της ταπετσαρίας δεν είναι διαθέσιμα σε αυτή τη συσκευή. Χρησιμοποιούνται τα χρώματα του θέματος Levyra.",
            "Κίνηση εκκίνησης", "Κινούμενο λογότυπο Levyra κατά το άνοιγμα της εφαρμογής"
        )),
        "sv" to VisualCraftCopy(visualCraft(
            "Beskärning av omslag", "Hur omslag som inte är kvadratiska fyller kvadratiska ramar", "Centrerad", "Smart med zoom", "Smart utan zoom",
            "Dela som story", "Dela som story", "Förhandsvisning av story", "Spelas nu",
            "Aura", "Vinyl", "Affisch", "Glas",
            "Omslagsfärger", "Midnatt", "Förloppsindikator", "Levyra-märke",
            "Dela", "Spara", "Sparad i Bilder/Levyra", "Förbereder…", "Det gick inte att skapa storyn. Försök igen.",
            "Bakgrundsfärger är inte tillgängliga på den här enheten. Levyra-temats färger används i stället.",
            "Startanimering", "Animerad Levyra-logga när appen öppnas"
        )),
        "da" to VisualCraftCopy(visualCraft(
            "Beskæring af covers", "Hvordan covers, der ikke er kvadratiske, fylder kvadratiske rammer", "Centreret", "Smart med zoom", "Smart uden zoom",
            "Del som story", "Del som story", "Forhåndsvisning af story", "Afspilles nu",
            "Aura", "Vinyl", "Plakat", "Glas",
            "Coverfarver", "Midnat", "Statuslinje", "Levyra-badge",
            "Del", "Gem", "Gemt i Billeder/Levyra", "Forbereder…", "Storyen kunne ikke oprettes. Prøv igen.",
            "Baggrundsfarver er ikke tilgængelige på denne enhed. Levyra-temaets farver bruges i stedet.",
            "Opstartsanimation", "Animeret Levyra-logo, når appen åbnes"
        )),
        "cs" to VisualCraftCopy(visualCraft(
            "Ořez obalů", "Jak nečtvercové obaly vyplní čtvercové rámečky", "Na střed", "Chytrý se zoomem", "Chytrý bez zoomu",
            "Sdílet jako příběh", "Sdílet jako příběh", "Náhled příběhu", "Právě hraje",
            "Aura", "Vinyl", "Plakát", "Sklo",
            "Barvy obalu", "Půlnoc", "Ukazatel průběhu", "Odznak Levyra",
            "Sdílet", "Uložit", "Uloženo do Obrázky/Levyra", "Příprava…", "Příběh se nepodařilo vytvořit. Zkuste to znovu.",
            "Barvy tapety nejsou na tomto zařízení k dispozici. Použijí se barvy motivu Levyra.",
            "Úvodní animace", "Animované logo Levyra při otevření aplikace"
        )),
        "sk" to VisualCraftCopy(visualCraft(
            "Orezanie obalov", "Ako neštvorcové obaly vyplnia štvorcové rámčeky", "Na stred", "Inteligentné so zoomom", "Inteligentné bez zoomu",
            "Zdieľať ako príbeh", "Zdieľať ako príbeh", "Ukážka príbehu", "Práve hrá",
            "Aura", "Vinyl", "Plagát", "Sklo",
            "Farby obalu", "Polnoc", "Indikátor priebehu", "Odznak Levyra",
            "Zdieľať", "Uložiť", "Uložené do Obrázky/Levyra", "Pripravuje sa…", "Príbeh sa nepodarilo vytvoriť. Skúste to znova.",
            "Farby tapety nie sú na tomto zariadení dostupné. Použijú sa farby motívu Levyra.",
            "Úvodná animácia", "Animované logo Levyra pri otvorení aplikácie"
        )),
        "bn" to VisualCraftCopy(visualCraft(
            "কভার ক্রপ", "বর্গাকার নয় এমন কভার কীভাবে বর্গাকার ফ্রেম ভরাট করে", "মাঝখানে", "জুমসহ স্মার্ট", "জুম ছাড়া স্মার্ট",
            "স্টোরি হিসেবে শেয়ার করুন", "স্টোরি হিসেবে শেয়ার করুন", "স্টোরির প্রিভিউ", "এখন বাজছে",
            "Aura", "ভিনাইল", "পোস্টার", "কাচ",
            "কভারের রং", "মধ্যরাত", "অগ্রগতির বার", "Levyra ব্যাজ",
            "শেয়ার করুন", "সংরক্ষণ করুন", "ছবি/Levyra-তে সংরক্ষিত", "প্রস্তুত হচ্ছে…", "স্টোরি তৈরি করা যায়নি। আবার চেষ্টা করুন।",
            "এই ডিভাইসে ওয়ালপেপারের রং পাওয়া যায় না। Levyra থিমের রং ব্যবহার করা হচ্ছে।",
            "শুরুর অ্যানিমেশন", "অ্যাপ খোলার সময় অ্যানিমেটেড Levyra লোগো"
        )),
        "kk" to VisualCraftCopy(visualCraft(
            "Мұқабаны кесу", "Шаршы емес мұқабалар шаршы жақтауларды қалай толтырады", "Ортасы", "Масштабпен ақылды", "Масштабсыз ақылды",
            "Сторис ретінде бөлісу", "Сторис ретінде бөлісу", "Сторис алдын ала көрінісі", "Қазір ойнап тұр",
            "Aura", "Винил", "Постер", "Шыны",
            "Мұқаба түстері", "Түн ортасы", "Барыс жолағы", "Levyra белгісі",
            "Бөлісу", "Сақтау", "Суреттер/Levyra ішіне сақталды", "Дайындалуда…", "Сторис жасалмады. Қайталап көріңіз.",
            "Бұл құрылғыда тұсқағаз түстері қолжетімсіз. Levyra тақырыбының түстері қолданылады.",
            "Іске қосу анимациясы", "Қолданба ашылғанда Levyra логотипінің анимациясы"
        )),
        "sr" to VisualCraftCopy(visualCraft(
            "Исецање омота", "Како омоти који нису квадратни попуњавају квадратне оквире", "Централно", "Паметно са зумом", "Паметно без зума",
            "Подели као причу", "Подели као причу", "Преглед приче", "Тренутно свира",
            "Aura", "Винил", "Постер", "Стакло",
            "Боје омота", "Поноћ", "Трака напретка", "Levyra значка",
            "Подели", "Сачувај", "Сачувано у Слике/Levyra", "Припрема…", "Прављење приче није успело. Покушај поново.",
            "Боје позадине нису доступне на овом уређају. Користе се боје Levyra теме.",
            "Анимација покретања", "Анимирани Levyra лого при отварању апликације"
        )),
        "sq" to VisualCraftCopy(visualCraft(
            "Prerja e kopertinave", "Si i mbushin kornizat katrore kopertinat që nuk janë katrore", "Në qendër", "Inteligjente me zmadhim", "Inteligjente pa zmadhim",
            "Ndaj si histori", "Ndaj si histori", "Pamja paraprake e historisë", "Po luhet tani",
            "Aura", "Vinil", "Poster", "Xham",
            "Ngjyrat e kopertinës", "Mesnatë", "Shiriti i progresit", "Distinktivi Levyra",
            "Ndaj", "Ruaj", "U ruajt në Fotografi/Levyra", "Po përgatitet…", "Historia nuk u krijua dot. Provo përsëri.",
            "Ngjyrat e sfondit nuk ofrohen në këtë pajisje. Përdoren ngjyrat e temës Levyra.",
            "Animacioni i nisjes", "Logoja e animuar e Levyra kur hapet aplikacioni"
        )),
        "hr" to VisualCraftCopy(visualCraft(
            "Obrezivanje omota", "Kako omoti koji nisu kvadratni ispunjavaju kvadratne okvire", "Središnje", "Pametno sa zumom", "Pametno bez zuma",
            "Podijeli kao priču", "Podijeli kao priču", "Pregled priče", "Trenutačno svira",
            "Aura", "Vinil", "Plakat", "Staklo",
            "Boje omota", "Ponoć", "Traka napretka", "Levyra značka",
            "Podijeli", "Spremi", "Spremljeno u Slike/Levyra", "Priprema…", "Izrada priče nije uspjela. Pokušaj ponovno.",
            "Boje pozadine nisu dostupne na ovom uređaju. Koriste se boje Levyra teme.",
            "Animacija pokretanja", "Animirani Levyra logotip pri otvaranju aplikacije"
        )),
        "bg" to VisualCraftCopy(visualCraft(
            "Изрязване на обложки", "Как неквадратните обложки запълват квадратни рамки", "Центрирано", "Интелигентно с мащабиране", "Интелигентно без мащабиране",
            "Споделяне като история", "Споделяне като история", "Преглед на историята", "Сега звучи",
            "Aura", "Винил", "Плакат", "Стъкло",
            "Цветове на обложката", "Полунощ", "Лента за напредък", "Значка Levyra",
            "Споделяне", "Запазване", "Запазено в Снимки/Levyra", "Подготвя се…", "Историята не можа да бъде създадена. Опитайте отново.",
            "Цветовете на тапета не са налични на това устройство. Използват се цветовете на темата Levyra.",
            "Начална анимация", "Анимирано лого на Levyra при отваряне на приложението"
        )),
        "hu" to VisualCraftCopy(visualCraft(
            "Borítók vágása", "Hogyan töltik ki a nem négyzetes borítók a négyzetes kereteket", "Középre", "Okos nagyítással", "Okos nagyítás nélkül",
            "Megosztás történetként", "Megosztás történetként", "Történet előnézete", "Most szól",
            "Aura", "Bakelit", "Plakát", "Üveg",
            "Borító színei", "Éjfél", "Folyamatjelző sáv", "Levyra jelvény",
            "Megosztás", "Mentés", "Mentve ide: Képek/Levyra", "Előkészítés…", "Nem sikerült létrehozni a történetet. Próbáld újra.",
            "A háttérkép színei nem érhetők el ezen az eszközön. A Levyra téma színei érvényesek.",
            "Indítási animáció", "Animált Levyra-logó az alkalmazás megnyitásakor"
        )),
        "fi" to VisualCraftCopy(visualCraft(
            "Kansien rajaus", "Miten muut kuin neliön muotoiset kannet täyttävät neliökehykset", "Keskitetty", "Älykäs zoomilla", "Älykäs ilman zoomia",
            "Jaa tarinana", "Jaa tarinana", "Tarinan esikatselu", "Nyt soi",
            "Aura", "Vinyyli", "Juliste", "Lasi",
            "Kansikuvan värit", "Keskiyö", "Edistymispalkki", "Levyra-merkki",
            "Jaa", "Tallenna", "Tallennettu kansioon Kuvat/Levyra", "Valmistellaan…", "Tarinan luominen epäonnistui. Yritä uudelleen.",
            "Taustakuvan värit eivät ole saatavilla tällä laitteella. Käytössä ovat Levyra-teeman värit.",
            "Käynnistysanimaatio", "Animoitu Levyra-logo sovellusta avattaessa"
        )),
        "et" to VisualCraftCopy(visualCraft(
            "Kaanepiltide kärpimine", "Kuidas mitte-ruudukujulised kaanepildid ruudukujulisi raame täidavad", "Keskel", "Nutikas suumiga", "Nutikas suumita",
            "Jaga loona", "Jaga loona", "Loo eelvaade", "Praegu mängib",
            "Aura", "Vinüül", "Plakat", "Klaas",
            "Kaanepildi värvid", "Kesköö", "Edenemisriba", "Levyra märk",
            "Jaga", "Salvesta", "Salvestatud kausta Pildid/Levyra", "Ettevalmistamine…", "Lugu ei õnnestunud luua. Proovi uuesti.",
            "Taustapildi värvid pole selles seadmes saadaval. Kasutatakse Levyra teema värve.",
            "Käivitusanimatsioon", "Animeeritud Levyra logo rakenduse avamisel"
        )),
        "nb" to VisualCraftCopy(visualCraft(
            "Beskjæring av omslag", "Hvordan omslag som ikke er kvadratiske fyller kvadratiske rammer", "Midtstilt", "Smart med zoom", "Smart uten zoom",
            "Del som story", "Del som story", "Forhåndsvisning av story", "Spilles nå",
            "Aura", "Vinyl", "Plakat", "Glass",
            "Omslagsfarger", "Midnatt", "Fremdriftslinje", "Levyra-merke",
            "Del", "Lagre", "Lagret i Bilder/Levyra", "Forbereder…", "Kunne ikke lage storyen. Prøv igjen.",
            "Bakgrunnsfarger er ikke tilgjengelige på denne enheten. Levyra-temaets farger brukes i stedet.",
            "Oppstartsanimasjon", "Animert Levyra-logo når appen åpnes"
        )),
        "ca" to VisualCraftCopy(visualCraft(
            "Retall de portades", "Com omplen els marcs quadrats les portades que no són quadrades", "Centrat", "Intel·ligent amb zoom", "Intel·ligent sense zoom",
            "Comparteix com a història", "Comparteix com a història", "Previsualització de la història", "S’està reproduint",
            "Aura", "Vinil", "Pòster", "Vidre",
            "Colors de la portada", "Mitjanit", "Barra de progrés", "Insígnia Levyra",
            "Comparteix", "Desa", "Desada a Imatges/Levyra", "S’està preparant…", "No s’ha pogut crear la història. Torna-ho a provar.",
            "Els colors del fons de pantalla no estan disponibles en aquest dispositiu. S’utilitzen els colors del tema Levyra.",
            "Animació d’inici", "Logotip de Levyra animat en obrir l’app"
        )),
        "uk" to VisualCraftCopy(visualCraft(
            "Обрізання обкладинок", "Як неквадратні обкладинки заповнюють квадратні рамки", "По центру", "Розумне з масштабуванням", "Розумне без масштабування",
            "Поділитися як історією", "Поділитися як історією", "Попередній перегляд історії", "Зараз грає",
            "Aura", "Вініл", "Постер", "Скло",
            "Кольори обкладинки", "Північ", "Смуга прогресу", "Значок Levyra",
            "Поділитися", "Зберегти", "Збережено в Зображення/Levyra", "Підготовка…", "Не вдалося створити історію. Спробуйте ще раз.",
            "Кольори шпалер недоступні на цьому пристрої. Використовуються кольори теми Levyra.",
            "Анімація запуску", "Анімований логотип Levyra під час відкриття застосунку"
        )),
        "ru" to VisualCraftCopy(visualCraft(
            "Обрезка обложек", "Как неквадратные обложки заполняют квадратные рамки", "По центру", "Умная с масштабированием", "Умная без масштабирования",
            "Поделиться в истории", "Поделиться в истории", "Предпросмотр истории", "Сейчас играет",
            "Aura", "Винил", "Постер", "Стекло",
            "Цвета обложки", "Полночь", "Полоса прогресса", "Значок Levyra",
            "Поделиться", "Сохранить", "Сохранено в Изображения/Levyra", "Подготовка…", "Не удалось создать историю. Попробуйте ещё раз.",
            "Цвета обоев недоступны на этом устройстве. Используются цвета темы Levyra.",
            "Анимация запуска", "Анимированный логотип Levyra при открытии приложения"
        )),
        "tr" to VisualCraftCopy(visualCraft(
            "Kapak kırpma", "Kare olmayan kapakların kare çerçeveleri nasıl dolduracağı", "Ortala", "Yakınlaştırmalı akıllı", "Yakınlaştırmasız akıllı",
            "Hikâye olarak paylaş", "Hikâye olarak paylaş", "Hikâye önizlemesi", "Şimdi çalıyor",
            "Aura", "Plak", "Poster", "Cam",
            "Kapak renkleri", "Gece yarısı", "İlerleme çubuğu", "Levyra rozeti",
            "Paylaş", "Kaydet", "Resimler/Levyra klasörüne kaydedildi", "Hazırlanıyor…", "Hikâye oluşturulamadı. Tekrar dene.",
            "Duvar kâğıdı renkleri bu cihazda kullanılamıyor. Bunun yerine Levyra tema renkleri kullanılıyor.",
            "Açılış animasyonu", "Uygulama açılırken animasyonlu Levyra logosu"
        )),
        "ar" to VisualCraftCopy(visualCraft(
            "قص الأغلفة", "كيف تملأ الأغلفة غير المربعة الإطارات المربعة", "في المنتصف", "ذكي مع التكبير", "ذكي بدون تكبير",
            "المشاركة كقصة", "المشاركة كقصة", "معاينة القصة", "قيد التشغيل الآن",
            "Aura", "أسطوانة", "ملصق", "زجاج",
            "ألوان الغلاف", "منتصف الليل", "شريط التقدم", "شارة Levyra",
            "مشاركة", "حفظ", "تم الحفظ في الصور/Levyra", "جارٍ التحضير…", "تعذّر إنشاء القصة. حاول مرة أخرى.",
            "ألوان الخلفية غير متاحة على هذا الجهاز. يتم استخدام ألوان سمة Levyra بدلاً منها.",
            "حركة بدء التشغيل", "شعار Levyra متحرك عند فتح التطبيق"
        )),
        "fa" to VisualCraftCopy(visualCraft(
            "برش جلدها", "جلدهای غیرمربعی چگونه قاب‌های مربعی را پر می‌کنند", "وسط", "هوشمند با بزرگ‌نمایی", "هوشمند بدون بزرگ‌نمایی",
            "اشتراک‌گذاری به‌صورت استوری", "اشتراک‌گذاری به‌صورت استوری", "پیش‌نمایش استوری", "در حال پخش",
            "Aura", "صفحهٔ وینیل", "پوستر", "شیشه",
            "رنگ‌های جلد", "نیمه‌شب", "نوار پیشرفت", "نشان Levyra",
            "اشتراک‌گذاری", "ذخیره", "در تصاویر/Levyra ذخیره شد", "در حال آماده‌سازی…", "ساخت استوری ممکن نشد. دوباره تلاش کنید.",
            "رنگ‌های کاغذدیواری در این دستگاه در دسترس نیست. رنگ‌های طرح زمینهٔ Levyra استفاده می‌شود.",
            "انیمیشن شروع", "لوگوی متحرک Levyra هنگام باز شدن برنامه"
        )),
        "zh" to VisualCraftCopy(visualCraft(
            "封面裁剪", "非方形封面如何填满方形画框", "居中", "智能（缩放）", "智能（不缩放）",
            "分享为快拍", "分享为快拍", "快拍预览", "正在播放",
            "Aura", "黑胶", "海报", "玻璃",
            "封面配色", "午夜", "进度条", "Levyra 标识",
            "分享", "保存", "已保存到 图片/Levyra", "正在准备…", "无法生成快拍，请重试。",
            "此设备无法获取壁纸颜色，将使用 Levyra 主题颜色。",
            "启动动画", "打开应用时显示 Levyra 动态标志"
        )),
        "zh-Hant" to VisualCraftCopy(visualCraft(
            "封面裁切", "非方形封面如何填滿方形畫框", "置中", "智慧（縮放）", "智慧（不縮放）",
            "分享為限時動態", "分享為限時動態", "限時動態預覽", "正在播放",
            "Aura", "黑膠", "海報", "玻璃",
            "封面配色", "午夜", "進度列", "Levyra 標誌",
            "分享", "儲存", "已儲存到 圖片/Levyra", "正在準備…", "無法建立限時動態，請再試一次。",
            "這部裝置無法取得桌布顏色，將改用 Levyra 主題顏色。",
            "啟動動畫", "開啟應用程式時顯示 Levyra 動態標誌"
        )),
        "ja" to VisualCraftCopy(visualCraft(
            "ジャケットの切り抜き", "正方形でないジャケットを正方形の枠に合わせる方法", "中央", "スマート（ズームあり）", "スマート（ズームなし）",
            "ストーリーでシェア", "ストーリーでシェア", "ストーリーのプレビュー", "再生中",
            "Aura", "レコード", "ポスター", "ガラス",
            "ジャケットの色", "ミッドナイト", "プログレスバー", "Levyra バッジ",
            "シェア", "保存", "ピクチャ/Levyra に保存しました", "準備中…", "ストーリーを作成できませんでした。もう一度お試しください。",
            "このデバイスでは壁紙の色を取得できません。Levyra テーマの色を使用します。",
            "起動アニメーション", "アプリを開くときに Levyra ロゴをアニメーション表示"
        )),
        "ko" to VisualCraftCopy(visualCraft(
            "커버 자르기", "정사각형이 아닌 커버가 정사각형 프레임을 채우는 방식", "가운데", "스마트(확대)", "스마트(확대 없음)",
            "스토리로 공유", "스토리로 공유", "스토리 미리보기", "지금 재생 중",
            "Aura", "바이닐", "포스터", "글래스",
            "커버 색상", "미드나이트", "진행률 표시줄", "Levyra 배지",
            "공유", "저장", "사진/Levyra에 저장됨", "준비 중…", "스토리를 만들지 못했어요. 다시 시도하세요.",
            "이 기기에서는 배경화면 색상을 사용할 수 없습니다. Levyra 테마 색상이 대신 사용됩니다.",
            "시작 애니메이션", "앱을 열 때 Levyra 로고 애니메이션 표시"
        )),
        "hi" to VisualCraftCopy(visualCraft(
            "कवर क्रॉपिंग", "जो कवर वर्गाकार नहीं हैं वे वर्गाकार फ़्रेम कैसे भरते हैं", "बीच में", "ज़ूम के साथ स्मार्ट", "बिना ज़ूम के स्मार्ट",
            "स्टोरी के रूप में शेयर करें", "स्टोरी के रूप में शेयर करें", "स्टोरी का प्रीव्यू", "अभी चल रहा है",
            "Aura", "विनाइल", "पोस्टर", "ग्लास",
            "कवर के रंग", "मिडनाइट", "प्रोग्रेस बार", "Levyra बैज",
            "शेयर करें", "सेव करें", "पिक्चर्स/Levyra में सेव किया गया", "तैयार हो रहा है…", "स्टोरी नहीं बन सकी। फिर से कोशिश करें।",
            "इस डिवाइस पर वॉलपेपर के रंग उपलब्ध नहीं हैं। इसकी जगह Levyra थीम के रंग इस्तेमाल होते हैं।",
            "स्टार्टअप एनिमेशन", "ऐप खुलने पर Levyra का एनिमेटेड लोगो"
        )),
        "id" to VisualCraftCopy(visualCraft(
            "Pemotongan sampul", "Cara sampul yang tidak persegi mengisi bingkai persegi", "Tengah", "Cerdas dengan zoom", "Cerdas tanpa zoom",
            "Bagikan sebagai story", "Bagikan sebagai story", "Pratinjau story", "Sedang diputar",
            "Aura", "Vinil", "Poster", "Kaca",
            "Warna sampul", "Tengah malam", "Bilah progres", "Lencana Levyra",
            "Bagikan", "Simpan", "Disimpan di Gambar/Levyra", "Menyiapkan…", "Tidak dapat membuat story. Coba lagi.",
            "Warna wallpaper tidak tersedia di perangkat ini. Warna tema Levyra digunakan sebagai gantinya.",
            "Animasi pembuka", "Logo Levyra beranimasi saat aplikasi dibuka"
        )),
        "ms" to VisualCraftCopy(visualCraft(
            "Pemangkasan kulit album", "Cara kulit album yang bukan segi empat sama mengisi bingkai segi empat sama", "Tengah", "Pintar dengan zum", "Pintar tanpa zum",
            "Kongsi sebagai cerita", "Kongsi sebagai cerita", "Pratonton cerita", "Sedang dimainkan",
            "Aura", "Vinil", "Poster", "Kaca",
            "Warna kulit album", "Tengah malam", "Bar kemajuan", "Lencana Levyra",
            "Kongsi", "Simpan", "Disimpan dalam Gambar/Levyra", "Menyediakan…", "Tidak dapat mencipta cerita. Cuba lagi.",
            "Warna kertas dinding tidak tersedia pada peranti ini. Warna tema Levyra digunakan sebaliknya.",
            "Animasi permulaan", "Logo Levyra beranimasi apabila apl dibuka"
        )),
        "vi" to VisualCraftCopy(visualCraft(
            "Cắt ảnh bìa", "Cách ảnh bìa không vuông lấp đầy khung vuông", "Giữa", "Thông minh có thu phóng", "Thông minh không thu phóng",
            "Chia sẻ thành tin", "Chia sẻ thành tin", "Xem trước tin", "Đang phát",
            "Aura", "Đĩa than", "Áp phích", "Kính",
            "Màu ảnh bìa", "Nửa đêm", "Thanh tiến trình", "Huy hiệu Levyra",
            "Chia sẻ", "Lưu", "Đã lưu vào Ảnh/Levyra", "Đang chuẩn bị…", "Không thể tạo tin. Hãy thử lại.",
            "Màu hình nền không khả dụng trên thiết bị này. Màu chủ đề Levyra được dùng thay thế.",
            "Hiệu ứng khởi động", "Logo Levyra chuyển động khi mở ứng dụng"
        )),
        "th" to VisualCraftCopy(visualCraft(
            "การครอบตัดปก", "วิธีที่ปกซึ่งไม่ใช่สี่เหลี่ยมจัตุรัสเติมกรอบสี่เหลี่ยมจัตุรัส", "กึ่งกลาง", "อัจฉริยะแบบซูม", "อัจฉริยะแบบไม่ซูม",
            "แชร์เป็นสตอรี่", "แชร์เป็นสตอรี่", "ตัวอย่างสตอรี่", "กำลังเล่น",
            "Aura", "แผ่นเสียง", "โปสเตอร์", "กระจก",
            "สีจากปก", "เที่ยงคืน", "แถบความคืบหน้า", "ตรา Levyra",
            "แชร์", "บันทึก", "บันทึกไว้ใน รูปภาพ/Levyra แล้ว", "กำลังเตรียม…", "สร้างสตอรี่ไม่ได้ โปรดลองอีกครั้ง",
            "อุปกรณ์นี้ไม่มีสีจากวอลเปเปอร์ จึงใช้สีธีม Levyra แทน",
            "แอนิเมชันเริ่มต้น", "โลโก้ Levyra แบบเคลื่อนไหวเมื่อเปิดแอป"
        )),
        "fil" to VisualCraftCopy(visualCraft(
            "Pag-crop ng cover", "Paano pinupuno ng mga cover na hindi parisukat ang mga parisukat na frame", "Gitna", "Smart na may zoom", "Smart na walang zoom",
            "Ibahagi bilang story", "Ibahagi bilang story", "Preview ng story", "Nagpe-play ngayon",
            "Aura", "Vinyl", "Poster", "Salamin",
            "Mga kulay ng cover", "Hatinggabi", "Progress bar", "Badge ng Levyra",
            "Ibahagi", "I-save", "Na-save sa Pictures/Levyra", "Inihahanda…", "Hindi magawa ang story. Subukang muli.",
            "Hindi available ang mga kulay ng wallpaper sa device na ito. Ginagamit ang mga kulay ng tema ng Levyra.",
            "Animation sa pagbukas", "Gumagalaw na logo ng Levyra kapag binuksan ang app"
        )),
        "he" to VisualCraftCopy(visualCraft(
            "חיתוך עטיפות", "איך עטיפות שאינן ריבועיות ממלאות מסגרות ריבועיות", "במרכז", "חכם עם זום", "חכם בלי זום",
            "שיתוף כסטורי", "שיתוף כסטורי", "תצוגה מקדימה של הסטורי", "מתנגן עכשיו",
            "Aura", "תקליט", "פוסטר", "זכוכית",
            "צבעי העטיפה", "חצות", "סרגל התקדמות", "תג Levyra",
            "שיתוף", "שמירה", "נשמר בתמונות/Levyra", "בהכנה…", "לא ניתן היה ליצור את הסטורי. נסו שוב.",
            "צבעי הטפט אינם זמינים במכשיר הזה. נעשה שימוש בצבעי ערכת הנושא של Levyra.",
            "אנימציית פתיחה", "לוגו Levyra מונפש בעת פתיחת האפליקציה"
        )),
        "ur" to VisualCraftCopy(visualCraft(
                "کور فصل", "کس طرح احاطہ کرتا ہے جو مربع نہیں ہیں مربع فریموں کو بھریں", "مرکز", "زوم کے ساتھ اسمارٹ", "زوم کے بغیر اسمارٹ",
                "کہانی کے طور پر شیئر کریں۔", "کہانی کے طور پر شیئر کریں۔", "کہانی کا پیش نظارہ", "ابھی چل رہا ہے",
                "چمک", "ونائل", "پوسٹر", "شیشہ",
                "آرٹ ورک کے رنگ", "آدھی رات", "پیش رفت بار", "Levyra بیج",
                "شیئر کریں۔", "محفوظ کریں۔", "Pictures/Levyra میں محفوظ کیا گیا۔", "تیار ہو رہا ہے…", "کہانی نہیں بنائی جا سکی۔ دوبارہ کوشش کریں۔",
                "وال پیپر کے رنگ اس ڈیوائس پر دستیاب نہیں ہیں۔ اس کے بجائے Levyra تھیم کے رنگ استعمال کیے گئے ہیں۔",
                "اسٹارٹ اپ اینیمیشن", "ایپ کھلنے پر متحرک Levyra لوگو"
        )),
        "ta" to VisualCraftCopy(visualCraft(
                "மூடி பயிர் செய்தல்", "சதுரமாக இல்லாத அட்டைகள் எப்படி சதுர சட்டங்களை நிரப்புகின்றன", "மையம்", "ஜூம் மூலம் ஸ்மார்ட்", "பெரிதாக்காமல் ஸ்மார்ட்",
                "கதையாக பகிரவும்", "கதையாகப் பகிரவும்", "கதை முன்னோட்டம்", "இப்போது இயங்குகிறது",
                "ஆரா", "வினைல்", "சுவரொட்டி", "கண்ணாடி",
                "கலைப்படைப்பு வண்ணங்கள்", "நள்ளிரவு", "முன்னேற்றப் பட்டி", "Levyra பேட்ஜ்",
                "பகிரவும்", "சேமிக்கவும்", "படங்கள்/Levyra இல் சேமிக்கப்பட்டது", "தயாராகிறது…", "கதையை உருவாக்க முடியவில்லை. மீண்டும் முயற்சிக்கவும்.",
                "இந்தச் சாதனத்தில் வால்பேப்பர் வண்ணங்கள் இல்லை. Levyra தீம் வண்ணங்கள் பதிலாக பயன்படுத்தப்படுகின்றன.",
                "தொடக்க அனிமேஷன்", "பயன்பாடு திறக்கும் போது அனிமேஷன் செய்யப்பட்ட Levyra லோகோ"
        )),
        "te" to VisualCraftCopy(visualCraft(
                "కవర్ క్రాపింగ్", "చతురస్రాకారంలో లేని కవర్లు చతురస్రాకార ఫ్రేమ్‌లను ఎలా నింపుతాయి", "కేంద్రం", "జూమ్‌తో స్మార్ట్", "జూమ్ లేకుండా స్మార్ట్",
                "కథనంగా షేర్ చేయండి", "కథనంగా షేర్ చేయండి", "కథ ప్రివ్యూ", "ఇప్పుడు ప్లే అవుతోంది",
                "ప్రకాశం", "వినైల్", "పోస్టర్", "గాజు",
                "కళాకృతి రంగులు", "అర్ధరాత్రి", "ప్రోగ్రెస్ బార్", "Levyra బ్యాడ్జ్",
                "షేర్ చేయండి", "సేవ్ చేయండి", "చిత్రాలు/Levyraకి సేవ్ చేయబడింది", "సిద్ధమవుతోంది...", "కథను సృష్టించడం సాధ్యపడలేదు. మళ్లీ ప్రయత్నించండి.",
                "ఈ పరికరంలో వాల్‌పేపర్ రంగులు అందుబాటులో లేవు. బదులుగా Levyra థీమ్ రంగులు ఉపయోగించబడతాయి.",
                "స్టార్టప్ యానిమేషన్", "యాప్ తెరిచినప్పుడు యానిమేటెడ్ Levyra లోగో"
        )),
        "mr" to VisualCraftCopy(visualCraft(
                "कव्हर क्रॉपिंग", "चौरस नसलेली कव्हर चौकोनी चौकटी कशी भरतात", "केंद्र", "झूम सह स्मार्ट", "झूम न करता स्मार्ट",
                "कथा म्हणून शेअर करा", "कथा म्हणून शेअर करा", "कथेचे पूर्वावलोकन", "आता वाजत आहे",
                "आभा", "विनाइल", "पोस्टर", "काच",
                "कलाकृती रंग", "मध्यरात्री", "प्रगती बार", "Levyra बॅज",
                "शेअर करा", "जतन करा", "Pictures/Levyra मध्ये सेव्ह केले", "तयारी करत आहे...", "कथा तयार करू शकलो नाही. पुन्हा प्रयत्न करा.",
                "या डिव्हाइसवर वॉलपेपर रंग उपलब्ध नाहीत. त्याऐवजी Levyra थीम रंग वापरले जातात.",
                "स्टार्टअप ॲनिमेशन", "ॲप उघडल्यावर ॲनिमेटेड Levyra लोगो"
        )),
        "gu" to VisualCraftCopy(visualCraft(
                "કવર પાક", "કેવી રીતે આવરી લે છે કે જે ચોરસ નથી ચોરસ ફ્રેમ ભરો", "કેન્દ્ર", "ઝૂમ સાથે સ્માર્ટ", "ઝૂમ વિના સ્માર્ટ",
                "વાર્તા તરીકે શેર કરો", "વાર્તા તરીકે શેર કરો", "વાર્તા પૂર્વાવલોકન", "હમણાં વાગી રહ્યું છે",
                "આભા", "વિનાઇલ", "પોસ્ટર", "કાચ",
                "આર્ટવર્ક રંગો", "મધરાત", "પ્રગતિ પટ્ટી", "Levyra બેજ",
                "શેર કરો", "સાચવો", "Pictures/Levyra માં સાચવેલ", "તૈયારી કરી રહ્યું છે...", "વાર્તા બનાવી શકાઈ નથી. ફરી પ્રયાસ કરો.",
                "આ ઉપકરણ પર વૉલપેપરના રંગો ઉપલબ્ધ નથી. તેના બદલે Levyra થીમ રંગોનો ઉપયોગ કરવામાં આવે છે.",
                "સ્ટાર્ટઅપ એનિમેશન", "જ્યારે એપ્લિકેશન ખુલે છે ત્યારે એનિમેટેડ Levyra લોગો"
        )),
        "pt-BR" to VisualCraftCopy(visualCraft(
                "Recorte de capa", "Como as capas que não são quadradas preenchem molduras quadradas", "Centro", "Inteligente com zoom", "Inteligente sem zoom",
                "Compartilhar como história", "Compartilhar como uma história", "Pré-visualização da história", "Em reprodução",
                "Aura", "Vinil", "Pôster", "Vidro",
                "Cores da arte", "Meia-noite", "Barra de progresso", "Emblema Levyra",
                "Compartilhar", "Salvar", "Salvo em imagens/Levyra", "Preparando…", "Não foi possível criar a história. Tente novamente.",
                "As cores do papel de parede não estão disponíveis neste dispositivo. As cores do tema Levyra são usadas em seu lugar.",
                "Animação de inicialização", "Logotipo Levyra animado quando o aplicativo é aberto"
        ))
    )
}

internal fun visualCraftIncompleteLocalizationCodes(): Set<String> =
    visualCraftCopies.filterValues { !it.complete }.keys

internal fun visualCraftLocalizationCodes(): Set<String> = visualCraftCopies.keys

internal val LevyraStrings.visualCraftCopy: VisualCraftCopy
    get() = localizedValueOrEnglish(visualCraftCopies, code).forBrazil(code) { brazilian }
