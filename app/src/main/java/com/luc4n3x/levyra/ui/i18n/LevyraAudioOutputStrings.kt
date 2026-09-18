package com.luc4n3x.levyra.ui.i18n

private fun audioOutputStrings(title: String, subtitle: String): Map<String, String> = mapOf(
    "audioOutputAaudio" to title,
    "audioOutputAaudioSubtitle" to subtitle
)

private val audioOutputBundles: Map<String, Map<String, String>> = mapOf(
    "en" to audioOutputStrings("AAudio output (Oboe)", "Native low-latency output. Uses slightly more battery and falls back to the standard output automatically"),
    "it" to audioOutputStrings("Uscita AAudio (Oboe)", "Uscita nativa a bassa latenza. Consuma un po' più batteria e torna da sola all'uscita standard in caso di problemi"),
    "es" to audioOutputStrings("Salida AAudio (Oboe)", "Salida nativa de baja latencia. Consume algo más de batería y vuelve sola a la salida estándar si hay problemas"),
    "fr" to audioOutputStrings("Sortie AAudio (Oboe)", "Sortie native à faible latence. Consomme un peu plus de batterie et revient seule à la sortie standard en cas de problème"),
    "de" to audioOutputStrings("AAudio-Ausgabe (Oboe)", "Native Ausgabe mit niedriger Latenz. Braucht etwas mehr Akku und wechselt bei Problemen automatisch zur Standardausgabe"),
    "pt" to audioOutputStrings("Saída AAudio (Oboe)", "Saída nativa de baixa latência. Gasta um pouco mais de bateria e volta sozinha à saída padrão em caso de problemas"),
    "nl" to audioOutputStrings("AAudio-uitvoer (Oboe)", "Native uitvoer met lage latentie. Gebruikt iets meer batterij en valt bij problemen automatisch terug op de standaarduitvoer"),
    "pl" to audioOutputStrings("Wyjście AAudio (Oboe)", "Natywne wyjście o niskim opóźnieniu. Zużywa nieco więcej baterii i w razie problemów samo wraca do standardowego wyjścia"),
    "ro" to audioOutputStrings("Ieșire AAudio (Oboe)", "Ieșire nativă cu latență redusă. Consumă puțin mai multă baterie și revine automat la ieșirea standard dacă apar probleme"),
    "el" to audioOutputStrings("Έξοδος AAudio (Oboe)", "Εγγενής έξοδος χαμηλής καθυστέρησης. Καταναλώνει λίγο περισσότερη μπαταρία και επιστρέφει αυτόματα στην τυπική έξοδο σε περίπτωση προβλήματος"),
    "sv" to audioOutputStrings("AAudio-utgång (Oboe)", "Inbyggd utgång med låg latens. Drar lite mer batteri och går automatiskt tillbaka till standardutgången vid problem"),
    "da" to audioOutputStrings("AAudio-udgang (Oboe)", "Indbygget udgang med lav latenstid. Bruger lidt mere batteri og skifter automatisk tilbage til standardudgangen ved problemer"),
    "cs" to audioOutputStrings("Výstup AAudio (Oboe)", "Nativní výstup s nízkou latencí. Spotřebuje o něco více baterie a při potížích se sám vrátí na standardní výstup"),
    "uk" to audioOutputStrings("Вихід AAudio (Oboe)", "Нативний вихід із низькою затримкою. Витрачає трохи більше заряду й у разі проблем сам повертається до стандартного виходу"),
    "ru" to audioOutputStrings("Вывод AAudio (Oboe)", "Нативный вывод с низкой задержкой. Расходует чуть больше заряда и при проблемах сам возвращается к стандартному выводу"),
    "tr" to audioOutputStrings("AAudio çıkışı (Oboe)", "Düşük gecikmeli yerel çıkış. Biraz daha fazla pil kullanır ve sorun olursa otomatik olarak standart çıkışa döner"),
    "ar" to audioOutputStrings("إخراج AAudio (Oboe)", "إخراج أصلي منخفض التأخير. يستهلك بطارية أكثر قليلًا ويعود تلقائيًا إلى الإخراج القياسي عند حدوث مشكلة"),
    "zh" to audioOutputStrings("AAudio 输出（Oboe）", "低延迟原生输出。耗电略高，出现问题时会自动回退到标准输出"),
    "ja" to audioOutputStrings("AAudio 出力（Oboe）", "低遅延のネイティブ出力。電池の消費がやや増え、問題が起きると標準出力に自動で戻ります"),
    "ko" to audioOutputStrings("AAudio 출력(Oboe)", "저지연 네이티브 출력입니다. 배터리를 조금 더 사용하며 문제가 생기면 표준 출력으로 자동 전환됩니다"),
    "hi" to audioOutputStrings("AAudio आउटपुट (Oboe)", "कम विलंब वाला नेटिव आउटपुट। थोड़ी अधिक बैटरी लेता है और समस्या होने पर अपने-आप मानक आउटपुट पर लौट आता है"),
    "id" to audioOutputStrings("Output AAudio (Oboe)", "Output native berlatensi rendah. Sedikit lebih boros baterai dan otomatis kembali ke output standar jika ada masalah"),
    "vi" to audioOutputStrings("Đầu ra AAudio (Oboe)", "Đầu ra gốc độ trễ thấp. Tốn pin hơn một chút và tự động quay lại đầu ra tiêu chuẩn khi gặp sự cố"),
    "th" to audioOutputStrings("เอาต์พุต AAudio (Oboe)", "เอาต์พุตแบบเนทีฟที่มีความหน่วงต่ำ ใช้แบตเตอรี่มากขึ้นเล็กน้อย และจะกลับไปใช้เอาต์พุตมาตรฐานโดยอัตโนมัติเมื่อเกิดปัญหา"),
    "fil" to audioOutputStrings("AAudio na output (Oboe)", "Native na output na mababa ang latency. Bahagyang mas malakas sa baterya at kusang bumabalik sa karaniwang output kapag may problema"),
    "he" to audioOutputStrings("פלט AAudio (Oboe)", "פלט מקורי עם השהיה נמוכה. צורך מעט יותר סוללה וחוזר אוטומטית לפלט הרגיל במקרה של תקלה"),
    "sk" to audioOutputStrings("Výstup AAudio (Oboe)", "Natívny výstup s nízkou latenciou. Spotrebuje o niečo viac batérie a pri problémoch sa sám vráti na štandardný výstup"),
    "hr" to audioOutputStrings("AAudio izlaz (Oboe)", "Izvorni izlaz s niskom latencijom. Troši malo više baterije i u slučaju problema sam se vraća na standardni izlaz"),
    "bg" to audioOutputStrings("Изход AAudio (Oboe)", "Вграден изход с ниска латентност. Използва малко повече батерия и при проблем автоматично се връща към стандартния изход"),
    "hu" to audioOutputStrings("AAudio kimenet (Oboe)", "Natív, alacsony késleltetésű kimenet. Kicsit több akkumulátort használ, és hiba esetén automatikusan visszavált a normál kimenetre"),
    "fi" to audioOutputStrings("AAudio-ulostulo (Oboe)", "Natiivi matalan viiveen ulostulo. Kuluttaa hieman enemmän akkua ja palaa ongelmatilanteessa automaattisesti vakioulostuloon"),
    "nb" to audioOutputStrings("AAudio-utgang (Oboe)", "Innebygd utgang med lav forsinkelse. Bruker litt mer batteri og går automatisk tilbake til standardutgangen ved problemer"),
    "ca" to audioOutputStrings("Sortida AAudio (Oboe)", "Sortida nativa de baixa latència. Consumeix una mica més de bateria i torna sola a la sortida estàndard si hi ha problemes"),
    "fa" to audioOutputStrings("خروجی AAudio (Oboe)", "خروجی بومی با تأخیر کم. کمی باتری بیشتری مصرف می‌کند و در صورت بروز مشکل به‌طور خودکار به خروجی استاندارد برمی‌گردد"),
    "zh-Hant" to audioOutputStrings("AAudio 輸出（Oboe）", "低延遲原生輸出。耗電略高，發生問題時會自動退回標準輸出"),
    "ms" to audioOutputStrings("Output AAudio (Oboe)", "Output asli berkependaman rendah. Menggunakan sedikit lebih banyak bateri dan kembali ke output standard secara automatik jika berlaku masalah")
)

internal fun audioOutputLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(audioOutputBundles, code)

internal fun audioOutputLocalizationCodes(): Set<String> = audioOutputBundles.keys
