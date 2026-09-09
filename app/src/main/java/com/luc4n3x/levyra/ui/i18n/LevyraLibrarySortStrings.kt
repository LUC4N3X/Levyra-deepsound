package com.luc4n3x.levyra.ui.i18n

internal val librarySortKeys = setOf(
    "librarySortBy",
    "librarySortDirection",
    "librarySortNewestFirst",
    "librarySortOldestFirst",
    "librarySortLongestFirst",
    "librarySortShortestFirst",
    "librarySortAscending",
    "librarySortDescending"
)

private fun librarySort(
    sortBy: String,
    direction: String,
    newestFirst: String,
    oldestFirst: String,
    longestFirst: String,
    shortestFirst: String,
    ascending: String,
    descending: String
): Map<String, String> = mapOf(
    "librarySortBy" to sortBy,
    "librarySortDirection" to direction,
    "librarySortNewestFirst" to newestFirst,
    "librarySortOldestFirst" to oldestFirst,
    "librarySortLongestFirst" to longestFirst,
    "librarySortShortestFirst" to shortestFirst,
    "librarySortAscending" to ascending,
    "librarySortDescending" to descending
)

private val librarySortBundles: Map<String, Map<String, String>> = mapOf(
    "en" to librarySort(
        "Sort by", "Sort direction",
        "Newest first", "Oldest first",
        "Longest first", "Shortest first",
        "A to Z", "Z to A"
    ),
    "it" to librarySort(
        "Ordina per", "Direzione ordinamento",
        "Prima i più recenti", "Prima i meno recenti",
        "Prima i più lunghi", "Prima i più brevi",
        "Dalla A alla Z", "Dalla Z alla A"
    ),
    "es" to librarySort(
        "Ordenar por", "Dirección de orden",
        "Primero los más recientes", "Primero los más antiguos",
        "Primero los más largos", "Primero los más cortos",
        "De la A a la Z", "De la Z a la A"
    ),
    "fr" to librarySort(
        "Trier par", "Sens du tri",
        "Plus récents d'abord", "Plus anciens d'abord",
        "Plus longs d'abord", "Plus courts d'abord",
        "De A à Z", "De Z à A"
    ),
    "de" to librarySort(
        "Sortieren nach", "Sortierrichtung",
        "Neueste zuerst", "Älteste zuerst",
        "Längste zuerst", "Kürzeste zuerst",
        "A bis Z", "Z bis A"
    ),
    "pt" to librarySort(
        "Ordenar por", "Direção da ordenação",
        "Mais recentes primeiro", "Mais antigos primeiro",
        "Mais longos primeiro", "Mais curtos primeiro",
        "De A a Z", "De Z a A"
    ),
    "nl" to librarySort(
        "Sorteren op", "Sorteerrichting",
        "Nieuwste eerst", "Oudste eerst",
        "Langste eerst", "Kortste eerst",
        "A tot Z", "Z tot A"
    ),
    "pl" to librarySort(
        "Sortuj według", "Kierunek sortowania",
        "Najpierw najnowsze", "Najpierw najstarsze",
        "Najpierw najdłuższe", "Najpierw najkrótsze",
        "Od A do Z", "Od Z do A"
    ),
    "ro" to librarySort(
        "Sortează după", "Direcția sortării",
        "Cele mai noi întâi", "Cele mai vechi întâi",
        "Cele mai lungi întâi", "Cele mai scurte întâi",
        "De la A la Z", "De la Z la A"
    ),
    "el" to librarySort(
        "Ταξινόμηση κατά", "Κατεύθυνση ταξινόμησης",
        "Πρώτα τα νεότερα", "Πρώτα τα παλαιότερα",
        "Πρώτα τα μεγαλύτερα", "Πρώτα τα μικρότερα",
        "Από Α έως Ω", "Από Ω έως Α"
    ),
    "sv" to librarySort(
        "Sortera efter", "Sorteringsriktning",
        "Nyaste först", "Äldsta först",
        "Längsta först", "Kortaste först",
        "A till Ö", "Ö till A"
    ),
    "da" to librarySort(
        "Sortér efter", "Sorteringsretning",
        "Nyeste først", "Ældste først",
        "Længste først", "Korteste først",
        "A til Å", "Å til A"
    ),
    "cs" to librarySort(
        "Seřadit podle", "Směr řazení",
        "Nejnovější první", "Nejstarší první",
        "Nejdelší první", "Nejkratší první",
        "Od A do Z", "Od Z do A"
    ),
    "uk" to librarySort(
        "Сортувати за", "Напрямок сортування",
        "Спочатку найновіші", "Спочатку найстаріші",
        "Спочатку найдовші", "Спочатку найкоротші",
        "Від А до Я", "Від Я до А"
    ),
    "ru" to librarySort(
        "Сортировать по", "Направление сортировки",
        "Сначала новые", "Сначала старые",
        "Сначала длинные", "Сначала короткие",
        "От А до Я", "От Я до А"
    ),
    "tr" to librarySort(
        "Sıralama ölçütü", "Sıralama yönü",
        "Önce en yeniler", "Önce en eskiler",
        "Önce en uzunlar", "Önce en kısalar",
        "A'dan Z'ye", "Z'den A'ya"
    ),
    "ar" to librarySort(
        "الترتيب حسب", "اتجاه الترتيب",
        "الأحدث أولاً", "الأقدم أولاً",
        "الأطول أولاً", "الأقصر أولاً",
        "تصاعدي", "تنازلي"
    ),
    "zh" to librarySort(
        "排序方式", "排序方向",
        "最新优先", "最早优先",
        "最长优先", "最短优先",
        "升序", "降序"
    ),
    "ja" to librarySort(
        "並べ替え", "並べ替えの方向",
        "新しい順", "古い順",
        "長い順", "短い順",
        "昇順", "降順"
    ),
    "ko" to librarySort(
        "정렬 기준", "정렬 방향",
        "최신순", "오래된순",
        "긴 순", "짧은 순",
        "오름차순", "내림차순"
    ),
    "hi" to librarySort(
        "इसके अनुसार क्रमबद्ध करें", "क्रम दिशा",
        "पहले नए", "पहले पुराने",
        "पहले लंबे", "पहले छोटे",
        "आरोही क्रम", "अवरोही क्रम"
    ),
    "id" to librarySort(
        "Urutkan menurut", "Arah pengurutan",
        "Terbaru dulu", "Terlama dulu",
        "Terpanjang dulu", "Terpendek dulu",
        "A ke Z", "Z ke A"
    ),
    "vi" to librarySort(
        "Sắp xếp theo", "Hướng sắp xếp",
        "Mới nhất trước", "Cũ nhất trước",
        "Dài nhất trước", "Ngắn nhất trước",
        "A đến Z", "Z đến A"
    ),
    "th" to librarySort(
        "เรียงตาม", "ทิศทางการเรียง",
        "ใหม่สุดก่อน", "เก่าสุดก่อน",
        "ยาวสุดก่อน", "สั้นสุดก่อน",
        "จากน้อยไปมาก", "จากมากไปน้อย"
    ),
    "fil" to librarySort(
        "Pagsunud-sunurin ayon sa", "Direksyon ng pagkakasunod",
        "Pinakabago muna", "Pinakaluma muna",
        "Pinakamahaba muna", "Pinakamaikli muna",
        "A hanggang Z", "Z hanggang A"
    ),
    "he" to librarySort(
        "מיון לפי", "כיוון המיון",
        "החדשים ביותר תחילה", "הישנים ביותר תחילה",
        "הארוכים ביותר תחילה", "הקצרים ביותר תחילה",
        "בסדר עולה", "בסדר יורד"
    )
)

internal fun librarySortLocalizationEntries(code: String): Map<String, String> =
    librarySortBundles.getValue(code)

internal fun librarySortLocalizationCodes(): Set<String> = librarySortBundles.keys
