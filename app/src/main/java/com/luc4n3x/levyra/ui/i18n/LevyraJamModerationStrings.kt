package com.luc4n3x.levyra.ui.i18n

internal val jamModerationKeys = setOf(
    "jamHostControls",
    "jamApprovalRequired",
    "jamApprovalRequiredSubtitle",
    "jamPendingRequests",
    "jamApprove",
    "jamReject",
    "jamKick",
    "jamBan",
    "jamBannedGuests",
    "jamClearBans",
    "jamLockSession",
    "jamLockSessionSubtitle",
    "jamLocked",
    "jamAwaitingApproval",
    "jamRejected",
    "jamBannedMessage",
    "jamSessionLockedMessage",
    "jamSessionFull",
    "jamRemovedMessage",
    "jamNoParticipants",
    "jamYou",
    "jamShareInvite"
)

private fun jamModeration(
    hostControls: String,
    approvalRequired: String,
    approvalRequiredSubtitle: String,
    pendingRequests: String,
    approve: String,
    reject: String,
    kick: String,
    ban: String,
    bannedGuests: String,
    clearBans: String,
    lockSession: String,
    lockSessionSubtitle: String,
    locked: String,
    awaitingApproval: String,
    rejected: String,
    bannedMessage: String,
    sessionLockedMessage: String,
    sessionFull: String,
    removedMessage: String,
    noParticipants: String,
    you: String,
    shareInvite: String
): Map<String, String> = mapOf(
    "jamHostControls" to hostControls,
    "jamApprovalRequired" to approvalRequired,
    "jamApprovalRequiredSubtitle" to approvalRequiredSubtitle,
    "jamPendingRequests" to pendingRequests,
    "jamApprove" to approve,
    "jamReject" to reject,
    "jamKick" to kick,
    "jamBan" to ban,
    "jamBannedGuests" to bannedGuests,
    "jamClearBans" to clearBans,
    "jamLockSession" to lockSession,
    "jamLockSessionSubtitle" to lockSessionSubtitle,
    "jamLocked" to locked,
    "jamAwaitingApproval" to awaitingApproval,
    "jamRejected" to rejected,
    "jamBannedMessage" to bannedMessage,
    "jamSessionLockedMessage" to sessionLockedMessage,
    "jamSessionFull" to sessionFull,
    "jamRemovedMessage" to removedMessage,
    "jamNoParticipants" to noParticipants,
    "jamYou" to you,
    "jamShareInvite" to shareInvite
)

private val jamModerationBundles: Map<String, Map<String, String>> = mapOf(
    "en" to jamModeration(
        "Host controls", "Approve new guests", "Every guest waits for your approval before joining",
        "Waiting to join", "Approve", "Reject", "Remove", "Block",
        "Blocked guests", "Clear blocked list", "Lock session",
        "Nobody new can join while the session is locked", "Locked",
        "Waiting for the host to let you in", "The host declined your request",
        "The host blocked you from this Unison", "The session is locked",
        "The session is full", "The host removed you from the Unison",
        "No guests yet", "You", "Share invite"
    ),
    "it" to jamModeration(
        "Controlli host", "Approva i nuovi ospiti", "Ogni ospite attende la tua approvazione prima di entrare",
        "In attesa di entrare", "Approva", "Rifiuta", "Rimuovi", "Blocca",
        "Ospiti bloccati", "Svuota lista bloccati", "Blocca sessione",
        "Con la sessione bloccata nessuno può più entrare", "Bloccata",
        "In attesa che l'host ti faccia entrare", "L'host ha rifiutato la tua richiesta",
        "L'host ti ha bloccato da questo Unison", "La sessione è bloccata",
        "La sessione è piena", "L'host ti ha rimosso dall'Unison",
        "Ancora nessun ospite", "Tu", "Condividi invito"
    ),
    "es" to jamModeration(
        "Controles del anfitrión", "Aprobar nuevos invitados", "Cada invitado espera tu aprobación antes de entrar",
        "Esperando para entrar", "Aprobar", "Rechazar", "Quitar", "Bloquear",
        "Invitados bloqueados", "Vaciar lista de bloqueados", "Bloquear sesión",
        "Con la sesión bloqueada nadie más puede entrar", "Bloqueada",
        "Esperando a que el anfitrión te deje entrar", "El anfitrión rechazó tu solicitud",
        "El anfitrión te bloqueó en este Unison", "La sesión está bloqueada",
        "La sesión está llena", "El anfitrión te quitó del Unison",
        "Aún no hay invitados", "Tú", "Compartir invitación"
    ),
    "fr" to jamModeration(
        "Contrôles de l'hôte", "Approuver les nouveaux invités", "Chaque invité attend votre approbation avant d'entrer",
        "En attente d'entrée", "Approuver", "Refuser", "Retirer", "Bloquer",
        "Invités bloqués", "Vider la liste des bloqués", "Verrouiller la session",
        "Personne ne peut entrer quand la session est verrouillée", "Verrouillée",
        "En attente que l'hôte vous laisse entrer", "L'hôte a refusé votre demande",
        "L'hôte vous a bloqué dans ce Unison", "La session est verrouillée",
        "La session est complète", "L'hôte vous a retiré de l'Unison",
        "Aucun invité pour l'instant", "Vous", "Partager l'invitation"
    ),
    "de" to jamModeration(
        "Host-Steuerung", "Neue Gäste bestätigen", "Jeder Gast wartet vor dem Beitritt auf deine Bestätigung",
        "Warten auf Beitritt", "Bestätigen", "Ablehnen", "Entfernen", "Sperren",
        "Gesperrte Gäste", "Sperrliste leeren", "Sitzung sperren",
        "Bei gesperrter Sitzung kann niemand mehr beitreten", "Gesperrt",
        "Warte, bis der Host dich hereinlässt", "Der Host hat deine Anfrage abgelehnt",
        "Der Host hat dich für diesen Unison gesperrt", "Die Sitzung ist gesperrt",
        "Die Sitzung ist voll", "Der Host hat dich aus dem Unison entfernt",
        "Noch keine Gäste", "Du", "Einladung teilen"
    ),
    "pt" to jamModeration(
        "Controlos do anfitrião", "Aprovar novos convidados", "Cada convidado espera a tua aprovação antes de entrar",
        "À espera para entrar", "Aprovar", "Recusar", "Remover", "Bloquear",
        "Convidados bloqueados", "Limpar lista de bloqueados", "Bloquear sessão",
        "Com a sessão bloqueada ninguém pode entrar", "Bloqueada",
        "À espera que o anfitrião te deixe entrar", "O anfitrião recusou o teu pedido",
        "O anfitrião bloqueou-te neste Unison", "A sessão está bloqueada",
        "A sessão está cheia", "O anfitrião removeu-te do Unison",
        "Ainda sem convidados", "Tu", "Partilhar convite"
    ),
    "nl" to jamModeration(
        "Hostbediening", "Nieuwe gasten goedkeuren", "Elke gast wacht op jouw goedkeuring voordat hij meedoet",
        "Wacht op toegang", "Goedkeuren", "Weigeren", "Verwijderen", "Blokkeren",
        "Geblokkeerde gasten", "Blokkeerlijst wissen", "Sessie vergrendelen",
        "Bij een vergrendelde sessie kan niemand meer meedoen", "Vergrendeld",
        "Wachten tot de host je binnenlaat", "De host heeft je verzoek geweigerd",
        "De host heeft je geblokkeerd voor deze Unison", "De sessie is vergrendeld",
        "De sessie is vol", "De host heeft je uit de Unison verwijderd",
        "Nog geen gasten", "Jij", "Uitnodiging delen"
    ),
    "pl" to jamModeration(
        "Panel gospodarza", "Zatwierdzaj nowych gości", "Każdy gość czeka na twoje zatwierdzenie przed dołączeniem",
        "Czekają na dołączenie", "Zatwierdź", "Odrzuć", "Usuń", "Zablokuj",
        "Zablokowani goście", "Wyczyść listę blokad", "Zablokuj sesję",
        "Przy zablokowanej sesji nikt nowy nie dołączy", "Zablokowana",
        "Czekasz, aż gospodarz cię wpuści", "Gospodarz odrzucił twoją prośbę",
        "Gospodarz zablokował cię w tym Unisonie", "Sesja jest zablokowana",
        "Sesja jest pełna", "Gospodarz usunął cię z Unisonu",
        "Brak gości", "Ty", "Udostępnij zaproszenie"
    ),
    "ro" to jamModeration(
        "Controale gazdă", "Aprobă invitații noi", "Fiecare invitat așteaptă aprobarea ta înainte să intre",
        "Așteaptă să intre", "Aprobă", "Respinge", "Elimină", "Blochează",
        "Invitați blocați", "Golește lista de blocați", "Blochează sesiunea",
        "Cu sesiunea blocată nimeni nu mai poate intra", "Blocată",
        "Aștepți ca gazda să te lase să intri", "Gazda ți-a respins cererea",
        "Gazda te-a blocat în acest Unison", "Sesiunea este blocată",
        "Sesiunea este plină", "Gazda te-a eliminat din Unison",
        "Încă niciun invitat", "Tu", "Distribuie invitația"
    ),
    "el" to jamModeration(
        "Έλεγχοι οικοδεσπότη", "Έγκριση νέων καλεσμένων", "Κάθε καλεσμένος περιμένει την έγκρισή σου πριν μπει",
        "Περιμένουν να μπουν", "Έγκριση", "Απόρριψη", "Αφαίρεση", "Αποκλεισμός",
        "Αποκλεισμένοι καλεσμένοι", "Καθαρισμός αποκλεισμένων", "Κλείδωμα συνεδρίας",
        "Με κλειδωμένη συνεδρία δεν μπορεί να μπει κανείς", "Κλειδωμένη",
        "Περιμένεις να σε δεχτεί ο οικοδεσπότης", "Ο οικοδεσπότης απέρριψε το αίτημά σου",
        "Ο οικοδεσπότης σε απέκλεισε από αυτό το Unison", "Η συνεδρία είναι κλειδωμένη",
        "Η συνεδρία είναι γεμάτη", "Ο οικοδεσπότης σε αφαίρεσε από το Unison",
        "Κανένας καλεσμένος ακόμα", "Εσύ", "Κοινή χρήση πρόσκλησης"
    ),
    "sv" to jamModeration(
        "Värdkontroller", "Godkänn nya gäster", "Varje gäst väntar på ditt godkännande innan de går med",
        "Väntar på att gå med", "Godkänn", "Avvisa", "Ta bort", "Blockera",
        "Blockerade gäster", "Rensa blockeringslistan", "Lås sessionen",
        "Med låst session kan ingen ny gå med", "Låst",
        "Väntar på att värden släpper in dig", "Värden avvisade din förfrågan",
        "Värden blockerade dig från detta Unison", "Sessionen är låst",
        "Sessionen är full", "Värden tog bort dig från Unison",
        "Inga gäster än", "Du", "Dela inbjudan"
    ),
    "da" to jamModeration(
        "Værtsindstillinger", "Godkend nye gæster", "Hver gæst venter på din godkendelse, før de deltager",
        "Venter på at deltage", "Godkend", "Afvis", "Fjern", "Blokér",
        "Blokerede gæster", "Ryd blokeringsliste", "Lås session",
        "Med låst session kan ingen nye deltage", "Låst",
        "Venter på, at værten lukker dig ind", "Værten afviste din anmodning",
        "Værten blokerede dig fra dette Unison", "Sessionen er låst",
        "Sessionen er fuld", "Værten fjernede dig fra Unison",
        "Ingen gæster endnu", "Dig", "Del invitation"
    ),
    "cs" to jamModeration(
        "Ovládání hostitele", "Schvalovat nové hosty", "Každý host před připojením čeká na tvé schválení",
        "Čekají na připojení", "Schválit", "Odmítnout", "Odebrat", "Zablokovat",
        "Zablokovaní hosté", "Vymazat seznam blokovaných", "Zamknout relaci",
        "Se zamčenou relací se už nikdo nepřipojí", "Zamčeno",
        "Čekáš, až tě hostitel vpustí", "Hostitel tvou žádost odmítl",
        "Hostitel tě v tomto Unisonu zablokoval", "Relace je zamčená",
        "Relace je plná", "Hostitel tě odebral z Unisonu",
        "Zatím žádní hosté", "Ty", "Sdílet pozvánku"
    ),
    "uk" to jamModeration(
        "Керування хостом", "Схвалювати нових гостей", "Кожен гість чекає на твоє схвалення перед приєднанням",
        "Очікують приєднання", "Схвалити", "Відхилити", "Видалити", "Заблокувати",
        "Заблоковані гості", "Очистити список блокувань", "Заблокувати сесію",
        "Із заблокованою сесією ніхто новий не приєднається", "Заблоковано",
        "Очікуєш, поки хост тебе впустить", "Хост відхилив твій запит",
        "Хост заблокував тебе в цьому Unison", "Сесію заблоковано",
        "Сесія заповнена", "Хост видалив тебе з Unison",
        "Поки що немає гостей", "Ти", "Поділитися запрошенням"
    ),
    "ru" to jamModeration(
        "Управление хостом", "Одобрять новых гостей", "Каждый гость ждёт твоего одобрения перед входом",
        "Ожидают входа", "Одобрить", "Отклонить", "Удалить", "Заблокировать",
        "Заблокированные гости", "Очистить список блокировок", "Заблокировать сессию",
        "При заблокированной сессии никто новый не войдёт", "Заблокирована",
        "Ждёшь, пока хост тебя впустит", "Хост отклонил твой запрос",
        "Хост заблокировал тебя в этом Unison", "Сессия заблокирована",
        "Сессия заполнена", "Хост удалил тебя из Unison",
        "Гостей пока нет", "Ты", "Поделиться приглашением"
    ),
    "tr" to jamModeration(
        "Sunucu kontrolleri", "Yeni misafirleri onayla", "Her misafir katılmadan önce onayını bekler",
        "Katılmayı bekleyenler", "Onayla", "Reddet", "Çıkar", "Engelle",
        "Engellenen misafirler", "Engel listesini temizle", "Oturumu kilitle",
        "Oturum kilitliyken kimse katılamaz", "Kilitli",
        "Sunucunun seni almasını bekliyorsun", "Sunucu isteğini reddetti",
        "Sunucu seni bu Unison'den engelledi", "Oturum kilitli",
        "Oturum dolu", "Sunucu seni Unison'den çıkardı",
        "Henüz misafir yok", "Sen", "Daveti paylaş"
    ),
    "ar" to jamModeration(
        "أدوات المضيف", "الموافقة على الضيوف الجدد", "ينتظر كل ضيف موافقتك قبل الانضمام",
        "في انتظار الانضمام", "موافقة", "رفض", "إزالة", "حظر",
        "الضيوف المحظورون", "مسح قائمة الحظر", "قفل الجلسة",
        "عند قفل الجلسة لا يمكن لأحد الانضمام", "مقفلة",
        "في انتظار أن يسمح لك المضيف بالدخول", "رفض المضيف طلبك",
        "حظرك المضيف من هذا الـ Unison", "الجلسة مقفلة",
        "الجلسة ممتلئة", "أزالك المضيف من الـ Unison",
        "لا يوجد ضيوف بعد", "أنت", "مشاركة الدعوة"
    ),
    "zh" to jamModeration(
        "主持人控制", "审批新访客", "每位访客加入前都需要你的批准",
        "等待加入", "批准", "拒绝", "移除", "屏蔽",
        "已屏蔽的访客", "清空屏蔽列表", "锁定会话",
        "会话锁定后无人可以加入", "已锁定",
        "正在等待主持人放行", "主持人拒绝了你的请求",
        "主持人已在此 Unison 中屏蔽你", "会话已锁定",
        "会话已满", "主持人已将你移出 Unison",
        "暂无访客", "你", "分享邀请"
    ),
    "ja" to jamModeration(
        "ホスト操作", "新しいゲストを承認", "参加前にすべてのゲストがあなたの承認を待ちます",
        "参加待ち", "承認", "却下", "削除", "ブロック",
        "ブロック中のゲスト", "ブロックリストを消去", "セッションをロック",
        "ロック中は誰も参加できません", "ロック中",
        "ホストの許可を待っています", "ホストがリクエストを却下しました",
        "ホストがこの Unison であなたをブロックしました", "セッションはロックされています",
        "セッションが満員です", "ホストが Unison からあなたを削除しました",
        "ゲストはまだいません", "あなた", "招待を共有"
    ),
    "ko" to jamModeration(
        "호스트 제어", "새 게스트 승인", "모든 게스트는 참여 전에 승인을 기다립니다",
        "참여 대기 중", "승인", "거절", "내보내기", "차단",
        "차단된 게스트", "차단 목록 지우기", "세션 잠금",
        "세션이 잠기면 아무도 참여할 수 없습니다", "잠김",
        "호스트의 승인을 기다리는 중", "호스트가 요청을 거절했습니다",
        "호스트가 이 Unison에서 차단했습니다", "세션이 잠겨 있습니다",
        "세션이 가득 찼습니다", "호스트가 Unison에서 내보냈습니다",
        "아직 게스트가 없습니다", "나", "초대 공유"
    ),
    "hi" to jamModeration(
        "होस्ट नियंत्रण", "नए मेहमानों को मंज़ूरी दें", "हर मेहमान शामिल होने से पहले आपकी मंज़ूरी का इंतज़ार करता है",
        "शामिल होने की प्रतीक्षा", "मंज़ूर करें", "अस्वीकार करें", "हटाएं", "ब्लॉक करें",
        "ब्लॉक किए मेहमान", "ब्लॉक सूची साफ़ करें", "सत्र लॉक करें",
        "सत्र लॉक होने पर कोई नया शामिल नहीं हो सकता", "लॉक",
        "होस्ट के प्रवेश देने का इंतज़ार", "होस्ट ने आपका अनुरोध अस्वीकार किया",
        "होस्ट ने आपको इस Unison से ब्लॉक किया", "सत्र लॉक है",
        "सत्र भरा है", "होस्ट ने आपको Unison से हटा दिया",
        "अभी कोई मेहमान नहीं", "आप", "आमंत्रण साझा करें"
    ),
    "id" to jamModeration(
        "Kontrol host", "Setujui tamu baru", "Setiap tamu menunggu persetujuanmu sebelum bergabung",
        "Menunggu bergabung", "Setujui", "Tolak", "Keluarkan", "Blokir",
        "Tamu diblokir", "Bersihkan daftar blokir", "Kunci sesi",
        "Saat sesi terkunci tidak ada yang bisa bergabung", "Terkunci",
        "Menunggu host mengizinkanmu masuk", "Host menolak permintaanmu",
        "Host memblokirmu dari Unison ini", "Sesi terkunci",
        "Sesi penuh", "Host mengeluarkanmu dari Unison",
        "Belum ada tamu", "Kamu", "Bagikan undangan"
    ),
    "vi" to jamModeration(
        "Điều khiển của chủ phòng", "Duyệt khách mới", "Mỗi khách chờ bạn duyệt trước khi tham gia",
        "Đang chờ tham gia", "Duyệt", "Từ chối", "Xóa", "Chặn",
        "Khách bị chặn", "Xóa danh sách chặn", "Khóa phiên",
        "Khi phiên bị khóa không ai tham gia được", "Đã khóa",
        "Đang chờ chủ phòng cho vào", "Chủ phòng đã từ chối yêu cầu của bạn",
        "Chủ phòng đã chặn bạn khỏi Unison này", "Phiên đang bị khóa",
        "Phiên đã đầy", "Chủ phòng đã xóa bạn khỏi Unison",
        "Chưa có khách", "Bạn", "Chia sẻ lời mời"
    ),
    "th" to jamModeration(
        "การควบคุมของเจ้าของห้อง", "อนุมัติผู้เข้าร่วมใหม่", "ผู้เข้าร่วมทุกคนต้องรอการอนุมัติก่อนเข้าห้อง",
        "รอเข้าร่วม", "อนุมัติ", "ปฏิเสธ", "นำออก", "บล็อก",
        "ผู้เข้าร่วมที่ถูกบล็อก", "ล้างรายการบล็อก", "ล็อกเซสชัน",
        "เมื่อล็อกเซสชันจะไม่มีใครเข้าร่วมได้", "ล็อกแล้ว",
        "กำลังรอเจ้าของห้องอนุญาต", "เจ้าของห้องปฏิเสธคำขอของคุณ",
        "เจ้าของห้องบล็อกคุณจาก Unison นี้", "เซสชันถูกล็อก",
        "เซสชันเต็มแล้ว", "เจ้าของห้องนำคุณออกจาก Unison",
        "ยังไม่มีผู้เข้าร่วม", "คุณ", "แชร์คำเชิญ"
    ),
    "fil" to jamModeration(
        "Mga kontrol ng host", "Aprubahan ang bagong bisita", "Bawat bisita ay naghihintay ng pag-apruba mo bago sumali",
        "Naghihintay sumali", "Aprubahan", "Tanggihan", "Alisin", "I-block",
        "Mga naka-block na bisita", "Linisin ang block list", "I-lock ang session",
        "Kapag naka-lock ang session walang makakasali", "Naka-lock",
        "Naghihintay na papasukin ka ng host", "Tinanggihan ng host ang request mo",
        "Na-block ka ng host sa Unison na ito", "Naka-lock ang session",
        "Puno na ang session", "Inalis ka ng host sa Unison",
        "Wala pang bisita", "Ikaw", "Ibahagi ang imbitasyon"
    ),
    "he" to jamModeration(
        "בקרות המארח", "אישור אורחים חדשים", "כל אורח ממתין לאישור שלך לפני ההצטרפות",
        "ממתינים להצטרף", "אישור", "דחייה", "הסרה", "חסימה",
        "אורחים חסומים", "ניקוי רשימת החסומים", "נעילת המפגש",
        "כשהמפגש נעול אף אחד לא יכול להצטרף", "נעול",
        "ממתין שהמארח יכניס אותך", "המארח דחה את הבקשה שלך",
        "המארח חסם אותך ב-Unison הזה", "המפגש נעול",
        "המפגש מלא", "המארח הסיר אותך מה-Unison",
        "עדיין אין אורחים", "את/ה", "שיתוף הזמנה"
    ),
    "fi" to jamModeration(
        "Isännän säätimet", "Hyväksy uudet vieraat", "Jokainen vieras odottaa hyväksyntääsi ennen liittymistä",
        "Odottaa liittymistä", "Hyväksy", "Hylkää", "Poista", "Estä",
        "Estetyt vieraat", "Tyhjennä estolista", "Lukitse istunto",
        "Kukaan uusi ei voi liittyä istunnon ollessa lukittu", "Lukittu",
        "Odotetaan isännän hyväksyntää", "Isäntä hylkäsi pyyntösi",
        "Isäntä esti sinut tästä Unisonista", "Istunto on lukittu",
        "Istunto on täynnä", "Isäntä poisti sinut Unisonista",
        "Ei vielä vieraita", "Sinä", "Jaa kutsu"
    ),
    "et" to jamModeration(
        "Võõrustaja juhtnupud", "Kinnita uued külalised", "Iga külaline ootab enne liitumist sinu heakskiitu",
        "Ootab liitumist", "Kinnita", "Lükka tagasi", "Eemalda", "Blokeeri",
        "Blokeeritud külalised", "Tühjenda blokeeritud nimekiri", "Lukusta seanss",
        "Lukustatud seansiga ei saa keegi uus liituda", "Lukustatud",
        "Oodatakse võõrustaja heakskiitu", "Võõrustaja lükkas su taotluse tagasi",
        "Võõrustaja blokeeris sind sellest Unisonist", "Seanss on lukustatud",
        "Seanss on täis", "Võõrustaja eemaldas sind Unisonist",
        "Külalisi veel pole", "Sina", "Jaga kutset"
    ),
    "ur" to jamModeration(
        "میزبان کنٹرولز", "نئے مہمانوں کو منظور کریں۔", "ہر مہمان شامل ہونے سے پہلے آپ کی منظوری کا انتظار کرتا ہے۔",
        "شامل ہونے کا انتظار ہے۔", "منظور کرو", "رد کرنا", "ہٹا دیں۔", "بلاک",
        "مسدود مہمان", "مسدود فہرست کو صاف کریں۔", "لاک سیشن",
        "سیشن کے مقفل ہونے کے دوران کوئی بھی نیا شامل نہیں ہو سکتا", "مقفل",
        "میزبان کے آپ کو اندر آنے کا انتظار ہے۔", "میزبان نے آپ کی درخواست مسترد کر دی۔",
        "میزبان نے آپ کو اس اتحاد سے روک دیا ہے۔", "سیشن مقفل ہے۔",
        "سیشن بھرا ہوا ہے۔", "میزبان نے آپ کو اتحاد سے ہٹا دیا۔",
        "ابھی تک کوئی مہمان نہیں ہے۔", "آپ", "دعوت کا اشتراک کریں۔"
    ),
    "ta" to jamModeration(
        "ஹோஸ்ட் கட்டுப்பாடுகள்", "புதிய விருந்தினர்களை அங்கீகரிக்கவும்", "ஒவ்வொரு விருந்தினரும் சேர்வதற்கு முன் உங்கள் ஒப்புதலுக்காக காத்திருக்கிறார்கள்",
        "சேர காத்திருக்கிறது", "ஒப்புதல்", "நிராகரிக்கவும்", "அகற்று", "தடு",
        "விருந்தினர்கள் தடுக்கப்பட்டனர்", "தடுக்கப்பட்ட பட்டியலை அழிக்கவும்", "அமர்வை பூட்டு",
        "அமர்வு பூட்டப்பட்டிருக்கும் போது புதிதாக யாரும் சேர முடியாது", "பூட்டப்பட்டது",
        "ஹோஸ்ட் உங்களை அனுமதிக்கும் வரை காத்திருக்கிறது", "ஹோஸ்ட் உங்கள் கோரிக்கையை நிராகரித்தார்",
        "இந்த யூனிசனில் இருந்து உங்களை ஹோஸ்ட் தடுத்துள்ளார்", "அமர்வு பூட்டப்பட்டுள்ளது",
        "அமர்வு நிரம்பியுள்ளது", "ஹோஸ்ட் உங்களை யூனிசனில் இருந்து நீக்கிவிட்டார்",
        "இன்னும் விருந்தினர்கள் இல்லை", "நீங்கள்", "அழைப்பைப் பகிரவும்"
    ),
    "te" to jamModeration(
        "హోస్ట్ నియంత్రణలు", "కొత్త అతిథులను ఆమోదించండి", "ప్రతి అతిథి చేరడానికి ముందు మీ ఆమోదం కోసం వేచి ఉంటారు",
        "చేరడానికి వేచి ఉంది", "ఆమోదించండి", "తిరస్కరించు", "తొలగించు", "నిరోధించు",
        "బ్లాక్ చేయబడిన అతిథులు", "బ్లాక్ చేయబడిన జాబితాను క్లియర్ చేయండి", "సెషన్ లాక్ చేయండి",
        "సెషన్ లాక్ చేయబడినప్పుడు కొత్తవారు ఎవరూ చేరలేరు", "లాక్ చేయబడింది",
        "హోస్ట్ మిమ్మల్ని అనుమతించే వరకు వేచి ఉంది", "హోస్ట్ మీ అభ్యర్థనను తిరస్కరించారు",
        "హోస్ట్ మిమ్మల్ని ఈ యునిసన్ నుండి బ్లాక్ చేసారు", "సెషన్ లాక్ చేయబడింది",
        "సెషన్ నిండింది", "హోస్ట్ మిమ్మల్ని యూనిసన్ నుండి తొలగించారు",
        "ఇంకా అతిథులు లేరు", "మీరు", "ఆహ్వానాన్ని షేర్ చేయండి"
    ),
    "mr" to jamModeration(
        "होस्ट नियंत्रणे", "नवीन पाहुण्यांना मान्यता द्या", "प्रत्येक अतिथी सामील होण्यापूर्वी आपल्या मंजुरीची प्रतीक्षा करतो",
        "सामील होण्याची प्रतीक्षा करत आहे", "मंजूर करा", "नकार द्या", "काढा", "ब्लॉक करा",
        "अवरोधित अतिथी", "ब्लॉक केलेली यादी साफ करा", "लॉक सत्र",
        "सत्र लॉक असताना कोणीही नवीन सामील होऊ शकत नाही", "कुलूपबंद",
        "यजमान तुम्हाला आत येण्याची वाट पाहत आहे", "होस्टने तुमची विनंती नाकारली",
        "होस्टने तुम्हाला या युनिसनमधून ब्लॉक केले आहे", "सत्र लॉक केलेले आहे",
        "सत्र भरले आहे", "यजमानाने तुम्हाला युनिसनमधून काढून टाकले",
        "अद्याप कोणतेही अतिथी नाहीत", "आपण", "आमंत्रण शेअर करा"
    ),
    "gu" to jamModeration(
        "યજમાન નિયંત્રણો", "નવા મહેમાનોને મંજૂરી આપો", "દરેક મહેમાન જોડાતા પહેલા તમારી મંજૂરીની રાહ જુએ છે",
        "જોડાવા માટે રાહ જોઈ રહ્યું છે", "મંજૂર", "અસ્વીકાર કરો", "દૂર કરો", "બ્લોક",
        "અવરોધિત મહેમાનો", "અવરોધિત સૂચિ સાફ કરો", "લૉક સત્ર",
        "સત્ર લૉક હોય ત્યારે કોઈ નવું જોડાઈ શકશે નહીં", "તાળું મારેલું",
        "યજમાન તમને અંદર આવવા દે તેની રાહ જોઈ રહ્યાં છીએ", "હોસ્ટે તમારી વિનંતી નકારી",
        "હોસ્ટે તમને આ યુનિસનથી અવરોધિત કર્યા છે", "સત્ર લૉક છે",
        "સત્ર ભરાઈ ગયું છે", "યજમાન તમને યુનિસનમાંથી દૂર કરે છે",
        "હજુ સુધી કોઈ મહેમાનો નથી", "તમે", "આમંત્રણ શેર કરો"
    ),
    "pt-BR" to jamModeration(
        "Controles de host", "Aprovar novos convidados", "Cada convidado aguarda sua aprovação antes de ingressar",
        "Esperando para entrar", "Aprovar", "Rejeitar", "Remover", "Bloquear",
        "Convidados bloqueados", "Limpar lista bloqueada", "Bloquear sessão",
        "Ninguém novo pode entrar enquanto a sessão estiver bloqueada", "Bloqueado",
        "Esperando o anfitrião deixar você entrar", "O anfitrião recusou sua solicitação",
        "O anfitrião bloqueou você neste Unison", "A sessão está bloqueada",
        "A sessão está cheia", "O anfitrião removeu você do Unison",
        "Ainda não há convidados", "Você", "Compartilhar convite"
    )
)

internal fun jamModerationLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(jamModerationBundles, code)

internal fun jamModerationLocalizationCodes(): Set<String> = jamModerationBundles.keys
