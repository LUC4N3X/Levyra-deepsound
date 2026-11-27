package com.luc4n3x.levyra.ui.i18n

data class ArtistCatalogCopy(
    val playAllSongs: String,
    val downloadAllSongs: String,
    val cancelDownload: String,
    val cancelDownloadTitle: String,
    val cancelDownloadBody: String,
    val keepDownloading: String,
    val downloadStartedHint: String
)

private val artistCatalogCopyValues: Map<String, List<String>> = mapOf(
    "en" to listOf(
        "Play all songs",
        "Download all songs",
        "Cancel download",
        "Cancel the download?",
        "Songs already downloaded stay on this device. Queued songs won't be downloaded.",
        "Keep downloading",
        "Download started. Tap download again to cancel it"
    ),
    "it" to listOf(
        "Riproduci tutti i brani",
        "Scarica tutti i brani",
        "Annulla download",
        "Annullare il download?",
        "I brani già scaricati restano sul dispositivo. Quelli in coda non verranno scaricati.",
        "Continua",
        "Download avviato. Tocca di nuovo Scarica per annullarlo"
    ),
    "es" to listOf(
        "Reproducir todas las canciones",
        "Descargar todas las canciones",
        "Cancelar descarga",
        "¿Cancelar la descarga?",
        "Las canciones ya descargadas se quedan en el dispositivo. Las que están en cola no se descargarán.",
        "Continuar",
        "Descarga iniciada. Toca Descargar otra vez para cancelarla"
    ),
    "fr" to listOf(
        "Écouter tous les titres",
        "Télécharger tous les titres",
        "Annuler le téléchargement",
        "Annuler le téléchargement ?",
        "Les titres déjà téléchargés restent sur l’appareil. Ceux en file d’attente ne seront pas téléchargés.",
        "Continuer",
        "Téléchargement lancé. Touchez à nouveau Télécharger pour l’annuler"
    ),
    "de" to listOf(
        "Alle Songs abspielen",
        "Alle Songs herunterladen",
        "Download abbrechen",
        "Download abbrechen?",
        "Bereits geladene Titel bleiben auf dem Gerät. Titel in der Warteschlange werden nicht geladen.",
        "Weiter laden",
        "Download gestartet. Tippe erneut auf Herunterladen, um ihn abzubrechen"
    ),
    "pt" to listOf(
        "Tocar todas as músicas",
        "Baixar todas as músicas",
        "Cancelar download",
        "Cancelar o download?",
        "As faixas já baixadas continuam no dispositivo. As que estão na fila não serão baixadas.",
        "Continuar",
        "Download iniciado. Toque em Baixar de novo para cancelar"
    ),
    "nl" to listOf(
        "Alle nummers afspelen",
        "Alle nummers downloaden",
        "Download annuleren",
        "Download annuleren?",
        "Al gedownloade nummers blijven op dit apparaat. Nummers in de wachtrij worden niet gedownload.",
        "Doorgaan",
        "Download gestart. Tik nogmaals op downloaden om te annuleren"
    ),
    "pl" to listOf(
        "Odtwórz wszystkie utwory",
        "Pobierz wszystkie utwory",
        "Anuluj pobieranie",
        "Anulować pobieranie?",
        "Pobrane utwory zostaną na urządzeniu. Utwory w kolejce nie zostaną pobrane.",
        "Kontynuuj",
        "Pobieranie rozpoczęte. Dotknij Pobierz ponownie, aby anulować"
    ),
    "ro" to listOf(
        "Redă toate melodiile",
        "Descarcă toate melodiile",
        "Anulează descărcarea",
        "Anulezi descărcarea?",
        "Melodiile deja descărcate rămân pe dispozitiv. Cele din coadă nu vor fi descărcate.",
        "Continuă",
        "Descărcarea a început. Atinge din nou Descarcă pentru a o anula"
    ),
    "el" to listOf(
        "Αναπαραγωγή όλων των τραγουδιών",
        "Λήψη όλων των τραγουδιών",
        "Ακύρωση λήψης",
        "Ακύρωση της λήψης;",
        "Όσα τραγούδια έχουν ήδη ληφθεί μένουν στη συσκευή. Όσα είναι σε αναμονή δεν θα ληφθούν.",
        "Συνέχεια",
        "Η λήψη ξεκίνησε. Πάτησε ξανά Λήψη για ακύρωση"
    ),
    "sv" to listOf(
        "Spela alla låtar",
        "Ladda ned alla låtar",
        "Avbryt nedladdning",
        "Avbryta nedladdningen?",
        "Låtar som redan laddats ned finns kvar på enheten. Låtar i kön laddas inte ned.",
        "Fortsätt",
        "Nedladdningen har startat. Tryck på Ladda ned igen för att avbryta"
    ),
    "da" to listOf(
        "Afspil alle sange",
        "Download alle sange",
        "Annuller download",
        "Vil du annullere downloaden?",
        "Sange, der allerede er downloadet, bliver på enheden. Sange i køen bliver ikke downloadet.",
        "Fortsæt",
        "Downloaden er startet. Tryk på Download igen for at annullere"
    ),
    "cs" to listOf(
        "Přehrát všechny skladby",
        "Stáhnout všechny skladby",
        "Zrušit stahování",
        "Zrušit stahování?",
        "Již stažené skladby zůstanou v zařízení. Skladby ve frontě se nestáhnou.",
        "Pokračovat",
        "Stahování začalo. Dalším klepnutím na Stáhnout ho zrušíte"
    ),
    "sk" to listOf(
        "Prehrať všetky skladby",
        "Stiahnuť všetky skladby",
        "Zrušiť sťahovanie",
        "Zrušiť sťahovanie?",
        "Už stiahnuté skladby zostanú v zariadení. Skladby v poradí sa nestiahnu.",
        "Pokračovať",
        "Sťahovanie sa začalo. Ďalším ťuknutím na Stiahnuť ho zrušíte"
    ),
    "hr" to listOf(
        "Reproduciraj sve pjesme",
        "Preuzmi sve pjesme",
        "Otkaži preuzimanje",
        "Otkazati preuzimanje?",
        "Već preuzete pjesme ostaju na uređaju. Pjesme u redu čekanja neće se preuzeti.",
        "Nastavi",
        "Preuzimanje je počelo. Ponovno dodirni Preuzmi za otkazivanje"
    ),
    "sr" to listOf(
        "Пусти све песме",
        "Преузми све песме",
        "Откажи преузимање",
        "Отказати преузимање?",
        "Већ преузете песме остају на уређају. Песме у реду чекања неће бити преузете.",
        "Настави",
        "Преузимање је почело. Поново додирни Преузми да га откажеш"
    ),
    "sq" to listOf(
        "Luaj të gjitha këngët",
        "Shkarko të gjitha këngët",
        "Anulo shkarkimin",
        "Të anulohet shkarkimi?",
        "Këngët e shkarkuara mbeten në pajisje. Ato në radhë nuk do të shkarkohen.",
        "Vazhdo",
        "Shkarkimi nisi. Prek sërish Shkarko për ta anuluar"
    ),
    "bg" to listOf(
        "Пусни всички песни",
        "Изтегли всички песни",
        "Отказ от изтеглянето",
        "Да се откаже ли изтеглянето?",
        "Вече изтеглените песни остават на устройството. Песните на опашка няма да бъдат изтеглени.",
        "Продължи",
        "Изтеглянето започна. Докосни Изтегли отново, за да го откажеш"
    ),
    "hu" to listOf(
        "Összes dal lejátszása",
        "Összes dal letöltése",
        "Letöltés megszakítása",
        "Megszakítod a letöltést?",
        "A már letöltött dalok az eszközön maradnak. A sorban állók nem töltődnek le.",
        "Folytatás",
        "A letöltés elindult. A megszakításhoz koppints újra a Letöltésre"
    ),
    "fi" to listOf(
        "Toista kaikki kappaleet",
        "Lataa kaikki kappaleet",
        "Peru lataus",
        "Perutaanko lataus?",
        "Jo ladatut kappaleet jäävät laitteelle. Jonossa olevia ei ladata.",
        "Jatka",
        "Lataus alkoi. Peru napauttamalla Lataa uudelleen"
    ),
    "et" to listOf(
        "Esita kõik lood",
        "Laadi kõik lood alla",
        "Tühista allalaadimine",
        "Kas tühistada allalaadimine?",
        "Juba alla laaditud lood jäävad seadmesse. Järjekorras olevaid ei laadita alla.",
        "Jätka",
        "Allalaadimine algas. Tühistamiseks puuduta uuesti Laadi alla"
    ),
    "nb" to listOf(
        "Spill av alle sanger",
        "Last ned alle sanger",
        "Avbryt nedlasting",
        "Vil du avbryte nedlastingen?",
        "Sanger som allerede er lastet ned, blir værende på enheten. Sanger i køen lastes ikke ned.",
        "Fortsett",
        "Nedlastingen har startet. Trykk på Last ned igjen for å avbryte"
    ),
    "ca" to listOf(
        "Reprodueix totes les cançons",
        "Baixa totes les cançons",
        "Cancel·la la baixada",
        "Vols cancel·lar la baixada?",
        "Les cançons ja baixades es queden al dispositiu. Les de la cua no es baixaran.",
        "Continua",
        "La baixada ha començat. Torna a tocar Baixa per cancel·lar-la"
    ),
    "uk" to listOf(
        "Слухати всі пісні",
        "Завантажити всі пісні",
        "Скасувати завантаження",
        "Скасувати завантаження?",
        "Уже завантажені треки залишаться на пристрої. Треки в черзі не завантажуватимуться.",
        "Продовжити",
        "Завантаження почалося. Натисніть «Завантажити» ще раз, щоб скасувати"
    ),
    "ru" to listOf(
        "Слушать все песни",
        "Скачать все песни",
        "Отменить загрузку",
        "Отменить загрузку?",
        "Уже скачанные треки останутся на устройстве. Треки в очереди скачаны не будут.",
        "Продолжить",
        "Загрузка началась. Нажмите «Скачать» ещё раз, чтобы отменить"
    ),
    "kk" to listOf(
        "Барлық әнді ойнату",
        "Барлық әнді жүктеп алу",
        "Жүктеуді тоқтату",
        "Жүктеу тоқтатылсын ба?",
        "Жүктеліп қойған әндер құрылғыда қалады. Кезектегі әндер жүктелмейді.",
        "Жалғастыру",
        "Жүктеу басталды. Тоқтату үшін «Жүктеп алу» түймесін қайта түртіңіз"
    ),
    "tr" to listOf(
        "Tüm şarkıları çal",
        "Tüm şarkıları indir",
        "İndirmeyi iptal et",
        "İndirme iptal edilsin mi?",
        "İndirilmiş şarkılar cihazda kalır. Sıradakiler indirilmez.",
        "Devam et",
        "İndirme başladı. İptal etmek için İndir’e tekrar dokunun"
    ),
    "ar" to listOf(
        "تشغيل كل الأغاني",
        "تنزيل كل الأغاني",
        "إلغاء التنزيل",
        "هل تريد إلغاء التنزيل؟",
        "تبقى الأغاني التي تم تنزيلها على الجهاز. لن يتم تنزيل الأغاني الموجودة في قائمة الانتظار.",
        "متابعة",
        "بدأ التنزيل. اضغط على تنزيل مرة أخرى لإلغائه"
    ),
    "fa" to listOf(
        "پخش همه آهنگ‌ها",
        "دانلود همه آهنگ‌ها",
        "لغو دانلود",
        "دانلود لغو شود؟",
        "آهنگ‌های دانلودشده روی دستگاه می‌مانند. آهنگ‌های در صف دانلود نمی‌شوند.",
        "ادامه",
        "دانلود شروع شد. برای لغو دوباره روی دانلود بزنید"
    ),
    "zh" to listOf(
        "播放全部歌曲",
        "下载全部歌曲",
        "取消下载",
        "要取消下载吗？",
        "已下载的歌曲会保留在此设备上，队列中的歌曲将不会下载。",
        "继续下载",
        "已开始下载。再次点按下载即可取消"
    ),
    "zh-Hant" to listOf(
        "播放所有歌曲",
        "下載所有歌曲",
        "取消下載",
        "要取消下載嗎？",
        "已下載的歌曲會保留在這部裝置上，佇列中的歌曲將不會下載。",
        "繼續下載",
        "已開始下載。再次輕觸下載即可取消"
    ),
    "ja" to listOf(
        "すべての曲を再生",
        "すべての曲をダウンロード",
        "ダウンロードをキャンセル",
        "ダウンロードをキャンセルしますか？",
        "ダウンロード済みの曲はこのデバイスに残ります。待機中の曲はダウンロードされません。",
        "続ける",
        "ダウンロードを開始しました。もう一度ダウンロードをタップするとキャンセルできます"
    ),
    "ko" to listOf(
        "모든 곡 재생",
        "모든 곡 다운로드",
        "다운로드 취소",
        "다운로드를 취소할까요?",
        "이미 다운로드한 곡은 기기에 남습니다. 대기 중인 곡은 다운로드되지 않습니다.",
        "계속",
        "다운로드를 시작했습니다. 취소하려면 다운로드를 다시 탭하세요"
    ),
    "hi" to listOf(
        "सभी गाने चलाएं",
        "सभी गाने डाउनलोड करें",
        "डाउनलोड रद्द करें",
        "डाउनलोड रद्द करें?",
        "पहले से डाउनलोड हुए गाने डिवाइस पर रहेंगे। कतार वाले गाने डाउनलोड नहीं होंगे।",
        "जारी रखें",
        "डाउनलोड शुरू हो गया। रद्द करने के लिए फिर से डाउनलोड पर टैप करें"
    ),
    "bn" to listOf(
        "সব গান চালান",
        "সব গান ডাউনলোড করুন",
        "ডাউনলোড বাতিল করুন",
        "ডাউনলোড বাতিল করবেন?",
        "আগে ডাউনলোড হওয়া গান ডিভাইসে থাকবে। সারিতে থাকা গান ডাউনলোড হবে না।",
        "চালিয়ে যান",
        "ডাউনলোড শুরু হয়েছে। বাতিল করতে আবার ডাউনলোডে ট্যাপ করুন"
    ),
    "id" to listOf(
        "Putar semua lagu",
        "Download semua lagu",
        "Batalkan download",
        "Batalkan download?",
        "Lagu yang sudah didownload tetap ada di perangkat. Lagu dalam antrean tidak akan didownload.",
        "Lanjutkan",
        "Download dimulai. Ketuk Download lagi untuk membatalkan"
    ),
    "ms" to listOf(
        "Mainkan semua lagu",
        "Muat turun semua lagu",
        "Batalkan muat turun",
        "Batalkan muat turun?",
        "Lagu yang sudah dimuat turun kekal pada peranti. Lagu dalam baris gilir tidak akan dimuat turun.",
        "Teruskan",
        "Muat turun bermula. Ketik Muat turun sekali lagi untuk membatalkannya"
    ),
    "vi" to listOf(
        "Phát tất cả bài hát",
        "Tải xuống tất cả bài hát",
        "Hủy tải xuống",
        "Hủy tải xuống?",
        "Các bài đã tải xuống vẫn ở trên thiết bị. Các bài trong hàng đợi sẽ không được tải.",
        "Tiếp tục",
        "Đã bắt đầu tải xuống. Nhấn Tải xuống lần nữa để hủy"
    ),
    "th" to listOf(
        "เล่นเพลงทั้งหมด",
        "ดาวน์โหลดเพลงทั้งหมด",
        "ยกเลิกการดาวน์โหลด",
        "ยกเลิกการดาวน์โหลดไหม",
        "เพลงที่ดาวน์โหลดแล้วจะยังอยู่ในอุปกรณ์ เพลงที่รอคิวจะไม่ถูกดาวน์โหลด",
        "ดาวน์โหลดต่อ",
        "เริ่มดาวน์โหลดแล้ว แตะดาวน์โหลดอีกครั้งเพื่อยกเลิก"
    ),
    "fil" to listOf(
        "I-play ang lahat ng kanta",
        "I-download ang lahat ng kanta",
        "Kanselahin ang download",
        "Kanselahin ang download?",
        "Mananatili sa device ang mga na-download na kanta. Hindi na ida-download ang mga nasa pila.",
        "Ituloy",
        "Nagsimula ang download. I-tap muli ang I-download para kanselahin"
    ),
    "he" to listOf(
        "הפעלת כל השירים",
        "הורדת כל השירים",
        "ביטול ההורדה",
        "לבטל את ההורדה?",
        "שירים שכבר הורדו יישארו במכשיר. שירים שבתור לא יורדו.",
        "המשך",
        "ההורדה התחילה. אפשר להקיש שוב על הורדה כדי לבטל"
    ),
    "ur" to listOf(
        "تمام گانے چلائیں۔",
        "تمام گانے ڈاؤن لوڈ کریں۔",
        "ڈاؤن لوڈ منسوخ کریں۔",
        "ڈاؤن لوڈ منسوخ کریں؟",
        "پہلے سے ڈاؤن لوڈ کردہ گانے اس ڈیوائس پر رہیں۔ قطار میں لگے گانوں کو ڈاؤن لوڈ نہیں کیا جائے گا۔",
        "ڈاؤن لوڈ کرتے رہیں",
        "ڈاؤن لوڈ شروع ہو گیا۔ اسے منسوخ کرنے کے لیے دوبارہ ڈاؤن لوڈ پر ٹیپ کریں۔"
    ),
    "ta" to listOf(
        "அனைத்து பாடல்களையும் இயக்கவும்",
        "அனைத்து பாடல்களையும் பதிவிறக்கவும்",
        "பதிவிறக்கத்தை ரத்துசெய்",
        "பதிவிறக்கத்தை ரத்துசெய்யவா?",
        "ஏற்கனவே பதிவிறக்கம் செய்யப்பட்ட பாடல்கள் இந்தச் சாதனத்தில் இருக்கும். வரிசைப்படுத்தப்பட்ட பாடல்கள் பதிவிறக்கப்படாது.",
        "தொடர்ந்து பதிவிறக்கவும்",
        "பதிவிறக்கம் தொடங்கியது. அதை ரத்து செய்ய மீண்டும் பதிவிறக்கம் என்பதைத் தட்டவும்"
    ),
    "te" to listOf(
        "అన్ని పాటలను ప్లే చేయండి",
        "అన్ని పాటలను డౌన్‌లోడ్ చేయండి",
        "డౌన్‌లోడ్ రద్దు చేయండి",
        "డౌన్‌లోడ్‌ను రద్దు చేయాలా?",
        "ఇప్పటికే డౌన్‌లోడ్ చేయబడిన పాటలు ఈ పరికరంలో ఉంటాయి. వరుసలో ఉన్న పాటలు డౌన్‌లోడ్ చేయబడవు.",
        "డౌన్‌లోడ్ చేస్తూ ఉండండి",
        "డౌన్‌లోడ్ ప్రారంభించబడింది. దీన్ని రద్దు చేయడానికి డౌన్‌లోడ్ మళ్లీ నొక్కండి"
    ),
    "mr" to listOf(
        "सर्व गाणी वाजवा",
        "सर्व गाणी डाउनलोड करा",
        "डाउनलोड रद्द करा",
        "डाउनलोड रद्द करायचे?",
        "आधीपासून डाउनलोड केलेली गाणी या डिव्हाइसवर राहतात. रांगेत असलेली गाणी डाउनलोड केली जाणार नाहीत.",
        "डाउनलोड करत रहा",
        "डाउनलोड सुरू झाले. ते रद्द करण्यासाठी डाउनलोड पुन्हा टॅप करा"
    ),
    "gu" to listOf(
        "બધા ગીતો વગાડો",
        "બધા ગીતો ડાઉનલોડ કરો",
        "ડાઉનલોડ રદ કરો",
        "ડાઉનલોડ રદ કરીએ?",
        "પહેલાથી ડાઉનલોડ કરેલ ગીતો આ ઉપકરણ પર રહે છે. કતારબદ્ધ ગીતો ડાઉનલોડ કરવામાં આવશે નહીં.",
        "ડાઉનલોડ કરતા રહો",
        "ડાઉનલોડ શરૂ થયું. તેને રદ કરવા માટે ફરીથી ડાઉનલોડ પર ટૅપ કરો"
    ),
    "pt-BR" to listOf(
        "Tocar todas as músicas",
        "Baixe todas as músicas",
        "Cancelar download",
        "Cancelar o download?",
        "As músicas já baixadas permanecem neste dispositivo. As músicas na fila não serão baixadas.",
        "Continue baixando",
        "Download iniciado. Toque em download novamente para cancelá-lo"
    )
)

fun LevyraStrings.artistCatalogCopy(): ArtistCatalogCopy = localizedValueOrEnglish(artistCatalogCopyValues, code).let { v -> ArtistCatalogCopy(v[0], v[1], v[2], v[3], v[4], v[5], v[6]) }
