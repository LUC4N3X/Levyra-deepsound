package com.luc4n3x.levyra.ui.i18n

internal val downloadLocationKeys = setOf(
    "downloadLocation",
    "downloadLocationSubtitle",
    "downloadLocationDefault",
    "downloadLocationReset",
    "downloadLocationResetSubtitle",
    "downloadLocationUnavailable",
    "downloadLocationPermissionFailed"
)

private fun downloadLocation(
    location: String,
    subtitle: String,
    defaultLocation: String,
    reset: String,
    resetSubtitle: String,
    unavailable: String,
    permissionFailed: String
): Map<String, String> = mapOf(
    "downloadLocation" to location,
    "downloadLocationSubtitle" to subtitle,
    "downloadLocationDefault" to defaultLocation,
    "downloadLocationReset" to reset,
    "downloadLocationResetSubtitle" to resetSubtitle,
    "downloadLocationUnavailable" to unavailable,
    "downloadLocationPermissionFailed" to permissionFailed
)

private val downloadLocationBundles: Map<String, Map<String, String>> = mapOf(
    "en" to downloadLocation("Download location", "Choose internal storage or an SD card folder", "Music/Levyra (default)", "Use default folder", "Save future downloads in Music/Levyra", "Selected folder unavailable — Music/Levyra will be used", "Levyra could not keep write access to that folder"),
    "it" to downloadLocation("Cartella dei download", "Scegli la memoria interna o una cartella sulla scheda SD", "Music/Levyra (predefinita)", "Usa cartella predefinita", "Salva i prossimi download in Music/Levyra", "Cartella selezionata non disponibile — verrà usata Music/Levyra", "Levyra non riesce a mantenere l'accesso in scrittura a questa cartella"),
    "es" to downloadLocation("Ubicación de descargas", "Elige el almacenamiento interno o una carpeta de la tarjeta SD", "Music/Levyra (predeterminada)", "Usar carpeta predeterminada", "Guardar las próximas descargas en Music/Levyra", "La carpeta seleccionada no está disponible — se usará Music/Levyra", "Levyra no pudo conservar el acceso de escritura a esa carpeta"),
    "fr" to downloadLocation("Emplacement des téléchargements", "Choisissez le stockage interne ou un dossier sur carte SD", "Music/Levyra (par défaut)", "Utiliser le dossier par défaut", "Enregistrer les prochains téléchargements dans Music/Levyra", "Dossier sélectionné indisponible — Music/Levyra sera utilisé", "Levyra n’a pas pu conserver l’accès en écriture à ce dossier"),
    "de" to downloadLocation("Download-Speicherort", "Internen Speicher oder einen Ordner auf der SD-Karte wählen", "Music/Levyra (Standard)", "Standardordner verwenden", "Künftige Downloads in Music/Levyra speichern", "Ausgewählter Ordner nicht verfügbar — Music/Levyra wird verwendet", "Levyra konnte den Schreibzugriff auf diesen Ordner nicht beibehalten"),
    "pt" to downloadLocation("Local dos downloads", "Escolha o armazenamento interno ou uma pasta no cartão SD", "Music/Levyra (padrão)", "Usar pasta padrão", "Salvar os próximos downloads em Music/Levyra", "Pasta selecionada indisponível — Music/Levyra será usada", "O Levyra não conseguiu manter acesso de escrita a essa pasta"),
    "nl" to downloadLocation("Downloadlocatie", "Kies interne opslag of een map op de SD-kaart", "Music/Levyra (standaard)", "Standaardmap gebruiken", "Sla toekomstige downloads op in Music/Levyra", "Geselecteerde map niet beschikbaar — Music/Levyra wordt gebruikt", "Levyra kon schrijftoegang tot deze map niet behouden"),
    "pl" to downloadLocation("Lokalizacja pobierania", "Wybierz pamięć wewnętrzną lub folder na karcie SD", "Music/Levyra (domyślnie)", "Użyj folderu domyślnego", "Zapisuj przyszłe pobrania w Music/Levyra", "Wybrany folder jest niedostępny — zostanie użyty Music/Levyra", "Levyra nie mogła zachować dostępu do zapisu w tym folderze"),
    "ro" to downloadLocation("Locația descărcărilor", "Alege stocarea internă sau un folder de pe cardul SD", "Music/Levyra (implicit)", "Folosește folderul implicit", "Salvează descărcările viitoare în Music/Levyra", "Folderul selectat nu este disponibil — se va folosi Music/Levyra", "Levyra nu a putut păstra accesul de scriere la acest folder"),
    "el" to downloadLocation("Τοποθεσία λήψεων", "Επιλέξτε εσωτερικό χώρο ή φάκελο στην κάρτα SD", "Music/Levyra (προεπιλογή)", "Χρήση προεπιλεγμένου φακέλου", "Αποθήκευση μελλοντικών λήψεων στο Music/Levyra", "Ο επιλεγμένος φάκελος δεν είναι διαθέσιμος — θα χρησιμοποιηθεί το Music/Levyra", "Το Levyra δεν μπόρεσε να διατηρήσει πρόσβαση εγγραφής σε αυτόν τον φάκελο"),
    "sv" to downloadLocation("Nedladdningsplats", "Välj intern lagring eller en mapp på SD-kortet", "Music/Levyra (standard)", "Använd standardmappen", "Spara framtida nedladdningar i Music/Levyra", "Vald mapp är inte tillgänglig — Music/Levyra används", "Levyra kunde inte behålla skrivåtkomst till den mappen"),
    "da" to downloadLocation("Downloadplacering", "Vælg intern lagerplads eller en mappe på SD-kortet", "Music/Levyra (standard)", "Brug standardmappen", "Gem fremtidige downloads i Music/Levyra", "Den valgte mappe er ikke tilgængelig — Music/Levyra bruges", "Levyra kunne ikke bevare skriveadgang til den mappe"),
    "cs" to downloadLocation("Umístění stažených souborů", "Vyberte interní úložiště nebo složku na SD kartě", "Music/Levyra (výchozí)", "Použít výchozí složku", "Budoucí stahování ukládat do Music/Levyra", "Vybraná složka není dostupná — použije se Music/Levyra", "Levyra nemohla zachovat oprávnění k zápisu do této složky"),
    "uk" to downloadLocation("Розташування завантажень", "Виберіть внутрішню пам’ять або папку на SD-карті", "Music/Levyra (типово)", "Використовувати типову папку", "Зберігати наступні завантаження в Music/Levyra", "Вибрана папка недоступна — буде використано Music/Levyra", "Levyra не вдалося зберегти доступ на запис до цієї папки"),
    "ru" to downloadLocation("Папка загрузок", "Выберите внутреннюю память или папку на SD-карте", "Music/Levyra (по умолчанию)", "Использовать папку по умолчанию", "Сохранять новые загрузки в Music/Levyra", "Выбранная папка недоступна — будет использована Music/Levyra", "Levyra не удалось сохранить доступ на запись к этой папке"),
    "tr" to downloadLocation("İndirme konumu", "Dahili depolamayı veya SD karttaki bir klasörü seçin", "Music/Levyra (varsayılan)", "Varsayılan klasörü kullan", "Yeni indirmeleri Music/Levyra içine kaydet", "Seçilen klasör kullanılamıyor — Music/Levyra kullanılacak", "Levyra bu klasöre yazma erişimini koruyamadı"),
    "ar" to downloadLocation("موقع التنزيل", "اختر التخزين الداخلي أو مجلدًا على بطاقة SD", "Music/Levyra (افتراضي)", "استخدام المجلد الافتراضي", "حفظ التنزيلات القادمة في Music/Levyra", "المجلد المحدد غير متاح — سيتم استخدام Music/Levyra", "تعذر على Levyra الاحتفاظ بإذن الكتابة إلى هذا المجلد"),
    "zh" to downloadLocation("下载位置", "选择内部存储或 SD 卡上的文件夹", "Music/Levyra（默认）", "使用默认文件夹", "将后续下载保存到 Music/Levyra", "所选文件夹不可用 — 将使用 Music/Levyra", "Levyra 无法保留对此文件夹的写入权限"),
    "ja" to downloadLocation("ダウンロード先", "内部ストレージまたはSDカードのフォルダーを選択", "Music/Levyra（デフォルト）", "デフォルトフォルダーを使用", "今後のダウンロードを Music/Levyra に保存", "選択したフォルダーは利用できません — Music/Levyra を使用します", "Levyra はこのフォルダーへの書き込み権限を保持できませんでした"),
    "ko" to downloadLocation("다운로드 위치", "내부 저장소 또는 SD 카드의 폴더를 선택하세요", "Music/Levyra (기본값)", "기본 폴더 사용", "앞으로의 다운로드를 Music/Levyra에 저장", "선택한 폴더를 사용할 수 없어 Music/Levyra를 사용합니다", "Levyra가 이 폴더의 쓰기 권한을 유지할 수 없습니다"),
    "hi" to downloadLocation("डाउनलोड स्थान", "आंतरिक स्टोरेज या SD कार्ड का फ़ोल्डर चुनें", "Music/Levyra (डिफ़ॉल्ट)", "डिफ़ॉल्ट फ़ोल्डर इस्तेमाल करें", "आगे के डाउनलोड Music/Levyra में सेव करें", "चुना गया फ़ोल्डर उपलब्ध नहीं है — Music/Levyra इस्तेमाल होगा", "Levyra इस फ़ोल्डर की लिखने की अनुमति बनाए नहीं रख सका"),
    "id" to downloadLocation("Lokasi unduhan", "Pilih penyimpanan internal atau folder di kartu SD", "Music/Levyra (default)", "Gunakan folder default", "Simpan unduhan berikutnya di Music/Levyra", "Folder yang dipilih tidak tersedia — Music/Levyra akan digunakan", "Levyra tidak dapat mempertahankan akses tulis ke folder tersebut"),
    "vi" to downloadLocation("Vị trí tải xuống", "Chọn bộ nhớ trong hoặc một thư mục trên thẻ SD", "Music/Levyra (mặc định)", "Dùng thư mục mặc định", "Lưu các bản tải xuống sau vào Music/Levyra", "Thư mục đã chọn không khả dụng — sẽ dùng Music/Levyra", "Levyra không thể duy trì quyền ghi vào thư mục này"),
    "th" to downloadLocation("ตำแหน่งดาวน์โหลด", "เลือกพื้นที่จัดเก็บภายในหรือโฟลเดอร์บนการ์ด SD", "Music/Levyra (ค่าเริ่มต้น)", "ใช้โฟลเดอร์เริ่มต้น", "บันทึกการดาวน์โหลดต่อไปใน Music/Levyra", "โฟลเดอร์ที่เลือกไม่พร้อมใช้งาน — จะใช้ Music/Levyra", "Levyra ไม่สามารถรักษาสิทธิ์เขียนในโฟลเดอร์นี้ได้"),
    "fil" to downloadLocation("Lokasyon ng download", "Pumili ng internal storage o folder sa SD card", "Music/Levyra (default)", "Gamitin ang default na folder", "I-save ang susunod na downloads sa Music/Levyra", "Hindi available ang napiling folder — Music/Levyra ang gagamitin", "Hindi mapanatili ng Levyra ang write access sa folder na iyon"),
    "he" to downloadLocation("מיקום הורדות", "בחרו אחסון פנימי או תיקייה בכרטיס SD", "Music/Levyra (ברירת מחדל)", "שימוש בתיקיית ברירת המחדל", "שמירת הורדות עתידיות ב-Music/Levyra", "התיקייה שנבחרה אינה זמינה — ייעשה שימוש ב-Music/Levyra", "Levyra לא הצליחה לשמור הרשאת כתיבה לתיקייה הזו")
)

internal fun downloadLocationLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(downloadLocationBundles, code)

internal fun downloadLocationLocalizationCodes(): Set<String> = supportedLocalizationCodes()
