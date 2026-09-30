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
    val sleepMinuteUnit: String,
    val sleepFadeActive: String,
    val releaseReady: String,
    val releaseFrom: String,
    val releaseTo: String,
    val releaseHighlights: String,
    val releaseProtection: String,
    val releaseProtectionDetail: String,
    val availableOutputs: String,
    val switchingOutput: String,
    val outputChangeFailed: String,
    val sampleRates: String,
    val channels: String,
    val openSystemOutput: String,
    val noOutputs: String
)

fun LevyraStrings.systemPlayerCopy(): LevyraSystemPlayerCopy = when (code) {
    "it" -> LevyraSystemPlayerCopy(
        "Uscita audio", "Il percorso audio reale usato da Levyra", "IN RIPRODUZIONE SU", "Scegli uscita",
        "Gestito da Android", "Altoparlante", "Bluetooth", "Cablata", "Esterna", "Qualità stream",
        "DSP attivo", "DSP non attivo", "Scegli quando fermare la musica", "Termina alle", "TIMER ATTIVO",
        "+15 min", "min", "Dissolvenza finale attiva", "Aggiornamento pronto", "Da", "A", "Novità principali",
        "Aggiornamento protetto", "APK ufficiale GitHub · pacchetto e firma verificati prima dell’installazione",
        "USCITE DISPONIBILI", "Cambio uscita…", "Impossibile cambiare uscita", "Frequenze", "Canali",
        "Apri selettore di sistema", "Nessuna uscita disponibile"
    )
    "es" -> LevyraSystemPlayerCopy(
        "Salida de audio", "La ruta de audio real que usa Levyra", "REPRODUCIENDO EN", "Elegir salida",
        "Gestionado por Android", "Altavoz", "Bluetooth", "Cable", "Externa", "Calidad del stream",
        "DSP activo", "DSP inactivo", "Elige cuándo detener la música", "Termina a las", "TEMPORIZADOR ACTIVO",
        "+15 min", "min", "Fundido final activo", "Actualización lista", "De", "A", "Novedades principales",
        "Actualización protegida", "APK oficial de GitHub · paquete y firma verificados antes de instalar",
        "SALIDAS DISPONIBLES", "Cambiando salida…", "No se pudo cambiar la salida", "Frecuencias", "Canales",
        "Abrir selector del sistema", "No hay salidas disponibles"
    )
    "fr" -> LevyraSystemPlayerCopy(
        "Sortie audio", "Le chemin audio réellement utilisé par Levyra", "LECTURE SUR", "Choisir la sortie",
        "Géré par Android", "Haut-parleur", "Bluetooth", "Filaire", "Externe", "Qualité du flux",
        "DSP actif", "DSP inactif", "Choisissez quand arrêter la musique", "Se termine à", "MINUTERIE ACTIVE",
        "+15 min", "min", "Fondu final actif", "Mise à jour prête", "De", "À", "Principales nouveautés",
        "Mise à jour protégée", "APK GitHub officiel · paquet et signature vérifiés avant installation",
        "SORTIES DISPONIBLES", "Changement de sortie…", "Impossible de changer la sortie", "Fréquences", "Canaux",
        "Ouvrir le sélecteur système", "Aucune sortie disponible"
    )
    "de" -> LevyraSystemPlayerCopy(
        "Audioausgabe", "Der tatsächlich von Levyra verwendete Audioweg", "WIEDERGABE ÜBER", "Ausgabe wählen",
        "Von Android verwaltet", "Lautsprecher", "Bluetooth", "Kabel", "Extern", "Stream-Qualität",
        "DSP aktiv", "DSP inaktiv", "Wähle, wann die Musik stoppen soll", "Endet um", "TIMER AKTIV",
        "+15 Min.", "Min.", "Ausblenden aktiv", "Update bereit", "Von", "Auf", "Wichtigste Neuerungen",
        "Geschütztes Update", "Offizielle GitHub-APK · Paket und Signatur werden vor Installation geprüft",
        "VERFÜGBARE AUSGÄNGE", "Ausgabe wird gewechselt…", "Ausgabe konnte nicht gewechselt werden", "Frequenzen", "Kanäle",
        "Systemauswahl öffnen", "Keine Ausgänge verfügbar"
    )
    "pt" -> LevyraSystemPlayerCopy(
        "Saída de áudio", "O caminho de áudio realmente usado pelo Levyra", "A REPRODUZIR EM", "Escolher saída",
        "Gerido pelo Android", "Altifalante", "Bluetooth", "Com fios", "Externa", "Qualidade do stream",
        "DSP ativo", "DSP inativo", "Escolha quando parar a música", "Termina às", "TEMPORIZADOR ATIVO",
        "+15 min", "min", "Fade final ativo", "Atualização pronta", "De", "Para", "Principais novidades",
        "Atualização protegida", "APK oficial do GitHub · pacote e assinatura verificados antes da instalação",
        "SAÍDAS DISPONÍVEIS", "A mudar a saída…", "Não foi possível mudar a saída", "Frequências", "Canais",
        "Abrir seletor do sistema", "Nenhuma saída disponível"
    )
    "ja" -> LevyraSystemPlayerCopy(
        "オーディオ出力", "Levyra が実際に使用している出力先", "再生先", "出力先を選択",
        "Android が管理", "スピーカー", "Bluetooth", "有線", "外部", "ストリーム品質",
        "DSP 有効", "DSP 無効", "音楽を停止するタイミングを選択", "終了時刻", "タイマー作動中",
        "+15分", "分", "終了時フェード有効", "アップデートの準備完了", "現在", "更新後", "主な変更",
        "保護されたアップデート", "公式 GitHub APK · インストール前にパッケージと署名を検証",
        "利用可能な出力先", "出力先を切り替えています…", "出力先を変更できませんでした", "周波数", "チャンネル",
        "システム選択画面を開く", "利用可能な出力先がありません"
    )
    "fi" -> LevyraSystemPlayerCopy(
        "Äänilähtö", "Todellinen äänipolku, jota Levyra käyttää", "TOISTETAAN LAITTEESSA", "Valitse lähtö",
        "Androidin hallitsema", "Kaiutin", "Bluetooth", "Langallinen", "Ulkoinen", "Suoratoiston laatu",
        "DSP aktiivinen", "DSP ei käytössä", "Valitse milloin musiikin tulisi pysähtyä", "Päättyy klo", "AJASTIN AKTIIVINEN",
        "+15 min", "min", "Loppuhäivytys aktiivinen", "Päivitys valmis", "Alkaen", "Päättyen", "Tärkeimmät muutokset",
        "Suojattu päivitys", "Virallinen GitHub APK · paketti ja allekirjoitus varmistettu ennen asennusta",
        "KÄYTETTÄVISSÄ OLEVAT LÄHDÖT", "Vaihdetaan lähtöä…", "Lähtöä ei voitu vaihtaa", "Taajuudet", "Kanavat",
        "Avaa järjestelmän valitsin", "Lähtöjä ei ole käytettävissä"
    )
    "et" -> LevyraSystemPlayerCopy(
        "Heliväljund", "Tegelik helitee, mida Levyra kasutab", "ESITATAKSE SEADMES", "Vali väljund",
        "Androidi hallatav", "Kõlar", "Bluetooth", "Juhtmega", "Väline", "Voo kvaliteet",
        "DSP aktiivne", "DSP passiivne", "Vali, millal muusika peaks peatuma", "Lõpeb kell", "TAIMER AKTIIVNE",
        "+15 min", "min", "Lõpu hajumine aktiivne", "Värskendus valmis", "Alates", "Kuni", "Peamised muudatused",
        "Kaitstud värskendus", "Ametlik GitHubi APK · pakett ja allkirjastuse identiteet kinnitatud enne paigaldamist",
        "SAADAOLEVAD VÄLJUNDID", "Väljundi vahetamine…", "Väljundit ei saanud vahetada", "Sagedused", "Kanalid",
        "Ava süsteemivalik", "Väljundeid pole saadaval"
    )
    else -> LevyraSystemPlayerCopy(
        "Audio output", "The real audio path Levyra is using", "PLAYING ON", "Choose output",
        "Managed by Android", "Speaker", "Bluetooth", "Wired", "External", "Stream quality",
        "DSP active", "DSP inactive", "Choose when the music should stop", "Ends at", "TIMER ACTIVE",
        "+15 min", "min", "End fade active", "Update ready", "From", "To", "Key changes",
        "Protected update", "Official GitHub APK · package and signing identity verified before install",
        "AVAILABLE OUTPUTS", "Switching output…", "Could not change output", "Sample rates", "Channels",
        "Open system output picker", "No outputs available"
    )
}
