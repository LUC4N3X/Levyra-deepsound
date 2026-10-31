package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal object LevyraDownloadRemovalStrings {
    private val prompts: Map<String, String> = mapOf(
        "en" to "Remove this song from your downloads?",
        "it" to "Vuoi rimuovere questo brano dai download?",
        "es" to "¿Quieres quitar esta canción de tus descargas?",
        "fr" to "Retirer ce titre de vos téléchargements ?",
        "de" to "Diesen Titel aus deinen Downloads entfernen?",
        "pt" to "Remover esta música das tuas transferências?",
        "nl" to "Dit nummer uit je downloads verwijderen?",
        "pl" to "Usunąć ten utwór z pobranych?",
        "ro" to "Elimini această melodie din descărcări?",
        "el" to "Να αφαιρεθεί αυτό το τραγούδι από τις λήψεις;",
        "sv" to "Ta bort låten från dina nedladdningar?",
        "da" to "Vil du fjerne denne sang fra dine downloads?",
        "cs" to "Odebrat tuto skladbu ze stažených?",
        "sk" to "Odstrániť túto skladbu zo stiahnutých?",
        "bn" to "এই গানটি ডাউনলোড থেকে সরাবেন?",
        "kk" to "Бұл әнді жүктеп алулардан жою керек пе?",
        "sr" to "Уклонити ову песму из преузимања?",
        "sq" to "Ta heqësh këtë këngë nga shkarkimet?",
        "hr" to "Ukloniti ovu pjesmu iz preuzimanja?",
        "bg" to "Да се премахне ли тази песен от изтеглянията?",
        "hu" to "Eltávolítod ezt a dalt a letöltések közül?",
        "fi" to "Poistetaanko tämä kappale latauksista?",
        "et" to "Kas eemaldada see lugu allalaadimistest?",
        "nb" to "Vil du fjerne denne sangen fra nedlastingene?",
        "ca" to "Vols treure aquesta cançó de les baixades?",
        "uk" to "Видалити цю пісню із завантажень?",
        "ru" to "Удалить эту песню из загрузок?",
        "tr" to "Bu şarkı indirilenlerden kaldırılsın mı?",
        "ar" to "هل تريد إزالة هذه الأغنية من التنزيلات؟",
        "fa" to "این آهنگ از دانلودها حذف شود؟",
        "zh" to "要从下载中移除这首歌曲吗？",
        "zh-Hant" to "要從下載中移除這首歌曲嗎？",
        "ja" to "この曲をダウンロードから削除しますか？",
        "ko" to "이 곡을 다운로드에서 삭제할까요?",
        "hi" to "क्या इस गाने को डाउनलोड से हटाना है?",
        "id" to "Hapus lagu ini dari unduhan?",
        "ms" to "Alih keluar lagu ini daripada muat turun?",
        "vi" to "Xóa bài hát này khỏi mục tải xuống?",
        "th" to "ลบเพลงนี้ออกจากรายการดาวน์โหลดใช่ไหม",
        "fil" to "Alisin ang kantang ito sa mga download?",
        "he" to "להסיר את השיר הזה מההורדות?"
    )

    init {
        check(prompts.keys == LevyraLanguageCatalog.languages.map { it.code }.toSet())
    }

    fun prompt(code: String): String = localizedValueOrEnglish(prompts, code)
}
