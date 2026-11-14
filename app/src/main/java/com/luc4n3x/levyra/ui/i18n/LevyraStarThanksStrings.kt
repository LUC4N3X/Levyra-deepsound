package com.luc4n3x.levyra.ui.i18n

internal data class StarThanksCopy(
    val title: String,
    val body: String,
    val star: String,
    val done: String,
    val logoDescription: String
)

private val starThanksCopies = mapOf(
    "en" to StarThanksCopy(
        "Levyra is free",
        "Every update takes a lot of time and work. I'm not asking for money: if you'd like to say thanks, leave a star on GitHub. If you already have, thank you so much. Happy listening.",
        "Leave a star",
        "I already did, thanks",
        "Levyra logo"
    ),
    "it" to StarThanksCopy(
        "Levyra è gratis",
        "Ogni aggiornamento richiede tanto tempo e lavoro. Non ti chiedo soldi: se vuoi dirmi grazie, lasciami una stella su GitHub. Se l'hai già fatto, grazie di cuore. Buon ascolto.",
        "Lascia una stella",
        "L'ho già fatto, grazie",
        "Logo di Levyra"
    ),
    "es" to StarThanksCopy(
        "Levyra es gratis",
        "Cada actualización lleva mucho tiempo y trabajo. No te pido dinero: si quieres darme las gracias, déjame una estrella en GitHub. Si ya lo hiciste, muchísimas gracias. Disfruta de la música.",
        "Dejar una estrella",
        "Ya lo hice, gracias",
        "Logo de Levyra"
    ),
    "fr" to StarThanksCopy(
        "Levyra est gratuite",
        "Chaque mise à jour demande beaucoup de temps et de travail. Je ne te demande pas d'argent : si tu veux me dire merci, laisse-moi une étoile sur GitHub. Si c'est déjà fait, merci infiniment. Bonne écoute.",
        "Laisser une étoile",
        "C'est déjà fait, merci",
        "Logo de Levyra"
    ),
    "de" to StarThanksCopy(
        "Levyra ist kostenlos",
        "Jedes Update kostet viel Zeit und Arbeit. Ich bitte nicht um Geld: Wenn du Danke sagen möchtest, lass mir einen Stern auf GitHub da. Falls du das schon getan hast, vielen herzlichen Dank. Viel Spaß beim Hören.",
        "Stern vergeben",
        "Schon erledigt, danke",
        "Levyra-Logo"
    ),
    "pt" to StarThanksCopy(
        "O Levyra é gratuito",
        "Cada atualização leva muito tempo e dá muito trabalho. Não te peço dinheiro: se me quiseres agradecer, deixa-me uma estrela no GitHub. Se já deixaste, muito obrigado. Aproveita a música.",
        "Deixar uma estrela",
        "Já deixei a minha estrela",
        "Logótipo do Levyra"
    ),
    "pt-BR" to StarThanksCopy(
        "O Levyra é gratuito",
        "Cada atualização leva muito tempo e dá muito trabalho. Não estou pedindo dinheiro: se quiser me agradecer, deixe uma estrela no GitHub. Se você já deixou, muito obrigado. Aproveite a música.",
        "Deixar uma estrela",
        "Já deixei minha estrela",
        "Logo do Levyra"
    ),
    "nl" to StarThanksCopy(
        "Levyra is gratis",
        "Elke update kost veel tijd en werk. Ik vraag je geen geld: wil je me bedanken, laat dan een ster achter op GitHub. Heb je dat al gedaan, dan heel erg bedankt. Veel luisterplezier.",
        "Geef een ster",
        "Al gedaan, bedankt",
        "Levyra-logo"
    ),
    "pl" to StarThanksCopy(
        "Levyra jest darmowa",
        "Każda aktualizacja to mnóstwo czasu i pracy. Nie proszę o pieniądze: jeśli chcesz mi podziękować, zostaw gwiazdkę na GitHubie. Jeśli gwiazdka już tam jest, z całego serca dziękuję. Miłego słuchania.",
        "Zostaw gwiazdkę",
        "Gwiazdka już jest, dzięki",
        "Logo Levyry"
    ),
    "ro" to StarThanksCopy(
        "Levyra este gratuită",
        "Fiecare actualizare cere mult timp și multă muncă. Nu îți cer bani: dacă vrei să-mi mulțumești, lasă-mi o stea pe GitHub. Dacă ai făcut-o deja, îți mulțumesc din suflet. Audiție plăcută.",
        "Lasă o stea",
        "Am făcut-o deja, mersi",
        "Logoul Levyra"
    ),
    "el" to StarThanksCopy(
        "Το Levyra είναι δωρεάν",
        "Κάθε ενημέρωση θέλει πολύ χρόνο και δουλειά. Δεν σου ζητάω χρήματα: αν θέλεις να μου πεις ευχαριστώ, άφησέ μου ένα αστέρι στο GitHub. Αν το έχεις ήδη κάνει, σε ευχαριστώ από καρδιάς. Καλή ακρόαση.",
        "Άφησε ένα αστέρι",
        "Το έκανα ήδη, ευχαριστώ",
        "Λογότυπο Levyra"
    ),
    "sv" to StarThanksCopy(
        "Levyra är gratis",
        "Varje uppdatering tar mycket tid och arbete. Jag ber inte om pengar: vill du tacka mig, lämna gärna en stjärna på GitHub. Har du redan gjort det, tack så hjärtligt. Trevlig lyssning.",
        "Ge en stjärna",
        "Redan gjort, tack",
        "Levyras logotyp"
    ),
    "da" to StarThanksCopy(
        "Levyra er gratis",
        "Hver opdatering tager meget tid og arbejde. Jeg beder ikke om penge: vil du sige tak, så giv mig en stjerne på GitHub. Har du allerede gjort det, så tusind tak. God lytning.",
        "Giv en stjerne",
        "Allerede gjort, tak",
        "Levyras logo"
    ),
    "cs" to StarThanksCopy(
        "Levyra je zdarma",
        "Každá aktualizace stojí spoustu času a práce. Nežádám o peníze: jestli mi chceš poděkovat, dej mi hvězdičku na GitHubu. Jestli už tam hvězdička je, moc děkuju. Příjemný poslech.",
        "Dát hvězdičku",
        "Už hotovo, díky",
        "Logo Levyry"
    ),
    "sk" to StarThanksCopy(
        "Levyra je zadarmo",
        "Každá aktualizácia stojí veľa času a práce. Nežiadam peniaze: ak mi chceš poďakovať, daj mi hviezdičku na GitHube. Ak tam už hviezdička je, veľmi pekne ďakujem. Príjemné počúvanie.",
        "Dať hviezdičku",
        "Už hotovo, vďaka",
        "Logo Levyry"
    ),
    "hr" to StarThanksCopy(
        "Levyra je besplatna",
        "Svako ažuriranje oduzme puno vremena i truda. Ne tražim novac: ako mi želiš zahvaliti, ostavi mi zvjezdicu na GitHubu. Ako je zvjezdica već tamo, od srca hvala. Ugodno slušanje.",
        "Ostavi zvjezdicu",
        "Već je tamo, hvala",
        "Logo Levyre"
    ),
    "sr" to StarThanksCopy(
        "Levyra је бесплатна",
        "Свако ажурирање одузме много времена и труда. Не тражим новац: ако желиш да ми захвалиш, остави ми звездицу на GitHub-у. Ако је звездица већ ту, од срца хвала. Пријатно слушање.",
        "Остави звездицу",
        "Већ је ту, хвала",
        "Логотип Levyra"
    ),
    "sq" to StarThanksCopy(
        "Levyra është falas",
        "Çdo përditësim merr shumë kohë dhe punë. Nuk të kërkoj para: nëse do të më falënderosh, lërmë një yll në GitHub. Nëse e ke bërë tashmë, të falënderoj nga zemra. Dëgjim të këndshëm.",
        "Lër një yll",
        "E kam bërë, faleminderit",
        "Logoja e Levyra"
    ),
    "bg" to StarThanksCopy(
        "Levyra е безплатна",
        "Всяка актуализация отнема много време и труд. Не моля за пари: ако искаш да ми благодариш, остави ми звезда в GitHub. Ако звездата вече е там, сърдечно благодаря. Приятно слушане.",
        "Остави звезда",
        "Вече е там, благодаря",
        "Лого на Levyra"
    ),
    "hu" to StarThanksCopy(
        "A Levyra ingyenes",
        "Minden frissítés rengeteg időt és munkát kíván. Nem kérek pénzt: ha meg szeretnéd köszönni, adj egy csillagot a GitHubon. Ha már megtetted, szívből köszönöm. Jó zenehallgatást.",
        "Csillagot adok",
        "Már megtettem, köszi",
        "A Levyra logója"
    ),
    "fi" to StarThanksCopy(
        "Levyra on ilmainen",
        "Jokainen päivitys vie paljon aikaa ja työtä. En pyydä rahaa: jos haluat kiittää, anna tähti GitHubissa. Jos olet jo antanut, kiitos tosi paljon. Mukavia kuunteluhetkiä.",
        "Anna tähti",
        "Annoin jo, kiitos",
        "Levyran logo"
    ),
    "et" to StarThanksCopy(
        "Levyra on tasuta",
        "Iga uuendus võtab palju aega ja tööd. Ma ei palu raha: kui tahad aitäh öelda, jäta mulle GitHubis täht. Kui oled seda juba teinud, suur-suur aitäh. Head kuulamist.",
        "Jäta täht",
        "Juba tehtud, aitäh",
        "Levyra logo"
    ),
    "nb" to StarThanksCopy(
        "Levyra er gratis",
        "Hver oppdatering tar mye tid og arbeid. Jeg ber ikke om penger: vil du si takk, gi meg gjerne en stjerne på GitHub. Har du allerede gjort det, tusen takk. God lytting.",
        "Gi en stjerne",
        "Allerede gjort, takk",
        "Levyras logo"
    ),
    "ca" to StarThanksCopy(
        "Levyra és gratuïta",
        "Cada actualització demana molt de temps i feina. No et demano diners: si em vols donar les gràcies, deixa'm una estrella a GitHub. Si ja ho has fet, moltíssimes gràcies. Gaudeix de la música.",
        "Deixar una estrella",
        "Ja ho he fet, gràcies",
        "Logotip de Levyra"
    ),
    "uk" to StarThanksCopy(
        "Levyra безкоштовна",
        "Кожне оновлення забирає багато часу й праці. Я не прошу грошей: якщо хочеш подякувати, постав зірку на GitHub. Якщо зірка вже стоїть, щиро дякую. Приємного прослуховування.",
        "Поставити зірку",
        "Вже стоїть, дякую",
        "Логотип Levyra"
    ),
    "ru" to StarThanksCopy(
        "Levyra бесплатна",
        "Каждое обновление отнимает много времени и сил. Я не прошу денег: если хочешь сказать спасибо, поставь звезду на GitHub. Если звезда уже стоит, огромное спасибо. Приятного прослушивания.",
        "Поставить звезду",
        "Уже стоит, спасибо",
        "Логотип Levyra"
    ),
    "kk" to StarThanksCopy(
        "Levyra тегін",
        "Әр жаңарту көп уақыт пен еңбекті қажет етеді. Мен ақша сұрамаймын: алғыс айтқың келсе, GitHub-та жұлдыз қой. Жұлдыз қойған болсаң, шын жүректен рахмет. Музыканы рахаттанып тыңда.",
        "Жұлдыз қою",
        "Қойдым, рахмет",
        "Levyra логотипі"
    ),
    "tr" to StarThanksCopy(
        "Levyra ücretsiz",
        "Her güncelleme çok zaman ve emek gerektiriyor. Para istemiyorum: teşekkür etmek istersen GitHub'da bir yıldız bırakabilirsin. Zaten bıraktıysan çok teşekkür ederim. İyi dinlemeler.",
        "Yıldız bırak",
        "Zaten bıraktım, teşekkürler",
        "Levyra logosu"
    ),
    "ar" to StarThanksCopy(
        "Levyra مجاني",
        "كل تحديث يأخذ الكثير من الوقت والعمل. لا أطلب منك مالًا: إن أردت أن تشكرني، اترك لي نجمة على GitHub. وإن كنت قد فعلت، فشكرًا من القلب. استماعًا ممتعًا.",
        "اترك نجمة",
        "فعلت ذلك، شكرًا",
        "شعار Levyra"
    ),
    "fa" to StarThanksCopy(
        "Levyra رایگان است",
        "هر به‌روزرسانی وقت و زحمت زیادی می‌برد. پولی نمی‌خواهم: اگر دوست داری تشکر کنی، در GitHub یک ستاره بده. اگر قبلاً داده‌ای، از ته دل ممنونم. از موسیقی لذت ببر.",
        "ستاره بده",
        "قبلاً دادم، ممنون",
        "لوگوی Levyra"
    ),
    "zh" to StarThanksCopy(
        "Levyra 是免费的",
        "每次更新都要花很多时间和精力。我不向你要钱：如果想说声谢谢，就在 GitHub 上给我点个星吧。如果已经点过了，真心谢谢你。听歌愉快。",
        "点个星",
        "已经点过了，谢谢",
        "Levyra 标志"
    ),
    "zh-Hant" to StarThanksCopy(
        "Levyra 是免費的",
        "每次更新都要花很多時間和心力。我不跟你要錢：如果想說聲謝謝，就在 GitHub 上給我一顆星吧。如果已經給過了，真心感謝。聽歌愉快。",
        "給一顆星",
        "已經給過了，謝謝",
        "Levyra 標誌"
    ),
    "ja" to StarThanksCopy(
        "Levyraは無料です",
        "アップデートのたびに、たくさんの時間と手間がかかっています。お金はいただきません。お礼の気持ちがあれば、GitHubでスターをつけてもらえるとうれしいです。もうつけてくれた方、本当にありがとうございます。音楽を楽しんでください。",
        "スターをつける",
        "もうつけました、ありがとう",
        "Levyraのロゴ"
    ),
    "ko" to StarThanksCopy(
        "Levyra는 무료예요",
        "업데이트 하나하나에 많은 시간과 노력이 들어가요. 돈은 받지 않아요. 고마운 마음을 전하고 싶다면 GitHub에 별 하나 남겨 주세요. 이미 남겨 주셨다면 정말 감사해요. 즐겁게 들으세요.",
        "별 남기기",
        "이미 남겼어요, 고마워요",
        "Levyra 로고"
    ),
    "hi" to StarThanksCopy(
        "Levyra मुफ़्त है",
        "हर अपडेट में बहुत समय और मेहनत लगती है। मैं पैसे नहीं माँग रहा: अगर आप शुक्रिया कहना चाहें, तो GitHub पर एक स्टार दे दीजिए। अगर आप पहले ही दे चुके हैं, तो दिल से धन्यवाद। सुनने का आनंद लें।",
        "स्टार दें",
        "दे दिया, धन्यवाद",
        "Levyra का लोगो"
    ),
    "bn" to StarThanksCopy(
        "Levyra বিনামূল্যে",
        "প্রতিটি আপডেটে অনেক সময় আর পরিশ্রম লাগে। আমি টাকা চাই না: ধন্যবাদ জানাতে চাইলে GitHub-এ একটি স্টার দিন। আগেই দিয়ে থাকলে আন্তরিক ধন্যবাদ। শুনে আনন্দ পান।",
        "স্টার দিন",
        "দিয়েছি, ধন্যবাদ",
        "Levyra-এর লোগো"
    ),
    "id" to StarThanksCopy(
        "Levyra gratis",
        "Setiap pembaruan butuh banyak waktu dan kerja. Aku tidak minta uang: kalau mau berterima kasih, beri bintang di GitHub, ya. Kalau sudah, terima kasih banyak. Selamat mendengarkan.",
        "Beri bintang",
        "Sudah, terima kasih",
        "Logo Levyra"
    ),
    "ms" to StarThanksCopy(
        "Levyra percuma",
        "Setiap kemas kini memerlukan banyak masa dan usaha. Saya tidak minta wang: jika mahu berterima kasih, berikan bintang di GitHub. Jika sudah, terima kasih banyak-banyak. Selamat mendengar.",
        "Beri bintang",
        "Sudah, terima kasih",
        "Logo Levyra"
    ),
    "vi" to StarThanksCopy(
        "Levyra miễn phí",
        "Mỗi bản cập nhật tốn rất nhiều thời gian và công sức. Mình không xin tiền đâu: nếu muốn cảm ơn, bạn hãy tặng mình một ngôi sao trên GitHub nhé. Nếu bạn đã tặng rồi, cảm ơn bạn rất nhiều. Chúc bạn nghe nhạc vui.",
        "Tặng một ngôi sao",
        "Mình tặng rồi, cảm ơn",
        "Logo Levyra"
    ),
    "th" to StarThanksCopy(
        "Levyra ใช้ฟรี",
        "การอัปเดตแต่ละครั้งใช้เวลาและแรงเยอะมาก ผมไม่ได้ขอเงิน ถ้าอยากขอบคุณ ฝากกดดาวให้บน GitHub สักดวงนะครับ ถ้ากดไปแล้ว ขอบคุณมากจริงๆ ครับ ฟังเพลงให้สนุกนะครับ",
        "กดดาว",
        "กดแล้ว ขอบคุณ",
        "โลโก้ Levyra"
    ),
    "fil" to StarThanksCopy(
        "Libre ang Levyra",
        "Maraming oras at trabaho ang napupunta sa bawat update. Hindi ako humihingi ng pera: kung gusto mong magpasalamat, mag-iwan ka ng star sa GitHub. Kung nagawa mo na, maraming-maraming salamat. Enjoy sa pakikinig.",
        "Mag-iwan ng star",
        "Nagawa ko na, salamat",
        "Logo ng Levyra"
    ),
    "he" to StarThanksCopy(
        "Levyra חינמית",
        "כל עדכון דורש הרבה זמן ועבודה. אני לא מבקש כסף: אם תרצו להגיד תודה, השאירו לי כוכב ב-GitHub. אם כבר עשיתם את זה, תודה מכל הלב. האזנה נעימה.",
        "להשאיר כוכב",
        "כבר עשיתי, תודה",
        "הלוגו של Levyra"
    )
)

internal fun starThanksCopy(code: String): StarThanksCopy = localizedValueOrEnglish(starThanksCopies, code)

internal fun starThanksLocalizationCodes(): Set<String> = starThanksCopies.keys
