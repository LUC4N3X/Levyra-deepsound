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

private fun autoEqCatalogStrings(
    title: String,
    hint: String,
    search: String,
    loading: String,
    empty: String,
    unavailable: String,
    profileFailed: String,
    retry: String,
    attribution: String
): Map<String, String> = mapOf(
    "autoEqCatalog" to title,
    "autoEqCatalogHint" to hint,
    "autoEqCatalogSearch" to search,
    "autoEqCatalogLoading" to loading,
    "autoEqCatalogEmpty" to empty,
    "autoEqCatalogUnavailable" to unavailable,
    "autoEqCatalogProfileFailed" to profileFailed,
    "autoEqCatalogRetry" to retry,
    "autoEqCatalogAttribution" to attribution
)

private val autoEqCatalogBundles: Map<String, Map<String, String>> = mapOf(
    "en" to autoEqCatalogStrings(
        "Headphone catalog",
        "Find your headphones and load their AutoEQ correction",
        "Search brand or model",
        "Loading catalog…",
        "No matching headphones",
        "Catalog unavailable. Check your connection.",
        "Profile download failed",
        "Retry",
        "Measurements from the AutoEq project (MIT)"
    ),
    "it" to autoEqCatalogStrings(
        "Catalogo cuffie",
        "Trova le tue cuffie e carica la loro correzione AutoEQ",
        "Cerca marca o modello",
        "Caricamento catalogo…",
        "Nessuna cuffia trovata",
        "Catalogo non disponibile. Controlla la connessione.",
        "Download del profilo non riuscito",
        "Riprova",
        "Misure dal progetto AutoEq (MIT)"
    ),
    "es" to autoEqCatalogStrings(
        "Catálogo de auriculares",
        "Busca tus auriculares y carga su corrección AutoEQ",
        "Buscar marca o modelo",
        "Cargando catálogo…",
        "No se encontraron auriculares",
        "Catálogo no disponible. Comprueba tu conexión.",
        "No se pudo descargar el perfil",
        "Reintentar",
        "Mediciones del proyecto AutoEq (MIT)"
    ),
    "fr" to autoEqCatalogStrings(
        "Catalogue de casques",
        "Trouvez votre casque et chargez sa correction AutoEQ",
        "Rechercher une marque ou un modèle",
        "Chargement du catalogue…",
        "Aucun casque correspondant",
        "Catalogue indisponible. Vérifiez votre connexion.",
        "Échec du téléchargement du profil",
        "Réessayer",
        "Mesures issues du projet AutoEq (MIT)"
    ),
    "de" to autoEqCatalogStrings(
        "Kopfhörerkatalog",
        "Finde deine Kopfhörer und lade ihre AutoEQ-Korrektur",
        "Marke oder Modell suchen",
        "Katalog wird geladen…",
        "Keine passenden Kopfhörer",
        "Katalog nicht verfügbar. Prüfe deine Verbindung.",
        "Profil konnte nicht geladen werden",
        "Erneut versuchen",
        "Messungen aus dem AutoEq-Projekt (MIT)"
    ),
    "pt" to autoEqCatalogStrings(
        "Catálogo de fones",
        "Encontre seus fones e carregue a correção AutoEQ deles",
        "Pesquisar marca ou modelo",
        "Carregando catálogo…",
        "Nenhum fone encontrado",
        "Catálogo indisponível. Verifique sua conexão.",
        "Falha ao baixar o perfil",
        "Tentar novamente",
        "Medições do projeto AutoEq (MIT)"
    ),
    "nl" to autoEqCatalogStrings(
        "Koptelefooncatalogus",
        "Zoek je koptelefoon en laad de AutoEQ-correctie",
        "Zoek merk of model",
        "Catalogus laden…",
        "Geen koptelefoons gevonden",
        "Catalogus niet beschikbaar. Controleer je verbinding.",
        "Profiel downloaden mislukt",
        "Opnieuw proberen",
        "Metingen van het AutoEq-project (MIT)"
    ),
    "pl" to autoEqCatalogStrings(
        "Katalog słuchawek",
        "Znajdź swoje słuchawki i wczytaj ich korekcję AutoEQ",
        "Szukaj marki lub modelu",
        "Wczytywanie katalogu…",
        "Nie znaleziono słuchawek",
        "Katalog niedostępny. Sprawdź połączenie.",
        "Nie udało się pobrać profilu",
        "Spróbuj ponownie",
        "Pomiary z projektu AutoEq (MIT)"
    ),
    "ro" to autoEqCatalogStrings(
        "Catalog de căști",
        "Găsește-ți căștile și încarcă corecția lor AutoEQ",
        "Caută marcă sau model",
        "Se încarcă catalogul…",
        "Nu s-au găsit căști",
        "Catalog indisponibil. Verifică conexiunea.",
        "Descărcarea profilului a eșuat",
        "Reîncearcă",
        "Măsurători din proiectul AutoEq (MIT)"
    ),
    "el" to autoEqCatalogStrings(
        "Κατάλογος ακουστικών",
        "Βρείτε τα ακουστικά σας και φορτώστε τη διόρθωση AutoEQ",
        "Αναζήτηση μάρκας ή μοντέλου",
        "Φόρτωση καταλόγου…",
        "Δεν βρέθηκαν ακουστικά",
        "Ο κατάλογος δεν είναι διαθέσιμος. Ελέγξτε τη σύνδεσή σας.",
        "Η λήψη του προφίλ απέτυχε",
        "Επανάληψη",
        "Μετρήσεις από το έργο AutoEq (MIT)"
    ),
    "sv" to autoEqCatalogStrings(
        "Hörlurskatalog",
        "Hitta dina hörlurar och läs in deras AutoEQ-korrigering",
        "Sök märke eller modell",
        "Läser in katalog…",
        "Inga matchande hörlurar",
        "Katalogen är inte tillgänglig. Kontrollera anslutningen.",
        "Det gick inte att hämta profilen",
        "Försök igen",
        "Mätningar från AutoEq-projektet (MIT)"
    ),
    "da" to autoEqCatalogStrings(
        "Hovedtelefonkatalog",
        "Find dine hovedtelefoner og indlæs deres AutoEQ-korrektion",
        "Søg efter mærke eller model",
        "Indlæser katalog…",
        "Ingen matchende hovedtelefoner",
        "Kataloget er ikke tilgængeligt. Tjek din forbindelse.",
        "Download af profil mislykkedes",
        "Prøv igen",
        "Målinger fra AutoEq-projektet (MIT)"
    ),
    "cs" to autoEqCatalogStrings(
        "Katalog sluchátek",
        "Najděte svá sluchátka a načtěte jejich korekci AutoEQ",
        "Hledat značku nebo model",
        "Načítání katalogu…",
        "Žádná odpovídající sluchátka",
        "Katalog není dostupný. Zkontrolujte připojení.",
        "Stažení profilu se nezdařilo",
        "Zkusit znovu",
        "Měření z projektu AutoEq (MIT)"
    ),
    "uk" to autoEqCatalogStrings(
        "Каталог навушників",
        "Знайдіть свої навушники та завантажте їхню корекцію AutoEQ",
        "Пошук бренду або моделі",
        "Завантаження каталогу…",
        "Навушників не знайдено",
        "Каталог недоступний. Перевірте з’єднання.",
        "Не вдалося завантажити профіль",
        "Повторити",
        "Вимірювання з проєкту AutoEq (MIT)"
    ),
    "ru" to autoEqCatalogStrings(
        "Каталог наушников",
        "Найдите свои наушники и загрузите их коррекцию AutoEQ",
        "Поиск бренда или модели",
        "Загрузка каталога…",
        "Наушники не найдены",
        "Каталог недоступен. Проверьте подключение.",
        "Не удалось загрузить профиль",
        "Повторить",
        "Измерения из проекта AutoEq (MIT)"
    ),
    "tr" to autoEqCatalogStrings(
        "Kulaklık kataloğu",
        "Kulaklığınızı bulun ve AutoEQ düzeltmesini yükleyin",
        "Marka veya model ara",
        "Katalog yükleniyor…",
        "Eşleşen kulaklık yok",
        "Katalog kullanılamıyor. Bağlantınızı kontrol edin.",
        "Profil indirilemedi",
        "Yeniden dene",
        "AutoEq projesinden ölçümler (MIT)"
    ),
    "ar" to autoEqCatalogStrings(
        "كتالوج سماعات الرأس",
        "اعثر على سماعاتك وحمّل تصحيح AutoEQ الخاص بها",
        "ابحث عن علامة تجارية أو طراز",
        "جارٍ تحميل الكتالوج…",
        "لا توجد سماعات مطابقة",
        "الكتالوج غير متاح. تحقق من اتصالك.",
        "تعذّر تنزيل الملف الشخصي",
        "إعادة المحاولة",
        "قياسات من مشروع AutoEq (MIT)"
    ),
    "zh" to autoEqCatalogStrings(
        "耳机目录",
        "找到你的耳机并加载其 AutoEQ 校正",
        "搜索品牌或型号",
        "正在加载目录…",
        "没有匹配的耳机",
        "目录不可用。请检查网络连接。",
        "配置文件下载失败",
        "重试",
        "测量数据来自 AutoEq 项目（MIT）"
    ),
    "ja" to autoEqCatalogStrings(
        "ヘッドホンカタログ",
        "お使いのヘッドホンを探して AutoEQ 補正を読み込みます",
        "ブランドまたはモデルを検索",
        "カタログを読み込み中…",
        "一致するヘッドホンがありません",
        "カタログを利用できません。接続を確認してください。",
        "プロファイルをダウンロードできませんでした",
        "再試行",
        "AutoEq プロジェクトの測定データ（MIT）"
    ),
    "ko" to autoEqCatalogStrings(
        "헤드폰 카탈로그",
        "헤드폰을 찾아 AutoEQ 보정을 불러오세요",
        "브랜드 또는 모델 검색",
        "카탈로그 불러오는 중…",
        "일치하는 헤드폰이 없습니다",
        "카탈로그를 사용할 수 없습니다. 연결을 확인하세요.",
        "프로필 다운로드 실패",
        "다시 시도",
        "AutoEq 프로젝트 측정 데이터(MIT)"
    ),
    "hi" to autoEqCatalogStrings(
        "हेडफ़ोन कैटलॉग",
        "अपने हेडफ़ोन खोजें और उनका AutoEQ सुधार लोड करें",
        "ब्रांड या मॉडल खोजें",
        "कैटलॉग लोड हो रहा है…",
        "कोई मेल खाता हेडफ़ोन नहीं मिला",
        "कैटलॉग उपलब्ध नहीं है। अपना कनेक्शन जाँचें।",
        "प्रोफ़ाइल डाउनलोड विफल",
        "फिर से कोशिश करें",
        "AutoEq प्रोजेक्ट के माप (MIT)"
    ),
    "id" to autoEqCatalogStrings(
        "Katalog headphone",
        "Temukan headphone Anda dan muat koreksi AutoEQ-nya",
        "Cari merek atau model",
        "Memuat katalog…",
        "Tidak ada headphone yang cocok",
        "Katalog tidak tersedia. Periksa koneksi Anda.",
        "Gagal mengunduh profil",
        "Coba lagi",
        "Pengukuran dari proyek AutoEq (MIT)"
    ),
    "vi" to autoEqCatalogStrings(
        "Danh mục tai nghe",
        "Tìm tai nghe của bạn và tải hiệu chỉnh AutoEQ",
        "Tìm thương hiệu hoặc mẫu",
        "Đang tải danh mục…",
        "Không tìm thấy tai nghe phù hợp",
        "Danh mục không khả dụng. Hãy kiểm tra kết nối.",
        "Tải hồ sơ thất bại",
        "Thử lại",
        "Số đo từ dự án AutoEq (MIT)"
    ),
    "th" to autoEqCatalogStrings(
        "แคตตาล็อกหูฟัง",
        "ค้นหาหูฟังของคุณและโหลดการแก้ไข AutoEQ",
        "ค้นหาแบรนด์หรือรุ่น",
        "กำลังโหลดแคตตาล็อก…",
        "ไม่พบหูฟังที่ตรงกัน",
        "แคตตาล็อกไม่พร้อมใช้งาน โปรดตรวจสอบการเชื่อมต่อ",
        "ดาวน์โหลดโปรไฟล์ไม่สำเร็จ",
        "ลองอีกครั้ง",
        "ค่าการวัดจากโครงการ AutoEq (MIT)"
    ),
    "fil" to autoEqCatalogStrings(
        "Katalogo ng headphone",
        "Hanapin ang iyong headphone at i-load ang AutoEQ correction nito",
        "Maghanap ng brand o model",
        "Nilo-load ang katalogo…",
        "Walang tugmang headphone",
        "Hindi available ang katalogo. Tingnan ang iyong koneksyon.",
        "Nabigo ang pag-download ng profile",
        "Subukan muli",
        "Mga sukat mula sa proyektong AutoEq (MIT)"
    ),
    "he" to autoEqCatalogStrings(
        "קטלוג אוזניות",
        "מצאו את האוזניות שלכם וטענו את תיקון ה-AutoEQ שלהן",
        "חיפוש מותג או דגם",
        "טוען קטלוג…",
        "לא נמצאו אוזניות תואמות",
        "הקטלוג אינו זמין. בדקו את החיבור.",
        "הורדת הפרופיל נכשלה",
        "ניסיון חוזר",
        "מדידות מפרויקט AutoEq (MIT)"
    )
)

internal fun autoEqLocalizationEntries(code: String): Map<String, String> =
    autoEqBundles.getValue(code) + autoEqCatalogBundles.getValue(code)

internal fun autoEqLocalizationCodes(): Set<String> = autoEqBundles.keys
