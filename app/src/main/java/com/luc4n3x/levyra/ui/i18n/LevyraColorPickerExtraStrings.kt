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

fun colorPickerExtraCopy(code: String): ColorPickerExtraCopy = when (translationKey(code, regional = true)) {
    "it" -> ColorPickerExtraCopy("Luminosità HSL", "Rosso", "Verde", "Blu", "Recenti", "Ripristina originale", "Contrasto", "Contrasto basso", "Modello colore")
    "es" -> ColorPickerExtraCopy("Luminosidad HSL", "Rojo", "Verde", "Azul", "Recientes", "Restaurar original", "Contraste", "Contraste bajo", "Modelo de color")
    "fr" -> ColorPickerExtraCopy("Luminosité TSL", "Rouge", "Vert", "Bleu", "Récents", "Rétablir l'original", "Contraste", "Contraste faible", "Modèle de couleur")
    "de" -> ColorPickerExtraCopy("Helligkeit HSL", "Rot", "Grün", "Blau", "Zuletzt", "Original wiederherstellen", "Kontrast", "Geringer Kontrast", "Farbmodell")
    "pt" -> ColorPickerExtraCopy("Luminosidade HSL", "Vermelho", "Verde", "Azul", "Recentes", "Restaurar original", "Contraste", "Contraste baixo", "Modelo de cor")
    "ru" -> ColorPickerExtraCopy("Светлота HSL", "Красный", "Зелёный", "Синий", "Недавние", "Вернуть исходный", "Контраст", "Низкий контраст", "Цветовая модель")
    "ur" -> ColorPickerExtraCopy("ہلکا پن", "سرخ", "سبز", "نیلا", "حالیہ", "اصل کو بحال کریں۔", "کنٹراسٹ", "کم کنٹراسٹ", "رنگین ماڈل")
    "ta" -> ColorPickerExtraCopy("லேசான தன்மை", "சிவப்பு", "பச்சை", "நீலம்", "சமீபத்திய", "அசல் மீட்டமை", "மாறுபாடு", "குறைந்த மாறுபாடு", "வண்ண மாதிரி")
    "te" -> ColorPickerExtraCopy("తేలిక", "ఎరుపు", "ఆకుపచ్చ", "నీలం", "ఇటీవలి", "అసలు పునరుద్ధరించు", "కాంట్రాస్ట్", "తక్కువ కాంట్రాస్ట్", "రంగు మోడల్")
    "mr" -> ColorPickerExtraCopy("हलकेपणा", "लाल", "हिरवा", "निळा", "अलीकडील", "मूळ पुनर्संचयित करा", "कॉन्ट्रास्ट", "कमी कॉन्ट्रास्ट", "रंग मॉडेल")
    "gu" -> ColorPickerExtraCopy("હળવાશ", "લાલ", "લીલા", "વાદળી", "તાજેતરના", "મૂળ પુનઃસ્થાપિત કરો", "કોન્ટ્રાસ્ટ", "ઓછો કોન્ટ્રાસ્ટ", "રંગ મોડેલ")
    "pt-BR" -> ColorPickerExtraCopy("Leveza", "Vermelho", "Verde", "Azul", "Recente", "Restaurar original", "Contraste", "Baixo contraste", "Modelo de cor")
    else -> ColorPickerExtraCopy("Lightness", "Red", "Green", "Blue", "Recent", "Restore original", "Contrast", "Low contrast", "Color model")
}
