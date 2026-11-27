package com.luc4n3x.levyra.ui.i18n

enum class SpectrumText {
    Title, Subtitle, Off, OffDesc,
    Bars, BarsDesc, Mirror, MirrorDesc, Led, LedDesc, Curve, CurveDesc,
    Analyzer, AnalyzerDesc, Spectrogram, SpectrogramDesc, Oscilloscope, OscilloscopeDesc,
    Latency, LatencyDesc, Earlier, Later, Reset, Accessibility,
    ExportTitle, ExportSubtitle, ExportGraphic, ExportGraphicDesc, ExportParametric, ExportParametricDesc,
    Exported, ExportFailed
}

class SpectrumCopy internal constructor(private val texts: Map<SpectrumText, String>) {
    operator fun get(key: SpectrumText): String = texts[key] ?: englishSpectrum.getValue(key)
}

private fun spectrum(vararg value: String): Map<SpectrumText, String> =
    if (value.size == SpectrumText.entries.size) SpectrumText.entries.zip(value).toMap() else emptyMap()

private val englishSpectrum: Map<SpectrumText, String> = spectrum(
    "Visualizer", "Moves with the music at the bottom of the player", "Off", "No visualizer and no extra work",
    "Bars", "Classic spectrum, bass on the left", "Mirrored bars", "Bass in the center, mirrored outwards",
    "LED meter", "Segments with peaks that hold, then fall", "Spectrum curve", "A smooth outline of the whole spectrum",
    "Analyzer (RTA)", "31 third-octave bands in dBFS with peak hold, like a studio analyzer",
    "Spectrogram", "Frequencies over time: brighter means louder",
    "Oscilloscope", "The live waveform, held steady by a trigger",
    "Output latency", "Shifts the visualizer and lyrics to match this output", "Earlier", "Later", "Reset", "Audio visualizer",
    "Export equalizer", "Save the current equalizer as an AutoEq file",
    "GraphicEQ", "For apps that read GraphicEQ files", "ParametricEQ", "For parametric equalizer apps",
    "Equalizer exported", "Couldn't export the equalizer"
)

private val spectrumCopies: Map<String, Map<SpectrumText, String>> = mapOf(
    "en" to englishSpectrum,
    "it" to spectrum(
        "Visualizzatore", "Si muove a tempo di musica in fondo al player", "Spento", "Nessun visualizzatore e nessun lavoro in più",
        "Barre", "Spettro classico, bassi a sinistra", "Barre speculari", "Bassi al centro, speculari verso l'esterno",
        "Indicatore LED", "Segmenti con picchi che restano e poi scendono", "Curva dello spettro", "Un contorno morbido di tutto lo spettro",
        "Analizzatore (RTA)", "31 bande in terzi d'ottava in dBFS con mantenimento dei picchi, come un analizzatore da studio",
        "Spettrogramma", "Frequenze nel tempo: più luminoso significa più forte",
        "Oscilloscopio", "La forma d'onda dal vivo, tenuta ferma da un trigger",
        "Latenza di uscita", "Sposta visualizzatore e testi per allinearli a questa uscita", "Prima", "Dopo", "Ripristina", "Visualizzatore audio",
        "Esporta equalizzatore", "Salva l'equalizzatore attuale come file AutoEq",
        "GraphicEQ", "Per le app che leggono file GraphicEQ", "ParametricEQ", "Per le app di equalizzazione parametrica",
        "Equalizzatore esportato", "Impossibile esportare l'equalizzatore"
    ),
    "es" to spectrum(
        "Visualizador", "Se mueve con la música en la parte inferior del reproductor", "Desactivado", "Sin visualizador y sin trabajo extra",
        "Barras", "Espectro clásico, graves a la izquierda", "Barras en espejo", "Graves en el centro, reflejados hacia fuera",
        "Medidor LED", "Segmentos con picos que se mantienen y luego caen", "Curva del espectro", "Un contorno suave de todo el espectro",
        "Analizador (RTA)", "31 bandas de tercio de octava en dBFS con retención de picos, como un analizador de estudio",
        "Espectrograma", "Frecuencias en el tiempo: más brillo significa más volumen",
        "Osciloscopio", "La forma de onda en vivo, fijada por un disparador",
        "Latencia de salida", "Desplaza el visualizador y la letra para ajustarlos a esta salida", "Antes", "Después", "Restablecer", "Visualizador de audio",
        "Exportar ecualizador", "Guarda el ecualizador actual como archivo AutoEq",
        "GraphicEQ", "Para apps que leen archivos GraphicEQ", "ParametricEQ", "Para apps de ecualización paramétrica",
        "Ecualizador exportado", "No se pudo exportar el ecualizador"
    ),
    "fr" to spectrum(
        "Visualiseur", "Bouge au rythme de la musique en bas du lecteur", "Désactivé", "Aucun visualiseur et aucun travail en plus",
        "Barres", "Spectre classique, basses à gauche", "Barres en miroir", "Basses au centre, en miroir vers l'extérieur",
        "Vumètre LED", "Des segments avec des crêtes qui restent puis retombent", "Courbe du spectre", "Un contour lisse de tout le spectre",
        "Analyseur (RTA)", "31 bandes de tiers d'octave en dBFS avec maintien des crêtes, comme un analyseur de studio",
        "Spectrogramme", "Les fréquences dans le temps : plus c'est clair, plus c'est fort",
        "Oscilloscope", "La forme d'onde en direct, stabilisée par un déclencheur",
        "Latence de sortie", "Décale le visualiseur et les paroles pour les caler sur cette sortie", "Plus tôt", "Plus tard", "Réinitialiser", "Visualiseur audio",
        "Exporter l'égaliseur", "Enregistrer l'égaliseur actuel dans un fichier AutoEq",
        "GraphicEQ", "Pour les apps qui lisent les fichiers GraphicEQ", "ParametricEQ", "Pour les apps d'égalisation paramétrique",
        "Égaliseur exporté", "Impossible d'exporter l'égaliseur"
    ),
    "de" to spectrum(
        "Visualizer", "Bewegt sich unten im Player mit der Musik", "Aus", "Kein Visualizer und keine Mehrarbeit",
        "Balken", "Klassisches Spektrum, Bass links", "Gespiegelte Balken", "Bass in der Mitte, nach außen gespiegelt",
        "LED-Anzeige", "Segmente mit Spitzen, die stehen bleiben und dann fallen", "Spektrumkurve", "Eine weiche Kontur des ganzen Spektrums",
        "Analysator (RTA)", "31 Terzbänder in dBFS mit Spitzenwerthaltung, wie ein Studio-Analysator",
        "Spektrogramm", "Frequenzen im Zeitverlauf: heller heißt lauter",
        "Oszilloskop", "Die Live-Wellenform, von einem Trigger ruhig gehalten",
        "Ausgabelatenz", "Verschiebt Visualizer und Songtext passend zu dieser Ausgabe", "Früher", "Später", "Zurücksetzen", "Audio-Visualizer",
        "Equalizer exportieren", "Den aktuellen Equalizer als AutoEq-Datei speichern",
        "GraphicEQ", "Für Apps, die GraphicEQ-Dateien lesen", "ParametricEQ", "Für parametrische Equalizer-Apps",
        "Equalizer exportiert", "Equalizer konnte nicht exportiert werden"
    ),
    "pt" to spectrum(
        "Visualizador", "Move-se com a música na parte inferior do leitor", "Desligado", "Sem visualizador e sem trabalho extra",
        "Barras", "Espetro clássico, graves à esquerda", "Barras espelhadas", "Graves ao centro, espelhados para fora",
        "Medidor LED", "Segmentos com picos que ficam e depois descem", "Curva do espetro", "Um contorno suave de todo o espetro",
        "Analisador (RTA)", "31 bandas de terço de oitava em dBFS com retenção de picos, como um analisador de estúdio",
        "Espetrograma", "Frequências ao longo do tempo: mais claro significa mais alto",
        "Osciloscópio", "A forma de onda ao vivo, estabilizada por um gatilho",
        "Latência de saída", "Desloca o visualizador e a letra para coincidirem com esta saída", "Mais cedo", "Mais tarde", "Repor", "Visualizador de áudio",
        "Exportar equalizador", "Guardar o equalizador atual como ficheiro AutoEq",
        "GraphicEQ", "Para apps que leem ficheiros GraphicEQ", "ParametricEQ", "Para apps de equalização paramétrica",
        "Equalizador exportado", "Não foi possível exportar o equalizador"
    ),
    "nl" to spectrum(
        "Visualizer", "Beweegt mee met de muziek onderaan de speler", "Uit", "Geen visualizer en geen extra werk",
        "Balken", "Klassiek spectrum, bas links", "Gespiegelde balken", "Bas in het midden, naar buiten gespiegeld",
        "LED-meter", "Segmenten met pieken die blijven staan en dan zakken", "Spectrumcurve", "Een vloeiende omtrek van het hele spectrum",
        "Analyzer (RTA)", "31 tertsbanden in dBFS met piekvasthouding, zoals een studio-analyzer",
        "Spectrogram", "Frequenties in de tijd: lichter betekent luider",
        "Oscilloscoop", "De live golfvorm, stilgehouden door een trigger",
        "Uitvoerlatentie", "Verschuift visualizer en songtekst zodat ze bij deze uitvoer passen", "Eerder", "Later", "Herstellen", "Audiovisualizer",
        "Equalizer exporteren", "Sla de huidige equalizer op als AutoEq-bestand",
        "GraphicEQ", "Voor apps die GraphicEQ-bestanden lezen", "ParametricEQ", "Voor parametrische equalizer-apps",
        "Equalizer geëxporteerd", "Kan de equalizer niet exporteren"
    ),
    "pl" to spectrum(
        "Wizualizer", "Porusza się w rytm muzyki na dole odtwarzacza", "Wyłączony", "Bez wizualizera i bez dodatkowej pracy",
        "Słupki", "Klasyczne widmo, basy po lewej", "Słupki lustrzane", "Basy na środku, odbite na zewnątrz",
        "Wskaźnik LED", "Segmenty ze szczytami, które się zatrzymują, a potem opadają", "Krzywa widma", "Gładki kontur całego widma",
        "Analizator (RTA)", "31 pasm tercjowych w dBFS z podtrzymaniem szczytów, jak analizator studyjny",
        "Spektrogram", "Częstotliwości w czasie: jaśniej znaczy głośniej",
        "Oscyloskop", "Przebieg na żywo, ustabilizowany wyzwalaczem",
        "Opóźnienie wyjścia", "Przesuwa wizualizer i tekst, aby pasowały do tego wyjścia", "Wcześniej", "Później", "Resetuj", "Wizualizer dźwięku",
        "Eksportuj korektor", "Zapisz bieżący korektor jako plik AutoEq",
        "GraphicEQ", "Dla aplikacji czytających pliki GraphicEQ", "ParametricEQ", "Dla aplikacji z korektorem parametrycznym",
        "Korektor wyeksportowany", "Nie udało się wyeksportować korektora"
    ),
    "ro" to spectrum(
        "Vizualizator", "Se mișcă odată cu muzica în partea de jos a playerului", "Dezactivat", "Fără vizualizator și fără muncă în plus",
        "Bare", "Spectru clasic, basul în stânga", "Bare în oglindă", "Basul în centru, oglindit spre exterior",
        "Indicator LED", "Segmente cu vârfuri care rămân, apoi coboară", "Curba spectrului", "Un contur fin al întregului spectru",
        "Analizor (RTA)", "31 de benzi de o treime de octavă în dBFS cu menținerea vârfurilor, ca un analizor de studio",
        "Spectrogramă", "Frecvențele în timp: mai luminos înseamnă mai tare",
        "Osciloscop", "Forma de undă live, stabilizată de un declanșator",
        "Latența ieșirii", "Mută vizualizatorul și versurile ca să se potrivească acestei ieșiri", "Mai devreme", "Mai târziu", "Resetează", "Vizualizator audio",
        "Exportă egalizatorul", "Salvează egalizatorul curent ca fișier AutoEq",
        "GraphicEQ", "Pentru aplicațiile care citesc fișiere GraphicEQ", "ParametricEQ", "Pentru aplicațiile de egalizare parametrică",
        "Egalizator exportat", "Egalizatorul nu a putut fi exportat"
    ),
    "el" to spectrum(
        "Οπτικοποιητής", "Κινείται με τη μουσική στο κάτω μέρος του player", "Ανενεργό", "Χωρίς οπτικοποίηση και χωρίς επιπλέον εργασία",
        "Μπάρες", "Κλασικό φάσμα, μπάσα αριστερά", "Κατοπτρικές μπάρες", "Μπάσα στο κέντρο, κατοπτρικά προς τα έξω",
        "Μετρητής LED", "Τμήματα με κορυφές που μένουν και μετά πέφτουν", "Καμπύλη φάσματος", "Ένα ομαλό περίγραμμα όλου του φάσματος",
        "Αναλυτής (RTA)", "31 ζώνες τρίτου οκτάβας σε dBFS με διατήρηση κορυφών, σαν αναλυτής στούντιο",
        "Φασματογράφημα", "Συχνότητες στον χρόνο: πιο φωτεινό σημαίνει πιο δυνατό",
        "Παλμογράφος", "Η κυματομορφή ζωντανά, σταθεροποιημένη από σκανδάλη",
        "Καθυστέρηση εξόδου", "Μετατοπίζει την οπτικοποίηση και τους στίχους ώστε να ταιριάζουν σε αυτή την έξοδο", "Νωρίτερα", "Αργότερα", "Επαναφορά", "Οπτικοποιητής ήχου",
        "Εξαγωγή ισοσταθμιστή", "Αποθήκευση του τρέχοντος ισοσταθμιστή ως αρχείο AutoEq",
        "GraphicEQ", "Για εφαρμογές που διαβάζουν αρχεία GraphicEQ", "ParametricEQ", "Για εφαρμογές παραμετρικού ισοσταθμιστή",
        "Ο ισοσταθμιστής εξήχθη", "Δεν ήταν δυνατή η εξαγωγή του ισοσταθμιστή"
    ),
    "sv" to spectrum(
        "Visualiserare", "Rör sig med musiken längst ned i spelaren", "Av", "Ingen visualisering och inget extra arbete",
        "Staplar", "Klassiskt spektrum, bas till vänster", "Speglade staplar", "Bas i mitten, speglad utåt",
        "LED-mätare", "Segment med toppar som stannar kvar och sedan faller", "Spektrumkurva", "En mjuk kontur av hela spektrumet",
        "Analysator (RTA)", "31 tersband i dBFS med topphållning, som en studioanalysator",
        "Spektrogram", "Frekvenser över tid: ljusare betyder starkare",
        "Oscilloskop", "Vågformen live, stillastående tack vare en trigger",
        "Utgångslatens", "Flyttar visualisering och låttext så att de passar denna utgång", "Tidigare", "Senare", "Återställ", "Ljudvisualisering",
        "Exportera equalizer", "Spara den aktuella equalizern som en AutoEq-fil",
        "GraphicEQ", "För appar som läser GraphicEQ-filer", "ParametricEQ", "För appar med parametrisk equalizer",
        "Equalizern exporterades", "Det gick inte att exportera equalizern"
    ),
    "da" to spectrum(
        "Visualizer", "Bevæger sig med musikken nederst i afspilleren", "Fra", "Ingen visualizer og intet ekstra arbejde",
        "Søjler", "Klassisk spektrum, bas til venstre", "Spejlede søjler", "Bas i midten, spejlet udad",
        "LED-måler", "Segmenter med toppe, der bliver stående og så falder", "Spektrumkurve", "En blød kontur af hele spektret",
        "Analysator (RTA)", "31 tertsbånd i dBFS med tophold, som en studieanalysator",
        "Spektrogram", "Frekvenser over tid: lysere betyder højere",
        "Oscilloskop", "Bølgeformen live, holdt stille af en trigger",
        "Outputforsinkelse", "Forskyder visualizer og sangtekst, så de passer til dette output", "Tidligere", "Senere", "Nulstil", "Lydvisualizer",
        "Eksportér equalizer", "Gem den aktuelle equalizer som en AutoEq-fil",
        "GraphicEQ", "Til apps, der læser GraphicEQ-filer", "ParametricEQ", "Til apps med parametrisk equalizer",
        "Equalizeren er eksporteret", "Equalizeren kunne ikke eksporteres"
    ),
    "cs" to spectrum(
        "Vizualizér", "Pohybuje se s hudbou v dolní části přehrávače", "Vypnuto", "Žádný vizualizér a žádná práce navíc",
        "Sloupce", "Klasické spektrum, basy vlevo", "Zrcadlové sloupce", "Basy uprostřed, zrcadlené směrem ven",
        "LED ukazatel", "Segmenty se špičkami, které chvíli zůstanou a pak klesnou", "Křivka spektra", "Hladký obrys celého spektra",
        "Analyzátor (RTA)", "31 třetinooktávových pásem v dBFS s držením špiček, jako studiový analyzátor",
        "Spektrogram", "Frekvence v čase: jasnější znamená hlasitější",
        "Osciloskop", "Živý průběh vlny, ustálený spouštěním",
        "Latence výstupu", "Posune vizualizér a text tak, aby odpovídaly tomuto výstupu", "Dříve", "Později", "Obnovit", "Zvukový vizualizér",
        "Exportovat ekvalizér", "Uložit aktuální ekvalizér jako soubor AutoEq",
        "GraphicEQ", "Pro aplikace, které čtou soubory GraphicEQ", "ParametricEQ", "Pro aplikace s parametrickým ekvalizérem",
        "Ekvalizér byl exportován", "Ekvalizér se nepodařilo exportovat"
    ),
    "sk" to spectrum(
        "Vizualizér", "Hýbe sa s hudbou v spodnej časti prehrávača", "Vypnuté", "Žiadny vizualizér a žiadna práca navyše",
        "Stĺpce", "Klasické spektrum, basy vľavo", "Zrkadlové stĺpce", "Basy v strede, zrkadlené smerom von",
        "LED ukazovateľ", "Segmenty so špičkami, ktoré chvíľu zostanú a potom klesnú", "Krivka spektra", "Hladký obrys celého spektra",
        "Analyzátor (RTA)", "31 tretinooktávových pásiem v dBFS s držaním špičiek, ako štúdiový analyzátor",
        "Spektrogram", "Frekvencie v čase: jasnejšie znamená hlasnejšie",
        "Osciloskop", "Živý priebeh vlny, ustálený spúšťaním",
        "Latencia výstupu", "Posunie vizualizér a text tak, aby zodpovedali tomuto výstupu", "Skôr", "Neskôr", "Obnoviť", "Zvukový vizualizér",
        "Exportovať ekvalizér", "Uložiť aktuálny ekvalizér ako súbor AutoEq",
        "GraphicEQ", "Pre aplikácie, ktoré čítajú súbory GraphicEQ", "ParametricEQ", "Pre aplikácie s parametrickým ekvalizérom",
        "Ekvalizér bol exportovaný", "Ekvalizér sa nepodarilo exportovať"
    ),
    "hr" to spectrum(
        "Vizualizator", "Kreće se uz glazbu pri dnu playera", "Isključeno", "Bez vizualizatora i bez dodatnog rada",
        "Stupci", "Klasični spektar, basovi lijevo", "Zrcalni stupci", "Basovi u sredini, zrcaljeni prema van",
        "LED mjerač", "Segmenti s vršcima koji zastanu pa padnu", "Krivulja spektra", "Glatki obris cijelog spektra",
        "Analizator (RTA)", "31 tercni pojas u dBFS sa zadržavanjem vršnih vrijednosti, kao studijski analizator",
        "Spektrogram", "Frekvencije kroz vrijeme: svjetlije znači glasnije",
        "Osciloskop", "Valni oblik uživo, stabiliziran okidačem",
        "Kašnjenje izlaza", "Pomiče vizualizator i tekst da odgovaraju ovom izlazu", "Ranije", "Kasnije", "Vrati", "Audio vizualizator",
        "Izvezi ekvilajzer", "Spremi trenutačni ekvilajzer kao AutoEq datoteku",
        "GraphicEQ", "Za aplikacije koje čitaju GraphicEQ datoteke", "ParametricEQ", "Za aplikacije s parametarskim ekvilajzerom",
        "Ekvilajzer je izvezen", "Ekvilajzer nije moguće izvesti"
    ),
    "sr" to spectrum(
        "Vizuelizator", "Kreće se uz muziku pri dnu plejera", "Isključeno", "Bez vizuelizatora i bez dodatnog rada",
        "Stubići", "Klasičan spektar, basovi levo", "Ogledalni stubići", "Basovi u sredini, preslikani ka spolja",
        "LED merač", "Segmenti sa vrhovima koji zastanu pa padnu", "Kriva spektra", "Gladak obris celog spektra",
        "Analizator (RTA)", "31 tercni opseg u dBFS sa zadržavanjem vrhova, kao studijski analizator",
        "Spektrogram", "Frekvencije kroz vreme: svetlije znači glasnije",
        "Osciloskop", "Talasni oblik uživo, stabilizovan okidačem",
        "Kašnjenje izlaza", "Pomera vizuelizator i tekst da odgovaraju ovom izlazu", "Ranije", "Kasnije", "Vrati", "Audio vizuelizator",
        "Izvezi ekvilajzer", "Sačuvaj trenutni ekvilajzer kao AutoEq datoteku",
        "GraphicEQ", "Za aplikacije koje čitaju GraphicEQ datoteke", "ParametricEQ", "Za aplikacije sa parametarskim ekvilajzerom",
        "Ekvilajzer je izvezen", "Nije moguće izvesti ekvilajzer"
    ),
    "sq" to spectrum(
        "Vizualizues", "Lëviz me muzikën në fund të luajtësit", "Joaktiv", "Pa vizualizues dhe pa punë shtesë",
        "Shtylla", "Spektër klasik, bas majtas", "Shtylla pasqyrë", "Basi në qendër, i pasqyruar nga jashtë",
        "Matës LED", "Segmente me maja që qëndrojnë e pastaj bien", "Kurba e spektrit", "Një kontur i butë i gjithë spektrit",
        "Analizues (RTA)", "31 breza të një të treteje oktave në dBFS me mbajtje majash, si analizues studioje",
        "Spektrogram", "Frekuencat në kohë: më e ndritshme do të thotë më e fortë",
        "Oshiloskop", "Forma e valës drejtpërdrejt, e qëndrueshme falë një shkrehësi",
        "Vonesa e daljes", "Zhvendos vizualizuesin dhe tekstin që të përputhen me këtë dalje", "Më herët", "Më vonë", "Rivendos", "Vizualizues audio",
        "Eksporto barazuesin", "Ruaj barazuesin aktual si skedar AutoEq",
        "GraphicEQ", "Për aplikacionet që lexojnë skedarë GraphicEQ", "ParametricEQ", "Për aplikacionet e barazuesit parametrik",
        "Barazuesi u eksportua", "Barazuesi nuk mund të eksportohej"
    ),
    "bg" to spectrum(
        "Визуализатор", "Движи се с музиката в долната част на плейъра", "Изключено", "Без визуализатор и без допълнителна работа",
        "Стълбове", "Класически спектър, басите вляво", "Огледални стълбове", "Басите в центъра, огледално навън",
        "LED индикатор", "Сегменти с върхове, които се задържат и после падат", "Крива на спектъра", "Плавен контур на целия спектър",
        "Анализатор (RTA)", "31 терцови ленти в dBFS със задържане на върховете, като студиен анализатор",
        "Спектрограма", "Честоти във времето: по-светло означава по-силно",
        "Осцилоскоп", "Формата на вълната на живо, стабилизирана от тригер",
        "Закъснение на изхода", "Измества визуализатора и текста, за да съвпаднат с този изход", "По-рано", "По-късно", "Нулиране", "Аудио визуализатор",
        "Експорт на еквалайзера", "Запазване на текущия еквалайзер като файл AutoEq",
        "GraphicEQ", "За приложения, които четат файлове GraphicEQ", "ParametricEQ", "За приложения с параметричен еквалайзер",
        "Еквалайзерът е експортиран", "Еквалайзерът не можа да бъде експортиран"
    ),
    "hu" to spectrum(
        "Vizualizáció", "A lejátszó alján mozog a zenével", "Ki", "Nincs vizualizáció és nincs többletmunka",
        "Oszlopok", "Klasszikus spektrum, basszus balra", "Tükrözött oszlopok", "Basszus középen, kifelé tükrözve",
        "LED-kijelző", "Szegmensek csúcsokkal, amelyek megmaradnak, majd leesnek", "Spektrumgörbe", "A teljes spektrum lágy körvonala",
        "Analizátor (RTA)", "31 tercsáv dBFS-ben csúcstartással, mint egy stúdióanalizátor",
        "Spektrogram", "Frekvenciák az időben: a világosabb hangosabbat jelent",
        "Oszcilloszkóp", "Az élő hullámforma, triggerrel állóképben tartva",
        "Kimeneti késleltetés", "Eltolja a vizualizációt és a dalszöveget, hogy illeszkedjenek ehhez a kimenethez", "Korábban", "Később", "Visszaállítás", "Hangvizualizáció",
        "Hangszínszabályzó exportálása", "A jelenlegi hangszínszabályzó mentése AutoEq-fájlként",
        "GraphicEQ", "GraphicEQ-fájlokat olvasó alkalmazásokhoz", "ParametricEQ", "Parametrikus hangszínszabályzó alkalmazásokhoz",
        "Hangszínszabályzó exportálva", "Nem sikerült exportálni a hangszínszabályzót"
    ),
    "fi" to spectrum(
        "Visualisointi", "Liikkuu musiikin tahdissa soittimen alareunassa", "Pois", "Ei visualisointia eikä lisätyötä",
        "Palkit", "Klassinen spektri, basso vasemmalla", "Peilatut palkit", "Basso keskellä, peilattuna ulospäin",
        "LED-mittari", "Lohkoja, joiden huiput jäävät hetkeksi ja putoavat sitten", "Spektrikäyrä", "Pehmeä ääriviiva koko spektristä",
        "Analysaattori (RTA)", "31 terssikaistaa dBFS-asteikolla huippujen pidolla, kuin studioanalysaattori",
        "Spektrogrammi", "Taajuudet ajan funktiona: kirkkaampi tarkoittaa voimakkaampaa",
        "Oskilloskooppi", "Aaltomuoto livenä, liipaisimella paikallaan pidettynä",
        "Lähdön viive", "Siirtää visualisointia ja sanoituksia vastaamaan tätä lähtöä", "Aiemmin", "Myöhemmin", "Palauta", "Äänen visualisointi",
        "Vie taajuuskorjain", "Tallenna nykyinen taajuuskorjain AutoEq-tiedostoksi",
        "GraphicEQ", "Sovelluksiin, jotka lukevat GraphicEQ-tiedostoja", "ParametricEQ", "Parametrisen taajuuskorjaimen sovelluksiin",
        "Taajuuskorjain viety", "Taajuuskorjaimen vienti epäonnistui"
    ),
    "et" to spectrum(
        "Visualiseerija", "Liigub muusika saatel pleieri allservas", "Väljas", "Ilma visualiseerijata ja lisatööta",
        "Tulbad", "Klassikaline spekter, bass vasakul", "Peegeldatud tulbad", "Bass keskel, peegeldatud väljapoole",
        "LED-mõõdik", "Segmendid tippudega, mis püsivad ja siis langevad", "Spektrikõver", "Kogu spektri sujuv kontuur",
        "Analüsaator (RTA)", "31 tertsiriba dBFS-is tippude hoidmisega, nagu stuudioanalüsaator",
        "Spektrogramm", "Sagedused ajas: heledam tähendab valjemat",
        "Ostsilloskoop", "Lainekuju reaalajas, päästikuga paigal hoitud",
        "Väljundi latentsus", "Nihutab visualiseerijat ja laulusõnu, et need sobiksid selle väljundiga", "Varem", "Hiljem", "Lähtesta", "Heli visualiseerija",
        "Ekvalaiseri eksport", "Salvesta praegune ekvalaiser AutoEq-failina",
        "GraphicEQ", "Rakendustele, mis loevad GraphicEQ-faile", "ParametricEQ", "Parameetrilise ekvalaiseri rakendustele",
        "Ekvalaiser eksporditi", "Ekvalaiserit ei õnnestunud eksportida"
    ),
    "nb" to spectrum(
        "Visualisering", "Beveger seg med musikken nederst i spilleren", "Av", "Ingen visualisering og ikke noe ekstra arbeid",
        "Søyler", "Klassisk spektrum, bass til venstre", "Speilede søyler", "Bass i midten, speilet utover",
        "LED-måler", "Segmenter med topper som blir stående og så faller", "Spektrumkurve", "En jevn kontur av hele spekteret",
        "Analysator (RTA)", "31 tersbånd i dBFS med topphold, som en studioanalysator",
        "Spektrogram", "Frekvenser over tid: lysere betyr sterkere",
        "Oscilloskop", "Bølgeformen live, holdt i ro av en trigger",
        "Utgangsforsinkelse", "Forskyver visualisering og sangtekst så de passer til denne utgangen", "Tidligere", "Senere", "Tilbakestill", "Lydvisualisering",
        "Eksporter equalizer", "Lagre gjeldende equalizer som en AutoEq-fil",
        "GraphicEQ", "For apper som leser GraphicEQ-filer", "ParametricEQ", "For apper med parametrisk equalizer",
        "Equalizeren er eksportert", "Kunne ikke eksportere equalizeren"
    ),
    "ca" to spectrum(
        "Visualitzador", "Es mou amb la música a la part inferior del reproductor", "Desactivat", "Sense visualitzador i sense feina extra",
        "Barres", "Espectre clàssic, greus a l'esquerra", "Barres en mirall", "Greus al centre, reflectits cap enfora",
        "Mesurador LED", "Segments amb pics que es mantenen i després cauen", "Corba de l'espectre", "Un contorn suau de tot l'espectre",
        "Analitzador (RTA)", "31 bandes de terç d'octava en dBFS amb retenció de pics, com un analitzador d'estudi",
        "Espectrograma", "Freqüències en el temps: més brillant vol dir més fort",
        "Oscil·loscopi", "La forma d'ona en directe, fixada per un disparador",
        "Latència de sortida", "Desplaça el visualitzador i la lletra perquè coincideixin amb aquesta sortida", "Abans", "Després", "Restableix", "Visualitzador d'àudio",
        "Exporta l'equalitzador", "Desa l'equalitzador actual com a fitxer AutoEq",
        "GraphicEQ", "Per a apps que llegeixen fitxers GraphicEQ", "ParametricEQ", "Per a apps d'equalització paramètrica",
        "Equalitzador exportat", "No s'ha pogut exportar l'equalitzador"
    ),
    "uk" to spectrum(
        "Візуалізатор", "Рухається в такт музиці внизу плеєра", "Вимкнено", "Без візуалізатора і без зайвої роботи",
        "Стовпчики", "Класичний спектр, баси ліворуч", "Дзеркальні стовпчики", "Баси в центрі, дзеркально назовні",
        "LED-індикатор", "Сегменти з піками, які затримуються, а потім падають", "Крива спектра", "Плавний контур усього спектра",
        "Аналізатор (RTA)", "31 третьоктавна смуга в dBFS з утриманням піків, як студійний аналізатор",
        "Спектрограма", "Частоти в часі: що яскравіше, то гучніше",
        "Осцилограф", "Хвиля наживо, утримувана тригером",
        "Затримка виходу", "Зсуває візуалізатор і текст пісні під цей вихід", "Раніше", "Пізніше", "Скинути", "Аудіовізуалізатор",
        "Експорт еквалайзера", "Зберегти поточний еквалайзер як файл AutoEq",
        "GraphicEQ", "Для застосунків, що читають файли GraphicEQ", "ParametricEQ", "Для застосунків із параметричним еквалайзером",
        "Еквалайзер експортовано", "Не вдалося експортувати еквалайзер"
    ),
    "ru" to spectrum(
        "Визуализатор", "Двигается в такт музыке внизу плеера", "Выключен", "Без визуализатора и без лишней работы",
        "Столбцы", "Классический спектр, басы слева", "Симметричные столбцы", "Басы в центре, зеркально наружу",
        "LED-индикатор", "Сегменты с пиками, которые задерживаются, а потом падают", "Кривая спектра", "Плавный контур всего спектра",
        "Анализатор (RTA)", "31 третьоктавная полоса в dBFS с удержанием пиков, как студийный анализатор",
        "Спектрограмма", "Частоты во времени: чем ярче, тем громче",
        "Осциллограф", "Живая форма волны, удерживаемая триггером",
        "Задержка выхода", "Сдвигает визуализатор и текст песни под этот выход", "Раньше", "Позже", "Сбросить", "Аудиовизуализатор",
        "Экспорт эквалайзера", "Сохранить текущий эквалайзер как файл AutoEq",
        "GraphicEQ", "Для приложений, читающих файлы GraphicEQ", "ParametricEQ", "Для приложений с параметрическим эквалайзером",
        "Эквалайзер экспортирован", "Не удалось экспортировать эквалайзер"
    ),
    "kk" to spectrum(
        "Визуализатор", "Плеердің төменгі жағында музыкамен бірге қозғалады", "Өшірулі", "Визуализаторсыз әрі артық жұмыссыз",
        "Бағандар", "Классикалық спектр, бас сол жақта", "Айналы бағандар", "Бас ортада, сыртқа қарай айналы",
        "LED индикаторы", "Шыңдары тұрып қалып, кейін түсетін сегменттер", "Спектр қисығы", "Бүкіл спектрдің тегіс контуры",
        "Анализатор (RTA)", "Шыңды ұстайтын dBFS бойынша 31 үштен бір октавалық жолақ, студиялық анализатор сияқты",
        "Спектрограмма", "Уақыт бойынша жиіліктер: неғұрлым жарық болса, соғұрлым қатты",
        "Осциллограф", "Триггер ұстап тұратын тірі толқын пішіні",
        "Шығыс кідірісі", "Визуализатор мен ән мәтінін осы шығысқа сәйкестендіреді", "Ертерек", "Кейінірек", "Қалпына келтіру", "Аудио визуализатор",
        "Эквалайзерді экспорттау", "Ағымдағы эквалайзерді AutoEq файлы ретінде сақтау",
        "GraphicEQ", "GraphicEQ файлдарын оқитын қолданбалар үшін", "ParametricEQ", "Параметрлік эквалайзер қолданбалары үшін",
        "Эквалайзер экспортталды", "Эквалайзерді экспорттау мүмкін болмады"
    ),
    "tr" to spectrum(
        "Görselleştirici", "Oynatıcının altında müzikle birlikte hareket eder", "Kapalı", "Görselleştirici yok, ek iş yok",
        "Çubuklar", "Klasik spektrum, bas solda", "Aynalı çubuklar", "Bas ortada, dışa doğru aynalanmış",
        "LED gösterge", "Tepeleri kısa süre kalıp sonra düşen bölümler", "Spektrum eğrisi", "Tüm spektrumun yumuşak bir dış hattı",
        "Analizör (RTA)", "Tepe tutmalı, dBFS cinsinden 31 üçte bir oktav bandı; bir stüdyo analizörü gibi",
        "Spektrogram", "Zaman içinde frekanslar: daha parlak, daha yüksek ses demek",
        "Osiloskop", "Tetikleyiciyle sabit tutulan canlı dalga biçimi",
        "Çıkış gecikmesi", "Görselleştiriciyi ve şarkı sözlerini bu çıkışa göre kaydırır", "Daha erken", "Daha geç", "Sıfırla", "Ses görselleştirici",
        "Ekolayzeri dışa aktar", "Geçerli ekolayzeri AutoEq dosyası olarak kaydet",
        "GraphicEQ", "GraphicEQ dosyalarını okuyan uygulamalar için", "ParametricEQ", "Parametrik ekolayzer uygulamaları için",
        "Ekolayzer dışa aktarıldı", "Ekolayzer dışa aktarılamadı"
    ),
    "ar" to spectrum(
        "المرئيات الصوتية", "تتحرك مع الموسيقى أسفل المشغّل", "متوقف", "بلا مرئيات وبلا عمل إضافي",
        "أعمدة", "طيف كلاسيكي، الجهير على اليسار", "أعمدة متناظرة", "الجهير في الوسط ومنعكس نحو الخارج",
        "مقياس LED", "مقاطع بقمم تثبت قليلًا ثم تهبط", "منحنى الطيف", "حدّ ناعم للطيف بأكمله",
        "المحلل (RTA)", "31 نطاقًا بثلث أوكتاف بوحدة dBFS مع تثبيت القمم، مثل محلل الاستوديو",
        "مخطط طيفي", "الترددات عبر الزمن: الأسطع يعني الأعلى صوتًا",
        "راسم الذبذبات", "شكل الموجة المباشر، مثبت بواسطة مُطلق",
        "تأخير الإخراج", "يزيح المرئيات وكلمات الأغنية لتطابق هذا الإخراج", "أبكر", "لاحقًا", "إعادة ضبط", "مرئيات الصوت",
        "تصدير المعادل", "حفظ المعادل الحالي كملف AutoEq",
        "GraphicEQ", "للتطبيقات التي تقرأ ملفات GraphicEQ", "ParametricEQ", "لتطبيقات المعادل البارامتري",
        "تم تصدير المعادل", "تعذّر تصدير المعادل"
    ),
    "fa" to spectrum(
        "نمایشگر صدا", "پایین پخش‌کننده همراه موسیقی حرکت می‌کند", "خاموش", "بدون نمایشگر و بدون کار اضافه",
        "ستون‌ها", "طیف کلاسیک، بم در سمت چپ", "ستون‌های قرینه", "بم در مرکز، قرینه به سمت بیرون",
        "نشانگر LED", "بخش‌هایی با اوج‌هایی که می‌مانند و سپس پایین می‌آیند", "منحنی طیف", "طرحی نرم از کل طیف",
        "تحلیلگر (RTA)", "۳۱ باند یک‌سوم اکتاو بر حسب dBFS با نگه‌داشت اوج، مانند تحلیلگر استودیو",
        "طیف‌نگاره", "فرکانس‌ها در طول زمان: روشن‌تر یعنی بلندتر",
        "اسیلوسکوپ", "شکل موج زنده که با تریگر ثابت می‌ماند",
        "تأخیر خروجی", "نمایشگر و متن آهنگ را با این خروجی هماهنگ می‌کند", "زودتر", "دیرتر", "بازنشانی", "نمایشگر صوتی",
        "صدور اکولایزر", "ذخیرهٔ اکولایزر فعلی به‌صورت فایل AutoEq",
        "GraphicEQ", "برای برنامه‌هایی که فایل GraphicEQ می‌خوانند", "ParametricEQ", "برای برنامه‌های اکولایزر پارامتریک",
        "اکولایزر صادر شد", "صدور اکولایزر ممکن نشد"
    ),
    "zh" to spectrum(
        "可视化效果", "在播放器底部随音乐律动", "关闭", "不显示可视化，也没有额外开销",
        "柱状", "经典频谱，低音在左", "对称柱状", "低音居中，向外对称",
        "LED 电平表", "峰值停留片刻后回落的分段", "频谱曲线", "整个频谱的平滑轮廓",
        "分析仪 (RTA)", "31 个三分之一倍频程频段，以 dBFS 显示并保持峰值，就像录音室分析仪",
        "频谱图", "频率随时间变化：越亮越响",
        "示波器", "由触发器稳定的实时波形",
        "输出延迟", "调整可视化和歌词，使其与此输出同步", "提前", "延后", "重置", "音频可视化",
        "导出均衡器", "将当前均衡器保存为 AutoEq 文件",
        "GraphicEQ", "适用于读取 GraphicEQ 文件的应用", "ParametricEQ", "适用于参数均衡器应用",
        "均衡器已导出", "无法导出均衡器"
    ),
    "zh-Hant" to spectrum(
        "視覺化效果", "在播放器底部隨音樂律動", "關閉", "不顯示視覺化，也沒有額外負擔",
        "長條", "經典頻譜，低音在左", "對稱長條", "低音置中，向外對稱",
        "LED 電平表", "峰值停留片刻後回落的分段", "頻譜曲線", "整個頻譜的平滑輪廓",
        "分析儀 (RTA)", "31 個三分之一倍頻程頻段，以 dBFS 顯示並保持峰值，就像錄音室分析儀",
        "頻譜圖", "頻率隨時間變化：越亮越響",
        "示波器", "由觸發器穩定的即時波形",
        "輸出延遲", "調整視覺化和歌詞，使其與此輸出同步", "提前", "延後", "重設", "音訊視覺化",
        "匯出等化器", "將目前的等化器儲存為 AutoEq 檔案",
        "GraphicEQ", "適用於讀取 GraphicEQ 檔案的應用程式", "ParametricEQ", "適用於參數等化器應用程式",
        "等化器已匯出", "無法匯出等化器"
    ),
    "ja" to spectrum(
        "ビジュアライザー", "プレーヤー下部で音楽に合わせて動きます", "オフ", "ビジュアライザーなし、余分な処理なし",
        "バー", "定番のスペクトラム、低音は左", "ミラーバー", "低音を中央に、外側へ左右対称",
        "LEDメーター", "ピークが少し残ってから落ちるセグメント", "スペクトラムカーブ", "スペクトラム全体のなめらかな輪郭",
        "アナライザー (RTA)", "ピークホールド付きの dBFS 表示 31 バンド 1/3 オクターブ。スタジオのアナライザーのように",
        "スペクトログラム", "時間ごとの周波数。明るいほど大きな音",
        "オシロスコープ", "トリガーで安定させたライブ波形",
        "出力レイテンシー", "この出力に合わせてビジュアライザーと歌詞をずらします", "早く", "遅く", "リセット", "オーディオビジュアライザー",
        "イコライザーを書き出す", "現在のイコライザーを AutoEq ファイルとして保存",
        "GraphicEQ", "GraphicEQ ファイルを読み込むアプリ向け", "ParametricEQ", "パラメトリックイコライザーアプリ向け",
        "イコライザーを書き出しました", "イコライザーを書き出せませんでした"
    ),
    "ko" to spectrum(
        "시각화", "플레이어 하단에서 음악에 맞춰 움직입니다", "끄기", "시각화 없음, 추가 작업 없음",
        "막대", "클래식 스펙트럼, 저음은 왼쪽", "대칭 막대", "저음은 가운데, 바깥쪽으로 대칭",
        "LED 미터", "피크가 잠시 머문 뒤 떨어지는 세그먼트", "스펙트럼 곡선", "전체 스펙트럼의 부드러운 윤곽",
        "분석기 (RTA)", "피크 홀드가 있는 dBFS 단위 1/3 옥타브 31개 대역, 스튜디오 분석기처럼",
        "스펙트로그램", "시간에 따른 주파수: 밝을수록 큰 소리",
        "오실로스코프", "트리거로 고정한 실시간 파형",
        "출력 지연", "이 출력에 맞게 시각화와 가사를 이동합니다", "더 일찍", "더 늦게", "초기화", "오디오 시각화",
        "이퀄라이저 내보내기", "현재 이퀄라이저를 AutoEq 파일로 저장",
        "GraphicEQ", "GraphicEQ 파일을 읽는 앱용", "ParametricEQ", "파라메트릭 이퀄라이저 앱용",
        "이퀄라이저를 내보냈습니다", "이퀄라이저를 내보낼 수 없습니다"
    ),
    "hi" to spectrum(
        "विज़ुअलाइज़र", "प्लेयर के नीचे संगीत के साथ थिरकता है", "बंद", "कोई विज़ुअलाइज़र नहीं और कोई अतिरिक्त काम नहीं",
        "बार", "क्लासिक स्पेक्ट्रम, बास बाईं ओर", "मिरर बार", "बास बीच में, बाहर की ओर प्रतिबिंबित",
        "LED मीटर", "ऐसे खंड जिनकी चोटियाँ ठहरकर फिर गिरती हैं", "स्पेक्ट्रम वक्र", "पूरे स्पेक्ट्रम की एक चिकनी रूपरेखा",
        "एनालाइज़र (RTA)", "पीक होल्ड के साथ dBFS में 31 एक-तिहाई ऑक्टेव बैंड, स्टूडियो एनालाइज़र की तरह",
        "स्पेक्ट्रोग्राम", "समय के साथ आवृत्तियाँ: जितना चमकीला, उतना तेज़",
        "ऑसिलोस्कोप", "ट्रिगर से स्थिर रखा गया लाइव वेवफ़ॉर्म",
        "आउटपुट लेटेंसी", "विज़ुअलाइज़र और बोल को इस आउटपुट के अनुसार खिसकाता है", "पहले", "बाद में", "रीसेट करें", "ऑडियो विज़ुअलाइज़र",
        "इक्वलाइज़र निर्यात करें", "मौजूदा इक्वलाइज़र को AutoEq फ़ाइल के रूप में सहेजें",
        "GraphicEQ", "GraphicEQ फ़ाइलें पढ़ने वाले ऐप्स के लिए", "ParametricEQ", "पैरामीट्रिक इक्वलाइज़र ऐप्स के लिए",
        "इक्वलाइज़र निर्यात हो गया", "इक्वलाइज़र निर्यात नहीं हो सका"
    ),
    "bn" to spectrum(
        "ভিজ্যুয়ালাইজার", "প্লেয়ারের নিচে গানের তালে নড়ে", "বন্ধ", "কোনো ভিজ্যুয়ালাইজার নেই, বাড়তি কাজও নেই",
        "বার", "ক্লাসিক স্পেকট্রাম, বেস বাঁ দিকে", "মিরর বার", "বেস মাঝখানে, বাইরের দিকে প্রতিফলিত",
        "LED মিটার", "এমন অংশ যার চূড়া একটু থেমে তারপর নামে", "স্পেকট্রাম বক্ররেখা", "পুরো স্পেকট্রামের মসৃণ রূপরেখা",
        "অ্যানালাইজার (RTA)", "পিক হোল্ডসহ dBFS-এ ৩১টি এক-তৃতীয়াংশ অক্টেভ ব্যান্ড, স্টুডিও অ্যানালাইজারের মতো",
        "স্পেকট্রোগ্রাম", "সময়ের সঙ্গে কম্পাঙ্ক: যত উজ্জ্বল, তত জোরে",
        "অসিলোস্কোপ", "ট্রিগার দিয়ে স্থির রাখা লাইভ তরঙ্গরূপ",
        "আউটপুট ল্যাটেন্সি", "এই আউটপুটের সঙ্গে মেলাতে ভিজ্যুয়ালাইজার ও লিরিক্স সরিয়ে দেয়", "আগে", "পরে", "রিসেট", "অডিও ভিজ্যুয়ালাইজার",
        "ইকুয়ালাইজার রপ্তানি", "বর্তমান ইকুয়ালাইজার AutoEq ফাইল হিসেবে সংরক্ষণ করুন",
        "GraphicEQ", "GraphicEQ ফাইল পড়ে এমন অ্যাপের জন্য", "ParametricEQ", "প্যারামেট্রিক ইকুয়ালাইজার অ্যাপের জন্য",
        "ইকুয়ালাইজার রপ্তানি হয়েছে", "ইকুয়ালাইজার রপ্তানি করা যায়নি"
    ),
    "id" to spectrum(
        "Visualizer", "Bergerak mengikuti musik di bagian bawah pemutar", "Mati", "Tanpa visualizer dan tanpa kerja tambahan",
        "Batang", "Spektrum klasik, bass di kiri", "Batang cermin", "Bass di tengah, dicerminkan ke luar",
        "Meter LED", "Segmen dengan puncak yang bertahan lalu turun", "Kurva spektrum", "Garis luar yang halus dari seluruh spektrum",
        "Penganalisis (RTA)", "31 pita sepertiga oktaf dalam dBFS dengan penahan puncak, seperti penganalisis studio",
        "Spektrogram", "Frekuensi dari waktu ke waktu: makin terang makin keras",
        "Osiloskop", "Bentuk gelombang langsung, ditahan stabil oleh pemicu",
        "Latensi output", "Menggeser visualizer dan lirik agar sesuai dengan output ini", "Lebih awal", "Lebih lambat", "Atur ulang", "Visualizer audio",
        "Ekspor equalizer", "Simpan equalizer saat ini sebagai file AutoEq",
        "GraphicEQ", "Untuk aplikasi yang membaca file GraphicEQ", "ParametricEQ", "Untuk aplikasi equalizer parametrik",
        "Equalizer diekspor", "Tidak dapat mengekspor equalizer"
    ),
    "ms" to spectrum(
        "Visualizer", "Bergerak mengikut muzik di bahagian bawah pemain", "Mati", "Tiada visualizer dan tiada kerja tambahan",
        "Bar", "Spektrum klasik, bes di kiri", "Bar cermin", "Bes di tengah, dicerminkan ke luar",
        "Meter LED", "Segmen dengan puncak yang kekal seketika lalu jatuh", "Lengkung spektrum", "Garis luar licin bagi seluruh spektrum",
        "Penganalisis (RTA)", "31 jalur satu pertiga oktaf dalam dBFS dengan tahanan puncak, seperti penganalisis studio",
        "Spektrogram", "Frekuensi mengikut masa: lebih terang bermakna lebih kuat",
        "Osiloskop", "Bentuk gelombang langsung, distabilkan oleh picu",
        "Kependaman output", "Mengalihkan visualizer dan lirik supaya sepadan dengan output ini", "Lebih awal", "Lebih lewat", "Tetap semula", "Visualizer audio",
        "Eksport penyama", "Simpan penyama semasa sebagai fail AutoEq",
        "GraphicEQ", "Untuk apl yang membaca fail GraphicEQ", "ParametricEQ", "Untuk apl penyama parametrik",
        "Penyama dieksport", "Tidak dapat mengeksport penyama"
    ),
    "vi" to spectrum(
        "Hiệu ứng hình ảnh", "Chuyển động theo nhạc ở cuối trình phát", "Tắt", "Không có hiệu ứng và không tốn thêm tài nguyên",
        "Cột", "Phổ cổ điển, âm trầm bên trái", "Cột đối xứng", "Âm trầm ở giữa, đối xứng ra ngoài",
        "Đồng hồ LED", "Các đoạn có đỉnh dừng lại một lúc rồi rơi xuống", "Đường cong phổ", "Đường viền mượt của toàn bộ phổ",
        "Bộ phân tích (RTA)", "31 dải một phần ba quãng tám theo dBFS có giữ đỉnh, như bộ phân tích phòng thu",
        "Phổ đồ", "Tần số theo thời gian: càng sáng càng to",
        "Máy hiện sóng", "Dạng sóng trực tiếp, được giữ ổn định bằng bộ kích hoạt",
        "Độ trễ đầu ra", "Dịch hiệu ứng hình ảnh và lời bài hát cho khớp với đầu ra này", "Sớm hơn", "Muộn hơn", "Đặt lại", "Hiệu ứng âm thanh",
        "Xuất bộ chỉnh âm", "Lưu bộ chỉnh âm hiện tại thành tệp AutoEq",
        "GraphicEQ", "Cho ứng dụng đọc tệp GraphicEQ", "ParametricEQ", "Cho ứng dụng chỉnh âm tham số",
        "Đã xuất bộ chỉnh âm", "Không thể xuất bộ chỉnh âm"
    ),
    "th" to spectrum(
        "ภาพประกอบเสียง", "เคลื่อนไหวตามเพลงที่ด้านล่างของเพลเยอร์", "ปิด", "ไม่มีภาพประกอบและไม่มีงานเพิ่ม",
        "แท่ง", "สเปกตรัมแบบคลาสสิก เสียงเบสอยู่ทางซ้าย", "แท่งสะท้อน", "เบสอยู่ตรงกลาง สะท้อนออกด้านนอก",
        "มาตรวัด LED", "ส่วนที่มีจุดสูงสุดค้างไว้ครู่หนึ่งแล้วตกลง", "เส้นโค้งสเปกตรัม", "เส้นขอบที่นุ่มนวลของสเปกตรัมทั้งหมด",
        "ตัววิเคราะห์ (RTA)", "31 ย่านความถี่หนึ่งในสามอ็อกเทฟในหน่วย dBFS พร้อมค้างค่าสูงสุด เหมือนตัววิเคราะห์ในสตูดิโอ",
        "สเปกโตรแกรม", "ความถี่ตามเวลา: ยิ่งสว่างยิ่งดัง",
        "ออสซิลโลสโคป", "รูปคลื่นสดที่ทริกเกอร์ช่วยให้นิ่ง",
        "ความหน่วงเอาต์พุต", "เลื่อนภาพประกอบและเนื้อเพลงให้ตรงกับเอาต์พุตนี้", "เร็วขึ้น", "ช้าลง", "รีเซ็ต", "ภาพประกอบเสียง",
        "ส่งออกอีควอไลเซอร์", "บันทึกอีควอไลเซอร์ปัจจุบันเป็นไฟล์ AutoEq",
        "GraphicEQ", "สำหรับแอปที่อ่านไฟล์ GraphicEQ", "ParametricEQ", "สำหรับแอปอีควอไลเซอร์แบบพาราเมตริก",
        "ส่งออกอีควอไลเซอร์แล้ว", "ส่งออกอีควอไลเซอร์ไม่ได้"
    ),
    "fil" to spectrum(
        "Visualizer", "Gumagalaw kasabay ng musika sa ibaba ng player", "Naka-off", "Walang visualizer at walang dagdag na trabaho",
        "Mga bar", "Klasikong spectrum, bass sa kaliwa", "Mga bar na nakasalamin", "Bass sa gitna, nakasalamin palabas",
        "LED meter", "Mga segment na may tuktok na sandaling nananatili bago bumaba", "Kurba ng spectrum", "Makinis na balangkas ng buong spectrum",
        "Analyzer (RTA)", "31 third-octave band sa dBFS na may peak hold, gaya ng analyzer sa studio",
        "Spectrogram", "Mga frequency sa paglipas ng oras: mas maliwanag, mas malakas",
        "Oscilloscope", "Ang live na waveform, pinapanatiling tuloy ng trigger",
        "Latency ng output", "Iniuusog ang visualizer at lyrics para tumugma sa output na ito", "Mas maaga", "Mas huli", "I-reset", "Audio visualizer",
        "I-export ang equalizer", "I-save ang kasalukuyang equalizer bilang AutoEq file",
        "GraphicEQ", "Para sa mga app na bumabasa ng GraphicEQ file", "ParametricEQ", "Para sa mga app ng parametric equalizer",
        "Na-export ang equalizer", "Hindi ma-export ang equalizer"
    ),
    "he" to spectrum(
        "ויזואליזציה", "זזה עם המוזיקה בתחתית הנגן", "כבוי", "בלי ויזואליזציה ובלי עבודה נוספת",
        "עמודות", "ספקטרום קלאסי, הבס משמאל", "עמודות משוקפות", "הבס במרכז, משוקף כלפי חוץ",
        "מד LED", "מקטעים עם שיאים שנשארים רגע ואז יורדים", "עקומת ספקטרום", "קו מתאר חלק של כל הספקטרום",
        "מנתח (RTA)", "31 פסים של שליש אוקטבה ב־dBFS עם החזקת שיא, כמו מנתח אולפן",
        "ספקטרוגרמה", "תדרים לאורך זמן: בהיר יותר פירושו חזק יותר",
        "אוסצילוסקופ", "צורת הגל בזמן אמת, מיוצבת בעזרת טריגר",
        "השהיית פלט", "מזיזה את הוויזואליזציה ואת המילים כך שיתאימו לפלט הזה", "מוקדם יותר", "מאוחר יותר", "איפוס", "ויזואליזציית שמע",
        "ייצוא אקולייזר", "שמירת האקולייזר הנוכחי כקובץ AutoEq",
        "GraphicEQ", "לאפליקציות שקוראות קובצי GraphicEQ", "ParametricEQ", "לאפליקציות של אקולייזר פרמטרי",
        "האקולייזר יוצא", "לא ניתן לייצא את האקולייזר"
    ),
    "ur" to spectrum(
        "آڈیو ویژولائزر", "پلیئر کے نیچے موسیقی کے ساتھ حرکت کرتا ہے", "بند", "ویژولائزر بند ہے، اضافی پروسیسنگ نہیں ہوتی",
        "بارز", "روایتی اسپیکٹرم، باس بائیں طرف", "آئینہ دار بارز", "باس درمیان میں، دونوں طرف ایک جیسی بارز",
        "LED میٹر", "چوٹیاں کچھ دیر اپنی سطح پر رہتی ہیں، پھر نیچے آتی ہیں", "اسپیکٹرم کا منحنی خط", "پورے اسپیکٹرم کی ہموار لکیر",
        "تجزیہ کار (RTA)", "dBFS میں ایک تہائی آکٹیو کے 31 بینڈ، جن میں بلند ترین سطح کچھ دیر دکھائی جاتی ہے",
        "اسپیکٹروگرام", "وقت کے ساتھ فریکوئنسیوں کی تبدیلی۔ زیادہ روشن رنگ کا مطلب زیادہ آواز ہے",
        "آسیلوسکوپ", "آواز کی موجودہ موج، جسے ٹرگر مستحکم رکھتا ہے",
        "آؤٹ پٹ میں تاخیر", "ویژولائزر اور گانے کے بول کو اس آؤٹ پٹ کے ساتھ ہم وقت کرتا ہے", "پہلے", "بعد میں", "ری سیٹ کریں", "آڈیو ویژولائزر",
        "ایکولائزر برآمد کریں", "موجودہ ایکولائزر کو AutoEq فائل میں محفوظ کریں",
        "GraphicEQ", "ان ایپس کے لیے جو GraphicEQ فائلیں پڑھتی ہیں", "ParametricEQ", "پیرامیٹرک ایکولائزر ایپس کے لیے",
        "ایکولائزر برآمد ہو گیا", "ایکولائزر برآمد نہیں ہو سکا"
    ),
    "ta" to spectrum(
        "ஒலிக் காட்சிப்படுத்தி", "பிளேயரின் கீழே இசைக்கேற்ப அசையும்", "முடக்கப்பட்டது", "காட்சிப்படுத்தியும் கூடுதல் செயலாக்கமும் முடக்கப்படும்",
        "பட்டைகள்", "இடப்புறம் தாழ் ஒலிகளைக் காட்டும் வழக்கமான நிறமாலை", "பிரதிபலிக்கும் பட்டைகள்", "தாழ் ஒலிகள் நடுவில், இருபுறமும் ஒரே மாதிரி",
        "LED அளவி", "உச்ச அளவுகள் சிறிது நேரம் நிலைத்துப் பிறகு குறையும்", "நிறமாலை வளைவு", "முழு நிறமாலையின் மென்மையான கோடு",
        "பகுப்பான் (RTA)", "ஒரு ஆக்டேவின் மூன்றில் ஒரு பங்கு அளவுள்ள 31 பட்டைகள். dBFS இல் உச்ச அளவுகள் சிறிது நேரம் காட்டப்படும்",
        "நிறமாலை வரைபடம்", "காலப்போக்கில் அதிர்வெண்கள். அதிக ஒளிர்வு அதிக ஒலியைக் குறிக்கும்",
        "அலைவடிவ அளவி", "தூண்டுதல் மூலம் நிலையாகக் காட்டப்படும் நேரடி அலைவடிவம்",
        "ஒலி வெளியீட்டுத் தாமதம்", "இந்த ஒலி வெளியீட்டுடன் காட்சிப்படுத்தியையும் பாடல் வரிகளையும் ஒத்திசைக்கிறது", "முன்னதாக", "பின்னதாக", "மீட்டமை", "ஒலிக் காட்சிப்படுத்தி",
        "ஒலி சமப்படுத்தியை ஏற்றுமதி செய்யுங்கள்", "தற்போதைய ஒலி சமப்படுத்தியை AutoEq கோப்பாகச் சேமிக்கவும்",
        "GraphicEQ", "GraphicEQ கோப்புகளைப் படிக்கும் செயலிகளுக்கு", "ParametricEQ", "அளவுரு ஒலி சமப்படுத்தி செயலிகளுக்கு",
        "ஒலி சமப்படுத்தி ஏற்றுமதி செய்யப்பட்டது", "ஒலி சமப்படுத்தியை ஏற்றுமதி செய்ய முடியவில்லை"
    ),
    "te" to spectrum(
        "ఆడియో విజువలైజర్", "ప్లేయర్ దిగువన సంగీతానికి అనుగుణంగా కదులుతుంది", "ఆఫ్", "విజువలైజర్, అదనపు ప్రాసెసింగ్ రెండూ ఆఫ్‌లో ఉంటాయి",
        "బార్‌లు", "ఎడమవైపు బాస్‌తో సాధారణ స్పెక్ట్రం", "అద్దంలా ప్రతిబింబించే బార్‌లు", "మధ్యలో బాస్, రెండు వైపులా ఒకేలా కనిపిస్తుంది",
        "LED మీటర్", "గరిష్ఠ స్థాయులు కొద్దిసేపు నిలిచి తర్వాత తగ్గుతాయి", "స్పెక్ట్రం వక్రరేఖ", "మొత్తం స్పెక్ట్రంను చూపే మృదువైన రేఖ",
        "విశ్లేషకం (RTA)", "మూడో వంతు ఆక్టేవ్ చొప్పున 31 బ్యాండ్‌లు. గరిష్ఠ స్థాయులు dBFSలో కొద్దిసేపు కనిపిస్తాయి",
        "స్పెక్ట్రోగ్రామ్", "కాలంతో మారే పౌనఃపున్యాలు. ఎక్కువ వెలుగు అంటే ఎక్కువ శబ్దం",
        "ఆసిలోస్కోప్", "ట్రిగ్గర్‌తో స్థిరంగా చూపించే ప్రత్యక్ష తరంగ రూపం",
        "అవుట్‌పుట్ ఆలస్యం", "ఈ ఆడియో అవుట్‌పుట్‌కు విజువలైజర్, పాట సాహిత్యం సమకాలీకరిస్తుంది", "ముందుగా", "ఆలస్యంగా", "రీసెట్ చేయండి", "ఆడియో విజువలైజర్",
        "ఈక్వలైజర్‌ను ఎగుమతి చేయండి", "ప్రస్తుత ఈక్వలైజర్‌ను AutoEq ఫైల్‌గా సేవ్ చేయండి",
        "GraphicEQ", "GraphicEQ ఫైల్‌లను చదివే యాప్‌ల కోసం", "ParametricEQ", "పారామెట్రిక్ ఈక్వలైజర్ యాప్‌ల కోసం",
        "ఈక్వలైజర్ ఎగుమతి అయింది", "ఈక్వలైజర్‌ను ఎగుమతి చేయలేకపోయాం"
    ),
    "mr" to spectrum(
        "ऑडिओ व्हिज्युअलायझर", "प्लेअरच्या तळाशी संगीताच्या तालावर हलतो", "बंद", "व्हिज्युअलायझर आणि अतिरिक्त प्रक्रिया बंद",
        "पट्ट्या", "डावीकडे बास असलेला पारंपरिक स्पेक्ट्रम", "सममित पट्ट्या", "मध्यभागी बास आणि दोन्ही बाजूंना त्याचे प्रतिबिंब",
        "LED मीटर", "कमाल पातळी थोडा वेळ दिसते आणि मग खाली येते", "स्पेक्ट्रमचा वक्र", "संपूर्ण स्पेक्ट्रमची गुळगुळीत रेषा",
        "विश्लेषक (RTA)", "प्रत्येकी एक तृतीयांश ऑक्टेव्हचे 31 बँड. कमाल पातळी dBFS मध्ये थोडा वेळ दिसते",
        "स्पेक्ट्रोग्राम", "कालानुसार बदलणारी वारंवारता. अधिक उजळ रंग म्हणजे अधिक मोठा आवाज",
        "ऑसिलोस्कोप", "ट्रिगरच्या मदतीने स्थिर दिसणारा प्रत्यक्ष तरंग",
        "आउटपुटचा विलंब", "या आउटपुटशी व्हिज्युअलायझर आणि गाण्याचे बोल जुळवतो", "लवकर", "उशिरा", "रीसेट करा", "ऑडिओ व्हिज्युअलायझर",
        "इक्वलायझर एक्सपोर्ट करा", "सध्याचा इक्वलायझर AutoEq फाइल म्हणून जतन करा",
        "GraphicEQ", "GraphicEQ फाइल वाचणाऱ्या ॲप्ससाठी", "ParametricEQ", "पॅरामेट्रिक इक्वलायझर ॲप्ससाठी",
        "इक्वलायझर एक्सपोर्ट झाला", "इक्वलायझर एक्सपोर्ट करता आला नाही"
    ),
    "gu" to spectrum(
        "ઑડિયો વિઝ્યુઅલાઇઝર", "પ્લેયરની નીચે સંગીતના તાલે હલે છે", "બંધ", "વિઝ્યુઅલાઇઝર અને વધારાની પ્રક્રિયા બંધ રહે છે",
        "પટ્ટીઓ", "ડાબી બાજુ બેસ સાથેનું પરંપરાગત સ્પેક્ટ્રમ", "પ્રતિબિંબિત પટ્ટીઓ", "વચ્ચે બેસ અને બંને બાજુ સમાન પટ્ટીઓ",
        "LED મીટર", "ટોચનું સ્તર થોડી વાર દેખાય છે અને પછી ઘટે છે", "સ્પેક્ટ્રમનું વક્ર", "આખા સ્પેક્ટ્રમની સરળ રેખા",
        "વિશ્લેષક (RTA)", "એક તૃતીયાંશ ઑક્ટેવના 31 બેન્ડ. ટોચનું સ્તર થોડા સમય માટે dBFSમાં દેખાય છે",
        "સ્પેક્ટ્રોગ્રામ", "સમય સાથે બદલાતી આવૃત્તિઓ. વધુ તેજસ્વી એટલે વધુ મોટો અવાજ",
        "ઑસિલોસ્કોપ", "ટ્રિગરની મદદથી સ્થિર દેખાતું જીવંત તરંગરૂપ",
        "આઉટપુટમાં વિલંબ", "વિઝ્યુઅલાઇઝર અને ગીતના બોલને આ ઑડિયો આઉટપુટ સાથે મેળવે છે", "વહેલું", "મોડું", "રીસેટ કરો", "ઑડિયો વિઝ્યુઅલાઇઝર",
        "ઇક્વલાઇઝર નિકાસ કરો", "હાલનું ઇક્વલાઇઝર AutoEq ફાઇલ તરીકે સાચવો",
        "GraphicEQ", "GraphicEQ ફાઇલો વાંચતી ઍપ્સ માટે", "ParametricEQ", "પેરામેટ્રિક ઇક્વલાઇઝર ઍપ્સ માટે",
        "ઇક્વલાઇઝરની નિકાસ થઈ", "ઇક્વલાઇઝરની નિકાસ થઈ શકી નથી"
    ),
    "pt-BR" to spectrum(
        "Visualizador", "Acompanha o ritmo da música na parte de baixo do player", "Desligado", "Desativa o visualizador e o processamento extra",
        "Barras", "Espectro clássico, com os graves à esquerda", "Barras espelhadas", "Graves no centro, com as barras espelhadas para fora",
        "Medidor de LED", "Segmentos que mantêm os picos por um instante antes de baixar", "Curva do espectro", "Uma linha suave que mostra todo o espectro",
        "Analisador (RTA)", "31 bandas de terço de oitava em dBFS, com retenção dos picos como em um analisador de estúdio",
        "Espectrograma", "Frequências ao longo do tempo. Quanto mais claro, mais alto o som",
        "Osciloscópio", "A forma de onda em tempo real, estabilizada por um disparo",
        "Latência de saída", "Ajusta o tempo do visualizador e das letras para sincronizá-los com esta saída", "Adiantar", "Atrasar", "Redefinir", "Visualizador de áudio",
        "Exportar equalizador", "Salve o equalizador atual como um arquivo AutoEq",
        "GraphicEQ", "Para apps que leem arquivos GraphicEQ", "ParametricEQ", "Para apps de equalização paramétrica",
        "Equalizador exportado", "Não foi possível exportar o equalizador"
    )
)

internal fun spectrumLocalizationCodes(): Set<String> = spectrumCopies.keys

internal fun spectrumCompleteLocalizationCodes(): Set<String> =
    spectrumCopies.filterValues { it.size == SpectrumText.entries.size }.keys

private val spectrumCopyObjects: Map<String, SpectrumCopy> by lazy {
    spectrumCopies.mapValues { (_, texts) -> SpectrumCopy(texts) }
}

internal val LevyraStrings.spectrumCopy: SpectrumCopy
    get() = localizedValueOrEnglish(spectrumCopyObjects, code)
