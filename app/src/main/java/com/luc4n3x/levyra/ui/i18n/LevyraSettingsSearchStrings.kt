package com.luc4n3x.levyra.ui.i18n

private fun settingsSearchStrings(
    placeholder: String,
    empty: String
): Map<String, String> = mapOf(
    "settingsSearchPlaceholder" to placeholder,
    "settingsSearchEmpty" to empty
)

private val settingsSearchBundles: Map<String, Map<String, String>> = mapOf(
    "en" to settingsSearchStrings("Search settings", "No settings found"),
    "it" to settingsSearchStrings("Cerca nelle impostazioni", "Nessuna impostazione trovata"),
    "es" to settingsSearchStrings("Buscar ajustes", "No se encontraron ajustes"),
    "fr" to settingsSearchStrings("Rechercher dans les paramètres", "Aucun paramètre trouvé"),
    "de" to settingsSearchStrings("Einstellungen durchsuchen", "Keine Einstellungen gefunden"),
    "pt" to settingsSearchStrings("Pesquisar definições", "Nenhuma definição encontrada"),
    "nl" to settingsSearchStrings("Instellingen zoeken", "Geen instellingen gevonden"),
    "pl" to settingsSearchStrings("Szukaj w ustawieniach", "Nie znaleziono ustawień"),
    "ro" to settingsSearchStrings("Caută în setări", "Nu s-au găsit setări"),
    "el" to settingsSearchStrings("Αναζήτηση ρυθμίσεων", "Δεν βρέθηκαν ρυθμίσεις"),
    "sv" to settingsSearchStrings("Sök i inställningar", "Inga inställningar hittades"),
    "da" to settingsSearchStrings("Søg i indstillinger", "Ingen indstillinger fundet"),
    "cs" to settingsSearchStrings("Hledat v nastavení", "Nebyla nalezena žádná nastavení"),
    "uk" to settingsSearchStrings("Пошук у налаштуваннях", "Налаштувань не знайдено"),
    "ru" to settingsSearchStrings("Поиск по настройкам", "Настройки не найдены"),
    "tr" to settingsSearchStrings("Ayarlarda ara", "Ayar bulunamadı"),
    "ar" to settingsSearchStrings("البحث في الإعدادات", "لم يتم العثور على إعدادات"),
    "zh" to settingsSearchStrings("搜索设置", "未找到设置"),
    "ja" to settingsSearchStrings("設定を検索", "設定が見つかりません"),
    "ko" to settingsSearchStrings("설정 검색", "설정을 찾을 수 없음"),
    "hi" to settingsSearchStrings("सेटिंग्स खोजें", "कोई सेटिंग नहीं मिली"),
    "id" to settingsSearchStrings("Cari setelan", "Setelan tidak ditemukan"),
    "vi" to settingsSearchStrings("Tìm trong cài đặt", "Không tìm thấy cài đặt"),
    "th" to settingsSearchStrings("ค้นหาการตั้งค่า", "ไม่พบการตั้งค่า"),
    "fil" to settingsSearchStrings("Maghanap sa Mga Setting", "Walang nahanap na setting"),
    "he" to settingsSearchStrings("חיפוש בהגדרות", "לא נמצאו הגדרות"),
    "fi" to settingsSearchStrings("Hae asetuksista", "Asetuksia ei löytynyt"),
    "et" to settingsSearchStrings("Otsi seadetest", "Seadeid ei leitud"),
    "sk" to settingsSearchStrings("Hľadať v nastaveniach", "Nenašli sa žiadne nastavenia"),
    "hr" to settingsSearchStrings("Pretraži postavke", "Nisu pronađene postavke"),
    "bg" to settingsSearchStrings("Търсене в настройките", "Няма намерени настройки"),
    "hu" to settingsSearchStrings("Keresés a beállításokban", "Nem található beállítás"),
    "nb" to settingsSearchStrings("Søk i innstillinger", "Fant ingen innstillinger"),
    "ca" to settingsSearchStrings("Cerca a la configuració", "No s'ha trobat cap configuració"),
    "ms" to settingsSearchStrings("Cari tetapan", "Tiada tetapan ditemui"),
    "fa" to settingsSearchStrings("جستجوی تنظیمات", "تنظیمی پیدا نشد"),
    "zh-Hant" to settingsSearchStrings("搜尋設定", "找不到設定")
)

internal fun settingsSearchLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(settingsSearchBundles, code)

internal fun settingsSearchLocalizationCodes(): Set<String> = settingsSearchBundles.keys

internal val settingsSearchKeys: Set<String> = setOf(
    "settingsSearchPlaceholder",
    "settingsSearchEmpty"
)
