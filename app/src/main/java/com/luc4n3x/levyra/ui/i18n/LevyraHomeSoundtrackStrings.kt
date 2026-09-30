package com.luc4n3x.levyra.ui.i18n

private val homeSoundtrackKeyOrder = listOf(
    "homeSoundtrackRadio",
    "homeSoundtrackTitle",
    "homeSoundtrackLeadArtists",
    "homeSoundtrackLeadFallback",
    "homeSoundtrackListSeparator",
    "homeSoundtrackListLastSeparator"
)

internal val homeSoundtrackKeys: Set<String> = homeSoundtrackKeyOrder.toSet()

private fun homeSoundtrack(vararg values: String): Map<String, String> {
    require(values.size == homeSoundtrackKeyOrder.size) { "Home soundtrack bundle has ${values.size} values" }
    return homeSoundtrackKeyOrder.zip(values).toMap()
}

private val homeSoundtrackBundles: Map<String, Map<String, String>> = mapOf(
    "en" to homeSoundtrack(
        "RADIO", "My soundtrack", "Starts with {artists}",
        "A radio that grows with every listen.", ", ", " and "
    ),
    "it" to homeSoundtrack(
        "RADIO", "La mia colonna sonora", "Inizia con {artists}",
        "Una radio che cresce a ogni tuo ascolto.", ", ", " e "
    ),
    "es" to homeSoundtrack(
        "RADIO", "Mi banda sonora", "Empieza con {artists}",
        "Una radio que crece con cada escucha.", ", ", " y "
    ),
    "fr" to homeSoundtrack(
        "RADIO", "Ma bande-son", "Commence avec {artists}",
        "Une radio qui grandit à chaque écoute.", ", ", " et "
    ),
    "de" to homeSoundtrack(
        "RADIO", "Mein Soundtrack", "Startet mit {artists}",
        "Ein Radio, das mit jedem Hören wächst.", ", ", " und "
    ),
    "pt" to homeSoundtrack(
        "RÁDIO", "Minha trilha sonora", "Começa com {artists}",
        "Uma rádio que cresce a cada música.", ", ", " e "
    ),
    "nl" to homeSoundtrack(
        "RADIO", "Mijn soundtrack", "Begint met {artists}",
        "Een radio die groeit bij elke luisterbeurt.", ", ", " en "
    ),
    "pl" to homeSoundtrack(
        "RADIO", "Moja ścieżka dźwiękowa", "Na początek: {artists}",
        "Radio, które rośnie z każdym odsłuchem.", ", ", " i "
    ),
    "ro" to homeSoundtrack(
        "RADIO", "Coloana mea sonoră", "Începe cu {artists}",
        "Un radio care crește cu fiecare ascultare.", ", ", " și "
    ),
    "el" to homeSoundtrack(
        "ΡΑΔΙΟΦΩΝΟ", "Το σάουντρακ μου", "Ξεκινά με {artists}",
        "Ένα ραδιόφωνο που μεγαλώνει με κάθε ακρόαση.", ", ", " και "
    ),
    "sv" to homeSoundtrack(
        "RADIO", "Mitt soundtrack", "Börjar med {artists}",
        "En radio som växer för varje lyssning.", ", ", " och "
    ),
    "da" to homeSoundtrack(
        "RADIO", "Mit soundtrack", "Starter med {artists}",
        "En radio, der vokser med hver lytning.", ", ", " og "
    ),
    "cs" to homeSoundtrack(
        "RÁDIO", "Můj soundtrack", "Na úvod: {artists}",
        "Rádio, které roste s každým poslechem.", ", ", " a "
    ),
    "sk" to homeSoundtrack(
        "RÁDIO", "Môj soundtrack", "Na úvod: {artists}",
        "Rádio, ktoré rastie s každým vypočutím.", ", ", " a "
    ),
    "hr" to homeSoundtrack(
        "RADIO", "Moj soundtrack", "Za početak: {artists}",
        "Radio koji raste sa svakim slušanjem.", ", ", " i "
    ),
    "bg" to homeSoundtrack(
        "РАДИО", "Моят саундтрак", "Започва с {artists}",
        "Радио, което расте с всяко слушане.", ", ", " и "
    ),
    "hu" to homeSoundtrack(
        "RÁDIÓ", "Saját filmzeném", "Kezdésként: {artists}",
        "Rádió, amely minden hallgatással bővül.", ", ", " és "
    ),
    "fi" to homeSoundtrack(
        "RADIO", "Oma soundtrackini", "Aluksi: {artists}",
        "Radio, joka kasvaa jokaisella kuuntelukerralla.", ", ", " ja "
    ),
    "et" to homeSoundtrack(
        "RAADIO", "Minu heliriba", "Alguseks: {artists}",
        "Raadio, mis kasvab iga kuulamisega.", ", ", " ja "
    ),
    "nb" to homeSoundtrack(
        "RADIO", "Mitt lydspor", "Starter med {artists}",
        "En radio som vokser for hver lytting.", ", ", " og "
    ),
    "ca" to homeSoundtrack(
        "RÀDIO", "La meva banda sonora", "Comença amb {artists}",
        "Una ràdio que creix amb cada escolta.", ", ", " i "
    ),
    "uk" to homeSoundtrack(
        "РАДІО", "Мій саундтрек", "Для початку: {artists}",
        "Радіо, що росте з кожним прослуховуванням.", ", ", " і "
    ),
    "ru" to homeSoundtrack(
        "РАДИО", "Мой саундтрек", "Для начала: {artists}",
        "Радио, которое растёт с каждым прослушиванием.", ", ", " и "
    ),
    "tr" to homeSoundtrack(
        "RADYO", "Film müziğim", "{artists} ile başlıyor",
        "Dinledikçe büyüyen bir radyo.", ", ", " ve "
    ),
    "ar" to homeSoundtrack(
        "راديو", "موسيقاي التصويرية", "يبدأ مع {artists}",
        "راديو ينمو مع كل استماع.", "، ", " و"
    ),
    "fa" to homeSoundtrack(
        "رادیو", "موسیقی متن من", "شروع با {artists}",
        "رادیویی که با هر بار گوش دادن بزرگ‌تر می‌شود.", "، ", " و "
    ),
    "zh" to homeSoundtrack(
        "电台", "我的原声带", "从{artists}开始",
        "越听越懂你的电台。", "、", "和"
    ),
    "zh-Hant" to homeSoundtrack(
        "電台", "我的原聲帶", "從{artists}開始",
        "越聽越懂你的電台。", "、", "和"
    ),
    "ja" to homeSoundtrack(
        "ラジオ", "私のサウンドトラック", "{artists}からスタート",
        "聴くほどに広がるラジオ。", "、", "、"
    ),
    "ko" to homeSoundtrack(
        "라디오", "나의 사운드트랙", "{artists}부터 시작해요",
        "들을수록 넓어지는 라디오.", ", ", ", "
    ),
    "hi" to homeSoundtrack(
        "रेडियो", "मेरा साउंडट्रैक", "{artists} से शुरुआत",
        "हर बार सुनने पर बढ़ने वाला रेडियो।", ", ", " और "
    ),
    "id" to homeSoundtrack(
        "RADIO", "Soundtrack saya", "Dimulai dengan {artists}",
        "Radio yang tumbuh setiap kali kamu mendengarkan.", ", ", " dan "
    ),
    "ms" to homeSoundtrack(
        "RADIO", "Runut bunyi saya", "Bermula dengan {artists}",
        "Radio yang berkembang setiap kali anda mendengar.", ", ", " dan "
    ),
    "vi" to homeSoundtrack(
        "RADIO", "Nhạc phim của tôi", "Bắt đầu với {artists}",
        "Một đài phát lớn dần theo mỗi lần bạn nghe.", ", ", " và "
    ),
    "th" to homeSoundtrack(
        "วิทยุ", "ซาวด์แทร็กของฉัน", "เริ่มต้นด้วย {artists}",
        "วิทยุที่เติบโตไปกับทุกการฟัง", ", ", " และ "
    ),
    "fil" to homeSoundtrack(
        "RADYO", "Aking soundtrack", "Nagsisimula sa {artists}",
        "Radyong lumalago sa bawat pakikinig.", ", ", " at "
    ),
    "he" to homeSoundtrack(
        "רדיו", "הפסקול שלי", "מתחיל עם {artists}",
        "רדיו שגדל עם כל האזנה.", ", ", " ו"
    )
)

internal fun homeSoundtrackLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(homeSoundtrackBundles, code)
