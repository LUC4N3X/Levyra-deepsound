package com.luc4n3x.levyra.ui.i18n

internal val queueSelectionKeys = setOf(
    "selectAll",
    "removeFromQueue",
    "selectTrack",
    "deselectTrack"
)

private fun queueSelection(
    selectAll: String,
    removeFromQueue: String,
    selectTrack: String,
    deselectTrack: String
): Map<String, String> = mapOf(
    "selectAll" to selectAll,
    "removeFromQueue" to removeFromQueue,
    "selectTrack" to selectTrack,
    "deselectTrack" to deselectTrack
)

private val queueSelectionBundles: Map<String, Map<String, String>> = mapOf(
    "en" to queueSelection("Select all", "Remove from queue", "Select track", "Deselect track"),
    "it" to queueSelection("Seleziona tutto", "Rimuovi dalla coda", "Seleziona brano", "Deseleziona brano"),
    "es" to queueSelection("Seleccionar todo", "Quitar de la cola", "Seleccionar canción", "Anular selección"),
    "fr" to queueSelection("Tout sélectionner", "Retirer de la file", "Sélectionner le morceau", "Désélectionner le morceau"),
    "de" to queueSelection("Alle auswählen", "Aus der Warteschlange entfernen", "Titel auswählen", "Titelauswahl aufheben"),
    "pt" to queueSelection("Selecionar tudo", "Remover da fila", "Selecionar faixa", "Desmarcar faixa"),
    "nl" to queueSelection("Alles selecteren", "Uit de wachtrij verwijderen", "Nummer selecteren", "Nummer deselecteren"),
    "pl" to queueSelection("Zaznacz wszystko", "Usuń z kolejki", "Zaznacz utwór", "Odznacz utwór"),
    "ro" to queueSelection("Selectează tot", "Elimină din coadă", "Selectează piesa", "Deselectează piesa"),
    "el" to queueSelection("Επιλογή όλων", "Αφαίρεση από την ουρά", "Επιλογή κομματιού", "Κατάργηση επιλογής κομματιού"),
    "sv" to queueSelection("Markera alla", "Ta bort från kön", "Markera låt", "Avmarkera låt"),
    "da" to queueSelection("Vælg alle", "Fjern fra køen", "Vælg nummer", "Fravælg nummer"),
    "cs" to queueSelection("Vybrat vše", "Odebrat z fronty", "Vybrat skladbu", "Zrušit výběr skladby"),
    "uk" to queueSelection("Вибрати все", "Прибрати з черги", "Вибрати трек", "Скасувати вибір треку"),
    "ru" to queueSelection("Выбрать все", "Убрать из очереди", "Выбрать трек", "Снять выбор трека"),
    "tr" to queueSelection("Tümünü seç", "Sıradan kaldır", "Parçayı seç", "Parça seçimini kaldır"),
    "ar" to queueSelection("تحديد الكل", "إزالة من قائمة الانتظار", "تحديد المقطع", "إلغاء تحديد المقطع"),
    "zh" to queueSelection("全选", "从队列中移除", "选择歌曲", "取消选择歌曲"),
    "ja" to queueSelection("すべて選択", "キューから削除", "曲を選択", "曲の選択を解除"),
    "ko" to queueSelection("모두 선택", "대기열에서 제거", "트랙 선택", "트랙 선택 해제"),
    "hi" to queueSelection("सभी चुनें", "कतार से हटाएं", "ट्रैक चुनें", "ट्रैक अचयनित करें"),
    "id" to queueSelection("Pilih semua", "Hapus dari antrean", "Pilih lagu", "Batalkan pilihan lagu"),
    "vi" to queueSelection("Chọn tất cả", "Xóa khỏi hàng đợi", "Chọn bài hát", "Bỏ chọn bài hát"),
    "th" to queueSelection("เลือกทั้งหมด", "นำออกจากคิว", "เลือกเพลง", "ยกเลิกการเลือกเพลง"),
    "fil" to queueSelection("Piliin lahat", "Alisin sa queue", "Piliin ang track", "Alisin sa pagkakapili"),
    "he" to queueSelection("בחירת הכול", "הסרה מהתור", "בחירת רצועה", "ביטול בחירת רצועה")
)

internal fun queueSelectionLocalizationEntries(code: String): Map<String, String> =
    queueSelectionBundles.getValue(code)

internal fun queueSelectionLocalizationCodes(): Set<String> = queueSelectionBundles.keys
