package com.luc4n3x.levyra.ui.i18n

data class PlaylistProCopy(
    val changeCover: String,
    val resetAutomaticCover: String,
    val adjustCover: String,
    val coverPreview: String,
    val cropHint: String,
    val confirmCover: String,
    val noSearchResults: String
)

fun LevyraStrings.playlistProCopy(): PlaylistProCopy = when (code) {
    "it" -> PlaylistProCopy(
        "Cambia copertina",
        "Ripristina copertina automatica",
        "Regola la copertina",
        "Anteprima copertina playlist",
        "Trascina per posizionare e pizzica per ingrandire",
        "Usa copertina",
        "Nessun brano corrisponde alla ricerca"
    )
    "es" -> PlaylistProCopy(
        "Cambiar portada",
        "Restablecer portada automática",
        "Ajustar portada",
        "Vista previa de la portada",
        "Arrastra para colocar y pellizca para ampliar",
        "Usar portada",
        "Ninguna canción coincide con la búsqueda"
    )
    "fr" -> PlaylistProCopy(
        "Changer la pochette",
        "Rétablir la pochette automatique",
        "Ajuster la pochette",
        "Aperçu de la pochette",
        "Faites glisser pour cadrer et pincez pour zoomer",
        "Utiliser la pochette",
        "Aucun titre ne correspond à la recherche"
    )
    "de" -> PlaylistProCopy(
        "Cover ändern",
        "Automatisches Cover wiederherstellen",
        "Cover anpassen",
        "Playlist-Cover-Vorschau",
        "Zum Positionieren ziehen und zum Zoomen aufziehen",
        "Cover verwenden",
        "Keine Titel entsprechen der Suche"
    )
    "pt" -> PlaylistProCopy(
        "Alterar capa",
        "Repor capa automática",
        "Ajustar capa",
        "Pré-visualização da capa",
        "Arraste para posicionar e aperte para ampliar",
        "Usar capa",
        "Nenhuma faixa corresponde à pesquisa"
    )
    "ar" -> PlaylistProCopy(
        "تغيير الغلاف",
        "استعادة الغلاف التلقائي",
        "ضبط الغلاف",
        "معاينة غلاف قائمة التشغيل",
        "اسحب لتحديد الموضع وقرّب بإصبعين للتكبير",
        "استخدام الغلاف",
        "لا توجد مقاطع مطابقة للبحث"
    )
    "he" -> PlaylistProCopy(
        "שינוי עטיפה",
        "שחזור עטיפה אוטומטית",
        "התאמת העטיפה",
        "תצוגה מקדימה של עטיפת הפלייליסט",
        "גרור למיקום וצבוט כדי לשנות את התקריב",
        "שימוש בעטיפה",
        "לא נמצאו רצועות מתאימות"
    )
    else -> PlaylistProCopy(
        "Change cover",
        "Reset to automatic cover",
        "Adjust cover",
        "Playlist cover preview",
        "Drag to position and pinch to zoom",
        "Use cover",
        "No tracks match your search"
    )
}
