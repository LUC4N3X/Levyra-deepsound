package com.luc4n3x.levyra.ui.i18n

internal data class LanguagePickerCopy(
    val search: String,
    val clearSearch: String,
    val suggested: String,
    val allLanguages: String,
    val noResults: String
)

internal object LevyraLanguagePickerStrings {
    private val copies = mapOf(
        "en" to LanguagePickerCopy("Search languages", "Clear search", "Suggested", "All languages", "No language found"),
        "it" to LanguagePickerCopy("Cerca una lingua", "Cancella ricerca", "Suggerite", "Tutte le lingue", "Nessuna lingua trovata"),
        "es" to LanguagePickerCopy("Buscar idioma", "Borrar búsqueda", "Sugeridos", "Todos los idiomas", "No se encontró ningún idioma"),
        "fr" to LanguagePickerCopy("Rechercher une langue", "Effacer la recherche", "Suggestions", "Toutes les langues", "Aucune langue trouvée"),
        "de" to LanguagePickerCopy("Sprache suchen", "Suche löschen", "Vorschläge", "Alle Sprachen", "Keine Sprache gefunden"),
        "pt" to LanguagePickerCopy("Procurar idioma", "Limpar pesquisa", "Sugeridos", "Todos os idiomas", "Nenhum idioma encontrado"),
        "pt-BR" to LanguagePickerCopy("Buscar idioma", "Limpar busca", "Sugeridos", "Todos os idiomas", "Nenhum idioma encontrado"),
        "nl" to LanguagePickerCopy("Taal zoeken", "Zoekopdracht wissen", "Voorgesteld", "Alle talen", "Geen taal gevonden"),
        "pl" to LanguagePickerCopy("Szukaj języka", "Wyczyść wyszukiwanie", "Sugerowane", "Wszystkie języki", "Nie znaleziono języka"),
        "ro" to LanguagePickerCopy("Caută o limbă", "Șterge căutarea", "Sugerate", "Toate limbile", "Nicio limbă găsită"),
        "el" to LanguagePickerCopy("Αναζήτηση γλώσσας", "Εκκαθάριση αναζήτησης", "Προτεινόμενες", "Όλες οι γλώσσες", "Δεν βρέθηκε γλώσσα"),
        "sv" to LanguagePickerCopy("Sök språk", "Rensa sökning", "Förslag", "Alla språk", "Inget språk hittades"),
        "da" to LanguagePickerCopy("Søg efter sprog", "Ryd søgning", "Forslag", "Alle sprog", "Intet sprog fundet"),
        "cs" to LanguagePickerCopy("Hledat jazyk", "Vymazat hledání", "Navrhované", "Všechny jazyky", "Žádný jazyk nenalezen"),
        "sk" to LanguagePickerCopy("Hľadať jazyk", "Vymazať hľadanie", "Navrhované", "Všetky jazyky", "Nenašiel sa žiadny jazyk"),
        "hr" to LanguagePickerCopy("Traži jezik", "Očisti pretragu", "Predloženo", "Svi jezici", "Jezik nije pronađen"),
        "sr" to LanguagePickerCopy("Претражи језик", "Обриши претрагу", "Предложено", "Сви језици", "Језик није пронађен"),
        "sq" to LanguagePickerCopy("Kërko gjuhën", "Pastro kërkimin", "Të sugjeruara", "Të gjitha gjuhët", "Nuk u gjet asnjë gjuhë"),
        "bg" to LanguagePickerCopy("Търсене на език", "Изчистване на търсенето", "Предложени", "Всички езици", "Няма намерен език"),
        "hu" to LanguagePickerCopy("Nyelv keresése", "Keresés törlése", "Javasolt", "Összes nyelv", "Nincs találat"),
        "fi" to LanguagePickerCopy("Etsi kieltä", "Tyhjennä haku", "Ehdotetut", "Kaikki kielet", "Kieltä ei löytynyt"),
        "et" to LanguagePickerCopy("Otsi keelt", "Tühjenda otsing", "Soovitatud", "Kõik keeled", "Keelt ei leitud"),
        "nb" to LanguagePickerCopy("Søk etter språk", "Tøm søket", "Forslag", "Alle språk", "Fant ikke noe språk"),
        "ca" to LanguagePickerCopy("Cerca un idioma", "Esborra la cerca", "Suggerits", "Tots els idiomes", "No s'ha trobat cap idioma"),
        "uk" to LanguagePickerCopy("Шукати мову", "Очистити пошук", "Рекомендовані", "Усі мови", "Мову не знайдено"),
        "ru" to LanguagePickerCopy("Поиск языка", "Очистить поиск", "Рекомендуемые", "Все языки", "Язык не найден"),
        "kk" to LanguagePickerCopy("Тілді іздеу", "Іздеуді тазалау", "Ұсынылған", "Барлық тілдер", "Тіл табылмады"),
        "tr" to LanguagePickerCopy("Dil ara", "Aramayı temizle", "Önerilenler", "Tüm diller", "Dil bulunamadı"),
        "ar" to LanguagePickerCopy("البحث عن لغة", "مسح البحث", "مقترحة", "كل اللغات", "لم يتم العثور على لغة"),
        "fa" to LanguagePickerCopy("جستجوی زبان", "پاک کردن جستجو", "پیشنهادی", "همه زبان‌ها", "زبانی پیدا نشد"),
        "zh" to LanguagePickerCopy("搜索语言", "清除搜索", "推荐", "所有语言", "未找到语言"),
        "zh-Hant" to LanguagePickerCopy("搜尋語言", "清除搜尋", "建議", "所有語言", "找不到語言"),
        "ja" to LanguagePickerCopy("言語を検索", "検索をクリア", "おすすめ", "すべての言語", "言語が見つかりません"),
        "ko" to LanguagePickerCopy("언어 검색", "검색 지우기", "추천", "모든 언어", "언어를 찾을 수 없습니다"),
        "hi" to LanguagePickerCopy("भाषा खोजें", "खोज साफ़ करें", "सुझाई गई", "सभी भाषाएँ", "कोई भाषा नहीं मिली"),
        "bn" to LanguagePickerCopy("ভাষা খুঁজুন", "অনুসন্ধান মুছুন", "প্রস্তাবিত", "সব ভাষা", "কোনো ভাষা পাওয়া যায়নি"),
        "id" to LanguagePickerCopy("Cari bahasa", "Hapus pencarian", "Disarankan", "Semua bahasa", "Bahasa tidak ditemukan"),
        "ms" to LanguagePickerCopy("Cari bahasa", "Kosongkan carian", "Dicadangkan", "Semua bahasa", "Bahasa tidak ditemui"),
        "vi" to LanguagePickerCopy("Tìm ngôn ngữ", "Xóa tìm kiếm", "Đề xuất", "Tất cả ngôn ngữ", "Không tìm thấy ngôn ngữ"),
        "th" to LanguagePickerCopy("ค้นหาภาษา", "ล้างการค้นหา", "แนะนำ", "ทุกภาษา", "ไม่พบภาษา"),
        "fil" to LanguagePickerCopy("Maghanap ng wika", "I-clear ang paghahanap", "Iminumungkahi", "Lahat ng wika", "Walang nahanap na wika"),
        "he" to LanguagePickerCopy("חיפוש שפה", "ניקוי החיפוש", "מוצעות", "כל השפות", "לא נמצאה שפה")
    )

    private val greetings = mapOf(
        "en" to "Hello", "it" to "Ciao", "es" to "Hola", "fr" to "Salut", "de" to "Hallo", "pt" to "Olá", "pt-BR" to "Oi",
        "nl" to "Hallo", "pl" to "Cześć", "ro" to "Salut", "el" to "Γεια", "sv" to "Hej", "da" to "Hej", "cs" to "Ahoj",
        "sk" to "Ahoj", "hr" to "Bok", "sr" to "Здраво", "sq" to "Përshëndetje", "bg" to "Здравей", "hu" to "Szia",
        "fi" to "Moi", "et" to "Tere", "nb" to "Hei", "ca" to "Hola", "uk" to "Привіт", "ru" to "Привет", "kk" to "Сәлем",
        "tr" to "Merhaba", "ar" to "مرحبا", "fa" to "سلام", "zh" to "你好", "zh-Hant" to "你好", "ja" to "こんにちは",
        "ko" to "안녕하세요", "hi" to "नमस्ते", "bn" to "নমস্কার", "id" to "Halo", "ms" to "Helo", "vi" to "Xin chào",
        "th" to "สวัสดี", "fil" to "Kumusta", "he" to "שלום"
    )

    fun greeting(code: String): String = localizedValueOrEnglish(greetings, code)

    fun forCode(code: String): LanguagePickerCopy = localizedValueOrEnglish(copies, code)
}
