package com.luc4n3x.levyra.ui.i18n


internal data class AtelierCopy(
    val skinBody: String,
    val shelf: String,
    val shelfEmptyTitle: String,
    val shelfEmptyBody: String,
    val playCollection: String,
    val openDetails: String,
    val previousSleeve: String,
    val nextSleeve: String,
    val shelfPosition: String,
    val tracksCount: String,
    val sideA: String,
    val goToShelf: String,
    val artistLabel: String = "Artist",
    val explicit: String = "Explicit",
    val queueUpNext: String = "Up next",
    val queueMoveUp: String = "Move up",
    val queueMoveDown: String = "Move down",
    val featureTitle: String = "Selections",
    val recentAlbums: String = "Recent albums",
    val crateTitle: String = "From the crate",
    val crateReroll: String = "Dig again",
    val openCollection: String = "Collection",
    val allTools: String = "All tools",
    val queueHistoryEmpty: String = "Nothing played yet",
    val retry: String = "Try again",
    val homeEmptyBody: String = "Play something and your collection starts here."
)

internal object LevyraAtelierStrings {
    private val copies = mapOf(
        "en" to AtelierCopy(
            skinBody = "A dark listening room. Sleeves stand on a shelf in real depth and every record lands on a turntable.",
            shelf = "Shelf",
            shelfEmptyTitle = "The shelf is empty",
            shelfEmptyBody = "Save songs or albums and their sleeves will appear here.",
            playCollection = "Play",
            openDetails = "Open details",
            previousSleeve = "Previous sleeve",
            nextSleeve = "Next sleeve",
            shelfPosition = "Position on the shelf",
            tracksCount = "%d tracks",
            sideA = "SIDE A · 33⅓",
            goToShelf = "Open the shelf"
        ),
        "it" to AtelierCopy(
            skinBody = "Una sala d'ascolto scura. Le custodie stanno su uno scaffale in vera profondità e ogni disco atterra su un giradischi.",
            shelf = "Scaffale",
            shelfEmptyTitle = "Lo scaffale è vuoto",
            shelfEmptyBody = "Salva brani o album e qui appariranno le loro custodie.",
            playCollection = "Riproduci",
            openDetails = "Apri i dettagli",
            previousSleeve = "Custodia precedente",
            nextSleeve = "Custodia successiva",
            shelfPosition = "Posizione sullo scaffale",
            tracksCount = "%d brani",
            sideA = "LATO A · 33⅓",
            goToShelf = "Apri lo scaffale",
            artistLabel = "Artista",
            explicit = "Esplicito",
            queueUpNext = "A seguire",
            queueMoveUp = "Sposta su",
            queueMoveDown = "Sposta giù",
            openCollection = "Collezione",
            allTools = "Tutti gli strumenti",
            featureTitle = "Selezioni",
            recentAlbums = "Album recenti",
            crateTitle = "Dalla cassa dei dischi",
            crateReroll = "Rovista ancora",
            queueHistoryEmpty = "Ancora nessun ascolto",
            retry = "Riprova",
            homeEmptyBody = "Ancora nulla sul giradischi."
        )
    )

    private val skinBodies = mapOf(
        "es" to "Una sala de escucha oscura. Las fundas se alzan en una estantería con profundidad real y cada disco aterriza en un tocadiscos.",
        "fr" to "Une salle d'écoute sombre. Les pochettes se dressent sur une étagère en vraie profondeur et chaque disque se pose sur une platine.",
        "de" to "Ein dunkler Hörraum. Hüllen stehen in echter Tiefe im Regal und jede Platte landet auf einem Plattenspieler.",
        "pt" to "Uma sala de audição escura. As capas estão numa prateleira com profundidade real e cada disco pousa num gira-discos.",
        "nl" to "Een donkere luisterkamer. Hoezen staan in echte diepte op een plank en elke plaat landt op een draaitafel.",
        "pl" to "Ciemny pokój odsłuchowy. Okładki stoją na półce w prawdziwej głębi, a każda płyta ląduje na gramofonie.",
        "ro" to "O cameră de ascultare întunecată. Copertele stau pe un raft, în adâncime reală, iar fiecare disc ajunge pe un pick-up.",
        "el" to "Ένα σκοτεινό δωμάτιο ακρόασης. Τα εξώφυλλα στέκονται σε ένα ράφι με πραγματικό βάθος και κάθε δίσκος προσγειώνεται σε ένα πικάπ.",
        "sv" to "Ett mörkt lyssningsrum. Omslagen står på en hylla med verkligt djup och varje skiva landar på en skivspelare.",
        "da" to "Et mørkt lytterum. Coverne står på en hylde i ægte dybde, og hver plade lander på en pladespiller.",
        "cs" to "Tmavá poslechová místnost. Obaly stojí na polici ve skutečné hloubce a každá deska přistane na gramofonu.",
        "sk" to "Tmavá posluchová miestnosť. Obaly stoja na polici v skutočnej hĺbke a každá platňa pristane na gramofóne.",
        "hr" to "Tamna soba za slušanje. Omoti stoje na polici u stvarnoj dubini, a svaka ploča završi na gramofonu.",
        "sr" to "Тамна соба за слушање. Омоти стоје на полици у стварној дубини, а свака плоча слеће на грамофон.",
        "sq" to "Një dhomë e errët dëgjimi. Kopertinat qëndrojnë në një raft me thellësi të vërtetë dhe çdo disk ulet mbi një gramafon.",
        "bg" to "Тъмна стая за слушане. Обложките стоят на рафт в истинска дълбочина, а всяка плоча каца на грамофон.",
        "hu" to "Sötét zenehallgató szoba. A borítók valódi mélységben állnak a polcon, és minden lemez egy lemezjátszón landol.",
        "fi" to "Hämärä kuunteluhuone. Kannet seisovat hyllyssä todellisessa syvyydessä ja jokainen levy laskeutuu levysoittimelle.",
        "et" to "Hämar kuulamistuba. Ümbrised seisavad riiulil tõelises sügavuses ja iga plaat maandub plaadimängijale.",
        "nb" to "Et mørkt lytterom. Omslagene står i ekte dybde på en hylle, og hver plate lander på en platespiller.",
        "ca" to "Una sala d'escolta fosca. Les fundes reposen en un prestatge amb profunditat real i cada disc aterra en un tocadiscos.",
        "uk" to "Темна кімната для прослуховування. Обкладинки стоять на полиці в справжній глибині, а кожна платівка опускається на програвач.",
        "ru" to "Тёмная комната для прослушивания. Обложки стоят на полке в настоящей глубине, а каждая пластинка ложится на проигрыватель.",
        "kk" to "Тыңдауға арналған қараңғы бөлме. Мұқабалар сөреде шынайы тереңдікте тұрады, ал әр пластинка ойнатқышқа түседі.",
        "tr" to "Karanlık bir dinleme odası. Kapaklar gerçek derinlikte bir rafta durur ve her plak bir pikaba konar.",
        "ar" to "غرفة استماع داكنة. تقف الأغلفة على رف بعمق حقيقي، وتستقر كل أسطوانة على مشغل الأسطوانات.",
        "fa" to "اتاقی تاریک برای شنیدن. جلدها با عمق واقعی روی قفسه ایستاده‌اند و هر صفحه روی گرامافون می‌نشیند.",
        "zh" to "一间昏暗的聆听室。唱片封套以真实的纵深立在架上，每张唱片都会落在唱机上。",
        "zh-Hant" to "一間昏暗的聆聽室。唱片封套以真實的縱深立在架上，每張唱片都會落在唱盤上。",
        "ja" to "暗いリスニングルーム。ジャケットは奥行きのある棚に並び、どのレコードもターンテーブルに降り立ちます。",
        "ko" to "어두운 감상실. 슬리브가 실제 깊이감으로 선반에 서 있고, 모든 레코드는 턴테이블 위에 내려앉습니다.",
        "hi" to "एक अँधेरा लिसनिंग रूम। कवर असली गहराई के साथ शेल्फ़ पर खड़े हैं और हर रिकॉर्ड टर्नटेबल पर उतरता है।",
        "bn" to "একটি অন্ধকার শোনার ঘর। কভারগুলো সত্যিকারের গভীরতায় তাকে দাঁড়িয়ে থাকে, আর প্রতিটি রেকর্ড টার্নটেবিলে নামে।",
        "id" to "Ruang dengar yang gelap. Sampul berdiri di rak dengan kedalaman nyata dan setiap piringan mendarat di turntable.",
        "ms" to "Bilik dengar yang gelap. Sampul berdiri di rak dengan kedalaman sebenar dan setiap piring hitam mendarat di atas pemain piring.",
        "vi" to "Một phòng nghe nhạc tối. Bìa đĩa đứng trên kệ với chiều sâu thật và mỗi đĩa than đều đáp xuống mâm quay.",
        "th" to "ห้องฟังเพลงโทนมืด ปกแผ่นเรียงบนชั้นด้วยมิติลึกจริง และทุกแผ่นเสียงจะลงสู่เครื่องเล่นแผ่นเสียง",
        "fil" to "Isang madilim na listening room. Nakatayo ang mga sleeve sa istante na may tunay na lalim at bawat record ay lumalapag sa turntable.",
        "he" to "חדר האזנה כהה. העטיפות עומדות על מדף בעומק אמיתי וכל תקליט נוחת על פטיפון."
    )

    fun forCode(code: String): AtelierCopy {
        val copy = localizedValueOrEnglish(copies, code)
        val body = skinBodies[translationKey(code)] ?: return copy
        return copy.copy(skinBody = if (isBrazilianPortuguese(code)) LevyraBrazilianPortuguese.adaptValue(body) else body)
    }
}
