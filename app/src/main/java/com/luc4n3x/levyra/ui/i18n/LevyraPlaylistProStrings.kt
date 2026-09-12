package com.luc4n3x.levyra.ui.i18n

data class PlaylistProCopy(
    val changeCover: String,
    val resetAutomaticCover: String,
    val adjustCover: String,
    val coverPreview: String,
    val cropHint: String,
    val confirmCover: String,
    val zoomOut: String,
    val zoomIn: String,
    val resetCrop: String,
    val moveCoverLeft: String,
    val moveCoverRight: String,
    val moveCoverUp: String,
    val moveCoverDown: String,
    val coverUpdateFailed: String,
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
        "Riduci zoom",
        "Aumenta zoom",
        "Reimposta ritaglio",
        "Sposta copertina a sinistra",
        "Sposta copertina a destra",
        "Sposta copertina in alto",
        "Sposta copertina in basso",
        "Impossibile aggiornare la copertina",
        "Nessun brano corrisponde alla ricerca"
    )
    "es" -> PlaylistProCopy(
        "Cambiar portada",
        "Restablecer portada automática",
        "Ajustar portada",
        "Vista previa de la portada",
        "Arrastra para colocar y pellizca para ampliar",
        "Usar portada",
        "Alejar",
        "Acercar",
        "Restablecer recorte",
        "Mover portada a la izquierda",
        "Mover portada a la derecha",
        "Mover portada hacia arriba",
        "Mover portada hacia abajo",
        "No se pudo actualizar la portada",
        "Ninguna canción coincide con la búsqueda"
    )
    "fr" -> PlaylistProCopy(
        "Changer la pochette",
        "Rétablir la pochette automatique",
        "Ajuster la pochette",
        "Aperçu de la pochette",
        "Faites glisser pour cadrer et pincez pour zoomer",
        "Utiliser la pochette",
        "Dézoomer",
        "Zoomer",
        "Réinitialiser le cadrage",
        "Déplacer la pochette vers la gauche",
        "Déplacer la pochette vers la droite",
        "Déplacer la pochette vers le haut",
        "Déplacer la pochette vers le bas",
        "Impossible de mettre à jour la pochette",
        "Aucun titre ne correspond à la recherche"
    )
    "de" -> PlaylistProCopy(
        "Cover ändern",
        "Automatisches Cover wiederherstellen",
        "Cover anpassen",
        "Playlist-Cover-Vorschau",
        "Zum Positionieren ziehen und zum Zoomen aufziehen",
        "Cover verwenden",
        "Verkleinern",
        "Vergrößern",
        "Zuschnitt zurücksetzen",
        "Cover nach links verschieben",
        "Cover nach rechts verschieben",
        "Cover nach oben verschieben",
        "Cover nach unten verschieben",
        "Cover konnte nicht aktualisiert werden",
        "Keine Titel entsprechen der Suche"
    )
    "pt" -> PlaylistProCopy(
        "Alterar capa",
        "Repor capa automática",
        "Ajustar capa",
        "Pré-visualização da capa",
        "Arraste para posicionar e aperte para ampliar",
        "Usar capa",
        "Diminuir zoom",
        "Aumentar zoom",
        "Repor recorte",
        "Mover capa para a esquerda",
        "Mover capa para a direita",
        "Mover capa para cima",
        "Mover capa para baixo",
        "Não foi possível atualizar a capa",
        "Nenhuma faixa corresponde à pesquisa"
    )
    "ar" -> PlaylistProCopy(
        "تغيير الغلاف",
        "استعادة الغلاف التلقائي",
        "ضبط الغلاف",
        "معاينة غلاف قائمة التشغيل",
        "اسحب لتحديد الموضع وقرّب بإصبعين للتكبير",
        "استخدام الغلاف",
        "تصغير",
        "تكبير",
        "إعادة ضبط الاقتصاص",
        "حرّك الغلاف إلى اليسار",
        "حرّك الغلاف إلى اليمين",
        "حرّك الغلاف إلى أعلى",
        "حرّك الغلاف إلى أسفل",
        "تعذر تحديث الغلاف",
        "لا توجد مقاطع مطابقة للبحث"
    )
    "he" -> PlaylistProCopy(
        "שינוי עטיפה",
        "שחזור עטיפה אוטומטית",
        "התאמת העטיפה",
        "תצוגה מקדימה של עטיפת הפלייליסט",
        "גרור למיקום וצבוט כדי לשנות את התקריב",
        "שימוש בעטיפה",
        "התרחקות",
        "התקרבות",
        "איפוס החיתוך",
        "הזזת העטיפה שמאלה",
        "הזזת העטיפה ימינה",
        "הזזת העטיפה למעלה",
        "הזזת העטיפה למטה",
        "לא ניתן לעדכן את העטיפה",
        "לא נמצאו רצועות מתאימות"
    )
    else -> PlaylistProCopy(
        "Change cover",
        "Reset to automatic cover",
        "Adjust cover",
        "Playlist cover preview",
        "Drag to position and pinch to zoom",
        "Use cover",
        "Zoom out",
        "Zoom in",
        "Reset crop",
        "Move cover left",
        "Move cover right",
        "Move cover up",
        "Move cover down",
        "Couldn’t update the cover",
        "No tracks match your search"
    )
}
