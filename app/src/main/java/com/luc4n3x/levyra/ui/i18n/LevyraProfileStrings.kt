package com.luc4n3x.levyra.ui.i18n

private val profileKeyOrder = listOf(
    "profilePhoto",
    "profilePhotoAddSubtitle",
    "profilePhotoChangeSubtitle",
    "profilePhotoRemove",
    "profilePhotoFailed"
)

internal val profileKeys: Set<String> = profileKeyOrder.toSet()

private fun profile(vararg values: String): Map<String, String> {
    require(values.size == profileKeyOrder.size) { "Profile bundle has ${values.size} values" }
    return profileKeyOrder.zip(values).toMap()
}

private val profileBundles: Map<String, Map<String, String>> = mapOf(
    "en" to profile(
        "Profile photo", "Shown next to your name on Home", "Tap to choose another photo",
        "Remove profile photo", "Couldn't use this photo"
    ),
    "it" to profile(
        "Foto profilo", "Appare accanto al tuo nome nella Home", "Tocca per scegliere un'altra foto",
        "Rimuovi foto profilo", "Impossibile usare questa foto"
    ),
    "es" to profile(
        "Foto de perfil", "Aparece junto a tu nombre en Inicio", "Toca para elegir otra foto",
        "Quitar foto de perfil", "No se pudo usar esta foto"
    ),
    "fr" to profile(
        "Photo de profil", "Affichée à côté de ton nom sur l'accueil", "Touche pour choisir une autre photo",
        "Supprimer la photo de profil", "Impossible d'utiliser cette photo"
    ),
    "de" to profile(
        "Profilbild", "Wird auf der Startseite neben deinem Namen angezeigt", "Tippe, um ein anderes Foto zu wählen",
        "Profilbild entfernen", "Dieses Foto kann nicht verwendet werden"
    ),
    "pt" to profile(
        "Foto de perfil", "Aparece junto ao teu nome no Início", "Toca para escolher outra foto",
        "Remover foto de perfil", "Não foi possível usar esta foto"
    ),
    "nl" to profile(
        "Profielfoto", "Wordt naast je naam op Home getoond", "Tik om een andere foto te kiezen",
        "Profielfoto verwijderen", "Deze foto kan niet worden gebruikt"
    ),
    "pl" to profile(
        "Zdjęcie profilowe", "Widoczne obok Twojego imienia na stronie głównej", "Dotknij, aby wybrać inne zdjęcie",
        "Usuń zdjęcie profilowe", "Nie można użyć tego zdjęcia"
    ),
    "ro" to profile(
        "Fotografie de profil", "Apare lângă numele tău pe pagina principală", "Atinge pentru a alege altă fotografie",
        "Elimină fotografia de profil", "Fotografia nu a putut fi folosită"
    ),
    "el" to profile(
        "Φωτογραφία προφίλ", "Εμφανίζεται δίπλα στο όνομά σου στην Αρχική", "Πάτησε για να διαλέξεις άλλη φωτογραφία",
        "Αφαίρεση φωτογραφίας προφίλ", "Δεν ήταν δυνατή η χρήση αυτής της φωτογραφίας"
    ),
    "sv" to profile(
        "Profilbild", "Visas bredvid ditt namn på Hem", "Tryck för att välja en annan bild",
        "Ta bort profilbild", "Det gick inte att använda bilden"
    ),
    "da" to profile(
        "Profilbillede", "Vises ved siden af dit navn på Hjem", "Tryk for at vælge et andet billede",
        "Fjern profilbillede", "Billedet kunne ikke bruges"
    ),
    "cs" to profile(
        "Profilová fotka", "Zobrazí se vedle tvého jména na domovské stránce", "Klepnutím vyber jinou fotku",
        "Odebrat profilovou fotku", "Tuto fotku nelze použít"
    ),
    "sk" to profile(
        "Profilová fotka", "Zobrazí sa vedľa tvojho mena na domovskej stránke", "Ťuknutím vyber inú fotku",
        "Odstrániť profilovú fotku", "Túto fotku nemožno použiť"
    ),
    "hr" to profile(
        "Profilna fotografija", "Prikazuje se uz tvoje ime na početnoj", "Dodirni za odabir druge fotografije",
        "Ukloni profilnu fotografiju", "Ova se fotografija ne može koristiti"
    ),
    "bg" to profile(
        "Профилна снимка", "Показва се до името ти в началото", "Докосни, за да избереш друга снимка",
        "Премахни профилната снимка", "Тази снимка не може да се използва"
    ),
    "hu" to profile(
        "Profilkép", "A kezdőlapon a neved mellett jelenik meg", "Koppints egy másik kép kiválasztásához",
        "Profilkép eltávolítása", "Ez a kép nem használható"
    ),
    "fi" to profile(
        "Profiilikuva", "Näkyy nimesi vieressä etusivulla", "Valitse toinen kuva napauttamalla",
        "Poista profiilikuva", "Kuvaa ei voitu käyttää"
    ),
    "et" to profile(
        "Profiilipilt", "Kuvatakse avalehel sinu nime kõrval", "Puuduta, et valida teine pilt",
        "Eemalda profiilipilt", "Seda pilti ei saanud kasutada"
    ),
    "nb" to profile(
        "Profilbilde", "Vises ved siden av navnet ditt på Hjem", "Trykk for å velge et annet bilde",
        "Fjern profilbilde", "Kunne ikke bruke dette bildet"
    ),
    "ca" to profile(
        "Foto de perfil", "Apareix al costat del teu nom a l'Inici", "Toca per triar una altra foto",
        "Elimina la foto de perfil", "No s'ha pogut fer servir aquesta foto"
    ),
    "uk" to profile(
        "Фото профілю", "Показується поруч із твоїм іменем на головній", "Торкнися, щоб вибрати інше фото",
        "Видалити фото профілю", "Не вдалося використати це фото"
    ),
    "ru" to profile(
        "Фото профиля", "Показывается рядом с твоим именем на главной", "Нажми, чтобы выбрать другое фото",
        "Удалить фото профиля", "Не удалось использовать это фото"
    ),
    "tr" to profile(
        "Profil fotoğrafı", "Ana sayfada adının yanında gösterilir", "Başka bir fotoğraf seçmek için dokun",
        "Profil fotoğrafını kaldır", "Bu fotoğraf kullanılamadı"
    ),
    "ar" to profile(
        "صورة الملف الشخصي", "تظهر بجانب اسمك في الصفحة الرئيسية", "انقر لاختيار صورة أخرى",
        "إزالة صورة الملف الشخصي", "تعذّر استخدام هذه الصورة"
    ),
    "fa" to profile(
        "عکس نمایه", "کنار نام تو در صفحهٔ اصلی نشان داده می‌شود", "برای انتخاب عکس دیگر ضربه بزن",
        "حذف عکس نمایه", "استفاده از این عکس ممکن نبود"
    ),
    "zh" to profile(
        "头像", "显示在首页你的名字旁边", "点按以选择其他照片",
        "移除头像", "无法使用这张照片"
    ),
    "zh-Hant" to profile(
        "大頭貼", "顯示在首頁你的名字旁邊", "輕觸以選擇其他照片",
        "移除大頭貼", "無法使用這張照片"
    ),
    "ja" to profile(
        "プロフィール写真", "ホームで名前の横に表示されます", "タップして別の写真を選択",
        "プロフィール写真を削除", "この写真は使用できません"
    ),
    "ko" to profile(
        "프로필 사진", "홈에서 이름 옆에 표시됩니다", "탭하여 다른 사진 선택",
        "프로필 사진 삭제", "이 사진을 사용할 수 없습니다"
    ),
    "hi" to profile(
        "प्रोफ़ाइल फ़ोटो", "होम पर आपके नाम के पास दिखती है", "दूसरी फ़ोटो चुनने के लिए टैप करें",
        "प्रोफ़ाइल फ़ोटो हटाएँ", "इस फ़ोटो का उपयोग नहीं हो सका"
    ),
    "id" to profile(
        "Foto profil", "Ditampilkan di samping namamu di Beranda", "Ketuk untuk memilih foto lain",
        "Hapus foto profil", "Foto ini tidak dapat digunakan"
    ),
    "ms" to profile(
        "Foto profil", "Dipaparkan di sebelah nama anda di Laman Utama", "Ketik untuk memilih foto lain",
        "Alih keluar foto profil", "Foto ini tidak dapat digunakan"
    ),
    "vi" to profile(
        "Ảnh hồ sơ", "Hiển thị cạnh tên của bạn trên Trang chủ", "Nhấn để chọn ảnh khác",
        "Xóa ảnh hồ sơ", "Không thể dùng ảnh này"
    ),
    "th" to profile(
        "รูปโปรไฟล์", "แสดงข้างชื่อของคุณในหน้าแรก", "แตะเพื่อเลือกรูปอื่น",
        "ลบรูปโปรไฟล์", "ใช้รูปนี้ไม่ได้"
    ),
    "fil" to profile(
        "Larawan sa profile", "Ipinapakita sa tabi ng pangalan mo sa Home", "I-tap para pumili ng ibang larawan",
        "Alisin ang larawan sa profile", "Hindi magamit ang larawang ito"
    ),
    "he" to profile(
        "תמונת פרופיל", "מוצגת ליד השם שלך בדף הבית", "הקש כדי לבחור תמונה אחרת",
        "הסרת תמונת הפרופיל", "לא ניתן להשתמש בתמונה זו"
    )
)

internal fun profileLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(profileBundles, code)
