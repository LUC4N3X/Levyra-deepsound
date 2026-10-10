package com.luc4n3x.levyra.ui.i18n

internal data class LevyraUpdateReleaseCopy(
    val releaseReady: String,
    val releaseFrom: String,
    val releaseTo: String,
    val releaseHighlights: String,
    val releaseProtection: String,
    val releaseProtectionDetail: String
)

private fun updateReleaseCopy(
    ready: String,
    from: String,
    to: String,
    highlights: String,
    protection: String,
    protectionDetail: String
) = LevyraUpdateReleaseCopy(
    releaseReady = ready,
    releaseFrom = from,
    releaseTo = to,
    releaseHighlights = highlights,
    releaseProtection = protection,
    releaseProtectionDetail = protectionDetail
)

private val updateReleaseCopies: Map<String, LevyraUpdateReleaseCopy> = mapOf(
    "en" to updateReleaseCopy(
        "Update ready", "From", "To", "Key changes", "Protected update",
        "Official GitHub APK · package and signing identity verified before install"
    ),
    "it" to updateReleaseCopy(
        "Aggiornamento pronto", "Da", "A", "Novità principali", "Aggiornamento protetto",
        "APK ufficiale GitHub · pacchetto e firma verificati prima dell'installazione"
    ),
    "es" to updateReleaseCopy(
        "Actualización lista", "De", "A", "Novedades principales", "Actualización protegida",
        "APK oficial de GitHub · paquete y firma verificados antes de instalar"
    ),
    "fr" to updateReleaseCopy(
        "Mise à jour prête", "De", "À", "Principales nouveautés", "Mise à jour protégée",
        "APK GitHub officiel · paquet et signature vérifiés avant installation"
    ),
    "de" to updateReleaseCopy(
        "Update bereit", "Von", "Auf", "Wichtigste Änderungen", "Geschütztes Update",
        "Offizielle GitHub-APK · Paket und Signatur werden vor der Installation geprüft"
    ),
    "pt" to updateReleaseCopy(
        "Atualização pronta", "De", "Para", "Principais novidades", "Atualização protegida",
        "APK oficial do GitHub · pacote e assinatura verificados antes da instalação"
    ),
    "nl" to updateReleaseCopy(
        "Update gereed", "Van", "Naar", "Belangrijkste wijzigingen", "Beveiligde update",
        "Officiële GitHub-APK · pakket en ondertekeningsidentiteit worden vóór installatie gecontroleerd"
    ),
    "pl" to updateReleaseCopy(
        "Aktualizacja gotowa", "Z", "Do", "Najważniejsze zmiany", "Chroniona aktualizacja",
        "Oficjalny plik APK z GitHub · pakiet i tożsamość podpisu są weryfikowane przed instalacją"
    ),
    "ro" to updateReleaseCopy(
        "Actualizare pregătită", "De la", "La", "Modificări principale", "Actualizare protejată",
        "APK oficial de pe GitHub · pachetul și identitatea semnăturii sunt verificate înainte de instalare"
    ),
    "el" to updateReleaseCopy(
        "Η ενημέρωση είναι έτοιμη", "Από", "Σε", "Κύριες αλλαγές", "Προστατευμένη ενημέρωση",
        "Επίσημο APK από το GitHub · το πακέτο και η ταυτότητα υπογραφής επαληθεύονται πριν από την εγκατάσταση"
    ),
    "sv" to updateReleaseCopy(
        "Uppdatering klar", "Från", "Till", "Viktigaste ändringarna", "Skyddad uppdatering",
        "Officiell GitHub-APK · paket och signeringsidentitet verifieras före installation"
    ),
    "da" to updateReleaseCopy(
        "Opdatering klar", "Fra", "Til", "Vigtigste ændringer", "Beskyttet opdatering",
        "Officiel GitHub-APK · pakke og signeringsidentitet verificeres før installation"
    ),
    "cs" to updateReleaseCopy(
        "Aktualizace připravena", "Z", "Na", "Hlavní změny", "Chráněná aktualizace",
        "Oficiální APK z GitHubu · balíček a identita podpisu jsou před instalací ověřeny"
    ),
    "sk" to updateReleaseCopy(
        "Aktualizácia pripravená", "Z", "Na", "Hlavné zmeny", "Chránená aktualizácia",
        "Oficiálny APK z GitHubu · balík a identita podpisu sa overia pred inštaláciou"
    ),
    "hr" to updateReleaseCopy(
        "Ažuriranje je spremno", "S", "Na", "Glavne promjene", "Zaštićeno ažuriranje",
        "Službeni GitHub APK · paket i identitet potpisa provjeravaju se prije instalacije"
    ),
    "bg" to updateReleaseCopy(
        "Актуализацията е готова", "От", "До", "Основни промени", "Защитена актуализация",
        "Официален APK от GitHub · пакетът и идентичността на подписа се проверяват преди инсталиране"
    ),
    "hu" to updateReleaseCopy(
        "Frissítés kész", "Erről", "Erre", "Főbb változások", "Védett frissítés",
        "Hivatalos GitHub APK · a csomag és az aláírás azonossága telepítés előtt ellenőrizve"
    ),
    "fi" to updateReleaseCopy(
        "Päivitys valmis", "Alkaen", "Päättyen", "Tärkeimmät muutokset", "Suojattu päivitys",
        "Virallinen GitHub APK · paketti ja allekirjoitus varmistetaan ennen asennusta"
    ),
    "et" to updateReleaseCopy(
        "Värskendus valmis", "Alates", "Kuni", "Peamised muudatused", "Kaitstud värskendus",
        "Ametlik GitHubi APK · pakett ja allkirjastamise identiteet kinnitatakse enne paigaldamist"
    ),
    "nb" to updateReleaseCopy(
        "Oppdatering klar", "Fra", "Til", "Viktigste endringer", "Beskyttet oppdatering",
        "Offisiell GitHub-APK · pakke og signeringsidentitet verifiseres før installasjon"
    ),
    "ca" to updateReleaseCopy(
        "Actualització preparada", "De", "A", "Canvis principals", "Actualització protegida",
        "APK oficial de GitHub · el paquet i la identitat de signatura es verifiquen abans d'instal·lar"
    ),
    "uk" to updateReleaseCopy(
        "Оновлення готове", "З версії", "До версії", "Основні зміни", "Захищене оновлення",
        "Офіційний APK з GitHub · пакет і підпис перевіряються перед установленням"
    ),
    "ru" to updateReleaseCopy(
        "Обновление готово", "С версии", "До версии", "Основные изменения", "Защищённое обновление",
        "Официальный APK из GitHub · пакет и подпись проверяются перед установкой"
    ),
    "tr" to updateReleaseCopy(
        "Güncelleme hazır", "Sürümden", "Sürüme", "Başlıca değişiklikler", "Korumalı güncelleme",
        "Resmî GitHub APK'sı · paket ve imza kimliği kurulumdan önce doğrulanır"
    ),
    "ar" to updateReleaseCopy(
        "التحديث جاهز", "من", "إلى", "أهم التغييرات", "تحديث محمي",
        "ملف APK رسمي من GitHub · يتم التحقق من الحزمة وهوية التوقيع قبل التثبيت"
    ),
    "fa" to updateReleaseCopy(
        "به‌روزرسانی آماده است", "از", "به", "تغییرات اصلی", "به‌روزرسانی محافظت‌شده",
        "APK رسمی GitHub · بسته و هویت امضا پیش از نصب بررسی می‌شوند"
    ),
    "zh" to updateReleaseCopy(
        "更新已就绪", "从", "到", "主要变化", "受保护的更新",
        "GitHub 官方 APK · 安装前会验证软件包和签名身份"
    ),
    "zh-Hant" to updateReleaseCopy(
        "更新已就緒", "從", "到", "主要變更", "受保護的更新",
        "GitHub 官方 APK · 安裝前會驗證套件與簽署身分"
    ),
    "ja" to updateReleaseCopy(
        "アップデートの準備完了", "現在", "更新後", "主な変更", "保護されたアップデート",
        "公式 GitHub APK · インストール前にパッケージと署名を検証"
    ),
    "ko" to updateReleaseCopy(
        "업데이트 준비 완료", "현재", "업데이트 후", "주요 변경 사항", "보호된 업데이트",
        "GitHub 공식 APK · 설치 전에 패키지와 서명 ID를 확인합니다"
    ),
    "hi" to updateReleaseCopy(
        "अपडेट तैयार है", "मौजूदा", "नया", "मुख्य बदलाव", "सुरक्षित अपडेट",
        "आधिकारिक GitHub APK · इंस्टॉल करने से पहले पैकेज और हस्ताक्षर पहचान की पुष्टि की जाती है"
    ),
    "id" to updateReleaseCopy(
        "Pembaruan siap", "Dari", "Ke", "Perubahan utama", "Pembaruan terlindungi",
        "APK resmi GitHub · paket dan identitas tanda tangan diverifikasi sebelum instalasi"
    ),
    "ms" to updateReleaseCopy(
        "Kemas kini sedia", "Dari", "Ke", "Perubahan utama", "Kemas kini dilindungi",
        "APK GitHub rasmi · pakej dan identiti tandatangan disahkan sebelum pemasangan"
    ),
    "vi" to updateReleaseCopy(
        "Bản cập nhật đã sẵn sàng", "Từ", "Đến", "Thay đổi chính", "Bản cập nhật được bảo vệ",
        "APK GitHub chính thức · gói và danh tính chữ ký được xác minh trước khi cài đặt"
    ),
    "th" to updateReleaseCopy(
        "อัปเดตพร้อมแล้ว", "จาก", "เป็น", "การเปลี่ยนแปลงหลัก", "การอัปเดตที่ได้รับการปกป้อง",
        "APK อย่างเป็นทางการจาก GitHub · ตรวจสอบแพ็กเกจและลายเซ็นก่อนติดตั้ง"
    ),
    "fil" to updateReleaseCopy(
        "Handa na ang update", "Mula", "Patungo sa", "Pangunahing pagbabago", "Protektadong update",
        "Opisyal na GitHub APK · bineberipika ang package at signing identity bago i-install"
    ),
    "he" to updateReleaseCopy(
        "העדכון מוכן", "מ־", "אל", "שינויים עיקריים", "עדכון מוגן",
        "APK רשמי מ-GitHub · החבילה וזהות החתימה מאומתות לפני ההתקנה"
    )
)

internal fun LevyraStrings.updateReleaseCopy(): LevyraUpdateReleaseCopy =
    updateReleaseCopies[code] ?: updateReleaseCopies.getValue("en")

internal fun updateReleaseLocalizationCodes(): Set<String> = updateReleaseCopies.keys
