package com.luc4n3x.levyra.ui.i18n

data class ColorPickerExtraCopy(
    val lightness: String,
    val red: String,
    val green: String,
    val blue: String,
    val recent: String,
    val restoreOriginal: String,
    val contrast: String,
    val lowContrast: String,
    val models: String
)

fun LevyraStrings.colorPickerExtraCopy(): ColorPickerExtraCopy = colorPickerExtraCopy(code)

fun colorPickerExtraCopy(code: String): ColorPickerExtraCopy = when (translationKey(code)) {
    "it" -> ColorPickerExtraCopy("Luminosità HSL", "Rosso", "Verde", "Blu", "Recenti", "Ripristina originale", "Contrasto", "Contrasto basso", "Modello colore")
    "es" -> ColorPickerExtraCopy("Luminosidad HSL", "Rojo", "Verde", "Azul", "Recientes", "Restaurar original", "Contraste", "Contraste bajo", "Modelo de color")
    "fr" -> ColorPickerExtraCopy("Luminosité TSL", "Rouge", "Vert", "Bleu", "Récents", "Rétablir l'original", "Contraste", "Contraste faible", "Modèle de couleur")
    "de" -> ColorPickerExtraCopy("Helligkeit HSL", "Rot", "Grün", "Blau", "Zuletzt", "Original wiederherstellen", "Kontrast", "Geringer Kontrast", "Farbmodell")
    "pt" -> ColorPickerExtraCopy("Luminosidade HSL", "Vermelho", "Verde", "Azul", "Recentes", "Restaurar original", "Contraste", "Contraste baixo", "Modelo de cor")
    "ru" -> ColorPickerExtraCopy("Светлота HSL", "Красный", "Зелёный", "Синий", "Недавние", "Вернуть исходный", "Контраст", "Низкий контраст", "Цветовая модель")
    else -> ColorPickerExtraCopy("Lightness", "Red", "Green", "Blue", "Recent", "Restore original", "Contrast", "Low contrast", "Color model")
}
