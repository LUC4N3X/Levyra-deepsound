package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.feature.search.PersonalizedSearchPrompt
import com.luc4n3x.levyra.feature.search.PersonalizedSearchPromptKind

internal data class PersonalizedSearchCopy(
    val basedOnListening: String,
    val artistsForYou: String,
    val searchArtist: String,
    val findMoreLike: String,
    val backToAlbum: String
)

internal fun personalizedSearchCopy(code: String): PersonalizedSearchCopy =
    PERSONALIZED_SEARCH_COPY.getValue(code)

internal fun personalizedSearchPromptText(code: String, prompt: PersonalizedSearchPrompt?): String? {
    prompt ?: return null
    val copy = personalizedSearchCopy(code)
    return when (prompt.kind) {
        PersonalizedSearchPromptKind.ARTIST -> copy.searchArtist.formatQuoted(prompt.value)
        PersonalizedSearchPromptKind.SIMILAR_TRACK -> copy.findMoreLike.formatQuoted(prompt.value)
        PersonalizedSearchPromptKind.ALBUM -> copy.backToAlbum.formatQuoted(prompt.value)
    }
}

private fun String.formatQuoted(value: String): String = replace("%s", "\u201c${value.take(48)}\u201d")

private fun searchCopy(vararg values: String): PersonalizedSearchCopy {
    require(values.size == 5)
    return PersonalizedSearchCopy(values[0], values[1], values[2], values[3], values[4])
}

private val PERSONALIZED_SEARCH_COPY = mapOf(
    "en" to searchCopy("Based on your listening", "Artists you listen to", "Search artist %s", "Find more like %s", "Back to album %s"),
    "it" to searchCopy("In base ai tuoi ascolti", "Artisti che ascolti", "Cerca l'artista %s", "Trova brani simili a %s", "Torna all'album %s"),
    "es" to searchCopy("Según lo que escuchas", "Artistas que escuchas", "Busca al artista %s", "Encuentra más como %s", "Vuelve al álbum %s"),
    "fr" to searchCopy("Selon vos écoutes", "Artistes que vous écoutez", "Rechercher l'artiste %s", "Trouver des titres comme %s", "Revenir à l'album %s"),
    "de" to searchCopy("Basierend auf deinem Hörverlauf", "Künstler, die du hörst", "Nach dem Künstler %s suchen", "Mehr wie %s finden", "Zurück zum Album %s"),
    "pt" to searchCopy("Com base no que ouves", "Artistas que ouves", "Pesquisar o artista %s", "Encontrar mais como %s", "Voltar ao álbum %s"),
    "nl" to searchCopy("Op basis van je luistergedrag", "Artiesten waar je naar luistert", "Zoek artiest %s", "Vind meer zoals %s", "Terug naar album %s"),
    "pl" to searchCopy("Na podstawie tego, czego słuchasz", "Artyści, których słuchasz", "Wyszukaj wykonawcę %s", "Znajdź więcej utworów podobnych do %s", "Wróć do albumu %s"),
    "ro" to searchCopy("Pe baza audițiilor tale", "Artiști pe care îi asculți", "Caută artistul %s", "Găsește mai multe ca %s", "Înapoi la albumul %s"),
    "el" to searchCopy("Με βάση τις ακροάσεις σου", "Καλλιτέχνες που ακούς", "Αναζήτησε τον καλλιτέχνη %s", "Βρες περισσότερα σαν το %s", "Πίσω στο άλμπουμ %s"),
    "sv" to searchCopy("Baserat på det du lyssnar på", "Artister du lyssnar på", "Sök efter artisten %s", "Hitta mer som %s", "Tillbaka till albumet %s"),
    "da" to searchCopy("Baseret på det, du lytter til", "Kunstnere, du lytter til", "Søg efter kunstneren %s", "Find mere som %s", "Tilbage til albummet %s"),
    "cs" to searchCopy("Podle toho, co posloucháš", "Interpreti, které posloucháš", "Vyhledat interpreta %s", "Najít další jako %s", "Zpět k albu %s"),
    "sk" to searchCopy("Podľa toho, čo počúvaš", "Interpreti, ktorých počúvaš", "Vyhľadať interpreta %s", "Nájsť viac ako %s", "Späť k albumu %s"),
    "hr" to searchCopy("Na temelju onoga što slušaš", "Izvođači koje slušaš", "Potraži izvođača %s", "Pronađi još poput %s", "Natrag na album %s"),
    "bg" to searchCopy("Според това, което слушаш", "Изпълнители, които слушаш", "Търси изпълнителя %s", "Намери още като %s", "Обратно към албума %s"),
    "hu" to searchCopy("A hallgatási szokásaid alapján", "Előadók, akiket hallgatsz", "%s előadó keresése", "További, ehhez hasonló zenék: %s", "Vissza ehhez az albumhoz: %s"),
    "fi" to searchCopy("Kuuntelusi perusteella", "Kuuntelemasi artistit", "Hae artistia %s", "Löydä lisää kappaleen %s kaltaisia", "Takaisin albumiin %s"),
    "et" to searchCopy("Sinu kuulamiste põhjal", "Esitajad, keda kuulad", "Otsi esitajat %s", "Leia rohkem nagu %s", "Tagasi albumi %s juurde"),
    "nb" to searchCopy("Basert på det du lytter til", "Artister du lytter til", "Søk etter artisten %s", "Finn mer som %s", "Tilbake til albumet %s"),
    "ca" to searchCopy("Segons el que escoltes", "Artistes que escoltes", "Cerca l'artista %s", "Troba més música com %s", "Torna a l'àlbum %s"),
    "uk" to searchCopy("На основі твоїх прослуховувань", "Виконавці, яких ти слухаєш", "Знайти виконавця %s", "Знайти більше схожого на %s", "Повернутися до альбому %s"),
    "ru" to searchCopy("На основе твоих прослушиваний", "Исполнители, которых ты слушаешь", "Найти исполнителя %s", "Найти больше похожего на %s", "Вернуться к альбому %s"),
    "tr" to searchCopy("Dinlediklerine göre", "Dinlediğin sanatçılar", "%s sanatçısını ara", "%s benzeri parçalar bul", "%s albümüne dön"),
    "ar" to searchCopy("استنادًا إلى ما تستمع إليه", "فنانون تستمع إليهم", "ابحث عن الفنان %s", "اعثر على المزيد مثل %s", "عُد إلى الألبوم %s"),
    "fa" to searchCopy("بر اساس شنیده‌های شما", "هنرمندانی که گوش می‌دهید", "جست‌وجوی هنرمند %s", "آثار بیشتری شبیه %s پیدا کنید", "بازگشت به آلبوم %s"),
    "zh" to searchCopy("根据你的收听记录", "你常听的艺人", "搜索艺人 %s", "查找更多类似 %s 的歌曲", "返回专辑 %s"),
    "zh-Hant" to searchCopy("根據你的聆聽記錄", "你常聽的藝人", "搜尋藝人 %s", "尋找更多類似 %s 的歌曲", "返回專輯 %s"),
    "ja" to searchCopy("あなたの再生履歴から", "よく聴くアーティスト", "アーティスト %s を検索", "%s に似た曲を探す", "アルバム %s に戻る"),
    "ko" to searchCopy("내 감상 기록을 바탕으로", "내가 즐겨 듣는 아티스트", "아티스트 %s 검색", "%s 같은 곡 더 찾기", "앨범 %s 다시 보기"),
    "hi" to searchCopy("आपके सुनने के आधार पर", "आपके सुने हुए कलाकार", "कलाकार %s खोजें", "%s जैसे और गाने खोजें", "एल्बम %s पर वापस जाएँ"),
    "id" to searchCopy("Berdasarkan yang Anda dengarkan", "Artis yang Anda dengarkan", "Cari artis %s", "Temukan lebih banyak seperti %s", "Kembali ke album %s"),
    "ms" to searchCopy("Berdasarkan apa yang anda dengar", "Artis yang anda dengar", "Cari artis %s", "Cari lebih banyak seperti %s", "Kembali ke album %s"),
    "vi" to searchCopy("Dựa trên nội dung bạn nghe", "Nghệ sĩ bạn thường nghe", "Tìm nghệ sĩ %s", "Tìm thêm bài giống %s", "Quay lại album %s"),
    "th" to searchCopy("อิงจากสิ่งที่คุณฟัง", "ศิลปินที่คุณฟัง", "ค้นหาศิลปิน %s", "ค้นหาเพลงเพิ่มเติมที่คล้าย %s", "กลับไปที่อัลบั้ม %s"),
    "fil" to searchCopy("Batay sa mga pinapakinggan mo", "Mga artist na pinapakinggan mo", "Hanapin ang artist na %s", "Maghanap pa ng tulad ng %s", "Bumalik sa album na %s"),
    "he" to searchCopy("על סמך ההאזנות שלך", "אמנים שבחרת להאזין להם", "חיפוש האמן %s", "מציאת שירים נוספים כמו %s", "חזרה לאלבום %s")
)

internal fun personalizedSearchLocalizationCodes(): Set<String> = PERSONALIZED_SEARCH_COPY.keys
