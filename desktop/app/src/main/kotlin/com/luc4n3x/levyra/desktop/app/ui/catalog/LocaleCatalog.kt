package com.luc4n3x.levyra.desktop.app.ui.catalog

import com.luc4n3x.levyra.desktop.core.model.AppLanguage

data class CountryOption(
    val code: String,
    val flag: String,
    val nativeName: String,
    val englishName: String
) {
    val displayLabel: String get() = "$flag $nativeName"
}

object LocaleCatalog {
    val languages: List<AppLanguage> = AppLanguage.entries

    val countries: List<CountryOption> = listOf(
        CountryOption("IT", "🇮🇹", "Italia", "Italy"),
        CountryOption("US", "🇺🇸", "United States", "United States"),
        CountryOption("GB", "🇬🇧", "United Kingdom", "United Kingdom"),
        CountryOption("ES", "🇪🇸", "España", "Spain"),
        CountryOption("FR", "🇫🇷", "France", "France"),
        CountryOption("DE", "🇩🇪", "Deutschland", "Germany"),
        CountryOption("BR", "🇧🇷", "Brasil", "Brazil"),
        CountryOption("PT", "🇵🇹", "Portugal", "Portugal"),
        CountryOption("NL", "🇳🇱", "Nederland", "Netherlands"),
        CountryOption("PL", "🇵🇱", "Polska", "Poland"),
        CountryOption("RO", "🇷🇴", "România", "Romania"),
        CountryOption("GR", "🇬🇷", "Ελλάδα", "Greece"),
        CountryOption("SE", "🇸🇪", "Sverige", "Sweden"),
        CountryOption("DK", "🇩🇰", "Danmark", "Denmark"),
        CountryOption("CZ", "🇨🇿", "Česko", "Czechia"),
        CountryOption("UA", "🇺🇦", "Україна", "Ukraine"),
        CountryOption("RU", "🇷🇺", "Россия", "Russia"),
        CountryOption("TR", "🇹🇷", "Türkiye", "Türkiye"),
        CountryOption("SA", "🇸🇦", "السعودية", "Saudi Arabia"),
        CountryOption("CN", "🇨🇳", "中国", "China"),
        CountryOption("JP", "🇯🇵", "日本", "Japan"),
        CountryOption("KR", "🇰🇷", "대한민국", "South Korea"),
        CountryOption("IN", "🇮🇳", "भारत", "India"),
        CountryOption("ID", "🇮🇩", "Indonesia", "Indonesia"),
        CountryOption("VN", "🇻🇳", "Việt Nam", "Vietnam"),
        CountryOption("TH", "🇹🇭", "ประเทศไทย", "Thailand"),
        CountryOption("PH", "🇵🇭", "Pilipinas", "Philippines"),
        CountryOption("IL", "🇮🇱", "ישראל", "Israel"),
        CountryOption("MX", "🇲🇽", "México", "Mexico"),
        CountryOption("CA", "🇨🇦", "Canada", "Canada"),
        CountryOption("AU", "🇦🇺", "Australia", "Australia")
    )

    fun country(code: String): CountryOption = countries.firstOrNull {
        it.code.equals(code, ignoreCase = true)
    } ?: countries.first()

    fun countryForLanguage(language: AppLanguage): CountryOption = country(language.defaultCountry)
}
