package com.luc4n3x.levyra.ui.i18n

data class ParametricEqCopy(
    val graphicEq: String,
    val parametricEq: String,
    val parametricSubtitle: String,
    val activeProfile: String,
    val bands: String,
    val addBand: String,
    val removeBand: String,
    val frequency: String,
    val gain: String,
    val qFactor: String,
    val saveProfile: String,
    val reset: String,
    val peak: String,
    val lowShelf: String,
    val highShelf: String,
    val importHint: String,
    val invalidDetail: String
)

private fun eq(vararg value: String): ParametricEqCopy {
    require(value.size == 17)
    return ParametricEqCopy(
        value[0], value[1], value[2], value[3], value[4], value[5], value[6], value[7], value[8],
        value[9], value[10], value[11], value[12], value[13], value[14], value[15], value[16]
    )
}

private val parametricEqCopies = mapOf(
    "en" to eq(
        "Graphic EQ", "Parametric EQ", "Precise filters for AutoEQ and custom tuning", "Active profile", "Filters",
        "Add filter", "Remove filter", "Frequency", "Gain", "Q factor", "Save profile", "Reset Parametric EQ",
        "Peak", "Low shelf", "High shelf", "Paste a GraphicEQ or ParametricEQ profile, or open a text file",
        "Check filter type, frequency, gain, and Q values"
    ),
    "it" to eq(
        "EQ grafico", "EQ parametrico", "Filtri precisi per AutoEQ e regolazioni personalizzate", "Profilo attivo", "Filtri",
        "Aggiungi filtro", "Rimuovi filtro", "Frequenza", "Guadagno", "Fattore Q", "Salva profilo", "Reimposta EQ parametrico",
        "Picco", "Shelf basso", "Shelf alto", "Incolla un profilo GraphicEQ o ParametricEQ, oppure apri un file di testo",
        "Controlla tipo, frequenza, guadagno e fattore Q dei filtri"
    ),
    "es" to eq(
        "EQ gráfico", "EQ paramétrico", "Filtros precisos para AutoEQ y ajustes personalizados", "Perfil activo", "Filtros",
        "Añadir filtro", "Eliminar filtro", "Frecuencia", "Ganancia", "Factor Q", "Guardar perfil", "Restablecer EQ paramétrico",
        "Pico", "Estante de graves", "Estante de agudos", "Pega un perfil GraphicEQ o ParametricEQ, o abre un archivo de texto",
        "Comprueba el tipo de filtro, la frecuencia, la ganancia y los valores Q"
    ),
    "fr" to eq(
        "Égaliseur graphique", "Égaliseur paramétrique", "Filtres précis pour AutoEQ et les réglages personnalisés", "Profil actif", "Filtres",
        "Ajouter un filtre", "Supprimer le filtre", "Fréquence", "Gain", "Facteur Q", "Enregistrer le profil", "Réinitialiser l’égaliseur paramétrique",
        "Crête", "Plateau grave", "Plateau aigu", "Collez un profil GraphicEQ ou ParametricEQ, ou ouvrez un fichier texte",
        "Vérifiez le type de filtre, la fréquence, le gain et les valeurs Q"
    ),
    "de" to eq(
        "Grafischer EQ", "Parametrischer EQ", "Präzise Filter für AutoEQ und eigene Abstimmungen", "Aktives Profil", "Filter",
        "Filter hinzufügen", "Filter entfernen", "Frequenz", "Verstärkung", "Q-Faktor", "Profil speichern", "Parametrischen EQ zurücksetzen",
        "Peak", "Low-Shelf", "High-Shelf", "GraphicEQ- oder ParametricEQ-Profil einfügen oder eine Textdatei öffnen",
        "Filtertyp, Frequenz, Verstärkung und Q-Werte prüfen"
    ),
    "pt" to eq(
        "EQ gráfico", "EQ paramétrico", "Filtros precisos para AutoEQ e ajustes personalizados", "Perfil ativo", "Filtros",
        "Adicionar filtro", "Remover filtro", "Frequência", "Ganho", "Fator Q", "Guardar perfil", "Repor EQ paramétrico",
        "Pico", "Prateleira de graves", "Prateleira de agudos", "Cole um perfil GraphicEQ ou ParametricEQ, ou abra um ficheiro de texto",
        "Verifique o tipo de filtro, a frequência, o ganho e os valores Q"
    ),
    "nl" to eq(
        "Grafische EQ", "Parametrische EQ", "Nauwkeurige filters voor AutoEQ en eigen afstemming", "Actief profiel", "Filters",
        "Filter toevoegen", "Filter verwijderen", "Frequentie", "Versterking", "Q-factor", "Profiel opslaan", "Parametrische EQ herstellen",
        "Piek", "Laag-shelf", "Hoog-shelf", "Plak een GraphicEQ- of ParametricEQ-profiel of open een tekstbestand",
        "Controleer filtertype, frequentie, versterking en Q-waarden"
    ),
    "pl" to eq(
        "Korektor graficzny", "Korektor parametryczny", "Precyzyjne filtry AutoEQ i własne strojenie", "Aktywny profil", "Filtry",
        "Dodaj filtr", "Usuń filtr", "Częstotliwość", "Wzmocnienie", "Współczynnik Q", "Zapisz profil", "Resetuj korektor parametryczny",
        "Szczytowy", "Półka dolna", "Półka górna", "Wklej profil GraphicEQ lub ParametricEQ albo otwórz plik tekstowy",
        "Sprawdź typ filtra, częstotliwość, wzmocnienie i wartości Q"
    ),
    "ro" to eq(
        "Egalizator grafic", "Egalizator parametric", "Filtre precise pentru AutoEQ și reglaje personalizate", "Profil activ", "Filtre",
        "Adaugă filtru", "Elimină filtrul", "Frecvență", "Câștig", "Factor Q", "Salvează profilul", "Resetează egalizatorul parametric",
        "Vârf", "Raft inferior", "Raft superior", "Lipește un profil GraphicEQ sau ParametricEQ ori deschide un fișier text",
        "Verifică tipul filtrului, frecvența, câștigul și valorile Q"
    ),
    "el" to eq(
        "Γραφικός ισοσταθμιστής", "Παραμετρικός ισοσταθμιστής", "Ακριβή φίλτρα για AutoEQ και προσαρμοσμένη ρύθμιση", "Ενεργό προφίλ", "Φίλτρα",
        "Προσθήκη φίλτρου", "Αφαίρεση φίλτρου", "Συχνότητα", "Ενίσχυση", "Συντελεστής Q", "Αποθήκευση προφίλ", "Επαναφορά παραμετρικού ισοσταθμιστή",
        "Κορυφής", "Χαμηλό ράφι", "Υψηλό ράφι", "Επικολλήστε προφίλ GraphicEQ ή ParametricEQ ή ανοίξτε αρχείο κειμένου",
        "Ελέγξτε τον τύπο φίλτρου, τη συχνότητα, την ενίσχυση και τις τιμές Q"
    ),
    "sv" to eq(
        "Grafisk EQ", "Parametrisk EQ", "Exakta filter för AutoEQ och egen ljudjustering", "Aktiv profil", "Filter",
        "Lägg till filter", "Ta bort filter", "Frekvens", "Förstärkning", "Q-faktor", "Spara profil", "Återställ parametrisk EQ",
        "Topp", "Låg hylla", "Hög hylla", "Klistra in en GraphicEQ- eller ParametricEQ-profil eller öppna en textfil",
        "Kontrollera filtertyp, frekvens, förstärkning och Q-värden"
    ),
    "da" to eq(
        "Grafisk EQ", "Parametrisk EQ", "Præcise filtre til AutoEQ og egen lydtilpasning", "Aktiv profil", "Filtre",
        "Tilføj filter", "Fjern filter", "Frekvens", "Forstærkning", "Q-faktor", "Gem profil", "Nulstil parametrisk EQ",
        "Spids", "Lav hylde", "Høj hylde", "Indsæt en GraphicEQ- eller ParametricEQ-profil, eller åbn en tekstfil",
        "Kontrollér filtertype, frekvens, forstærkning og Q-værdier"
    ),
    "cs" to eq(
        "Grafický ekvalizér", "Parametrický ekvalizér", "Přesné filtry pro AutoEQ a vlastní ladění", "Aktivní profil", "Filtry",
        "Přidat filtr", "Odebrat filtr", "Frekvence", "Zisk", "Činitel Q", "Uložit profil", "Obnovit parametrický ekvalizér",
        "Špičkový", "Dolní police", "Horní police", "Vložte profil GraphicEQ nebo ParametricEQ, případně otevřete textový soubor",
        "Zkontrolujte typ filtru, frekvenci, zisk a hodnoty Q"
    ),
    "sk" to eq(
        "Grafický ekvalizér", "Parametrický ekvalizér", "Presné filtre pre AutoEQ a vlastné ladenie", "Aktívny profil", "Filtre",
        "Pridať filter", "Odstrániť filter", "Frekvencia", "Zisk", "Činiteľ Q", "Uložiť profil", "Obnoviť parametrický ekvalizér",
        "Špičkový", "Dolná polica", "Horná polica", "Vložte profil GraphicEQ alebo ParametricEQ, prípadne otvorte textový súbor",
        "Skontrolujte typ filtra, frekvenciu, zisk a hodnoty Q"
    ),
    "hr" to eq(
        "Grafički ekvilizator", "Parametarski ekvilizator", "Precizni filtri za AutoEQ i prilagođeno ugađanje", "Aktivni profil", "Filtri",
        "Dodaj filtar", "Ukloni filtar", "Frekvencija", "Pojačanje", "Q faktor", "Spremi profil", "Poništi parametarski ekvilizator",
        "Vršni", "Niska polica", "Visoka polica", "Zalijepite profil GraphicEQ ili ParametricEQ ili otvorite tekstnu datoteku",
        "Provjerite vrstu filtra, frekvenciju, pojačanje i Q vrijednosti"
    ),
    "bg" to eq(
        "Графичен еквалайзер", "Параметричен еквалайзер", "Прецизни филтри за AutoEQ и персонална настройка", "Активен профил", "Филтри",
        "Добавяне на филтър", "Премахване на филтър", "Честота", "Усилване", "Q фактор", "Запазване на профила", "Нулиране на параметричния еквалайзер",
        "Пиков", "Нисък рафт", "Висок рафт", "Поставете профил GraphicEQ или ParametricEQ или отворете текстов файл",
        "Проверете типа на филтъра, честотата, усилването и стойностите Q"
    ),
    "hu" to eq(
        "Grafikus EQ", "Parametrikus EQ", "Pontos szűrők AutoEQ-hoz és egyéni hangoláshoz", "Aktív profil", "Szűrők",
        "Szűrő hozzáadása", "Szűrő eltávolítása", "Frekvencia", "Erősítés", "Q-tényező", "Profil mentése", "Parametrikus EQ visszaállítása",
        "Csúcs", "Mélypolc", "Magaspolc", "Illesszen be GraphicEQ- vagy ParametricEQ-profilt, vagy nyisson meg egy szövegfájlt",
        "Ellenőrizze a szűrőtípust, a frekvenciát, az erősítést és a Q-értékeket"
    ),
    "fi" to eq(
        "Graafinen EQ", "Parametrinen EQ", "Tarkat suodattimet AutoEQ:lle ja omille säädöille", "Aktiivinen profiili", "Suodattimet",
        "Lisää suodatin", "Poista suodatin", "Taajuus", "Vahvistus", "Q-kerroin", "Tallenna profiili", "Palauta parametrinen EQ",
        "Huippu", "Alahylly", "Ylähylly", "Liitä GraphicEQ- tai ParametricEQ-profiili tai avaa tekstitiedosto",
        "Tarkista suodatintyyppi, taajuus, vahvistus ja Q-arvot"
    ),
    "et" to eq(
        "Graafiline EQ", "Parameetriline EQ", "Täpsed filtrid AutoEQ ja kohandatud häälestuse jaoks", "Aktiivne profiil", "Filtrid",
        "Lisa filter", "Eemalda filter", "Sagedus", "Võimendus", "Q-tegur", "Salvesta profiil", "Lähtesta parameetriline EQ",
        "Tipp", "Madal riiul", "Kõrge riiul", "Kleebi GraphicEQ või ParametricEQ profiil või ava tekstifail",
        "Kontrolli filtri tüüpi, sagedust, võimendust ja Q-väärtusi"
    ),
    "nb" to eq(
        "Grafisk EQ", "Parametrisk EQ", "Presise filtre for AutoEQ og egen lydjustering", "Aktiv profil", "Filtre",
        "Legg til filter", "Fjern filter", "Frekvens", "Forsterkning", "Q-faktor", "Lagre profil", "Tilbakestill parametrisk EQ",
        "Topp", "Lav hylle", "Høy hylle", "Lim inn en GraphicEQ- eller ParametricEQ-profil, eller åpne en tekstfil",
        "Kontroller filtertype, frekvens, forsterkning og Q-verdier"
    ),
    "ca" to eq(
        "EQ gràfic", "EQ paramètric", "Filtres precisos per a AutoEQ i ajustos personalitzats", "Perfil actiu", "Filtres",
        "Afegeix un filtre", "Elimina el filtre", "Freqüència", "Guany", "Factor Q", "Desa el perfil", "Restableix l’EQ paramètric",
        "Pic", "Prestatge de greus", "Prestatge d’aguts", "Enganxa un perfil GraphicEQ o ParametricEQ, o obre un fitxer de text",
        "Comprova el tipus de filtre, la freqüència, el guany i els valors Q"
    ),
    "uk" to eq(
        "Графічний еквалайзер", "Параметричний еквалайзер", "Точні фільтри для AutoEQ і власного налаштування", "Активний профіль", "Фільтри",
        "Додати фільтр", "Видалити фільтр", "Частота", "Підсилення", "Добротність Q", "Зберегти профіль", "Скинути параметричний еквалайзер",
        "Піковий", "Низька полиця", "Висока полиця", "Вставте профіль GraphicEQ або ParametricEQ чи відкрийте текстовий файл",
        "Перевірте тип фільтра, частоту, підсилення та значення Q"
    ),
    "ru" to eq(
        "Графический эквалайзер", "Параметрический эквалайзер", "Точные фильтры для AutoEQ и ручной настройки", "Активный профиль", "Фильтры",
        "Добавить фильтр", "Удалить фильтр", "Частота", "Усиление", "Добротность Q", "Сохранить профиль", "Сбросить параметрический эквалайзер",
        "Пиковый", "Низкая полка", "Высокая полка", "Вставьте профиль GraphicEQ или ParametricEQ либо откройте текстовый файл",
        "Проверьте тип фильтра, частоту, усиление и значения Q"
    ),
    "tr" to eq(
        "Grafik EQ", "Parametrik EQ", "AutoEQ ve özel ayarlar için hassas filtreler", "Etkin profil", "Filtreler",
        "Filtre ekle", "Filtreyi kaldır", "Frekans", "Kazanç", "Q faktörü", "Profili kaydet", "Parametrik EQ’yu sıfırla",
        "Tepe", "Alt raf", "Üst raf", "Bir GraphicEQ veya ParametricEQ profili yapıştırın ya da metin dosyası açın",
        "Filtre türünü, frekansı, kazancı ve Q değerlerini kontrol edin"
    ),
    "ar" to eq(
        "معادل رسومي", "معادل بارامتري", "مرشحات دقيقة لـ AutoEQ والضبط المخصص", "الملف النشط", "المرشحات",
        "إضافة مرشح", "إزالة المرشح", "التردد", "الكسب", "معامل Q", "حفظ الملف", "إعادة ضبط المعادل البارامتري",
        "ذروة", "رف منخفض", "رف مرتفع", "الصق ملف GraphicEQ أو ParametricEQ، أو افتح ملفًا نصيًا",
        "تحقق من نوع المرشح والتردد والكسب وقيم Q"
    ),
    "fa" to eq(
        "اکولایزر گرافیکی", "اکولایزر پارامتریک", "فیلترهای دقیق برای AutoEQ و تنظیم سفارشی", "نمایهٔ فعال", "فیلترها",
        "افزودن فیلتر", "حذف فیلتر", "فرکانس", "بهره", "ضریب Q", "ذخیرهٔ نمایه", "بازنشانی اکولایزر پارامتریک",
        "قله‌ای", "قفسهٔ پایین", "قفسهٔ بالا", "نمایهٔ GraphicEQ یا ParametricEQ را جای‌گذاری کنید یا یک فایل متنی باز کنید",
        "نوع فیلتر، فرکانس، بهره و مقادیر Q را بررسی کنید"
    ),
    "zh" to eq(
        "图示均衡器", "参数均衡器", "用于 AutoEQ 和自定义调音的精确滤波器", "当前配置", "滤波器",
        "添加滤波器", "移除滤波器", "频率", "增益", "Q 值", "保存配置", "重置参数均衡器",
        "峰值", "低架", "高架", "粘贴 GraphicEQ 或 ParametricEQ 配置，或打开文本文件",
        "请检查滤波器类型、频率、增益和 Q 值"
    ),
    "zh-Hant" to eq(
        "圖示等化器", "參數等化器", "用於 AutoEQ 與自訂調音的精確濾波器", "目前設定檔", "濾波器",
        "新增濾波器", "移除濾波器", "頻率", "增益", "Q 值", "儲存設定檔", "重設參數等化器",
        "峰值", "低架", "高架", "貼上 GraphicEQ 或 ParametricEQ 設定檔，或開啟文字檔",
        "請檢查濾波器類型、頻率、增益與 Q 值"
    ),
    "ja" to eq(
        "グラフィックEQ", "パラメトリックEQ", "AutoEQとカスタム調整用の高精度フィルター", "有効なプロファイル", "フィルター",
        "フィルターを追加", "フィルターを削除", "周波数", "ゲイン", "Q値", "プロファイルを保存", "パラメトリックEQをリセット",
        "ピーク", "ローシェルフ", "ハイシェルフ", "GraphicEQまたはParametricEQプロファイルを貼り付けるか、テキストファイルを開きます",
        "フィルターの種類、周波数、ゲイン、Q値を確認してください"
    ),
    "ko" to eq(
        "그래픽 EQ", "파라메트릭 EQ", "AutoEQ 및 사용자 조정을 위한 정밀 필터", "활성 프로필", "필터",
        "필터 추가", "필터 제거", "주파수", "게인", "Q 값", "프로필 저장", "파라메트릭 EQ 초기화",
        "피크", "로우 셸프", "하이 셸프", "GraphicEQ 또는 ParametricEQ 프로필을 붙여넣거나 텍스트 파일을 여세요",
        "필터 유형, 주파수, 게인 및 Q 값을 확인하세요"
    ),
    "hi" to eq(
        "ग्राफ़िक EQ", "पैरामीट्रिक EQ", "AutoEQ और कस्टम ट्यूनिंग के लिए सटीक फ़िल्टर", "सक्रिय प्रोफ़ाइल", "फ़िल्टर",
        "फ़िल्टर जोड़ें", "फ़िल्टर हटाएँ", "आवृत्ति", "गेन", "Q गुणांक", "प्रोफ़ाइल सहेजें", "पैरामीट्रिक EQ रीसेट करें",
        "पीक", "लो शेल्फ", "हाई शेल्फ", "GraphicEQ या ParametricEQ प्रोफ़ाइल चिपकाएँ, या टेक्स्ट फ़ाइल खोलें",
        "फ़िल्टर प्रकार, आवृत्ति, गेन और Q मान जाँचें"
    ),
    "id" to eq(
        "EQ grafis", "EQ parametrik", "Filter presisi untuk AutoEQ dan penyetelan khusus", "Profil aktif", "Filter",
        "Tambah filter", "Hapus filter", "Frekuensi", "Gain", "Faktor Q", "Simpan profil", "Atur ulang EQ parametrik",
        "Puncak", "Rak rendah", "Rak tinggi", "Tempel profil GraphicEQ atau ParametricEQ, atau buka berkas teks",
        "Periksa jenis filter, frekuensi, gain, dan nilai Q"
    ),
    "ms" to eq(
        "EQ grafik", "EQ parametrik", "Penapis tepat untuk AutoEQ dan penalaan tersuai", "Profil aktif", "Penapis",
        "Tambah penapis", "Alih keluar penapis", "Frekuensi", "Gandaan", "Faktor Q", "Simpan profil", "Tetapkan semula EQ parametrik",
        "Puncak", "Rak rendah", "Rak tinggi", "Tampal profil GraphicEQ atau ParametricEQ, atau buka fail teks",
        "Semak jenis penapis, frekuensi, gandaan dan nilai Q"
    ),
    "vi" to eq(
        "EQ đồ họa", "EQ tham số", "Bộ lọc chính xác cho AutoEQ và tinh chỉnh tùy chỉnh", "Cấu hình đang dùng", "Bộ lọc",
        "Thêm bộ lọc", "Xóa bộ lọc", "Tần số", "Độ lợi", "Hệ số Q", "Lưu cấu hình", "Đặt lại EQ tham số",
        "Đỉnh", "Kệ thấp", "Kệ cao", "Dán cấu hình GraphicEQ hoặc ParametricEQ, hoặc mở tệp văn bản",
        "Kiểm tra loại bộ lọc, tần số, độ lợi và giá trị Q"
    ),
    "th" to eq(
        "EQ แบบกราฟิก", "EQ แบบพาราเมตริก", "ฟิลเตอร์แม่นยำสำหรับ AutoEQ และการปรับแต่งเอง", "โปรไฟล์ที่ใช้งาน", "ฟิลเตอร์",
        "เพิ่มฟิลเตอร์", "นำฟิลเตอร์ออก", "ความถี่", "อัตราขยาย", "ค่า Q", "บันทึกโปรไฟล์", "รีเซ็ต EQ แบบพาราเมตริก",
        "พีก", "โลว์เชลฟ์", "ไฮเชลฟ์", "วางโปรไฟล์ GraphicEQ หรือ ParametricEQ หรือเปิดไฟล์ข้อความ",
        "ตรวจสอบชนิดฟิลเตอร์ ความถี่ อัตราขยาย และค่า Q"
    ),
    "fil" to eq(
        "Graphic EQ", "Parametric EQ", "Tumpak na mga filter para sa AutoEQ at sariling pagtimpla", "Aktibong profile", "Mga filter",
        "Magdagdag ng filter", "Alisin ang filter", "Frequency", "Gain", "Q factor", "I-save ang profile", "I-reset ang Parametric EQ",
        "Peak", "Low shelf", "High shelf", "I-paste ang GraphicEQ o ParametricEQ profile, o magbukas ng text file",
        "Suriin ang uri ng filter, frequency, gain, at mga Q value"
    ),
    "he" to eq(
        "אקולייזר גרפי", "אקולייזר פרמטרי", "מסננים מדויקים ל־AutoEQ ולכוונון מותאם אישית", "פרופיל פעיל", "מסננים",
        "הוספת מסנן", "הסרת מסנן", "תדר", "הגבר", "מקדם Q", "שמירת פרופיל", "איפוס האקולייזר הפרמטרי",
        "שיא", "מדף נמוך", "מדף גבוה", "הדביקו פרופיל GraphicEQ או ParametricEQ, או פתחו קובץ טקסט",
        "בדקו את סוג המסנן, התדר, ההגבר וערכי Q"
    )
)

internal fun parametricEqLocalizationCodes(): Set<String> = parametricEqCopies.keys

fun LevyraStrings.parametricEqCopy(): ParametricEqCopy = parametricEqCopies.getValue(code)
