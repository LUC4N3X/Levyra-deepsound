package com.luc4n3x.levyra.ui.i18n

internal data class OnboardingIntroDiscoveryCopy(val title: String, val body: String)

internal object LevyraOnboardingIntroStrings {
    private val copies = mapOf(
        "en" to OnboardingIntroDiscoveryCopy("Made for your taste", "Discover artists, new releases and music that feels like you."),
        "it" to OnboardingIntroDiscoveryCopy("Il tuo gusto, nuove scoperte", "Artisti, novità e musica da scoprire, sempre più vicini a te."),
        "es" to OnboardingIntroDiscoveryCopy("A tu gusto", "Descubre artistas, novedades y música que va contigo."),
        "fr" to OnboardingIntroDiscoveryCopy("À ton image", "Découvre des artistes, des nouveautés et une musique qui te ressemble."),
        "de" to OnboardingIntroDiscoveryCopy("Ganz nach deinem Geschmack", "Entdecke Künstler, Neuerscheinungen und Musik, die zu dir passt."),
        "pt" to OnboardingIntroDiscoveryCopy("Ao teu gosto", "Descobre artistas, novidades e música que combina contigo."),
        "pt-BR" to OnboardingIntroDiscoveryCopy("Do seu jeito", "Descubra artistas, lançamentos e músicas que combinam com você."),
        "nl" to OnboardingIntroDiscoveryCopy("Helemaal jouw smaak", "Ontdek artiesten, nieuwe releases en muziek die bij je past."),
        "pl" to OnboardingIntroDiscoveryCopy("W twoim guście", "Odkrywaj artystów, nowości i muzykę, która do ciebie pasuje."),
        "ro" to OnboardingIntroDiscoveryCopy("Pe gustul tău", "Descoperă artiști, noutăți și muzică potrivită pentru tine."),
        "el" to OnboardingIntroDiscoveryCopy("Στα μέτρα σου", "Ανακάλυψε καλλιτέχνες, νέες κυκλοφορίες και μουσική που σου ταιριάζει."),
        "sv" to OnboardingIntroDiscoveryCopy("Helt i din smak", "Upptäck artister, nya släpp och musik som passar dig."),
        "da" to OnboardingIntroDiscoveryCopy("Helt efter din smag", "Opdag kunstnere, nye udgivelser og musik, der passer til dig."),
        "cs" to OnboardingIntroDiscoveryCopy("Přesně podle tvého vkusu", "Objevuj interprety, novinky a hudbu, která ti sedne."),
        "sk" to OnboardingIntroDiscoveryCopy("Presne podľa tvojho vkusu", "Objavuj interpretov, novinky a hudbu, ktorá ti sadne."),
        "hr" to OnboardingIntroDiscoveryCopy("Po tvom ukusu", "Otkrij izvođače, nova izdanja i glazbu koja ti odgovara."),
        "sr" to OnboardingIntroDiscoveryCopy("По твом укусу", "Откриј извођаче, нова издања и музику која ти одговара."),
        "sq" to OnboardingIntroDiscoveryCopy("Sipas shijes tënde", "Zbulo artistë, publikime të reja dhe muzikë që të përshtatet."),
        "bg" to OnboardingIntroDiscoveryCopy("По твой вкус", "Открий изпълнители, нови издания и музика, която ти подхожда."),
        "hu" to OnboardingIntroDiscoveryCopy("A te ízlésedre szabva", "Fedezz fel előadókat, új megjelenéseket és hozzád illő zenéket."),
        "fi" to OnboardingIntroDiscoveryCopy("Juuri sinun makuusi", "Löydä artisteja, uutuuksia ja sinulle sopivaa musiikkia."),
        "et" to OnboardingIntroDiscoveryCopy("Just sinu maitse järgi", "Avasta artiste, uusi lugusid ja sulle sobivat muusikat."),
        "nb" to OnboardingIntroDiscoveryCopy("Helt etter din smak", "Oppdag artister, nye utgivelser og musikk som passer deg."),
        "ca" to OnboardingIntroDiscoveryCopy("Al teu gust", "Descobreix artistes, novetats i música que va amb tu."),
        "uk" to OnboardingIntroDiscoveryCopy("На твій смак", "Відкривай виконавців, нові релізи та музику, яка тобі пасує."),
        "ru" to OnboardingIntroDiscoveryCopy("На твой вкус", "Открывай исполнителей, новые релизы и музыку, которая тебе подходит."),
        "kk" to OnboardingIntroDiscoveryCopy("Талғамыңа сай", "Өзіңе ұнайтын орындаушыларды, жаңа туындыларды және музыканы тап."),
        "tr" to OnboardingIntroDiscoveryCopy("Tam senin zevkine göre", "Sanatçıları, yeni çıkanları ve sana uygun müzikleri keşfet."),
        "ar" to OnboardingIntroDiscoveryCopy("على ذوقك", "اكتشف فنانين وإصدارات جديدة وموسيقى تشبهك."),
        "fa" to OnboardingIntroDiscoveryCopy("مطابق سلیقه تو", "هنرمندان، آثار تازه و موسیقی متناسب با سلیقه‌ات را کشف کن."),
        "he" to OnboardingIntroDiscoveryCopy("בדיוק לטעם שלך", "גלה אמנים, יצירות חדשות ומוזיקה שמתאימה לך."),
        "zh" to OnboardingIntroDiscoveryCopy("懂你的音乐品味", "发现艺人、新作和更合你心意的音乐。"),
        "zh-Hant" to OnboardingIntroDiscoveryCopy("懂你的音樂品味", "發現藝人、新作和更合你心意的音樂。"),
        "ja" to OnboardingIntroDiscoveryCopy("あなたの好みに合わせて", "アーティストや新作、あなたらしい音楽に出会えます。"),
        "ko" to OnboardingIntroDiscoveryCopy("내 취향에 맞게", "아티스트, 신곡, 나에게 맞는 음악을 발견하세요."),
        "hi" to OnboardingIntroDiscoveryCopy("आपकी पसंद के लिए", "कलाकार, नई रिलीज़ और अपनी पसंद का संगीत खोजें।"),
        "bn" to OnboardingIntroDiscoveryCopy("আপনার পছন্দ অনুযায়ী", "শিল্পী, নতুন গান ও আপনার পছন্দের সংগীত আবিষ্কার করুন।"),
        "id" to OnboardingIntroDiscoveryCopy("Sesuai seleramu", "Temukan artis, rilisan baru, dan musik yang cocok untukmu."),
        "ms" to OnboardingIntroDiscoveryCopy("Mengikut cita rasa anda", "Temui artis, keluaran baharu dan muzik yang sesuai dengan anda."),
        "vi" to OnboardingIntroDiscoveryCopy("Đúng gu của bạn", "Khám phá nghệ sĩ, bản phát hành mới và âm nhạc hợp với bạn."),
        "th" to OnboardingIntroDiscoveryCopy("ตรงใจคุณ", "ค้นพบศิลปิน ผลงานใหม่ และเพลงที่เข้ากับคุณ"),
        "fil" to OnboardingIntroDiscoveryCopy("Para sa panlasa mo", "Tumuklas ng mga artist, bagong release at musikang bagay sa iyo.")
    )

    fun forCode(code: String): OnboardingIntroDiscoveryCopy = localizedValueOrEnglish(copies, code)
}
