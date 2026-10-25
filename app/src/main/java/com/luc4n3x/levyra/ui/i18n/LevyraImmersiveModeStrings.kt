package com.luc4n3x.levyra.ui.i18n

internal val immersiveModeKeys = setOf(
    "immersiveSection",
    "immersiveHideStatusBar",
    "immersiveHideStatusBarSubtitle",
    "immersiveHideNavigationBar",
    "immersiveHideNavigationBarSubtitle",
    "immersiveFull",
    "immersiveFullSubtitle",
    "immersiveEverywhere",
    "immersiveEverywhereSubtitle"
)

private fun immersive(vararg value: String): Map<String, String> {
    require(value.size == 9)
    return immersiveModeKeys.toList().zip(value.toList()).toMap()
}

private val immersiveModeBundles = mapOf(
    "en" to immersive(
        "Immersive mode",
        "Hide status bar", "Swipe down from the top edge to show it for a moment",
        "Hide navigation bar", "Swipe up from the bottom edge to show it for a moment",
        "Full immersive mode", "Hide both system bars; edge swipes reveal them temporarily",
        "Apply to the whole app", "When off, bars are hidden only on the full-screen player"
    ),
    "it" to immersive(
        "Modalità immersiva",
        "Nascondi barra di stato", "Scorri dal bordo superiore per mostrarla per un momento",
        "Nascondi barra di navigazione", "Scorri dal bordo inferiore per mostrarla per un momento",
        "Modalità immersiva completa", "Nasconde entrambe le barre; uno swipe dal bordo le richiama temporaneamente",
        "Applica a tutta l'app", "Se disattivato, le barre si nascondono solo nel player a schermo intero"
    ),
    "es" to immersive(
        "Modo inmersivo",
        "Ocultar barra de estado", "Desliza desde el borde superior para mostrarla un momento",
        "Ocultar barra de navegación", "Desliza desde el borde inferior para mostrarla un momento",
        "Modo inmersivo completo", "Oculta ambas barras; deslizar desde el borde las muestra temporalmente",
        "Aplicar a toda la app", "Si está desactivado, las barras solo se ocultan en el reproductor a pantalla completa"
    ),
    "fr" to immersive(
        "Mode immersif",
        "Masquer la barre d’état", "Balayez depuis le bord supérieur pour l’afficher un instant",
        "Masquer la barre de navigation", "Balayez depuis le bord inférieur pour l’afficher un instant",
        "Mode immersif complet", "Masque les deux barres ; un balayage depuis le bord les affiche temporairement",
        "Appliquer à toute l’app", "Désactivé : les barres sont masquées uniquement dans le lecteur plein écran"
    ),
    "de" to immersive(
        "Immersiver Modus",
        "Statusleiste ausblenden", "Vom oberen Rand wischen, um sie kurz anzuzeigen",
        "Navigationsleiste ausblenden", "Vom unteren Rand wischen, um sie kurz anzuzeigen",
        "Vollständig immersiv", "Blendet beide Leisten aus; ein Wischen vom Rand zeigt sie kurz an",
        "Auf die ganze App anwenden", "Aus: Leisten werden nur im Vollbild-Player ausgeblendet"
    ),
    "pt" to immersive(
        "Modo imersivo",
        "Ocultar barra de estado", "Deslize a partir do topo para a mostrar por um momento",
        "Ocultar barra de navegação", "Deslize a partir da base para a mostrar por um momento",
        "Modo imersivo completo", "Oculta ambas as barras; deslizar a partir da margem mostra-as temporariamente",
        "Aplicar a toda a app", "Desativado: as barras só ficam ocultas no leitor em ecrã inteiro"
    ),
    "nl" to immersive(
        "Immersieve modus",
        "Statusbalk verbergen", "Veeg vanaf de bovenrand om hem even te tonen",
        "Navigatiebalk verbergen", "Veeg vanaf de onderrand om hem even te tonen",
        "Volledig immersief", "Verbergt beide balken; vegen vanaf de rand toont ze tijdelijk",
        "Op de hele app toepassen", "Uit: balken worden alleen verborgen in de schermvullende speler"
    ),
    "pl" to immersive(
        "Tryb immersyjny",
        "Ukryj pasek stanu", "Przesuń od górnej krawędzi, aby na chwilę go pokazać",
        "Ukryj pasek nawigacji", "Przesuń od dolnej krawędzi, aby na chwilę go pokazać",
        "Pełny tryb immersyjny", "Ukrywa oba paski; przesunięcie od krawędzi pokazuje je tymczasowo",
        "Zastosuj w całej aplikacji", "Wyłączone: paski są ukryte tylko w odtwarzaczu pełnoekranowym"
    ),
    "ro" to immersive(
        "Mod imersiv",
        "Ascunde bara de stare", "Glisează de la marginea de sus pentru a o afișa pentru o clipă",
        "Ascunde bara de navigare", "Glisează de la marginea de jos pentru a o afișa pentru o clipă",
        "Mod imersiv complet", "Ascunde ambele bare; glisarea de la margine le afișează temporar",
        "Aplică în toată aplicația", "Dezactivat: barele se ascund doar în playerul pe ecran complet"
    ),
    "el" to immersive(
        "Καθηλωτική λειτουργία",
        "Απόκρυψη γραμμής κατάστασης", "Σύρετε από το πάνω άκρο για να εμφανιστεί για λίγο",
        "Απόκρυψη γραμμής πλοήγησης", "Σύρετε από το κάτω άκρο για να εμφανιστεί για λίγο",
        "Πλήρης καθηλωτική λειτουργία", "Κρύβει και τις δύο γραμμές· ένα σύρσιμο από το άκρο τις εμφανίζει προσωρινά",
        "Εφαρμογή σε όλη την εφαρμογή", "Όταν είναι ανενεργό, οι γραμμές κρύβονται μόνο στο πρόγραμμα αναπαραγωγής πλήρους οθόνης"
    ),
    "sv" to immersive(
        "Uppslukande läge",
        "Dölj statusfältet", "Svep från överkanten för att visa det en stund",
        "Dölj navigeringsfältet", "Svep från nederkanten för att visa det en stund",
        "Helt uppslukande läge", "Döljer båda fälten; ett svep från kanten visar dem tillfälligt",
        "Använd i hela appen", "Av: fälten döljs bara i helskärmsspelaren"
    ),
    "da" to immersive(
        "Fordybende tilstand",
        "Skjul statuslinjen", "Stryg fra den øverste kant for at vise den et øjeblik",
        "Skjul navigationslinjen", "Stryg fra den nederste kant for at vise den et øjeblik",
        "Fuldt fordybende tilstand", "Skjuler begge linjer; et stryg fra kanten viser dem midlertidigt",
        "Anvend i hele appen", "Fra: linjerne skjules kun i fuldskærmsafspilleren"
    ),
    "cs" to immersive(
        "Imerzivní režim",
        "Skrýt stavový řádek", "Přejetím od horního okraje jej na chvíli zobrazíte",
        "Skrýt navigační lištu", "Přejetím od dolního okraje ji na chvíli zobrazíte",
        "Plně imerzivní režim", "Skryje obě lišty; přejetí od okraje je dočasně zobrazí",
        "Použít v celé aplikaci", "Vypnuto: lišty se skryjí jen v přehrávači na celou obrazovku"
    ),
    "uk" to immersive(
        "Режим занурення",
        "Приховати рядок стану", "Проведіть від верхнього краю, щоб ненадовго показати його",
        "Приховати панель навігації", "Проведіть від нижнього краю, щоб ненадовго показати її",
        "Повний режим занурення", "Приховує обидві панелі; свайп від краю тимчасово показує їх",
        "Застосувати до всього застосунку", "Якщо вимкнено, панелі приховуються лише в повноекранному плеєрі"
    ),
    "ru" to immersive(
        "Режим погружения",
        "Скрыть строку состояния", "Проведите от верхнего края, чтобы ненадолго показать её",
        "Скрыть панель навигации", "Проведите от нижнего края, чтобы ненадолго показать её",
        "Полный режим погружения", "Скрывает обе панели; свайп от края временно показывает их",
        "Применять ко всему приложению", "Если выключено, панели скрываются только в полноэкранном плеере"
    ),
    "tr" to immersive(
        "Sürükleyici mod",
        "Durum çubuğunu gizle", "Kısa süre göstermek için üst kenardan kaydırın",
        "Gezinme çubuğunu gizle", "Kısa süre göstermek için alt kenardan kaydırın",
        "Tam sürükleyici mod", "İki çubuğu da gizler; kenardan kaydırma onları geçici olarak gösterir",
        "Tüm uygulamaya uygula", "Kapalıyken çubuklar yalnızca tam ekran oynatıcıda gizlenir"
    ),
    "ar" to immersive(
        "الوضع الغامر",
        "إخفاء شريط الحالة", "اسحب من الحافة العلوية لإظهاره لحظة",
        "إخفاء شريط التنقل", "اسحب من الحافة السفلية لإظهاره لحظة",
        "وضع غامر كامل", "يخفي الشريطين؛ السحب من الحافة يظهرهما مؤقتًا",
        "تطبيق على التطبيق بالكامل", "عند الإيقاف، تُخفى الأشرطة في مشغل ملء الشاشة فقط"
    ),
    "zh" to immersive(
        "沉浸模式",
        "隐藏状态栏", "从顶部边缘下滑可暂时显示",
        "隐藏导航栏", "从底部边缘上滑可暂时显示",
        "完全沉浸模式", "同时隐藏两个系统栏；从边缘滑动可临时显示",
        "应用于整个应用", "关闭时仅在全屏播放器中隐藏系统栏"
    ),
    "ja" to immersive(
        "没入モード",
        "ステータスバーを隠す", "上端からスワイプすると一時的に表示されます",
        "ナビゲーションバーを隠す", "下端からスワイプすると一時的に表示されます",
        "完全没入モード", "両方のシステムバーを隠します。端からのスワイプで一時的に表示されます",
        "アプリ全体に適用", "オフの場合、バーはフルスクリーンプレーヤーでのみ隠れます"
    ),
    "ko" to immersive(
        "몰입 모드",
        "상태 표시줄 숨기기", "위쪽 가장자리에서 스와이프하면 잠시 표시됩니다",
        "탐색 메뉴 숨기기", "아래쪽 가장자리에서 스와이프하면 잠시 표시됩니다",
        "완전 몰입 모드", "두 시스템 바를 모두 숨기며 가장자리 스와이프로 잠시 표시됩니다",
        "앱 전체에 적용", "끄면 전체 화면 플레이어에서만 바가 숨겨집니다"
    ),
    "hi" to immersive(
        "इमर्सिव मोड",
        "स्टेटस बार छिपाएँ", "थोड़ी देर दिखाने के लिए ऊपरी किनारे से स्वाइप करें",
        "नेविगेशन बार छिपाएँ", "थोड़ी देर दिखाने के लिए निचले किनारे से स्वाइप करें",
        "पूर्ण इमर्सिव मोड", "दोनों सिस्टम बार छिपाता है; किनारे से स्वाइप करने पर अस्थायी रूप से दिखते हैं",
        "पूरे ऐप पर लागू करें", "बंद होने पर बार केवल फ़ुल-स्क्रीन प्लेयर में छिपते हैं"
    ),
    "id" to immersive(
        "Mode imersif",
        "Sembunyikan bilah status", "Geser dari tepi atas untuk menampilkannya sebentar",
        "Sembunyikan bilah navigasi", "Geser dari tepi bawah untuk menampilkannya sebentar",
        "Mode imersif penuh", "Menyembunyikan kedua bilah; geser dari tepi untuk menampilkannya sementara",
        "Terapkan ke seluruh aplikasi", "Jika nonaktif, bilah hanya disembunyikan di pemutar layar penuh"
    ),
    "vi" to immersive(
        "Chế độ toàn màn hình",
        "Ẩn thanh trạng thái", "Vuốt từ cạnh trên để hiện tạm thời",
        "Ẩn thanh điều hướng", "Vuốt từ cạnh dưới để hiện tạm thời",
        "Toàn màn hình hoàn toàn", "Ẩn cả hai thanh hệ thống; vuốt từ cạnh để hiện tạm thời",
        "Áp dụng cho toàn ứng dụng", "Khi tắt, các thanh chỉ bị ẩn trong trình phát toàn màn hình"
    ),
    "th" to immersive(
        "โหมดเต็มจอ",
        "ซ่อนแถบสถานะ", "ปัดจากขอบบนเพื่อแสดงชั่วคราว",
        "ซ่อนแถบนำทาง", "ปัดจากขอบล่างเพื่อแสดงชั่วคราว",
        "โหมดเต็มจอสมบูรณ์", "ซ่อนแถบระบบทั้งสอง ปัดจากขอบเพื่อแสดงชั่วคราว",
        "ใช้กับทั้งแอป", "เมื่อปิด แถบจะถูกซ่อนเฉพาะในเครื่องเล่นแบบเต็มจอ"
    ),
    "fil" to immersive(
        "Immersive mode",
        "Itago ang status bar", "Mag-swipe mula sa itaas na gilid para saglit itong ipakita",
        "Itago ang navigation bar", "Mag-swipe mula sa ibabang gilid para saglit itong ipakita",
        "Buong immersive mode", "Itinatago ang dalawang system bar; ang swipe mula sa gilid ay pansamantalang nagpapakita sa kanila",
        "Ilapat sa buong app", "Kapag naka-off, sa full-screen player lang nakatago ang mga bar"
    ),
    "he" to immersive(
        "מצב סוחף",
        "הסתרת שורת המצב", "החליקו מהקצה העליון כדי להציג אותה לרגע",
        "הסתרת סרגל הניווט", "החליקו מהקצה התחתון כדי להציג אותו לרגע",
        "מצב סוחף מלא", "מסתיר את שני הסרגלים; החלקה מהקצה מציגה אותם זמנית",
        "החלה על כל האפליקציה", "כשכבוי, הסרגלים מוסתרים רק בנגן במסך מלא"
    ),
    "sk" to immersive(
        "Imerzívny režim",
        "Skryť stavový riadok", "Potiahnutím od horného okraja ho na chvíľu zobrazíte",
        "Skryť navigačný panel", "Potiahnutím od dolného okraja ho na chvíľu zobrazíte",
        "Úplne imerzívny režim", "Skryje oba panely; potiahnutie od okraja ich dočasne zobrazí",
        "Použiť v celej aplikácii", "Vypnuté: panely sa skryjú len v prehrávači na celú obrazovku"
    ),
    "bn" to immersive(
        "ইমার্সিভ মোড",
        "স্ট্যাটাস বার লুকান", "কিছুক্ষণ দেখাতে উপরের প্রান্ত থেকে সোয়াইপ করুন",
        "নেভিগেশন বার লুকান", "কিছুক্ষণ দেখাতে নিচের প্রান্ত থেকে সোয়াইপ করুন",
        "সম্পূর্ণ ইমার্সিভ মোড", "দুটি সিস্টেম বারই লুকায়; প্রান্ত থেকে সোয়াইপ করলে সাময়িকভাবে দেখা যায়",
        "পুরো অ্যাপে প্রয়োগ করুন", "বন্ধ থাকলে বার শুধু ফুল-স্ক্রিন প্লেয়ারে লুকানো থাকে"
    ),
    "kk" to immersive(
        "Ену режимі",
        "Күй жолағын жасыру", "Қысқа уақыт көрсету үшін жоғарғы шеттен сырғытыңыз",
        "Навигация жолағын жасыру", "Қысқа уақыт көрсету үшін төменгі шеттен сырғытыңыз",
        "Толық ену режимі", "Екі жүйелік жолақты да жасырады; шеттен сырғыту оларды уақытша көрсетеді",
        "Бүкіл қолданбаға қолдану", "Өшірулі болса, жолақтар тек толық экранды ойнатқышта жасырылады"
    ),
    "sr" to immersive(
        "Imerzivni režim",
        "Sakrij statusnu traku", "Prevucite od gornje ivice da biste je nakratko prikazali",
        "Sakrij navigacionu traku", "Prevucite od donje ivice da biste je nakratko prikazali",
        "Potpuno imerzivni režim", "Sakriva obe trake; prevlačenje od ivice ih privremeno prikazuje",
        "Primeni na celu aplikaciju", "Isključeno: trake se sakrivaju samo u plejeru preko celog ekrana"
    ),
    "sq" to immersive(
        "Modaliteti zhytës",
        "Fshih shiritin e statusit", "Rrëshqit nga skaji i sipërm për ta shfaqur për një çast",
        "Fshih shiritin e navigimit", "Rrëshqit nga skaji i poshtëm për ta shfaqur për një çast",
        "Modaliteti zhytës i plotë", "Fsheh të dy shiritat; rrëshqitja nga skaji i shfaq përkohësisht",
        "Zbato në të gjithë aplikacionin", "Kur është joaktiv, shiritat fshihen vetëm në luajtësin në ekran të plotë"
    ),
    "hr" to immersive(
        "Imerzivni način",
        "Sakrij traku statusa", "Prijeđite prstom od gornjeg ruba da je nakratko prikažete",
        "Sakrij navigacijsku traku", "Prijeđite prstom od donjeg ruba da je nakratko prikažete",
        "Potpuno imerzivni način", "Skriva obje trake; povlačenje od ruba privremeno ih prikazuje",
        "Primijeni na cijelu aplikaciju", "Isključeno: trake se skrivaju samo u playeru preko cijelog zaslona"
    ),
    "bg" to immersive(
        "Режим на потапяне",
        "Скриване на лентата на състоянието", "Плъзнете от горния край, за да я покажете за момент",
        "Скриване на лентата за навигация", "Плъзнете от долния край, за да я покажете за момент",
        "Пълен режим на потапяне", "Скрива двете ленти; плъзгане от края ги показва временно",
        "Прилагане в цялото приложение", "Когато е изключено, лентите се скриват само в плейъра на цял екран"
    ),
    "hu" to immersive(
        "Magával ragadó mód",
        "Állapotsor elrejtése", "Húzz lefelé a felső szélről a rövid megjelenítéshez",
        "Navigációs sáv elrejtése", "Húzz felfelé az alsó szélről a rövid megjelenítéshez",
        "Teljes magával ragadó mód", "Mindkét rendszersávot elrejti; a szélről húzás ideiglenesen megjeleníti őket",
        "Alkalmazás az egész appban", "Kikapcsolva a sávok csak a teljes képernyős lejátszóban rejtőznek el"
    ),
    "fi" to immersive(
        "Immersiivinen tila",
        "Piilota tilarivi", "Pyyhkäise yläreunasta näyttääksesi sen hetkeksi",
        "Piilota navigointipalkki", "Pyyhkäise alareunasta näyttääksesi sen hetkeksi",
        "Täysin immersiivinen tila", "Piilottaa molemmat palkit; reunapyyhkäisy näyttää ne väliaikaisesti",
        "Käytä koko sovelluksessa", "Pois päältä: palkit piilotetaan vain koko näytön soittimessa"
    ),
    "et" to immersive(
        "Kaasahaarav režiim",
        "Peida olekuriba", "Pühkige ülaservast, et seda hetkeks näidata",
        "Peida navigeerimisriba", "Pühkige alaservast, et seda hetkeks näidata",
        "Täielik kaasahaarav režiim", "Peidab mõlemad ribad; servast pühkimine näitab neid ajutiselt",
        "Rakenda kogu rakendusele", "Välja lülitatuna peidetakse ribad ainult täisekraanpleieris"
    ),
    "nb" to immersive(
        "Oppslukende modus",
        "Skjul statuslinjen", "Sveip fra øvre kant for å vise den et øyeblikk",
        "Skjul navigasjonsfeltet", "Sveip fra nedre kant for å vise det et øyeblikk",
        "Helt oppslukende modus", "Skjuler begge feltene; et sveip fra kanten viser dem midlertidig",
        "Bruk i hele appen", "Av: feltene skjules bare i fullskjermspilleren"
    ),
    "ca" to immersive(
        "Mode immersiu",
        "Amaga la barra d'estat", "Llisca des de la vora superior per mostrar-la un moment",
        "Amaga la barra de navegació", "Llisca des de la vora inferior per mostrar-la un moment",
        "Mode immersiu complet", "Amaga les dues barres; lliscar des de la vora les mostra temporalment",
        "Aplica a tota l'app", "Desactivat: les barres només s'amaguen al reproductor a pantalla completa"
    ),
    "ms" to immersive(
        "Mod imersif",
        "Sembunyikan bar status", "Leret dari tepi atas untuk memaparkannya seketika",
        "Sembunyikan bar navigasi", "Leret dari tepi bawah untuk memaparkannya seketika",
        "Mod imersif penuh", "Menyembunyikan kedua-dua bar; leretan dari tepi memaparkannya buat sementara",
        "Gunakan pada seluruh apl", "Jika dimatikan, bar hanya disembunyikan dalam pemain skrin penuh"
    ),
    "fa" to immersive(
        "حالت فراگیر",
        "پنهان کردن نوار وضعیت", "برای نمایش کوتاه از لبه بالا بکشید",
        "پنهان کردن نوار پیمایش", "برای نمایش کوتاه از لبه پایین بکشید",
        "حالت فراگیر کامل", "هر دو نوار سیستم را پنهان می‌کند؛ کشیدن از لبه آن‌ها را موقتاً نشان می‌دهد",
        "اعمال در کل برنامه", "در حالت خاموش، نوارها فقط در پخش‌کننده تمام‌صفحه پنهان می‌شوند"
    ),
    "zh-Hant" to immersive(
        "沉浸模式",
        "隱藏狀態列", "從頂部邊緣下滑可暫時顯示",
        "隱藏導覽列", "從底部邊緣上滑可暫時顯示",
        "完全沉浸模式", "同時隱藏兩個系統列；從邊緣滑動可暫時顯示",
        "套用至整個應用程式", "關閉時僅在全螢幕播放器中隱藏系統列"
    )
)

internal fun immersiveModeLocalizationCodes(): Set<String> = immersiveModeBundles.keys

internal fun immersiveModeLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(immersiveModeBundles, code)
