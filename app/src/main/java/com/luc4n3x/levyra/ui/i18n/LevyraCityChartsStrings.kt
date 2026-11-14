package com.luc4n3x.levyra.ui.i18n

internal data class CityChartsCopy(
    val title: String,
    val subtitle: String
)

private val cityChartsCopies = mapOf(
    "en" to CityChartsCopy("City charts", "What each city is playing right now"),
    "it" to CityChartsCopy("Classifiche città", "Cosa ascolta ogni città in questo momento"),
    "es" to CityChartsCopy("Listas por ciudad", "Lo que suena ahora mismo en cada ciudad"),
    "fr" to CityChartsCopy("Classements par ville", "Ce que chaque ville écoute en ce moment"),
    "de" to CityChartsCopy("Stadt-Charts", "Was jede Stadt gerade hört"),
    "pt" to CityChartsCopy("Tabelas por cidade", "O que cada cidade está a ouvir agora"),
    "pt-BR" to CityChartsCopy("Paradas por cidade", "O que cada cidade está ouvindo agora"),
    "nl" to CityChartsCopy("Stedenlijsten", "Waar elke stad nu naar luistert"),
    "pl" to CityChartsCopy("Listy miast", "Czego teraz słucha każde miasto"),
    "ro" to CityChartsCopy("Clasamente pe orașe", "Ce ascultă fiecare oraș chiar acum"),
    "el" to CityChartsCopy("Chart πόλεων", "Τι ακούει κάθε πόλη αυτή τη στιγμή"),
    "sv" to CityChartsCopy("Stadslistor", "Vad varje stad lyssnar på just nu"),
    "da" to CityChartsCopy("Byhitlister", "Hvad hver by lytter til lige nu"),
    "cs" to CityChartsCopy("Žebříčky měst", "Co právě poslouchá každé město"),
    "sk" to CityChartsCopy("Rebríčky miest", "Čo práve počúva každé mesto"),
    "hr" to CityChartsCopy("Ljestvice gradova", "Što svaki grad sluša upravo sada"),
    "sr" to CityChartsCopy("Листе градова", "Шта сваки град слуша управо сада"),
    "sq" to CityChartsCopy("Listat e qyteteve", "Çfarë dëgjon çdo qytet tani"),
    "bg" to CityChartsCopy("Класации по градове", "Какво слуша всеки град в момента"),
    "hu" to CityChartsCopy("Városok listái", "Mit hallgat most minden város"),
    "fi" to CityChartsCopy("Kaupunkien listat", "Mitä kukin kaupunki kuuntelee juuri nyt"),
    "et" to CityChartsCopy("Linnade edetabelid", "Mida iga linn praegu kuulab"),
    "nb" to CityChartsCopy("Bylister", "Hva hver by hører på akkurat nå"),
    "ca" to CityChartsCopy("Llistes per ciutat", "Què escolta ara cada ciutat"),
    "uk" to CityChartsCopy("Чарти міст", "Що зараз слухає кожне місто"),
    "ru" to CityChartsCopy("Чарты городов", "Что сейчас слушает каждый город"),
    "kk" to CityChartsCopy("Қалалар чарттары", "Қазір әр қала не тыңдап жатыр"),
    "tr" to CityChartsCopy("Şehir listeleri", "Her şehir şu an ne dinliyor"),
    "ar" to CityChartsCopy("قوائم المدن", "ما تستمع إليه كل مدينة الآن"),
    "fa" to CityChartsCopy("جدول شهرها", "هر شهر همین حالا چه گوش می‌دهد"),
    "zh" to CityChartsCopy("城市榜单", "每座城市此刻正在听什么"),
    "zh-Hant" to CityChartsCopy("城市排行榜", "每座城市此刻正在聽什麼"),
    "ja" to CityChartsCopy("都市別チャート", "いま各都市で聴かれている曲"),
    "ko" to CityChartsCopy("도시별 차트", "지금 각 도시에서 듣는 음악"),
    "hi" to CityChartsCopy("शहरों के चार्ट", "अभी हर शहर क्या सुन रहा है"),
    "bn" to CityChartsCopy("শহরের চার্ট", "এই মুহূর্তে প্রতিটি শহর কী শুনছে"),
    "id" to CityChartsCopy("Tangga lagu kota", "Yang sedang diputar di tiap kota sekarang"),
    "ms" to CityChartsCopy("Carta bandar", "Apa yang sedang dimainkan di setiap bandar sekarang"),
    "vi" to CityChartsCopy("Bảng xếp hạng thành phố", "Mỗi thành phố đang nghe gì lúc này"),
    "th" to CityChartsCopy("ชาร์ตเมือง", "ตอนนี้แต่ละเมืองกำลังฟังอะไร"),
    "fil" to CityChartsCopy("Mga chart ng lungsod", "Ang pinakikinggan ng bawat lungsod ngayon"),
    "he" to CityChartsCopy("מצעדי ערים", "מה כל עיר שומעת כרגע")
)

internal fun cityChartsCopy(code: String): CityChartsCopy = localizedValueOrEnglish(cityChartsCopies, code)
