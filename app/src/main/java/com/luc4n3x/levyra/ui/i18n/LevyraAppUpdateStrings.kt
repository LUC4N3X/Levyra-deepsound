package com.luc4n3x.levyra.ui.i18n

internal val appUpdateKeys = setOf(
    "appUpToDate",
    "appUpdateAvailable",
    "appUpdateCheckFailed"
)

private fun appUpdate(upToDate: String, available: String, failed: String): Map<String, String> = mapOf(
    "appUpToDate" to upToDate,
    "appUpdateAvailable" to available,
    "appUpdateCheckFailed" to failed
)

private val appUpdateBundles: Map<String, Map<String, String>> = mapOf(
    "en" to appUpdate("LEVYRA is up to date", "LEVYRA {version} is available", "Couldn't check for updates"),
    "it" to appUpdate("LEVYRA è già aggiornata", "LEVYRA {version} è disponibile", "Controllo aggiornamenti non riuscito"),
    "es" to appUpdate("LEVYRA está actualizada", "LEVYRA {version} está disponible", "No se pudieron buscar actualizaciones"),
    "fr" to appUpdate("LEVYRA est à jour", "LEVYRA {version} est disponible", "Impossible de rechercher les mises à jour"),
    "de" to appUpdate("LEVYRA ist auf dem neuesten Stand", "LEVYRA {version} ist verfügbar", "Suche nach Updates fehlgeschlagen"),
    "pt" to appUpdate("O LEVYRA está atualizado", "O LEVYRA {version} está disponível", "Não foi possível verificar atualizações"),
    "nl" to appUpdate("LEVYRA is up-to-date", "LEVYRA {version} is beschikbaar", "Kan niet controleren op updates"),
    "pl" to appUpdate("LEVYRA jest aktualna", "Dostępna jest LEVYRA {version}", "Nie udało się sprawdzić aktualizacji"),
    "ro" to appUpdate("LEVYRA este actualizată", "LEVYRA {version} este disponibilă", "Verificarea actualizărilor a eșuat"),
    "el" to appUpdate("Το LEVYRA είναι ενημερωμένο", "Το LEVYRA {version} είναι διαθέσιμο", "Δεν ήταν δυνατός ο έλεγχος για ενημερώσεις"),
    "sv" to appUpdate("LEVYRA är uppdaterad", "LEVYRA {version} är tillgänglig", "Det gick inte att söka efter uppdateringar"),
    "da" to appUpdate("LEVYRA er opdateret", "LEVYRA {version} er tilgængelig", "Kunne ikke søge efter opdateringer"),
    "cs" to appUpdate("LEVYRA je aktuální", "Je dostupná LEVYRA {version}", "Kontrola aktualizací se nezdařila"),
    "sk" to appUpdate("LEVYRA je aktuálna", "Je dostupná LEVYRA {version}", "Kontrola aktualizácií zlyhala"),
    "hr" to appUpdate("LEVYRA je ažurirana", "Dostupna je LEVYRA {version}", "Provjera ažuriranja nije uspjela"),
    "sr" to appUpdate("LEVYRA је ажурирана", "Доступна је LEVYRA {version}", "Провера ажурирања није успела"),
    "sq" to appUpdate("LEVYRA është e përditësuar", "LEVYRA {version} është e disponueshme", "Kontrolli për përditësime dështoi"),
    "bg" to appUpdate("LEVYRA е актуализирана", "Налична е LEVYRA {version}", "Проверката за актуализации не бе успешна"),
    "hu" to appUpdate("A LEVYRA naprakész", "Elérhető a LEVYRA {version}", "Nem sikerült frissítéseket keresni"),
    "fi" to appUpdate("LEVYRA on ajan tasalla", "LEVYRA {version} on saatavilla", "Päivitysten tarkistus epäonnistui"),
    "et" to appUpdate("LEVYRA on ajakohane", "LEVYRA {version} on saadaval", "Värskenduste otsimine ebaõnnestus"),
    "nb" to appUpdate("LEVYRA er oppdatert", "LEVYRA {version} er tilgjengelig", "Kunne ikke se etter oppdateringer"),
    "ca" to appUpdate("LEVYRA està actualitzada", "LEVYRA {version} està disponible", "No s'han pogut cercar actualitzacions"),
    "uk" to appUpdate("LEVYRA оновлено до останньої версії", "Доступна LEVYRA {version}", "Не вдалося перевірити оновлення"),
    "ru" to appUpdate("LEVYRA обновлена до последней версии", "Доступна LEVYRA {version}", "Не удалось проверить обновления"),
    "kk" to appUpdate("LEVYRA соңғы нұсқаға жаңартылған", "LEVYRA {version} қолжетімді", "Жаңартуларды тексеру мүмкін болмады"),
    "tr" to appUpdate("LEVYRA güncel", "LEVYRA {version} kullanılabilir", "Güncellemeler denetlenemedi"),
    "ar" to appUpdate("LEVYRA محدّث", "يتوفر LEVYRA {version}", "تعذّر التحقق من التحديثات"),
    "fa" to appUpdate("LEVYRA به‌روز است", "LEVYRA {version} در دسترس است", "بررسی به‌روزرسانی‌ها ممکن نشد"),
    "zh" to appUpdate("LEVYRA 已是最新版本", "LEVYRA {version} 现已推出", "无法检查更新"),
    "zh-Hant" to appUpdate("LEVYRA 已是最新版本", "LEVYRA {version} 現已推出", "無法檢查更新"),
    "ja" to appUpdate("LEVYRA は最新です", "LEVYRA {version} が利用可能です", "アップデートを確認できませんでした"),
    "ko" to appUpdate("LEVYRA가 최신 상태입니다", "LEVYRA {version} 버전을 사용할 수 있습니다", "업데이트를 확인하지 못했습니다"),
    "hi" to appUpdate("LEVYRA अप टू डेट है", "LEVYRA {version} उपलब्ध है", "अपडेट की जाँच नहीं हो सकी"),
    "bn" to appUpdate("LEVYRA হালনাগাদ আছে", "LEVYRA {version} উপলব্ধ", "আপডেট পরীক্ষা করা যায়নি"),
    "id" to appUpdate("LEVYRA sudah versi terbaru", "LEVYRA {version} tersedia", "Gagal memeriksa pembaruan"),
    "ms" to appUpdate("LEVYRA sudah dikemas kini", "LEVYRA {version} tersedia", "Tidak dapat menyemak kemas kini"),
    "vi" to appUpdate("LEVYRA đã được cập nhật", "Đã có LEVYRA {version}", "Không thể kiểm tra bản cập nhật"),
    "th" to appUpdate("LEVYRA เป็นเวอร์ชันล่าสุดแล้ว", "LEVYRA {version} พร้อมใช้งานแล้ว", "ตรวจหาอัปเดตไม่สำเร็จ"),
    "fil" to appUpdate("Updated na ang LEVYRA", "Available na ang LEVYRA {version}", "Hindi ma-check ang mga update"),
    "he" to appUpdate("LEVYRA מעודכנת", "LEVYRA {version} זמינה", "לא ניתן היה לבדוק אם יש עדכונים")
)

internal fun appUpdateLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(appUpdateBundles, code)

internal fun appUpdateLocalizationCodes(): Set<String> = appUpdateBundles.keys
