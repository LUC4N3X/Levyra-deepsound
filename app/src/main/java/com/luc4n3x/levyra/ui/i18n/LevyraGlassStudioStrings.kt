package com.luc4n3x.levyra.ui.i18n

internal data class GlassStudioCopy(
    val title: String,
    val subtitle: String,
    val blur: String,
    val transparency: String,
    val tint: String,
    val refraction: String,
    val refractionSubtitle: String,
    val performanceNote: String,
    val disabledNote: String,
    val reset: String
)

internal fun LevyraStrings.glassStudioCopy(): GlassStudioCopy = when (code) {
    "it" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profondità e materiale delle superfici in vetro",
        blur = "Sfocatura",
        transparency = "Trasparenza",
        tint = "Tinta",
        refraction = "Rifrazione leggera",
        refractionSubtitle = "I bordi delle barre flottanti curvano lo sfondo (Android 13+)",
        performanceNote = "Segue Prestazioni visive: sui dispositivi limitati e con il risparmio energetico gli effetti avanzati si disattivano.",
        disabledNote = "Attiva Liquid Glass nelle impostazioni dell'interfaccia per usare questi controlli.",
        reset = "Ripristina"
    )
    "es" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profundidad y material de las superficies de cristal",
        blur = "Desenfoque",
        transparency = "Transparencia",
        tint = "Tinte",
        refraction = "Refracción ligera",
        refractionSubtitle = "Los bordes de las barras flotantes curvan el fondo (Android 13+)",
        performanceNote = "Sigue Rendimiento visual: en dispositivos limitados y con ahorro de batería se desactivan los efectos avanzados.",
        disabledNote = "Activa Liquid Glass en los ajustes de interfaz para usar estos controles.",
        reset = "Restablecer"
    )
    "fr" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profondeur et matière des surfaces en verre",
        blur = "Flou",
        transparency = "Transparence",
        tint = "Teinte",
        refraction = "Réfraction légère",
        refractionSubtitle = "Les bords des barres flottantes courbent l'arrière-plan (Android 13+)",
        performanceNote = "Suit Performances visuelles : sur les appareils limités et en économie d'énergie, les effets avancés sont désactivés.",
        disabledNote = "Activez Liquid Glass dans les réglages de l'interface pour utiliser ces commandes.",
        reset = "Réinitialiser"
    )
    "de" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Tiefe und Material der Glasflächen",
        blur = "Unschärfe",
        transparency = "Transparenz",
        tint = "Tönung",
        refraction = "Leichte Lichtbrechung",
        refractionSubtitle = "Die Ränder schwebender Leisten krümmen den Hintergrund (Android 13+)",
        performanceNote = "Folgt der visuellen Leistung: Auf schwächeren Geräten und im Energiesparmodus werden erweiterte Effekte deaktiviert.",
        disabledNote = "Aktiviere Liquid Glass in den Oberflächeneinstellungen, um diese Regler zu nutzen.",
        reset = "Zurücksetzen"
    )
    "pt" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profundidade e material das superfícies de vidro",
        blur = "Desfocagem",
        transparency = "Transparência",
        tint = "Tonalidade",
        refraction = "Refração ligeira",
        refractionSubtitle = "As margens das barras flutuantes curvam o fundo (Android 13+)",
        performanceNote = "Segue o Desempenho visual: em dispositivos limitados e com poupança de bateria os efeitos avançados são desativados.",
        disabledNote = "Ativa o Liquid Glass nas definições da interface para usar estes controlos.",
        reset = "Repor"
    )
    "ja" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "ガラス面の奥行きと質感",
        blur = "ぼかし",
        transparency = "透明度",
        tint = "色味",
        refraction = "軽い屈折",
        refractionSubtitle = "フローティングバーの縁で背景がわずかに曲がります（Android 13 以降）",
        performanceNote = "表示パフォーマンスの設定に従います。低スペック端末や省電力モードでは高度な効果をオフにします。",
        disabledNote = "これらの設定を使うには、インターフェース設定で Liquid Glass をオンにしてください。",
        reset = "リセット"
    )
    "nl" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Diepte en materiaal van glasvlakken",
        blur = "Vervaging",
        transparency = "Transparantie",
        tint = "Tint",
        refraction = "Lichte breking",
        refractionSubtitle = "Randen van zwevende balken buigen de achtergrond (Android 13+)",
        performanceNote = "Volgt Visuele prestaties: geavanceerde effecten gaan uit op beperkte apparaten en bij batterijbesparing.",
        disabledNote = "Zet Liquid Glass aan in de interface-instellingen om deze opties te gebruiken.",
        reset = "Herstellen"
    )
    "pl" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Głębia i materiał szklanych powierzchni",
        blur = "Rozmycie",
        transparency = "Przezroczystość",
        tint = "Odcień",
        refraction = "Lekkie załamanie światła",
        refractionSubtitle = "Krawędzie pływających pasków lekko zaginają tło (Android 13+)",
        performanceNote = "Zgodnie z Wydajnością wizualną: zaawansowane efekty wyłączają się na słabszych urządzeniach i w trybie oszczędzania baterii.",
        disabledNote = "Włącz Liquid Glass w ustawieniach interfejsu, aby używać tych opcji.",
        reset = "Przywróć"
    )
    "ro" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Adâncimea și materialul suprafețelor de sticlă",
        blur = "Estompare",
        transparency = "Transparență",
        tint = "Nuanță",
        refraction = "Refracție ușoară",
        refractionSubtitle = "Marginile barelor flotante curbează fundalul (Android 13+)",
        performanceNote = "Urmează Performanța vizuală: efectele avansate se opresc pe dispozitivele limitate și în economisirea bateriei.",
        disabledNote = "Activează Liquid Glass în setările interfeței pentru a folosi aceste opțiuni.",
        reset = "Resetează"
    )
    "el" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Βάθος και υλικό των γυάλινων επιφανειών",
        blur = "Θόλωση",
        transparency = "Διαφάνεια",
        tint = "Απόχρωση",
        refraction = "Ελαφριά διάθλαση",
        refractionSubtitle = "Οι άκρες των αιωρούμενων γραμμών λυγίζουν απαλά το φόντο (Android 13+)",
        performanceNote = "Ακολουθεί την Οπτική απόδοση: τα προηγμένα εφέ απενεργοποιούνται σε περιορισμένες συσκευές και στην εξοικονόμηση μπαταρίας.",
        disabledNote = "Ενεργοποιήστε το Liquid Glass στις ρυθμίσεις διεπαφής για να χρησιμοποιήσετε αυτές τις επιλογές.",
        reset = "Επαναφορά"
    )
    "sv" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Djup och material för glasytor",
        blur = "Oskärpa",
        transparency = "Transparens",
        tint = "Färgton",
        refraction = "Lätt ljusbrytning",
        refractionSubtitle = "Kanterna på flytande fält böjer bakgrunden (Android 13+)",
        performanceNote = "Följer Visuell prestanda: avancerade effekter stängs av på begränsade enheter och i batterisparläge.",
        disabledNote = "Slå på Liquid Glass i gränssnittsinställningarna för att använda dessa reglage.",
        reset = "Återställ"
    )
    "da" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Dybde og materiale for glasflader",
        blur = "Sløring",
        transparency = "Gennemsigtighed",
        tint = "Farvetone",
        refraction = "Let lysbrydning",
        refractionSubtitle = "Kanterne på svævende bjælker bøjer baggrunden (Android 13+)",
        performanceNote = "Følger Visuel ydeevne: avancerede effekter slås fra på begrænsede enheder og i batterisparetilstand.",
        disabledNote = "Slå Liquid Glass til i grænsefladeindstillingerne for at bruge disse kontroller.",
        reset = "Nulstil"
    )
    "cs" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Hloubka a materiál skleněných ploch",
        blur = "Rozostření",
        transparency = "Průhlednost",
        tint = "Odstín",
        refraction = "Jemný lom světla",
        refractionSubtitle = "Okraje plovoucích panelů jemně ohýbají pozadí (Android 13+)",
        performanceNote = "Řídí se Vizuálním výkonem: pokročilé efekty se vypínají na slabších zařízeních a při spořiči baterie.",
        disabledNote = "Pro použití těchto ovládacích prvků zapněte Liquid Glass v nastavení rozhraní.",
        reset = "Obnovit"
    )
    "uk" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Глибина й матеріал скляних поверхонь",
        blur = "Розмиття",
        transparency = "Прозорість",
        tint = "Відтінок",
        refraction = "Легке заломлення",
        refractionSubtitle = "Краї плаваючих панелей м’яко вигинають тло (Android 13+)",
        performanceNote = "Залежить від Візуальної продуктивності: розширені ефекти вимикаються на слабших пристроях і в режимі енергозбереження.",
        disabledNote = "Увімкніть Liquid Glass у налаштуваннях інтерфейсу, щоб користуватися цими параметрами.",
        reset = "Скинути"
    )
    "ru" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Глубина и материал стеклянных поверхностей",
        blur = "Размытие",
        transparency = "Прозрачность",
        tint = "Оттенок",
        refraction = "Лёгкое преломление",
        refractionSubtitle = "Края плавающих панелей мягко изгибают фон (Android 13+)",
        performanceNote = "Зависит от Визуальной производительности: расширенные эффекты отключаются на слабых устройствах и в режиме энергосбережения.",
        disabledNote = "Включите Liquid Glass в настройках интерфейса, чтобы пользоваться этими параметрами.",
        reset = "Сбросить"
    )
    "tr" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Cam yüzeylerin derinliği ve dokusu",
        blur = "Bulanıklık",
        transparency = "Saydamlık",
        tint = "Ton",
        refraction = "Hafif kırılma",
        refractionSubtitle = "Yüzen çubukların kenarları arka planı hafifçe büker (Android 13+)",
        performanceNote = "Görsel performansı izler: gelişmiş efektler sınırlı cihazlarda ve pil tasarrufunda kapanır.",
        disabledNote = "Bu denetimleri kullanmak için arayüz ayarlarından Liquid Glass'ı açın.",
        reset = "Sıfırla"
    )
    "ar" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "عمق الأسطح الزجاجية وخامتها",
        blur = "التمويه",
        transparency = "الشفافية",
        tint = "الصبغة",
        refraction = "انكسار خفيف",
        refractionSubtitle = "تنحني الخلفية برفق عند حواف الأشرطة العائمة (Android 13 والأحدث)",
        performanceNote = "يتبع إعداد الأداء المرئي: تتوقف التأثيرات المتقدمة على الأجهزة المحدودة وفي وضع توفير البطارية.",
        disabledNote = "فعّل Liquid Glass في إعدادات الواجهة لاستخدام هذه الخيارات.",
        reset = "إعادة تعيين"
    )
    "zh" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "玻璃表面的层次与质感",
        blur = "模糊",
        transparency = "透明度",
        tint = "色调",
        refraction = "轻度折射",
        refractionSubtitle = "悬浮栏边缘会轻微弯折背景（Android 13 及以上）",
        performanceNote = "遵循视觉性能设置：在受限设备和省电模式下会关闭高级效果。",
        disabledNote = "请在界面设置中开启 Liquid Glass 以使用这些选项。",
        reset = "重置"
    )
    "ko" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "유리 표면의 깊이와 질감",
        blur = "블러",
        transparency = "투명도",
        tint = "색조",
        refraction = "은은한 굴절",
        refractionSubtitle = "떠 있는 바의 가장자리가 배경을 살짝 굴절시킵니다 (Android 13 이상)",
        performanceNote = "시각 성능 설정을 따릅니다. 제한된 기기와 절전 모드에서는 고급 효과가 꺼집니다.",
        disabledNote = "이 설정을 사용하려면 인터페이스 설정에서 Liquid Glass를 켜세요.",
        reset = "초기화"
    )
    "hi" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "काँच वाली सतहों की गहराई और बनावट",
        blur = "धुंधलापन",
        transparency = "पारदर्शिता",
        tint = "रंगत",
        refraction = "हल्का अपवर्तन",
        refractionSubtitle = "तैरती पट्टियों के किनारे पृष्ठभूमि को हल्का मोड़ते हैं (Android 13+)",
        performanceNote = "विज़ुअल परफ़ॉर्मेंस के अनुसार: सीमित डिवाइस और बैटरी सेवर में उन्नत प्रभाव बंद हो जाते हैं।",
        disabledNote = "इन विकल्पों के लिए इंटरफ़ेस सेटिंग में Liquid Glass चालू करें।",
        reset = "रीसेट करें"
    )
    "id" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Kedalaman dan material permukaan kaca",
        blur = "Buram",
        transparency = "Transparansi",
        tint = "Rona",
        refraction = "Refraksi ringan",
        refractionSubtitle = "Tepi bilah mengambang sedikit membelokkan latar (Android 13+)",
        performanceNote = "Mengikuti Performa visual: efek lanjutan nonaktif di perangkat terbatas dan saat penghemat baterai.",
        disabledNote = "Aktifkan Liquid Glass di setelan antarmuka untuk memakai kontrol ini.",
        reset = "Atur ulang"
    )
    "vi" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Chiều sâu và chất liệu của bề mặt kính",
        blur = "Làm mờ",
        transparency = "Độ trong suốt",
        tint = "Sắc độ",
        refraction = "Khúc xạ nhẹ",
        refractionSubtitle = "Cạnh các thanh nổi uốn nhẹ hình nền (Android 13+)",
        performanceNote = "Theo Hiệu năng hình ảnh: hiệu ứng nâng cao sẽ tắt trên thiết bị hạn chế và khi tiết kiệm pin.",
        disabledNote = "Bật Liquid Glass trong cài đặt giao diện để dùng các tùy chọn này.",
        reset = "Đặt lại"
    )
    "th" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "มิติและวัสดุของพื้นผิวกระจก",
        blur = "ความเบลอ",
        transparency = "ความโปร่งใส",
        tint = "โทนสี",
        refraction = "การหักเหแสงเล็กน้อย",
        refractionSubtitle = "ขอบแถบลอยจะดัดพื้นหลังเล็กน้อย (Android 13 ขึ้นไป)",
        performanceNote = "เป็นไปตามประสิทธิภาพภาพ: เอฟเฟกต์ขั้นสูงจะปิดในอุปกรณ์สเปกจำกัดและโหมดประหยัดแบตเตอรี่",
        disabledNote = "เปิด Liquid Glass ในการตั้งค่าอินเทอร์เฟซเพื่อใช้ตัวเลือกเหล่านี้",
        reset = "รีเซ็ต"
    )
    "fil" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Lalim at materyal ng mga glass surface",
        blur = "Blur",
        transparency = "Transparency",
        tint = "Tint",
        refraction = "Bahagyang refraction",
        refractionSubtitle = "Bahagyang binabaluktot ng gilid ng mga floating bar ang background (Android 13+)",
        performanceNote = "Sumusunod sa Visual performance: namamatay ang advanced effects sa limitadong device at sa battery saver.",
        disabledNote = "I-on ang Liquid Glass sa interface settings para magamit ang mga kontrol na ito.",
        reset = "I-reset"
    )
    "he" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "עומק וחומר של משטחי הזכוכית",
        blur = "טשטוש",
        transparency = "שקיפות",
        tint = "גוון",
        refraction = "שבירת אור עדינה",
        refractionSubtitle = "קצוות הסרגלים הצפים מכופפים מעט את הרקע (Android 13 ומעלה)",
        performanceNote = "תלוי בביצועים החזותיים: אפקטים מתקדמים כבויים במכשירים מוגבלים ובחיסכון בסוללה.",
        disabledNote = "הפעל את Liquid Glass בהגדרות הממשק כדי להשתמש בפקדים האלה.",
        reset = "איפוס"
    )
    "sk" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Hĺbka a materiál sklenených plôch",
        blur = "Rozmazanie",
        transparency = "Priehľadnosť",
        tint = "Odtieň",
        refraction = "Jemný lom svetla",
        refractionSubtitle = "Okraje plávajúcich panelov jemne ohýbajú pozadie (Android 13+)",
        performanceNote = "Riadi sa Vizuálnym výkonom: pokročilé efekty sa vypínajú na slabších zariadeniach a pri šetriči batérie.",
        disabledNote = "Na použitie týchto ovládacích prvkov zapnite Liquid Glass v nastaveniach rozhrania.",
        reset = "Obnoviť"
    )
    "bn" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "কাচের পৃষ্ঠের গভীরতা ও গঠন",
        blur = "ব্লার",
        transparency = "স্বচ্ছতা",
        tint = "আভা",
        refraction = "হালকা প্রতিসরণ",
        refractionSubtitle = "ভাসমান বারের প্রান্ত পটভূমিকে হালকা বাঁকায় (Android 13+)",
        performanceNote = "ভিজ্যুয়াল পারফরম্যান্স অনুসরণ করে: সীমিত ডিভাইসে ও ব্যাটারি সেভারে উন্নত ইফেক্ট বন্ধ থাকে।",
        disabledNote = "এই নিয়ন্ত্রণগুলো ব্যবহার করতে ইন্টারফেস সেটিংসে Liquid Glass চালু করুন।",
        reset = "রিসেট"
    )
    "kk" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Шыны беттердің тереңдігі мен материалы",
        blur = "Бұлыңғырлық",
        transparency = "Мөлдірлік",
        tint = "Реңк",
        refraction = "Жеңіл сыну",
        refractionSubtitle = "Қалқымалы жолақтардың жиектері фонды жұмсақ иеді (Android 13+)",
        performanceNote = "Визуалды өнімділікке бағынады: әлсіз құрылғыларда және батареяны үнемдеу режимінде кеңейтілген әсерлер өшеді.",
        disabledNote = "Бұл параметрлерді пайдалану үшін интерфейс параметрлерінде Liquid Glass-ты қосыңыз.",
        reset = "Қалпына келтіру"
    )
    "sr" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Dubina i materijal staklenih površina",
        blur = "Zamućenje",
        transparency = "Providnost",
        tint = "Nijansa",
        refraction = "Blago prelamanje",
        refractionSubtitle = "Ivice plutajućih traka blago savijaju pozadinu (Android 13+)",
        performanceNote = "Prati Vizuelne performanse: napredni efekti se isključuju na slabijim uređajima i uz uštedu baterije.",
        disabledNote = "Uključite Liquid Glass u podešavanjima interfejsa da biste koristili ove opcije.",
        reset = "Resetuj"
    )
    "sq" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Thellësia dhe materiali i sipërfaqeve prej xhami",
        blur = "Turbullimi",
        transparency = "Tejdukshmëria",
        tint = "Ngjyrimi",
        refraction = "Përthyerje e lehtë",
        refractionSubtitle = "Skajet e shiritave lundrues e përkulin lehtë sfondin (Android 13+)",
        performanceNote = "Ndjek Performancën vizuale: efektet e avancuara fiken në pajisjet e kufizuara dhe në kursimin e baterisë.",
        disabledNote = "Aktivizo Liquid Glass te cilësimet e ndërfaqes për t'i përdorur këto kontrolle.",
        reset = "Rivendos"
    )
    "hr" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Dubina i materijal staklenih površina",
        blur = "Zamućenje",
        transparency = "Prozirnost",
        tint = "Nijansa",
        refraction = "Blago lomljenje svjetla",
        refractionSubtitle = "Rubovi plutajućih traka blago savijaju pozadinu (Android 13+)",
        performanceNote = "Prati Vizualne performanse: napredni efekti isključuju se na slabijim uređajima i uz štednju baterije.",
        disabledNote = "Uključite Liquid Glass u postavkama sučelja za korištenje ovih kontrola.",
        reset = "Vrati zadano"
    )
    "bg" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Дълбочина и материя на стъклените повърхности",
        blur = "Размиване",
        transparency = "Прозрачност",
        tint = "Оттенък",
        refraction = "Леко пречупване",
        refractionSubtitle = "Ръбовете на плаващите ленти леко огъват фона (Android 13+)",
        performanceNote = "Следва Визуалната производителност: разширените ефекти се изключват на по-слаби устройства и при пестене на батерията.",
        disabledNote = "Включете Liquid Glass в настройките на интерфейса, за да използвате тези контроли.",
        reset = "Нулиране"
    )
    "hu" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Az üvegfelületek mélysége és anyaga",
        blur = "Elmosás",
        transparency = "Átlátszóság",
        tint = "Árnyalat",
        refraction = "Enyhe fénytörés",
        refractionSubtitle = "A lebegő sávok szélei finoman meghajlítják a hátteret (Android 13+)",
        performanceNote = "A Vizuális teljesítményt követi: korlátozott eszközökön és energiatakarékos módban a speciális effektusok kikapcsolnak.",
        disabledNote = "A vezérlők használatához kapcsold be a Liquid Glass-t a felület beállításaiban.",
        reset = "Visszaállítás"
    )
    "nb" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Dybde og materiale for glassflater",
        blur = "Uskarphet",
        transparency = "Gjennomsiktighet",
        tint = "Fargetone",
        refraction = "Lett lysbrytning",
        refractionSubtitle = "Kantene på flytende felt bøyer bakgrunnen (Android 13+)",
        performanceNote = "Følger Visuell ytelse: avanserte effekter slås av på begrensede enheter og i batterisparing.",
        disabledNote = "Slå på Liquid Glass i grensesnittinnstillingene for å bruke disse kontrollene.",
        reset = "Tilbakestill"
    )
    "ca" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Profunditat i material de les superfícies de vidre",
        blur = "Desenfocament",
        transparency = "Transparència",
        tint = "Tonalitat",
        refraction = "Refracció lleugera",
        refractionSubtitle = "Les vores de les barres flotants corben el fons (Android 13+)",
        performanceNote = "Segueix el Rendiment visual: els efectes avançats es desactiven en dispositius limitats i amb l'estalvi de bateria.",
        disabledNote = "Activa Liquid Glass a la configuració de la interfície per fer servir aquests controls.",
        reset = "Restableix"
    )
    "ms" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Kedalaman dan bahan permukaan kaca",
        blur = "Kabur",
        transparency = "Ketelusan",
        tint = "Rona",
        refraction = "Pembiasan ringan",
        refractionSubtitle = "Tepi bar terapung membengkokkan latar sedikit (Android 13+)",
        performanceNote = "Mengikut Prestasi visual: kesan lanjutan dimatikan pada peranti terhad dan semasa penjimat bateri.",
        disabledNote = "Hidupkan Liquid Glass dalam tetapan antara muka untuk menggunakan kawalan ini.",
        reset = "Tetapkan semula"
    )
    "fa" -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "عمق و جنس سطح‌های شیشه‌ای",
        blur = "تاری",
        transparency = "شفافیت",
        tint = "ته‌رنگ",
        refraction = "شکست نور ملایم",
        refractionSubtitle = "لبه‌ی نوارهای شناور پس‌زمینه را کمی خم می‌کند (Android 13 و بالاتر)",
        performanceNote = "از تنظیم عملکرد بصری پیروی می‌کند: جلوه‌های پیشرفته در دستگاه‌های محدود و حالت صرفه‌جویی باتری خاموش می‌شوند.",
        disabledNote = "برای استفاده از این گزینه‌ها، Liquid Glass را در تنظیمات رابط روشن کنید.",
        reset = "بازنشانی"
    )
    else -> GlassStudioCopy(
        title = "Expressive Glass",
        subtitle = "Depth and material of glass surfaces",
        blur = "Blur",
        transparency = "Transparency",
        tint = "Tint",
        refraction = "Light refraction",
        refractionSubtitle = "Floating bar edges gently bend the backdrop (Android 13+)",
        performanceNote = "Follows Visual performance: advanced effects turn off on constrained devices and in battery saver.",
        disabledNote = "Turn on Liquid Glass in interface settings to use these controls.",
        reset = "Reset"
    )
}
