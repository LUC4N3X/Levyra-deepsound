package com.luc4n3x.levyra.domain

enum class PrismAppearance { Night, Light, Automatic }
enum class PrismPlayerLayout { Flow, Focus }
enum class PrismTranslucency { Automatic, Reduced }

data class PrismSettings(
    val appearance: PrismAppearance = PrismAppearance.Night,
    val playerLayout: PrismPlayerLayout = PrismPlayerLayout.Flow,
    val translucency: PrismTranslucency = PrismTranslucency.Automatic
) {
    companion object {
        fun from(appearance: String, playerLayout: String, translucency: String) = PrismSettings(
            PrismAppearance.entries.firstOrNull { it.name == appearance } ?: PrismAppearance.Night,
            PrismPlayerLayout.entries.firstOrNull { it.name == playerLayout } ?: PrismPlayerLayout.Flow,
            PrismTranslucency.entries.firstOrNull { it.name == translucency } ?: PrismTranslucency.Automatic
        )
    }
}

enum class TorqueAccent { Cover, Red }

data class TorqueSettings(val accent: TorqueAccent = TorqueAccent.Cover) {
    companion object {
        fun from(accent: String) = TorqueSettings(TorqueAccent.entries.firstOrNull { it.name == accent } ?: TorqueAccent.Cover)
    }
}
