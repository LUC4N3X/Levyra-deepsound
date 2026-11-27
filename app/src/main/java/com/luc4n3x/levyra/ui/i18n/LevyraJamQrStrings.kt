package com.luc4n3x.levyra.ui.i18n

internal val jamQrKeys = setOf("jamScanQr", "jamScanToJoin", "jamCopyInvite", "jamInviteCopied", "jamScannerUnavailable", "jamQrUnavailable")

private fun jamQr(scan: String, join: String, copy: String, copied: String, unavailable: String, qrUnavailable: String) =
    listOf(scan, join, copy, copied, unavailable, qrUnavailable).let { jamQrKeys.zip(it).toMap() }

private val jamQrBundles = mapOf(
    "en" to jamQr("Scan QR code", "Scan to join", "Copy invite", "Invite copied", "Camera unavailable. Allow camera access in Settings or paste an invitation to join.", "QR code unavailable. Copy or share the invitation instead."),
    "it" to jamQr("Scansiona codice QR", "Scansiona per partecipare", "Copia invito", "Invito copiato", "Fotocamera non disponibile. Consenti l’accesso nelle Impostazioni o incolla un invito per partecipare.", "Codice QR non disponibile. Copia o condividi l’invito."),
    "es" to jamQr("Escanear código QR", "Escanea para unirte", "Copiar invitación", "Invitación copiada", "Cámara no disponible. Permite el acceso en Ajustes o pega una invitación.", "Código QR no disponible. Copia o comparte la invitación."),
    "fr" to jamQr("Scanner le code QR", "Scannez pour rejoindre", "Copier l’invitation", "Invitation copiée", "Caméra indisponible. Autorisez l’accès dans les paramètres ou collez une invitation.", "Code QR indisponible. Copiez ou partagez l’invitation."),
    "de" to jamQr("QR-Code scannen", "Scannen und beitreten", "Einladung kopieren", "Einladung kopiert", "Kamera nicht verfügbar. Erlaube den Zugriff in den Einstellungen oder füge eine Einladung ein.", "QR-Code nicht verfügbar. Kopiere oder teile die Einladung."),
    "ur" to jamQr("QR کوڈ اسکین کریں۔", "شامل ہونے کے لیے اسکین کریں۔", "دعوت نامہ کاپی کریں۔", "دعوت نامہ کاپی ہو گیا۔", "کیمرہ دستیاب نہیں ہے۔ سیٹنگز میں کیمرے تک رسائی کی اجازت دیں یا شمولیت کے لیے دعوت نامہ چسپاں کریں۔", "QR کوڈ دستیاب نہیں ہے۔ اس کے بجائے دعوت نامہ کاپی یا شیئر کریں۔"),
    "ta" to jamQr("QR குறியீட்டை ஸ்கேன் செய்யவும்", "சேர ஸ்கேன் செய்யவும்", "அழைப்பை நகலெடுக்கவும்", "அழைப்பு நகலெடுக்கப்பட்டது", "கேமரா கிடைக்கவில்லை. அமைப்புகளில் கேமரா அணுகலை அனுமதிக்கவும் அல்லது சேர்வதற்கான அழைப்பை ஒட்டவும்.", "QR குறியீடு கிடைக்கவில்லை. அதற்கு பதிலாக அழைப்பிதழை நகலெடுக்கவும் அல்லது பகிரவும்."),
    "te" to jamQr("QR కోడ్‌ని స్కాన్ చేయండి", "చేరడానికి స్కాన్ చేయండి", "ఆహ్వానాన్ని కాపీ చేయండి", "ఆహ్వానం కాపీ చేయబడింది", "కెమెరా అందుబాటులో లేదు. సెట్టింగ్‌లలో కెమెరా యాక్సెస్‌ను అనుమతించండి లేదా చేరడానికి ఆహ్వానాన్ని అతికించండి.", "QR కోడ్ అందుబాటులో లేదు. బదులుగా ఆహ్వానాన్ని కాపీ చేయండి లేదా షేర్ చేయండి."),
    "mr" to jamQr("QR कोड स्कॅन करा", "सामील होण्यासाठी स्कॅन करा", "आमंत्रण कॉपी करा", "आमंत्रण कॉपी केले", "कॅमेरा अनुपलब्ध. सेटिंग्जमध्ये कॅमेरा प्रवेशास अनुमती द्या किंवा सामील होण्यासाठी आमंत्रण पेस्ट करा.", "QR कोड अनुपलब्ध. त्याऐवजी आमंत्रण कॉपी करा किंवा शेअर करा."),
    "gu" to jamQr("QR કોડ સ્કેન કરો", "જોડાવા માટે સ્કેન કરો", "આમંત્રણની નકલ કરો", "આમંત્રણ કૉપિ કર્યું", "કૅમેરો અનુપલબ્ધ. સેટિંગ્સમાં કૅમેરાને ઍક્સેસ કરવાની મંજૂરી આપો અથવા જોડાવા માટેનું આમંત્રણ પેસ્ટ કરો.", "QR કોડ અનુપલબ્ધ. તેના બદલે આમંત્રણ કૉપિ કરો અથવા શેર કરો."),
    "pt-BR" to jamQr("Digitalize o código QR", "Digitalize para participar", "Copiar convite", "Convite copiado", "Câmera indisponível. Permita o acesso à câmera em Configurações ou cole um convite para participar.", "Código QR indisponível. Copie ou compartilhe o convite.")
)

internal fun jamQrLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(jamQrBundles, code)

internal val LevyraStrings.jamScanQr: String get() = value("jamScanQr")
internal val LevyraStrings.jamScanToJoin: String get() = value("jamScanToJoin")
internal val LevyraStrings.jamCopyInvite: String get() = value("jamCopyInvite")
internal val LevyraStrings.jamInviteCopied: String get() = value("jamInviteCopied")
internal val LevyraStrings.jamScannerUnavailable: String get() = value("jamScannerUnavailable")
internal val LevyraStrings.jamQrUnavailable: String get() = value("jamQrUnavailable")
