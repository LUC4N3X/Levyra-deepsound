package com.luc4n3x.levyra.ui.i18n

internal val offlineStorageKeys = setOf(
    "offlineSavedForListening",
    "offlineStorageFull",
    "offlineRestoreFolderAccess",
    "offlineFolderAccessMismatch"
)

private fun offlineStorage(saved: String, storageFull: String, restoreAccess: String, accessMismatch: String): Map<String, String> = mapOf(
    "offlineSavedForListening" to saved,
    "offlineStorageFull" to storageFull,
    "offlineRestoreFolderAccess" to restoreAccess,
    "offlineFolderAccessMismatch" to accessMismatch
)

private val offlineStorageBundles: Map<String, Map<String, String>> = mapOf(
    "en" to offlineStorage("Saved for offline listening", "Not enough storage space. Free up some space and try again.", "Restore folder access", "Choose the same folder this download was saved in."),
    "it" to offlineStorage("Salvato per l'ascolto offline", "Spazio di archiviazione insufficiente. Libera spazio e riprova.", "Ripristina l'accesso alla cartella", "Scegli la stessa cartella in cui è stato salvato questo download."),
    "es" to offlineStorage("Guardado para escuchar sin conexión", "No hay suficiente espacio de almacenamiento. Libera espacio e inténtalo de nuevo.", "Restaurar acceso a la carpeta", "Elige la misma carpeta donde se guardó esta descarga."),
    "fr" to offlineStorage("Enregistré pour l'écoute hors ligne", "Espace de stockage insuffisant. Libérez de l'espace et réessayez.", "Rétablir l'accès au dossier", "Choisissez le dossier dans lequel ce téléchargement a été enregistré."),
    "de" to offlineStorage("Zum Offline-Hören gespeichert", "Nicht genügend Speicherplatz. Gib Speicher frei und versuche es erneut.", "Ordnerzugriff wiederherstellen", "Wähle denselben Ordner, in dem dieser Download gespeichert wurde."),
    "pt" to offlineStorage("Guardado para ouvir offline", "Espaço de armazenamento insuficiente. Liberte espaço e tente novamente.", "Restaurar acesso à pasta", "Escolha a mesma pasta onde esta transferência foi guardada."),
    "nl" to offlineStorage("Opgeslagen om offline te luisteren", "Onvoldoende opslagruimte. Maak ruimte vrij en probeer het opnieuw.", "Maptoegang herstellen", "Kies dezelfde map waarin deze download is opgeslagen."),
    "pl" to offlineStorage("Zapisano do słuchania offline", "Za mało miejsca w pamięci. Zwolnij miejsce i spróbuj ponownie.", "Przywróć dostęp do folderu", "Wybierz ten sam folder, w którym zapisano to pobranie."),
    "ro" to offlineStorage("Salvat pentru ascultare offline", "Spațiu de stocare insuficient. Eliberează spațiu și încearcă din nou.", "Restabilește accesul la folder", "Alege același folder în care a fost salvată această descărcare."),
    "el" to offlineStorage("Αποθηκεύτηκε για ακρόαση εκτός σύνδεσης", "Δεν υπάρχει αρκετός αποθηκευτικός χώρος. Ελευθέρωσε χώρο και δοκίμασε ξανά.", "Επαναφορά πρόσβασης στον φάκελο", "Επίλεξε τον ίδιο φάκελο όπου αποθηκεύτηκε αυτή η λήψη."),
    "sv" to offlineStorage("Sparad för offlinelyssning", "Inte tillräckligt med lagringsutrymme. Frigör utrymme och försök igen.", "Återställ mappåtkomst", "Välj samma mapp som nedladdningen sparades i."),
    "da" to offlineStorage("Gemt til offline-lytning", "Ikke nok lagerplads. Frigør plads, og prøv igen.", "Gendan adgang til mappen", "Vælg den samme mappe, som denne download blev gemt i."),
    "cs" to offlineStorage("Uloženo pro poslech offline", "Nedostatek místa v úložišti. Uvolněte místo a zkuste to znovu.", "Obnovit přístup ke složce", "Vyberte stejnou složku, do které bylo toto stažení uloženo."),
    "uk" to offlineStorage("Збережено для прослуховування офлайн", "Недостатньо місця у сховищі. Звільни місце та спробуй ще раз.", "Відновити доступ до папки", "Виберіть ту саму папку, у якій збережено це завантаження."),
    "ru" to offlineStorage("Сохранено для прослушивания офлайн", "Недостаточно места в хранилище. Освободи место и повтори попытку.", "Восстановить доступ к папке", "Выберите ту же папку, в которой сохранена эта загрузка."),
    "tr" to offlineStorage("Çevrimdışı dinleme için kaydedildi", "Yeterli depolama alanı yok. Yer açıp yeniden dene.", "Klasör erişimini geri yükle", "Bu indirmenin kaydedildiği klasörün aynısını seçin."),
    "ar" to offlineStorage("تم الحفظ للاستماع دون اتصال", "مساحة التخزين غير كافية. حرّر بعض المساحة وحاول مرة أخرى.", "استعادة الوصول إلى المجلد", "اختر المجلد نفسه الذي حُفظ فيه هذا التنزيل."),
    "zh" to offlineStorage("已保存，可离线收听", "存储空间不足。请释放一些空间后重试。", "恢复文件夹访问权限", "请选择保存此下载内容的同一文件夹。"),
    "ja" to offlineStorage("オフライン再生用に保存しました", "ストレージの空き容量が不足しています。空き容量を増やしてもう一度お試しください。", "フォルダへのアクセスを復元", "このダウンロードを保存したフォルダと同じフォルダを選択してください。"),
    "ko" to offlineStorage("오프라인 감상용으로 저장됨", "저장 공간이 부족합니다. 공간을 확보한 후 다시 시도하세요.", "폴더 액세스 복원", "이 다운로드가 저장된 폴더와 같은 폴더를 선택하세요."),
    "hi" to offlineStorage("ऑफ़लाइन सुनने के लिए सहेजा गया", "पर्याप्त स्टोरेज नहीं है। कुछ जगह खाली करके फिर से कोशिश करें।", "फ़ोल्डर की पहुँच बहाल करें", "वही फ़ोल्डर चुनें जिसमें यह डाउनलोड सहेजा गया था।"),
    "id" to offlineStorage("Disimpan untuk didengarkan offline", "Ruang penyimpanan tidak cukup. Kosongkan ruang lalu coba lagi.", "Pulihkan akses folder", "Pilih folder yang sama tempat unduhan ini disimpan."),
    "vi" to offlineStorage("Đã lưu để nghe ngoại tuyến", "Không đủ dung lượng lưu trữ. Hãy giải phóng dung lượng rồi thử lại.", "Khôi phục quyền truy cập thư mục", "Hãy chọn đúng thư mục đã lưu bản tải xuống này."),
    "th" to offlineStorage("บันทึกไว้ฟังแบบออฟไลน์แล้ว", "พื้นที่เก็บข้อมูลไม่เพียงพอ โปรดเพิ่มพื้นที่ว่างแล้วลองอีกครั้ง", "กู้คืนสิทธิ์เข้าถึงโฟลเดอร์", "เลือกโฟลเดอร์เดียวกับที่บันทึกการดาวน์โหลดนี้ไว้"),
    "fil" to offlineStorage("Na-save para sa offline na pakikinig", "Kulang ang storage space. Magbakante ng espasyo at subukang muli.", "Ibalik ang access sa folder", "Piliin ang parehong folder kung saan na-save ang download na ito."),
    "he" to offlineStorage("נשמר להאזנה במצב לא מקוון", "אין מספיק שטח אחסון. פנו מקום ונסו שוב.", "שחזור הגישה לתיקייה", "בחרו את אותה תיקייה שבה נשמרה ההורדה הזו."),
    "fi" to offlineStorage("Tallennettu offline-kuuntelua varten", "Tallennustila ei riitä. Vapauta tilaa ja yritä uudelleen.", "Palauta kansion käyttöoikeus", "Valitse sama kansio, johon tämä lataus tallennettiin."),
    "et" to offlineStorage("Salvestatud võrguühenduseta kuulamiseks", "Salvestusruumi pole piisavalt. Vabastage ruumi ja proovige uuesti.", "Taasta kausta juurdepääs", "Valige sama kaust, kuhu see allalaadimine salvestati."),
    "zh-Hant" to offlineStorage("已儲存，可離線收聽", "儲存空間不足。請釋出一些空間後再試一次。", "恢復資料夾存取權", "請選擇儲存此下載內容的同一個資料夾。"),
    "hu" to offlineStorage("Mentve offline hallgatáshoz", "Nincs elég tárhely. Szabadíts fel helyet, majd próbáld újra.", "Mappahozzáférés visszaállítása", "Válaszd ki ugyanazt a mappát, ahová ez a letöltés mentve lett."),
    "bg" to offlineStorage("Запазено за слушане офлайн", "Няма достатъчно място за съхранение. Освободете място и опитайте отново.", "Възстановяване на достъпа до папката", "Изберете същата папка, в която е запазено това изтегляне."),
    "nb" to offlineStorage("Lagret for lytting uten nett", "Ikke nok lagringsplass. Frigjør plass og prøv igjen.", "Gjenopprett mappetilgang", "Velg den samme mappen som denne nedlastingen ble lagret i."),
    "ca" to offlineStorage("Desat per escoltar sense connexió", "No hi ha prou espai d'emmagatzematge. Allibera espai i torna-ho a provar.", "Restaura l'accés a la carpeta", "Tria la mateixa carpeta on es va desar aquesta baixada."),
    "bn" to offlineStorage("অফলাইনে শোনার জন্য সংরক্ষিত", "পর্যাপ্ত স্টোরেজ নেই। কিছু জায়গা খালি করে আবার চেষ্টা করুন।", "ফোল্ডারের অ্যাক্সেস ফিরিয়ে আনুন", "যে ফোল্ডারে এই ডাউনলোডটি সংরক্ষিত হয়েছিল সেটিই বেছে নিন।"),
    "kk" to offlineStorage("Офлайн тыңдау үшін сақталды", "Жад орны жеткіліксіз. Орын босатып, қайталап көріңіз.", "Қалтаға кіруді қалпына келтіру", "Осы жүктеп алу сақталған қалтаны таңдаңыз."),
    "sr" to offlineStorage("Сачувано за слушање ван мреже", "Нема довољно меморијског простора. Ослободите простор и покушајте поново.", "Врати приступ фасцикли", "Изаберите исту фасциклу у којој је ово преузимање сачувано."),
    "sq" to offlineStorage("U ruajt për dëgjim jashtë linje", "Hapësira e ruajtjes nuk mjafton. Liro hapësirë dhe provo përsëri.", "Rikthe qasjen në dosje", "Zgjidh të njëjtën dosje ku u ruajt ky shkarkim."),
    "hr" to offlineStorage("Spremljeno za slušanje izvan mreže", "Nema dovoljno prostora za pohranu. Oslobodite prostor i pokušajte ponovno.", "Vrati pristup mapi", "Odaberite istu mapu u koju je ovo preuzimanje spremljeno."),
    "sk" to offlineStorage("Uložené na počúvanie offline", "Nedostatok miesta v úložisku. Uvoľnite miesto a skúste to znova.", "Obnoviť prístup k priečinku", "Vyberte rovnaký priečinok, do ktorého bolo toto stiahnutie uložené."),
    "ms" to offlineStorage("Disimpan untuk didengar di luar talian", "Ruang storan tidak mencukupi. Kosongkan sedikit ruang dan cuba lagi.", "Pulihkan akses folder", "Pilih folder yang sama tempat muat turun ini disimpan."),
    "fa" to offlineStorage("برای شنیدن آفلاین ذخیره شد", "فضای ذخیره‌سازی کافی نیست. مقداری فضا آزاد کنید و دوباره تلاش کنید.", "بازیابی دسترسی به پوشه", "همان پوشه‌ای را انتخاب کنید که این دانلود در آن ذخیره شده است.")
)

internal fun offlineStorageLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(offlineStorageBundles, code)

internal fun offlineStorageLocalizationCodes(): Set<String> = supportedLocalizationCodes()
