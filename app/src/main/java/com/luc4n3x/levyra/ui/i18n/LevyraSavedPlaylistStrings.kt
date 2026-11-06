package com.luc4n3x.levyra.ui.i18n

internal data class SavedPlaylistCopy(
    val save: String,
    val remove: String,
    val section: String
)

private val savedPlaylistCopies = mapOf(
    "en" to SavedPlaylistCopy("Save to library", "Remove from library", "Saved playlists"),
    "it" to SavedPlaylistCopy("Salva nella libreria", "Rimuovi dalla libreria", "Playlist salvate"),
    "es" to SavedPlaylistCopy("Guardar en la biblioteca", "Quitar de la biblioteca", "Playlists guardadas"),
    "fr" to SavedPlaylistCopy("Enregistrer dans la bibliothèque", "Retirer de la bibliothèque", "Playlists enregistrées"),
    "de" to SavedPlaylistCopy("In Mediathek speichern", "Aus Mediathek entfernen", "Gespeicherte Playlists"),
    "pt" to SavedPlaylistCopy("Guardar na biblioteca", "Remover da biblioteca", "Playlists guardadas"),
    "pt-BR" to SavedPlaylistCopy("Salvar na biblioteca", "Remover da biblioteca", "Playlists salvas"),
    "nl" to SavedPlaylistCopy("Opslaan in bibliotheek", "Verwijderen uit bibliotheek", "Opgeslagen playlists"),
    "pl" to SavedPlaylistCopy("Zapisz w bibliotece", "Usuń z biblioteki", "Zapisane playlisty"),
    "ro" to SavedPlaylistCopy("Salvează în bibliotecă", "Elimină din bibliotecă", "Playlisturi salvate"),
    "el" to SavedPlaylistCopy("Αποθήκευση στη βιβλιοθήκη", "Αφαίρεση από τη βιβλιοθήκη", "Αποθηκευμένα playlist"),
    "sv" to SavedPlaylistCopy("Spara i biblioteket", "Ta bort från biblioteket", "Sparade spellistor"),
    "da" to SavedPlaylistCopy("Gem i biblioteket", "Fjern fra biblioteket", "Gemte playlister"),
    "cs" to SavedPlaylistCopy("Uložit do knihovny", "Odebrat z knihovny", "Uložené playlisty"),
    "sk" to SavedPlaylistCopy("Uložiť do knižnice", "Odstrániť z knižnice", "Uložené playlisty"),
    "hr" to SavedPlaylistCopy("Spremi u biblioteku", "Ukloni iz biblioteke", "Spremljene playliste"),
    "sr" to SavedPlaylistCopy("Сачувај у библиотеку", "Уклони из библиотеке", "Сачуване плејлисте"),
    "sq" to SavedPlaylistCopy("Ruaje në bibliotekë", "Hiqe nga biblioteka", "Playlist të ruajtura"),
    "bg" to SavedPlaylistCopy("Запази в библиотеката", "Премахни от библиотеката", "Запазени плейлисти"),
    "hu" to SavedPlaylistCopy("Mentés a könyvtárba", "Eltávolítás a könyvtárból", "Mentett lejátszási listák"),
    "fi" to SavedPlaylistCopy("Tallenna kirjastoon", "Poista kirjastosta", "Tallennetut soittolistat"),
    "et" to SavedPlaylistCopy("Salvesta kogusse", "Eemalda kogust", "Salvestatud esitusloendid"),
    "nb" to SavedPlaylistCopy("Lagre i biblioteket", "Fjern fra biblioteket", "Lagrede spillelister"),
    "ca" to SavedPlaylistCopy("Desa a la biblioteca", "Treu de la biblioteca", "Llistes desades"),
    "uk" to SavedPlaylistCopy("Зберегти в бібліотеці", "Видалити з бібліотеки", "Збережені плейлисти"),
    "ru" to SavedPlaylistCopy("Сохранить в медиатеку", "Удалить из медиатеки", "Сохранённые плейлисты"),
    "kk" to SavedPlaylistCopy("Кітапханаға сақтау", "Кітапханадан жою", "Сақталған плейлистер"),
    "tr" to SavedPlaylistCopy("Kitaplığa kaydet", "Kitaplıktan kaldır", "Kaydedilen çalma listeleri"),
    "ar" to SavedPlaylistCopy("حفظ في المكتبة", "إزالة من المكتبة", "قوائم التشغيل المحفوظة"),
    "fa" to SavedPlaylistCopy("ذخیره در کتابخانه", "حذف از کتابخانه", "فهرست‌های پخش ذخیره‌شده"),
    "zh" to SavedPlaylistCopy("保存到音乐库", "从音乐库移除", "已保存的歌单"),
    "zh-Hant" to SavedPlaylistCopy("儲存到音樂庫", "從音樂庫移除", "已儲存的播放清單"),
    "ja" to SavedPlaylistCopy("ライブラリに保存", "ライブラリから削除", "保存したプレイリスト"),
    "ko" to SavedPlaylistCopy("보관함에 저장", "보관함에서 삭제", "저장한 플레이리스트"),
    "hi" to SavedPlaylistCopy("लाइब्रेरी में सहेजें", "लाइब्रेरी से हटाएं", "सहेजी गई प्लेलिस्ट"),
    "bn" to SavedPlaylistCopy("লাইব্রেরিতে সংরক্ষণ করুন", "লাইব্রেরি থেকে সরান", "সংরক্ষিত প্লেলিস্ট"),
    "id" to SavedPlaylistCopy("Simpan ke koleksi", "Hapus dari koleksi", "Playlist tersimpan"),
    "ms" to SavedPlaylistCopy("Simpan ke pustaka", "Alih keluar dari pustaka", "Senarai main disimpan"),
    "vi" to SavedPlaylistCopy("Lưu vào thư viện", "Xóa khỏi thư viện", "Danh sách phát đã lưu"),
    "th" to SavedPlaylistCopy("บันทึกลงคลังเพลง", "นำออกจากคลังเพลง", "เพลย์ลิสต์ที่บันทึกไว้"),
    "fil" to SavedPlaylistCopy("I-save sa library", "Alisin sa library", "Mga naka-save na playlist"),
    "he" to SavedPlaylistCopy("שמירה בספרייה", "הסרה מהספרייה", "פלייליסטים שמורים")
)

internal fun savedPlaylistCopy(code: String): SavedPlaylistCopy = localizedValueOrEnglish(savedPlaylistCopies, code)

internal fun savedPlaylistLocalizationCodes(): Set<String> = savedPlaylistCopies.keys
