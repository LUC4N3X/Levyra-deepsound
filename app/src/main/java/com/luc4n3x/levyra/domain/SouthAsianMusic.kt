package com.luc4n3x.levyra.domain

internal data class SouthAsianMusic(
    val country: String,
    val artists: List<String>,
    val hits: String,
    val rap: String,
    val pop: String,
    val drive: String,
    val workout: String,
    val chill: String,
    val focus: String,
    val party: String,
    val sad: String,
    val quickTitle: String,
    val localTitle: String,
    val energyTitle: String,
    val artistsTitle: String,
    val suggestionsTitle: String
) {
    val quickSearches: List<String> = artists.take(4) + listOf(hits, rap, drive, workout)
}

internal val southAsianMusic = mapOf(
    "ur" to SouthAsianMusic(
        "PK",
        listOf("Atif Aslam", "Nusrat Fateh Ali Khan", "Arooj Aftab", "Ali Sethi", "Abida Parveen", "Strings", "Young Stunners", "Coke Studio Pakistan"),
        "مشہور اردو گانے", "اردو ریپ", "اردو پاپ", "رات کی ڈرائیو کے لیے اردو گانے", "ورزش کے لیے اردو گانے",
        "پرسکون اردو گانے", "توجہ کے لیے موسیقی", "اردو پارٹی گانے", "اداس اردو گانے",
        "فوری انتخاب", "اردو موسیقی", "جوش بھرے گانے", "فنکار دریافت کریں", "تجاویز"
    ),
    "ta" to SouthAsianMusic(
        "IN",
        listOf("A. R. Rahman", "Ilaiyaraaja", "Anirudh Ravichander", "Yuvan Shankar Raja", "Santhosh Narayanan", "Sid Sriram", "S. P. Balasubrahmanyam", "Chinmayi"),
        "பிரபலமான தமிழ் பாடல்கள்", "தமிழ் ராப் பாடல்கள்", "தமிழ் பாப் பாடல்கள்", "இரவுப் பயணத்திற்கான தமிழ் பாடல்கள்", "உடற்பயிற்சிக்கான தமிழ் பாடல்கள்",
        "மென்மையான தமிழ் பாடல்கள்", "கவனம் செலுத்த உதவும் இசை", "கொண்டாட்டத்திற்கான தமிழ் பாடல்கள்", "சோகமான தமிழ் பாடல்கள்",
        "விரைவுத் தேர்வுகள்", "தமிழ் இசை", "உற்சாகமூட்டும் பாடல்கள்", "கலைஞர்களைக் கண்டறியுங்கள்", "பரிந்துரைகள்"
    ),
    "te" to SouthAsianMusic(
        "IN",
        listOf("S. P. Balasubrahmanyam", "Devi Sri Prasad", "M. M. Keeravani", "Thaman S", "K. S. Chithra", "Sid Sriram", "Ram Miriyala", "Mangli"),
        "ప్రజాదరణ పొందిన తెలుగు పాటలు", "తెలుగు ర్యాప్ పాటలు", "తెలుగు పాప్ పాటలు", "రాత్రి ప్రయాణానికి తెలుగు పాటలు", "వ్యాయామానికి తెలుగు పాటలు",
        "ప్రశాంతమైన తెలుగు పాటలు", "ఏకాగ్రత కోసం సంగీతం", "తెలుగు పార్టీ పాటలు", "విషాద తెలుగు పాటలు",
        "త్వరిత ఎంపికలు", "తెలుగు సంగీతం", "ఉత్సాహాన్నిచ్చే పాటలు", "కళాకారులను కనుగొనండి", "సూచనలు"
    ),
    "mr" to SouthAsianMusic(
        "IN",
        listOf("Ajay-Atul", "Shreya Ghoshal", "Avadhoot Gupte", "Bela Shende", "Vaishali Samant", "Rahul Deshpande", "Suresh Wadkar", "Shankar Mahadevan"),
        "लोकप्रिय मराठी गाणी", "मराठी रॅप", "मराठी पॉप गाणी", "रात्रीच्या प्रवासासाठी मराठी गाणी", "व्यायामासाठी मराठी गाणी",
        "निवांत मराठी गाणी", "एकाग्रतेसाठी संगीत", "पार्टीसाठी मराठी गाणी", "हळवी मराठी गाणी",
        "झटपट निवडी", "मराठी संगीत", "उत्साह देणारी गाणी", "नवे कलाकार शोधा", "सूचना"
    ),
    "gu" to SouthAsianMusic(
        "IN",
        listOf("Aditya Gadhvi", "Kinjal Dave", "Geeta Rabari", "Falguni Pathak", "Kirtidan Gadhvi", "Osman Mir", "Parthiv Gohil", "Jigardan Gadhavi"),
        "લોકપ્રિય ગુજરાતી ગીતો", "ગુજરાતી રૅપ", "ગુજરાતી પૉપ ગીતો", "રાત્રિની સફર માટે ગુજરાતી ગીતો", "કસરત માટે ગુજરાતી ગીતો",
        "હળવાં ગુજરાતી ગીતો", "એકાગ્રતા માટે સંગીત", "પાર્ટી માટે ગુજરાતી ગીતો", "ઉદાસ ગુજરાતી ગીતો",
        "ઝડપી પસંદગીઓ", "ગુજરાતી સંગીત", "જોશીલાં ગીતો", "નવા કલાકારો શોધો", "સૂચનો"
    )
)
