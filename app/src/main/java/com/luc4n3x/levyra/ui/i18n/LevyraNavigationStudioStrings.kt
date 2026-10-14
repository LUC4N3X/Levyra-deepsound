package com.luc4n3x.levyra.ui.i18n

enum class NavStudioText {
    Title, Subtitle, StyleSection, DefaultBadge, Selected,
    DescOriginal, DescSuperExpressive, DescFloating, DescMinimal, DescCompact,
    OriginalLocked, Destinations, DestinationsHint, Show, DragHandle,
    Labels, LabelsAlways, LabelsActive, LabelsNever,
    Indicator, IndicatorPill, IndicatorMorph, IndicatorDot, IndicatorLine,
    Size, IconSize, Height,
    Background, BgSolid, BgGradient, BgTranslucent,
    Effects, ExpressiveMotion, Intensity,
    ResetStyle, RestoreDefault, RestoreConfirm, Preview
}

class NavigationStudioCopy internal constructor(private val texts: Map<NavStudioText, String>) {
    operator fun get(key: NavStudioText): String = texts[key] ?: englishNavStudio.getValue(key)
}

private fun navStudio(vararg value: String): Map<NavStudioText, String> =
    if (value.size == NavStudioText.entries.size) NavStudioText.entries.zip(value).toMap() else emptyMap()

private val englishNavStudio: Map<NavStudioText, String> = navStudio(
    "Navigation bar", "Styles, buttons and labels", "Style", "Default", "Selected",
    "The original Levyra navigation bar", "Morphing indicator and spring motion", "A floating capsule above your content", "Clean icons, no extras", "Lower height, expressive shapes",
    "Levyra's original navigation bar stays exactly as it is. Pick another style to customize it.", "Buttons", "Hold the handle and drag to reorder. Home and Search are always shown.", "Show button", "Drag to reorder",
    "Labels", "Always", "Active tab only", "Hidden",
    "Active indicator", "Pill", "Morphing", "Dot", "Line",
    "Size", "Icon size", "Height",
    "Background", "Solid", "Gradient", "Translucent",
    "Effects", "Expressive animations", "Effect intensity",
    "Reset this style", "Restore default", "The navigation bar returns to Levyra's original design and every customization is removed.", "Preview"
)

private val navStudioCopies: Map<String, Map<NavStudioText, String>> = mapOf(
    "en" to englishNavStudio,
    "it" to navStudio(
        "Barra di navigazione", "Stili, pulsanti ed etichette", "Stile", "Predefinito", "Selezionato",
        "La barra di navigazione originale di Levyra", "Indicatore che si trasforma e animazioni elastiche", "Una capsula sospesa sopra i contenuti", "Icone pulite, nient'altro", "Altezza ridotta, forme espressive",
        "La barra originale di Levyra resta esattamente com'è. Scegli un altro stile per personalizzarla.", "Pulsanti", "Tieni premuta la maniglia e trascina per riordinare. Home e Cerca restano sempre visibili.", "Mostra pulsante", "Trascina per riordinare",
        "Etichette", "Sempre", "Solo scheda attiva", "Nascoste",
        "Indicatore attivo", "Pillola", "Trasformabile", "Punto", "Linea",
        "Dimensioni", "Dimensione icone", "Altezza",
        "Sfondo", "Pieno", "Sfumato", "Traslucido",
        "Effetti", "Animazioni espressive", "Intensità degli effetti",
        "Ripristina questo stile", "Ripristina predefinito", "La barra di navigazione torna al design originale di Levyra e ogni personalizzazione viene rimossa.", "Anteprima"
    ),
    "es" to navStudio(
        "Barra de navegación", "Estilos, botones y etiquetas", "Estilo", "Predeterminado", "Seleccionado",
        "La barra de navegación original de Levyra", "Indicador que se transforma y movimiento elástico", "Una cápsula flotante sobre el contenido", "Iconos limpios, nada más", "Menos altura, formas expresivas",
        "La barra original de Levyra se mantiene tal cual. Elige otro estilo para personalizarla.", "Botones", "Mantén pulsado el asa y arrastra para reordenar. Inicio y Buscar siempre se muestran.", "Mostrar botón", "Arrastra para reordenar",
        "Etiquetas", "Siempre", "Solo pestaña activa", "Ocultas",
        "Indicador activo", "Píldora", "Transformable", "Punto", "Línea",
        "Tamaño", "Tamaño de iconos", "Altura",
        "Fondo", "Sólido", "Degradado", "Translúcido",
        "Efectos", "Animaciones expresivas", "Intensidad de efectos",
        "Restablecer este estilo", "Restaurar predeterminado", "La barra de navegación vuelve al diseño original de Levyra y se eliminan todas las personalizaciones.", "Vista previa"
    ),
    "fr" to navStudio(
        "Barre de navigation", "Styles, boutons et libellés", "Style", "Par défaut", "Sélectionné",
        "La barre de navigation d'origine de Levyra", "Indicateur qui se transforme et mouvement élastique", "Une capsule flottante au-dessus du contenu", "Icônes épurées, rien de plus", "Moins haute, formes expressives",
        "La barre d'origine de Levyra reste telle quelle. Choisissez un autre style pour la personnaliser.", "Boutons", "Maintenez la poignée et faites glisser pour réorganiser. Accueil et Recherche restent toujours visibles.", "Afficher le bouton", "Glisser pour réorganiser",
        "Libellés", "Toujours", "Onglet actif seulement", "Masqués",
        "Indicateur actif", "Pilule", "Évolutif", "Point", "Ligne",
        "Taille", "Taille des icônes", "Hauteur",
        "Arrière-plan", "Uni", "Dégradé", "Translucide",
        "Effets", "Animations expressives", "Intensité des effets",
        "Réinitialiser ce style", "Restaurer par défaut", "La barre de navigation revient au design d'origine de Levyra et toutes les personnalisations sont supprimées.", "Aperçu"
    ),
    "de" to navStudio(
        "Navigationsleiste", "Stile, Tasten und Beschriftungen", "Stil", "Standard", "Ausgewählt",
        "Die originale Levyra-Navigationsleiste", "Wandelbarer Indikator und federnde Bewegung", "Eine schwebende Kapsel über deinen Inhalten", "Klare Symbole, sonst nichts", "Geringere Höhe, ausdrucksstarke Formen",
        "Die originale Leiste von Levyra bleibt genau so. Wähle einen anderen Stil, um sie anzupassen.", "Tasten", "Griff halten und ziehen zum Sortieren. Start und Suche werden immer angezeigt.", "Taste anzeigen", "Ziehen zum Sortieren",
        "Beschriftungen", "Immer", "Nur aktiver Tab", "Ausgeblendet",
        "Aktiver Indikator", "Pille", "Wandelbar", "Punkt", "Linie",
        "Größe", "Symbolgröße", "Höhe",
        "Hintergrund", "Einfarbig", "Verlauf", "Transluzent",
        "Effekte", "Ausdrucksstarke Animationen", "Effektstärke",
        "Diesen Stil zurücksetzen", "Standard wiederherstellen", "Die Navigationsleiste kehrt zum Originaldesign von Levyra zurück und alle Anpassungen werden entfernt.", "Vorschau"
    ),
    "pt" to navStudio(
        "Barra de navegação", "Estilos, botões e rótulos", "Estilo", "Padrão", "Selecionado",
        "A barra de navegação original do Levyra", "Indicador que se transforma e movimento elástico", "Uma cápsula flutuante sobre o conteúdo", "Ícones limpos, nada mais", "Menos altura, formas expressivas",
        "A barra original do Levyra continua exatamente igual. Escolha outro estilo para personalizar.", "Botões", "Segure a alça e arraste para reordenar. Início e Buscar sempre aparecem.", "Mostrar botão", "Arraste para reordenar",
        "Rótulos", "Sempre", "Só a aba ativa", "Ocultos",
        "Indicador ativo", "Pílula", "Transformável", "Ponto", "Linha",
        "Tamanho", "Tamanho dos ícones", "Altura",
        "Fundo", "Sólido", "Gradiente", "Translúcido",
        "Efeitos", "Animações expressivas", "Intensidade dos efeitos",
        "Redefinir este estilo", "Restaurar padrão", "A barra de navegação volta ao design original do Levyra e todas as personalizações são removidas.", "Prévia"
    ),
    "nl" to navStudio(
        "Navigatiebalk", "Stijlen, knoppen en labels", "Stijl", "Standaard", "Geselecteerd",
        "De originele navigatiebalk van Levyra", "Veranderende indicator en verende beweging", "Een zwevende capsule boven je content", "Strakke iconen, verder niets", "Lagere hoogte, expressieve vormen",
        "De originele balk van Levyra blijft precies zoals hij is. Kies een andere stijl om aan te passen.", "Knoppen", "Houd de greep vast en sleep om te ordenen. Home en Zoeken blijven altijd zichtbaar.", "Knop tonen", "Sleep om te ordenen",
        "Labels", "Altijd", "Alleen actief tabblad", "Verborgen",
        "Actieve indicator", "Pil", "Veranderend", "Punt", "Lijn",
        "Grootte", "Icoongrootte", "Hoogte",
        "Achtergrond", "Effen", "Verloop", "Doorschijnend",
        "Effecten", "Expressieve animaties", "Effectsterkte",
        "Deze stijl herstellen", "Standaard herstellen", "De navigatiebalk keert terug naar het originele ontwerp van Levyra en alle aanpassingen worden verwijderd.", "Voorbeeld"
    ),
    "pl" to navStudio(
        "Pasek nawigacji", "Style, przyciski i etykiety", "Styl", "Domyślny", "Wybrany",
        "Oryginalny pasek nawigacji Levyra", "Zmieniający się wskaźnik i sprężysty ruch", "Pływająca kapsuła nad treścią", "Czyste ikony, nic więcej", "Mniejsza wysokość, ekspresyjne kształty",
        "Oryginalny pasek Levyra pozostaje bez zmian. Wybierz inny styl, aby go dostosować.", "Przyciski", "Przytrzymaj uchwyt i przeciągnij, aby zmienić kolejność. Strona główna i Szukaj są zawsze widoczne.", "Pokaż przycisk", "Przeciągnij, aby zmienić kolejność",
        "Etykiety", "Zawsze", "Tylko aktywna karta", "Ukryte",
        "Aktywny wskaźnik", "Pigułka", "Zmienny", "Kropka", "Linia",
        "Rozmiar", "Rozmiar ikon", "Wysokość",
        "Tło", "Jednolite", "Gradient", "Półprzezroczyste",
        "Efekty", "Ekspresyjne animacje", "Intensywność efektów",
        "Resetuj ten styl", "Przywróć domyślny", "Pasek nawigacji wraca do oryginalnego projektu Levyra, a wszystkie zmiany zostają usunięte.", "Podgląd"
    ),
    "ro" to navStudio(
        "Bara de navigare", "Stiluri, butoane și etichete", "Stil", "Implicit", "Selectat",
        "Bara de navigare originală Levyra", "Indicator care se transformă și mișcare elastică", "O capsulă plutitoare deasupra conținutului", "Pictograme curate, nimic în plus", "Înălțime redusă, forme expresive",
        "Bara originală Levyra rămâne exact așa cum este. Alege alt stil pentru a o personaliza.", "Butoane", "Ține apăsat mânerul și trage pentru a reordona. Acasă și Căutare rămân mereu vizibile.", "Afișează butonul", "Trage pentru a reordona",
        "Etichete", "Mereu", "Doar fila activă", "Ascunse",
        "Indicator activ", "Pastilă", "Transformabil", "Punct", "Linie",
        "Dimensiune", "Dimensiunea pictogramelor", "Înălțime",
        "Fundal", "Uniform", "Gradient", "Translucid",
        "Efecte", "Animații expresive", "Intensitatea efectelor",
        "Resetează acest stil", "Restabilește implicit", "Bara de navigare revine la designul original Levyra și toate personalizările sunt eliminate.", "Previzualizare"
    ),
    "el" to navStudio(
        "Γραμμή πλοήγησης", "Στιλ, κουμπιά και ετικέτες", "Στιλ", "Προεπιλογή", "Επιλεγμένο",
        "Η αρχική γραμμή πλοήγησης του Levyra", "Δείκτης που μεταμορφώνεται και ελαστική κίνηση", "Μια αιωρούμενη κάψουλα πάνω από το περιεχόμενο", "Καθαρά εικονίδια, τίποτα παραπάνω", "Μικρότερο ύψος, εκφραστικά σχήματα",
        "Η αρχική γραμμή του Levyra μένει ακριβώς ίδια. Διάλεξε άλλο στιλ για να την προσαρμόσεις.", "Κουμπιά", "Κράτησε τη λαβή και σύρε για αναδιάταξη. Η Αρχική και η Αναζήτηση εμφανίζονται πάντα.", "Εμφάνιση κουμπιού", "Σύρε για αναδιάταξη",
        "Ετικέτες", "Πάντα", "Μόνο η ενεργή καρτέλα", "Κρυφές",
        "Ενεργός δείκτης", "Χάπι", "Μεταμορφούμενος", "Τελεία", "Γραμμή",
        "Μέγεθος", "Μέγεθος εικονιδίων", "Ύψος",
        "Φόντο", "Συμπαγές", "Διαβάθμιση", "Ημιδιάφανο",
        "Εφέ", "Εκφραστικά κινούμενα σχέδια", "Ένταση εφέ",
        "Επαναφορά αυτού του στιλ", "Επαναφορά προεπιλογής", "Η γραμμή πλοήγησης επιστρέφει στον αρχικό σχεδιασμό του Levyra και αφαιρούνται όλες οι προσαρμογές.", "Προεπισκόπηση"
    ),
    "sv" to navStudio(
        "Navigeringsfält", "Stilar, knappar och etiketter", "Stil", "Standard", "Vald",
        "Levyras ursprungliga navigeringsfält", "Formskiftande indikator och fjädrande rörelse", "En svävande kapsel ovanför innehållet", "Rena ikoner, inget mer", "Lägre höjd, uttrycksfulla former",
        "Levyras ursprungliga fält förblir exakt som det är. Välj en annan stil för att anpassa.", "Knappar", "Håll i handtaget och dra för att ändra ordning. Hem och Sök visas alltid.", "Visa knapp", "Dra för att ändra ordning",
        "Etiketter", "Alltid", "Endast aktiv flik", "Dolda",
        "Aktiv indikator", "Piller", "Formskiftande", "Punkt", "Linje",
        "Storlek", "Ikonstorlek", "Höjd",
        "Bakgrund", "Enfärgad", "Övertoning", "Genomskinlig",
        "Effekter", "Uttrycksfulla animationer", "Effektstyrka",
        "Återställ den här stilen", "Återställ standard", "Navigeringsfältet återgår till Levyras ursprungliga design och alla anpassningar tas bort.", "Förhandsvisning"
    ),
    "da" to navStudio(
        "Navigationslinje", "Stilarter, knapper og etiketter", "Stil", "Standard", "Valgt",
        "Levyras oprindelige navigationslinje", "Skiftende indikator og fjedrende bevægelse", "En svævende kapsel over indholdet", "Rene ikoner, intet mere", "Lavere højde, udtryksfulde former",
        "Levyras oprindelige linje forbliver præcis som den er. Vælg en anden stil for at tilpasse.", "Knapper", "Hold håndtaget og træk for at ændre rækkefølge. Hjem og Søg vises altid.", "Vis knap", "Træk for at ændre rækkefølge",
        "Etiketter", "Altid", "Kun aktiv fane", "Skjult",
        "Aktiv indikator", "Pille", "Skiftende", "Prik", "Linje",
        "Størrelse", "Ikonstørrelse", "Højde",
        "Baggrund", "Ensfarvet", "Gradient", "Gennemsigtig",
        "Effekter", "Udtryksfulde animationer", "Effektstyrke",
        "Nulstil denne stil", "Gendan standard", "Navigationslinjen vender tilbage til Levyras oprindelige design, og alle tilpasninger fjernes.", "Forhåndsvisning"
    ),
    "cs" to navStudio(
        "Navigační lišta", "Styly, tlačítka a popisky", "Styl", "Výchozí", "Vybráno",
        "Původní navigační lišta Levyra", "Proměnlivý indikátor a pružný pohyb", "Plovoucí kapsle nad obsahem", "Čisté ikony, nic navíc", "Nižší výška, výrazné tvary",
        "Původní lišta Levyra zůstává přesně taková, jaká je. Vyberte jiný styl a přizpůsobte ji.", "Tlačítka", "Podržte úchyt a přetažením změňte pořadí. Domů a Hledat jsou vždy zobrazeny.", "Zobrazit tlačítko", "Přetažením změníte pořadí",
        "Popisky", "Vždy", "Jen aktivní karta", "Skryté",
        "Aktivní indikátor", "Pilulka", "Proměnlivý", "Tečka", "Čára",
        "Velikost", "Velikost ikon", "Výška",
        "Pozadí", "Jednolité", "Přechod", "Průsvitné",
        "Efekty", "Výrazné animace", "Intenzita efektů",
        "Obnovit tento styl", "Obnovit výchozí", "Navigační lišta se vrátí k původnímu designu Levyra a všechna přizpůsobení budou odstraněna.", "Náhled"
    ),
    "sk" to navStudio(
        "Navigačná lišta", "Štýly, tlačidlá a popisy", "Štýl", "Predvolený", "Vybraný",
        "Pôvodná navigačná lišta Levyra", "Meniaci sa indikátor a pružný pohyb", "Plávajúca kapsula nad obsahom", "Čisté ikony, nič navyše", "Nižšia výška, výrazné tvary",
        "Pôvodná lišta Levyra zostáva presne taká, aká je. Vyberte iný štýl a prispôsobte ju.", "Tlačidlá", "Podržte úchyt a potiahnutím zmeňte poradie. Domov a Hľadať sú vždy zobrazené.", "Zobraziť tlačidlo", "Potiahnutím zmeníte poradie",
        "Popisy", "Vždy", "Len aktívna karta", "Skryté",
        "Aktívny indikátor", "Pilulka", "Meniaci sa", "Bodka", "Čiara",
        "Veľkosť", "Veľkosť ikon", "Výška",
        "Pozadie", "Jednofarebné", "Prechod", "Priesvitné",
        "Efekty", "Výrazné animácie", "Intenzita efektov",
        "Obnoviť tento štýl", "Obnoviť predvolený", "Navigačná lišta sa vráti k pôvodnému dizajnu Levyra a všetky úpravy sa odstránia.", "Náhľad"
    ),
    "bn" to navStudio(
        "নেভিগেশন বার", "স্টাইল, বোতাম ও লেবেল", "স্টাইল", "ডিফল্ট", "নির্বাচিত",
        "Levyra-র আসল নেভিগেশন বার", "রূপ বদলানো নির্দেশক ও স্প্রিং অ্যানিমেশন", "কনটেন্টের উপর ভাসমান ক্যাপসুল", "পরিষ্কার আইকন, বাড়তি কিছু নয়", "কম উচ্চতা, অভিব্যক্তিপূর্ণ আকার",
        "Levyra-র আসল বার ঠিক যেমন আছে তেমনই থাকবে। কাস্টমাইজ করতে অন্য স্টাইল বেছে নিন।", "বোতাম", "হ্যান্ডেল চেপে ধরে টেনে ক্রম বদলান। হোম ও সার্চ সবসময় দেখা যাবে।", "বোতাম দেখান", "টেনে ক্রম বদলান",
        "লেবেল", "সবসময়", "শুধু সক্রিয় ট্যাব", "লুকানো",
        "সক্রিয় নির্দেশক", "পিল", "রূপান্তরযোগ্য", "বিন্দু", "রেখা",
        "আকার", "আইকনের আকার", "উচ্চতা",
        "পটভূমি", "একরঙা", "গ্রেডিয়েন্ট", "স্বচ্ছ",
        "ইফেক্ট", "অভিব্যক্তিপূর্ণ অ্যানিমেশন", "ইফেক্টের তীব্রতা",
        "এই স্টাইল রিসেট করুন", "ডিফল্টে ফেরান", "নেভিগেশন বার Levyra-র আসল ডিজাইনে ফিরে যাবে এবং সব কাস্টমাইজেশন মুছে যাবে।", "প্রিভিউ"
    ),
    "kk" to navStudio(
        "Навигация жолағы", "Стильдер, түймелер және белгілер", "Стиль", "Әдепкі", "Таңдалған",
        "Levyra-ның түпнұсқа навигация жолағы", "Өзгермелі индикатор және серпімді қозғалыс", "Мазмұн үстіндегі қалқыма капсула", "Таза белгішелер, артық ештеңе жоқ", "Аласа биіктік, мәнерлі пішіндер",
        "Levyra-ның түпнұсқа жолағы дәл сол күйінде қалады. Баптау үшін басқа стиль таңдаңыз.", "Түймелер", "Тұтқаны басып тұрып, ретін өзгерту үшін сүйреңіз. Басты бет пен Іздеу әрқашан көрсетіледі.", "Түймені көрсету", "Ретін өзгерту үшін сүйреңіз",
        "Белгілер", "Әрқашан", "Тек белсенді қойынды", "Жасырын",
        "Белсенді индикатор", "Таблетка", "Өзгермелі", "Нүкте", "Сызық",
        "Өлшем", "Белгіше өлшемі", "Биіктік",
        "Фон", "Біркелкі", "Градиент", "Мөлдір",
        "Әсерлер", "Мәнерлі анимациялар", "Әсер қарқыны",
        "Осы стильді қалпына келтіру", "Әдепкіні қалпына келтіру", "Навигация жолағы Levyra-ның түпнұсқа дизайнына оралады және барлық баптаулар жойылады.", "Алдын ала қарау"
    ),
    "sr" to navStudio(
        "Трака за навигацију", "Стилови, дугмад и ознаке", "Стил", "Подразумевано", "Изабрано",
        "Оригинална Levyra трака за навигацију", "Индикатор који се мења и еластичан покрет", "Лебдећа капсула изнад садржаја", "Чисте иконе, ништа више", "Мања висина, изражајни облици",
        "Оригинална Levyra трака остаје тачно онаква каква јесте. Изаберите други стил да бисте је прилагодили.", "Дугмад", "Држите ручицу и превуците да промените редослед. Почетна и Претрага су увек приказане.", "Прикажи дугме", "Превуците да промените редослед",
        "Ознаке", "Увек", "Само активна картица", "Скривене",
        "Активни индикатор", "Пилула", "Променљив", "Тачка", "Линија",
        "Величина", "Величина икона", "Висина",
        "Позадина", "Једнобојна", "Прелаз", "Провидна",
        "Ефекти", "Изражајне анимације", "Јачина ефеката",
        "Ресетуј овај стил", "Врати подразумевано", "Трака за навигацију се враћа на оригинални Levyra дизајн и сва прилагођавања се уклањају.", "Преглед"
    ),
    "sq" to navStudio(
        "Shiriti i navigimit", "Stile, butona dhe etiketa", "Stili", "I parazgjedhur", "I zgjedhur",
        "Shiriti origjinal i navigimit të Levyra", "Tregues që transformohet dhe lëvizje elastike", "Një kapsulë pezull mbi përmbajtjen", "Ikona të pastra, asgjë tjetër", "Lartësi më e vogël, forma shprehëse",
        "Shiriti origjinal i Levyra mbetet saktësisht siç është. Zgjidh një stil tjetër për ta personalizuar.", "Butonat", "Mbaj dorezën dhe tërhiq për të rirenditur. Kreu dhe Kërko shfaqen gjithmonë.", "Shfaq butonin", "Tërhiq për të rirenditur",
        "Etiketat", "Gjithmonë", "Vetëm skeda aktive", "Të fshehura",
        "Treguesi aktiv", "Pilulë", "I ndryshueshëm", "Pikë", "Vijë",
        "Madhësia", "Madhësia e ikonave", "Lartësia",
        "Sfondi", "I njëngjyrshëm", "Gradient", "I tejdukshëm",
        "Efektet", "Animacione shprehëse", "Intensiteti i efekteve",
        "Rivendos këtë stil", "Rikthe të parazgjedhurin", "Shiriti i navigimit kthehet te dizajni origjinal i Levyra dhe çdo personalizim hiqet.", "Pamja paraprake"
    ),
    "hr" to navStudio(
        "Navigacijska traka", "Stilovi, gumbi i oznake", "Stil", "Zadano", "Odabrano",
        "Izvorna Levyra navigacijska traka", "Indikator koji se mijenja i elastičan pokret", "Lebdeća kapsula iznad sadržaja", "Čiste ikone, ništa više", "Manja visina, izražajni oblici",
        "Izvorna Levyra traka ostaje točno kakva jest. Odaberite drugi stil za prilagodbu.", "Gumbi", "Držite ručku i povucite za promjenu redoslijeda. Početna i Pretraživanje uvijek su prikazani.", "Prikaži gumb", "Povucite za promjenu redoslijeda",
        "Oznake", "Uvijek", "Samo aktivna kartica", "Skrivene",
        "Aktivni indikator", "Pilula", "Promjenjiv", "Točka", "Linija",
        "Veličina", "Veličina ikona", "Visina",
        "Pozadina", "Jednobojna", "Prijelaz", "Prozirna",
        "Efekti", "Izražajne animacije", "Jačina efekata",
        "Vrati ovaj stil", "Vrati zadano", "Navigacijska traka vraća se na izvorni Levyra dizajn i sve prilagodbe se uklanjaju.", "Pregled"
    ),
    "bg" to navStudio(
        "Лента за навигация", "Стилове, бутони и етикети", "Стил", "По подразбиране", "Избран",
        "Оригиналната лента за навигация на Levyra", "Променящ се индикатор и еластично движение", "Носеща се капсула над съдържанието", "Чисти икони, нищо повече", "По-ниска височина, изразителни форми",
        "Оригиналната лента на Levyra остава точно такава, каквато е. Изберете друг стил, за да я персонализирате.", "Бутони", "Задръжте дръжката и плъзнете, за да пренаредите. Начало и Търсене винаги се показват.", "Показване на бутона", "Плъзнете, за да пренаредите",
        "Етикети", "Винаги", "Само активният раздел", "Скрити",
        "Активен индикатор", "Хапче", "Променящ се", "Точка", "Линия",
        "Размер", "Размер на иконите", "Височина",
        "Фон", "Плътен", "Преливка", "Полупрозрачен",
        "Ефекти", "Изразителни анимации", "Сила на ефектите",
        "Нулиране на този стил", "Възстановяване по подразбиране", "Лентата за навигация се връща към оригиналния дизайн на Levyra и всички персонализации се премахват.", "Преглед"
    ),
    "hu" to navStudio(
        "Navigációs sáv", "Stílusok, gombok és feliratok", "Stílus", "Alapértelmezett", "Kiválasztva",
        "A Levyra eredeti navigációs sávja", "Átalakuló jelző és rugalmas mozgás", "Lebegő kapszula a tartalom felett", "Letisztult ikonok, semmi más", "Kisebb magasság, kifejező formák",
        "A Levyra eredeti sávja pontosan olyan marad, amilyen. Válassz másik stílust a testreszabáshoz.", "Gombok", "Tartsd lenyomva a fogantyút és húzd az átrendezéshez. A Kezdőlap és a Keresés mindig látható.", "Gomb megjelenítése", "Húzd az átrendezéshez",
        "Feliratok", "Mindig", "Csak az aktív lap", "Rejtett",
        "Aktív jelző", "Pirula", "Átalakuló", "Pont", "Vonal",
        "Méret", "Ikonméret", "Magasság",
        "Háttér", "Egyszínű", "Színátmenet", "Áttetsző",
        "Effektek", "Kifejező animációk", "Effektek erőssége",
        "Stílus visszaállítása", "Alapértelmezett visszaállítása", "A navigációs sáv visszatér a Levyra eredeti dizájnjához, és minden testreszabás törlődik.", "Előnézet"
    ),
    "fi" to navStudio(
        "Navigointipalkki", "Tyylit, painikkeet ja tekstit", "Tyyli", "Oletus", "Valittu",
        "Levyran alkuperäinen navigointipalkki", "Muuntuva ilmaisin ja joustava liike", "Leijuva kapseli sisällön päällä", "Selkeät kuvakkeet, ei muuta", "Matalampi, ilmeikkäät muodot",
        "Levyran alkuperäinen palkki pysyy täsmälleen ennallaan. Valitse toinen tyyli mukauttaaksesi.", "Painikkeet", "Pidä kahvaa painettuna ja vedä järjestääksesi. Koti ja Haku näkyvät aina.", "Näytä painike", "Vedä järjestääksesi",
        "Tekstit", "Aina", "Vain aktiivinen välilehti", "Piilotettu",
        "Aktiivinen ilmaisin", "Pilleri", "Muuntuva", "Piste", "Viiva",
        "Koko", "Kuvakkeiden koko", "Korkeus",
        "Tausta", "Yksivärinen", "Liukuväri", "Läpikuultava",
        "Tehosteet", "Ilmeikkäät animaatiot", "Tehosteiden voimakkuus",
        "Palauta tämä tyyli", "Palauta oletus", "Navigointipalkki palaa Levyran alkuperäiseen muotoiluun ja kaikki mukautukset poistetaan.", "Esikatselu"
    ),
    "et" to navStudio(
        "Navigeerimisriba", "Stiilid, nupud ja sildid", "Stiil", "Vaikimisi", "Valitud",
        "Levyra algne navigeerimisriba", "Muutuv indikaator ja vetruv liikumine", "Hõljuv kapsel sisu kohal", "Puhtad ikoonid, ei midagi muud", "Madalam kõrgus, ilmekad kujud",
        "Levyra algne riba jääb täpselt selliseks, nagu see on. Kohandamiseks vali mõni muu stiil.", "Nupud", "Hoia käepidet ja lohista järjestuse muutmiseks. Avaleht ja Otsing on alati nähtaval.", "Näita nuppu", "Lohista järjestuse muutmiseks",
        "Sildid", "Alati", "Ainult aktiivne vaheleht", "Peidetud",
        "Aktiivne indikaator", "Pill", "Muutuv", "Punkt", "Joon",
        "Suurus", "Ikoonide suurus", "Kõrgus",
        "Taust", "Ühevärviline", "Üleminek", "Poolläbipaistev",
        "Efektid", "Ilmekad animatsioonid", "Efektide tugevus",
        "Lähtesta see stiil", "Taasta vaikimisi", "Navigeerimisriba naaseb Levyra algse kujunduse juurde ja kõik kohandused eemaldatakse.", "Eelvaade"
    ),
    "nb" to navStudio(
        "Navigasjonslinje", "Stiler, knapper og etiketter", "Stil", "Standard", "Valgt",
        "Levyras opprinnelige navigasjonslinje", "Skiftende indikator og fjærende bevegelse", "En svevende kapsel over innholdet", "Rene ikoner, ingenting mer", "Lavere høyde, uttrykksfulle former",
        "Levyras opprinnelige linje forblir nøyaktig som den er. Velg en annen stil for å tilpasse.", "Knapper", "Hold håndtaket og dra for å endre rekkefølge. Hjem og Søk vises alltid.", "Vis knapp", "Dra for å endre rekkefølge",
        "Etiketter", "Alltid", "Bare aktiv fane", "Skjult",
        "Aktiv indikator", "Pille", "Skiftende", "Prikk", "Linje",
        "Størrelse", "Ikonstørrelse", "Høyde",
        "Bakgrunn", "Ensfarget", "Gradient", "Gjennomsiktig",
        "Effekter", "Uttrykksfulle animasjoner", "Effektstyrke",
        "Tilbakestill denne stilen", "Gjenopprett standard", "Navigasjonslinjen går tilbake til Levyras opprinnelige design, og alle tilpasninger fjernes.", "Forhåndsvisning"
    ),
    "ca" to navStudio(
        "Barra de navegació", "Estils, botons i etiquetes", "Estil", "Predeterminat", "Seleccionat",
        "La barra de navegació original de Levyra", "Indicador que es transforma i moviment elàstic", "Una càpsula flotant sobre el contingut", "Icones netes, res més", "Menys alçada, formes expressives",
        "La barra original de Levyra es manté tal com és. Tria un altre estil per personalitzar-la.", "Botons", "Mantén premuda la nansa i arrossega per reordenar. Inici i Cerca sempre es mostren.", "Mostra el botó", "Arrossega per reordenar",
        "Etiquetes", "Sempre", "Només la pestanya activa", "Ocultes",
        "Indicador actiu", "Píndola", "Transformable", "Punt", "Línia",
        "Mida", "Mida de les icones", "Alçada",
        "Fons", "Sòlid", "Degradat", "Translúcid",
        "Efectes", "Animacions expressives", "Intensitat dels efectes",
        "Restableix aquest estil", "Restaura el predeterminat", "La barra de navegació torna al disseny original de Levyra i s'eliminen totes les personalitzacions.", "Previsualització"
    ),
    "uk" to navStudio(
        "Панель навігації", "Стилі, кнопки та підписи", "Стиль", "За замовчуванням", "Вибрано",
        "Оригінальна панель навігації Levyra", "Мінливий індикатор і пружний рух", "Капсула, що ширяє над вмістом", "Чисті значки, нічого зайвого", "Менша висота, виразні форми",
        "Оригінальна панель Levyra залишається точно такою, як є. Виберіть інший стиль, щоб налаштувати її.", "Кнопки", "Утримуйте маркер і перетягніть, щоб змінити порядок. Головна й Пошук показуються завжди.", "Показати кнопку", "Перетягніть, щоб змінити порядок",
        "Підписи", "Завжди", "Лише активна вкладка", "Приховані",
        "Активний індикатор", "Пігулка", "Мінливий", "Крапка", "Лінія",
        "Розмір", "Розмір значків", "Висота",
        "Фон", "Суцільний", "Градієнт", "Напівпрозорий",
        "Ефекти", "Виразні анімації", "Інтенсивність ефектів",
        "Скинути цей стиль", "Відновити за замовчуванням", "Панель навігації повернеться до оригінального дизайну Levyra, а всі налаштування буде видалено.", "Попередній перегляд"
    ),
    "ru" to navStudio(
        "Панель навигации", "Стили, кнопки и подписи", "Стиль", "По умолчанию", "Выбран",
        "Оригинальная панель навигации Levyra", "Меняющийся индикатор и пружинное движение", "Парящая капсула над контентом", "Чистые значки, ничего лишнего", "Меньшая высота, выразительные формы",
        "Оригинальная панель Levyra остаётся точно такой, как есть. Выберите другой стиль, чтобы настроить её.", "Кнопки", "Удерживайте маркер и перетащите, чтобы изменить порядок. Главная и Поиск показываются всегда.", "Показать кнопку", "Перетащите, чтобы изменить порядок",
        "Подписи", "Всегда", "Только активная вкладка", "Скрыты",
        "Активный индикатор", "Пилюля", "Меняющийся", "Точка", "Линия",
        "Размер", "Размер значков", "Высота",
        "Фон", "Сплошной", "Градиент", "Полупрозрачный",
        "Эффекты", "Выразительные анимации", "Интенсивность эффектов",
        "Сбросить этот стиль", "Восстановить по умолчанию", "Панель навигации вернётся к оригинальному дизайну Levyra, а все настройки будут удалены.", "Предпросмотр"
    ),
    "tr" to navStudio(
        "Gezinme çubuğu", "Stiller, düğmeler ve etiketler", "Stil", "Varsayılan", "Seçili",
        "Levyra'nın orijinal gezinme çubuğu", "Dönüşen gösterge ve yaylı hareket", "İçeriğin üzerinde yüzen bir kapsül", "Sade simgeler, fazlası yok", "Daha alçak, etkileyici şekiller",
        "Levyra'nın orijinal çubuğu olduğu gibi kalır. Özelleştirmek için başka bir stil seç.", "Düğmeler", "Tutamacı basılı tutup sürükleyerek sırala. Ana sayfa ve Ara her zaman görünür.", "Düğmeyi göster", "Sıralamak için sürükle",
        "Etiketler", "Her zaman", "Yalnızca etkin sekme", "Gizli",
        "Etkin gösterge", "Hap", "Dönüşen", "Nokta", "Çizgi",
        "Boyut", "Simge boyutu", "Yükseklik",
        "Arka plan", "Düz", "Gradyan", "Yarı saydam",
        "Efektler", "Etkileyici animasyonlar", "Efekt yoğunluğu",
        "Bu stili sıfırla", "Varsayılanı geri yükle", "Gezinme çubuğu Levyra'nın orijinal tasarımına döner ve tüm özelleştirmeler kaldırılır.", "Önizleme"
    ),
    "ar" to navStudio(
        "شريط التنقل", "أنماط وأزرار وتسميات", "النمط", "الافتراضي", "المحدد",
        "شريط التنقل الأصلي في Levyra", "مؤشر متحوّل وحركة نابضة", "كبسولة عائمة فوق المحتوى", "أيقونات نظيفة دون إضافات", "ارتفاع أقل وأشكال معبّرة",
        "يبقى شريط Levyra الأصلي كما هو تمامًا. اختر نمطًا آخر لتخصيصه.", "الأزرار", "اضغط مطولًا على المقبض واسحب لإعادة الترتيب. الرئيسية والبحث يظهران دائمًا.", "إظهار الزر", "اسحب لإعادة الترتيب",
        "التسميات", "دائمًا", "التبويب النشط فقط", "مخفية",
        "المؤشر النشط", "كبسولة", "متحوّل", "نقطة", "خط",
        "الحجم", "حجم الأيقونات", "الارتفاع",
        "الخلفية", "سادة", "متدرجة", "شفافة",
        "التأثيرات", "رسوم متحركة معبّرة", "شدة التأثيرات",
        "إعادة ضبط هذا النمط", "استعادة الافتراضي", "يعود شريط التنقل إلى تصميم Levyra الأصلي وتُزال كل التخصيصات.", "معاينة"
    ),
    "fa" to navStudio(
        "نوار پیمایش", "سبک‌ها، دکمه‌ها و برچسب‌ها", "سبک", "پیش‌فرض", "انتخاب‌شده",
        "نوار پیمایش اصلی Levyra", "نشانگر دگرگون‌شونده و حرکت فنری", "کپسولی شناور روی محتوا", "نمادهای تمیز، بدون اضافه", "ارتفاع کمتر، شکل‌های گویا",
        "نوار اصلی Levyra دقیقاً همان‌طور که هست باقی می‌ماند. برای سفارشی‌سازی سبک دیگری انتخاب کنید.", "دکمه‌ها", "دستگیره را نگه دارید و برای تغییر ترتیب بکشید. خانه و جست‌وجو همیشه نمایش داده می‌شوند.", "نمایش دکمه", "برای تغییر ترتیب بکشید",
        "برچسب‌ها", "همیشه", "فقط زبانه فعال", "پنهان",
        "نشانگر فعال", "قرص", "دگرگون‌شونده", "نقطه", "خط",
        "اندازه", "اندازه نمادها", "ارتفاع",
        "پس‌زمینه", "یکدست", "گرادیان", "نیمه‌شفاف",
        "جلوه‌ها", "پویانمایی‌های گویا", "شدت جلوه‌ها",
        "بازنشانی این سبک", "بازگرداندن پیش‌فرض", "نوار پیمایش به طراحی اصلی Levyra برمی‌گردد و همه سفارشی‌سازی‌ها حذف می‌شوند.", "پیش‌نمایش"
    ),
    "zh" to navStudio(
        "导航栏", "风格、按钮与标签", "风格", "默认", "已选择",
        "Levyra 原版导航栏", "变形指示器与弹性动效", "悬浮在内容之上的胶囊", "简洁图标，别无其他", "更低高度，更具表现力的形状",
        "Levyra 原版导航栏将保持原样。选择其他风格即可自定义。", "按钮", "按住手柄拖动即可排序。首页和搜索始终显示。", "显示按钮", "拖动排序",
        "标签", "始终显示", "仅当前标签", "隐藏",
        "当前指示器", "胶囊", "变形", "圆点", "线条",
        "尺寸", "图标大小", "高度",
        "背景", "纯色", "渐变", "半透明",
        "效果", "表现力动效", "效果强度",
        "重置此风格", "恢复默认", "导航栏将恢复为 Levyra 原版设计，并移除所有自定义设置。", "预览"
    ),
    "zh-Hant" to navStudio(
        "導覽列", "風格、按鈕與標籤", "風格", "預設", "已選取",
        "Levyra 原版導覽列", "變形指示器與彈性動態", "懸浮在內容之上的膠囊", "簡潔圖示，別無其他", "更低高度，更具表現力的形狀",
        "Levyra 原版導覽列將維持原樣。選擇其他風格即可自訂。", "按鈕", "按住把手拖曳即可排序。首頁和搜尋一律顯示。", "顯示按鈕", "拖曳排序",
        "標籤", "一律顯示", "僅目前分頁", "隱藏",
        "目前指示器", "膠囊", "變形", "圓點", "線條",
        "尺寸", "圖示大小", "高度",
        "背景", "純色", "漸層", "半透明",
        "效果", "表現力動態", "效果強度",
        "重設此風格", "還原預設", "導覽列將還原為 Levyra 原版設計，並移除所有自訂設定。", "預覽"
    ),
    "ja" to navStudio(
        "ナビゲーションバー", "スタイル・ボタン・ラベル", "スタイル", "デフォルト", "選択中",
        "Levyra オリジナルのナビゲーションバー", "変形するインジケーターと弾むモーション", "コンテンツの上に浮かぶカプセル", "すっきりしたアイコンだけ", "低めの高さと表情豊かな形",
        "Levyra のオリジナルのバーはそのまま残ります。カスタマイズするには別のスタイルを選んでください。", "ボタン", "ハンドルを長押ししてドラッグすると並べ替えられます。ホームと検索は常に表示されます。", "ボタンを表示", "ドラッグして並べ替え",
        "ラベル", "常に表示", "選択中のタブのみ", "非表示",
        "選択インジケーター", "ピル", "変形", "ドット", "ライン",
        "サイズ", "アイコンのサイズ", "高さ",
        "背景", "単色", "グラデーション", "半透明",
        "エフェクト", "表情豊かなアニメーション", "エフェクトの強さ",
        "このスタイルをリセット", "デフォルトに戻す", "ナビゲーションバーは Levyra のオリジナルデザインに戻り、すべてのカスタマイズが削除されます。", "プレビュー"
    ),
    "ko" to navStudio(
        "탐색 바", "스타일, 버튼 및 라벨", "스타일", "기본값", "선택됨",
        "Levyra 오리지널 탐색 바", "변하는 표시기와 탄성 있는 모션", "콘텐츠 위에 떠 있는 캡슐", "깔끔한 아이콘만", "낮은 높이, 표현력 있는 모양",
        "Levyra의 오리지널 바는 그대로 유지됩니다. 맞춤 설정하려면 다른 스타일을 선택하세요.", "버튼", "핸들을 길게 누른 채 드래그해 순서를 바꾸세요. 홈과 검색은 항상 표시됩니다.", "버튼 표시", "드래그하여 순서 변경",
        "라벨", "항상", "선택한 탭만", "숨김",
        "활성 표시기", "알약", "변형", "점", "선",
        "크기", "아이콘 크기", "높이",
        "배경", "단색", "그라데이션", "반투명",
        "효과", "표현력 있는 애니메이션", "효과 강도",
        "이 스타일 초기화", "기본값 복원", "탐색 바가 Levyra 오리지널 디자인으로 돌아가고 모든 맞춤 설정이 삭제됩니다.", "미리보기"
    ),
    "hi" to navStudio(
        "नेविगेशन बार", "स्टाइल, बटन और लेबल", "स्टाइल", "डिफ़ॉल्ट", "चुना गया",
        "Levyra का मूल नेविगेशन बार", "बदलता इंडिकेटर और स्प्रिंग मोशन", "कंटेंट के ऊपर तैरता कैप्सूल", "साफ़ आइकन, और कुछ नहीं", "कम ऊँचाई, अभिव्यंजक आकार",
        "Levyra का मूल बार बिल्कुल वैसा ही रहेगा। कस्टमाइज़ करने के लिए कोई दूसरा स्टाइल चुनें।", "बटन", "हैंडल दबाकर रखें और क्रम बदलने के लिए खींचें। होम और सर्च हमेशा दिखते हैं।", "बटन दिखाएँ", "क्रम बदलने के लिए खींचें",
        "लेबल", "हमेशा", "सिर्फ़ सक्रिय टैब", "छिपे हुए",
        "सक्रिय इंडिकेटर", "पिल", "बदलने वाला", "बिंदु", "रेखा",
        "आकार", "आइकन का आकार", "ऊँचाई",
        "पृष्ठभूमि", "ठोस", "ग्रेडिएंट", "पारभासी",
        "इफ़ेक्ट", "अभिव्यंजक एनिमेशन", "इफ़ेक्ट की तीव्रता",
        "यह स्टाइल रीसेट करें", "डिफ़ॉल्ट पुनर्स्थापित करें", "नेविगेशन बार Levyra के मूल डिज़ाइन पर लौट आएगा और सभी कस्टमाइज़ेशन हट जाएँगे।", "पूर्वावलोकन"
    ),
    "id" to navStudio(
        "Bilah navigasi", "Gaya, tombol, dan label", "Gaya", "Default", "Dipilih",
        "Bilah navigasi asli Levyra", "Indikator yang berubah bentuk dan gerakan pegas", "Kapsul melayang di atas konten", "Ikon bersih, tanpa tambahan", "Lebih rendah, bentuk ekspresif",
        "Bilah asli Levyra tetap persis seperti apa adanya. Pilih gaya lain untuk menyesuaikannya.", "Tombol", "Tahan pegangan lalu seret untuk mengurutkan. Beranda dan Cari selalu ditampilkan.", "Tampilkan tombol", "Seret untuk mengurutkan",
        "Label", "Selalu", "Hanya tab aktif", "Tersembunyi",
        "Indikator aktif", "Pil", "Berubah bentuk", "Titik", "Garis",
        "Ukuran", "Ukuran ikon", "Tinggi",
        "Latar belakang", "Solid", "Gradien", "Tembus pandang",
        "Efek", "Animasi ekspresif", "Intensitas efek",
        "Setel ulang gaya ini", "Pulihkan default", "Bilah navigasi kembali ke desain asli Levyra dan semua penyesuaian dihapus.", "Pratinjau"
    ),
    "ms" to navStudio(
        "Bar navigasi", "Gaya, butang dan label", "Gaya", "Lalai", "Dipilih",
        "Bar navigasi asal Levyra", "Penunjuk yang berubah bentuk dan gerakan pegas", "Kapsul terapung di atas kandungan", "Ikon kemas, tiada tambahan", "Lebih rendah, bentuk ekspresif",
        "Bar asal Levyra kekal seperti sedia ada. Pilih gaya lain untuk menyesuaikannya.", "Butang", "Tahan pemegang dan seret untuk menyusun semula. Laman utama dan Cari sentiasa dipaparkan.", "Tunjukkan butang", "Seret untuk menyusun semula",
        "Label", "Sentiasa", "Tab aktif sahaja", "Tersembunyi",
        "Penunjuk aktif", "Pil", "Berubah bentuk", "Titik", "Garisan",
        "Saiz", "Saiz ikon", "Ketinggian",
        "Latar belakang", "Padu", "Gradien", "Lut sinar",
        "Kesan", "Animasi ekspresif", "Keamatan kesan",
        "Set semula gaya ini", "Pulihkan lalai", "Bar navigasi kembali kepada reka bentuk asal Levyra dan semua penyesuaian dibuang.", "Pratonton"
    ),
    "vi" to navStudio(
        "Thanh điều hướng", "Kiểu, nút và nhãn", "Kiểu", "Mặc định", "Đã chọn",
        "Thanh điều hướng gốc của Levyra", "Chỉ báo biến đổi và chuyển động đàn hồi", "Viên nang lơ lửng phía trên nội dung", "Biểu tượng gọn gàng, không gì thêm", "Thấp hơn, hình dạng biểu cảm",
        "Thanh gốc của Levyra được giữ nguyên. Hãy chọn kiểu khác để tùy chỉnh.", "Nút", "Giữ tay cầm và kéo để sắp xếp lại. Trang chủ và Tìm kiếm luôn hiển thị.", "Hiện nút", "Kéo để sắp xếp lại",
        "Nhãn", "Luôn hiện", "Chỉ tab đang chọn", "Ẩn",
        "Chỉ báo đang chọn", "Viên thuốc", "Biến đổi", "Chấm", "Đường",
        "Kích thước", "Kích thước biểu tượng", "Chiều cao",
        "Nền", "Đơn sắc", "Chuyển sắc", "Trong mờ",
        "Hiệu ứng", "Hoạt ảnh biểu cảm", "Cường độ hiệu ứng",
        "Đặt lại kiểu này", "Khôi phục mặc định", "Thanh điều hướng trở về thiết kế gốc của Levyra và mọi tùy chỉnh sẽ bị xóa.", "Xem trước"
    ),
    "th" to navStudio(
        "แถบนำทาง", "สไตล์ ปุ่ม และป้ายกำกับ", "สไตล์", "ค่าเริ่มต้น", "เลือกอยู่",
        "แถบนำทางดั้งเดิมของ Levyra", "ตัวบ่งชี้ที่เปลี่ยนรูปและการเคลื่อนไหวแบบสปริง", "แคปซูลลอยเหนือเนื้อหา", "ไอคอนเรียบง่าย ไม่มีส่วนเกิน", "เตี้ยลง รูปทรงสื่ออารมณ์",
        "แถบดั้งเดิมของ Levyra จะคงไว้ตามเดิม เลือกสไตล์อื่นเพื่อปรับแต่ง", "ปุ่ม", "กดค้างที่จับแล้วลากเพื่อจัดลำดับ หน้าแรกและค้นหาจะแสดงเสมอ", "แสดงปุ่ม", "ลากเพื่อจัดลำดับ",
        "ป้ายกำกับ", "แสดงเสมอ", "เฉพาะแท็บที่เลือก", "ซ่อน",
        "ตัวบ่งชี้ที่เลือก", "แคปซูล", "เปลี่ยนรูป", "จุด", "เส้น",
        "ขนาด", "ขนาดไอคอน", "ความสูง",
        "พื้นหลัง", "สีทึบ", "ไล่สี", "โปร่งแสง",
        "เอฟเฟกต์", "แอนิเมชันแบบสื่ออารมณ์", "ความแรงของเอฟเฟกต์",
        "รีเซ็ตสไตล์นี้", "คืนค่าเริ่มต้น", "แถบนำทางจะกลับไปใช้ดีไซน์ดั้งเดิมของ Levyra และการปรับแต่งทั้งหมดจะถูกลบ", "ตัวอย่าง"
    ),
    "fil" to navStudio(
        "Navigation bar", "Mga istilo, button at label", "Istilo", "Default", "Napili",
        "Ang orihinal na navigation bar ng Levyra", "Nagbabagong indicator at malambot na galaw", "Lumulutang na kapsula sa ibabaw ng content", "Malinis na icon, wala nang iba", "Mas mababa, mas makulay na hugis",
        "Mananatili ang orihinal na bar ng Levyra. Pumili ng ibang istilo para i-customize ito.", "Mga button", "Pindutin nang matagal ang handle at i-drag para ayusin ang pagkakasunod. Laging makikita ang Home at Search.", "Ipakita ang button", "I-drag para ayusin",
        "Mga label", "Palagi", "Aktibong tab lang", "Nakatago",
        "Aktibong indicator", "Pill", "Nagbabago", "Tuldok", "Linya",
        "Laki", "Laki ng icon", "Taas",
        "Background", "Solid", "Gradient", "Malinaw",
        "Mga effect", "Masiglang animation", "Lakas ng effect",
        "I-reset ang istilong ito", "Ibalik ang default", "Babalik ang navigation bar sa orihinal na disenyo ng Levyra at aalisin ang lahat ng customization.", "Preview"
    ),
    "he" to navStudio(
        "סרגל ניווט", "סגנונות, כפתורים ותוויות", "סגנון", "ברירת מחדל", "נבחר",
        "סרגל הניווט המקורי של Levyra", "מחוון משתנה ותנועה קפיצית", "קפסולה מרחפת מעל התוכן", "סמלים נקיים, בלי תוספות", "גובה נמוך יותר, צורות הבעתיות",
        "הסרגל המקורי של Levyra נשאר בדיוק כפי שהוא. בחר סגנון אחר כדי להתאים אותו.", "כפתורים", "לחץ לחיצה ארוכה על הידית וגרור כדי לסדר. דף הבית והחיפוש מוצגים תמיד.", "הצגת הכפתור", "גרור כדי לסדר",
        "תוויות", "תמיד", "רק הלשונית הפעילה", "מוסתרות",
        "מחוון פעיל", "גלולה", "משתנה", "נקודה", "קו",
        "גודל", "גודל הסמלים", "גובה",
        "רקע", "אחיד", "מעבר צבע", "שקוף למחצה",
        "אפקטים", "אנימציות הבעתיות", "עוצמת האפקטים",
        "איפוס סגנון זה", "שחזור ברירת המחדל", "סרגל הניווט יחזור לעיצוב המקורי של Levyra וכל ההתאמות האישיות יוסרו.", "תצוגה מקדימה"
    )
)

private val navStudioCopyObjects: Map<String, NavigationStudioCopy> by lazy {
    navStudioCopies.mapValues { (_, texts) -> NavigationStudioCopy(texts) }
}

internal fun navigationStudioLocalizationCodes(): Set<String> = navStudioCopies.keys

internal fun navigationStudioCompleteLocalizationCodes(): Set<String> =
    navStudioCopies.filterValues { it.size == NavStudioText.entries.size }.keys

internal val LevyraStrings.navigationStudioCopy: NavigationStudioCopy
    get() = navStudioCopyObjects[code] ?: navStudioCopyObjects.getValue("en")
