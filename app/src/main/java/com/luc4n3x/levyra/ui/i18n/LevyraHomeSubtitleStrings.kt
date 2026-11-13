package com.luc4n3x.levyra.ui.i18n

import androidx.compose.runtime.Composable
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin

internal enum class HomeSubtitle {
    OfflineDownloads,
    OfflinePlaylists,
    OfflineFavorites,
    OfflineRecent,
    Orbit,
    QuickPicks,
    ForgottenFavorites,
    AlbumsForYou,
    NewAlbums,
    TrendingArtists,
    NewReleases,
    TrendingVideos,
    NewVideos,
    ReleaseRadar,
    SimilarArtists,
    MoreMusic,
    CuratedPlaylists,
    Collections,
    DailyDiscover,
    FavoriteRadio,
    ChartMarket
}

internal data class HomeSectionSubtitles(
    val offlineDownloads: String,
    val offlinePlaylists: String,
    val offlineFavorites: String,
    val offlineRecent: String,
    val orbit: String,
    val quickPicks: String,
    val forgottenFavorites: String,
    val albumsForYou: String,
    val newAlbums: String,
    val trendingArtists: String,
    val newReleases: String,
    val trendingVideos: String,
    val newVideos: String,
    val releaseRadar: String,
    val similarArtists: String,
    val moreMusic: String,
    val curatedPlaylists: String,
    val collections: String,
    val dailyDiscover: String,
    val favoriteRadio: String,
    val chartMarket: String
) {
    operator fun get(subtitle: HomeSubtitle): String = when (subtitle) {
        HomeSubtitle.OfflineDownloads -> offlineDownloads
        HomeSubtitle.OfflinePlaylists -> offlinePlaylists
        HomeSubtitle.OfflineFavorites -> offlineFavorites
        HomeSubtitle.OfflineRecent -> offlineRecent
        HomeSubtitle.Orbit -> orbit
        HomeSubtitle.QuickPicks -> quickPicks
        HomeSubtitle.ForgottenFavorites -> forgottenFavorites
        HomeSubtitle.AlbumsForYou -> albumsForYou
        HomeSubtitle.NewAlbums -> newAlbums
        HomeSubtitle.TrendingArtists -> trendingArtists
        HomeSubtitle.NewReleases -> newReleases
        HomeSubtitle.TrendingVideos -> trendingVideos
        HomeSubtitle.NewVideos -> newVideos
        HomeSubtitle.ReleaseRadar -> releaseRadar
        HomeSubtitle.SimilarArtists -> similarArtists
        HomeSubtitle.MoreMusic -> moreMusic
        HomeSubtitle.CuratedPlaylists -> curatedPlaylists
        HomeSubtitle.Collections -> collections
        HomeSubtitle.DailyDiscover -> dailyDiscover
        HomeSubtitle.FavoriteRadio -> favoriteRadio
        HomeSubtitle.ChartMarket -> chartMarket
    }
}

private val homeSectionSubtitleCopies = mapOf(
    "en" to HomeSectionSubtitles("Saved on this device", "With saved songs", "Your favorite songs", "Played recently", "From your listening", "Ready to play", "Not heard in a while", "Based on your listening", "Out this week", "Popular near you", "Just released", "Trending right now", "Latest music videos", "From artists you follow", "New names to discover", "More to explore", "Curated playlists", "By mood and activity", "Fresh picks every day", "Your saved stations", "Most played right now"),
    "it" to HomeSectionSubtitles("Salvati su questo dispositivo", "Con brani salvati", "I tuoi brani preferiti", "Ascoltati di recente", "Dai tuoi ascolti", "Pronti da ascoltare", "Non li ascolti da un po'", "In base ai tuoi ascolti", "Usciti questa settimana", "Popolari vicino a te", "Appena usciti", "Di tendenza ora", "Ultimi video musicali", "Dagli artisti che segui", "Nuovi nomi da scoprire", "Altro da esplorare", "Playlist curate", "Per umore e attività", "Nuove scelte ogni giorno", "Le tue stazioni salvate", "I più ascoltati ora"),
    "es" to HomeSectionSubtitles("Guardadas en este dispositivo", "Con canciones guardadas", "Tus canciones favoritas", "Escuchadas hace poco", "Según lo que escuchas", "Listas para sonar", "Sin escuchar hace tiempo", "Basados en tus escuchas", "Publicados esta semana", "Populares cerca de ti", "Recién publicadas", "En tendencia ahora", "Últimos videos musicales", "De artistas que sigues", "Nombres nuevos por descubrir", "Más para explorar", "Listas seleccionadas", "Por estado de ánimo y actividad", "Novedades cada día", "Tus emisoras guardadas", "Lo más escuchado ahora"),
    "fr" to HomeSectionSubtitles("Enregistrés sur cet appareil", "Avec des titres enregistrés", "Tes titres favoris", "Écoutés récemment", "D'après tes écoutes", "Prêts à écouter", "Pas écoutés depuis un moment", "Selon tes écoutes", "Sortis cette semaine", "Populaires près de chez toi", "Tout juste sortis", "Tendance en ce moment", "Derniers clips musicaux", "Des artistes que tu suis", "Nouveaux noms à découvrir", "Encore plus à explorer", "Playlists sélectionnées", "Par humeur et activité", "Nouveautés chaque jour", "Tes stations enregistrées", "Les plus écoutés du moment"),
    "de" to HomeSectionSubtitles("Auf diesem Gerät gespeichert", "Mit gespeicherten Songs", "Deine Lieblingssongs", "Kürzlich gehört", "Aus deinem Hörverhalten", "Bereit zum Abspielen", "Länger nicht gehört", "Passend zu deinem Hörverhalten", "Diese Woche erschienen", "Beliebt in deiner Nähe", "Gerade erschienen", "Gerade im Trend", "Neueste Musikvideos", "Von Künstlern, denen du folgst", "Neue Namen entdecken", "Mehr zum Entdecken", "Ausgewählte Playlists", "Nach Stimmung und Aktivität", "Täglich neue Tipps", "Deine gespeicherten Sender", "Aktuell am meisten gehört"),
    "pt" to HomeSectionSubtitles("Guardadas neste dispositivo", "Com músicas guardadas", "As tuas músicas favoritas", "Ouvidas recentemente", "A partir do que ouves", "Prontas a tocar", "Não as ouves há algum tempo", "Com base no que ouves", "Lançados esta semana", "Populares perto de ti", "Acabadas de lançar", "Em tendência agora", "Vídeos musicais mais recentes", "Dos artistas que segues", "Novos nomes a descobrir", "Mais para explorar", "Playlists selecionadas", "Por humor e atividade", "Novas escolhas todos os dias", "As tuas estações guardadas", "As mais ouvidas agora"),
    "pt-BR" to HomeSectionSubtitles("Salvas neste dispositivo", "Com músicas salvas", "Suas músicas favoritas", "Ouvidas recentemente", "A partir do que você ouve", "Prontas para tocar", "Sem ouvir há um tempo", "Com base no que você ouve", "Lançados nesta semana", "Populares perto de você", "Acabaram de sair", "Em alta agora", "Clipes mais recentes", "Dos artistas que você segue", "Novos nomes para descobrir", "Mais para explorar", "Playlists selecionadas", "Por humor e atividade", "Novas escolhas todo dia", "Suas rádios salvas", "As mais ouvidas agora"),
    "nl" to HomeSectionSubtitles("Opgeslagen op dit apparaat", "Met opgeslagen nummers", "Je favoriete nummers", "Onlangs beluisterd", "Op basis van wat je luistert", "Klaar om te spelen", "Al een tijd niet gehoord", "Passend bij wat je luistert", "Deze week uitgekomen", "Populair bij jou in de buurt", "Net uitgekomen", "Nu trending", "Nieuwste muziekvideo's", "Van artiesten die je volgt", "Nieuwe namen om te ontdekken", "Meer om te ontdekken", "Geselecteerde playlists", "Op stemming en activiteit", "Elke dag nieuwe tips", "Je opgeslagen zenders", "Nu het meest beluisterd"),
    "pl" to HomeSectionSubtitles("Zapisane na tym urządzeniu", "Z zapisanymi utworami", "Twoje ulubione utwory", "Ostatnio odtwarzane", "Na podstawie Twoich odsłuchań", "Gotowe do odtworzenia", "Dawno nieodtwarzane", "Dopasowane do Twoich odsłuchań", "Wydane w tym tygodniu", "Popularni w Twojej okolicy", "Świeżo wydane", "Teraz na topie", "Najnowsze teledyski", "Od obserwowanych wykonawców", "Nowe nazwiska do odkrycia", "Więcej do odkrycia", "Wybrane playlisty", "Według nastroju i aktywności", "Codziennie nowe propozycje", "Twoje zapisane stacje", "Najczęściej słuchane teraz"),
    "ro" to HomeSectionSubtitles("Salvate pe acest dispozitiv", "Cu piese salvate", "Piesele tale preferate", "Ascultate recent", "Din ce asculți", "Gata de redare", "Neascultate de ceva timp", "Pe baza a ce asculți", "Lansate săptămâna aceasta", "Populari în zona ta", "Lansate de curând", "În tendințe acum", "Cele mai noi videoclipuri", "De la artiștii urmăriți", "Nume noi de descoperit", "Mai mult de explorat", "Playlisturi selectate", "După stare și activitate", "Recomandări noi zilnic", "Posturile tale salvate", "Cele mai ascultate acum"),
    "el" to HomeSectionSubtitles("Αποθηκευμένα σε αυτή τη συσκευή", "Με αποθηκευμένα τραγούδια", "Τα αγαπημένα σου τραγούδια", "Ακούστηκαν πρόσφατα", "Από όσα ακούς", "Έτοιμα για αναπαραγωγή", "Δεν τα άκουσες καιρό", "Με βάση όσα ακούς", "Κυκλοφόρησαν αυτή την εβδομάδα", "Δημοφιλείς κοντά σου", "Μόλις κυκλοφόρησαν", "Στις τάσεις τώρα", "Τα νεότερα μουσικά βίντεο", "Από καλλιτέχνες που ακολουθείς", "Νέα ονόματα για ανακάλυψη", "Κι άλλα για εξερεύνηση", "Επιλεγμένα playlist", "Ανά διάθεση και δραστηριότητα", "Νέες επιλογές κάθε μέρα", "Οι αποθηκευμένοι σταθμοί σου", "Τα πιο ακουσμένα τώρα"),
    "sv" to HomeSectionSubtitles("Sparade på den här enheten", "Med sparade låtar", "Dina favoritlåtar", "Nyligen spelade", "Utifrån din lyssning", "Redo att spelas", "Inte hörda på ett tag", "Baserat på din lyssning", "Släppta den här veckan", "Populära nära dig", "Nysläppta", "Trendar just nu", "Senaste musikvideorna", "Från artister du följer", "Nya namn att upptäcka", "Mer att utforska", "Utvalda spellistor", "Efter stämning och aktivitet", "Nya tips varje dag", "Dina sparade stationer", "Mest spelade just nu"),
    "da" to HomeSectionSubtitles("Gemt på denne enhed", "Med gemte numre", "Dine yndlingsnumre", "Spillet for nylig", "Ud fra din lytning", "Klar til afspilning", "Ikke hørt i et stykke tid", "Baseret på din lytning", "Udgivet i denne uge", "Populære nær dig", "Lige udgivet", "Trender lige nu", "Nyeste musikvideoer", "Fra kunstnere, du følger", "Nye navne at opdage", "Mere at udforske", "Udvalgte playlister", "Efter stemning og aktivitet", "Nye forslag hver dag", "Dine gemte stationer", "Mest spillet lige nu"),
    "cs" to HomeSectionSubtitles("Uložené v tomto zařízení", "S uloženými skladbami", "Vaše oblíbené skladby", "Nedávno přehrávané", "Z vašeho poslechu", "Připravené k přehrání", "Dlouho neposlouchané", "Podle vašeho poslechu", "Vydané tento týden", "Oblíbení ve vašem okolí", "Čerstvě vydané", "Právě v trendu", "Nejnovější hudební videa", "Od interpretů, které sledujete", "Nová jména k objevení", "Další k objevování", "Vybrané playlisty", "Podle nálady a činnosti", "Každý den nové tipy", "Vaše uložené stanice", "Teď nejposlouchanější"),
    "uk" to HomeSectionSubtitles("Збережені на цьому пристрої", "Зі збереженими піснями", "Ваші улюблені пісні", "Нещодавно прослухані", "З ваших прослуховувань", "Готові до відтворення", "Давно не слухали", "За вашими прослуховуваннями", "Вийшли цього тижня", "Популярні поруч із вами", "Щойно вийшли", "Популярні просто зараз", "Найновіші кліпи", "Від виконавців, за якими стежите", "Нові імена для відкриття", "Більше для відкриття", "Добірки плейлистів", "За настроєм і заняттям", "Нові добірки щодня", "Ваші збережені станції", "Найпопулярніше зараз"),
    "ru" to HomeSectionSubtitles("Сохранены на этом устройстве", "С сохранёнными песнями", "Ваши любимые песни", "Недавно прослушанные", "Из ваших прослушиваний", "Готовы к воспроизведению", "Давно не звучали", "По вашим прослушиваниям", "Вышли на этой неделе", "Популярны рядом с вами", "Только что вышли", "Популярны прямо сейчас", "Новейшие клипы", "От исполнителей из подписок", "Новые имена для открытий", "Ещё для знакомства", "Подборки плейлистов", "По настроению и занятию", "Новые подборки каждый день", "Ваши сохранённые станции", "Самое популярное сейчас"),
    "nb" to HomeSectionSubtitles("Lagret på denne enheten", "Med lagrede låter", "Favorittlåtene dine", "Spilt nylig", "Ut fra lyttingen din", "Klar til avspilling", "Ikke hørt på en stund", "Basert på lyttingen din", "Utgitt denne uken", "Populære nær deg", "Nettopp utgitt", "Trender akkurat nå", "Nyeste musikkvideoer", "Fra artister du følger", "Nye navn å oppdage", "Mer å utforske", "Utvalgte spillelister", "Etter stemning og aktivitet", "Nye tips hver dag", "Dine lagrede stasjoner", "Mest spilt akkurat nå"),
    "ca" to HomeSectionSubtitles("Desades en aquest dispositiu", "Amb cançons desades", "Les teves cançons preferides", "Escoltades fa poc", "A partir del que escoltes", "A punt per sonar", "Fa temps que no les escoltes", "Segons el que escoltes", "Publicats aquesta setmana", "Populars a prop teu", "Acabades de sortir", "En tendència ara", "Els vídeos musicals més nous", "Dels artistes que segueixes", "Noms nous per descobrir", "Més per explorar", "Llistes seleccionades", "Per estat d'ànim i activitat", "Novetats cada dia", "Les teves emissores desades", "El més escoltat ara"),
    "kk" to HomeSectionSubtitles("Осы құрылғыда сақталған", "Сақталған әндерімен", "Таңдаулы әндеріңіз", "Жақында тыңдалған", "Тыңдағаныңыз негізінде", "Тыңдауға дайын", "Біраз уақыт тыңдалмаған", "Тыңдау әдетіңізге сай", "Осы аптада шыққан", "Сіздің өңірде танымал", "Жаңа ғана шыққан", "Қазір трендте", "Ең жаңа бейнеклиптер", "Жазылған орындаушылардан", "Ашатын жаңа есімдер", "Тағы басқа әндер", "Іріктелген плейлистер", "Көңіл күй мен әрекет бойынша", "Күн сайын жаңа таңдау", "Сақталған станцияларыңыз", "Қазір ең көп тыңдалатын"),
    "tr" to HomeSectionSubtitles("Bu cihaza kaydedilenler", "Kayıtlı şarkılarla", "Favori şarkıların", "Son dinlenenler", "Dinlediklerinden", "Çalmaya hazır", "Bir süredir dinlenmeyenler", "Dinlediklerine göre", "Bu hafta çıkanlar", "Yakınında popüler", "Yeni çıkanlar", "Şu an trend", "En yeni müzik videoları", "Takip ettiğin sanatçılardan", "Keşfedilecek yeni isimler", "Keşfedilecek daha fazlası", "Seçme çalma listeleri", "Ruh hâli ve etkinliğe göre", "Her gün yeni öneriler", "Kayıtlı istasyonların", "Şu an en çok dinlenenler"),
    "ar" to HomeSectionSubtitles("محفوظة على هذا الجهاز", "تضم أغاني محفوظة", "أغانيك المفضلة", "استمعت إليها مؤخرًا", "من استماعك", "جاهزة للتشغيل", "لم تسمعها منذ مدة", "بناءً على استماعك", "صدرت هذا الأسبوع", "رائجون بالقرب منك", "صدرت للتو", "رائجة الآن", "أحدث الفيديوهات الموسيقية", "من فنانين تتابعهم", "أسماء جديدة لتكتشفها", "المزيد لاستكشافه", "قوائم تشغيل مختارة", "حسب المزاج والنشاط", "اختيارات جديدة كل يوم", "محطاتك المحفوظة", "الأكثر استماعًا الآن"),
    "fa" to HomeSectionSubtitles("ذخیره‌شده در این دستگاه", "با آهنگ‌های ذخیره‌شده", "آهنگ‌های برگزیدهٔ شما", "اخیراً شنیده‌شده", "از شنیده‌های شما", "آمادهٔ پخش", "مدتی است شنیده نشده", "بر اساس شنیده‌های شما", "منتشرشده در این هفته", "محبوب در نزدیکی شما", "تازه منتشرشده", "پرطرفدار همین حالا", "تازه‌ترین موزیک‌ویدیوها", "از هنرمندانی که دنبال می‌کنید", "نام‌های تازه برای کشف", "موارد بیشتر برای کاوش", "فهرست‌های پخش منتخب", "بر اساس حال‌وهوا و فعالیت", "پیشنهادهای تازه هر روز", "ایستگاه‌های ذخیره‌شدهٔ شما", "پرشنیده‌ترین‌ها در این لحظه"),
    "zh" to HomeSectionSubtitles("已保存到此设备", "含已保存的歌曲", "你收藏的歌曲", "最近播放", "来自你的收听记录", "即点即播", "有段时间没听了", "根据你的收听推荐", "本周新发行", "你附近的热门艺人", "最新发行", "正在流行", "最新音乐视频", "来自你关注的艺人", "发现新面孔", "更多可探索内容", "精选歌单", "按心情和场景", "每日新推荐", "你保存的电台", "当前最热播"),
    "zh-Hant" to HomeSectionSubtitles("已儲存到此裝置", "含已儲存的歌曲", "你收藏的歌曲", "最近播放", "來自你的收聽紀錄", "隨點即播", "有段時間沒聽了", "根據你的收聽推薦", "本週新發行", "你附近的熱門藝人", "最新發行", "正在流行", "最新音樂影片", "來自你追蹤的藝人", "發現新面孔", "更多可探索內容", "精選播放清單", "依心情和情境", "每日新推薦", "你儲存的電台", "目前最熱播"),
    "ja" to HomeSectionSubtitles("このデバイスに保存済み", "保存済みの曲を含む", "お気に入りの曲", "最近再生した曲", "あなたの再生履歴から", "すぐに再生できる曲", "しばらく聴いていない曲", "再生傾向にもとづくおすすめ", "今週リリース", "あなたの地域で人気", "新着リリース", "いま話題", "最新のミュージックビデオ", "フォロー中のアーティストから", "新しいアーティストを発見", "その他のおすすめ", "厳選プレイリスト", "気分やシーン別", "毎日届く新しいおすすめ", "保存した放送局", "いま最も再生されている曲"),
    "ko" to HomeSectionSubtitles("이 기기에 저장됨", "저장된 곡 포함", "즐겨찾는 곡", "최근 재생한 곡", "내 청취 기록에서", "바로 재생 가능", "한동안 듣지 않은 곡", "청취 기록 기반 추천", "이번 주 발매", "내 주변 인기 아티스트", "최신 발매곡", "지금 인기 급상승", "최신 뮤직비디오", "팔로우한 아티스트의 신곡", "새로운 아티스트 발견", "더 둘러볼 음악", "엄선된 플레이리스트", "분위기·활동별", "매일 새로운 추천", "저장한 방송국", "지금 가장 많이 듣는 곡"),
    "hi" to HomeSectionSubtitles("इस डिवाइस पर सेव", "सेव किए गानों वाली", "आपके पसंदीदा गाने", "हाल ही में सुने गए", "आपके सुने गानों से", "चलाने के लिए तैयार", "कुछ समय से नहीं सुने", "आपकी पसंद के मुताबिक", "इस हफ़्ते रिलीज़", "आपके आस-पास लोकप्रिय", "अभी-अभी रिलीज़", "अभी ट्रेंड में", "नए म्यूज़िक वीडियो", "फ़ॉलो किए कलाकारों से", "खोजने के लिए नए नाम", "और भी बहुत कुछ", "चुनी हुई प्लेलिस्ट", "मूड और गतिविधि के अनुसार", "हर दिन नए सुझाव", "आपके सेव किए स्टेशन", "अभी सबसे ज़्यादा सुने गए"),
    "bn" to HomeSectionSubtitles("এই ডিভাইসে সেভ করা", "সেভ করা গানসহ", "আপনার প্রিয় গান", "সম্প্রতি শোনা", "আপনার শোনা গান থেকে", "চালানোর জন্য প্রস্তুত", "অনেক দিন শোনা হয়নি", "আপনার রুচি অনুযায়ী", "এই সপ্তাহে প্রকাশিত", "আপনার কাছাকাছি জনপ্রিয়", "সদ্য প্রকাশিত", "এখন ট্রেন্ডিং", "নতুন মিউজিক ভিডিও", "অনুসরণ করা শিল্পীদের থেকে", "আবিষ্কারের নতুন নাম", "আরও ঘুরে দেখার মতো", "বাছাই করা প্লেলিস্ট", "মেজাজ ও কাজ অনুযায়ী", "প্রতিদিন নতুন বাছাই", "আপনার সেভ করা স্টেশন", "এখন সবচেয়ে বেশি শোনা"),
    "id" to HomeSectionSubtitles("Tersimpan di perangkat ini", "Berisi lagu tersimpan", "Lagu favoritmu", "Baru saja diputar", "Dari riwayat dengarmu", "Siap diputar", "Lama tidak diputar", "Sesuai seleramu", "Rilis minggu ini", "Populer di sekitarmu", "Baru dirilis", "Sedang trending", "Video musik terbaru", "Dari artis yang kamu ikuti", "Nama baru untuk ditemukan", "Lainnya untuk dijelajahi", "Playlist pilihan", "Menurut suasana dan aktivitas", "Pilihan baru setiap hari", "Stasiun yang kamu simpan", "Paling banyak diputar kini"),
    "ms" to HomeSectionSubtitles("Disimpan pada peranti ini", "Dengan lagu yang disimpan", "Lagu kegemaran anda", "Dimainkan baru-baru ini", "Daripada pendengaran anda", "Sedia untuk dimainkan", "Lama tidak didengar", "Berdasarkan pendengaran anda", "Dikeluarkan minggu ini", "Popular berhampiran anda", "Baru dikeluarkan", "Sohor kini", "Video muzik terkini", "Daripada artis yang anda ikuti", "Nama baharu untuk diterokai", "Lagi untuk diterokai", "Senarai main pilihan", "Mengikut suasana dan aktiviti", "Pilihan baharu setiap hari", "Stesen yang anda simpan", "Paling banyak dimainkan kini"),
    "vi" to HomeSectionSubtitles("Đã lưu trên thiết bị này", "Có bài hát đã lưu", "Bài hát yêu thích của bạn", "Nghe gần đây", "Từ lịch sử nghe của bạn", "Sẵn sàng phát", "Lâu rồi chưa nghe", "Dựa trên thói quen nghe", "Phát hành tuần này", "Phổ biến gần bạn", "Vừa phát hành", "Đang thịnh hành", "Video âm nhạc mới nhất", "Từ nghệ sĩ bạn theo dõi", "Những cái tên mới để khám phá", "Thêm nội dung để khám phá", "Danh sách phát tuyển chọn", "Theo tâm trạng và hoạt động", "Gợi ý mới mỗi ngày", "Đài bạn đã lưu", "Nghe nhiều nhất hiện nay"),
    "th" to HomeSectionSubtitles("บันทึกไว้ในอุปกรณ์นี้", "มีเพลงที่บันทึกไว้", "เพลงโปรดของคุณ", "เล่นล่าสุด", "จากการฟังของคุณ", "พร้อมเล่น", "ไม่ได้ฟังมาสักพัก", "ตามการฟังของคุณ", "ออกในสัปดาห์นี้", "ยอดนิยมใกล้คุณ", "เพิ่งออก", "มาแรงตอนนี้", "มิวสิกวิดีโอล่าสุด", "จากศิลปินที่คุณติดตาม", "ชื่อใหม่ให้ค้นพบ", "เพิ่มเติมให้สำรวจ", "เพลย์ลิสต์คัดสรร", "ตามอารมณ์และกิจกรรม", "ตัวเลือกใหม่ทุกวัน", "สถานีที่คุณบันทึกไว้", "ฟังมากที่สุดตอนนี้"),
    "fil" to HomeSectionSubtitles("Naka-save sa device na ito", "May mga naka-save na kanta", "Mga paborito mong kanta", "Pinakinggan kamakailan", "Mula sa iyong pinapakinggan", "Handa nang patugtugin", "Matagal nang hindi napakinggan", "Batay sa iyong pinapakinggan", "Inilabas ngayong linggo", "Sikat malapit sa iyo", "Kalalabas lang", "Trending ngayon", "Pinakabagong music video", "Mula sa mga fina-follow mo", "Mga bagong pangalang tuklasin", "Iba pang matutuklasan", "Mga piling playlist", "Ayon sa mood at aktibidad", "Bagong pili araw-araw", "Mga na-save mong istasyon", "Pinakapinakikinggan ngayon"),
    "he" to HomeSectionSubtitles("שמורים במכשיר הזה", "עם שירים שמורים", "השירים האהובים עליך", "הושמעו לאחרונה", "מתוך ההאזנות שלך", "מוכנים להשמעה", "לא הושמעו זמן מה", "לפי ההאזנות שלך", "יצאו השבוע", "פופולריים באזור שלך", "יצאו עכשיו", "בטרנד עכשיו", "הקליפים החדשים ביותר", "מאמנים שבמעקב שלך", "שמות חדשים לגלות", "עוד לגלות", "פלייליסטים נבחרים", "לפי מצב רוח ופעילות", "בחירות חדשות בכל יום", "התחנות ששמרת", "הכי מושמעים עכשיו"),
    "hu" to HomeSectionSubtitles("Az eszközön tárolva", "Mentett dalokkal", "Kedvenc dalaid", "Nemrég lejátszott", "A hallgatásaidból", "Lejátszásra készen", "Egy ideje nem hallottad", "A hallgatásaid alapján", "Ezen a héten jelent meg", "Népszerű a közeledben", "Most jelent meg", "Most felkapott", "Legújabb zenei videók", "A követett előadóktól", "Felfedezendő új nevek", "További felfedeznivaló", "Válogatott listák", "Hangulat és tevékenység szerint", "Naponta új ajánlatok", "Mentett állomásaid", "Most a legtöbbet hallgatott"),
    "fi" to HomeSectionSubtitles("Tallennettu tälle laitteelle", "Sisältää tallennettuja", "Suosikkikappaleesi", "Äskettäin kuunneltu", "Kuuntelusi pohjalta", "Valmiina soimaan", "Kuuntelematta hetkeen", "Kuunteluusi perustuen", "Julkaistu tällä viikolla", "Suosittuja lähelläsi", "Juuri julkaistu", "Trendaa nyt", "Uusimmat musiikkivideot", "Seuraamiltasi artisteilta", "Uusia nimiä löydettäväksi", "Lisää tutkittavaa", "Valikoidut soittolistat", "Tunnelman ja tekemisen mukaan", "Uusia vinkkejä päivittäin", "Tallentamasi asemat", "Kuunnelluimmat nyt"),
    "bg" to HomeSectionSubtitles("Запазени на това устройство", "Със запазени песни", "Любимите ти песни", "Наскоро слушани", "От слушанията ти", "Готови за пускане", "Отдавна непускани", "Според слушанията ти", "Излезли тази седмица", "Популярни около теб", "Току-що излезли", "Популярни в момента", "Най-новите клипове", "От изпълнители, които следваш", "Нови имена за откриване", "Още за откриване", "Подбрани плейлисти", "По настроение и занимание", "Нови предложения всеки ден", "Запазените ти станции", "Най-слушани в момента"),
    "hr" to HomeSectionSubtitles("Spremljeno na ovom uređaju", "Sa spremljenim pjesmama", "Tvoje omiljene pjesme", "Nedavno slušano", "Iz tvojeg slušanja", "Spremno za reprodukciju", "Dugo nisu puštane", "Prema tvojem slušanju", "Objavljeno ovaj tjedan", "Popularni u tvojoj blizini", "Upravo objavljeno", "Trenutačno u trendu", "Najnoviji glazbeni spotovi", "Od izvođača koje pratiš", "Nova imena za otkrivanje", "Još za istraživanje", "Odabrane playliste", "Prema raspoloženju i aktivnosti", "Svaki dan novi prijedlozi", "Tvoje spremljene postaje", "Trenutačno najslušanije"),
    "sr" to HomeSectionSubtitles("Сачувано на овом уређају", "Са сачуваним песмама", "Твоје омиљене песме", "Недавно слушано", "Из твог слушања", "Спремно за пуштање", "Дуго нису пуштане", "Према твом слушању", "Објављено ове недеље", "Популарни у твојој близини", "Управо објављено", "Тренутно у тренду", "Најновији музички спотови", "Од извођача које пратиш", "Нова имена за откривање", "Још за истраживање", "Одабране плејлисте", "Према расположењу и активности", "Сваког дана нови предлози", "Твоје сачуване станице", "Тренутно најслушаније"),
    "sk" to HomeSectionSubtitles("Uložené v tomto zariadení", "S uloženými skladbami", "Vaše obľúbené skladby", "Nedávno prehrávané", "Z vášho počúvania", "Pripravené na prehranie", "Dlho nepočúvané", "Podľa vášho počúvania", "Vydané tento týždeň", "Obľúbení vo vašom okolí", "Čerstvo vydané", "Práve v trende", "Najnovšie hudobné videá", "Od interpretov, ktorých sledujete", "Nové mená na objavenie", "Ďalšie na objavovanie", "Vybrané playlisty", "Podľa nálady a činnosti", "Každý deň nové tipy", "Vaše uložené stanice", "Teraz najpočúvanejšie"),
    "et" to HomeSectionSubtitles("Salvestatud sellesse seadmesse", "Salvestatud lugudega", "Sinu lemmiklood", "Hiljuti kuulatud", "Sinu kuulamistest", "Valmis mängimiseks", "Ammu kuulamata", "Sinu kuulamiste põhjal", "Sel nädalal ilmunud", "Populaarsed sinu lähedal", "Äsja ilmunud", "Praegu trendis", "Uusimad muusikavideod", "Esitajatelt, keda jälgid", "Uued nimed avastamiseks", "Veel avastamiseks", "Valitud esitusloendid", "Meeleolu ja tegevuse järgi", "Iga päev uued soovitused", "Sinu salvestatud jaamad", "Praegu enim kuulatud"),
    "sq" to HomeSectionSubtitles("Ruajtur në këtë pajisje", "Me këngë të ruajtura", "Këngët e tua të preferuara", "Dëgjuar së fundi", "Nga dëgjimet e tua", "Gati për t'u luajtur", "Pa u dëgjuar prej kohësh", "Sipas dëgjimeve të tua", "Dalë këtë javë", "Të njohur pranë teje", "Sapo dalë", "Në trend tani", "Videoklipet më të reja", "Nga artistët që ndjek", "Emra të rinj për të zbuluar", "Më shumë për të eksploruar", "Lista të përzgjedhura", "Sipas disponimit dhe aktivitetit", "Zgjedhje të reja çdo ditë", "Stacionet e tua të ruajtura", "Më të dëgjuarat tani")
)

internal fun homeSectionSubtitles(code: String): HomeSectionSubtitles =
    localizedValueOrEnglish(homeSectionSubtitleCopies, code)

internal fun homeSectionSubtitleLocalizationCodes(): Set<String> = homeSectionSubtitleCopies.keys

internal const val HOME_SUBTITLE_OFFLINE_DOWNLOADS = 0
internal const val HOME_SUBTITLE_OFFLINE_PLAYLISTS = 1
internal const val HOME_SUBTITLE_OFFLINE_FAVORITES = 2
internal const val HOME_SUBTITLE_OFFLINE_RECENT = 3
internal const val HOME_SUBTITLE_ORBIT = 4
internal const val HOME_SUBTITLE_QUICK_PICKS = 5
internal const val HOME_SUBTITLE_FORGOTTEN_FAVORITES = 6
internal const val HOME_SUBTITLE_ALBUMS = 7
internal const val HOME_SUBTITLE_TRENDING_ARTISTS = 8
internal const val HOME_SUBTITLE_NEW_RELEASES = 9
internal const val HOME_SUBTITLE_VIDEOS = 10
internal const val HOME_SUBTITLE_RELEASE_RADAR = 11
internal const val HOME_SUBTITLE_SIMILAR_ARTISTS = 12
internal const val HOME_SUBTITLE_MORE_MUSIC = 13
internal const val HOME_SUBTITLE_CURATED_PLAYLISTS = 14
internal const val HOME_SUBTITLE_COLLECTIONS = 15
internal const val HOME_SUBTITLE_DAILY_DISCOVER = 16
internal const val HOME_SUBTITLE_FAVORITE_RADIO = 17
internal const val HOME_SUBTITLE_CHART_MARKET = 18
internal const val HOME_SUBTITLE_YOUR_MUSIC = 19

internal fun homeSubtitleFor(key: Int, title: String, strings: LevyraStrings): String? {
    val subtitle = when (key) {
        HOME_SUBTITLE_OFFLINE_DOWNLOADS -> HomeSubtitle.OfflineDownloads
        HOME_SUBTITLE_OFFLINE_PLAYLISTS -> HomeSubtitle.OfflinePlaylists
        HOME_SUBTITLE_OFFLINE_FAVORITES -> HomeSubtitle.OfflineFavorites
        HOME_SUBTITLE_OFFLINE_RECENT -> HomeSubtitle.OfflineRecent
        HOME_SUBTITLE_ORBIT -> HomeSubtitle.Orbit
        HOME_SUBTITLE_QUICK_PICKS -> HomeSubtitle.QuickPicks
        HOME_SUBTITLE_FORGOTTEN_FAVORITES -> HomeSubtitle.ForgottenFavorites
        HOME_SUBTITLE_ALBUMS -> if (title == strings.albumsForYou) HomeSubtitle.AlbumsForYou else HomeSubtitle.NewAlbums
        HOME_SUBTITLE_TRENDING_ARTISTS -> HomeSubtitle.TrendingArtists
        HOME_SUBTITLE_NEW_RELEASES -> HomeSubtitle.NewReleases
        HOME_SUBTITLE_VIDEOS -> if (title == strings.homeTrendingVideoShelfTitle) HomeSubtitle.TrendingVideos else HomeSubtitle.NewVideos
        HOME_SUBTITLE_RELEASE_RADAR -> HomeSubtitle.ReleaseRadar
        HOME_SUBTITLE_SIMILAR_ARTISTS -> HomeSubtitle.SimilarArtists
        HOME_SUBTITLE_MORE_MUSIC -> HomeSubtitle.MoreMusic
        HOME_SUBTITLE_CURATED_PLAYLISTS -> HomeSubtitle.CuratedPlaylists
        HOME_SUBTITLE_COLLECTIONS -> HomeSubtitle.Collections
        HOME_SUBTITLE_DAILY_DISCOVER -> HomeSubtitle.DailyDiscover
        HOME_SUBTITLE_FAVORITE_RADIO -> HomeSubtitle.FavoriteRadio
        HOME_SUBTITLE_CHART_MARKET -> HomeSubtitle.ChartMarket
        HOME_SUBTITLE_YOUR_MUSIC -> return youtubeMusicHomeCopy(strings.code).source
        else -> return null
    }
    return homeSectionSubtitles(strings.code)[subtitle]
}

@Composable
internal fun defaultHomeSubtitle(key: Int, title: String): String? =
    if (LevyraIsDefaultSkin) homeSubtitleFor(key, title, LocalLevyraStrings.current) else null
