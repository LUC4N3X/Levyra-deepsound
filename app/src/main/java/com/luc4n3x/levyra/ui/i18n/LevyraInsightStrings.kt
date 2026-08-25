package com.luc4n3x.levyra.ui.i18n

private fun insightStrings(
    artworkPreview: String,
    saveArtwork: String,
    artworkSaved: String,
    artworkSaveFailed: String,
    pulseRhythm: String,
    lyricsCalibrate: String
): Map<String, String> = mapOf(
    "artworkPreview" to artworkPreview,
    "saveArtwork" to saveArtwork,
    "artworkSaved" to artworkSaved,
    "artworkSaveFailed" to artworkSaveFailed,
    "pulseRhythm" to pulseRhythm,
    "lyricsCalibrate" to lyricsCalibrate
)

private val insightBundles: Map<String, Map<String, String>> = mapOf(
    "en" to insightStrings("Artwork preview", "Save artwork", "Artwork saved", "Could not save artwork", "Your rhythm", "Calibrate sync"),
    "it" to insightStrings("Anteprima copertina", "Salva copertina", "Copertina salvata", "Impossibile salvare la copertina", "Il tuo ritmo", "Calibra sync"),
    "es" to insightStrings("Vista previa de la portada", "Guardar portada", "Portada guardada", "No se pudo guardar la portada", "Tu ritmo", "Calibrar sincronía"),
    "fr" to insightStrings("Aperçu de la pochette", "Enregistrer la pochette", "Pochette enregistrée", "Impossible d'enregistrer la pochette", "Ton rythme", "Calibrer la synchro"),
    "de" to insightStrings("Cover-Vorschau", "Cover speichern", "Cover gespeichert", "Cover konnte nicht gespeichert werden", "Dein Rhythmus", "Sync kalibrieren"),
    "pt" to insightStrings("Pré-visualização da capa", "Guardar capa", "Capa guardada", "Não foi possível guardar a capa", "O teu ritmo", "Calibrar sincronização"),
    "nl" to insightStrings("Hoesvoorbeeld", "Hoes opslaan", "Hoes opgeslagen", "Hoes kon niet worden opgeslagen", "Jouw ritme", "Sync kalibreren"),
    "pl" to insightStrings("Podgląd okładki", "Zapisz okładkę", "Okładka zapisana", "Nie udało się zapisać okładki", "Twój rytm", "Kalibruj synchronizację"),
    "ro" to insightStrings("Previzualizare copertă", "Salvează coperta", "Copertă salvată", "Coperta nu a putut fi salvată", "Ritmul tău", "Calibrează sincronizarea"),
    "el" to insightStrings("Προεπισκόπηση εξωφύλλου", "Αποθήκευση εξωφύλλου", "Το εξώφυλλο αποθηκεύτηκε", "Δεν ήταν δυνατή η αποθήκευση του εξωφύλλου", "Ο ρυθμός σου", "Βαθμονόμηση συγχρονισμού"),
    "sv" to insightStrings("Förhandsvisning av omslag", "Spara omslag", "Omslag sparat", "Kunde inte spara omslaget", "Din rytm", "Kalibrera synk"),
    "da" to insightStrings("Forhåndsvisning af cover", "Gem cover", "Cover gemt", "Kunne ikke gemme coveret", "Din rytme", "Kalibrér synk"),
    "cs" to insightStrings("Náhled obalu", "Uložit obal", "Obal uložen", "Obal se nepodařilo uložit", "Tvůj rytmus", "Kalibrovat synchronizaci"),
    "uk" to insightStrings("Перегляд обкладинки", "Зберегти обкладинку", "Обкладинку збережено", "Не вдалося зберегти обкладинку", "Твій ритм", "Калібрувати синхронізацію"),
    "ru" to insightStrings("Просмотр обложки", "Сохранить обложку", "Обложка сохранена", "Не удалось сохранить обложку", "Твой ритм", "Калибровать синхронизацию"),
    "tr" to insightStrings("Kapak önizlemesi", "Kapağı kaydet", "Kapak kaydedildi", "Kapak kaydedilemedi", "Ritmin", "Senkronu ayarla"),
    "ar" to insightStrings("معاينة الغلاف", "حفظ الغلاف", "تم حفظ الغلاف", "تعذّر حفظ الغلاف", "إيقاعك", "معايرة التزامن"),
    "zh" to insightStrings("封面预览", "保存封面", "封面已保存", "无法保存封面", "你的节奏", "校准同步"),
    "ja" to insightStrings("アートワークのプレビュー", "アートワークを保存", "アートワークを保存しました", "アートワークを保存できませんでした", "あなたのリズム", "同期を調整"),
    "ko" to insightStrings("아트워크 미리보기", "아트워크 저장", "아트워크를 저장했습니다", "아트워크를 저장하지 못했습니다", "나의 리듬", "싱크 보정"),
    "hi" to insightStrings("आर्टवर्क पूर्वावलोकन", "आर्टवर्क सहेजें", "आर्टवर्क सहेजा गया", "आर्टवर्क सहेजा नहीं जा सका", "आपकी लय", "सिंक कैलिब्रेट करें"),
    "id" to insightStrings("Pratinjau sampul", "Simpan sampul", "Sampul disimpan", "Sampul gagal disimpan", "Ritmemu", "Kalibrasi sinkron"),
    "vi" to insightStrings("Xem trước ảnh bìa", "Lưu ảnh bìa", "Đã lưu ảnh bìa", "Không thể lưu ảnh bìa", "Nhịp của bạn", "Hiệu chỉnh đồng bộ"),
    "th" to insightStrings("ดูตัวอย่างปก", "บันทึกปก", "บันทึกปกแล้ว", "บันทึกปกไม่สำเร็จ", "จังหวะของคุณ", "ปรับซิงก์"),
    "fil" to insightStrings("Preview ng artwork", "I-save ang artwork", "Na-save ang artwork", "Hindi na-save ang artwork", "Ang ritmo mo", "I-calibrate ang sync"),
    "he" to insightStrings("תצוגה מקדימה של העטיפה", "שמירת העטיפה", "העטיפה נשמרה", "לא ניתן לשמור את העטיפה", "הקצב שלך", "כיול סנכרון")
)

internal fun insightLocalizationEntries(code: String): Map<String, String> = insightBundles.getValue(code)

internal fun insightLocalizationCodes(): Set<String> = insightBundles.keys
