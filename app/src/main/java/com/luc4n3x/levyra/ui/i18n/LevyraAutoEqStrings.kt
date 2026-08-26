package com.luc4n3x.levyra.ui.i18n

private fun autoEqStrings(
    importLabel: String,
    hint: String,
    pickFile: String,
    applyLabel: String,
    savePreset: String,
    presetName: String,
    invalid: String,
    tooLarge: String,
    adjusted: String
): Map<String, String> = mapOf(
    "autoEqImport" to importLabel,
    "autoEqImportHint" to hint,
    "autoEqPickFile" to pickFile,
    "autoEqApply" to applyLabel,
    "autoEqSavePreset" to savePreset,
    "autoEqPresetName" to presetName,
    "autoEqInvalidProfile" to invalid,
    "autoEqInputTooLarge" to tooLarge,
    "autoEqAdjustedNotice" to adjusted
)

private val autoEqBundles: Map<String, Map<String, String>> = mapOf(
    "en" to autoEqStrings(
        "Import AutoEQ profile",
        "Paste a GraphicEQ profile or open a text file",
        "Open file",
        "Apply",
        "Save as preset",
        "Preset name",
        "Profile not valid",
        "File too large",
        "Gains interpolated and limited to the Levyra range"
    ),
    "it" to autoEqStrings(
        "Importa profilo AutoEQ",
        "Incolla un profilo GraphicEQ o apri un file di testo",
        "Apri file",
        "Applica",
        "Salva come preset",
        "Nome preset",
        "Profilo non valido",
        "File troppo grande",
        "Guadagni interpolati e limitati all'intervallo Levyra"
    ),
    "es" to autoEqStrings(
        "Importar perfil AutoEQ",
        "Pega un perfil GraphicEQ o abre un archivo de texto",
        "Abrir archivo",
        "Aplicar",
        "Guardar como preajuste",
        "Nombre del preajuste",
        "Perfil no válido",
        "Archivo demasiado grande",
        "Ganancias interpoladas y limitadas al rango de Levyra"
    ),
    "fr" to autoEqStrings(
        "Importer un profil AutoEQ",
        "Colle un profil GraphicEQ ou ouvre un fichier texte",
        "Ouvrir un fichier",
        "Appliquer",
        "Enregistrer comme préréglage",
        "Nom du préréglage",
        "Profil non valide",
        "Fichier trop volumineux",
        "Gains interpolés et limités à la plage Levyra"
    ),
    "de" to autoEqStrings(
        "AutoEQ-Profil importieren",
        "GraphicEQ-Profil einfügen oder Textdatei öffnen",
        "Datei öffnen",
        "Anwenden",
        "Als Preset speichern",
        "Preset-Name",
        "Profil ungültig",
        "Datei zu groß",
        "Verstärkungen interpoliert und auf den Levyra-Bereich begrenzt"
    ),
    "pt" to autoEqStrings(
        "Importar perfil AutoEQ",
        "Cola um perfil GraphicEQ ou abre um ficheiro de texto",
        "Abrir ficheiro",
        "Aplicar",
        "Guardar como predefinição",
        "Nome da predefinição",
        "Perfil inválido",
        "Ficheiro demasiado grande",
        "Ganhos interpolados e limitados ao intervalo Levyra"
    ),
    "nl" to autoEqStrings(
        "AutoEQ-profiel importeren",
        "Plak een GraphicEQ-profiel of open een tekstbestand",
        "Bestand openen",
        "Toepassen",
        "Opslaan als preset",
        "Presetnaam",
        "Profiel ongeldig",
        "Bestand te groot",
        "Versterkingen geïnterpoleerd en beperkt tot het Levyra-bereik"
    ),
    "pl" to autoEqStrings(
        "Importuj profil AutoEQ",
        "Wklej profil GraphicEQ lub otwórz plik tekstowy",
        "Otwórz plik",
        "Zastosuj",
        "Zapisz jako ustawienie",
        "Nazwa ustawienia",
        "Profil nieprawidłowy",
        "Plik zbyt duży",
        "Wzmocnienia interpolowane i ograniczone do zakresu Levyra"
    ),
    "ro" to autoEqStrings(
        "Importă profil AutoEQ",
        "Lipește un profil GraphicEQ sau deschide un fișier text",
        "Deschide fișier",
        "Aplică",
        "Salvează ca presetare",
        "Nume presetare",
        "Profil nevalid",
        "Fișier prea mare",
        "Câștiguri interpolate și limitate la intervalul Levyra"
    ),
    "el" to autoEqStrings(
        "Εισαγωγή προφίλ AutoEQ",
        "Επικόλλησε προφίλ GraphicEQ ή άνοιξε αρχείο κειμένου",
        "Άνοιγμα αρχείου",
        "Εφαρμογή",
        "Αποθήκευση ως προεπιλογή",
        "Όνομα προεπιλογής",
        "Μη έγκυρο προφίλ",
        "Πολύ μεγάλο αρχείο",
        "Οι ενισχύσεις παρεμβλήθηκαν και περιορίστηκαν στο εύρος Levyra"
    ),
    "sv" to autoEqStrings(
        "Importera AutoEQ-profil",
        "Klistra in en GraphicEQ-profil eller öppna en textfil",
        "Öppna fil",
        "Tillämpa",
        "Spara som förinställning",
        "Namn på förinställning",
        "Ogiltig profil",
        "Filen är för stor",
        "Förstärkningar interpolerade och begränsade till Levyras intervall"
    ),
    "da" to autoEqStrings(
        "Importér AutoEQ-profil",
        "Indsæt en GraphicEQ-profil, eller åbn en tekstfil",
        "Åbn fil",
        "Anvend",
        "Gem som forudindstilling",
        "Navn på forudindstilling",
        "Ugyldig profil",
        "Filen er for stor",
        "Forstærkninger interpoleret og begrænset til Levyras område"
    ),
    "cs" to autoEqStrings(
        "Importovat profil AutoEQ",
        "Vlož profil GraphicEQ nebo otevři textový soubor",
        "Otevřít soubor",
        "Použít",
        "Uložit jako předvolbu",
        "Název předvolby",
        "Neplatný profil",
        "Soubor je příliš velký",
        "Zisky interpolovány a omezeny na rozsah Levyra"
    ),
    "uk" to autoEqStrings(
        "Імпортувати профіль AutoEQ",
        "Встав профіль GraphicEQ або відкрий текстовий файл",
        "Відкрити файл",
        "Застосувати",
        "Зберегти як пресет",
        "Назва пресета",
        "Недійсний профіль",
        "Файл завеликий",
        "Підсилення інтерпольовані та обмежені діапазоном Levyra"
    ),
    "ru" to autoEqStrings(
        "Импорт профиля AutoEQ",
        "Вставьте профиль GraphicEQ или откройте текстовый файл",
        "Открыть файл",
        "Применить",
        "Сохранить как пресет",
        "Название пресета",
        "Недопустимый профиль",
        "Файл слишком большой",
        "Усиления интерполированы и ограничены диапазоном Levyra"
    ),
    "tr" to autoEqStrings(
        "AutoEQ profili içe aktar",
        "GraphicEQ profili yapıştır veya metin dosyası aç",
        "Dosya aç",
        "Uygula",
        "Ön ayar olarak kaydet",
        "Ön ayar adı",
        "Profil geçersiz",
        "Dosya çok büyük",
        "Kazançlar ara değerlendi ve Levyra aralığına sınırlandı"
    ),
    "ar" to autoEqStrings(
        "استيراد ملف AutoEQ",
        "الصق ملف GraphicEQ أو افتح ملفًا نصيًا",
        "فتح ملف",
        "تطبيق",
        "حفظ كإعداد مسبق",
        "اسم الإعداد المسبق",
        "الملف غير صالح",
        "الملف كبير جدًا",
        "تم استيفاء المكاسب وتقييدها ضمن نطاق Levyra"
    ),
    "zh" to autoEqStrings(
        "导入 AutoEQ 配置",
        "粘贴 GraphicEQ 配置或打开文本文件",
        "打开文件",
        "应用",
        "另存为预设",
        "预设名称",
        "配置无效",
        "文件过大",
        "增益已插值并限制在 Levyra 范围内"
    ),
    "ja" to autoEqStrings(
        "AutoEQ プロファイルを読み込む",
        "GraphicEQ プロファイルを貼り付けるかテキストファイルを開く",
        "ファイルを開く",
        "適用",
        "プリセットとして保存",
        "プリセット名",
        "プロファイルが無効です",
        "ファイルが大きすぎます",
        "ゲインを補間し Levyra の範囲に制限しました"
    ),
    "ko" to autoEqStrings(
        "AutoEQ 프로필 가져오기",
        "GraphicEQ 프로필을 붙여넣거나 텍스트 파일을 여세요",
        "파일 열기",
        "적용",
        "프리셋으로 저장",
        "프리셋 이름",
        "프로필이 유효하지 않습니다",
        "파일이 너무 큽니다",
        "게인을 보간하고 Levyra 범위로 제한했습니다"
    ),
    "hi" to autoEqStrings(
        "AutoEQ प्रोफ़ाइल आयात करें",
        "GraphicEQ प्रोफ़ाइल चिपकाएँ या टेक्स्ट फ़ाइल खोलें",
        "फ़ाइल खोलें",
        "लागू करें",
        "प्रीसेट के रूप में सहेजें",
        "प्रीसेट नाम",
        "प्रोफ़ाइल मान्य नहीं",
        "फ़ाइल बहुत बड़ी है",
        "गेन इंटरपोलेट किए और Levyra सीमा तक सीमित किए गए"
    ),
    "id" to autoEqStrings(
        "Impor profil AutoEQ",
        "Tempel profil GraphicEQ atau buka berkas teks",
        "Buka berkas",
        "Terapkan",
        "Simpan sebagai preset",
        "Nama preset",
        "Profil tidak valid",
        "Berkas terlalu besar",
        "Penguatan diinterpolasi dan dibatasi ke rentang Levyra"
    ),
    "vi" to autoEqStrings(
        "Nhập hồ sơ AutoEQ",
        "Dán hồ sơ GraphicEQ hoặc mở tệp văn bản",
        "Mở tệp",
        "Áp dụng",
        "Lưu thành cấu hình",
        "Tên cấu hình",
        "Hồ sơ không hợp lệ",
        "Tệp quá lớn",
        "Độ lợi đã nội suy và giới hạn theo dải Levyra"
    ),
    "th" to autoEqStrings(
        "นำเข้าโปรไฟล์ AutoEQ",
        "วางโปรไฟล์ GraphicEQ หรือเปิดไฟล์ข้อความ",
        "เปิดไฟล์",
        "ใช้งาน",
        "บันทึกเป็นพรีเซ็ต",
        "ชื่อพรีเซ็ต",
        "โปรไฟล์ไม่ถูกต้อง",
        "ไฟล์ใหญ่เกินไป",
        "ค่าเกนถูกประมาณค่าและจำกัดในช่วงของ Levyra"
    ),
    "fil" to autoEqStrings(
        "Mag-import ng AutoEQ profile",
        "I-paste ang GraphicEQ profile o magbukas ng text file",
        "Buksan ang file",
        "Ilapat",
        "I-save bilang preset",
        "Pangalan ng preset",
        "Hindi wasto ang profile",
        "Masyadong malaki ang file",
        "Na-interpolate ang gains at nilimitahan sa saklaw ng Levyra"
    ),
    "he" to autoEqStrings(
        "ייבוא פרופיל AutoEQ",
        "הדביקו פרופיל GraphicEQ או פתחו קובץ טקסט",
        "פתיחת קובץ",
        "החלה",
        "שמירה כהגדרה",
        "שם ההגדרה",
        "הפרופיל אינו תקין",
        "הקובץ גדול מדי",
        "העצמות חושבו באינטרפולציה והוגבלו לטווח של Levyra"
    )
)

internal fun autoEqLocalizationEntries(code: String): Map<String, String> = autoEqBundles.getValue(code)

internal fun autoEqLocalizationCodes(): Set<String> = autoEqBundles.keys
