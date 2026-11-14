package com.luc4n3x.levyra.ui.i18n

data class OrbitEqCopy(
    val title: String,
    val subtitle: String,
    val warm: String,
    val bright: String,
    val forward: String,
    val open: String,
    val headroom: String,
    val center: String,
    val compare: String,
    val save: String,
    val padDescription: String,
    val resetPoles: String
)

private fun orbit(vararg value: String): OrbitEqCopy {
    require(value.size == 12)
    return OrbitEqCopy(value[0], value[1], value[2], value[3], value[4], value[5], value[6], value[7], value[8], value[9], value[10], value[11])
}

private val orbitEqCopies = mapOf(
    "en" to orbit(
        "Orbit", "Drag the point: the closer to the edge, the stronger the tone", "Warm", "Bright", "Forward", "Open",
        "Headroom", "Center", "Hold to compare", "Save as profile", "Tone orbit", "Reset poles"
    ),
    "it" to orbit(
        "Orbita", "Trascina il punto: più ti avvicini al bordo, più il tono cambia", "Caldo", "Brillante", "Presente", "Arioso",
        "Margine", "Centro", "Tieni premuto per confrontare", "Salva come profilo", "Orbita del tono", "Ripristina poli"
    ),
    "es" to orbit(
        "Órbita", "Arrastra el punto: cuanto más cerca del borde, más cambia el tono", "Cálido", "Brillante", "Presente", "Abierto",
        "Margen", "Centro", "Mantén pulsado para comparar", "Guardar como perfil", "Órbita de tono", "Restablecer polos"
    ),
    "fr" to orbit(
        "Orbite", "Faites glisser le point : plus il approche du bord, plus le timbre change", "Chaud", "Brillant", "Présent", "Ouvert",
        "Marge", "Centre", "Maintenir pour comparer", "Enregistrer comme profil", "Orbite du timbre", "Réinitialiser les pôles"
    ),
    "de" to orbit(
        "Orbit", "Ziehe den Punkt: je näher am Rand, desto stärker der Klang", "Warm", "Hell", "Präsent", "Offen",
        "Reserve", "Mitte", "Gedrückt halten zum Vergleichen", "Als Profil speichern", "Klang-Orbit", "Pole zurücksetzen"
    ),
    "pt" to orbit(
        "Órbita", "Arraste o ponto: quanto mais perto da borda, mais o tom muda", "Quente", "Brilhante", "Presente", "Aberto",
        "Margem", "Centro", "Mantenha premido para comparar", "Guardar como perfil", "Órbita do tom", "Repor polos"
    ),
    "nl" to orbit(
        "Baan", "Sleep het punt: hoe dichter bij de rand, hoe sterker de klank", "Warm", "Helder", "Direct", "Open",
        "Marge", "Midden", "Ingedrukt houden om te vergelijken", "Opslaan als profiel", "Klankbaan", "Polen herstellen"
    ),
    "pl" to orbit(
        "Orbita", "Przeciągnij punkt: im bliżej krawędzi, tym mocniejsza barwa", "Ciepło", "Jasno", "Blisko", "Przestrzennie",
        "Zapas", "Środek", "Przytrzymaj, aby porównać", "Zapisz jako profil", "Orbita barwy", "Przywróć bieguny"
    ),
    "ro" to orbit(
        "Orbită", "Trage punctul: cu cât e mai aproape de margine, cu atât tonul e mai puternic", "Cald", "Luminos", "Prezent", "Aerisit",
        "Rezervă", "Centru", "Ține apăsat pentru comparație", "Salvează ca profil", "Orbita tonului", "Resetează polii"
    ),
    "el" to orbit(
        "Τροχιά", "Σύρετε το σημείο: όσο πιο κοντά στην άκρη, τόσο πιο έντονος ο ήχος", "Ζεστό", "Φωτεινό", "Μπροστά", "Ανοιχτό",
        "Περιθώριο", "Κέντρο", "Πατήστε παρατεταμένα για σύγκριση", "Αποθήκευση ως προφίλ", "Τροχιά ήχου", "Επαναφορά πόλων"
    ),
    "sv" to orbit(
        "Omlopp", "Dra punkten: ju närmare kanten, desto starkare klang", "Varm", "Ljus", "Framträdande", "Öppen",
        "Marginal", "Mitten", "Håll in för att jämföra", "Spara som profil", "Klangomlopp", "Återställ poler"
    ),
    "da" to orbit(
        "Kredsløb", "Træk punktet: jo tættere på kanten, desto stærkere klang", "Varm", "Lys", "Fremtrædende", "Åben",
        "Margin", "Midte", "Hold nede for at sammenligne", "Gem som profil", "Klangkredsløb", "Nulstil poler"
    ),
    "cs" to orbit(
        "Orbita", "Táhněte bodem: čím blíž k okraji, tím výraznější zvuk", "Teplý", "Jasný", "Blízký", "Otevřený",
        "Rezerva", "Střed", "Podržte pro porovnání", "Uložit jako profil", "Orbita zvuku", "Obnovit póly"
    ),
    "sk" to orbit(
        "Orbita", "Ťahajte bod: čím bližšie k okraju, tým výraznejší zvuk", "Teplý", "Jasný", "Blízky", "Otvorený",
        "Rezerva", "Stred", "Podržte na porovnanie", "Uložiť ako profil", "Orbita zvuku", "Obnoviť póly"
    ),
    "bn" to orbit(
        "কক্ষপথ", "বিন্দুটি টানুন: প্রান্তের যত কাছে, সুর তত জোরালো", "উষ্ণ", "উজ্জ্বল", "সামনে", "খোলা",
        "হেডরুম", "কেন্দ্র", "তুলনা করতে চেপে ধরুন", "প্রোফাইল হিসেবে সংরক্ষণ", "সুরের কক্ষপথ", "পোল রিসেট করুন"
    ),
    "kk" to orbit(
        "Орбита", "Нүктені сүйреңіз: жиекке жақындаған сайын дыбыс күшейеді", "Жылы", "Ашық", "Алдыңғы", "Кең",
        "Қор", "Орта", "Салыстыру үшін басып тұрыңыз", "Профиль ретінде сақтау", "Дыбыс орбитасы", "Полюстерді қалпына келтіру"
    ),
    "sr" to orbit(
        "Орбита", "Превуците тачку: што ближе ивици, то јачи тон", "Топло", "Светло", "Близу", "Отворено",
        "Резерва", "Средина", "Задржите за поређење", "Сачувај као профил", "Орбита тона", "Врати полове"
    ),
    "sq" to orbit(
        "Orbita", "Tërhiq pikën: sa më afër skajit, aq më i fortë toni", "I ngrohtë", "I ndritshëm", "I afërt", "I hapur",
        "Rezervë", "Qendra", "Mbaj shtypur për krahasim", "Ruaj si profil", "Orbita e tingullit", "Rivendos polet"
    ),
    "hr" to orbit(
        "Orbita", "Povuci točku: što bliže rubu, to jači ton", "Toplo", "Svijetlo", "Blizu", "Otvoreno",
        "Rezerva", "Sredina", "Drži za usporedbu", "Spremi kao profil", "Orbita tona", "Vrati polove"
    ),
    "bg" to orbit(
        "Орбита", "Плъзнете точката: колкото по-близо до ръба, толкова по-силен тонът", "Топло", "Ярко", "Близко", "Отворено",
        "Резерв", "Център", "Задръжте за сравнение", "Запази като профил", "Орбита на тона", "Нулиране на полюсите"
    ),
    "hu" to orbit(
        "Pálya", "Húzd a pontot: minél közelebb a szélhez, annál erősebb a hangszín", "Meleg", "Fényes", "Közeli", "Nyitott",
        "Tartalék", "Közép", "Tartsd lenyomva az összehasonlításhoz", "Mentés profilként", "Hangszínpálya", "Pólusok visszaállítása"
    ),
    "fi" to orbit(
        "Kiertorata", "Vedä pistettä: mitä lähempänä reunaa, sitä vahvempi sävy", "Lämmin", "Kirkas", "Läheinen", "Avara",
        "Varaus", "Keskikohta", "Pidä painettuna vertailuun", "Tallenna profiiliksi", "Sävyrata", "Palauta navat"
    ),
    "et" to orbit(
        "Orbiit", "Lohista punkti: mida lähemal servale, seda tugevam kõla", "Soe", "Hele", "Lähedane", "Avar",
        "Varu", "Kese", "Võrdlemiseks hoia all", "Salvesta profiilina", "Kõlaorbiit", "Lähtesta poolused"
    ),
    "nb" to orbit(
        "Bane", "Dra punktet: jo nærmere kanten, desto sterkere klang", "Varm", "Lys", "Nær", "Åpen",
        "Margin", "Midten", "Hold inne for å sammenligne", "Lagre som profil", "Klangbane", "Tilbakestill poler"
    ),
    "ca" to orbit(
        "Òrbita", "Arrossega el punt: com més a prop de la vora, més canvia el to", "Càlid", "Brillant", "Present", "Obert",
        "Marge", "Centre", "Mantén premut per comparar", "Desa com a perfil", "Òrbita del to", "Restableix els pols"
    ),
    "uk" to orbit(
        "Орбіта", "Перетягніть точку: що ближче до краю, то сильніший тембр", "Теплий", "Яскравий", "Близький", "Просторий",
        "Запас", "Центр", "Утримуйте, щоб порівняти", "Зберегти як профіль", "Орбіта тембру", "Скинути полюси"
    ),
    "ru" to orbit(
        "Орбита", "Перетащите точку: чем ближе к краю, тем сильнее тембр", "Тёплый", "Яркий", "Близкий", "Просторный",
        "Запас", "Центр", "Удерживайте, чтобы сравнить", "Сохранить как профиль", "Орбита тембра", "Сбросить полюса"
    ),
    "tr" to orbit(
        "Yörünge", "Noktayı sürükleyin: kenara yaklaştıkça ton güçlenir", "Sıcak", "Parlak", "Önde", "Açık",
        "Pay", "Merkez", "Karşılaştırmak için basılı tutun", "Profil olarak kaydet", "Ton yörüngesi", "Kutupları sıfırla"
    ),
    "ar" to orbit(
        "المدار", "اسحب النقطة: كلما اقتربت من الحافة زادت قوة النغمة", "دافئ", "ساطع", "قريب", "منفتح",
        "هامش", "المركز", "اضغط مطولًا للمقارنة", "حفظ كملف", "مدار النغمة", "إعادة ضبط الأقطاب"
    ),
    "fa" to orbit(
        "مدار", "نقطه را بکشید: هرچه به لبه نزدیک‌تر، رنگ صدا قوی‌تر", "گرم", "روشن", "نزدیک", "باز",
        "حاشیه", "مرکز", "برای مقایسه نگه دارید", "ذخیره به‌عنوان نمایه", "مدار رنگ صدا", "بازنشانی قطب‌ها"
    ),
    "zh" to orbit(
        "轨道", "拖动圆点：越靠近边缘，音色越明显", "温暖", "明亮", "前凸", "开阔",
        "余量", "居中", "按住以对比", "另存为配置", "音色轨道", "重置极点"
    ),
    "zh-Hant" to orbit(
        "軌道", "拖曳圓點：越靠近邊緣，音色越明顯", "溫暖", "明亮", "前凸", "開闊",
        "餘量", "置中", "按住以比較", "另存為設定檔", "音色軌道", "重設極點"
    ),
    "ja" to orbit(
        "オービット", "点をドラッグ：端に近いほど音色が強くなります", "ウォーム", "ブライト", "前に出る", "広がり",
        "ヘッドルーム", "中央", "長押しで比較", "プロファイルとして保存", "音色オービット", "ポールをリセット"
    ),
    "ko" to orbit(
        "오빗", "점을 드래그하세요: 가장자리에 가까울수록 음색이 강해집니다", "따뜻하게", "밝게", "앞으로", "개방감",
        "헤드룸", "가운데", "길게 눌러 비교", "프로필로 저장", "음색 오빗", "극 위치 초기화"
    ),
    "hi" to orbit(
        "ऑर्बिट", "बिंदु खींचें: किनारे के जितना पास, स्वर उतना तेज़", "गर्म", "चमकीला", "सामने", "खुला",
        "हेडरूम", "केंद्र", "तुलना के लिए दबाकर रखें", "प्रोफ़ाइल के रूप में सहेजें", "स्वर ऑर्बिट", "पोल रीसेट करें"
    ),
    "id" to orbit(
        "Orbit", "Seret titik: makin dekat ke tepi, makin kuat nadanya", "Hangat", "Terang", "Maju", "Lapang",
        "Ruang aman", "Tengah", "Tahan untuk membandingkan", "Simpan sebagai profil", "Orbit nada", "Atur ulang kutub"
    ),
    "ms" to orbit(
        "Orbit", "Seret titik: makin hampir ke tepi, makin kuat nadanya", "Hangat", "Terang", "Ke depan", "Lapang",
        "Ruang lebih", "Tengah", "Tahan untuk membandingkan", "Simpan sebagai profil", "Orbit nada", "Tetapkan semula kutub"
    ),
    "vi" to orbit(
        "Quỹ đạo", "Kéo điểm: càng gần mép, âm sắc càng rõ", "Ấm", "Sáng", "Gần", "Thoáng",
        "Khoảng dự trữ", "Giữa", "Nhấn giữ để so sánh", "Lưu thành cấu hình", "Quỹ đạo âm sắc", "Đặt lại các cực"
    ),
    "th" to orbit(
        "วงโคจร", "ลากจุด: ยิ่งใกล้ขอบ โทนเสียงยิ่งชัด", "อบอุ่น", "สดใส", "เด่นหน้า", "โปร่ง",
        "เฮดรูม", "กึ่งกลาง", "กดค้างเพื่อเปรียบเทียบ", "บันทึกเป็นโปรไฟล์", "วงโคจรโทนเสียง", "รีเซ็ตขั้ว"
    ),
    "fil" to orbit(
        "Orbit", "I-drag ang punto: habang palapit sa gilid, lumalakas ang tono", "Mainit", "Maliwanag", "Harapan", "Maluwag",
        "Headroom", "Gitna", "Pindutin nang matagal para ikumpara", "I-save bilang profile", "Orbit ng tono", "I-reset ang mga pole"
    ),
    "he" to orbit(
        "מסלול", "גררו את הנקודה: ככל שהיא קרובה יותר לקצה, הגוון חזק יותר", "חם", "בהיר", "קרוב", "פתוח",
        "מרווח", "מרכז", "לחצו לחיצה ארוכה להשוואה", "שמירה כפרופיל", "מסלול הגוון", "איפוס הקטבים"
    )
)

internal fun orbitEqLocalizationCodes(): Set<String> = orbitEqCopies.keys

fun LevyraStrings.orbitEqCopy(): OrbitEqCopy = orbitEqCopies.getValue(translationKey(code)).forBrazil(code) { brazilian() }

private fun OrbitEqCopy.brazilian(): OrbitEqCopy = copy(
    compare = "Mantenha pressionado para comparar",
    save = "Salvar como perfil",
    resetPoles = "Redefinir polos"
)
