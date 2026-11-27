package com.luc4n3x.levyra.ui.i18n

private val homeSoundtrackKeyOrder = listOf(
    "homeSoundtrackRadio",
    "homeSoundtrackTitle",
    "homeSoundtrackLeadArtists",
    "homeSoundtrackLeadFallback",
    "homeSoundtrackListSeparator",
    "homeSoundtrackListLastSeparator",
    "homeSoundtrackTodayTitle"
)

internal val homeSoundtrackKeys: Set<String> = homeSoundtrackKeyOrder.toSet()

private fun homeSoundtrack(vararg values: String): Map<String, String> {
    require(values.size == homeSoundtrackKeyOrder.size) { "Home soundtrack bundle has ${values.size} values" }
    return homeSoundtrackKeyOrder.zip(values).toMap()
}

private val homeSoundtrackBundles: Map<String, Map<String, String>> = mapOf(
    "en" to homeSoundtrack(
        "RADIO", "My soundtrack", "Starts with {artists}",
        "A radio that grows with every listen.", ", ", " and ", "Your sound today"
    ),
    "it" to homeSoundtrack(
        "RADIO", "La mia colonna sonora", "Inizia con {artists}",
        "Una radio che cresce a ogni tuo ascolto.", ", ", " e ", "Il tuo sound di oggi"
    ),
    "es" to homeSoundtrack(
        "RADIO", "Mi banda sonora", "Empieza con {artists}",
        "Una radio que crece con cada escucha.", ", ", " y ", "Tu sonido de hoy"
    ),
    "fr" to homeSoundtrack(
        "RADIO", "Ma bande-son", "Commence avec {artists}",
        "Une radio qui grandit à chaque écoute.", ", ", " et ", "Ton son du jour"
    ),
    "de" to homeSoundtrack(
        "RADIO", "Mein Soundtrack", "Startet mit {artists}",
        "Ein Radio, das mit jedem Hören wächst.", ", ", " und ", "Dein Sound für heute"
    ),
    "pt" to homeSoundtrack(
        "RÁDIO", "A minha banda sonora", "Começa com {artists}",
        "Uma rádio que cresce a cada música.", ", ", " e ", "O teu som de hoje"
    ),
    "pt-BR" to homeSoundtrack(
        "RÁDIO", "Minha trilha sonora", "Começa com {artists}",
        "Uma rádio que cresce a cada música que você ouve.", ", ", " e ", "Seu som de hoje"
    ),
    "nl" to homeSoundtrack(
        "RADIO", "Mijn soundtrack", "Begint met {artists}",
        "Een radio die groeit bij elke luisterbeurt.", ", ", " en ", "Jouw sound van vandaag"
    ),
    "pl" to homeSoundtrack(
        "RADIO", "Moja ścieżka dźwiękowa", "Na początek: {artists}",
        "Radio, które rośnie z każdym odsłuchem.", ", ", " i ", "Twoje brzmienie na dziś"
    ),
    "ro" to homeSoundtrack(
        "RADIO", "Coloana mea sonoră", "Începe cu {artists}",
        "Un radio care crește cu fiecare ascultare.", ", ", " și ", "Sunetul tău de azi"
    ),
    "el" to homeSoundtrack(
        "ΡΑΔΙΟΦΩΝΟ", "Το σάουντρακ μου", "Ξεκινά με {artists}",
        "Ένα ραδιόφωνο που μεγαλώνει με κάθε ακρόαση.", ", ", " και ", "Ο ήχος σου για σήμερα"
    ),
    "sv" to homeSoundtrack(
        "RADIO", "Mitt soundtrack", "Börjar med {artists}",
        "En radio som växer för varje lyssning.", ", ", " och ", "Ditt sound i dag"
    ),
    "da" to homeSoundtrack(
        "RADIO", "Mit soundtrack", "Starter med {artists}",
        "En radio, der vokser med hver lytning.", ", ", " og ", "Din lyd i dag"
    ),
    "cs" to homeSoundtrack(
        "RÁDIO", "Můj soundtrack", "Na úvod: {artists}",
        "Rádio, které roste s každým poslechem.", ", ", " a ", "Tvůj zvuk na dnešek"
    ),
    "sk" to homeSoundtrack(
        "RÁDIO", "Môj soundtrack", "Na úvod: {artists}",
        "Rádio, ktoré rastie s každým vypočutím.", ", ", " a ", "Tvoj zvuk na dnes"
    ),
    "bn" to homeSoundtrack(
        "রেডিও", "আমার সাউন্ডট্র্যাক", "শুরু হচ্ছে {artists} দিয়ে",
        "প্রতিটি শোনার সঙ্গে বেড়ে ওঠা রেডিও।", ", ", " ও ", "আজকের জন্য তোমার সাউন্ড"
    ),
    "kk" to homeSoundtrack(
        "РАДИО", "Менің саундтрегім", "Басталуы: {artists}",
        "Әр тыңдаған сайын дамитын радио.", ", ", " және ", "Бүгінгі сенің саундың"
    ),
    "sr" to homeSoundtrack(
        "РАДИО", "Мој саундтрек", "За почетак: {artists}",
        "Радио који расте са сваким слушањем.", ", ", " и ", "Твој звук за данас"
    ),
    "sq" to homeSoundtrack(
        "RADIO", "Kolona ime zanore", "Fillon me {artists}",
        "Radio që rritet me çdo dëgjim.", ", ", " dhe ", "Tingulli yt i sotëm"
    ),
    "hr" to homeSoundtrack(
        "RADIO", "Moj soundtrack", "Za početak: {artists}",
        "Radio koji raste sa svakim slušanjem.", ", ", " i ", "Tvoj zvuk za danas"
    ),
    "bg" to homeSoundtrack(
        "РАДИО", "Моят саундтрак", "Започва с {artists}",
        "Радио, което расте с всяко слушане.", ", ", " и ", "Твоят звук за днес"
    ),
    "hu" to homeSoundtrack(
        "RÁDIÓ", "Saját filmzeném", "Kezdésként: {artists}",
        "Rádió, amely minden hallgatással bővül.", ", ", " és ", "A mai hangzásod"
    ),
    "fi" to homeSoundtrack(
        "RADIO", "Oma soundtrackini", "Aluksi: {artists}",
        "Radio, joka kasvaa jokaisella kuuntelukerralla.", ", ", " ja ", "Päivän soundisi"
    ),
    "et" to homeSoundtrack(
        "RAADIO", "Minu heliriba", "Alguseks: {artists}",
        "Raadio, mis kasvab iga kuulamisega.", ", ", " ja ", "Sinu tänane heli"
    ),
    "nb" to homeSoundtrack(
        "RADIO", "Mitt lydspor", "Starter med {artists}",
        "En radio som vokser for hver lytting.", ", ", " og ", "Din lyd i dag"
    ),
    "ca" to homeSoundtrack(
        "RÀDIO", "La meva banda sonora", "Comença amb {artists}",
        "Una ràdio que creix amb cada escolta.", ", ", " i ", "El teu so d’avui"
    ),
    "uk" to homeSoundtrack(
        "РАДІО", "Мій саундтрек", "Для початку: {artists}",
        "Радіо, що росте з кожним прослуховуванням.", ", ", " і ", "Твій звук на сьогодні"
    ),
    "ru" to homeSoundtrack(
        "РАДИО", "Мой саундтрек", "Для начала: {artists}",
        "Радио, которое растёт с каждым прослушиванием.", ", ", " и ", "Твой звук на сегодня"
    ),
    "tr" to homeSoundtrack(
        "RADYO", "Film müziğim", "{artists} ile başlıyor",
        "Dinledikçe büyüyen bir radyo.", ", ", " ve ", "Bugünkü sesin"
    ),
    "ar" to homeSoundtrack(
        "راديو", "موسيقاي التصويرية", "يبدأ مع {artists}",
        "راديو ينمو مع كل استماع.", "، ", " و", "صوتك لهذا اليوم"
    ),
    "fa" to homeSoundtrack(
        "رادیو", "موسیقی متن من", "شروع با {artists}",
        "رادیویی که با هر بار گوش دادن بزرگ‌تر می‌شود.", "، ", " و ", "صدای امروز تو"
    ),
    "zh" to homeSoundtrack(
        "电台", "我的原声带", "从{artists}开始",
        "越听越懂你的电台。", "、", "和", "你的今日之声"
    ),
    "zh-Hant" to homeSoundtrack(
        "電台", "我的原聲帶", "從{artists}開始",
        "越聽越懂你的電台。", "、", "和", "你的今日之聲"
    ),
    "ja" to homeSoundtrack(
        "ラジオ", "私のサウンドトラック", "{artists}からスタート",
        "聴くほどに広がるラジオ。", "、", "、", "今日のあなたのサウンド"
    ),
    "ko" to homeSoundtrack(
        "라디오", "나의 사운드트랙", "{artists}부터 시작해요",
        "들을수록 넓어지는 라디오.", ", ", ", ", "오늘의 나의 사운드"
    ),
    "hi" to homeSoundtrack(
        "रेडियो", "मेरा साउंडट्रैक", "{artists} से शुरुआत",
        "हर बार सुनने पर बढ़ने वाला रेडियो।", ", ", " और ", "आज का तुम्हारा साउंड"
    ),
    "id" to homeSoundtrack(
        "RADIO", "Soundtrack saya", "Dimulai dengan {artists}",
        "Radio yang tumbuh setiap kali kamu mendengarkan.", ", ", " dan ", "Suaramu hari ini"
    ),
    "ms" to homeSoundtrack(
        "RADIO", "Runut bunyi saya", "Bermula dengan {artists}",
        "Radio yang berkembang setiap kali anda mendengar.", ", ", " dan ", "Bunyi anda hari ini"
    ),
    "vi" to homeSoundtrack(
        "RADIO", "Nhạc phim của tôi", "Bắt đầu với {artists}",
        "Một đài phát lớn dần theo mỗi lần bạn nghe.", ", ", " và ", "Âm thanh của bạn hôm nay"
    ),
    "th" to homeSoundtrack(
        "วิทยุ", "ซาวด์แทร็กของฉัน", "เริ่มต้นด้วย {artists}",
        "วิทยุที่เติบโตไปกับทุกการฟัง", ", ", " และ ", "เสียงของคุณวันนี้"
    ),
    "fil" to homeSoundtrack(
        "RADYO", "Aking soundtrack", "Nagsisimula sa {artists}",
        "Radyong lumalago sa bawat pakikinig.", ", ", " at ", "Ang tunog mo ngayong araw"
    ),
    "he" to homeSoundtrack(
        "רדיו", "הפסקול שלי", "מתחיל עם {artists}",
        "רדיו שגדל עם כל האזנה.", ", ", " ו", "הסאונד שלך להיום"
    ),
    "ur" to homeSoundtrack(
        "ریڈیو", "میرا ساؤنڈ ٹریک", "{artists} سے شروع ہوتا ہے۔",
        "ایک ریڈیو جو ہر سننے کے ساتھ بڑھتا ہے۔", ", ", " اور ", "آج آپ کی آواز"
    ),
    "ta" to homeSoundtrack(
        "வானொலி", "என் ஒலிப்பதிவு", "{artists} உடன் தொடங்குகிறது",
        "ஒவ்வொரு கேட்கும் போதும் வளரும் வானொலி.", ", ", " மற்றும் ", "இன்று உங்கள் ஒலி"
    ),
    "te" to homeSoundtrack(
        "రేడియో", "నా సౌండ్‌ట్రాక్", "{artists}తో ప్రారంభమవుతుంది",
        "ప్రతి వినికిడితో పెరిగే రేడియో.", ", ", " మరియు ", "ఈ రోజు మీ ధ్వని"
    ),
    "mr" to homeSoundtrack(
        "रेडिओ", "माझा साउंडट्रॅक", "{artists} ने सुरू होते",
        "प्रत्येक ऐकताना वाढणारा रेडिओ.", ", ", " आणि ", "आज तुझा आवाज"
    ),
    "gu" to homeSoundtrack(
        "રેડિયો", "મારી સાઉન્ડટ્રેક", "{artists} થી શરૂ થાય છે",
        "એક રેડિયો જે દરેક સાંભળવા સાથે વધે છે.", ", ", " અને ", "આજે તમારો અવાજ"
    )
)

internal fun homeSoundtrackLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(homeSoundtrackBundles, code)
