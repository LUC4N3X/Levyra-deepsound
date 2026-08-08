package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.PlaylistImportFailureKind

fun playlistImportStartedMessage(code: String): String = when (code) {
    "it" -> "Importazione playlist in corso…"
    "es" -> "Importando la playlist…"
    "fr" -> "Importation de la playlist…"
    "de" -> "Playlist wird importiert…"
    "pt" -> "A importar a playlist…"
    "nl" -> "Playlist wordt geïmporteerd…"
    "pl" -> "Importowanie playlisty…"
    "ro" -> "Se importă playlistul…"
    "el" -> "Γίνεται εισαγωγή της playlist…"
    "sv" -> "Importerar spellista…"
    "da" -> "Importerer playliste…"
    "cs" -> "Importuje se playlist…"
    "uk" -> "Імпорт плейлиста…"
    "ru" -> "Импорт плейлиста…"
    "tr" -> "Çalma listesi içe aktarılıyor…"
    "ar" -> "جارٍ استيراد قائمة التشغيل…"
    "zh" -> "正在导入播放列表…"
    "ja" -> "プレイリストをインポート中…"
    "ko" -> "플레이리스트를 가져오는 중…"
    "hi" -> "प्लेलिस्ट आयात की जा रही है…"
    "id" -> "Mengimpor playlist…"
    "vi" -> "Đang nhập playlist…"
    "th" -> "กำลังนำเข้าเพลย์ลิสต์…"
    "fil" -> "Ini-import ang playlist…"
    "he" -> "מייבא פלייליסט…"
    else -> "Importing playlist…"
}

fun playlistImportAlreadyRunningMessage(code: String): String = when (code) {
    "it" -> "Un’importazione è già in corso."
    "es" -> "Ya hay una importación en curso."
    "fr" -> "Une importation est déjà en cours."
    "de" -> "Es läuft bereits ein Import."
    "pt" -> "Já existe uma importação em curso."
    "nl" -> "Er wordt al een playlist geïmporteerd."
    "pl" -> "Import playlisty już trwa."
    "ro" -> "Un import este deja în curs."
    "el" -> "Μια εισαγωγή βρίσκεται ήδη σε εξέλιξη."
    "sv" -> "En import pågår redan."
    "da" -> "En import er allerede i gang."
    "cs" -> "Import již probíhá."
    "uk" -> "Імпорт уже виконується."
    "ru" -> "Импорт уже выполняется."
    "tr" -> "Zaten devam eden bir içe aktarma var."
    "ar" -> "هناك عملية استيراد قيد التنفيذ بالفعل."
    "zh" -> "已有导入任务正在进行。"
    "ja" -> "すでにインポートを実行中です。"
    "ko" -> "이미 가져오기가 진행 중입니다."
    "hi" -> "एक आयात पहले से चल रहा है।"
    "id" -> "Proses impor sedang berlangsung."
    "vi" -> "Một quá trình nhập đang diễn ra."
    "th" -> "มีการนำเข้าอยู่แล้ว"
    "fil" -> "May kasalukuyan nang pag-import."
    "he" -> "ייבוא כבר מתבצע."
    else -> "An import is already in progress."
}

fun playlistImportSuccessMessage(code: String, count: Int, playlistName: String): String = when (code) {
    "it" -> "Importati $count brani in $playlistName"
    "es" -> "Se importaron $count canciones a $playlistName"
    "fr" -> "$count titres importés dans $playlistName"
    "de" -> "$count Titel in $playlistName importiert"
    "pt" -> "$count faixas importadas para $playlistName"
    "nl" -> "$count nummers geïmporteerd in $playlistName"
    "pl" -> "Zaimportowano $count utworów do $playlistName"
    "ro" -> "Au fost importate $count piese în $playlistName"
    "el" -> "Εισήχθησαν $count κομμάτια στο $playlistName"
    "sv" -> "$count låtar importerades till $playlistName"
    "da" -> "$count numre importeret til $playlistName"
    "cs" -> "Do $playlistName bylo importováno $count skladeb"
    "uk" -> "Імпортовано $count композицій у $playlistName"
    "ru" -> "Импортировано $count треков в $playlistName"
    "tr" -> "$playlistName içine $count parça aktarıldı"
    "ar" -> "تم استيراد $count مقطعًا إلى $playlistName"
    "zh" -> "已将 $count 首歌曲导入 $playlistName"
    "ja" -> "$playlistName に $count 曲をインポートしました"
    "ko" -> "${playlistName}에 ${count}곡을 가져왔습니다"
    "hi" -> "$playlistName में $count गाने आयात किए गए"
    "id" -> "$count lagu diimpor ke $playlistName"
    "vi" -> "Đã nhập $count bài hát vào $playlistName"
    "th" -> "นำเข้า $count เพลงไปยัง $playlistName แล้ว"
    "fil" -> "Na-import ang $count kanta sa $playlistName"
    "he" -> "יובאו $count שירים אל $playlistName"
    else -> "Imported $count tracks into $playlistName"
}

fun playlistImportSuccessMessage(
    code: String,
    importedCount: Int,
    requestedCount: Int,
    playlistName: String
): String {
    if (requestedCount <= importedCount) return playlistImportSuccessMessage(code, importedCount, playlistName)
    return when (code) {
        "it" -> "Importati $importedCount di $requestedCount brani in $playlistName"
        "es" -> "Se importaron $importedCount de $requestedCount canciones a $playlistName"
        "fr" -> "$importedCount titres sur $requestedCount importés dans $playlistName"
        "de" -> "$importedCount von $requestedCount Titeln in $playlistName importiert"
        "pt" -> "$importedCount de $requestedCount faixas importadas para $playlistName"
        "nl" -> "$importedCount van $requestedCount nummers geïmporteerd in $playlistName"
        "pl" -> "Zaimportowano $importedCount z $requestedCount utworów do $playlistName"
        "ro" -> "Au fost importate $importedCount din $requestedCount piese în $playlistName"
        "el" -> "Εισήχθησαν $importedCount από $requestedCount κομμάτια στο $playlistName"
        "sv" -> "$importedCount av $requestedCount låtar importerades till $playlistName"
        "da" -> "$importedCount af $requestedCount numre importeret til $playlistName"
        "cs" -> "Do $playlistName bylo importováno $importedCount z $requestedCount skladeb"
        "uk" -> "Імпортовано $importedCount із $requestedCount композицій у $playlistName"
        "ru" -> "Импортировано $importedCount из $requestedCount треков в $playlistName"
        "tr" -> "$requestedCount parçadan $importedCount tanesi $playlistName içine aktarıldı"
        "ar" -> "تم استيراد $importedCount من أصل $requestedCount مقطعًا إلى $playlistName"
        "zh" -> "已将 $requestedCount 首中的 $importedCount 首导入 $playlistName"
        "ja" -> "$requestedCount 曲中 $importedCount 曲を $playlistName にインポートしました"
        "ko" -> "${requestedCount}곡 중 ${importedCount}곡을 ${playlistName}에 가져왔습니다"
        "hi" -> "$requestedCount में से $importedCount गाने $playlistName में आयात किए गए"
        "id" -> "$importedCount dari $requestedCount lagu diimpor ke $playlistName"
        "vi" -> "Đã nhập $importedCount trong số $requestedCount bài hát vào $playlistName"
        "th" -> "นำเข้า $importedCount จาก $requestedCount เพลงไปยัง $playlistName แล้ว"
        "fil" -> "Na-import ang $importedCount sa $requestedCount kanta sa $playlistName"
        "he" -> "יובאו $importedCount מתוך $requestedCount שירים אל $playlistName"
        else -> "Imported $importedCount of $requestedCount tracks into $playlistName"
    }
}

fun playlistImportFailureMessage(code: String): String = when (code) {
    "it" -> "Importazione non riuscita. Controlla il link o il backup e riprova."
    "es" -> "No se pudo importar. Comprueba el enlace o la copia de seguridad e inténtalo de nuevo."
    "fr" -> "L’importation a échoué. Vérifiez le lien ou la sauvegarde, puis réessayez."
    "de" -> "Import fehlgeschlagen. Prüfe den Link oder die Sicherung und versuche es erneut."
    "pt" -> "Não foi possível importar. Verifique o link ou a cópia de segurança e tente novamente."
    "nl" -> "Importeren mislukt. Controleer de link of back-up en probeer het opnieuw."
    "pl" -> "Import nie powiódł się. Sprawdź link lub kopię zapasową i spróbuj ponownie."
    "ro" -> "Importul nu a reușit. Verifică linkul sau copia de siguranță și încearcă din nou."
    "el" -> "Η εισαγωγή απέτυχε. Έλεγξε τον σύνδεσμο ή το αντίγραφο ασφαλείας και δοκίμασε ξανά."
    "sv" -> "Importen misslyckades. Kontrollera länken eller säkerhetskopian och försök igen."
    "da" -> "Importen mislykkedes. Kontrollér linket eller sikkerhedskopien, og prøv igen."
    "cs" -> "Import se nezdařil. Zkontrolujte odkaz nebo zálohu a zkuste to znovu."
    "uk" -> "Не вдалося імпортувати. Перевірте посилання або резервну копію й спробуйте ще раз."
    "ru" -> "Не удалось импортировать. Проверьте ссылку или резервную копию и повторите попытку."
    "tr" -> "İçe aktarma başarısız oldu. Bağlantıyı veya yedeği kontrol edip tekrar deneyin."
    "ar" -> "تعذّر الاستيراد. تحقّق من الرابط أو النسخة الاحتياطية ثم حاول مرة أخرى."
    "zh" -> "导入失败。请检查链接或备份内容后重试。"
    "ja" -> "インポートできませんでした。リンクまたはバックアップを確認して、もう一度お試しください。"
    "ko" -> "가져오기에 실패했습니다. 링크 또는 백업을 확인한 후 다시 시도하세요."
    "hi" -> "आयात नहीं हो सका। लिंक या बैकअप जाँचें और फिर से कोशिश करें।"
    "id" -> "Impor gagal. Periksa tautan atau cadangan lalu coba lagi."
    "vi" -> "Không thể nhập. Hãy kiểm tra liên kết hoặc bản sao lưu rồi thử lại."
    "th" -> "นำเข้าไม่สำเร็จ โปรดตรวจสอบลิงก์หรือข้อมูลสำรองแล้วลองอีกครั้ง"
    "fil" -> "Hindi na-import. Suriin ang link o backup at subukan ulit."
    "he" -> "הייבוא נכשל. בדקו את הקישור או את הגיבוי ונסו שוב."
    else -> "Import failed. Check the link or backup and try again."
}

private data class PlaylistImportFailureCopy(
    val invalidInput: String,
    val notAvailable: String,
    val tooLarge: String,
    val noMatches: String,
    val network: String,
    val providerChanged: String,
    val storage: String,
    val dismiss: String
)

private fun playlistImportFailureCopy(code: String): PlaylistImportFailureCopy = when (code) {
    "it" -> PlaylistImportFailureCopy("Il link o il backup non è riconosciuto. Controllalo e riprova.", "Non riesco a leggere questa playlist. Verifica che sia pubblica e accessibile.", "Questa playlist supera il limite di importazione supportato{limit}.", "Nessun brano compatibile è stato riconosciuto nel catalogo Levyra.", "Errore di rete durante l’importazione. Controlla la connessione e riprova.", "Il servizio musicale ha restituito un formato non riconosciuto. Riprova più tardi o usa un backup compatibile.", "Non è stato possibile salvare la playlist. Riprova.", "Nascondi suggerimento di importazione")
    "es" -> PlaylistImportFailureCopy("No se reconoce el enlace o la copia de seguridad. Compruébalo e inténtalo de nuevo.", "No se puede leer esta playlist. Comprueba que sea pública y accesible.", "Esta playlist supera el límite de importación admitido{limit}.", "No se pudo identificar ninguna canción compatible en el catálogo de Levyra.", "Se produjo un error de red durante la importación. Comprueba la conexión e inténtalo de nuevo.", "El servicio de música devolvió un formato no reconocido. Inténtalo más tarde o utiliza una copia de seguridad compatible.", "No se pudo guardar la playlist. Inténtalo de nuevo.", "Ocultar sugerencia de importación")
    "fr" -> PlaylistImportFailureCopy("Le lien ou la sauvegarde n’est pas reconnu. Vérifiez-le puis réessayez.", "Impossible de lire cette playlist. Vérifiez qu’elle est publique et accessible.", "Cette playlist dépasse la limite d’importation prise en charge{limit}.", "Aucun titre compatible n’a pu être identifié dans le catalogue Levyra.", "Une erreur réseau s’est produite pendant l’importation. Vérifiez votre connexion puis réessayez.", "Le service musical a renvoyé un format non reconnu. Réessayez plus tard ou utilisez une sauvegarde compatible.", "Impossible d’enregistrer la playlist. Réessayez.", "Masquer la suggestion d’importation")
    "de" -> PlaylistImportFailureCopy("Der Link oder die Sicherung wird nicht erkannt. Prüfe die Eingabe und versuche es erneut.", "Diese Playlist kann nicht gelesen werden. Prüfe, ob sie öffentlich und erreichbar ist.", "Diese Playlist überschreitet das unterstützte Importlimit{limit}.", "Im Levyra-Katalog konnten keine passenden Titel erkannt werden.", "Beim Import ist ein Netzwerkfehler aufgetreten. Prüfe die Verbindung und versuche es erneut.", "Der Musikdienst hat ein unbekanntes Format geliefert. Versuche es später erneut oder nutze eine kompatible Sicherung.", "Die Playlist konnte nicht gespeichert werden. Versuche es erneut.", "Importhinweis ausblenden")
    "pt" -> PlaylistImportFailureCopy("O link ou a cópia de segurança não foi reconhecido. Verifique e tente novamente.", "Não foi possível ler esta playlist. Verifique se é pública e está acessível.", "Esta playlist excede o limite de importação suportado{limit}.", "Não foi possível identificar faixas compatíveis no catálogo Levyra.", "Ocorreu um erro de rede durante a importação. Verifique a ligação e tente novamente.", "O serviço de música devolveu um formato não reconhecido. Tente mais tarde ou use uma cópia de segurança compatível.", "Não foi possível guardar a playlist. Tente novamente.", "Ocultar sugestão de importação")
    "nl" -> PlaylistImportFailureCopy("De link of back-up wordt niet herkend. Controleer deze en probeer opnieuw.", "Deze playlist kan niet worden gelezen. Controleer of deze openbaar en bereikbaar is.", "Deze playlist is groter dan de ondersteunde importlimiet{limit}.", "Er zijn geen geschikte nummers in de Levyra-catalogus gevonden.", "Er is een netwerkfout opgetreden tijdens het importeren. Controleer je verbinding en probeer opnieuw.", "De muziekdienst stuurde een onbekend formaat terug. Probeer het later opnieuw of gebruik een compatibele back-up.", "De playlist kon niet worden opgeslagen. Probeer opnieuw.", "Importsuggestie verbergen")
    "pl" -> PlaylistImportFailureCopy("Link lub kopia zapasowa nie zostały rozpoznane. Sprawdź je i spróbuj ponownie.", "Nie można odczytać tej playlisty. Sprawdź, czy jest publiczna i dostępna.", "Ta playlista przekracza obsługiwany limit importu{limit}.", "Nie udało się dopasować żadnych zgodnych utworów w katalogu Levyra.", "Podczas importu wystąpił błąd sieci. Sprawdź połączenie i spróbuj ponownie.", "Serwis muzyczny zwrócił nierozpoznany format. Spróbuj później lub użyj zgodnej kopii zapasowej.", "Nie udało się zapisać playlisty. Spróbuj ponownie.", "Ukryj sugestię importu")
    "ro" -> PlaylistImportFailureCopy("Linkul sau copia de siguranță nu este recunoscută. Verifică și încearcă din nou.", "Acest playlist nu poate fi citit. Verifică dacă este public și accesibil.", "Acest playlist depășește limita de import acceptată{limit}.", "Nu au fost identificate piese compatibile în catalogul Levyra.", "A apărut o eroare de rețea în timpul importului. Verifică conexiunea și încearcă din nou.", "Serviciul muzical a returnat un format nerecunoscut. Încearcă mai târziu sau folosește o copie de siguranță compatibilă.", "Playlistul nu a putut fi salvat. Încearcă din nou.", "Ascunde sugestia de import")
    "el" -> PlaylistImportFailureCopy("Ο σύνδεσμος ή το αντίγραφο ασφαλείας δεν αναγνωρίζεται. Έλεγξέ το και δοκίμασε ξανά.", "Δεν είναι δυνατή η ανάγνωση αυτής της playlist. Βεβαιώσου ότι είναι δημόσια και προσβάσιμη.", "Αυτή η playlist υπερβαίνει το υποστηριζόμενο όριο εισαγωγής{limit}.", "Δεν εντοπίστηκαν συμβατά κομμάτια στον κατάλογο Levyra.", "Παρουσιάστηκε σφάλμα δικτύου κατά την εισαγωγή. Έλεγξε τη σύνδεσή σου και δοκίμασε ξανά.", "Η μουσική υπηρεσία επέστρεψε μη αναγνωρίσιμη μορφή. Δοκίμασε αργότερα ή χρησιμοποίησε συμβατό αντίγραφο ασφαλείας.", "Δεν ήταν δυνατή η αποθήκευση της playlist. Δοκίμασε ξανά.", "Απόκρυψη πρότασης εισαγωγής")
    "sv" -> PlaylistImportFailureCopy("Länken eller säkerhetskopian känns inte igen. Kontrollera den och försök igen.", "Spellistan kan inte läsas. Kontrollera att den är offentlig och tillgänglig.", "Spellistan överskrider den importgräns som stöds{limit}.", "Inga kompatibla låtar kunde matchas i Levyra-katalogen.", "Ett nätverksfel inträffade under importen. Kontrollera anslutningen och försök igen.", "Musiktjänsten returnerade ett okänt format. Försök senare eller använd en kompatibel säkerhetskopia.", "Spellistan kunde inte sparas. Försök igen.", "Dölj importförslaget")
    "da" -> PlaylistImportFailureCopy("Linket eller sikkerhedskopien genkendes ikke. Kontrollér den, og prøv igen.", "Denne playliste kan ikke læses. Kontrollér, at den er offentlig og tilgængelig.", "Denne playliste overskrider den understøttede importgrænse{limit}.", "Ingen kompatible numre kunne matches i Levyra-kataloget.", "Der opstod en netværksfejl under importen. Kontrollér forbindelsen, og prøv igen.", "Musiktjenesten returnerede et ukendt format. Prøv igen senere, eller brug en kompatibel sikkerhedskopi.", "Playlisten kunne ikke gemmes. Prøv igen.", "Skjul importforslaget")
    "cs" -> PlaylistImportFailureCopy("Odkaz nebo záloha nebyly rozpoznány. Zkontrolujte je a zkuste to znovu.", "Tento playlist nelze načíst. Ověřte, že je veřejný a dostupný.", "Tento playlist překračuje podporovaný limit importu{limit}.", "V katalogu Levyra nebyly nalezeny žádné odpovídající skladby.", "Během importu došlo k chybě sítě. Zkontrolujte připojení a zkuste to znovu.", "Hudební služba vrátila nerozpoznaný formát. Zkuste to později nebo použijte kompatibilní zálohu.", "Playlist se nepodařilo uložit. Zkuste to znovu.", "Skrýt návrh importu")
    "uk" -> PlaylistImportFailureCopy("Посилання або резервну копію не розпізнано. Перевірте їх і спробуйте ще раз.", "Не вдається прочитати цей плейлист. Переконайтеся, що він загальнодоступний і доступний.", "Цей плейлист перевищує підтримуваний ліміт імпорту{limit}.", "У каталозі Levyra не вдалося знайти сумісні композиції.", "Під час імпорту сталася помилка мережі. Перевірте з’єднання й спробуйте ще раз.", "Музичний сервіс повернув нерозпізнаний формат. Спробуйте пізніше або скористайтеся сумісною резервною копією.", "Не вдалося зберегти плейлист. Спробуйте ще раз.", "Сховати підказку імпорту")
    "ru" -> PlaylistImportFailureCopy("Ссылка или резервная копия не распознаны. Проверьте их и повторите попытку.", "Не удаётся прочитать этот плейлист. Убедитесь, что он общедоступен и доступен.", "Этот плейлист превышает поддерживаемый лимит импорта{limit}.", "В каталоге Levyra не удалось найти подходящие треки.", "Во время импорта произошла сетевая ошибка. Проверьте подключение и повторите попытку.", "Музыкальный сервис вернул неизвестный формат. Попробуйте позже или используйте совместимую резервную копию.", "Не удалось сохранить плейлист. Повторите попытку.", "Скрыть подсказку импорта")
    "tr" -> PlaylistImportFailureCopy("Bağlantı veya yedek tanınmadı. Kontrol edip tekrar deneyin.", "Bu çalma listesi okunamıyor. Herkese açık ve erişilebilir olduğundan emin olun.", "Bu çalma listesi desteklenen içe aktarma sınırını aşıyor{limit}.", "Levyra kataloğunda eşleşen uyumlu parça bulunamadı.", "İçe aktarma sırasında ağ hatası oluştu. Bağlantınızı kontrol edip tekrar deneyin.", "Müzik servisi tanınmayan bir biçim döndürdü. Daha sonra tekrar deneyin veya uyumlu bir yedek kullanın.", "Çalma listesi kaydedilemedi. Tekrar deneyin.", "İçe aktarma önerisini gizle")
    "ar" -> PlaylistImportFailureCopy("لم يتم التعرّف على الرابط أو النسخة الاحتياطية. تحقّق منهما وحاول مرة أخرى.", "تعذّرت قراءة قائمة التشغيل. تأكد من أنها عامة ومتاحة.", "تتجاوز قائمة التشغيل حد الاستيراد المدعوم{limit}.", "لم يتم العثور على مقاطع متوافقة في كتالوج Levyra.", "حدث خطأ في الشبكة أثناء الاستيراد. تحقّق من اتصالك وحاول مرة أخرى.", "أعادت خدمة الموسيقى تنسيقًا غير معروف. حاول لاحقًا أو استخدم نسخة احتياطية متوافقة.", "تعذّر حفظ قائمة التشغيل. حاول مرة أخرى.", "إخفاء اقتراح الاستيراد")
    "zh" -> PlaylistImportFailureCopy("无法识别该链接或备份。请检查后重试。", "无法读取此播放列表。请确认它是公开且可访问的。", "此播放列表超过支持的导入限制{limit}。", "无法在 Levyra 曲库中匹配到兼容歌曲。", "导入时发生网络错误。请检查网络连接后重试。", "音乐服务返回了无法识别的格式。请稍后重试或使用兼容备份。", "无法保存播放列表。请重试。", "隐藏导入提示")
    "ja" -> PlaylistImportFailureCopy("リンクまたはバックアップを認識できません。内容を確認して、もう一度お試しください。", "このプレイリストを読み込めません。公開されていてアクセス可能か確認してください。", "このプレイリストは対応しているインポート上限を超えています{limit}。", "Levyra カタログで一致する互換トラックが見つかりませんでした。", "インポート中にネットワークエラーが発生しました。接続を確認して、もう一度お試しください。", "音楽サービスから認識できない形式が返されました。後でもう一度試すか、互換バックアップを使用してください。", "プレイリストを保存できませんでした。もう一度お試しください。", "インポートの案内を非表示")
    "ko" -> PlaylistImportFailureCopy("링크 또는 백업을 인식할 수 없습니다. 확인한 후 다시 시도하세요.", "이 플레이리스트를 읽을 수 없습니다. 공개 상태이며 접근 가능한지 확인하세요.", "이 플레이리스트는 지원되는 가져오기 한도를 초과합니다{limit}.", "Levyra 카탈로그에서 일치하는 호환 곡을 찾지 못했습니다.", "가져오는 중 네트워크 오류가 발생했습니다. 연결을 확인한 후 다시 시도하세요.", "음악 서비스가 인식할 수 없는 형식을 반환했습니다. 나중에 다시 시도하거나 호환 백업을 사용하세요.", "플레이리스트를 저장할 수 없습니다. 다시 시도하세요.", "가져오기 안내 숨기기")
    "hi" -> PlaylistImportFailureCopy("लिंक या बैकअप पहचाना नहीं गया। इसे जाँचें और फिर से कोशिश करें।", "यह प्लेलिस्ट पढ़ी नहीं जा सकती। सुनिश्चित करें कि यह सार्वजनिक और उपलब्ध है।", "यह प्लेलिस्ट समर्थित आयात सीमा से बड़ी है{limit}।", "Levyra कैटलॉग में कोई संगत गाना मेल नहीं खाया।", "आयात के दौरान नेटवर्क त्रुटि हुई। कनेक्शन जाँचें और फिर से कोशिश करें।", "संगीत सेवा ने अपरिचित प्रारूप लौटाया। बाद में फिर कोशिश करें या संगत बैकअप का उपयोग करें।", "प्लेलिस्ट सहेजी नहीं जा सकी। फिर से कोशिश करें।", "आयात सुझाव छिपाएँ")
    "id" -> PlaylistImportFailureCopy("Tautan atau cadangan tidak dikenali. Periksa lalu coba lagi.", "Playlist ini tidak dapat dibaca. Pastikan playlist bersifat publik dan dapat diakses.", "Playlist ini melebihi batas impor yang didukung{limit}.", "Tidak ada lagu kompatibel yang cocok di katalog Levyra.", "Terjadi kesalahan jaringan saat mengimpor. Periksa koneksi lalu coba lagi.", "Layanan musik mengembalikan format yang tidak dikenali. Coba lagi nanti atau gunakan cadangan yang kompatibel.", "Playlist tidak dapat disimpan. Coba lagi.", "Sembunyikan saran impor")
    "vi" -> PlaylistImportFailureCopy("Không nhận dạng được liên kết hoặc bản sao lưu. Hãy kiểm tra rồi thử lại.", "Không thể đọc playlist này. Hãy đảm bảo playlist ở chế độ công khai và có thể truy cập.", "Playlist này vượt quá giới hạn nhập được hỗ trợ{limit}.", "Không tìm thấy bài hát tương thích phù hợp trong danh mục Levyra.", "Đã xảy ra lỗi mạng khi nhập. Hãy kiểm tra kết nối rồi thử lại.", "Dịch vụ nhạc trả về định dạng không nhận dạng được. Hãy thử lại sau hoặc dùng bản sao lưu tương thích.", "Không thể lưu playlist. Hãy thử lại.", "Ẩn gợi ý nhập")
    "th" -> PlaylistImportFailureCopy("ไม่รู้จักลิงก์หรือข้อมูลสำรอง โปรดตรวจสอบแล้วลองอีกครั้ง", "ไม่สามารถอ่านเพลย์ลิสต์นี้ได้ โปรดตรวจสอบว่าเป็นสาธารณะและเข้าถึงได้", "เพลย์ลิสต์นี้เกินขีดจำกัดการนำเข้าที่รองรับ{limit}", "ไม่พบเพลงที่เข้ากันได้ในแค็ตตาล็อก Levyra", "เกิดข้อผิดพลาดของเครือข่ายระหว่างนำเข้า โปรดตรวจสอบการเชื่อมต่อแล้วลองอีกครั้ง", "บริการเพลงส่งรูปแบบที่ไม่รู้จัก โปรดลองอีกครั้งภายหลังหรือใช้ข้อมูลสำรองที่เข้ากันได้", "ไม่สามารถบันทึกเพลย์ลิสต์ได้ โปรดลองอีกครั้ง", "ซ่อนคำแนะนำการนำเข้า")
    "fil" -> PlaylistImportFailureCopy("Hindi nakilala ang link o backup. Suriin ito at subukan ulit.", "Hindi mabasa ang playlist na ito. Tiyaking pampubliko at naa-access ito.", "Lampas ang playlist na ito sa suportadong limitasyon ng pag-import{limit}.", "Walang tumugmang compatible na kanta sa catalog ng Levyra.", "Nagkaroon ng network error habang nag-i-import. Suriin ang koneksyon at subukan ulit.", "Nagbalik ang music service ng hindi makilalang format. Subukan mamaya o gumamit ng compatible na backup.", "Hindi ma-save ang playlist. Subukan ulit.", "Itago ang mungkahi sa pag-import")
    "he" -> PlaylistImportFailureCopy("הקישור או הגיבוי לא זוהו. בדקו אותם ונסו שוב.", "לא ניתן לקרוא את הפלייליסט הזה. ודאו שהוא ציבורי ונגיש.", "הפלייליסט חורג ממגבלת הייבוא הנתמכת{limit}.", "לא נמצאו שירים תואמים בקטלוג Levyra.", "אירעה שגיאת רשת במהלך הייבוא. בדקו את החיבור ונסו שוב.", "שירות המוזיקה החזיר פורמט לא מזוהה. נסו שוב מאוחר יותר או השתמשו בגיבוי תואם.", "לא ניתן לשמור את הפלייליסט. נסו שוב.", "הסתרת הצעת הייבוא")
    else -> PlaylistImportFailureCopy("The link or backup is not recognized. Check it and try again.", "This playlist cannot be read. Make sure it is public and accessible.", "This playlist exceeds the supported import limit{limit}.", "No compatible tracks could be matched in the Levyra catalog.", "A network error occurred while importing. Check your connection and try again.", "The music service returned an unrecognized format. Try again later or use a compatible backup.", "The playlist could not be saved. Try again.", "Hide import suggestion")
}

fun playlistImportFailureMessage(
    code: String,
    kind: PlaylistImportFailureKind,
    limit: Int? = null
): String {
    val copy = playlistImportFailureCopy(code)
    val raw = when (kind) {
        PlaylistImportFailureKind.INVALID_INPUT -> copy.invalidInput
        PlaylistImportFailureKind.NOT_AVAILABLE -> copy.notAvailable
        PlaylistImportFailureKind.TOO_LARGE -> copy.tooLarge
        PlaylistImportFailureKind.NO_MATCHES -> copy.noMatches
        PlaylistImportFailureKind.NETWORK -> copy.network
        PlaylistImportFailureKind.PROVIDER_CHANGED -> copy.providerChanged
        PlaylistImportFailureKind.STORAGE -> copy.storage
    }
    val limitText = limit?.let { value ->
        when (code) {
            "it" -> ": massimo $value brani"
            "es" -> ": máximo $value canciones"
            "fr" -> " : $value titres maximum"
            "de" -> ": maximal $value Titel"
            "pt" -> ": máximo de $value faixas"
            "nl" -> ": maximaal $value nummers"
            "pl" -> ": maksymalnie $value utworów"
            "ro" -> ": maximum $value piese"
            "el" -> ": έως $value κομμάτια"
            "sv" -> ": högst $value låtar"
            "da" -> ": højst $value numre"
            "cs" -> ": nejvýše $value skladeb"
            "uk" -> ": максимум $value композицій"
            "ru" -> ": максимум $value треков"
            "tr" -> ": en fazla $value parça"
            "ar" -> ": بحد أقصى $value مقطعًا"
            "zh" -> "：最多 $value 首歌曲"
            "ja" -> "：最大 $value 曲"
            "ko" -> ": 최대 ${value}곡"
            "hi" -> ": अधिकतम $value गाने"
            "id" -> ": maksimum $value lagu"
            "vi" -> ": tối đa $value bài hát"
            "th" -> ": สูงสุด $value เพลง"
            "fil" -> ": hanggang $value kanta"
            "he" -> ": עד $value שירים"
            else -> ": maximum $value tracks"
        }
    }.orEmpty()
    return raw.replace("{limit}", limitText)
}

fun playlistImportDismissMessage(code: String): String = playlistImportFailureCopy(code).dismiss
