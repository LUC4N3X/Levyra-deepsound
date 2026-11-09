package com.luc4n3x.levyra.ui.i18n

internal data class DailyTop100Copy(
    val title: String,
    val subtitle: String,
    val global: String,
    val failed: String,
    val retry: String
)

private val dailyTop100Copies = mapOf(
    "en" to DailyTop100Copy("Top 100 · Today", "The most played songs, country by country", "Global", "This chart didn't load", "Try again"),
    "it" to DailyTop100Copy("Top 100 · Oggi", "I brani più ascoltati, paese per paese", "Mondo", "Questa classifica non si è caricata", "Riprova"),
    "es" to DailyTop100Copy("Top 100 · Hoy", "Las canciones más escuchadas, país por país", "Mundo", "Esta lista no se ha cargado", "Reintentar"),
    "fr" to DailyTop100Copy("Top 100 · Aujourd'hui", "Les titres les plus écoutés, pays par pays", "Monde", "Ce classement n'a pas pu se charger", "Réessayer"),
    "de" to DailyTop100Copy("Top 100 · Heute", "Die meistgehörten Songs, Land für Land", "Welt", "Diese Charts konnten nicht geladen werden", "Erneut versuchen"),
    "pt" to DailyTop100Copy("Top 100 · Hoje", "As músicas mais ouvidas, país a país", "Mundo", "Esta tabela não carregou", "Tentar novamente"),
    "pt-BR" to DailyTop100Copy("Top 100 · Hoje", "As músicas mais ouvidas, país por país", "Mundo", "Esta parada não carregou", "Tentar de novo"),
    "nl" to DailyTop100Copy("Top 100 · Vandaag", "De meest beluisterde nummers, per land", "Wereld", "Deze lijst kon niet worden geladen", "Opnieuw proberen"),
    "pl" to DailyTop100Copy("Top 100 · Dziś", "Najczęściej słuchane utwory w każdym kraju", "Świat", "Nie udało się wczytać tej listy", "Spróbuj ponownie"),
    "ro" to DailyTop100Copy("Top 100 · Azi", "Cele mai ascultate piese, țară cu țară", "Global", "Acest clasament nu s-a încărcat", "Încearcă din nou"),
    "el" to DailyTop100Copy("Top 100 · Σήμερα", "Τα πιο δημοφιλή τραγούδια, χώρα με χώρα", "Παγκόσμιο", "Αυτό το chart δεν φορτώθηκε", "Δοκίμασε ξανά"),
    "sv" to DailyTop100Copy("Topp 100 · I dag", "De mest spelade låtarna, land för land", "Världen", "Den här listan kunde inte läsas in", "Försök igen"),
    "da" to DailyTop100Copy("Top 100 · I dag", "De mest spillede numre, land for land", "Verden", "Denne hitliste kunne ikke indlæses", "Prøv igen"),
    "cs" to DailyTop100Copy("Top 100 · Dnes", "Nejposlouchanější skladby země po zemi", "Svět", "Tento žebříček se nenačetl", "Zkusit znovu"),
    "sk" to DailyTop100Copy("Top 100 · Dnes", "Najpočúvanejšie skladby krajinu po krajine", "Svet", "Tento rebríček sa nenačítal", "Skúsiť znova"),
    "hr" to DailyTop100Copy("Top 100 · Danas", "Najslušanije pjesme, zemlju po zemlju", "Svijet", "Ova ljestvica nije se učitala", "Pokušaj ponovno"),
    "sr" to DailyTop100Copy("Топ 100 · Данас", "Најслушаније песме, земљу по земљу", "Свет", "Ова листа није учитана", "Покушај поново"),
    "sq" to DailyTop100Copy("Top 100 · Sot", "Këngët më të dëgjuara, vend pas vendi", "Bota", "Kjo listë nuk u ngarkua", "Provo përsëri"),
    "bg" to DailyTop100Copy("Топ 100 · Днес", "Най-слушаните песни, държава по държава", "Свят", "Тази класация не се зареди", "Опитай отново"),
    "hu" to DailyTop100Copy("Top 100 · Ma", "A legtöbbet hallgatott dalok országonként", "Világ", "Ez a lista nem töltődött be", "Újra"),
    "fi" to DailyTop100Copy("Top 100 · Tänään", "Kuunnelluimmat kappaleet maittain", "Maailma", "Listaa ei voitu ladata", "Yritä uudelleen"),
    "et" to DailyTop100Copy("Top 100 · Täna", "Enim kuulatud lood riikide kaupa", "Maailm", "Seda edetabelit ei saanud laadida", "Proovi uuesti"),
    "nb" to DailyTop100Copy("Topp 100 · I dag", "De mest spilte låtene, land for land", "Verden", "Denne listen kunne ikke lastes inn", "Prøv igjen"),
    "ca" to DailyTop100Copy("Top 100 · Avui", "Les cançons més escoltades, país per país", "Món", "Aquesta llista no s'ha carregat", "Torna-ho a provar"),
    "uk" to DailyTop100Copy("Топ-100 · Сьогодні", "Найпопулярніші пісні в кожній країні", "Світ", "Цей чарт не завантажився", "Спробувати ще"),
    "ru" to DailyTop100Copy("Топ-100 · Сегодня", "Самые популярные песни в каждой стране", "Мир", "Этот чарт не загрузился", "Повторить"),
    "kk" to DailyTop100Copy("Топ-100 · Бүгін", "Әр елдегі ең көп тыңдалған әндер", "Әлем", "Бұл чарт жүктелмеді", "Қайталау"),
    "tr" to DailyTop100Copy("Top 100 · Bugün", "Ülke ülke en çok dinlenen şarkılar", "Dünya", "Bu liste yüklenemedi", "Tekrar dene"),
    "ar" to DailyTop100Copy("أفضل 100 · اليوم", "الأغاني الأكثر استماعًا في كل بلد", "العالم", "تعذّر تحميل هذه القائمة", "إعادة المحاولة"),
    "fa" to DailyTop100Copy("۱۰۰ برتر · امروز", "پرشنیده‌ترین آهنگ‌ها، کشور به کشور", "جهان", "این جدول بارگیری نشد", "تلاش دوباره"),
    "zh" to DailyTop100Copy("今日 Top 100", "各国家和地区播放最多的歌曲", "全球", "此榜单未能加载", "重试"),
    "zh-Hant" to DailyTop100Copy("今日 Top 100", "各國家和地區播放最多的歌曲", "全球", "此排行榜未能載入", "重試"),
    "ja" to DailyTop100Copy("今日のトップ100", "国・地域ごとに最も再生された曲", "グローバル", "このチャートを読み込めませんでした", "再試行"),
    "ko" to DailyTop100Copy("오늘의 Top 100", "국가별로 가장 많이 재생된 곡", "글로벌", "이 차트를 불러오지 못했어요", "다시 시도"),
    "hi" to DailyTop100Copy("टॉप 100 · आज", "हर देश में सबसे ज़्यादा सुने गए गाने", "दुनिया", "यह चार्ट लोड नहीं हुआ", "फिर कोशिश करें"),
    "bn" to DailyTop100Copy("সেরা ১০০ · আজ", "দেশ অনুযায়ী সবচেয়ে বেশি শোনা গান", "বিশ্ব", "এই চার্টটি লোড হয়নি", "আবার চেষ্টা করুন"),
    "id" to DailyTop100Copy("Top 100 · Hari ini", "Lagu paling banyak diputar di tiap negara", "Global", "Tangga lagu ini gagal dimuat", "Coba lagi"),
    "ms" to DailyTop100Copy("Top 100 · Hari ini", "Lagu paling banyak dimainkan mengikut negara", "Global", "Carta ini gagal dimuatkan", "Cuba lagi"),
    "vi" to DailyTop100Copy("Top 100 · Hôm nay", "Những bài hát được nghe nhiều nhất ở từng quốc gia", "Toàn cầu", "Không tải được bảng xếp hạng này", "Thử lại"),
    "th" to DailyTop100Copy("Top 100 · วันนี้", "เพลงที่ถูกเล่นมากที่สุดในแต่ละประเทศ", "ทั่วโลก", "โหลดชาร์ตนี้ไม่สำเร็จ", "ลองอีกครั้ง"),
    "fil" to DailyTop100Copy("Top 100 · Ngayon", "Ang pinakapinakikinggang kanta sa bawat bansa", "Mundo", "Hindi na-load ang chart na ito", "Subukan ulit"),
    "he" to DailyTop100Copy("טופ 100 · היום", "השירים המושמעים ביותר בכל מדינה", "עולמי", "המצעד לא נטען", "לנסות שוב")
)

internal fun dailyTop100Copy(code: String): DailyTop100Copy = localizedValueOrEnglish(dailyTop100Copies, code)

internal fun dailyTop100LocalizationCodes(): Set<String> = dailyTop100Copies.keys
