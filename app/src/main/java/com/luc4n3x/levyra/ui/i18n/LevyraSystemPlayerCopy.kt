package com.luc4n3x.levyra.ui.i18n

data class LevyraSystemPlayerCopy(
    val outputTitle: String,
    val outputSubtitle: String,
    val currentOutput: String,
    val chooseOutput: String,
    val systemManaged: String,
    val speaker: String,
    val bluetooth: String,
    val wired: String,
    val external: String,
    val streamQuality: String,
    val dspActive: String,
    val dspOff: String,
    val sleepChooseDuration: String,
    val sleepEndsAt: String,
    val sleepActive: String,
    val sleepAddFifteen: String,
    val sleepFadeActive: String,
    val releaseReady: String,
    val releaseFrom: String,
    val releaseTo: String,
    val releaseHighlights: String,
    val releaseProtection: String,
    val releaseProtectionDetail: String
)

fun LevyraStrings.systemPlayerCopy(): LevyraSystemPlayerCopy = when (code) {
    "it" -> LevyraSystemPlayerCopy(
        "Uscita audio", "Il percorso audio reale usato da Levyra", "IN RIPRODUZIONE SU", "Scegli uscita",
        "Gestito da Android", "Altoparlante", "Bluetooth", "Cablata", "Esterna", "Qualità stream",
        "DSP attivo", "DSP non attivo", "Scegli quando fermare la musica", "Termina alle", "TIMER ATTIVO",
        "+15 min", "Dissolvenza finale attiva", "Aggiornamento pronto", "Da", "A", "Novità principali",
        "Aggiornamento protetto", "APK ufficiale GitHub · pacchetto e firma verificati prima dell’installazione"
    )
    "es" -> LevyraSystemPlayerCopy(
        "Salida de audio", "La ruta de audio real que usa Levyra", "REPRODUCIENDO EN", "Elegir salida",
        "Gestionado por Android", "Altavoz", "Bluetooth", "Cable", "Externa", "Calidad del stream",
        "DSP activo", "DSP inactivo", "Elige cuándo detener la música", "Termina a las", "TEMPORIZADOR ACTIVO",
        "+15 min", "Fundido final activo", "Actualización lista", "De", "A", "Novedades principales",
        "Actualización protegida", "APK oficial de GitHub · paquete y firma verificados antes de instalar"
    )
    "fr" -> LevyraSystemPlayerCopy(
        "Sortie audio", "Le chemin audio réellement utilisé par Levyra", "LECTURE SUR", "Choisir la sortie",
        "Géré par Android", "Haut-parleur", "Bluetooth", "Filaire", "Externe", "Qualité du flux",
        "DSP actif", "DSP inactif", "Choisissez quand arrêter la musique", "Se termine à", "MINUTERIE ACTIVE",
        "+15 min", "Fondu final actif", "Mise à jour prête", "De", "À", "Principales nouveautés",
        "Mise à jour protégée", "APK GitHub officiel · paquet et signature vérifiés avant installation"
    )
    "de" -> LevyraSystemPlayerCopy(
        "Audioausgabe", "Der tatsächlich von Levyra verwendete Audioweg", "WIEDERGABE ÜBER", "Ausgabe wählen",
        "Von Android verwaltet", "Lautsprecher", "Bluetooth", "Kabel", "Extern", "Stream-Qualität",
        "DSP aktiv", "DSP inaktiv", "Wähle, wann die Musik stoppen soll", "Endet um", "TIMER AKTIV",
        "+15 Min.", "Ausblenden aktiv", "Update bereit", "Von", "Auf", "Wichtigste Neuerungen",
        "Geschütztes Update", "Offizielle GitHub-APK · Paket und Signatur werden vor Installation geprüft"
    )
    "pt" -> LevyraSystemPlayerCopy(
        "Saída de áudio", "O caminho de áudio realmente usado pelo Levyra", "A REPRODUZIR EM", "Escolher saída",
        "Gerido pelo Android", "Altifalante", "Bluetooth", "Com fios", "Externa", "Qualidade do stream",
        "DSP ativo", "DSP inativo", "Escolha quando parar a música", "Termina às", "TEMPORIZADOR ATIVO",
        "+15 min", "Fade final ativo", "Atualização pronta", "De", "Para", "Principais novidades",
        "Atualização protegida", "APK oficial do GitHub · pacote e assinatura verificados antes da instalação"
    )
    "ja" -> LevyraSystemPlayerCopy(
        "オーディオ出力", "Levyra が実際に使用している出力先", "再生先", "出力先を選択",
        "Android が管理", "スピーカー", "Bluetooth", "有線", "外部", "ストリーム品質",
        "DSP 有効", "DSP 無効", "音楽を停止するタイミングを選択", "終了時刻", "タイマー作動中",
        "+15分", "終了時フェード有効", "アップデートの準備完了", "現在", "更新後", "主な変更",
        "保護されたアップデート", "公式 GitHub APK · インストール前にパッケージと署名を検証"
    )
    else -> LevyraSystemPlayerCopy(
        "Audio output", "The real audio path Levyra is using", "PLAYING ON", "Choose output",
        "Managed by Android", "Speaker", "Bluetooth", "Wired", "External", "Stream quality",
        "DSP active", "DSP inactive", "Choose when the music should stop", "Ends at", "TIMER ACTIVE",
        "+15 min", "End fade active", "Update ready", "From", "To", "Key changes",
        "Protected update", "Official GitHub APK · package and signing identity verified before install"
    )
}
