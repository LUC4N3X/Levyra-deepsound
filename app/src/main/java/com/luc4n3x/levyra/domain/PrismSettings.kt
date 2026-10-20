package com.luc4n3x.levyra.domain

enum class PrismAppearance { Night, Light, Automatic }
enum class PrismColors { Adaptive, Ultraviolet, Glacier }
enum class PrismPlayerLayout { Flow, Focus }
enum class PrismTranslucency { Automatic, Reduced }

data class PrismSettings(
    val appearance: PrismAppearance = PrismAppearance.Night,
    val colors: PrismColors = PrismColors.Adaptive,
    val playerLayout: PrismPlayerLayout = PrismPlayerLayout.Flow,
    val translucency: PrismTranslucency = PrismTranslucency.Automatic
) {
    companion object {
        fun from(appearance: String, colors: String, playerLayout: String, translucency: String) = PrismSettings(
            PrismAppearance.entries.firstOrNull { it.name == appearance } ?: PrismAppearance.Night,
            PrismColors.entries.firstOrNull { it.name == colors } ?: PrismColors.Adaptive,
            PrismPlayerLayout.entries.firstOrNull { it.name == playerLayout } ?: PrismPlayerLayout.Flow,
            PrismTranslucency.entries.firstOrNull { it.name == translucency } ?: PrismTranslucency.Automatic
        )
    }
}
