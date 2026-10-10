package com.luc4n3x.levyra.ui.i18n

private val chartMarketKeyOrder = listOf(
    "chartMarketTitle",
    "chartMarketChange",
    "chartMarketSheetTitle",
    "chartMarketSearchHint",
    "chartMarketSuggested",
    "chartMarketYourRegion",
    "chartMarketAllCountries",
    "chartMarketNoResults"
)

internal val chartMarketKeys: Set<String> = chartMarketKeyOrder.toSet()

private fun chartMarket(vararg values: String): Map<String, String> {
    require(values.size == chartMarketKeyOrder.size) { "Chart market bundle has ${values.size} values" }
    return chartMarketKeyOrder.zip(values).toMap()
}

private val chartMarketBundles: Map<String, Map<String, String>> = mapOf(
    "en" to chartMarket(
        "Top {count} {country}", "Change country", "Choose a chart", "Search country",
        "Suggested", "Your region", "All countries", "No countries match “{query}”"
    ),
    "it" to chartMarket(
        "Top {count} {country}", "Cambia paese", "Scegli una classifica", "Cerca paese",
        "Consigliati", "La tua regione", "Tutti i paesi", "Nessun paese corrisponde a “{query}”"
    ),
    "es" to chartMarket(
        "Top {count} {country}", "Cambiar país", "Elige una lista", "Buscar país",
        "Sugeridos", "Tu región", "Todos los países", "Ningún país coincide con «{query}»"
    ),
    "fr" to chartMarket(
        "Top {count} {country}", "Changer de pays", "Choisis un classement", "Rechercher un pays",
        "Suggestions", "Ta région", "Tous les pays", "Aucun pays ne correspond à « {query} »"
    ),
    "de" to chartMarket(
        "Top {count} {country}", "Land ändern", "Wähle eine Chartliste", "Land suchen",
        "Vorschläge", "Deine Region", "Alle Länder", "Kein Land passt zu „{query}“"
    ),
    "pt" to chartMarket(
        "Top {count} {country}", "Mudar de país", "Escolhe uma tabela", "Pesquisar país",
        "Sugeridos", "A tua região", "Todos os países", "Nenhum país corresponde a “{query}”"
    ),
    "nl" to chartMarket(
        "Top {count} {country}", "Land wijzigen", "Kies een hitlijst", "Land zoeken",
        "Voorgesteld", "Jouw regio", "Alle landen", "Geen land komt overeen met ‘{query}’"
    ),
    "pl" to chartMarket(
        "Top {count} {country}", "Zmień kraj", "Wybierz listę przebojów", "Szukaj kraju",
        "Sugerowane", "Twój region", "Wszystkie kraje", "Brak krajów pasujących do „{query}”"
    ),
    "ro" to chartMarket(
        "Top {count} {country}", "Schimbă țara", "Alege un clasament", "Caută o țară",
        "Sugerate", "Regiunea ta", "Toate țările", "Nicio țară nu corespunde cu „{query}”"
    ),
    "el" to chartMarket(
        "Top {count} {country}", "Αλλαγή χώρας", "Επίλεξε κατάταξη", "Αναζήτηση χώρας",
        "Προτεινόμενα", "Η περιοχή σου", "Όλες οι χώρες", "Καμία χώρα δεν ταιριάζει με «{query}»"
    ),
    "sv" to chartMarket(
        "Topp {count} {country}", "Byt land", "Välj en topplista", "Sök land",
        "Förslag", "Din region", "Alla länder", "Inga länder matchar ”{query}”"
    ),
    "da" to chartMarket(
        "Top {count} {country}", "Skift land", "Vælg en hitliste", "Søg efter land",
        "Forslag", "Din region", "Alle lande", "Ingen lande matcher “{query}”"
    ),
    "cs" to chartMarket(
        "Top {count} {country}", "Změnit zemi", "Vyber žebříček", "Hledat zemi",
        "Doporučené", "Tvůj region", "Všechny země", "Žádná země neodpovídá „{query}“"
    ),
    "sk" to chartMarket(
        "Top {count} {country}", "Zmeniť krajinu", "Vyber rebríček", "Hľadať krajinu",
        "Odporúčané", "Tvoj región", "Všetky krajiny", "Žiadna krajina nezodpovedá „{query}“"
    ),
    "hr" to chartMarket(
        "Top {count} {country}", "Promijeni državu", "Odaberi ljestvicu", "Pretraži države",
        "Preporučeno", "Tvoja regija", "Sve države", "Nijedna država ne odgovara „{query}”"
    ),
    "bg" to chartMarket(
        "Топ {count} {country}", "Смени държавата", "Избери класация", "Търси държава",
        "Предложени", "Твоят регион", "Всички държави", "Няма държави, съвпадащи с „{query}“"
    ),
    "hu" to chartMarket(
        "Top {count} {country}", "Ország módosítása", "Válassz slágerlistát", "Ország keresése",
        "Javasolt", "A régiód", "Összes ország", "Egy ország sem egyezik ezzel: „{query}”"
    ),
    "fi" to chartMarket(
        "Top {count} {country}", "Vaihda maata", "Valitse lista", "Hae maata",
        "Ehdotetut", "Oma alueesi", "Kaikki maat", "Mikään maa ei vastaa hakua ”{query}”"
    ),
    "et" to chartMarket(
        "Top {count} {country}", "Muuda riiki", "Vali edetabel", "Otsi riiki",
        "Soovitatud", "Sinu piirkond", "Kõik riigid", "Päringule „{query}” ei vasta ükski riik"
    ),
    "nb" to chartMarket(
        "Topp {count} {country}", "Bytt land", "Velg en hitliste", "Søk etter land",
        "Forslag", "Din region", "Alle land", "Ingen land samsvarer med «{query}»"
    ),
    "ca" to chartMarket(
        "Top {count} {country}", "Canvia de país", "Tria una llista", "Cerca un país",
        "Suggerits", "La teva regió", "Tots els països", "Cap país coincideix amb «{query}»"
    ),
    "uk" to chartMarket(
        "Топ-{count} {country}", "Змінити країну", "Обери чарт", "Пошук країни",
        "Рекомендовані", "Твій регіон", "Усі країни", "Немає країн за запитом «{query}»"
    ),
    "ru" to chartMarket(
        "Топ-{count} {country}", "Сменить страну", "Выбери чарт", "Поиск страны",
        "Рекомендуемые", "Твой регион", "Все страны", "Нет стран по запросу «{query}»"
    ),
    "tr" to chartMarket(
        "{country} İlk {count}", "Ülkeyi değiştir", "Bir liste seç", "Ülke ara",
        "Önerilenler", "Bölgen", "Tüm ülkeler", "“{query}” ile eşleşen ülke yok"
    ),
    "ar" to chartMarket(
        "أفضل {count} في {country}", "تغيير الدولة", "اختر قائمة", "ابحث عن دولة",
        "مقترحة", "منطقتك", "كل الدول", "لا توجد دولة تطابق «{query}»"
    ),
    "fa" to chartMarket(
        "{count} آهنگ برتر {country}", "تغییر کشور", "یک فهرست انتخاب کنید", "جستجوی کشور",
        "پیشنهادی", "منطقه شما", "همه کشورها", "هیچ کشوری با «{query}» مطابقت ندارد"
    ),
    "zh" to chartMarket(
        "{country} Top {count}", "更改国家/地区", "选择榜单", "搜索国家/地区",
        "推荐", "你所在的地区", "所有国家/地区", "没有与“{query}”匹配的国家/地区"
    ),
    "zh-Hant" to chartMarket(
        "{country} Top {count}", "變更國家/地區", "選擇排行榜", "搜尋國家/地區",
        "推薦", "你所在的地區", "所有國家/地區", "沒有符合「{query}」的國家/地區"
    ),
    "ja" to chartMarket(
        "{country} Top {count}", "国・地域を変更", "チャートを選択", "国・地域を検索",
        "おすすめ", "あなたの地域", "すべての国・地域", "「{query}」に一致する国・地域はありません"
    ),
    "ko" to chartMarket(
        "{country} Top {count}", "국가 변경", "차트 선택", "국가 검색",
        "추천", "내 지역", "모든 국가", "'{query}'와(과) 일치하는 국가가 없습니다"
    ),
    "hi" to chartMarket(
        "टॉप {count} {country}", "देश बदलें", "चार्ट चुनें", "देश खोजें",
        "सुझाए गए", "आपका क्षेत्र", "सभी देश", "“{query}” से मेल खाता कोई देश नहीं"
    ),
    "id" to chartMarket(
        "Top {count} {country}", "Ganti negara", "Pilih tangga lagu", "Cari negara",
        "Disarankan", "Wilayah kamu", "Semua negara", "Tidak ada negara yang cocok dengan “{query}”"
    ),
    "ms" to chartMarket(
        "Top {count} {country}", "Tukar negara", "Pilih carta", "Cari negara",
        "Dicadangkan", "Wilayah anda", "Semua negara", "Tiada negara sepadan dengan “{query}”"
    ),
    "vi" to chartMarket(
        "Top {count} {country}", "Đổi quốc gia", "Chọn bảng xếp hạng", "Tìm quốc gia",
        "Đề xuất", "Khu vực của bạn", "Tất cả quốc gia", "Không có quốc gia nào khớp với “{query}”"
    ),
    "th" to chartMarket(
        "ท็อป {count} {country}", "เปลี่ยนประเทศ", "เลือกชาร์ต", "ค้นหาประเทศ",
        "แนะนำ", "ภูมิภาคของคุณ", "ทุกประเทศ", "ไม่พบประเทศที่ตรงกับ “{query}”"
    ),
    "fil" to chartMarket(
        "Top {count} {country}", "Palitan ang bansa", "Pumili ng chart", "Maghanap ng bansa",
        "Iminumungkahi", "Iyong rehiyon", "Lahat ng bansa", "Walang bansang tumutugma sa “{query}”"
    ),
    "he" to chartMarket(
        "{count} הגדולים · {country}", "שינוי מדינה", "בחירת מצעד", "חיפוש מדינה",
        "מוצעות", "האזור שלך", "כל המדינות", "אין מדינות שתואמות ל„{query}”"
    )
)

internal fun chartMarketLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(chartMarketBundles, code)
