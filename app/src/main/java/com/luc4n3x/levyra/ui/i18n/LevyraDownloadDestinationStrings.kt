package com.luc4n3x.levyra.ui.i18n

internal val downloadDestinationKeys = setOf(
    "downloadDestination",
    "downloadDestinationSubtitle",
    "downloadDestinationPrivate",
    "downloadDestinationPublic",
    "downloadSavedIn"
)

private fun downloadDestination(
    title: String,
    subtitle: String,
    privateLabel: String,
    publicLabel: String,
    savedIn: String
): Map<String, String> = mapOf(
    "downloadDestination" to title,
    "downloadDestinationSubtitle" to subtitle,
    "downloadDestinationPrivate" to privateLabel,
    "downloadDestinationPublic" to publicLabel,
    "downloadSavedIn" to savedIn
)

private val downloadDestinationBundles: Map<String, Map<String, String>> = mapOf(
    "en" to downloadDestination("Save downloads to", "Private storage keeps them inside Levyra. The Music folder saves regular audio files other apps can open.", "Private storage", "Music folder", "Saved in {destination}"),
    "it" to downloadDestination("Salva i download in", "La memoria privata li tiene dentro Levyra. La cartella Musica salva normali file audio apribili da altre app.", "Memoria privata", "Cartella Musica", "Salvato in {destination}"),
    "es" to downloadDestination("Guardar descargas en", "El almacenamiento privado las mantiene dentro de Levyra. La carpeta Música guarda archivos de audio normales que otras apps pueden abrir.", "Almacenamiento privado", "Carpeta Música", "Guardado en {destination}"),
    "fr" to downloadDestination("Enregistrer les téléchargements dans", "Le stockage privé les garde dans Levyra. Le dossier Musique enregistre des fichiers audio classiques que d’autres applis peuvent ouvrir.", "Stockage privé", "Dossier Musique", "Enregistré dans {destination}"),
    "de" to downloadDestination("Downloads speichern in", "Der private Speicher behält sie in Levyra. Der Musikordner speichert normale Audiodateien, die andere Apps öffnen können.", "Privater Speicher", "Musikordner", "Gespeichert in {destination}"),
    "pt" to downloadDestination("Guardar transferências em", "O armazenamento privado mantém-nas dentro do Levyra. A pasta Música guarda ficheiros de áudio normais que outras apps podem abrir.", "Armazenamento privado", "Pasta Música", "Guardado em {destination}"),
    "nl" to downloadDestination("Downloads opslaan in", "Privéopslag houdt ze binnen Levyra. De map Muziek bewaart gewone audiobestanden die andere apps kunnen openen.", "Privéopslag", "Map Muziek", "Opgeslagen in {destination}"),
    "pl" to downloadDestination("Zapisuj pobrane w", "Pamięć prywatna trzyma je w Levyra. Folder Muzyka zapisuje zwykłe pliki audio, które mogą otwierać inne aplikacje.", "Pamięć prywatna", "Folder Muzyka", "Zapisano w {destination}"),
    "ro" to downloadDestination("Salvează descărcările în", "Stocarea privată le păstrează în Levyra. Dosarul Muzică salvează fișiere audio obișnuite pe care le pot deschide alte aplicații.", "Stocare privată", "Dosarul Muzică", "Salvat în {destination}"),
    "el" to downloadDestination("Αποθήκευση λήψεων σε", "Ο ιδιωτικός χώρος τις κρατά μέσα στο Levyra. Ο φάκελος Μουσική αποθηκεύει κανονικά αρχεία ήχου που ανοίγουν κι άλλες εφαρμογές.", "Ιδιωτικός χώρος", "Φάκελος Μουσική", "Αποθηκεύτηκε σε {destination}"),
    "sv" to downloadDestination("Spara nedladdningar i", "Privat lagring håller dem inne i Levyra. Mappen Musik sparar vanliga ljudfiler som andra appar kan öppna.", "Privat lagring", "Mappen Musik", "Sparad i {destination}"),
    "da" to downloadDestination("Gem downloads i", "Privat lager holder dem inde i Levyra. Mappen Musik gemmer almindelige lydfiler, som andre apps kan åbne.", "Privat lager", "Mappen Musik", "Gemt i {destination}"),
    "cs" to downloadDestination("Ukládat stažené do", "Soukromé úložiště je ponechá v Levyra. Složka Hudba ukládá běžné zvukové soubory, které mohou otevřít jiné aplikace.", "Soukromé úložiště", "Složka Hudba", "Uloženo do {destination}"),
    "sk" to downloadDestination("Ukladať stiahnuté do", "Súkromné úložisko ich ponechá v Levyra. Priečinok Hudba ukladá bežné zvukové súbory, ktoré môžu otvoriť iné aplikácie.", "Súkromné úložisko", "Priečinok Hudba", "Uložené do {destination}"),
    "hr" to downloadDestination("Spremi preuzimanja u", "Privatna pohrana drži ih unutar Levyre. Mapa Glazba sprema obične audiodatoteke koje druge aplikacije mogu otvoriti.", "Privatna pohrana", "Mapa Glazba", "Spremljeno u {destination}"),
    "sr" to downloadDestination("Сачувај преузимања у", "Приватно складиште их чува унутар Levyra. Фасцикла Музика чува обичне аудио датотеке које друге апликације могу да отворе.", "Приватно складиште", "Фасцикла Музика", "Сачувано у {destination}"),
    "sq" to downloadDestination("Ruaj shkarkimet në", "Hapësira private i mban brenda Levyra. Dosja Muzikë ruan skedarë të zakonshëm audio që mund t’i hapin aplikacione të tjera.", "Hapësirë private", "Dosja Muzikë", "U ruajt në {destination}"),
    "bg" to downloadDestination("Запазване на изтеглянията в", "Частното хранилище ги пази в Levyra. Папка Музика запазва обикновени аудиофайлове, които други приложения могат да отварят.", "Частно хранилище", "Папка Музика", "Запазено в {destination}"),
    "hu" to downloadDestination("Letöltések mentése ide", "A privát tárhely a Levyrán belül tartja őket. A Zene mappa normál hangfájlokat ment, amelyeket más appok is megnyithatnak.", "Privát tárhely", "Zene mappa", "Mentve ide: {destination}"),
    "fi" to downloadDestination("Tallenna lataukset kohteeseen", "Yksityinen tallennustila pitää ne Levyrassa. Musiikki-kansioon tallentuu tavallisia äänitiedostoja, joita muut sovellukset voivat avata.", "Yksityinen tallennustila", "Musiikki-kansio", "Tallennettu kohteeseen {destination}"),
    "et" to downloadDestination("Salvesta allalaadimised asukohta", "Privaatne salvestusruum hoiab need Levyras. Kaust Muusika salvestab tavalised helifailid, mida saavad avada ka teised rakendused.", "Privaatne salvestusruum", "Kaust Muusika", "Salvestatud asukohta {destination}"),
    "nb" to downloadDestination("Lagre nedlastinger i", "Privat lagring holder dem inne i Levyra. Musikk-mappen lagrer vanlige lydfiler som andre apper kan åpne.", "Privat lagring", "Musikk-mappen", "Lagret i {destination}"),
    "ca" to downloadDestination("Desa les baixades a", "L’emmagatzematge privat les manté dins de Levyra. La carpeta Música desa fitxers d’àudio normals que altres aplicacions poden obrir.", "Emmagatzematge privat", "Carpeta Música", "Desat a {destination}"),
    "uk" to downloadDestination("Зберігати завантаження в", "Приватне сховище тримає їх у Levyra. Папка «Музика» зберігає звичайні аудіофайли, які можуть відкривати інші застосунки.", "Приватне сховище", "Папка «Музика»", "Збережено в {destination}"),
    "ru" to downloadDestination("Сохранять загрузки в", "Личное хранилище держит их внутри Levyra. Папка «Музыка» сохраняет обычные аудиофайлы, которые могут открывать другие приложения.", "Личное хранилище", "Папка «Музыка»", "Сохранено в {destination}"),
    "kk" to downloadDestination("Жүктеулерді сақтау орны", "Жеке қойма оларды Levyra ішінде сақтайды. «Музыка» қалтасы басқа қолданбалар аша алатын кәдімгі аудиофайлдарды сақтайды.", "Жеке қойма", "«Музыка» қалтасы", "{destination} ішіне сақталды"),
    "tr" to downloadDestination("İndirmeleri şuraya kaydet", "Özel depolama onları Levyra içinde tutar. Müzik klasörü, diğer uygulamaların açabileceği normal ses dosyaları kaydeder.", "Özel depolama", "Müzik klasörü", "{destination} konumuna kaydedildi"),
    "ar" to downloadDestination("حفظ التنزيلات في", "يبقيها التخزين الخاص داخل Levyra. يحفظ مجلد الموسيقى ملفات صوتية عادية يمكن للتطبيقات الأخرى فتحها.", "التخزين الخاص", "مجلد الموسيقى", "تم الحفظ في {destination}"),
    "fa" to downloadDestination("ذخیرهٔ دانلودها در", "فضای خصوصی آن‌ها را داخل Levyra نگه می‌دارد. پوشهٔ موسیقی فایل‌های صوتی معمولی ذخیره می‌کند که برنامه‌های دیگر هم می‌توانند باز کنند.", "فضای خصوصی", "پوشهٔ موسیقی", "در {destination} ذخیره شد"),
    "zh" to downloadDestination("下载保存位置", "私有存储会将文件保留在 Levyra 内。音乐文件夹会保存其他应用也能打开的普通音频文件。", "私有存储", "音乐文件夹", "已保存到 {destination}"),
    "zh-Hant" to downloadDestination("下載儲存位置", "私人儲存空間會將檔案保留在 Levyra 內。音樂資料夾會儲存其他應用程式也能開啟的一般音訊檔案。", "私人儲存空間", "音樂資料夾", "已儲存至 {destination}"),
    "ja" to downloadDestination("ダウンロードの保存先", "プライベートストレージでは Levyra 内に保存します。音楽フォルダには他のアプリでも開ける通常の音声ファイルを保存します。", "プライベートストレージ", "音楽フォルダ", "{destination} に保存しました"),
    "ko" to downloadDestination("다운로드 저장 위치", "비공개 저장공간은 Levyra 안에만 보관합니다. 음악 폴더에는 다른 앱에서도 열 수 있는 일반 오디오 파일로 저장합니다.", "비공개 저장공간", "음악 폴더", "{destination}에 저장됨"),
    "hi" to downloadDestination("डाउनलोड यहां सहेजें", "निजी स्टोरेज उन्हें Levyra के अंदर रखता है। Music फ़ोल्डर सामान्य ऑडियो फ़ाइलें सहेजता है जिन्हें दूसरे ऐप खोल सकते हैं।", "निजी स्टोरेज", "Music फ़ोल्डर", "{destination} में सहेजा गया"),
    "bn" to downloadDestination("ডাউনলোড যেখানে সংরক্ষণ হবে", "ব্যক্তিগত স্টোরেজ এগুলো Levyra-র ভেতরে রাখে। Music ফোল্ডারে সাধারণ অডিও ফাইল সংরক্ষিত হয়, যা অন্য অ্যাপও খুলতে পারে।", "ব্যক্তিগত স্টোরেজ", "Music ফোল্ডার", "{destination}-এ সংরক্ষিত"),
    "id" to downloadDestination("Simpan unduhan ke", "Penyimpanan pribadi menyimpannya di dalam Levyra. Folder Musik menyimpan file audio biasa yang dapat dibuka aplikasi lain.", "Penyimpanan pribadi", "Folder Musik", "Disimpan di {destination}"),
    "ms" to downloadDestination("Simpan muat turun ke", "Storan peribadi menyimpannya di dalam Levyra. Folder Muzik menyimpan fail audio biasa yang boleh dibuka oleh apl lain.", "Storan peribadi", "Folder Muzik", "Disimpan dalam {destination}"),
    "vi" to downloadDestination("Lưu nội dung tải xuống vào", "Bộ nhớ riêng giữ chúng trong Levyra. Thư mục Nhạc lưu các tệp âm thanh thông thường mà ứng dụng khác có thể mở.", "Bộ nhớ riêng", "Thư mục Nhạc", "Đã lưu vào {destination}"),
    "th" to downloadDestination("บันทึกการดาวน์โหลดไปที่", "พื้นที่ส่วนตัวจะเก็บไว้ใน Levyra โฟลเดอร์เพลงจะบันทึกเป็นไฟล์เสียงทั่วไปที่แอปอื่นเปิดได้", "พื้นที่ส่วนตัว", "โฟลเดอร์เพลง", "บันทึกไว้ที่ {destination}"),
    "fil" to downloadDestination("I-save ang mga download sa", "Pinapanatili ng pribadong storage ang mga ito sa loob ng Levyra. Ang Music folder ay nagse-save ng mga karaniwang audio file na mabubuksan ng ibang app.", "Pribadong storage", "Music folder", "Na-save sa {destination}"),
    "he" to downloadDestination("שמירת הורדות אל", "אחסון פרטי שומר אותן בתוך Levyra. תיקיית המוזיקה שומרת קובצי אודיו רגילים שאפליקציות אחרות יכולות לפתוח.", "אחסון פרטי", "תיקיית מוזיקה", "נשמר אל {destination}")
)

internal fun downloadDestinationLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(downloadDestinationBundles, code)

internal fun downloadDestinationLocalizationCodes(): Set<String> = downloadDestinationBundles.keys
