package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.MoodStyle

class MoodStyleCopy internal constructor(private val values: List<String>) {
    val title: String get() = values[0]
    val mixed: String get() = values[1]
    val hint: String get() = values[2]
    val loading: String get() = values[3]
    val empty: String get() = values[4]
    val tone: String get() = values[5]
    val toneLow: String get() = values[6]
    val toneHigh: String get() = values[7]
    val toneHint: String get() = values[8]
    val pointDescription: String get() = values[9]
    val groupToggle: String get() = values[10]
    val groupClear: String get() = values[11]

    fun name(style: MoodStyle): String = values[FIXED_ENTRIES + style.ordinal]

    internal companion object {
        const val FIXED_ENTRIES = 12
    }
}

private fun moodStyle(
    title: String,
    mixed: String,
    hint: String,
    loading: String,
    empty: String,
    tone: String,
    toneLow: String,
    toneHigh: String,
    toneHint: String,
    pointDescription: String,
    groupToggle: String,
    groupClear: String,
    names: String
): List<String> = listOf(title, mixed, hint, loading, empty, tone, toneLow, toneHigh, toneHint, pointDescription, groupToggle, groupClear) +
    names.split('|')

private val moodStyleCopies: Map<String, List<String>> = mapOf(
    "en" to moodStyle(
        "Style", "Mixed", "Pick one or more styles to blend, or keep Mixed",
        "Gathering songs for these styles…", "Not enough songs for these styles yet. Try Mixed or add another style.",
        "Tone", "Melancholic", "Joyful",
        "The height of the curve is energy. Tone is how sad or joyful the songs feel at this point. Both only choose songs: they never change speed or pitch.",
        "Point {n}: energy {e}%, tone {b}%",
        "Move together", "Clear selection",
        "Pop|Rock|Hip-hop|Electronic|R&B|Jazz|Classical|Reggae|Folk|Country|Disco|New age|Latin|Metal|Blues"
    ),
    "it" to moodStyle(
        "Stile", "Misto", "Scegli uno o più stili da combinare, oppure lascia Misto",
        "Cerco brani per questi stili…", "Non ci sono ancora abbastanza brani per questi stili. Prova Misto o aggiungi un altro stile.",
        "Tono", "Malinconico", "Gioioso",
        "L’altezza della curva è l’energia. Il tono è quanto i brani suonano malinconici o gioiosi in questo punto. Entrambi scelgono solo i brani: non cambiano mai velocità o intonazione.",
        "Punto {n}: energia {e}%, tono {b}%",
        "Sposta insieme", "Annulla selezione",
        "Pop|Rock|Hip-hop|Elettronica|R&B|Jazz|Classica|Reggae|Folk|Country|Disco|New age|Latina|Metal|Blues"
    ),
    "es" to moodStyle(
        "Estilo", "Mixto", "Elige uno o varios estilos para mezclar, o deja Mixto",
        "Buscando canciones de estos estilos…", "Aún no hay suficientes canciones de estos estilos. Prueba Mixto o añade otro estilo.",
        "Tono", "Melancólico", "Alegre",
        "La altura de la curva es la energía. El tono indica lo melancólicas o alegres que suenan las canciones en este punto. Ambos solo eligen canciones: nunca cambian la velocidad ni el tono musical.",
        "Punto {n}: energía {e}%, tono {b}%",
        "Mover juntos", "Borrar selección",
        "Pop|Rock|Hip-hop|Electrónica|R&B|Jazz|Clásica|Reggae|Folk|Country|Disco|New age|Latina|Metal|Blues"
    ),
    "fr" to moodStyle(
        "Style", "Mixte", "Choisissez un ou plusieurs styles à mélanger, ou gardez Mixte",
        "Recherche de titres pour ces styles…", "Pas encore assez de titres pour ces styles. Essayez Mixte ou ajoutez un autre style.",
        "Ton", "Mélancolique", "Joyeux",
        "La hauteur de la courbe est l’énergie. Le ton indique si les titres sonnent mélancoliques ou joyeux à ce point. Les deux choisissent seulement les titres : ils ne changent jamais la vitesse ni la hauteur.",
        "Point {n} : énergie {e} %, ton {b} %",
        "Déplacer ensemble", "Effacer la sélection",
        "Pop|Rock|Hip-hop|Électro|R&B|Jazz|Classique|Reggae|Folk|Country|Disco|New age|Latino|Metal|Blues"
    ),
    "de" to moodStyle(
        "Stil", "Gemischt", "Wähle einen oder mehrere Stile zum Mischen oder bleib bei Gemischt",
        "Suche Songs für diese Stile…", "Noch nicht genug Songs für diese Stile. Versuche Gemischt oder füge einen weiteren Stil hinzu.",
        "Stimmung", "Melancholisch", "Fröhlich",
        "Die Höhe der Kurve ist die Energie. Die Stimmung zeigt, wie melancholisch oder fröhlich die Songs an dieser Stelle klingen. Beides wählt nur Songs aus und ändert nie Tempo oder Tonhöhe.",
        "Punkt {n}: Energie {e} %, Stimmung {b} %",
        "Gemeinsam verschieben", "Auswahl aufheben",
        "Pop|Rock|Hip-Hop|Elektronik|R&B|Jazz|Klassik|Reggae|Folk|Country|Disco|New Age|Latin|Metal|Blues"
    ),
    "pt" to moodStyle(
        "Estilo", "Misto", "Escolha um ou mais estilos para misturar, ou mantenha Misto",
        "A procurar músicas destes estilos…", "Ainda não há músicas suficientes destes estilos. Experimente Misto ou adicione outro estilo.",
        "Tom", "Melancólico", "Alegre",
        "A altura da curva é a energia. O tom indica o quão melancólicas ou alegres as músicas soam neste ponto. Ambos só escolhem músicas: nunca mudam a velocidade nem a afinação.",
        "Ponto {n}: energia {e}%, tom {b}%",
        "Mover em conjunto", "Limpar seleção",
        "Pop|Rock|Hip-hop|Eletrónica|R&B|Jazz|Clássica|Reggae|Folk|Country|Disco|New age|Latina|Metal|Blues"
    ),
    "nl" to moodStyle(
        "Stijl", "Gemengd", "Kies een of meer stijlen om te mengen, of houd Gemengd",
        "Nummers zoeken voor deze stijlen…", "Nog niet genoeg nummers voor deze stijlen. Probeer Gemengd of voeg een andere stijl toe.",
        "Toon", "Melancholisch", "Vrolijk",
        "De hoogte van de curve is de energie. De toon is hoe melancholisch of vrolijk de nummers op dit punt klinken. Beide kiezen alleen nummers en veranderen nooit tempo of toonhoogte.",
        "Punt {n}: energie {e}%, toon {b}%",
        "Samen verplaatsen", "Selectie wissen",
        "Pop|Rock|Hiphop|Elektronisch|R&B|Jazz|Klassiek|Reggae|Folk|Country|Disco|New age|Latin|Metal|Blues"
    ),
    "pl" to moodStyle(
        "Styl", "Mieszany", "Wybierz jeden lub kilka stylów do połączenia albo zostaw Mieszany",
        "Szukam utworów w tych stylach…", "Za mało utworów w tych stylach. Wybierz Mieszany lub dodaj inny styl.",
        "Ton", "Melancholijny", "Radosny",
        "Wysokość krzywej to energia. Ton określa, jak melancholijnie lub radośnie brzmią utwory w tym punkcie. Oba tylko wybierają utwory i nigdy nie zmieniają tempa ani wysokości dźwięku.",
        "Punkt {n}: energia {e}%, ton {b}%",
        "Przesuwaj razem", "Wyczyść zaznaczenie",
        "Pop|Rock|Hip-hop|Elektronika|R&B|Jazz|Klasyczna|Reggae|Folk|Country|Disco|New age|Latino|Metal|Blues"
    ),
    "ro" to moodStyle(
        "Stil", "Mixt", "Alege unul sau mai multe stiluri de combinat sau păstrează Mixt",
        "Caut melodii pentru aceste stiluri…", "Încă nu sunt destule melodii pentru aceste stiluri. Încearcă Mixt sau adaugă alt stil.",
        "Ton", "Melancolic", "Vesel",
        "Înălțimea curbei este energia. Tonul arată cât de melancolice sau vesele sună melodiile în acest punct. Ambele aleg doar melodii și nu schimbă niciodată viteza sau înălțimea sunetului.",
        "Punctul {n}: energie {e}%, ton {b}%",
        "Mută împreună", "Șterge selecția",
        "Pop|Rock|Hip-hop|Electronică|R&B|Jazz|Clasică|Reggae|Folk|Country|Disco|New age|Latino|Metal|Blues"
    ),
    "el" to moodStyle(
        "Στυλ", "Μικτό", "Επίλεξε ένα ή περισσότερα στυλ για συνδυασμό ή κράτησε το Μικτό",
        "Αναζήτηση τραγουδιών για αυτά τα στυλ…", "Δεν υπάρχουν ακόμη αρκετά τραγούδια για αυτά τα στυλ. Δοκίμασε το Μικτό ή πρόσθεσε άλλο στυλ.",
        "Τόνος", "Μελαγχολικό", "Χαρούμενο",
        "Το ύψος της καμπύλης είναι η ενέργεια. Ο τόνος δείχνει πόσο μελαγχολικά ή χαρούμενα ακούγονται τα τραγούδια σε αυτό το σημείο. Και τα δύο απλώς επιλέγουν τραγούδια και δεν αλλάζουν ποτέ ταχύτητα ή τονικό ύψος.",
        "Σημείο {n}: ενέργεια {e}%, τόνος {b}%",
        "Μετακίνηση μαζί", "Απαλοιφή επιλογής",
        "Ποπ|Ροκ|Χιπ-χοπ|Ηλεκτρονική|R&B|Τζαζ|Κλασική|Ρέγκε|Φολκ|Κάντρι|Ντίσκο|New age|Λάτιν|Μέταλ|Μπλουζ"
    ),
    "sv" to moodStyle(
        "Stil", "Blandat", "Välj en eller flera stilar att blanda, eller behåll Blandat",
        "Hämtar låtar för de här stilarna…", "Inte tillräckligt med låtar för de här stilarna ännu. Prova Blandat eller lägg till en annan stil.",
        "Ton", "Melankolisk", "Glad",
        "Kurvans höjd är energin. Tonen är hur melankoliska eller glada låtarna känns vid den här punkten. Båda väljer bara låtar och ändrar aldrig tempo eller tonhöjd.",
        "Punkt {n}: energi {e} %, ton {b} %",
        "Flytta tillsammans", "Rensa markering",
        "Pop|Rock|Hiphop|Elektroniskt|R&B|Jazz|Klassiskt|Reggae|Folk|Country|Disco|New age|Latin|Metal|Blues"
    ),
    "da" to moodStyle(
        "Stil", "Blandet", "Vælg en eller flere stilarter at blande, eller behold Blandet",
        "Finder numre til disse stilarter…", "Ikke nok numre til disse stilarter endnu. Prøv Blandet eller tilføj en anden stilart.",
        "Tone", "Melankolsk", "Glad",
        "Kurvens højde er energien. Tonen er, hvor melankolske eller glade numrene føles på dette punkt. Begge vælger kun numre og ændrer aldrig tempo eller tonehøjde.",
        "Punkt {n}: energi {e} %, tone {b} %",
        "Flyt sammen", "Ryd markering",
        "Pop|Rock|Hiphop|Elektronisk|R&B|Jazz|Klassisk|Reggae|Folk|Country|Disco|New age|Latin|Metal|Blues"
    ),
    "cs" to moodStyle(
        "Styl", "Mix", "Vyberte jeden nebo více stylů ke smíchání, nebo nechte Mix",
        "Hledám skladby pro tyto styly…", "Pro tyto styly zatím není dost skladeb. Zkuste Mix nebo přidejte další styl.",
        "Tón", "Melancholický", "Radostný",
        "Výška křivky je energie. Tón určuje, jak melancholicky nebo radostně skladby v tomto bodě zní. Obojí jen vybírá skladby a nikdy nemění tempo ani výšku tónu.",
        "Bod {n}: energie {e} %, tón {b} %",
        "Posouvat společně", "Zrušit výběr",
        "Pop|Rock|Hip-hop|Elektronika|R&B|Jazz|Klasika|Reggae|Folk|Country|Disco|New age|Latino|Metal|Blues"
    ),
    "sk" to moodStyle(
        "Štýl", "Mix", "Vyberte jeden alebo viac štýlov na zmiešanie, alebo nechajte Mix",
        "Hľadám skladby pre tieto štýly…", "Pre tieto štýly zatiaľ nie je dosť skladieb. Skúste Mix alebo pridajte ďalší štýl.",
        "Tón", "Melancholický", "Radostný",
        "Výška krivky je energia. Tón určuje, ako melancholicky alebo radostne skladby v tomto bode znejú. Oboje len vyberá skladby a nikdy nemení tempo ani výšku tónu.",
        "Bod {n}: energia {e} %, tón {b} %",
        "Posúvať spolu", "Zrušiť výber",
        "Pop|Rock|Hip-hop|Elektronika|R&B|Jazz|Klasika|Reggae|Folk|Country|Disco|New age|Latino|Metal|Blues"
    ),
    "hr" to moodStyle(
        "Stil", "Miješano", "Odaberi jedan ili više stilova za miješanje ili ostavi Miješano",
        "Tražim pjesme za ove stilove…", "Još nema dovoljno pjesama za ove stilove. Probaj Miješano ili dodaj drugi stil.",
        "Ton", "Melankolično", "Radosno",
        "Visina krivulje je energija. Ton pokazuje koliko pjesme u ovoj točki zvuče melankolično ili radosno. Oboje samo bira pjesme i nikad ne mijenja tempo ni visinu tona.",
        "Točka {n}: energija {e} %, ton {b} %",
        "Pomakni zajedno", "Poništi odabir",
        "Pop|Rock|Hip-hop|Elektronika|R&B|Jazz|Klasika|Reggae|Folk|Country|Disco|New age|Latino|Metal|Blues"
    ),
    "sr" to moodStyle(
        "Стил", "Мешано", "Изабери један или више стилова за мешање или остави Мешано",
        "Тражим песме за ове стилове…", "Још нема довољно песама за ове стилове. Пробај Мешано или додај други стил.",
        "Тон", "Меланхолично", "Радосно",
        "Висина криве је енергија. Тон показује колико песме у овој тачки звуче меланхолично или радосно. Обоје само бира песме и никад не мења темпо ни висину тона.",
        "Тачка {n}: енергија {e} %, тон {b} %",
        "Помери заједно", "Обриши избор",
        "Поп|Рок|Хип-хоп|Електроника|R&B|Џез|Класика|Реге|Фолк|Кантри|Диско|Њу ејџ|Латино|Метал|Блуз"
    ),
    "sq" to moodStyle(
        "Stili", "I përzier", "Zgjidh një ose më shumë stile për t’i përzier, ose mbaj I përzier",
        "Po kërkoj këngë për këto stile…", "Ende nuk ka mjaftueshëm këngë për këto stile. Provo I përzier ose shto një stil tjetër.",
        "Toni", "Melankolik", "I gëzueshëm",
        "Lartësia e kurbës është energjia. Toni tregon sa melankolike ose të gëzueshme tingëllojnë këngët në këtë pikë. Të dyja vetëm zgjedhin këngë dhe nuk ndryshojnë kurrë shpejtësinë ose lartësinë e zërit.",
        "Pika {n}: energjia {e}%, toni {b}%",
        "Lëvizi së bashku", "Pastro përzgjedhjen",
        "Pop|Rock|Hip-hop|Elektronike|R&B|Xhaz|Klasike|Reggae|Folk|Country|Disko|New age|Latine|Metal|Blues"
    ),
    "bg" to moodStyle(
        "Стил", "Смесено", "Изберете един или повече стилове за смесване или оставете Смесено",
        "Търся песни за тези стилове…", "Все още няма достатъчно песни за тези стилове. Опитайте Смесено или добавете друг стил.",
        "Тон", "Меланхолично", "Радостно",
        "Височината на кривата е енергията. Тонът показва колко меланхолично или радостно звучат песните в тази точка. И двете само избират песни и никога не променят темпото или височината на звука.",
        "Точка {n}: енергия {e}%, тон {b}%",
        "Премести заедно", "Изчисти избора",
        "Поп|Рок|Хип-хоп|Електронна|R&B|Джаз|Класическа|Реге|Фолк|Кънтри|Диско|Ню ейдж|Латино|Метъл|Блус"
    ),
    "hu" to moodStyle(
        "Stílus", "Vegyes", "Válassz egy vagy több stílust a keveréshez, vagy maradj a Vegyesnél",
        "Dalok keresése ezekhez a stílusokhoz…", "Még nincs elég dal ezekhez a stílusokhoz. Próbáld a Vegyest, vagy adj hozzá másik stílust.",
        "Hangulat", "Melankolikus", "Vidám",
        "A görbe magassága az energia. A hangulat azt mutatja, mennyire melankolikusan vagy vidáman szólnak a dalok ezen a ponton. Mindkettő csak dalokat választ, a tempót és a hangmagasságot sosem változtatja meg.",
        "{n}. pont: energia {e}%, hangulat {b}%",
        "Együtt mozgatás", "Kijelölés törlése",
        "Pop|Rock|Hiphop|Elektronikus|R&B|Jazz|Klasszikus|Reggae|Folk|Country|Disco|New age|Latin|Metal|Blues"
    ),
    "fi" to moodStyle(
        "Tyyli", "Sekoitus", "Valitse yksi tai useampi tyyli sekoitettavaksi tai pidä Sekoitus",
        "Haetaan kappaleita näille tyyleille…", "Näille tyyleille ei vielä löydy tarpeeksi kappaleita. Kokeile Sekoitusta tai lisää toinen tyyli.",
        "Sävy", "Melankolinen", "Iloinen",
        "Käyrän korkeus on energia. Sävy kertoo, kuinka melankolisilta tai iloisilta kappaleet tuntuvat tässä kohdassa. Molemmat vain valitsevat kappaleita eivätkä koskaan muuta tempoa tai sävelkorkeutta.",
        "Piste {n}: energia {e} %, sävy {b} %",
        "Siirrä yhdessä", "Tyhjennä valinta",
        "Pop|Rock|Hiphop|Elektroninen|R&B|Jazz|Klassinen|Reggae|Folk|Country|Disco|New age|Latin|Metalli|Blues"
    ),
    "et" to moodStyle(
        "Stiil", "Segamini", "Vali segamiseks üks või mitu stiili või jäta Segamini",
        "Otsin nende stiilide lugusid…", "Nende stiilide jaoks pole veel piisavalt lugusid. Proovi Segamini või lisa veel üks stiil.",
        "Toon", "Melanhoolne", "Rõõmus",
        "Kõvera kõrgus on energia. Toon näitab, kui melanhoolsed või rõõmsad lood selles punktis tunduvad. Mõlemad ainult valivad lugusid ega muuda kunagi tempot ega helikõrgust.",
        "Punkt {n}: energia {e}%, toon {b}%",
        "Liiguta koos", "Tühjenda valik",
        "Pop|Rock|Hip-hop|Elektrooniline|R&B|Džäss|Klassika|Reggae|Folk|Kantri|Disko|New age|Ladina|Metal|Bluus"
    ),
    "nb" to moodStyle(
        "Stil", "Blandet", "Velg én eller flere stiler å blande, eller behold Blandet",
        "Henter låter for disse stilene…", "Ikke nok låter for disse stilene ennå. Prøv Blandet eller legg til en annen stil.",
        "Tone", "Melankolsk", "Glad",
        "Kurvens høyde er energien. Tonen er hvor melankolske eller glade låtene føles på dette punktet. Begge velger bare låter og endrer aldri tempo eller tonehøyde.",
        "Punkt {n}: energi {e} %, tone {b} %",
        "Flytt sammen", "Fjern markering",
        "Pop|Rock|Hiphop|Elektronisk|R&B|Jazz|Klassisk|Reggae|Folk|Country|Disco|New age|Latin|Metal|Blues"
    ),
    "ca" to moodStyle(
        "Estil", "Mixt", "Tria un o més estils per barrejar, o deixa Mixt",
        "Cercant cançons d’aquests estils…", "Encara no hi ha prou cançons d’aquests estils. Prova Mixt o afegeix un altre estil.",
        "To", "Melancòlic", "Alegre",
        "L’alçada de la corba és l’energia. El to indica com de melancòliques o alegres sonen les cançons en aquest punt. Tots dos només trien cançons: mai no canvien la velocitat ni l’afinació.",
        "Punt {n}: energia {e}%, to {b}%",
        "Mou junts", "Esborra la selecció",
        "Pop|Rock|Hip-hop|Electrònica|R&B|Jazz|Clàssica|Reggae|Folk|Country|Disco|New age|Llatina|Metal|Blues"
    ),
    "uk" to moodStyle(
        "Стиль", "Змішаний", "Оберіть один або кілька стилів для поєднання або залиште «Змішаний»",
        "Шукаю треки для цих стилів…", "Поки що замало треків для цих стилів. Спробуйте «Змішаний» або додайте інший стиль.",
        "Тон", "Сумний", "Радісний",
        "Висота кривої — це енергія. Тон показує, наскільки сумно чи радісно звучать треки в цій точці. Обидва лише добирають треки й ніколи не змінюють темп чи висоту звуку.",
        "Точка {n}: енергія {e}%, тон {b}%",
        "Рухати разом", "Скасувати вибір",
        "Поп|Рок|Хіп-хоп|Електроніка|R&B|Джаз|Класика|Реггі|Фолк|Кантрі|Диско|Нью-ейдж|Латино|Метал|Блюз"
    ),
    "ru" to moodStyle(
        "Стиль", "Смешанный", "Выберите один или несколько стилей для смешивания или оставьте «Смешанный»",
        "Подбираю треки этих стилей…", "Пока мало треков этих стилей. Попробуйте «Смешанный» или добавьте другой стиль.",
        "Тон", "Грустный", "Радостный",
        "Высота кривой — это энергия. Тон показывает, насколько грустно или радостно звучат треки в этой точке. Оба параметра только подбирают треки и никогда не меняют темп или высоту звука.",
        "Точка {n}: энергия {e}%, тон {b}%",
        "Двигать вместе", "Снять выделение",
        "Поп|Рок|Хип-хоп|Электроника|R&B|Джаз|Классика|Регги|Фолк|Кантри|Диско|Нью-эйдж|Латино|Метал|Блюз"
    ),
    "kk" to moodStyle(
        "Стиль", "Аралас", "Араластыру үшін бір немесе бірнеше стильді таңдаңыз не «Аралас» қалдырыңыз",
        "Осы стильдердің әндерін іздеп жатырмын…", "Бұл стильдерге әлі әндер жеткіліксіз. «Аралас» таңдаңыз не басқа стиль қосыңыз.",
        "Тон", "Мұңды", "Қуанышты",
        "Қисықтың биіктігі — энергия. Тон осы нүктеде әндердің қаншалықты мұңды не қуанышты естілетінін көрсетеді. Екеуі де тек ән таңдайды, қарқын мен дыбыс биіктігін ешқашан өзгертпейді.",
        "{n}-нүкте: энергия {e}%, тон {b}%",
        "Бірге жылжыту", "Таңдауды тазалау",
        "Поп|Рок|Хип-хоп|Электроника|R&B|Джаз|Классика|Регги|Фолк|Кантри|Диско|Нью-эйдж|Латино|Метал|Блюз"
    ),
    "tr" to moodStyle(
        "Tarz", "Karışık", "Karıştırmak için bir veya daha fazla tarz seç ya da Karışık’ta bırak",
        "Bu tarzlar için şarkılar aranıyor…", "Bu tarzlar için henüz yeterli şarkı yok. Karışık’ı dene ya da başka bir tarz ekle.",
        "Ton", "Hüzünlü", "Neşeli",
        "Eğrinin yüksekliği enerjidir. Ton, şarkıların bu noktada ne kadar hüzünlü ya da neşeli hissettirdiğidir. İkisi de yalnızca şarkı seçer; hızı ya da perdeyi asla değiştirmez.",
        "Nokta {n}: enerji %{e}, ton %{b}",
        "Birlikte taşı", "Seçimi temizle",
        "Pop|Rock|Hip-hop|Elektronik|R&B|Caz|Klasik|Reggae|Folk|Country|Disko|New age|Latin|Metal|Blues"
    ),
    "ar" to moodStyle(
        "الأسلوب", "مختلط", "اختر أسلوبًا واحدًا أو أكثر لمزجها، أو اترك «مختلط»",
        "جارٍ جمع أغانٍ لهذه الأساليب…", "لا توجد أغانٍ كافية لهذه الأساليب بعد. جرّب «مختلط» أو أضف أسلوبًا آخر.",
        "النبرة", "حزينة", "مبهجة",
        "ارتفاع المنحنى هو الطاقة. النبرة تعني مدى حزن الأغاني أو بهجتها في هذه النقطة. كلاهما يختار الأغاني فقط ولا يغيّر أبدًا السرعة أو طبقة الصوت.",
        "النقطة {n}: الطاقة {e}٪، النبرة {b}٪",
        "تحريك معًا", "مسح التحديد",
        "بوب|روك|هيب هوب|إلكترونية|R&B|جاز|كلاسيكية|ريغي|فولك|كانتري|ديسكو|نيو إيج|لاتينية|ميتال|بلوز"
    ),
    "fa" to moodStyle(
        "سبک", "ترکیبی", "یک یا چند سبک را برای ترکیب انتخاب کنید یا «ترکیبی» را نگه دارید",
        "در حال یافتن آهنگ برای این سبک‌ها…", "هنوز آهنگ کافی برای این سبک‌ها نیست. «ترکیبی» را امتحان کنید یا سبک دیگری اضافه کنید.",
        "حس", "غمگین", "شاد",
        "ارتفاع منحنی انرژی است. حس نشان می‌دهد آهنگ‌ها در این نقطه چقدر غمگین یا شاد به نظر می‌رسند. هر دو فقط آهنگ انتخاب می‌کنند و هرگز سرعت یا زیر و بمی را تغییر نمی‌دهند.",
        "نقطه {n}: انرژی {e}٪، حس {b}٪",
        "جابه‌جایی با هم", "پاک کردن انتخاب",
        "پاپ|راک|هیپ‌هاپ|الکترونیک|R&B|جاز|کلاسیک|رگی|فولک|کانتری|دیسکو|نیوایج|لاتین|متال|بلوز"
    ),
    "zh" to moodStyle(
        "风格", "混合", "选择一种或多种风格混合，或保持“混合”",
        "正在收集这些风格的歌曲…", "这些风格的歌曲还不够。试试“混合”或再添加一种风格。",
        "基调", "忧郁", "欢快",
        "曲线高度代表能量。基调表示此处歌曲听起来有多忧郁或欢快。两者都只用于选歌，绝不会改变速度或音高。",
        "第 {n} 个点：能量 {e}%，基调 {b}%",
        "一起移动", "清除选择",
        "流行|摇滚|嘻哈|电子|R&B|爵士|古典|雷鬼|民谣|乡村|迪斯科|新世纪|拉丁|金属|布鲁斯"
    ),
    "zh-Hant" to moodStyle(
        "風格", "混合", "選擇一種或多種風格混合，或保持「混合」",
        "正在收集這些風格的歌曲…", "這些風格的歌曲還不夠。試試「混合」或再加入一種風格。",
        "基調", "憂鬱", "歡快",
        "曲線高度代表能量。基調表示此處歌曲聽起來有多憂鬱或歡快。兩者都只用於選歌，絕不會改變速度或音高。",
        "第 {n} 個點：能量 {e}%，基調 {b}%",
        "一起移動", "清除選取",
        "流行|搖滾|嘻哈|電子|R&B|爵士|古典|雷鬼|民謠|鄉村|迪斯可|新世紀|拉丁|金屬|藍調"
    ),
    "ja" to moodStyle(
        "スタイル", "ミックス", "組み合わせるスタイルを1つ以上選ぶか、ミックスのままにします",
        "これらのスタイルの曲を集めています…", "これらのスタイルの曲がまだ足りません。ミックスにするか、別のスタイルを追加してください。",
        "トーン", "切ない", "明るい",
        "カーブの高さはエネルギーです。トーンはこの位置で曲がどれだけ切なく、または明るく感じられるかです。どちらも選曲のためだけに使われ、速さや音程は変わりません。",
        "ポイント {n}：エネルギー {e}%、トーン {b}%",
        "まとめて動かす", "選択を解除",
        "ポップ|ロック|ヒップホップ|エレクトロニック|R&B|ジャズ|クラシック|レゲエ|フォーク|カントリー|ディスコ|ニューエイジ|ラテン|メタル|ブルース"
    ),
    "ko" to moodStyle(
        "스타일", "믹스", "섞을 스타일을 하나 이상 고르거나 믹스로 두세요",
        "이 스타일의 곡을 모으는 중…", "이 스타일의 곡이 아직 부족해요. 믹스를 선택하거나 다른 스타일을 추가하세요.",
        "톤", "슬픔", "밝음",
        "곡선의 높이는 에너지예요. 톤은 이 지점에서 곡이 얼마나 슬프거나 밝게 느껴지는지예요. 둘 다 곡을 고르는 데만 쓰이며 속도나 음정은 절대 바뀌지 않아요.",
        "{n}번 지점: 에너지 {e}%, 톤 {b}%",
        "함께 이동", "선택 해제",
        "팝|록|힙합|일렉트로닉|R&B|재즈|클래식|레게|포크|컨트리|디스코|뉴에이지|라틴|메탈|블루스"
    ),
    "hi" to moodStyle(
        "शैली", "मिश्रित", "मिलाने के लिए एक या अधिक शैलियाँ चुनें, या मिश्रित रहने दें",
        "इन शैलियों के गाने खोजे जा रहे हैं…", "इन शैलियों के अभी पर्याप्त गाने नहीं हैं। मिश्रित आज़माएँ या कोई और शैली जोड़ें।",
        "टोन", "उदास", "खुशनुमा",
        "वक्र की ऊँचाई ऊर्जा है। टोन बताता है कि इस बिंदु पर गाने कितने उदास या खुशनुमा लगते हैं। दोनों सिर्फ़ गाने चुनते हैं और कभी गति या सुर नहीं बदलते।",
        "बिंदु {n}: ऊर्जा {e}%, टोन {b}%",
        "साथ में खिसकाएँ", "चयन हटाएँ",
        "पॉप|रॉक|हिप-हॉप|इलेक्ट्रॉनिक|R&B|जैज़|क्लासिकल|रेगे|फ़ोक|कंट्री|डिस्को|न्यू एज|लैटिन|मेटल|ब्लूज़"
    ),
    "bn" to moodStyle(
        "স্টাইল", "মিশ্র", "মেশাতে এক বা একাধিক স্টাইল বেছে নিন, অথবা মিশ্র রাখুন",
        "এই স্টাইলগুলোর গান খোঁজা হচ্ছে…", "এই স্টাইলগুলোর যথেষ্ট গান এখনও নেই। মিশ্র চেষ্টা করুন বা অন্য স্টাইল যোগ করুন।",
        "টোন", "বিষণ্ণ", "আনন্দময়",
        "বক্ররেখার উচ্চতা হলো এনার্জি। টোন বোঝায় এই বিন্দুতে গানগুলো কতটা বিষণ্ণ বা আনন্দময় লাগে। দুটোই শুধু গান বাছাই করে, কখনও গতি বা সুর বদলায় না।",
        "বিন্দু {n}: এনার্জি {e}%, টোন {b}%",
        "একসঙ্গে সরান", "নির্বাচন মুছুন",
        "পপ|রক|হিপ-হপ|ইলেকট্রনিক|R&B|জ্যাজ|ক্লাসিক্যাল|রেগে|ফোক|কান্ট্রি|ডিস্কো|নিউ এজ|লাতিন|মেটাল|ব্লুজ"
    ),
    "id" to moodStyle(
        "Gaya", "Campuran", "Pilih satu atau beberapa gaya untuk dipadukan, atau biarkan Campuran",
        "Mengumpulkan lagu untuk gaya ini…", "Belum cukup lagu untuk gaya ini. Coba Campuran atau tambahkan gaya lain.",
        "Nuansa", "Sendu", "Ceria",
        "Tinggi kurva adalah energi. Nuansa menunjukkan seberapa sendu atau ceria lagu terasa di titik ini. Keduanya hanya memilih lagu dan tidak pernah mengubah tempo atau nada.",
        "Titik {n}: energi {e}%, nuansa {b}%",
        "Geser bersama", "Hapus pilihan",
        "Pop|Rock|Hip-hop|Elektronik|R&B|Jazz|Klasik|Reggae|Folk|Country|Disko|New age|Latin|Metal|Blues"
    ),
    "ms" to moodStyle(
        "Gaya", "Campuran", "Pilih satu atau lebih gaya untuk digabungkan, atau kekalkan Campuran",
        "Mengumpul lagu untuk gaya ini…", "Belum cukup lagu untuk gaya ini. Cuba Campuran atau tambah gaya lain.",
        "Nada", "Sayu", "Ceria",
        "Ketinggian lengkung ialah tenaga. Nada menunjukkan sejauh mana lagu terasa sayu atau ceria pada titik ini. Kedua-duanya hanya memilih lagu dan tidak pernah mengubah tempo atau pic.",
        "Titik {n}: tenaga {e}%, nada {b}%",
        "Alih bersama", "Kosongkan pilihan",
        "Pop|Rock|Hip-hop|Elektronik|R&B|Jazz|Klasik|Reggae|Folk|Country|Disko|New age|Latin|Metal|Blues"
    ),
    "vi" to moodStyle(
        "Phong cách", "Kết hợp", "Chọn một hoặc nhiều phong cách để pha trộn, hoặc giữ Kết hợp",
        "Đang tìm bài hát cho các phong cách này…", "Chưa đủ bài hát cho các phong cách này. Hãy thử Kết hợp hoặc thêm phong cách khác.",
        "Sắc thái", "Buồn", "Vui",
        "Độ cao của đường cong là năng lượng. Sắc thái cho biết bài hát ở điểm này buồn hay vui đến mức nào. Cả hai chỉ dùng để chọn bài và không bao giờ đổi tốc độ hay cao độ.",
        "Điểm {n}: năng lượng {e}%, sắc thái {b}%",
        "Di chuyển cùng nhau", "Bỏ chọn",
        "Pop|Rock|Hip-hop|Điện tử|R&B|Jazz|Cổ điển|Reggae|Dân ca|Đồng quê|Disco|New age|Latin|Metal|Blues"
    ),
    "th" to moodStyle(
        "สไตล์", "ผสม", "เลือกสไตล์อย่างน้อยหนึ่งแบบเพื่อผสม หรือคงไว้ที่ผสม",
        "กำลังรวบรวมเพลงของสไตล์เหล่านี้…", "ยังมีเพลงของสไตล์เหล่านี้ไม่พอ ลองเลือกผสมหรือเพิ่มสไตล์อื่น",
        "โทน", "เศร้า", "สดใส",
        "ความสูงของเส้นโค้งคือพลังงาน โทนคือความเศร้าหรือสดใสของเพลงที่จุดนี้ ทั้งสองใช้เลือกเพลงเท่านั้นและไม่เปลี่ยนความเร็วหรือระดับเสียง",
        "จุดที่ {n}: พลังงาน {e}%, โทน {b}%",
        "ย้ายพร้อมกัน", "ล้างการเลือก",
        "ป๊อป|ร็อก|ฮิปฮอป|อิเล็กทรอนิกส์|R&B|แจ๊ส|คลาสสิก|เร็กเก้|โฟล์ก|คันทรี|ดิสโก้|นิวเอจ|ละติน|เมทัล|บลูส์"
    ),
    "fil" to moodStyle(
        "Estilo", "Halo", "Pumili ng isa o higit pang estilo na pagsasamahin, o hayaang Halo",
        "Naghahanap ng mga kanta para sa mga estilong ito…", "Wala pang sapat na kanta para sa mga estilong ito. Subukan ang Halo o magdagdag ng ibang estilo.",
        "Tono", "Malungkot", "Masaya",
        "Ang taas ng kurba ay ang enerhiya. Ang tono ay kung gaano kalungkot o kasaya ang dating ng mga kanta sa puntong ito. Pareho lang silang pumipili ng kanta at hindi kailanman binabago ang bilis o tono ng tunog.",
        "Punto {n}: enerhiya {e}%, tono {b}%",
        "Igalaw nang sabay", "I-clear ang pinili",
        "Pop|Rock|Hip-hop|Electronic|R&B|Jazz|Classical|Reggae|Folk|Country|Disco|New age|Latin|Metal|Blues"
    ),
    "he" to moodStyle(
        "סגנון", "מעורב", "בחרו סגנון אחד או יותר לשילוב, או השאירו מעורב",
        "אוספים שירים בסגנונות האלה…", "עדיין אין מספיק שירים בסגנונות האלה. נסו מעורב או הוסיפו סגנון אחר.",
        "גוון", "עצוב", "שמח",
        "גובה העקומה הוא האנרגיה. הגוון מראה עד כמה השירים נשמעים עצובים או שמחים בנקודה הזו. שניהם רק בוחרים שירים ולעולם לא משנים מהירות או גובה צליל.",
        "נקודה {n}: אנרגיה {e}%, גוון {b}%",
        "הזזה יחד", "ניקוי הבחירה",
        "פופ|רוק|היפ־הופ|אלקטרוני|R&B|ג׳אז|קלאסי|רגאיי|פולק|קאנטרי|דיסקו|ניו אייג׳|לטיני|מטאל|בלוז"
    )
)

private val parsedMoodStyleCopies = HashMap<String, MoodStyleCopy>()

internal fun moodStyleCopy(code: String): MoodStyleCopy = synchronized(parsedMoodStyleCopies) {
    parsedMoodStyleCopies.getOrPut(code) { MoodStyleCopy(localizedValueOrEnglish(moodStyleCopies, code)) }
}

internal fun moodStyleLocalizationCodes(): Set<String> = moodStyleCopies.keys

internal fun moodStyleRawEntries(code: String): List<String> = moodStyleCopies.getValue(code)
