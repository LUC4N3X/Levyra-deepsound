package com.luc4n3x.levyra.ui.i18n

internal data class PreferLocalAudioCopy(
    val title: String,
    val subtitle: String,
    val permissionNeeded: String
)

internal fun preferLocalAudioCopy(code: String): PreferLocalAudioCopy =
    localizedValueOrEnglish(preferLocalAudioCopies, code).forBrazil(code) { brazilian() }

internal fun preferLocalAudioLocalizationCodes(): Set<String> = preferLocalAudioCopies.keys

private fun PreferLocalAudioCopy.brazilian(): PreferLocalAudioCopy = copy(
    title = LevyraBrazilianPortuguese.adapt(title),
    subtitle = LevyraBrazilianPortuguese.adapt(subtitle),
    permissionNeeded = LevyraBrazilianPortuguese.adapt(permissionNeeded)
)

private val preferLocalAudioCopies: Map<String, PreferLocalAudioCopy> = mapOf(
    "en" to PreferLocalAudioCopy(
        "Prefer local audio",
        "Plays the matching song from this device before streaming it. If unsure, Levyra streams.",
        "Allow access to audio files to use local copies"
    ),
    "it" to PreferLocalAudioCopy(
        "Preferisci audio locale",
        "Riproduce il brano corrispondente dal dispositivo prima dello streaming. Nel dubbio Levyra usa lo streaming.",
        "Consenti l'accesso ai file audio per usare le copie locali"
    ),
    "es" to PreferLocalAudioCopy(
        "Preferir audio local",
        "Reproduce la canción coincidente desde el dispositivo antes de usar streaming. Si hay dudas, Levyra usa streaming.",
        "Permite el acceso a los archivos de audio para usar copias locales"
    ),
    "fr" to PreferLocalAudioCopy(
        "Préférer l'audio local",
        "Lit le titre correspondant depuis l'appareil avant le streaming. En cas de doute, Levyra utilise le streaming.",
        "Autorisez l'accès aux fichiers audio pour utiliser les copies locales"
    ),
    "de" to PreferLocalAudioCopy(
        "Lokale Audiodateien bevorzugen",
        "Spielt den passenden Titel vom Gerät ab, bevor gestreamt wird. Im Zweifel streamt Levyra.",
        "Erlaube den Zugriff auf Audiodateien, um lokale Kopien zu nutzen"
    ),
    "pt" to PreferLocalAudioCopy(
        "Preferir áudio local",
        "Reproduz a música correspondente a partir do dispositivo antes do streaming. Em caso de dúvida, o Levyra usa streaming.",
        "Permite o acesso aos ficheiros de áudio para usar cópias locais"
    ),
    "nl" to PreferLocalAudioCopy(
        "Lokale audio voorrang geven",
        "Speelt het overeenkomende nummer van dit apparaat af vóór streaming. Bij twijfel streamt Levyra.",
        "Geef toegang tot audiobestanden om lokale kopieën te gebruiken"
    ),
    "pl" to PreferLocalAudioCopy(
        "Preferuj lokalne audio",
        "Odtwarza pasujący utwór z urządzenia przed streamingiem. W razie wątpliwości Levyra używa streamingu.",
        "Zezwól na dostęp do plików audio, aby używać lokalnych kopii"
    ),
    "ro" to PreferLocalAudioCopy(
        "Preferă audio local",
        "Redă melodia potrivită de pe dispozitiv înainte de streaming. În caz de îndoială, Levyra folosește streamingul.",
        "Permite accesul la fișierele audio pentru a folosi copiile locale"
    ),
    "el" to PreferLocalAudioCopy(
        "Προτίμηση τοπικού ήχου",
        "Παίζει το αντίστοιχο τραγούδι από τη συσκευή πριν από τη ροή. Αν υπάρχει αμφιβολία, το Levyra κάνει ροή.",
        "Επιτρέψτε την πρόσβαση στα αρχεία ήχου για χρήση τοπικών αντιγράφων"
    ),
    "sv" to PreferLocalAudioCopy(
        "Föredra lokalt ljud",
        "Spelar den matchande låten från enheten innan den streamas. Vid osäkerhet streamar Levyra.",
        "Ge åtkomst till ljudfiler för att använda lokala kopior"
    ),
    "da" to PreferLocalAudioCopy(
        "Foretræk lokal lyd",
        "Afspiller det matchende nummer fra enheden før streaming. Ved tvivl streamer Levyra.",
        "Giv adgang til lydfiler for at bruge lokale kopier"
    ),
    "cs" to PreferLocalAudioCopy(
        "Upřednostnit místní zvuk",
        "Přehraje odpovídající skladbu ze zařízení před streamováním. V případě pochybností Levyra streamuje.",
        "Povolte přístup ke zvukovým souborům pro použití místních kopií"
    ),
    "sk" to PreferLocalAudioCopy(
        "Uprednostniť lokálny zvuk",
        "Prehrá zodpovedajúcu skladbu zo zariadenia pred streamovaním. Pri pochybnostiach Levyra streamuje.",
        "Povoľte prístup k zvukovým súborom na použitie lokálnych kópií"
    ),
    "hr" to PreferLocalAudioCopy(
        "Preferiraj lokalni zvuk",
        "Reproducira odgovarajuću pjesmu s uređaja prije streaminga. U slučaju sumnje Levyra koristi streaming.",
        "Dopusti pristup audiodatotekama za korištenje lokalnih kopija"
    ),
    "sr" to PreferLocalAudioCopy(
        "Дај предност локалном звуку",
        "Пушта одговарајућу песму са уређаја пре стриминга. Ако није сигурна, Levyra користи стриминг.",
        "Дозволи приступ аудио датотекама да би се користиле локалне копије"
    ),
    "sq" to PreferLocalAudioCopy(
        "Prefero audion lokale",
        "Luan këngën përkatëse nga pajisja përpara transmetimit. Në rast dyshimi, Levyra transmeton.",
        "Lejo qasjen te skedarët audio për të përdorur kopjet lokale"
    ),
    "bg" to PreferLocalAudioCopy(
        "Предпочитай локално аудио",
        "Пуска съответната песен от устройството преди стрийминг. При съмнение Levyra използва стрийминг.",
        "Разреши достъп до аудиофайловете, за да се използват локални копия"
    ),
    "hu" to PreferLocalAudioCopy(
        "Helyi hang előnyben részesítése",
        "Streamelés előtt az eszközön lévő egyező dalt játssza le. Kétség esetén a Levyra streamel.",
        "Engedélyezd a hangfájlok elérését a helyi másolatok használatához"
    ),
    "fi" to PreferLocalAudioCopy(
        "Suosi paikallista ääntä",
        "Toistaa vastaavan kappaleen laitteelta ennen suoratoistoa. Epävarmoissa tapauksissa Levyra suoratoistaa.",
        "Salli pääsy äänitiedostoihin paikallisten kopioiden käyttämiseksi"
    ),
    "et" to PreferLocalAudioCopy(
        "Eelista kohalikku heli",
        "Esitab vastava loo seadmest enne voogedastust. Kahtluse korral kasutab Levyra voogedastust.",
        "Luba juurdepääs helifailidele, et kasutada kohalikke koopiaid"
    ),
    "nb" to PreferLocalAudioCopy(
        "Foretrekk lokal lyd",
        "Spiller av den samsvarende sangen fra enheten før strømming. Ved tvil strømmer Levyra.",
        "Gi tilgang til lydfiler for å bruke lokale kopier"
    ),
    "ca" to PreferLocalAudioCopy(
        "Prefereix l'àudio local",
        "Reprodueix la cançó coincident des del dispositiu abans del streaming. En cas de dubte, Levyra fa streaming.",
        "Permet l'accés als fitxers d'àudio per fer servir còpies locals"
    ),
    "uk" to PreferLocalAudioCopy(
        "Віддавати перевагу локальному аудіо",
        "Відтворює відповідну пісню з пристрою перед стримінгом. Якщо є сумнів, Levyra використовує стримінг.",
        "Дозвольте доступ до аудіофайлів, щоб використовувати локальні копії"
    ),
    "ru" to PreferLocalAudioCopy(
        "Предпочитать локальное аудио",
        "Воспроизводит совпадающую песню с устройства перед стримингом. При сомнении Levyra использует стриминг.",
        "Разрешите доступ к аудиофайлам, чтобы использовать локальные копии"
    ),
    "kk" to PreferLocalAudioCopy(
        "Жергілікті аудиоға басымдық беру",
        "Стримингтен бұрын құрылғыдағы сәйкес әнді ойнатады. Күмән болса, Levyra стримингті қолданады.",
        "Жергілікті көшірмелерді пайдалану үшін аудиофайлдарға рұқсат беріңіз"
    ),
    "tr" to PreferLocalAudioCopy(
        "Yerel sesi tercih et",
        "Akıştan önce cihazdaki eşleşen şarkıyı çalar. Emin olunamazsa Levyra akışı kullanır.",
        "Yerel kopyaları kullanmak için ses dosyalarına erişim izni ver"
    ),
    "ar" to PreferLocalAudioCopy(
        "تفضيل الصوت المحلي",
        "يشغّل الأغنية المطابقة من الجهاز قبل البث. عند الشك يستخدم Levyra البث.",
        "اسمح بالوصول إلى الملفات الصوتية لاستخدام النسخ المحلية"
    ),
    "fa" to PreferLocalAudioCopy(
        "ترجیح صدای محلی",
        "پیش از پخش آنلاین، آهنگ مطابق را از دستگاه پخش می‌کند. در صورت تردید، Levyra پخش آنلاین را استفاده می‌کند.",
        "برای استفاده از نسخه‌های محلی، دسترسی به فایل‌های صوتی را مجاز کنید"
    ),
    "zh" to PreferLocalAudioCopy(
        "优先使用本地音频",
        "在流式播放前先播放设备上匹配的歌曲。无法确定时，Levyra 会使用流式播放。",
        "允许访问音频文件以使用本地副本"
    ),
    "zh-Hant" to PreferLocalAudioCopy(
        "優先使用本機音訊",
        "在串流播放前先播放裝置上相符的歌曲。無法確定時，Levyra 會使用串流播放。",
        "允許存取音訊檔案以使用本機副本"
    ),
    "ja" to PreferLocalAudioCopy(
        "ローカル音源を優先",
        "ストリーミングの前に端末内の一致する曲を再生します。確信が持てない場合はストリーミングを使います。",
        "ローカルのコピーを使うには音声ファイルへのアクセスを許可してください"
    ),
    "ko" to PreferLocalAudioCopy(
        "로컬 오디오 우선",
        "스트리밍 전에 기기에 있는 일치하는 곡을 재생합니다. 확실하지 않으면 Levyra가 스트리밍합니다.",
        "로컬 사본을 사용하려면 오디오 파일 접근을 허용하세요"
    ),
    "hi" to PreferLocalAudioCopy(
        "स्थानीय ऑडियो को प्राथमिकता दें",
        "स्ट्रीमिंग से पहले डिवाइस पर मेल खाता गाना चलाता है। संदेह होने पर Levyra स्ट्रीम करता है।",
        "स्थानीय कॉपी इस्तेमाल करने के लिए ऑडियो फ़ाइलों की अनुमति दें"
    ),
    "bn" to PreferLocalAudioCopy(
        "লোকাল অডিওকে অগ্রাধিকার দিন",
        "স্ট্রিমিংয়ের আগে ডিভাইসে থাকা মিল থাকা গানটি চালায়। সন্দেহ হলে Levyra স্ট্রিম করে।",
        "লোকাল কপি ব্যবহার করতে অডিও ফাইলে অ্যাক্সেস দিন"
    ),
    "id" to PreferLocalAudioCopy(
        "Utamakan audio lokal",
        "Memutar lagu yang cocok dari perangkat sebelum streaming. Jika ragu, Levyra melakukan streaming.",
        "Izinkan akses ke file audio untuk memakai salinan lokal"
    ),
    "ms" to PreferLocalAudioCopy(
        "Utamakan audio setempat",
        "Memainkan lagu yang sepadan daripada peranti sebelum penstriman. Jika ragu, Levyra menstrim.",
        "Benarkan akses kepada fail audio untuk menggunakan salinan setempat"
    ),
    "vi" to PreferLocalAudioCopy(
        "Ưu tiên âm thanh cục bộ",
        "Phát bài hát khớp trên thiết bị trước khi phát trực tuyến. Khi không chắc chắn, Levyra sẽ phát trực tuyến.",
        "Cho phép truy cập tệp âm thanh để dùng bản sao cục bộ"
    ),
    "th" to PreferLocalAudioCopy(
        "ใช้เสียงในเครื่องก่อน",
        "เล่นเพลงที่ตรงกันจากอุปกรณ์ก่อนสตรีม หากไม่แน่ใจ Levyra จะสตรีมแทน",
        "อนุญาตให้เข้าถึงไฟล์เสียงเพื่อใช้สำเนาในเครื่อง"
    ),
    "fil" to PreferLocalAudioCopy(
        "Unahin ang lokal na audio",
        "Pinapatugtog muna ang katugmang kanta mula sa device bago mag-stream. Kapag hindi sigurado, nag-i-stream ang Levyra.",
        "Payagan ang access sa mga audio file para magamit ang mga lokal na kopya"
    ),
    "he" to PreferLocalAudioCopy(
        "העדפת אודיו מקומי",
        "מנגן את השיר התואם מהמכשיר לפני הזרמה. במקרה של ספק, Levyra מזרים.",
        "אפשרו גישה לקובצי אודיו כדי להשתמש בעותקים מקומיים"
    )
)
