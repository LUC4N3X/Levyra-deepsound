package com.luc4n3x.levyra.ui.i18n

data class ParametricProfileCopy(
    val profiles: String,
    val yourProfiles: String,
    val autoEq: String,
    val builtIn: String,
    val newProfile: String,
    val edit: String,
    val rename: String,
    val duplicate: String,
    val delete: String,
    val deleteTitle: String,
    val deleteMessage: String,
    val active: String,
    val flat: String,
    val profileName: String,
    val nameRequired: String,
    val nameTaken: String,
    val save: String,
    val discardTitle: String,
    val discardMessage: String,
    val discard: String,
    val keepEditing: String,
    val band: String,
    val range: String,
    val invalidNumber: String,
    val copyName: String,
    val copyNameNumbered: String,
    val previewing: String,
    val emptyCustom: String,
    val profileOptions: String,
    val limitReached: String,
    val responseCurve: String
) {
    fun deleteTitle(name: String): String = deleteTitle.replace("%s", name)
    fun band(number: Int): String = band.replace("%d", number.toString())
    fun range(min: String, max: String): String = range.replace("%1\$s", min).replace("%2\$s", max)
    fun copyName(name: String): String = copyName.replace("%s", name)
    fun copyName(name: String, number: Int): String =
        copyNameNumbered.replace("%1\$s", name).replace("%2\$d", number.toString())
}

private fun profileCopy(vararg value: String): ParametricProfileCopy {
    require(value.size == 31)
    return ParametricProfileCopy(
        value[0], value[1], value[2], value[3], value[4], value[5], value[6], value[7], value[8], value[9],
        value[10], value[11], value[12], value[13], value[14], value[15], value[16], value[17], value[18], value[19],
        value[20], value[21], value[22], value[23], value[24], value[25], value[26], value[27], value[28], value[29],
        value[30]
    )
}

private val parametricProfileCopies = mapOf(
    "en" to profileCopy(
        "Profiles", "Your profiles", "AutoEQ", "Built-in", "New profile", "Edit", "Rename", "Duplicate", "Delete",
        "Delete “%s”?", "This profile will be removed from this device.", "Active", "Flat", "Profile name",
        "Enter a name", "You already have a profile with this name", "Save", "Discard changes?",
        "Your edits to this profile haven’t been saved.", "Discard", "Keep editing", "Band %d",
        "Between %1\$s and %2\$s", "Enter a valid number", "%s copy", "%1\$s copy %2\$d",
        "You’re hearing your edits live until you save", "Create a profile or duplicate one to shape your own sound",
        "Profile options", "You can keep up to 24 profiles", "Frequency response"
    ),
    "it" to profileCopy(
        "Profili", "I tuoi profili", "AutoEQ", "Predefiniti", "Nuovo profilo", "Modifica", "Rinomina", "Duplica", "Elimina",
        "Eliminare “%s”?", "Il profilo verrà rimosso da questo dispositivo.", "Attivo", "Piatto", "Nome del profilo",
        "Inserisci un nome", "Hai già un profilo con questo nome", "Salva", "Scartare le modifiche?",
        "Le modifiche a questo profilo non sono state salvate.", "Scarta", "Continua a modificare", "Banda %d",
        "Tra %1\$s e %2\$s", "Inserisci un numero valido", "%s copia", "%1\$s copia %2\$d",
        "Senti le modifiche dal vivo finché non salvi", "Crea un profilo o duplicane uno per modellare il tuo suono",
        "Opzioni profilo", "Puoi tenere fino a 24 profili", "Risposta in frequenza"
    ),
    "es" to profileCopy(
        "Perfiles", "Tus perfiles", "AutoEQ", "Integrados", "Nuevo perfil", "Editar", "Renombrar", "Duplicar", "Eliminar",
        "¿Eliminar «%s»?", "Este perfil se eliminará de este dispositivo.", "Activo", "Plano", "Nombre del perfil",
        "Escribe un nombre", "Ya tienes un perfil con este nombre", "Guardar", "¿Descartar los cambios?",
        "Los cambios en este perfil no se han guardado.", "Descartar", "Seguir editando", "Banda %d",
        "Entre %1\$s y %2\$s", "Escribe un número válido", "%s copia", "%1\$s copia %2\$d",
        "Escuchas tus cambios en directo hasta que guardes", "Crea un perfil o duplica uno para dar forma a tu sonido",
        "Opciones del perfil", "Puedes guardar hasta 24 perfiles", "Respuesta en frecuencia"
    ),
    "fr" to profileCopy(
        "Profils", "Vos profils", "AutoEQ", "Intégrés", "Nouveau profil", "Modifier", "Renommer", "Dupliquer", "Supprimer",
        "Supprimer « %s » ?", "Ce profil sera supprimé de cet appareil.", "Actif", "Neutre", "Nom du profil",
        "Saisissez un nom", "Vous avez déjà un profil portant ce nom", "Enregistrer", "Abandonner les modifications ?",
        "Les modifications de ce profil n’ont pas été enregistrées.", "Abandonner", "Continuer", "Bande %d",
        "Entre %1\$s et %2\$s", "Saisissez un nombre valide", "%s copie", "%1\$s copie %2\$d",
        "Vous entendez vos réglages en direct jusqu’à l’enregistrement", "Créez ou dupliquez un profil pour façonner votre son",
        "Options du profil", "Vous pouvez conserver jusqu’à 24 profils", "Réponse en fréquence"
    ),
    "de" to profileCopy(
        "Profile", "Deine Profile", "AutoEQ", "Integriert", "Neues Profil", "Bearbeiten", "Umbenennen", "Duplizieren", "Löschen",
        "„%s“ löschen?", "Dieses Profil wird von diesem Gerät entfernt.", "Aktiv", "Neutral", "Profilname",
        "Gib einen Namen ein", "Du hast bereits ein Profil mit diesem Namen", "Speichern", "Änderungen verwerfen?",
        "Deine Änderungen an diesem Profil wurden nicht gespeichert.", "Verwerfen", "Weiter bearbeiten", "Band %d",
        "Zwischen %1\$s und %2\$s", "Gib eine gültige Zahl ein", "%s Kopie", "%1\$s Kopie %2\$d",
        "Du hörst deine Änderungen live, bis du speicherst", "Erstelle oder dupliziere ein Profil für deinen eigenen Klang",
        "Profiloptionen", "Du kannst bis zu 24 Profile behalten", "Frequenzgang"
    ),
    "pt" to profileCopy(
        "Perfis", "Seus perfis", "AutoEQ", "Integrados", "Novo perfil", "Editar", "Renomear", "Duplicar", "Excluir",
        "Excluir “%s”?", "Este perfil será removido deste dispositivo.", "Ativo", "Plano", "Nome do perfil",
        "Digite um nome", "Você já tem um perfil com este nome", "Salvar", "Descartar alterações?",
        "As alterações neste perfil não foram salvas.", "Descartar", "Continuar editando", "Banda %d",
        "Entre %1\$s e %2\$s", "Digite um número válido", "%s cópia", "%1\$s cópia %2\$d",
        "Você ouve as alterações ao vivo até salvar", "Crie ou duplique um perfil para moldar seu som",
        "Opções do perfil", "Você pode manter até 24 perfis", "Resposta de frequência"
    ),
    "nl" to profileCopy(
        "Profielen", "Jouw profielen", "AutoEQ", "Ingebouwd", "Nieuw profiel", "Bewerken", "Hernoemen", "Dupliceren", "Verwijderen",
        "‘%s’ verwijderen?", "Dit profiel wordt van dit apparaat verwijderd.", "Actief", "Vlak", "Profielnaam",
        "Voer een naam in", "Je hebt al een profiel met deze naam", "Opslaan", "Wijzigingen verwerpen?",
        "Je wijzigingen aan dit profiel zijn niet opgeslagen.", "Verwerpen", "Verder bewerken", "Band %d",
        "Tussen %1\$s en %2\$s", "Voer een geldig getal in", "%s kopie", "%1\$s kopie %2\$d",
        "Je hoort je wijzigingen live tot je opslaat", "Maak of dupliceer een profiel om je eigen geluid te vormen",
        "Profielopties", "Je kunt tot 24 profielen bewaren", "Frequentierespons"
    ),
    "pl" to profileCopy(
        "Profile", "Twoje profile", "AutoEQ", "Wbudowane", "Nowy profil", "Edytuj", "Zmień nazwę", "Duplikuj", "Usuń",
        "Usunąć „%s”?", "Ten profil zostanie usunięty z tego urządzenia.", "Aktywny", "Płaski", "Nazwa profilu",
        "Wpisz nazwę", "Masz już profil o tej nazwie", "Zapisz", "Odrzucić zmiany?",
        "Zmiany w tym profilu nie zostały zapisane.", "Odrzuć", "Edytuj dalej", "Pasmo %d",
        "Od %1\$s do %2\$s", "Wpisz prawidłową liczbę", "%s kopia", "%1\$s kopia %2\$d",
        "Słyszysz zmiany na żywo, dopóki nie zapiszesz", "Utwórz lub zduplikuj profil, aby nadać brzmieniu własny charakter",
        "Opcje profilu", "Możesz mieć maksymalnie 24 profile", "Charakterystyka częstotliwościowa"
    ),
    "ro" to profileCopy(
        "Profiluri", "Profilurile tale", "AutoEQ", "Integrate", "Profil nou", "Editează", "Redenumește", "Duplică", "Șterge",
        "Ștergi „%s”?", "Profilul va fi eliminat de pe acest dispozitiv.", "Activ", "Plat", "Numele profilului",
        "Introdu un nume", "Ai deja un profil cu acest nume", "Salvează", "Renunți la modificări?",
        "Modificările acestui profil nu au fost salvate.", "Renunță", "Continuă editarea", "Banda %d",
        "Între %1\$s și %2\$s", "Introdu un număr valid", "%s copie", "%1\$s copie %2\$d",
        "Auzi modificările live până salvezi", "Creează sau duplică un profil pentru a-ți modela sunetul",
        "Opțiuni profil", "Poți păstra până la 24 de profiluri", "Răspuns în frecvență"
    ),
    "el" to profileCopy(
        "Προφίλ", "Τα προφίλ σας", "AutoEQ", "Ενσωματωμένα", "Νέο προφίλ", "Επεξεργασία", "Μετονομασία", "Αντιγραφή", "Διαγραφή",
        "Διαγραφή «%s»;", "Το προφίλ θα αφαιρεθεί από αυτή τη συσκευή.", "Ενεργό", "Επίπεδο", "Όνομα προφίλ",
        "Εισαγάγετε όνομα", "Υπάρχει ήδη προφίλ με αυτό το όνομα", "Αποθήκευση", "Απόρριψη αλλαγών;",
        "Οι αλλαγές σε αυτό το προφίλ δεν έχουν αποθηκευτεί.", "Απόρριψη", "Συνέχεια επεξεργασίας", "Ζώνη %d",
        "Από %1\$s έως %2\$s", "Εισαγάγετε έγκυρο αριθμό", "%s αντίγραφο", "%1\$s αντίγραφο %2\$d",
        "Ακούτε τις αλλαγές ζωντανά μέχρι να αποθηκεύσετε", "Δημιουργήστε ή αντιγράψτε ένα προφίλ για τον δικό σας ήχο",
        "Επιλογές προφίλ", "Μπορείτε να κρατήσετε έως 24 προφίλ", "Απόκριση συχνότητας"
    ),
    "sv" to profileCopy(
        "Profiler", "Dina profiler", "AutoEQ", "Inbyggda", "Ny profil", "Redigera", "Byt namn", "Duplicera", "Ta bort",
        "Ta bort ”%s”?", "Profilen tas bort från den här enheten.", "Aktiv", "Neutral", "Profilnamn",
        "Ange ett namn", "Du har redan en profil med det här namnet", "Spara", "Förkasta ändringar?",
        "Ändringarna i profilen har inte sparats.", "Förkasta", "Fortsätt redigera", "Band %d",
        "Mellan %1\$s och %2\$s", "Ange ett giltigt tal", "%s kopia", "%1\$s kopia %2\$d",
        "Du hör ändringarna live tills du sparar", "Skapa eller duplicera en profil för att forma ditt ljud",
        "Profilalternativ", "Du kan ha upp till 24 profiler", "Frekvensgång"
    ),
    "da" to profileCopy(
        "Profiler", "Dine profiler", "AutoEQ", "Indbyggede", "Ny profil", "Rediger", "Omdøb", "Dupliker", "Slet",
        "Slet “%s”?", "Profilen fjernes fra denne enhed.", "Aktiv", "Neutral", "Profilnavn",
        "Angiv et navn", "Du har allerede en profil med dette navn", "Gem", "Kassér ændringer?",
        "Dine ændringer i profilen er ikke gemt.", "Kassér", "Fortsæt redigering", "Bånd %d",
        "Mellem %1\$s og %2\$s", "Angiv et gyldigt tal", "%s kopi", "%1\$s kopi %2\$d",
        "Du hører ændringerne live, indtil du gemmer", "Opret eller dupliker en profil for at forme din lyd",
        "Profilindstillinger", "Du kan have op til 24 profiler", "Frekvensgang"
    ),
    "cs" to profileCopy(
        "Profily", "Vaše profily", "AutoEQ", "Vestavěné", "Nový profil", "Upravit", "Přejmenovat", "Duplikovat", "Smazat",
        "Smazat „%s“?", "Profil bude z tohoto zařízení odstraněn.", "Aktivní", "Rovný", "Název profilu",
        "Zadejte název", "Profil s tímto názvem už máte", "Uložit", "Zahodit změny?",
        "Změny v tomto profilu nebyly uloženy.", "Zahodit", "Pokračovat v úpravách", "Pásmo %d",
        "Mezi %1\$s a %2\$s", "Zadejte platné číslo", "%s kopie", "%1\$s kopie %2\$d",
        "Změny slyšíte živě, dokud neuložíte", "Vytvořte nebo duplikujte profil a vytvarujte si vlastní zvuk",
        "Možnosti profilu", "Můžete mít až 24 profilů", "Frekvenční charakteristika"
    ),
    "sk" to profileCopy(
        "Profily", "Vaše profily", "AutoEQ", "Vstavané", "Nový profil", "Upraviť", "Premenovať", "Duplikovať", "Odstrániť",
        "Odstrániť „%s“?", "Profil sa z tohto zariadenia odstráni.", "Aktívny", "Rovný", "Názov profilu",
        "Zadajte názov", "Profil s týmto názvom už máte", "Uložiť", "Zahodiť zmeny?",
        "Zmeny v tomto profile neboli uložené.", "Zahodiť", "Pokračovať v úpravách", "Pásmo %d",
        "Medzi %1\$s a %2\$s", "Zadajte platné číslo", "%s kópia", "%1\$s kópia %2\$d",
        "Zmeny počujete naživo, kým neuložíte", "Vytvorte alebo duplikujte profil a vytvarujte si vlastný zvuk",
        "Možnosti profilu", "Môžete mať najviac 24 profilov", "Frekvenčná charakteristika"
    ),
    "hr" to profileCopy(
        "Profili", "Vaši profili", "AutoEQ", "Ugrađeni", "Novi profil", "Uredi", "Preimenuj", "Dupliciraj", "Izbriši",
        "Izbrisati „%s”?", "Profil će biti uklonjen s ovog uređaja.", "Aktivan", "Ravan", "Naziv profila",
        "Unesite naziv", "Već imate profil s tim nazivom", "Spremi", "Odbaciti promjene?",
        "Promjene ovog profila nisu spremljene.", "Odbaci", "Nastavi uređivati", "Pojas %d",
        "Između %1\$s i %2\$s", "Unesite valjani broj", "%s kopija", "%1\$s kopija %2\$d",
        "Promjene čujete uživo dok ne spremite", "Izradite ili duplicirajte profil i oblikujte vlastiti zvuk",
        "Mogućnosti profila", "Možete imati do 24 profila", "Frekvencijski odziv"
    ),
    "bg" to profileCopy(
        "Профили", "Вашите профили", "AutoEQ", "Вградени", "Нов профил", "Редактиране", "Преименуване", "Дублиране", "Изтриване",
        "Изтриване на „%s“?", "Профилът ще бъде премахнат от това устройство.", "Активен", "Равен", "Име на профила",
        "Въведете име", "Вече имате профил с това име", "Запазване", "Отхвърляне на промените?",
        "Промените в този профил не са запазени.", "Отхвърляне", "Продължаване", "Лента %d",
        "Между %1\$s и %2\$s", "Въведете валидно число", "%s копие", "%1\$s копие %2\$d",
        "Чувате промените на живо, докато не запазите", "Създайте или дублирайте профил, за да оформите своя звук",
        "Опции на профила", "Можете да имате до 24 профила", "Честотна характеристика"
    ),
    "hu" to profileCopy(
        "Profilok", "Saját profilok", "AutoEQ", "Beépített", "Új profil", "Szerkesztés", "Átnevezés", "Duplikálás", "Törlés",
        "Törlöd: „%s”?", "A profil törlődik erről az eszközről.", "Aktív", "Lineáris", "Profilnév",
        "Adj meg egy nevet", "Már van ilyen nevű profilod", "Mentés", "Elveted a módosításokat?",
        "A profil módosításai nincsenek mentve.", "Elvetés", "Szerkesztés folytatása", "%d. sáv",
        "%1\$s és %2\$s között", "Adj meg egy érvényes számot", "%s másolat", "%1\$s másolat %2\$d",
        "A módosításokat élőben hallod a mentésig", "Hozz létre vagy duplikálj egy profilt a saját hangzásodhoz",
        "Profilbeállítások", "Legfeljebb 24 profilt tarthatsz meg", "Frekvenciaválasz"
    ),
    "fi" to profileCopy(
        "Profiilit", "Omat profiilit", "AutoEQ", "Valmiit", "Uusi profiili", "Muokkaa", "Nimeä uudelleen", "Monista", "Poista",
        "Poistetaanko ”%s”?", "Profiili poistetaan tältä laitteelta.", "Käytössä", "Tasainen", "Profiilin nimi",
        "Anna nimi", "Sinulla on jo samanniminen profiili", "Tallenna", "Hylätäänkö muutokset?",
        "Profiilin muutoksia ei ole tallennettu.", "Hylkää", "Jatka muokkausta", "Kaista %d",
        "Välillä %1\$s–%2\$s", "Anna kelvollinen luku", "%s kopio", "%1\$s kopio %2\$d",
        "Kuulet muutokset suorana, kunnes tallennat", "Luo tai monista profiili ja muotoile oma soundisi",
        "Profiilin valinnat", "Voit pitää enintään 24 profiilia", "Taajuusvaste"
    ),
    "et" to profileCopy(
        "Profiilid", "Sinu profiilid", "AutoEQ", "Sisseehitatud", "Uus profiil", "Muuda", "Nimeta ümber", "Dubleeri", "Kustuta",
        "Kas kustutada „%s”?", "Profiil eemaldatakse sellest seadmest.", "Aktiivne", "Tasane", "Profiili nimi",
        "Sisesta nimi", "Sul on juba sama nimega profiil", "Salvesta", "Kas loobuda muudatustest?",
        "Selle profiili muudatused on salvestamata.", "Loobu", "Jätka muutmist", "Riba %d",
        "Vahemikus %1\$s–%2\$s", "Sisesta kehtiv arv", "%s koopia", "%1\$s koopia %2\$d",
        "Kuuled muudatusi otse, kuni salvestad", "Loo või dubleeri profiil, et kujundada oma heli",
        "Profiili valikud", "Saad hoida kuni 24 profiili", "Sageduskarakteristik"
    ),
    "nb" to profileCopy(
        "Profiler", "Dine profiler", "AutoEQ", "Innebygde", "Ny profil", "Rediger", "Gi nytt navn", "Dupliser", "Slett",
        "Slette «%s»?", "Profilen fjernes fra denne enheten.", "Aktiv", "Nøytral", "Profilnavn",
        "Skriv inn et navn", "Du har allerede en profil med dette navnet", "Lagre", "Forkaste endringene?",
        "Endringene i profilen er ikke lagret.", "Forkast", "Fortsett å redigere", "Bånd %d",
        "Mellom %1\$s og %2\$s", "Skriv inn et gyldig tall", "%s kopi", "%1\$s kopi %2\$d",
        "Du hører endringene direkte til du lagrer", "Opprett eller dupliser en profil for å forme lyden din",
        "Profilvalg", "Du kan ha opptil 24 profiler", "Frekvensrespons"
    ),
    "ca" to profileCopy(
        "Perfils", "Els teus perfils", "AutoEQ", "Integrats", "Perfil nou", "Edita", "Canvia el nom", "Duplica", "Suprimeix",
        "Vols suprimir «%s»?", "Aquest perfil se suprimirà d’aquest dispositiu.", "Actiu", "Pla", "Nom del perfil",
        "Escriu un nom", "Ja tens un perfil amb aquest nom", "Desa", "Vols descartar els canvis?",
        "Els canvis d’aquest perfil no s’han desat.", "Descarta", "Continua editant", "Banda %d",
        "Entre %1\$s i %2\$s", "Escriu un número vàlid", "%s còpia", "%1\$s còpia %2\$d",
        "Sents els canvis en directe fins que desis", "Crea o duplica un perfil per donar forma al teu so",
        "Opcions del perfil", "Pots tenir fins a 24 perfils", "Resposta en freqüència"
    ),
    "uk" to profileCopy(
        "Профілі", "Ваші профілі", "AutoEQ", "Вбудовані", "Новий профіль", "Редагувати", "Перейменувати", "Дублювати", "Видалити",
        "Видалити «%s»?", "Профіль буде видалено з цього пристрою.", "Активний", "Рівний", "Назва профілю",
        "Введіть назву", "У вас уже є профіль із такою назвою", "Зберегти", "Скасувати зміни?",
        "Зміни в цьому профілі не збережено.", "Скасувати", "Продовжити редагування", "Смуга %d",
        "Від %1\$s до %2\$s", "Введіть коректне число", "%s копія", "%1\$s копія %2\$d",
        "Ви чуєте зміни наживо, доки не збережете", "Створіть або дублюйте профіль, щоб сформувати власний звук",
        "Параметри профілю", "Можна зберігати до 24 профілів", "Амплітудно-частотна характеристика"
    ),
    "ru" to profileCopy(
        "Профили", "Ваши профили", "AutoEQ", "Встроенные", "Новый профиль", "Изменить", "Переименовать", "Дублировать", "Удалить",
        "Удалить «%s»?", "Профиль будет удалён с этого устройства.", "Активен", "Ровный", "Название профиля",
        "Введите название", "У вас уже есть профиль с таким названием", "Сохранить", "Отменить изменения?",
        "Изменения в этом профиле не сохранены.", "Отменить", "Продолжить", "Полоса %d",
        "От %1\$s до %2\$s", "Введите корректное число", "%s копия", "%1\$s копия %2\$d",
        "Вы слышите изменения вживую, пока не сохраните", "Создайте или продублируйте профиль, чтобы настроить свой звук",
        "Параметры профиля", "Можно хранить до 24 профилей", "АЧХ"
    ),
    "tr" to profileCopy(
        "Profiller", "Profillerin", "AutoEQ", "Yerleşik", "Yeni profil", "Düzenle", "Yeniden adlandır", "Çoğalt", "Sil",
        "“%s” silinsin mi?", "Bu profil bu cihazdan kaldırılacak.", "Etkin", "Düz", "Profil adı",
        "Bir ad girin", "Bu adda bir profilin zaten var", "Kaydet", "Değişiklikler atılsın mı?",
        "Bu profildeki değişiklikler kaydedilmedi.", "At", "Düzenlemeye devam et", "Bant %d",
        "%1\$s ile %2\$s arasında", "Geçerli bir sayı girin", "%s kopyası", "%1\$s kopyası %2\$d",
        "Kaydedene kadar değişiklikleri canlı duyarsın", "Kendi sesini şekillendirmek için bir profil oluştur veya çoğalt",
        "Profil seçenekleri", "En fazla 24 profil tutabilirsin", "Frekans yanıtı"
    ),
    "ar" to profileCopy(
        "الملفات الشخصية", "ملفاتك الشخصية", "AutoEQ", "مدمجة", "ملف شخصي جديد", "تعديل", "إعادة تسمية", "تكرار", "حذف",
        "هل تريد حذف «%s»؟", "ستتم إزالة هذا الملف الشخصي من هذا الجهاز.", "نشط", "مستوٍ", "اسم الملف الشخصي",
        "أدخل اسمًا", "لديك بالفعل ملف شخصي بهذا الاسم", "حفظ", "تجاهل التغييرات؟",
        "لم يتم حفظ تعديلاتك على هذا الملف الشخصي.", "تجاهل", "متابعة التعديل", "النطاق %d",
        "بين %1\$s و%2\$s", "أدخل رقمًا صالحًا", "نسخة من %s", "نسخة من %1\$s %2\$d",
        "تسمع تعديلاتك مباشرة حتى تحفظ", "أنشئ ملفًا شخصيًا أو كرّر واحدًا لتشكيل صوتك",
        "خيارات الملف الشخصي", "يمكنك الاحتفاظ بما يصل إلى 24 ملفًا شخصيًا", "الاستجابة الترددية"
    ),
    "fa" to profileCopy(
        "پروفایل‌ها", "پروفایل‌های شما", "AutoEQ", "داخلی", "پروفایل جدید", "ویرایش", "تغییر نام", "تکثیر", "حذف",
        "«%s» حذف شود؟", "این پروفایل از این دستگاه حذف می‌شود.", "فعال", "صاف", "نام پروفایل",
        "یک نام وارد کنید", "از قبل پروفایلی با این نام دارید", "ذخیره", "تغییرات کنار گذاشته شود؟",
        "تغییرات این پروفایل ذخیره نشده است.", "کنار گذاشتن", "ادامهٔ ویرایش", "باند %d",
        "بین %1\$s و %2\$s", "یک عدد معتبر وارد کنید", "رونوشت %s", "رونوشت %1\$s %2\$d",
        "تا ذخیره نکنید، تغییرات را زنده می‌شنوید", "برای ساختن صدای خودتان یک پروفایل بسازید یا تکثیر کنید",
        "گزینه‌های پروفایل", "حداکثر ۲۴ پروفایل می‌توانید نگه دارید", "پاسخ فرکانسی"
    ),
    "zh" to profileCopy(
        "配置", "我的配置", "AutoEQ", "内置", "新建配置", "编辑", "重命名", "复制", "删除",
        "删除“%s”？", "此配置将从这台设备上移除。", "使用中", "平直", "配置名称",
        "请输入名称", "已有同名配置", "保存", "放弃更改？",
        "此配置的更改尚未保存。", "放弃", "继续编辑", "频段 %d",
        "%1\$s 至 %2\$s 之间", "请输入有效数字", "%s 副本", "%1\$s 副本 %2\$d",
        "保存前你会实时听到更改", "新建或复制配置，打造你自己的声音",
        "配置选项", "最多可保留 24 个配置", "频率响应"
    ),
    "zh-Hant" to profileCopy(
        "設定檔", "我的設定檔", "AutoEQ", "內建", "新增設定檔", "編輯", "重新命名", "複製", "刪除",
        "要刪除「%s」嗎？", "此設定檔將從這部裝置移除。", "使用中", "平直", "設定檔名稱",
        "請輸入名稱", "已有同名的設定檔", "儲存", "要捨棄變更嗎？",
        "此設定檔的變更尚未儲存。", "捨棄", "繼續編輯", "頻段 %d",
        "介於 %1\$s 與 %2\$s", "請輸入有效數字", "%s 副本", "%1\$s 副本 %2\$d",
        "儲存前你會即時聽到變更", "新增或複製設定檔，打造你自己的聲音",
        "設定檔選項", "最多可保留 24 個設定檔", "頻率響應"
    ),
    "ja" to profileCopy(
        "プロファイル", "マイプロファイル", "AutoEQ", "内蔵", "新規プロファイル", "編集", "名前を変更", "複製", "削除",
        "「%s」を削除しますか？", "このプロファイルはこのデバイスから削除されます。", "使用中", "フラット", "プロファイル名",
        "名前を入力してください", "同じ名前のプロファイルがすでにあります", "保存", "変更を破棄しますか？",
        "このプロファイルの変更は保存されていません。", "破棄", "編集を続ける", "バンド %d",
        "%1\$s～%2\$s", "有効な数値を入力してください", "%s のコピー", "%1\$s のコピー %2\$d",
        "保存するまで変更をリアルタイムで試聴できます", "プロファイルを作成または複製して自分だけのサウンドに",
        "プロファイルのオプション", "プロファイルは最大 24 個まで保存できます", "周波数特性"
    ),
    "ko" to profileCopy(
        "프로필", "내 프로필", "AutoEQ", "기본 제공", "새 프로필", "편집", "이름 변경", "복제", "삭제",
        "‘%s’을(를) 삭제할까요?", "이 기기에서 프로필이 삭제됩니다.", "사용 중", "플랫", "프로필 이름",
        "이름을 입력하세요", "같은 이름의 프로필이 이미 있어요", "저장", "변경사항을 삭제할까요?",
        "이 프로필의 변경사항이 저장되지 않았어요.", "삭제", "계속 편집", "밴드 %d",
        "%1\$s~%2\$s", "올바른 숫자를 입력하세요", "%s 사본", "%1\$s 사본 %2\$d",
        "저장할 때까지 변경사항을 실시간으로 들을 수 있어요", "프로필을 만들거나 복제해 나만의 사운드를 만드세요",
        "프로필 옵션", "프로필은 최대 24개까지 보관할 수 있어요", "주파수 응답"
    ),
    "hi" to profileCopy(
        "प्रोफ़ाइल", "आपकी प्रोफ़ाइल", "AutoEQ", "बिल्ट-इन", "नई प्रोफ़ाइल", "संपादित करें", "नाम बदलें", "डुप्लिकेट करें", "हटाएँ",
        "“%s” हटाएँ?", "यह प्रोफ़ाइल इस डिवाइस से हटा दी जाएगी।", "सक्रिय", "फ़्लैट", "प्रोफ़ाइल का नाम",
        "नाम दर्ज करें", "इस नाम की प्रोफ़ाइल पहले से है", "सहेजें", "बदलाव छोड़ें?",
        "इस प्रोफ़ाइल के बदलाव सहेजे नहीं गए हैं।", "छोड़ें", "संपादन जारी रखें", "बैंड %d",
        "%1\$s और %2\$s के बीच", "मान्य संख्या दर्ज करें", "%s कॉपी", "%1\$s कॉपी %2\$d",
        "सहेजने तक आप बदलाव लाइव सुनते हैं", "अपनी आवाज़ गढ़ने के लिए प्रोफ़ाइल बनाएँ या डुप्लिकेट करें",
        "प्रोफ़ाइल विकल्प", "आप अधिकतम 24 प्रोफ़ाइल रख सकते हैं", "फ़्रीक्वेंसी रिस्पॉन्स"
    ),
    "id" to profileCopy(
        "Profil", "Profil Anda", "AutoEQ", "Bawaan", "Profil baru", "Edit", "Ganti nama", "Duplikat", "Hapus",
        "Hapus “%s”?", "Profil ini akan dihapus dari perangkat ini.", "Aktif", "Datar", "Nama profil",
        "Masukkan nama", "Anda sudah punya profil dengan nama ini", "Simpan", "Buang perubahan?",
        "Perubahan pada profil ini belum disimpan.", "Buang", "Lanjut mengedit", "Band %d",
        "Antara %1\$s dan %2\$s", "Masukkan angka yang valid", "%s salinan", "%1\$s salinan %2\$d",
        "Anda mendengar perubahan secara langsung sampai disimpan", "Buat atau duplikat profil untuk membentuk suara Anda",
        "Opsi profil", "Anda dapat menyimpan hingga 24 profil", "Respons frekuensi"
    ),
    "ms" to profileCopy(
        "Profil", "Profil anda", "AutoEQ", "Terbina dalam", "Profil baharu", "Edit", "Namakan semula", "Pendua", "Padam",
        "Padam “%s”?", "Profil ini akan dialih keluar daripada peranti ini.", "Aktif", "Rata", "Nama profil",
        "Masukkan nama", "Anda sudah mempunyai profil dengan nama ini", "Simpan", "Buang perubahan?",
        "Perubahan pada profil ini belum disimpan.", "Buang", "Terus mengedit", "Jalur %d",
        "Antara %1\$s dan %2\$s", "Masukkan nombor yang sah", "Salinan %s", "Salinan %1\$s %2\$d",
        "Anda mendengar perubahan secara langsung sehingga disimpan", "Cipta atau pendua profil untuk membentuk bunyi anda",
        "Pilihan profil", "Anda boleh menyimpan sehingga 24 profil", "Sambutan frekuensi"
    ),
    "vi" to profileCopy(
        "Cấu hình", "Cấu hình của bạn", "AutoEQ", "Tích hợp", "Cấu hình mới", "Chỉnh sửa", "Đổi tên", "Nhân bản", "Xóa",
        "Xóa “%s”?", "Cấu hình này sẽ bị xóa khỏi thiết bị.", "Đang dùng", "Phẳng", "Tên cấu hình",
        "Nhập tên", "Bạn đã có cấu hình trùng tên", "Lưu", "Bỏ thay đổi?",
        "Các thay đổi của cấu hình này chưa được lưu.", "Bỏ", "Tiếp tục chỉnh sửa", "Dải %d",
        "Từ %1\$s đến %2\$s", "Nhập một số hợp lệ", "Bản sao %s", "Bản sao %1\$s %2\$d",
        "Bạn nghe thay đổi trực tiếp cho đến khi lưu", "Tạo hoặc nhân bản cấu hình để định hình âm thanh của bạn",
        "Tùy chọn cấu hình", "Bạn có thể giữ tối đa 24 cấu hình", "Đáp tuyến tần số"
    ),
    "th" to profileCopy(
        "โปรไฟล์", "โปรไฟล์ของคุณ", "AutoEQ", "ในตัว", "โปรไฟล์ใหม่", "แก้ไข", "เปลี่ยนชื่อ", "ทำสำเนา", "ลบ",
        "ลบ “%s” ไหม", "โปรไฟล์นี้จะถูกลบออกจากอุปกรณ์นี้", "ใช้งานอยู่", "แบน", "ชื่อโปรไฟล์",
        "ป้อนชื่อ", "คุณมีโปรไฟล์ชื่อนี้อยู่แล้ว", "บันทึก", "ทิ้งการเปลี่ยนแปลงไหม",
        "การเปลี่ยนแปลงโปรไฟล์นี้ยังไม่ได้บันทึก", "ทิ้ง", "แก้ไขต่อ", "แบนด์ %d",
        "ระหว่าง %1\$s ถึง %2\$s", "ป้อนตัวเลขที่ถูกต้อง", "สำเนา %s", "สำเนา %1\$s %2\$d",
        "คุณได้ยินการเปลี่ยนแปลงแบบสดจนกว่าจะบันทึก", "สร้างหรือทำสำเนาโปรไฟล์เพื่อปรับเสียงในแบบของคุณ",
        "ตัวเลือกโปรไฟล์", "เก็บโปรไฟล์ได้สูงสุด 24 รายการ", "การตอบสนองความถี่"
    ),
    "fil" to profileCopy(
        "Mga profile", "Iyong mga profile", "AutoEQ", "Built-in", "Bagong profile", "I-edit", "Palitan ang pangalan", "I-duplicate", "Burahin",
        "Burahin ang “%s”?", "Aalisin ang profile na ito sa device na ito.", "Aktibo", "Flat", "Pangalan ng profile",
        "Maglagay ng pangalan", "May profile ka nang ganitong pangalan", "I-save", "Itapon ang mga pagbabago?",
        "Hindi pa naise-save ang mga pagbabago sa profile na ito.", "Itapon", "Ituloy ang pag-edit", "Band %d",
        "Mula %1\$s hanggang %2\$s", "Maglagay ng wastong numero", "Kopya ng %s", "Kopya ng %1\$s %2\$d",
        "Maririnig mo nang live ang mga pagbabago hanggang mag-save ka", "Gumawa o mag-duplicate ng profile para hubugin ang sarili mong tunog",
        "Mga opsyon ng profile", "Hanggang 24 na profile ang maitatabi mo", "Frequency response"
    ),
    "he" to profileCopy(
        "פרופילים", "הפרופילים שלך", "AutoEQ", "מובנים", "פרופיל חדש", "עריכה", "שינוי שם", "שכפול", "מחיקה",
        "למחוק את „%s”?", "הפרופיל יוסר מהמכשיר הזה.", "פעיל", "שטוח", "שם הפרופיל",
        "יש להזין שם", "כבר יש לך פרופיל בשם הזה", "שמירה", "לבטל את השינויים?",
        "השינויים בפרופיל הזה לא נשמרו.", "ביטול שינויים", "המשך עריכה", "פס %d",
        "בין %1\$s ל־%2\$s", "יש להזין מספר תקין", "עותק של %s", "עותק של %1\$s %2\$d",
        "השינויים נשמעים בזמן אמת עד השמירה", "אפשר ליצור או לשכפל פרופיל כדי לעצב את הצליל שלך",
        "אפשרויות פרופיל", "אפשר לשמור עד 24 פרופילים", "תגובת תדר"
    )
)

internal fun parametricProfileLocalizationCodes(): Set<String> = parametricProfileCopies.keys

fun LevyraStrings.parametricProfileCopy(): ParametricProfileCopy =
    parametricProfileCopies[code] ?: parametricProfileCopies.getValue("en")
