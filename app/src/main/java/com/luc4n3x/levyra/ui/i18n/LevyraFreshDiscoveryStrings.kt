package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.FreshDiscoveryWindow

internal data class FreshDiscoveryCopy(
    val title: String,
    val description: String,
    val windowTitle: String,
    val windowDescription: String,
    val days7: String,
    val days30: String,
    val days90: String,
    val allTime: String,
    val allowFavoritesTitle: String,
    val allowFavoritesDescription: String
) {
    fun window(window: FreshDiscoveryWindow): String = when (window) {
        FreshDiscoveryWindow.DAYS_7 -> days7
        FreshDiscoveryWindow.DAYS_30 -> days30
        FreshDiscoveryWindow.DAYS_90 -> days90
        FreshDiscoveryWindow.ALL_TIME -> allTime
    }
}

private val freshDiscoveryCopies = mapOf(
    "en" to FreshDiscoveryCopy(
        "Fresh Discovery",
        "Keeps songs you've already heard out of recommendations and automatic queues. Songs you choose yourself always play.",
        "Exclusion period",
        "Songs heard within this period are skipped",
        "7 days",
        "30 days",
        "90 days",
        "All time",
        "Allow favorites",
        "Favorite songs can still appear even if you've heard them"
    ),
    "it" to FreshDiscoveryCopy(
        "Fresh Discovery",
        "Evita di riproporre i brani già ascoltati nelle raccomandazioni e nelle code automatiche. I brani scelti da te vengono sempre riprodotti.",
        "Periodo di esclusione",
        "I brani ascoltati in questo periodo vengono saltati",
        "7 giorni",
        "30 giorni",
        "90 giorni",
        "Da sempre",
        "Consenti preferiti",
        "I brani preferiti possono comparire anche se li hai già ascoltati"
    ),
    "es" to FreshDiscoveryCopy(
        "Descubrimiento fresco",
        "Evita que las canciones que ya escuchaste vuelvan a aparecer en recomendaciones y colas automáticas. Las canciones que eliges tú siempre se reproducen.",
        "Periodo de exclusión",
        "Se omiten las canciones escuchadas en este periodo",
        "7 días",
        "30 días",
        "90 días",
        "Desde siempre",
        "Permitir favoritas",
        "Las canciones favoritas pueden aparecer aunque ya las hayas escuchado"
    ),
    "fr" to FreshDiscoveryCopy(
        "Découverte fraîche",
        "Évite de reproposer les titres déjà écoutés dans les recommandations et les files automatiques. Les titres que vous choisissez sont toujours lus.",
        "Période d’exclusion",
        "Les titres écoutés pendant cette période sont ignorés",
        "7 jours",
        "30 jours",
        "90 jours",
        "Depuis toujours",
        "Autoriser les favoris",
        "Les titres favoris peuvent apparaître même si vous les avez déjà écoutés"
    ),
    "de" to FreshDiscoveryCopy(
        "Frische Entdeckungen",
        "Hält bereits gehörte Titel aus Empfehlungen und automatischen Warteschlangen fern. Selbst gewählte Titel werden immer abgespielt.",
        "Ausschlusszeitraum",
        "In diesem Zeitraum gehörte Titel werden übersprungen",
        "7 Tage",
        "30 Tage",
        "90 Tage",
        "Gesamter Zeitraum",
        "Favoriten erlauben",
        "Lieblingstitel können erscheinen, auch wenn du sie schon gehört hast"
    ),
    "pt" to FreshDiscoveryCopy(
        "Descoberta nova",
        "Evita repetir músicas que você já ouviu nas recomendações e nas filas automáticas. As músicas que você escolhe sempre tocam.",
        "Período de exclusão",
        "Músicas ouvidas neste período são ignoradas",
        "7 dias",
        "30 dias",
        "90 dias",
        "Desde sempre",
        "Permitir favoritas",
        "Músicas favoritas podem aparecer mesmo que você já as tenha ouvido"
    ),
    "nl" to FreshDiscoveryCopy(
        "Verse ontdekkingen",
        "Houdt nummers die je al hebt gehoord uit aanbevelingen en automatische wachtrijen. Nummers die je zelf kiest, worden altijd afgespeeld.",
        "Uitsluitingsperiode",
        "Nummers die je in deze periode hebt gehoord, worden overgeslagen",
        "7 dagen",
        "30 dagen",
        "90 dagen",
        "Altijd",
        "Favorieten toestaan",
        "Favoriete nummers kunnen verschijnen, ook als je ze al hebt gehoord"
    ),
    "pl" to FreshDiscoveryCopy(
        "Świeże odkrycia",
        "Nie proponuje ponownie już słuchanych utworów w rekomendacjach i automatycznych kolejkach. Utwory wybrane przez Ciebie zawsze są odtwarzane.",
        "Okres wykluczenia",
        "Utwory słuchane w tym okresie są pomijane",
        "7 dni",
        "30 dni",
        "90 dni",
        "Od zawsze",
        "Zezwalaj na ulubione",
        "Ulubione utwory mogą się pojawiać, nawet jeśli już ich słuchano"
    ),
    "ro" to FreshDiscoveryCopy(
        "Descoperiri proaspete",
        "Nu mai propune piesele deja ascultate în recomandări și în cozile automate. Piesele alese de tine sunt redate întotdeauna.",
        "Perioadă de excludere",
        "Piesele ascultate în această perioadă sunt omise",
        "7 zile",
        "30 de zile",
        "90 de zile",
        "Dintotdeauna",
        "Permite favoritele",
        "Piesele favorite pot apărea chiar dacă le-ai ascultat deja"
    ),
    "el" to FreshDiscoveryCopy(
        "Φρέσκες ανακαλύψεις",
        "Κρατά τα τραγούδια που έχεις ήδη ακούσει εκτός προτάσεων και αυτόματων ουρών. Τα τραγούδια που επιλέγεις εσύ παίζουν πάντα.",
        "Περίοδος εξαίρεσης",
        "Τα τραγούδια που ακούστηκαν σε αυτή την περίοδο παραλείπονται",
        "7 ημέρες",
        "30 ημέρες",
        "90 ημέρες",
        "Από πάντα",
        "Να επιτρέπονται τα αγαπημένα",
        "Τα αγαπημένα τραγούδια μπορούν να εμφανίζονται ακόμη κι αν τα έχεις ακούσει"
    ),
    "sv" to FreshDiscoveryCopy(
        "Färska upptäckter",
        "Håller låtar du redan har hört borta från rekommendationer och automatiska köer. Låtar du själv väljer spelas alltid.",
        "Uteslutningsperiod",
        "Låtar som hörts under den här perioden hoppas över",
        "7 dagar",
        "30 dagar",
        "90 dagar",
        "Hela tiden",
        "Tillåt favoriter",
        "Favoritlåtar kan visas även om du redan har hört dem"
    ),
    "da" to FreshDiscoveryCopy(
        "Friske opdagelser",
        "Holder sange, du allerede har hørt, ude af anbefalinger og automatiske køer. Sange, du selv vælger, afspilles altid.",
        "Udelukkelsesperiode",
        "Sange hørt i denne periode springes over",
        "7 dage",
        "30 dage",
        "90 dage",
        "Altid",
        "Tillad favoritter",
        "Favoritsange kan stadig vises, selv om du har hørt dem"
    ),
    "cs" to FreshDiscoveryCopy(
        "Čerstvé objevy",
        "Nenabízí znovu již poslechnuté skladby v doporučeních a automatických frontách. Skladby, které vyberete sami, se přehrají vždy.",
        "Období vyloučení",
        "Skladby poslechnuté v tomto období se přeskočí",
        "7 dní",
        "30 dní",
        "90 dní",
        "Odjakživa",
        "Povolit oblíbené",
        "Oblíbené skladby se mohou objevit, i když jste je už slyšeli"
    ),
    "sk" to FreshDiscoveryCopy(
        "Čerstvé objavy",
        "Neponúka znova už vypočuté skladby v odporúčaniach a automatických frontoch. Skladby, ktoré si vyberiete sami, sa prehrajú vždy.",
        "Obdobie vylúčenia",
        "Skladby vypočuté v tomto období sa preskočia",
        "7 dní",
        "30 dní",
        "90 dní",
        "Odjakživa",
        "Povoliť obľúbené",
        "Obľúbené skladby sa môžu objaviť, aj keď ste ich už počuli"
    ),
    "bn" to FreshDiscoveryCopy(
        "নতুন আবিষ্কার",
        "আগে শোনা গানগুলো সুপারিশ ও স্বয়ংক্রিয় কিউতে আবার আনে না। আপনি নিজে বেছে নেওয়া গান সবসময় বাজবে।",
        "বাদ দেওয়ার সময়কাল",
        "এই সময়ের মধ্যে শোনা গানগুলো বাদ দেওয়া হয়",
        "৭ দিন",
        "৩০ দিন",
        "৯০ দিন",
        "সবসময়",
        "প্রিয় গান অনুমতি দিন",
        "আগে শুনে থাকলেও প্রিয় গানগুলো দেখা যেতে পারে"
    ),
    "kk" to FreshDiscoveryCopy(
        "Жаңа ашылымдар",
        "Бұрын тыңдалған әндерді ұсыныстар мен автоматты кезектерде қайта ұсынбайды. Өзіңіз таңдаған әндер әрқашан ойнатылады.",
        "Шығарып тастау кезеңі",
        "Осы кезеңде тыңдалған әндер өткізіп жіберіледі",
        "7 күн",
        "30 күн",
        "90 күн",
        "Барлық уақыт",
        "Таңдаулыларға рұқсат беру",
        "Таңдаулы әндер бұрын тыңдалған болса да шыға береді"
    ),
    "sr" to FreshDiscoveryCopy(
        "Свежа открића",
        "Не нуди поново већ слушане песме у препорукама и аутоматским редовима. Песме које сами изаберете увек се пуштају.",
        "Период искључивања",
        "Песме слушане у овом периоду се прескачу",
        "7 дана",
        "30 дана",
        "90 дана",
        "Одувек",
        "Дозволи омиљене",
        "Омиљене песме могу да се појаве иако сте их већ слушали"
    ),
    "sq" to FreshDiscoveryCopy(
        "Zbulime të freskëta",
        "Nuk i ripropozon këngët e dëgjuara më parë në rekomandime dhe në radhët automatike. Këngët që zgjidhni vetë luhen gjithmonë.",
        "Periudha e përjashtimit",
        "Këngët e dëgjuara në këtë periudhë kapërcehen",
        "7 ditë",
        "30 ditë",
        "90 ditë",
        "Gjithmonë",
        "Lejo të preferuarat",
        "Këngët e preferuara mund të shfaqen edhe nëse i keni dëgjuar"
    ),
    "hr" to FreshDiscoveryCopy(
        "Svježa otkrića",
        "Ne nudi ponovno već slušane pjesme u preporukama i automatskim redovima. Pjesme koje sami odaberete uvijek se reproduciraju.",
        "Razdoblje isključivanja",
        "Pjesme slušane u ovom razdoblju se preskaču",
        "7 dana",
        "30 dana",
        "90 dana",
        "Oduvijek",
        "Dopusti omiljene",
        "Omiljene pjesme mogu se pojaviti iako ste ih već slušali"
    ),
    "bg" to FreshDiscoveryCopy(
        "Свежи открития",
        "Не предлага отново вече слушани песни в препоръките и автоматичните опашки. Песните, които избирате сами, винаги се пускат.",
        "Период на изключване",
        "Песните, слушани в този период, се пропускат",
        "7 дни",
        "30 дни",
        "90 дни",
        "Откакто има история",
        "Разрешаване на любими",
        "Любимите песни може да се появяват, дори ако вече сте ги слушали"
    ),
    "hu" to FreshDiscoveryCopy(
        "Friss felfedezések",
        "A már meghallgatott dalokat nem ajánlja újra az ajánlásokban és az automatikus sorokban. Az általad választott dalok mindig lejátszódnak.",
        "Kizárási időszak",
        "Az ebben az időszakban hallgatott dalok kimaradnak",
        "7 nap",
        "30 nap",
        "90 nap",
        "Mindig",
        "Kedvencek engedélyezése",
        "A kedvenc dalok akkor is megjelenhetnek, ha már hallottad őket"
    ),
    "fi" to FreshDiscoveryCopy(
        "Tuoreet löydöt",
        "Pitää jo kuunnellut kappaleet poissa suosituksista ja automaattisista jonoista. Itse valitsemasi kappaleet soivat aina.",
        "Poissulkemisjakso",
        "Tällä jaksolla kuunnellut kappaleet ohitetaan",
        "7 päivää",
        "30 päivää",
        "90 päivää",
        "Koko historia",
        "Salli suosikit",
        "Suosikkikappaleita voi näkyä, vaikka olisit jo kuunnellut ne"
    ),
    "et" to FreshDiscoveryCopy(
        "Värsked avastused",
        "Ei paku juba kuulatud lugusid uuesti soovitustes ja automaatsetes järjekordades. Ise valitud lood mängivad alati.",
        "Välistamise periood",
        "Selle perioodi jooksul kuulatud lood jäetakse vahele",
        "7 päeva",
        "30 päeva",
        "90 päeva",
        "Kogu aeg",
        "Luba lemmikud",
        "Lemmiklood võivad ilmuda ka siis, kui oled neid juba kuulanud"
    ),
    "nb" to FreshDiscoveryCopy(
        "Ferske oppdagelser",
        "Holder sanger du allerede har hørt, utenfor anbefalinger og automatiske køer. Sanger du velger selv, spilles alltid.",
        "Utelukkelsesperiode",
        "Sanger hørt i denne perioden hoppes over",
        "7 dager",
        "30 dager",
        "90 dager",
        "Hele tiden",
        "Tillat favoritter",
        "Favorittsanger kan fortsatt vises selv om du har hørt dem"
    ),
    "ca" to FreshDiscoveryCopy(
        "Descobriment fresc",
        "Evita tornar a proposar cançons que ja has escoltat a les recomanacions i a les cues automàtiques. Les cançons que tries tu sempre es reprodueixen.",
        "Període d’exclusió",
        "S’ometen les cançons escoltades en aquest període",
        "7 dies",
        "30 dies",
        "90 dies",
        "Des de sempre",
        "Permet les preferides",
        "Les cançons preferides poden aparèixer encara que ja les hagis escoltat"
    ),
    "uk" to FreshDiscoveryCopy(
        "Свіжі відкриття",
        "Не пропонує повторно вже прослухані треки в рекомендаціях і автоматичних чергах. Треки, які ви обираєте самі, відтворюються завжди.",
        "Період виключення",
        "Треки, прослухані за цей період, пропускаються",
        "7 днів",
        "30 днів",
        "90 днів",
        "За весь час",
        "Дозволити улюблені",
        "Улюблені треки можуть з’являтися, навіть якщо ви їх уже слухали"
    ),
    "ru" to FreshDiscoveryCopy(
        "Свежие открытия",
        "Не предлагает повторно уже прослушанные треки в рекомендациях и автоматических очередях. Треки, которые вы выбираете сами, воспроизводятся всегда.",
        "Период исключения",
        "Треки, прослушанные за этот период, пропускаются",
        "7 дней",
        "30 дней",
        "90 дней",
        "За всё время",
        "Разрешить избранное",
        "Избранные треки могут появляться, даже если вы их уже слушали"
    ),
    "tr" to FreshDiscoveryCopy(
        "Taze Keşifler",
        "Daha önce dinlediğin şarkıları önerilerde ve otomatik sıralarda tekrar sunmaz. Kendi seçtiğin şarkılar her zaman çalar.",
        "Hariç tutma süresi",
        "Bu sürede dinlenen şarkılar atlanır",
        "7 gün",
        "30 gün",
        "90 gün",
        "Tüm zamanlar",
        "Favorilere izin ver",
        "Favori şarkılar daha önce dinlemiş olsan bile görünebilir"
    ),
    "ar" to FreshDiscoveryCopy(
        "اكتشافات جديدة",
        "يمنع إعادة اقتراح الأغاني التي استمعت إليها في التوصيات وقوائم الانتظار التلقائية. الأغاني التي تختارها بنفسك تُشغَّل دائمًا.",
        "فترة الاستبعاد",
        "يتم تخطي الأغاني التي استمعت إليها خلال هذه الفترة",
        "7 أيام",
        "30 يومًا",
        "90 يومًا",
        "منذ البداية",
        "السماح بالمفضلة",
        "قد تظهر الأغاني المفضلة حتى لو استمعت إليها من قبل"
    ),
    "fa" to FreshDiscoveryCopy(
        "کشف‌های تازه",
        "آهنگ‌هایی را که قبلاً شنیده‌اید دوباره در پیشنهادها و صف‌های خودکار نمی‌آورد. آهنگ‌هایی که خودتان انتخاب می‌کنید همیشه پخش می‌شوند.",
        "دوره حذف",
        "آهنگ‌هایی که در این دوره شنیده شده‌اند رد می‌شوند",
        "۷ روز",
        "۳۰ روز",
        "۹۰ روز",
        "از ابتدا",
        "اجازه به موردعلاقه‌ها",
        "آهنگ‌های موردعلاقه حتی اگر قبلاً شنیده شده باشند ممکن است نمایش داده شوند"
    ),
    "zh" to FreshDiscoveryCopy(
        "新鲜发现",
        "不在推荐和自动队列中重复推送你已听过的歌曲。你主动选择的歌曲始终会播放。",
        "排除期限",
        "跳过在此期间听过的歌曲",
        "7 天",
        "30 天",
        "90 天",
        "全部时间",
        "允许收藏歌曲",
        "即使听过，收藏的歌曲仍可能出现"
    ),
    "zh-Hant" to FreshDiscoveryCopy(
        "新鮮發現",
        "不在推薦和自動佇列中重複推送你已聽過的歌曲。你主動選擇的歌曲一律會播放。",
        "排除期間",
        "略過在此期間聽過的歌曲",
        "7 天",
        "30 天",
        "90 天",
        "所有時間",
        "允許收藏歌曲",
        "即使聽過，收藏的歌曲仍可能出現"
    ),
    "ja" to FreshDiscoveryCopy(
        "フレッシュディスカバリー",
        "おすすめや自動キューで、すでに聴いた曲を再び提案しません。自分で選んだ曲は常に再生されます。",
        "除外期間",
        "この期間内に聴いた曲はスキップされます",
        "7日間",
        "30日間",
        "90日間",
        "全期間",
        "お気に入りを許可",
        "聴いたことがあっても、お気に入りの曲は表示される場合があります"
    ),
    "ko" to FreshDiscoveryCopy(
        "새로운 발견",
        "이미 들은 곡을 추천과 자동 대기열에 다시 넣지 않습니다. 직접 선택한 곡은 항상 재생됩니다.",
        "제외 기간",
        "이 기간에 들은 곡은 건너뜁니다",
        "7일",
        "30일",
        "90일",
        "전체 기간",
        "좋아요 표시한 곡 허용",
        "이미 들었더라도 좋아요 표시한 곡은 나올 수 있습니다"
    ),
    "hi" to FreshDiscoveryCopy(
        "नई खोज",
        "पहले सुने गए गानों को सुझावों और स्वचालित कतारों में दोबारा नहीं लाता। आपके खुद चुने गए गाने हमेशा चलते हैं।",
        "बाहर रखने की अवधि",
        "इस अवधि में सुने गए गाने छोड़ दिए जाते हैं",
        "7 दिन",
        "30 दिन",
        "90 दिन",
        "शुरुआत से",
        "पसंदीदा की अनुमति दें",
        "पसंदीदा गाने पहले सुने होने पर भी दिख सकते हैं"
    ),
    "id" to FreshDiscoveryCopy(
        "Penemuan Segar",
        "Tidak menampilkan lagi lagu yang sudah kamu dengar di rekomendasi dan antrean otomatis. Lagu yang kamu pilih sendiri selalu diputar.",
        "Periode pengecualian",
        "Lagu yang didengar dalam periode ini dilewati",
        "7 hari",
        "30 hari",
        "90 hari",
        "Sepanjang waktu",
        "Izinkan favorit",
        "Lagu favorit tetap bisa muncul meski sudah pernah kamu dengar"
    ),
    "ms" to FreshDiscoveryCopy(
        "Penemuan Segar",
        "Tidak mencadangkan semula lagu yang telah anda dengar dalam cadangan dan baris gilir automatik. Lagu yang anda pilih sendiri sentiasa dimainkan.",
        "Tempoh pengecualian",
        "Lagu yang didengar dalam tempoh ini akan dilangkau",
        "7 hari",
        "30 hari",
        "90 hari",
        "Sepanjang masa",
        "Benarkan kegemaran",
        "Lagu kegemaran masih boleh muncul walaupun anda pernah mendengarnya"
    ),
    "vi" to FreshDiscoveryCopy(
        "Khám phá mới",
        "Không đề xuất lại những bài bạn đã nghe trong gợi ý và hàng đợi tự động. Bài bạn tự chọn luôn được phát.",
        "Khoảng thời gian loại trừ",
        "Bỏ qua các bài đã nghe trong khoảng thời gian này",
        "7 ngày",
        "30 ngày",
        "90 ngày",
        "Mọi lúc",
        "Cho phép bài yêu thích",
        "Bài yêu thích vẫn có thể xuất hiện dù bạn đã nghe"
    ),
    "th" to FreshDiscoveryCopy(
        "การค้นพบใหม่",
        "ไม่นำเพลงที่คุณเคยฟังแล้วกลับมาแนะนำหรือใส่ในคิวอัตโนมัติอีก เพลงที่คุณเลือกเองจะเล่นเสมอ",
        "ช่วงเวลาที่ยกเว้น",
        "ข้ามเพลงที่ฟังในช่วงเวลานี้",
        "7 วัน",
        "30 วัน",
        "90 วัน",
        "ตลอดเวลา",
        "อนุญาตเพลงโปรด",
        "เพลงโปรดยังอาจปรากฏแม้คุณเคยฟังแล้ว"
    ),
    "fil" to FreshDiscoveryCopy(
        "Sariwang Pagtuklas",
        "Hindi na muling inirerekomenda ang mga kantang napakinggan mo na sa mga rekomendasyon at awtomatikong queue. Laging pinapatugtog ang mga kantang ikaw mismo ang pumili.",
        "Panahon ng pagbubukod",
        "Nilalaktawan ang mga kantang napakinggan sa panahong ito",
        "7 araw",
        "30 araw",
        "90 araw",
        "Kahit kailan",
        "Payagan ang mga paborito",
        "Puwede pa ring lumabas ang mga paboritong kanta kahit napakinggan mo na"
    ),
    "he" to FreshDiscoveryCopy(
        "גילויים טריים",
        "לא מציע שוב שירים ששמעת בהמלצות ובתורים אוטומטיים. שירים שבחרת בעצמך תמיד מתנגנים.",
        "תקופת החרגה",
        "שירים שנשמעו בתקופה זו יידלגו",
        "7 ימים",
        "30 ימים",
        "90 ימים",
        "מאז ומעולם",
        "לאפשר מועדפים",
        "שירים מועדפים עדיין יכולים להופיע גם אם כבר שמעת אותם"
    )
)

internal fun freshDiscoveryCopy(code: String): FreshDiscoveryCopy =
    localizedValueOrEnglish(freshDiscoveryCopies, code)

internal fun freshDiscoveryLocalizationCodes(): Set<String> = freshDiscoveryCopies.keys
