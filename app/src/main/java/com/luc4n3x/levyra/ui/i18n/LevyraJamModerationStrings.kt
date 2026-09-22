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
        "The host blocked you from this Jam", "The session is locked",
        "The session is full", "The host removed you from the Jam",
        "No guests yet", "You", "Share invite"
    ),
    "it" to jamModeration(
        "Controlli host", "Approva i nuovi ospiti", "Ogni ospite attende la tua approvazione prima di entrare",
        "In attesa di entrare", "Approva", "Rifiuta", "Rimuovi", "Blocca",
        "Ospiti bloccati", "Svuota lista bloccati", "Blocca sessione",
        "Con la sessione bloccata nessuno può più entrare", "Bloccata",
        "In attesa che l'host ti faccia entrare", "L'host ha rifiutato la tua richiesta",
        "L'host ti ha bloccato da questo Jam", "La sessione è bloccata",
        "La sessione è piena", "L'host ti ha rimosso dal Jam",
        "Ancora nessun ospite", "Tu", "Condividi invito"
    ),
    "es" to jamModeration(
        "Controles del anfitrión", "Aprobar nuevos invitados", "Cada invitado espera tu aprobación antes de entrar",
        "Esperando para entrar", "Aprobar", "Rechazar", "Quitar", "Bloquear",
        "Invitados bloqueados", "Vaciar lista de bloqueados", "Bloquear sesión",
        "Con la sesión bloqueada nadie más puede entrar", "Bloqueada",
        "Esperando a que el anfitrión te deje entrar", "El anfitrión rechazó tu solicitud",
        "El anfitrión te bloqueó en este Jam", "La sesión está bloqueada",
        "La sesión está llena", "El anfitrión te quitó del Jam",
        "Aún no hay invitados", "Tú", "Compartir invitación"
    ),
    "fr" to jamModeration(
        "Contrôles de l'hôte", "Approuver les nouveaux invités", "Chaque invité attend votre approbation avant d'entrer",
        "En attente d'entrée", "Approuver", "Refuser", "Retirer", "Bloquer",
        "Invités bloqués", "Vider la liste des bloqués", "Verrouiller la session",
        "Personne ne peut entrer quand la session est verrouillée", "Verrouillée",
        "En attente que l'hôte vous laisse entrer", "L'hôte a refusé votre demande",
        "L'hôte vous a bloqué dans ce Jam", "La session est verrouillée",
        "La session est complète", "L'hôte vous a retiré du Jam",
        "Aucun invité pour l'instant", "Vous", "Partager l'invitation"
    ),
    "de" to jamModeration(
        "Host-Steuerung", "Neue Gäste bestätigen", "Jeder Gast wartet vor dem Beitritt auf deine Bestätigung",
        "Warten auf Beitritt", "Bestätigen", "Ablehnen", "Entfernen", "Sperren",
        "Gesperrte Gäste", "Sperrliste leeren", "Sitzung sperren",
        "Bei gesperrter Sitzung kann niemand mehr beitreten", "Gesperrt",
        "Warte, bis der Host dich hereinlässt", "Der Host hat deine Anfrage abgelehnt",
        "Der Host hat dich für diesen Jam gesperrt", "Die Sitzung ist gesperrt",
        "Die Sitzung ist voll", "Der Host hat dich aus dem Jam entfernt",
        "Noch keine Gäste", "Du", "Einladung teilen"
    ),
    "pt" to jamModeration(
        "Controlos do anfitrião", "Aprovar novos convidados", "Cada convidado espera a tua aprovação antes de entrar",
        "À espera para entrar", "Aprovar", "Recusar", "Remover", "Bloquear",
        "Convidados bloqueados", "Limpar lista de bloqueados", "Bloquear sessão",
        "Com a sessão bloqueada ninguém pode entrar", "Bloqueada",
        "À espera que o anfitrião te deixe entrar", "O anfitrião recusou o teu pedido",
        "O anfitrião bloqueou-te neste Jam", "A sessão está bloqueada",
        "A sessão está cheia", "O anfitrião removeu-te do Jam",
        "Ainda sem convidados", "Tu", "Partilhar convite"
    ),
    "nl" to jamModeration(
        "Hostbediening", "Nieuwe gasten goedkeuren", "Elke gast wacht op jouw goedkeuring voordat hij meedoet",
        "Wacht op toegang", "Goedkeuren", "Weigeren", "Verwijderen", "Blokkeren",
        "Geblokkeerde gasten", "Blokkeerlijst wissen", "Sessie vergrendelen",
        "Bij een vergrendelde sessie kan niemand meer meedoen", "Vergrendeld",
        "Wachten tot de host je binnenlaat", "De host heeft je verzoek geweigerd",
        "De host heeft je geblokkeerd voor deze Jam", "De sessie is vergrendeld",
        "De sessie is vol", "De host heeft je uit de Jam verwijderd",
        "Nog geen gasten", "Jij", "Uitnodiging delen"
    ),
    "pl" to jamModeration(
        "Panel gospodarza", "Zatwierdzaj nowych gości", "Każdy gość czeka na twoje zatwierdzenie przed dołączeniem",
        "Czekają na dołączenie", "Zatwierdź", "Odrzuć", "Usuń", "Zablokuj",
        "Zablokowani goście", "Wyczyść listę blokad", "Zablokuj sesję",
        "Przy zablokowanej sesji nikt nowy nie dołączy", "Zablokowana",
        "Czekasz, aż gospodarz cię wpuści", "Gospodarz odrzucił twoją prośbę",
        "Gospodarz zablokował cię w tym Jamie", "Sesja jest zablokowana",
        "Sesja jest pełna", "Gospodarz usunął cię z Jamu",
        "Brak gości", "Ty", "Udostępnij zaproszenie"
    ),
    "ro" to jamModeration(
        "Controale gazdă", "Aprobă invitații noi", "Fiecare invitat așteaptă aprobarea ta înainte să intre",
        "Așteaptă să intre", "Aprobă", "Respinge", "Elimină", "Blochează",
        "Invitați blocați", "Golește lista de blocați", "Blochează sesiunea",
        "Cu sesiunea blocată nimeni nu mai poate intra", "Blocată",
        "Aștepți ca gazda să te lase să intri", "Gazda ți-a respins cererea",
        "Gazda te-a blocat în acest Jam", "Sesiunea este blocată",
        "Sesiunea este plină", "Gazda te-a eliminat din Jam",
        "Încă niciun invitat", "Tu", "Distribuie invitația"
    ),
    "el" to jamModeration(
        "Έλεγχοι οικοδεσπότη", "Έγκριση νέων καλεσμένων", "Κάθε καλεσμένος περιμένει την έγκρισή σου πριν μπει",
        "Περιμένουν να μπουν", "Έγκριση", "Απόρριψη", "Αφαίρεση", "Αποκλεισμός",
        "Αποκλεισμένοι καλεσμένοι", "Καθαρισμός αποκλεισμένων", "Κλείδωμα συνεδρίας",
        "Με κλειδωμένη συνεδρία δεν μπορεί να μπει κανείς", "Κλειδωμένη",
        "Περιμένεις να σε δεχτεί ο οικοδεσπότης", "Ο οικοδεσπότης απέρριψε το αίτημά σου",
        "Ο οικοδεσπότης σε απέκλεισε από αυτό το Jam", "Η συνεδρία είναι κλειδωμένη",
        "Η συνεδρία είναι γεμάτη", "Ο οικοδεσπότης σε αφαίρεσε από το Jam",
        "Κανένας καλεσμένος ακόμα", "Εσύ", "Κοινή χρήση πρόσκλησης"
    ),
    "sv" to jamModeration(
        "Värdkontroller", "Godkänn nya gäster", "Varje gäst väntar på ditt godkännande innan de går med",
        "Väntar på att gå med", "Godkänn", "Avvisa", "Ta bort", "Blockera",
        "Blockerade gäster", "Rensa blockeringslistan", "Lås sessionen",
        "Med låst session kan ingen ny gå med", "Låst",
        "Väntar på att värden släpper in dig", "Värden avvisade din förfrågan",
        "Värden blockerade dig från detta Jam", "Sessionen är låst",
        "Sessionen är full", "Värden tog bort dig från Jam",
        "Inga gäster än", "Du", "Dela inbjudan"
    ),
    "da" to jamModeration(
        "Værtsindstillinger", "Godkend nye gæster", "Hver gæst venter på din godkendelse, før de deltager",
        "Venter på at deltage", "Godkend", "Afvis", "Fjern", "Blokér",
        "Blokerede gæster", "Ryd blokeringsliste", "Lås session",
        "Med låst session kan ingen nye deltage", "Låst",
        "Venter på, at værten lukker dig ind", "Værten afviste din anmodning",
        "Værten blokerede dig fra dette Jam", "Sessionen er låst",
        "Sessionen er fuld", "Værten fjernede dig fra Jam",
        "Ingen gæster endnu", "Dig", "Del invitation"
    ),
    "cs" to jamModeration(
        "Ovládání hostitele", "Schvalovat nové hosty", "Každý host před připojením čeká na tvé schválení",
        "Čekají na připojení", "Schválit", "Odmítnout", "Odebrat", "Zablokovat",
        "Zablokovaní hosté", "Vymazat seznam blokovaných", "Zamknout relaci",
        "Se zamčenou relací se už nikdo nepřipojí", "Zamčeno",
        "Čekáš, až tě hostitel vpustí", "Hostitel tvou žádost odmítl",
        "Hostitel tě v tomto Jamu zablokoval", "Relace je zamčená",
        "Relace je plná", "Hostitel tě odebral z Jamu",
        "Zatím žádní hosté", "Ty", "Sdílet pozvánku"
    ),
    "uk" to jamModeration(
        "Керування хостом", "Схвалювати нових гостей", "Кожен гість чекає на твоє схвалення перед приєднанням",
        "Очікують приєднання", "Схвалити", "Відхилити", "Видалити", "Заблокувати",
        "Заблоковані гості", "Очистити список блокувань", "Заблокувати сесію",
        "Із заблокованою сесією ніхто новий не приєднається", "Заблоковано",
        "Очікуєш, поки хост тебе впустить", "Хост відхилив твій запит",
        "Хост заблокував тебе в цьому Jam", "Сесію заблоковано",
        "Сесія заповнена", "Хост видалив тебе з Jam",
        "Поки що немає гостей", "Ти", "Поділитися запрошенням"
    ),
    "ru" to jamModeration(
        "Управление хостом", "Одобрять новых гостей", "Каждый гость ждёт твоего одобрения перед входом",
        "Ожидают входа", "Одобрить", "Отклонить", "Удалить", "Заблокировать",
        "Заблокированные гости", "Очистить список блокировок", "Заблокировать сессию",
        "При заблокированной сессии никто новый не войдёт", "Заблокирована",
        "Ждёшь, пока хост тебя впустит", "Хост отклонил твой запрос",
        "Хост заблокировал тебя в этом Jam", "Сессия заблокирована",
        "Сессия заполнена", "Хост удалил тебя из Jam",
        "Гостей пока нет", "Ты", "Поделиться приглашением"
    ),
    "tr" to jamModeration(
        "Sunucu kontrolleri", "Yeni misafirleri onayla", "Her misafir katılmadan önce onayını bekler",
        "Katılmayı bekleyenler", "Onayla", "Reddet", "Çıkar", "Engelle",
        "Engellenen misafirler", "Engel listesini temizle", "Oturumu kilitle",
        "Oturum kilitliyken kimse katılamaz", "Kilitli",
        "Sunucunun seni almasını bekliyorsun", "Sunucu isteğini reddetti",
        "Sunucu seni bu Jam'den engelledi", "Oturum kilitli",
        "Oturum dolu", "Sunucu seni Jam'den çıkardı",
        "Henüz misafir yok", "Sen", "Daveti paylaş"
    ),
    "ar" to jamModeration(
        "أدوات المضيف", "الموافقة على الضيوف الجدد", "ينتظر كل ضيف موافقتك قبل الانضمام",
        "في انتظار الانضمام", "موافقة", "رفض", "إزالة", "حظر",
        "الضيوف المحظورون", "مسح قائمة الحظر", "قفل الجلسة",
        "عند قفل الجلسة لا يمكن لأحد الانضمام", "مقفلة",
        "في انتظار أن يسمح لك المضيف بالدخول", "رفض المضيف طلبك",
        "حظرك المضيف من هذا الـ Jam", "الجلسة مقفلة",
        "الجلسة ممتلئة", "أزالك المضيف من الـ Jam",
        "لا يوجد ضيوف بعد", "أنت", "مشاركة الدعوة"
    ),
    "zh" to jamModeration(
        "主持人控制", "审批新访客", "每位访客加入前都需要你的批准",
        "等待加入", "批准", "拒绝", "移除", "屏蔽",
        "已屏蔽的访客", "清空屏蔽列表", "锁定会话",
        "会话锁定后无人可以加入", "已锁定",
        "正在等待主持人放行", "主持人拒绝了你的请求",
        "主持人已在此 Jam 中屏蔽你", "会话已锁定",
        "会话已满", "主持人已将你移出 Jam",
        "暂无访客", "你", "分享邀请"
    ),
    "ja" to jamModeration(
        "ホスト操作", "新しいゲストを承認", "参加前にすべてのゲストがあなたの承認を待ちます",
        "参加待ち", "承認", "却下", "削除", "ブロック",
        "ブロック中のゲスト", "ブロックリストを消去", "セッションをロック",
        "ロック中は誰も参加できません", "ロック中",
        "ホストの許可を待っています", "ホストがリクエストを却下しました",
        "ホストがこの Jam であなたをブロックしました", "セッションはロックされています",
        "セッションが満員です", "ホストが Jam からあなたを削除しました",
        "ゲストはまだいません", "あなた", "招待を共有"
    ),
    "ko" to jamModeration(
        "호스트 제어", "새 게스트 승인", "모든 게스트는 참여 전에 승인을 기다립니다",
        "참여 대기 중", "승인", "거절", "내보내기", "차단",
        "차단된 게스트", "차단 목록 지우기", "세션 잠금",
        "세션이 잠기면 아무도 참여할 수 없습니다", "잠김",
        "호스트의 승인을 기다리는 중", "호스트가 요청을 거절했습니다",
        "호스트가 이 Jam에서 차단했습니다", "세션이 잠겨 있습니다",
        "세션이 가득 찼습니다", "호스트가 Jam에서 내보냈습니다",
        "아직 게스트가 없습니다", "나", "초대 공유"
    ),
    "hi" to jamModeration(
        "होस्ट नियंत्रण", "नए मेहमानों को मंज़ूरी दें", "हर मेहमान शामिल होने से पहले आपकी मंज़ूरी का इंतज़ार करता है",
        "शामिल होने की प्रतीक्षा", "मंज़ूर करें", "अस्वीकार करें", "हटाएं", "ब्लॉक करें",
        "ब्लॉक किए मेहमान", "ब्लॉक सूची साफ़ करें", "सत्र लॉक करें",
        "सत्र लॉक होने पर कोई नया शामिल नहीं हो सकता", "लॉक",
        "होस्ट के प्रवेश देने का इंतज़ार", "होस्ट ने आपका अनुरोध अस्वीकार किया",
        "होस्ट ने आपको इस Jam से ब्लॉक किया", "सत्र लॉक है",
        "सत्र भरा है", "होस्ट ने आपको Jam से हटा दिया",
        "अभी कोई मेहमान नहीं", "आप", "आमंत्रण साझा करें"
    ),
    "id" to jamModeration(
        "Kontrol host", "Setujui tamu baru", "Setiap tamu menunggu persetujuanmu sebelum bergabung",
        "Menunggu bergabung", "Setujui", "Tolak", "Keluarkan", "Blokir",
        "Tamu diblokir", "Bersihkan daftar blokir", "Kunci sesi",
        "Saat sesi terkunci tidak ada yang bisa bergabung", "Terkunci",
        "Menunggu host mengizinkanmu masuk", "Host menolak permintaanmu",
        "Host memblokirmu dari Jam ini", "Sesi terkunci",
        "Sesi penuh", "Host mengeluarkanmu dari Jam",
        "Belum ada tamu", "Kamu", "Bagikan undangan"
    ),
    "vi" to jamModeration(
        "Điều khiển của chủ phòng", "Duyệt khách mới", "Mỗi khách chờ bạn duyệt trước khi tham gia",
        "Đang chờ tham gia", "Duyệt", "Từ chối", "Xóa", "Chặn",
        "Khách bị chặn", "Xóa danh sách chặn", "Khóa phiên",
        "Khi phiên bị khóa không ai tham gia được", "Đã khóa",
        "Đang chờ chủ phòng cho vào", "Chủ phòng đã từ chối yêu cầu của bạn",
        "Chủ phòng đã chặn bạn khỏi Jam này", "Phiên đang bị khóa",
        "Phiên đã đầy", "Chủ phòng đã xóa bạn khỏi Jam",
        "Chưa có khách", "Bạn", "Chia sẻ lời mời"
    ),
    "th" to jamModeration(
        "การควบคุมของเจ้าของห้อง", "อนุมัติผู้เข้าร่วมใหม่", "ผู้เข้าร่วมทุกคนต้องรอการอนุมัติก่อนเข้าห้อง",
        "รอเข้าร่วม", "อนุมัติ", "ปฏิเสธ", "นำออก", "บล็อก",
        "ผู้เข้าร่วมที่ถูกบล็อก", "ล้างรายการบล็อก", "ล็อกเซสชัน",
        "เมื่อล็อกเซสชันจะไม่มีใครเข้าร่วมได้", "ล็อกแล้ว",
        "กำลังรอเจ้าของห้องอนุญาต", "เจ้าของห้องปฏิเสธคำขอของคุณ",
        "เจ้าของห้องบล็อกคุณจาก Jam นี้", "เซสชันถูกล็อก",
        "เซสชันเต็มแล้ว", "เจ้าของห้องนำคุณออกจาก Jam",
        "ยังไม่มีผู้เข้าร่วม", "คุณ", "แชร์คำเชิญ"
    ),
    "fil" to jamModeration(
        "Mga kontrol ng host", "Aprubahan ang bagong bisita", "Bawat bisita ay naghihintay ng pag-apruba mo bago sumali",
        "Naghihintay sumali", "Aprubahan", "Tanggihan", "Alisin", "I-block",
        "Mga naka-block na bisita", "Linisin ang block list", "I-lock ang session",
        "Kapag naka-lock ang session walang makakasali", "Naka-lock",
        "Naghihintay na papasukin ka ng host", "Tinanggihan ng host ang request mo",
        "Na-block ka ng host sa Jam na ito", "Naka-lock ang session",
        "Puno na ang session", "Inalis ka ng host sa Jam",
        "Wala pang bisita", "Ikaw", "Ibahagi ang imbitasyon"
    ),
    "he" to jamModeration(
        "בקרות המארח", "אישור אורחים חדשים", "כל אורח ממתין לאישור שלך לפני ההצטרפות",
        "ממתינים להצטרף", "אישור", "דחייה", "הסרה", "חסימה",
        "אורחים חסומים", "ניקוי רשימת החסומים", "נעילת המפגש",
        "כשהמפגש נעול אף אחד לא יכול להצטרף", "נעול",
        "ממתין שהמארח יכניס אותך", "המארח דחה את הבקשה שלך",
        "המארח חסם אותך ב-Jam הזה", "המפגש נעול",
        "המפגש מלא", "המארח הסיר אותך מה-Jam",
        "עדיין אין אורחים", "את/ה", "שיתוף הזמנה"
    ),
    "fi" to jamModeration(
        "Isännän säätimet", "Hyväksy uudet vieraat", "Jokainen vieras odottaa hyväksyntääsi ennen liittymistä",
        "Odottaa liittymistä", "Hyväksy", "Hylkää", "Poista", "Estä",
        "Estetyt vieraat", "Tyhjennä estolista", "Lukitse istunto",
        "Kukaan uusi ei voi liittyä istunnon ollessa lukittu", "Lukittu",
        "Odotetaan isännän hyväksyntää", "Isäntä hylkäsi pyyntösi",
        "Isäntä esti sinut tästä Jamista", "Istunto on lukittu",
        "Istunto on täynnä", "Isäntä poisti sinut Jamista",
        "Ei vielä vieraita", "Sinä", "Jaa kutsu"
    ),
    "et" to jamModeration(
        "Võõrustaja juhtnupud", "Kinnita uued külalised", "Iga külaline ootab enne liitumist sinu heakskiitu",
        "Ootab liitumist", "Kinnita", "Lükka tagasi", "Eemalda", "Blokeeri",
        "Blokeeritud külalised", "Tühjenda blokeeritud nimekiri", "Lukusta seanss",
        "Lukustatud seansiga ei saa keegi uus liituda", "Lukustatud",
        "Oodatakse võõrustaja heakskiitu", "Võõrustaja lükkas su taotluse tagasi",
        "Võõrustaja blokeeris sind sellest Jamist", "Seanss on lukustatud",
        "Seanss on täis", "Võõrustaja eemaldas sind Jamist",
        "Külalisi veel pole", "Sina", "Jaga kutset"
    )
)

internal fun jamModerationLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(jamModerationBundles, code)

internal fun jamModerationLocalizationCodes(): Set<String> = jamModerationBundles.keys
