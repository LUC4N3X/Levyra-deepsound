package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal data class SkinCopy(val title: String, val defaultBody: String, val nocturneBody: String)

internal object LevyraSkinStrings {
    private val copies = mapOf(
        "en" to SkinCopy("Skin", "Levyra's original interface, with your personal settings.", "AMOLED black, graphite and amber. Inspired by vintage hi-fi."),
        "it" to SkinCopy("Skin", "L'interfaccia originale di Levyra, con le tue personalizzazioni.", "Nero AMOLED, grafite e ambra. Ispirata agli impianti hi-fi vintage."),
        "es" to SkinCopy("Skin", "La interfaz original de Levyra, con tus ajustes personales.", "Negro AMOLED, grafito y ámbar. Inspirada en equipos de alta fidelidad clásicos."),
        "fr" to SkinCopy("Habillage", "L'interface originale de Levyra, avec vos réglages personnels.", "Noir AMOLED, graphite et ambre. Inspiré de la hi-fi vintage."),
        "de" to SkinCopy("Skin", "Die ursprüngliche Levyra-Oberfläche mit deinen persönlichen Einstellungen.", "AMOLED-Schwarz, Graphit und Bernstein. Inspiriert von klassischer Hi-Fi-Technik."),
        "pt" to SkinCopy("Skin", "A interface original do Levyra, com as tuas personalizações.", "Preto AMOLED, grafite e âmbar. Inspirada em sistemas de alta fidelidade vintage."),
        "nl" to SkinCopy("Skin", "De originele Levyra-interface met je persoonlijke instellingen.", "AMOLED-zwart, grafiet en amber. Geïnspireerd op vintage hifi."),
        "pl" to SkinCopy("Skórka", "Oryginalny interfejs Levyra z Twoimi ustawieniami.", "Czerń AMOLED, grafit i bursztyn. Inspiracja klasycznym sprzętem hi-fi."),
        "ru" to SkinCopy("Обложка", "Оригинальный интерфейс Levyra с вашими настройками.", "Чёрный AMOLED, графит и янтарь. Вдохновение — винтажная hi-fi техника."),
        "uk" to SkinCopy("Оболонка", "Оригінальний інтерфейс Levyra з вашими налаштуваннями.", "Чорний AMOLED, графіт і бурштин. Натхнення — вінтажна hi-fi техніка."),
        "ja" to SkinCopy("スキン", "個人設定を反映した Levyra 本来のインターフェース。", "AMOLED の黒、グラファイト、アンバー。ヴィンテージ Hi-Fi をイメージ。"),
        "ko" to SkinCopy("스킨", "개인 설정이 적용된 Levyra의 기본 인터페이스.", "AMOLED 블랙, 그래파이트, 앰버. 빈티지 하이파이에서 영감을 받았습니다."),
        "zh" to SkinCopy("界面风格", "保留个人设置的 Levyra 原始界面。", "AMOLED 黑色、石墨灰和琥珀色，灵感源于复古 Hi-Fi 音响。"),
        "zh-Hant" to SkinCopy("介面風格", "保留個人設定的 Levyra 原始介面。", "AMOLED 黑色、石墨灰和琥珀色，靈感源於復古 Hi-Fi 音響。"),
        "ar" to SkinCopy("المظهر", "واجهة Levyra الأصلية مع إعداداتك الشخصية.", "أسود AMOLED وغرافيت وكهرماني. مستوحى من أجهزة الصوت الكلاسيكية."),
        "ro" to SkinCopy("Aspect", "Interfața originală Levyra, cu setările tale.", "Negru AMOLED, grafit și chihlimbar. Inspirat de sistemele hi-fi vintage."),
        "sq" to SkinCopy("Pamja", "Ndërfaqja origjinale e Levyra me cilësimet e tua.", "E zezë AMOLED, grafit dhe qelibar. Frymëzuar nga pajisjet klasike hi-fi."),
        "bg" to SkinCopy("Облик", "Оригиналният интерфейс на Levyra с личните ти настройки.", "AMOLED черно, графит и кехлибар. Вдъхновен от класическите hi-fi системи."),
        "el" to SkinCopy("Εμφάνιση", "Η αρχική διεπαφή του Levyra, με τις ρυθμίσεις σου.", "Μαύρο AMOLED, γραφίτης και κεχριμπάρι. Έμπνευση από κλασικά συστήματα hi-fi."),
        "sv" to SkinCopy("Utseende", "Levyras ursprungliga gränssnitt med dina inställningar.", "AMOLED-svart, grafit och bärnsten. Inspirerat av klassisk hi-fi."),
        "da" to SkinCopy("Udseende", "Levyras oprindelige grænseflade med dine indstillinger.", "AMOLED-sort, grafit og rav. Inspireret af klassisk hi-fi."),
        "cs" to SkinCopy("Vzhled", "Původní rozhraní Levyra s tvým nastavením.", "Černá AMOLED, grafit a jantar. Inspirace klasickými hi-fi sestavami."),
        "sk" to SkinCopy("Vzhľad", "Pôvodné rozhranie Levyra s tvojimi nastaveniami.", "Čierna AMOLED, grafit a jantár. Inšpirované klasickými hi-fi zostavami."),
        "bn" to SkinCopy("স্কিন", "আপনার ব্যক্তিগত সেটিংসসহ Levyra-এর মূল ইন্টারফেস।", "AMOLED কালো, গ্রাফাইট ও অ্যাম্বার। পুরোনো হাই-ফাই যন্ত্র থেকে অনুপ্রাণিত।"),
        "kk" to SkinCopy("Безендіру", "Жеке параметрлеріңіз сақталған Levyra-ның бастапқы интерфейсі.", "AMOLED қара, графит және кәріптас. Көне hi-fi жүйелерінен шабыт алған."),
        "sr" to SkinCopy("Изглед", "Оригинални Levyra интерфејс са твојим подешавањима.", "AMOLED црна, графит и ћилибар. Инспирисано старим hi-fi уређајима."),
        "hr" to SkinCopy("Izgled", "Izvorno sučelje Levyra s tvojim postavkama.", "AMOLED crna, grafit i jantar. Nadahnuto klasičnim hi-fi sustavima."),
        "hu" to SkinCopy("Megjelenés", "A Levyra eredeti felülete a személyes beállításaiddal.", "AMOLED-fekete, grafit és borostyán. Klasszikus hi-fi készülékek ihlették."),
        "fi" to SkinCopy("Ulkoasu", "Levyran alkuperäinen käyttöliittymä omilla asetuksillasi.", "AMOLED-musta, grafiitti ja meripihka. Vanhojen hifilaitteiden inspiroima."),
        "et" to SkinCopy("Välimus", "Levyra algne liides sinu isiklike seadetega.", "AMOLED-must, grafiit ja merevaik. Inspireeritud vanadest hi-fi seadmetest."),
        "nb" to SkinCopy("Utseende", "Levyras opprinnelige grensesnitt med dine innstillinger.", "AMOLED-svart, grafitt og rav. Inspirert av klassisk hi-fi."),
        "ca" to SkinCopy("Aparença", "La interfície original de Levyra, amb els teus ajustos.", "Negre AMOLED, grafit i ambre. Inspirada en equips d'alta fidelitat clàssics."),
        "tr" to SkinCopy("Görünüm", "Kişisel ayarlarınla Levyra'nın özgün arayüzü.", "AMOLED siyah, grafit ve kehribar. Klasik hi-fi sistemlerinden esinlenildi."),
        "fa" to SkinCopy("پوسته", "رابط اصلی Levyra با تنظیمات شخصی شما.", "مشکی AMOLED، گرافیت و کهربایی. الهام‌گرفته از سیستم‌های صوتی قدیمی."),
        "hi" to SkinCopy("स्किन", "आपकी निजी सेटिंग्स के साथ Levyra का मूल इंटरफ़ेस।", "AMOLED काला, ग्रेफाइट और एम्बर। पुराने हाई-फाई उपकरणों से प्रेरित।"),
        "id" to SkinCopy("Tampilan", "Antarmuka asli Levyra dengan pengaturan pribadimu.", "Hitam AMOLED, grafit, dan amber. Terinspirasi sistem hi-fi klasik."),
        "ms" to SkinCopy("Rupa", "Antara muka asal Levyra dengan tetapan peribadi anda.", "Hitam AMOLED, grafit dan ambar. Diilhamkan oleh sistem hi-fi klasik."),
        "vi" to SkinCopy("Giao diện", "Giao diện gốc của Levyra với cài đặt cá nhân của bạn.", "Đen AMOLED, xám than và hổ phách. Lấy cảm hứng từ dàn hi-fi cổ điển."),
        "th" to SkinCopy("สกิน", "อินเทอร์เฟซดั้งเดิมของ Levyra พร้อมการตั้งค่าส่วนตัวของคุณ", "ดำ AMOLED กราไฟต์ และอำพัน ได้แรงบันดาลใจจากเครื่องเสียงไฮไฟวินเทจ"),
        "fil" to SkinCopy("Hitsura", "Ang orihinal na interface ng Levyra kasama ang personal mong mga setting.", "AMOLED na itim, graphite at amber. Inspirado ng klasikong hi-fi."),
        "he" to SkinCopy("עיצוב", "הממשק המקורי של Levyra עם ההגדרות האישיות שלך.", "שחור AMOLED, גרפיט וענבר. בהשראת מערכות Hi-Fi קלאסיות.")
    )

    fun forCode(code: String): SkinCopy = copies[LevyraLanguageCatalog.normalize(code)] ?: copies.getValue("en")

    fun managedForCode(code: String, skin: String = "Nocturne 78"): String =
        (managed[LevyraLanguageCatalog.normalize(code)] ?: managed.getValue("en")).replace("Nocturne 78", skin)

    private val managed = mapOf(
        "en" to "Nocturne 78 manages the appearance. Switch to Levyra Default to change it. Your previous choices are saved.",
        "it" to "L'aspetto è gestito da Nocturne 78. Per modificarlo, passa a Levyra Default. Le tue scelte precedenti sono conservate.",
        "es" to "Nocturne 78 controla la apariencia. Cambia a Levyra Default para modificarla. Tus ajustes anteriores se conservan.",
        "fr" to "Nocturne 78 gère l'apparence. Passez à Levyra Default pour la modifier. Vos choix précédents sont conservés.",
        "de" to "Nocturne 78 bestimmt das Aussehen. Wechsle zu Levyra Default, um es anzupassen. Deine bisherigen Einstellungen bleiben gespeichert.",
        "pt" to "Nocturne 78 controla o aspeto. Muda para Levyra Default para o personalizares. As escolhas anteriores ficam guardadas.",
        "nl" to "Nocturne 78 bepaalt het uiterlijk. Schakel naar Levyra Default om dit te wijzigen. Je eerdere keuzes blijven bewaard.",
        "pl" to "Nocturne 78 ustala wygląd. Przełącz na Levyra Default, aby go zmienić. Poprzednie ustawienia są zachowane.",
        "ro" to "Nocturne 78 controlează aspectul. Treci la Levyra Default pentru a-l schimba. Alegerile anterioare sunt păstrate.",
        "el" to "Το Nocturne 78 ορίζει την εμφάνιση. Επιλέξτε Levyra Default για αλλαγές. Οι προηγούμενες επιλογές σας διατηρούνται.",
        "sv" to "Nocturne 78 styr utseendet. Byt till Levyra Default för att ändra det. Dina tidigare val sparas.",
        "da" to "Nocturne 78 styrer udseendet. Skift til Levyra Default for at ændre det. Dine tidligere valg er gemt.",
        "cs" to "Vzhled řídí Nocturne 78. Pro změny přepni na Levyra Default. Předchozí volby zůstávají uložené.",
        "sk" to "Vzhľad riadi Nocturne 78. Pre zmeny prejdi na Levyra Default. Predchádzajúce voľby zostávajú uložené.",
        "hr" to "Nocturne 78 određuje izgled. Prijeđi na Levyra Default za promjene. Prethodni odabiri ostaju spremljeni.",
        "sr" to "Nocturne 78 одређује изглед. Пређи на Levyra Default за измене. Претходни избори остају сачувани.",
        "sq" to "Nocturne 78 përcakton pamjen. Kalo te Levyra Default për ta ndryshuar. Zgjedhjet e mëparshme ruhen.",
        "bg" to "Nocturne 78 определя облика. Премини към Levyra Default за промени. Предишните избори се запазват.",
        "hu" to "A megjelenést a Nocturne 78 kezeli. Válts Levyra Default módra a módosításhoz. A korábbi beállításaid megmaradnak.",
        "fi" to "Nocturne 78 määrittää ulkoasun. Vaihda Levyra Defaultiin muokataksesi sitä. Aiemmat valintasi säilyvät.",
        "et" to "Nocturne 78 määrab välimuse. Muutmiseks vali Levyra Default. Varasemad valikud säilivad.",
        "nb" to "Nocturne 78 styrer utseendet. Bytt til Levyra Default for å endre det. Dine tidligere valg beholdes.",
        "ca" to "Nocturne 78 controla l'aparença. Canvia a Levyra Default per modificar-la. Les tries anteriors es conserven.",
        "uk" to "Nocturne 78 визначає вигляд. Перейдіть на Levyra Default для змін. Попередні налаштування збережено.",
        "ru" to "Nocturne 78 определяет оформление. Переключитесь на Levyra Default для изменений. Прежние настройки сохранены.",
        "kk" to "Көріністі Nocturne 78 басқарады. Өзгерту үшін Levyra Default таңдаңыз. Бұрынғы таңдаулар сақталады.",
        "tr" to "Görünümü Nocturne 78 yönetir. Değiştirmek için Levyra Default'a geç. Önceki seçimlerin saklanır.",
        "ar" to "يتحكم Nocturne 78 بالمظهر. انتقل إلى Levyra Default لتغييره. تبقى اختياراتك السابقة محفوظة.",
        "fa" to "ظاهر توسط Nocturne 78 مدیریت می‌شود. برای تغییر، Levyra Default را انتخاب کنید. انتخاب‌های قبلی شما حفظ می‌شوند.",
        "zh" to "外观由 Nocturne 78 管理。切换至 Levyra Default 即可修改。之前的设置会保留。",
        "zh-Hant" to "外觀由 Nocturne 78 管理。切換至 Levyra Default 即可修改。先前的設定會保留。",
        "ja" to "外観は Nocturne 78 が管理します。変更するには Levyra Default に切り替えてください。以前の設定は保持されます。",
        "ko" to "Nocturne 78이 외관을 관리합니다. 변경하려면 Levyra Default로 전환하세요. 이전 선택은 보존됩니다.",
        "hi" to "रूप Nocturne 78 द्वारा नियंत्रित है। बदलने के लिए Levyra Default चुनें। आपकी पिछली सेटिंग्स सुरक्षित हैं।",
        "bn" to "দেখতে কেমন হবে তা Nocturne 78 নির্ধারণ করে। পরিবর্তনের জন্য Levyra Default বেছে নিন। আগের পছন্দগুলো সংরক্ষিত আছে।",
        "id" to "Tampilan diatur oleh Nocturne 78. Beralih ke Levyra Default untuk mengubahnya. Pilihan sebelumnya tetap tersimpan.",
        "ms" to "Rupa diuruskan oleh Nocturne 78. Tukar kepada Levyra Default untuk mengubahnya. Pilihan terdahulu disimpan.",
        "vi" to "Nocturne 78 quản lý giao diện. Chuyển sang Levyra Default để thay đổi. Các lựa chọn trước đó được giữ lại.",
        "th" to "Nocturne 78 จัดการรูปลักษณ์ สลับเป็น Levyra Default เพื่อเปลี่ยนแปลง การตั้งค่าเดิมของคุณจะถูกเก็บไว้",
        "fil" to "Nocturne 78 ang namamahala sa hitsura. Lumipat sa Levyra Default para baguhin ito. Naka-save ang mga dati mong pinili.",
        "he" to "המראה מנוהל על ידי Nocturne 78. לשינוי, עבור אל Levyra Default. הבחירות הקודמות שלך נשמרות."
    )
}
