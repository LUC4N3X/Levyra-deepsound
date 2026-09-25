package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.feature.search.PersonalizedSearchPrompt
import com.luc4n3x.levyra.feature.search.PersonalizedSearchPromptKind

internal data class PersonalizedSearchCopy(
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
    require(values.size == 4)
    return PersonalizedSearchCopy(values[0], values[1], values[2], values[3])
}

private val PERSONALIZED_SEARCH_COPY = mapOf(
    "en" to searchCopy("Artists you listen to", "Search artist %s", "Find more like %s", "Back to album %s"),
    "it" to searchCopy("Artisti che ascolti", "Cerca l'artista %s", "Trova brani simili a %s", "Torna all'album %s"),
    "es" to searchCopy("Artistas que escuchas", "Busca al artista %s", "Encuentra más como %s", "Vuelve al álbum %s"),
    "fr" to searchCopy("Artistes que vous écoutez", "Rechercher l'artiste %s", "Trouver des titres comme %s", "Revenir à l'album %s"),
    "de" to searchCopy("Künstler, die du hörst", "Nach dem Künstler %s suchen", "Mehr wie %s finden", "Zurück zum Album %s"),
    "pt" to searchCopy("Artistas que ouves", "Pesquisar o artista %s", "Encontrar mais como %s", "Voltar ao álbum %s"),
    "nl" to searchCopy("Artiesten waar je naar luistert", "Zoek artiest %s", "Vind meer zoals %s", "Terug naar album %s"),
    "pl" to searchCopy("Artyści, których słuchasz", "Wyszukaj wykonawcę %s", "Znajdź więcej utworów podobnych do %s", "Wróć do albumu %s"),
    "ro" to searchCopy("Artiști pe care îi asculți", "Caută artistul %s", "Găsește mai multe ca %s", "Înapoi la albumul %s"),
    "el" to searchCopy("Καλλιτέχνες που ακούς", "Αναζήτησε τον καλλιτέχνη %s", "Βρες περισσότερα σαν το %s", "Πίσω στο άλμπουμ %s"),
    "sv" to searchCopy("Artister du lyssnar på", "Sök efter artisten %s", "Hitta mer som %s", "Tillbaka till albumet %s"),
    "da" to searchCopy("Kunstnere, du lytter til", "Søg efter kunstneren %s", "Find mere som %s", "Tilbage til albummet %s"),
    "cs" to searchCopy("Interpreti, které posloucháš", "Vyhledat interpreta %s", "Najít další jako %s", "Zpět k albu %s"),
    "sk" to searchCopy("Interpreti, ktorých počúvaš", "Vyhľadať interpreta %s", "Nájsť viac ako %s", "Späť k albumu %s"),
    "hr" to searchCopy("Izvođači koje slušaš", "Potraži izvođača %s", "Pronađi još poput %s", "Natrag na album %s"),
    "bg" to searchCopy("Изпълнители, които слушаш", "Търси изпълнителя %s", "Намери още като %s", "Обратно към албума %s"),
    "hu" to searchCopy("Előadók, akiket hallgatsz", "%s előadó keresése", "További, ehhez hasonló zenék: %s", "Vissza ehhez az albumhoz: %s"),
    "fi" to searchCopy("Kuuntelemasi artistit", "Hae artistia %s", "Löydä lisää kappaleen %s kaltaisia", "Takaisin albumiin %s"),
    "et" to searchCopy("Esitajad, keda kuulad", "Otsi esitajat %s", "Leia rohkem nagu %s", "Tagasi albumi %s juurde"),
    "nb" to searchCopy("Artister du lytter til", "Søk etter artisten %s", "Finn mer som %s", "Tilbake til albumet %s"),
    "ca" to searchCopy("Artistes que escoltes", "Cerca l'artista %s", "Troba més música com %s", "Torna a l'àlbum %s"),
    "uk" to searchCopy("Виконавці, яких ти слухаєш", "Знайти виконавця %s", "Знайти більше схожого на %s", "Повернутися до альбому %s"),
    "ru" to searchCopy("Исполнители, которых ты слушаешь", "Найти исполнителя %s", "Найти больше похожего на %s", "Вернуться к альбому %s"),
    "tr" to searchCopy("Dinlediğin sanatçılar", "%s sanatçısını ara", "%s benzeri parçalar bul", "%s albümüne dön"),
    "ar" to searchCopy("فنانون تستمع إليهم", "ابحث عن الفنان %s", "اعثر على المزيد مثل %s", "عُد إلى الألبوم %s"),
    "fa" to searchCopy("هنرمندانی که گوش می‌دهید", "جست‌وجوی هنرمند %s", "آثار بیشتری شبیه %s پیدا کنید", "بازگشت به آلبوم %s"),
    "zh" to searchCopy("你常听的艺人", "搜索艺人 %s", "查找更多类似 %s 的歌曲", "返回专辑 %s"),
    "zh-Hant" to searchCopy("你常聽的藝人", "搜尋藝人 %s", "尋找更多類似 %s 的歌曲", "返回專輯 %s"),
    "ja" to searchCopy("よく聴くアーティスト", "アーティスト %s を検索", "%s に似た曲を探す", "アルバム %s に戻る"),
    "ko" to searchCopy("내가 즐겨 듣는 아티스트", "아티스트 %s 검색", "%s 같은 곡 더 찾기", "앨범 %s 다시 보기"),
    "hi" to searchCopy("आपके सुने हुए कलाकार", "कलाकार %s खोजें", "%s जैसे और गाने खोजें", "एल्बम %s पर वापस जाएँ"),
    "id" to searchCopy("Artis yang Anda dengarkan", "Cari artis %s", "Temukan lebih banyak seperti %s", "Kembali ke album %s"),
    "ms" to searchCopy("Artis yang anda dengar", "Cari artis %s", "Cari lebih banyak seperti %s", "Kembali ke album %s"),
    "vi" to searchCopy("Nghệ sĩ bạn thường nghe", "Tìm nghệ sĩ %s", "Tìm thêm bài giống %s", "Quay lại album %s"),
    "th" to searchCopy("ศิลปินที่คุณฟัง", "ค้นหาศิลปิน %s", "ค้นหาเพลงเพิ่มเติมที่คล้าย %s", "กลับไปที่อัลบั้ม %s"),
    "fil" to searchCopy("Mga artist na pinapakinggan mo", "Hanapin ang artist na %s", "Maghanap pa ng tulad ng %s", "Bumalik sa album na %s"),
    "he" to searchCopy("אמנים שבחרת להאזין להם", "חיפוש האמן %s", "מציאת שירים נוספים כמו %s", "חזרה לאלבום %s")
)

internal fun personalizedSearchLocalizationCodes(): Set<String> = PERSONALIZED_SEARCH_COPY.keys
