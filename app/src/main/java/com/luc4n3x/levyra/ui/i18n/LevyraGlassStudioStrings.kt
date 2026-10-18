package com.luc4n3x.levyra.ui.i18n

internal data class GlassStudioCopy(
    val title: String,
    val subtitle: String,
    val blur: String,
    val transparency: String,
    val tint: String,
    val refraction: String,
    val refractionSubtitle: String,
    val performanceNote: String,
    val disabledNote: String,
    val reset: String
)

internal fun LevyraStrings.glassStudioCopy(): GlassStudioCopy = when (code) {
    "it" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profondità e materiale delle superfici in vetro",
        blur = "Sfocatura",
        transparency = "Trasparenza",
        tint = "Tinta",
        refraction = "Rifrazione leggera",
        refractionSubtitle = "I bordi delle barre flottanti curvano lo sfondo (Android 13+)",
        performanceNote = "Segue Prestazioni visive: sui dispositivi limitati e con il risparmio energetico gli effetti avanzati si disattivano.",
        disabledNote = "Attiva Liquid Glass nelle impostazioni dell'interfaccia per usare questi controlli.",
        reset = "Ripristina"
    )
    "es" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profundidad y material de las superficies de cristal",
        blur = "Desenfoque",
        transparency = "Transparencia",
        tint = "Tinte",
        refraction = "Refracción ligera",
        refractionSubtitle = "Los bordes de las barras flotantes curvan el fondo (Android 13+)",
        performanceNote = "Sigue Rendimiento visual: en dispositivos limitados y con ahorro de batería se desactivan los efectos avanzados.",
        disabledNote = "Activa Liquid Glass en los ajustes de interfaz para usar estos controles.",
        reset = "Restablecer"
    )
    "fr" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profondeur et matière des surfaces en verre",
        blur = "Flou",
        transparency = "Transparence",
        tint = "Teinte",
        refraction = "Réfraction légère",
        refractionSubtitle = "Les bords des barres flottantes courbent l'arrière-plan (Android 13+)",
        performanceNote = "Suit Performances visuelles : sur les appareils limités et en économie d'énergie, les effets avancés sont désactivés.",
        disabledNote = "Activez Liquid Glass dans les réglages de l'interface pour utiliser ces commandes.",
        reset = "Réinitialiser"
    )
    "de" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Tiefe und Material der Glasflächen",
        blur = "Unschärfe",
        transparency = "Transparenz",
        tint = "Tönung",
        refraction = "Leichte Lichtbrechung",
        refractionSubtitle = "Die Ränder schwebender Leisten krümmen den Hintergrund (Android 13+)",
        performanceNote = "Folgt der visuellen Leistung: Auf schwächeren Geräten und im Energiesparmodus werden erweiterte Effekte deaktiviert.",
        disabledNote = "Aktiviere Liquid Glass in den Oberflächeneinstellungen, um diese Regler zu nutzen.",
        reset = "Zurücksetzen"
    )
    "pt" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profundidade e material das superfícies de vidro",
        blur = "Desfocagem",
        transparency = "Transparência",
        tint = "Tonalidade",
        refraction = "Refração ligeira",
        refractionSubtitle = "As margens das barras flutuantes curvam o fundo (Android 13+)",
        performanceNote = "Segue o Desempenho visual: em dispositivos limitados e com poupança de bateria os efeitos avançados são desativados.",
        disabledNote = "Ativa o Liquid Glass nas definições da interface para usar estes controlos.",
        reset = "Repor"
    )
    "ja" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "ガラス面の奥行きと質感",
        blur = "ぼかし",
        transparency = "透明度",
        tint = "色味",
        refraction = "軽い屈折",
        refractionSubtitle = "フローティングバーの縁で背景がわずかに曲がります（Android 13 以降）",
        performanceNote = "表示パフォーマンスの設定に従います。低スペック端末や省電力モードでは高度な効果をオフにします。",
        disabledNote = "これらの設定を使うには、インターフェース設定で Liquid Glass をオンにしてください。",
        reset = "リセット"
    )
    else -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Depth and material of glass surfaces",
        blur = "Blur",
        transparency = "Transparency",
        tint = "Tint",
        refraction = "Light refraction",
        refractionSubtitle = "Floating bar edges gently bend the backdrop (Android 13+)",
        performanceNote = "Follows Visual performance: advanced effects turn off on constrained devices and in battery saver.",
        disabledNote = "Turn on Liquid Glass in interface settings to use these controls.",
        reset = "Reset"
    )
}
