package com.luc4n3x.levyra.ui.i18n

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal object LevyraBrazilianPortuguese {
    private val sentenceStartForms = mapOf(
        "Abre" to "Abra",
        "Adiciona" to "Adicione",
        "Ativa" to "Ative",
        "Começa" to "Comece",
        "Cola" to "Cole",
        "Cria" to "Crie",
        "Dá" to "Dê",
        "Decide" to "Decida",
        "Escolhe" to "Escolha",
        "Esvazia-a" to "Esvazie-a",
        "Explora" to "Explore",
        "Importa" to "Importe",
        "Introduz" to "Insira",
        "Liga" to "Conecte",
        "Marca" to "Marque",
        "Muda" to "Mude",
        "Ouve" to "Ouça",
        "Permite" to "Permita",
        "Reinicia" to "Reinicie",
        "Reordena" to "Reordene",
        "Reúne" to "Reúna",
        "Ajusta" to "Ajuste",
        "Arrasta" to "Arraste",
        "Personaliza" to "Personalize",
        "Escreve" to "Escreva",
        "Seleciona" to "Selecione",
        "Tenta" to "Tente",
        "Toca" to "Toque",
        "Transfere" to "Baixe",
        "Usa" to "Use",
        "Verifica" to "Verifique"
    )

    private val regionalPhrases = mapOf(
        "a analisar" to "analisando",
        "a aprender" to "aprendendo",
        "a atualizar" to "atualizando",
        "a carregar" to "carregando",
        "a criar" to "criando",
        "a guardar" to "salvando",
        "a importar" to "importando",
        "a instalar" to "instalando",
        "a ler" to "lendo",
        "a ligar" to "conectando",
        "a mostrar" to "mostrando",
        "a mudar" to "mudando",
        "a ouvir" to "ouvindo",
        "para a ouvir" to "para ouvi-la",
        "que realmente ouves" to "que você realmente ouve",
        "a pesquisar" to "buscando",
        "a preparar" to "preparando",
        "a procurar" to "procurando",
        "a reproduzir" to "tocando",
        "a restabelecer" to "restabelecendo",
        "a reunir" to "reunindo",
        "a tocar" to "tocando",
        "a transferir" to "baixando",
        "a verificar" to "verificando",
        "começar a ouvir" to "começar a ouvir",
        "continuar a editar" to "continuar editando",
        "continuar a ouvir" to "continuar ouvindo",
        "links a verificar" to "links para verificar",
        "a importar a playlist" to "importando a playlist",
        "para a guardar" to "para salvá-la",
        "para a adicionar" to "para adicioná-la",
        "para a preencher" to "para preenchê-la",
        "para a mostrar" to "para mostrá-la",
        "pronto a instalar" to "pronto para instalar",
        "pronta a abrir" to "pronta para abrir",
        "quase a terminar" to "quase no fim",

        "a tua" to "sua",
        "as tuas" to "suas",
        "o teu" to "seu",
        "os teus" to "seus",
        "à tua volta" to "ao seu redor",
        "para ti" to "para você",
        "para si" to "para você",
        "para o personalizares" to "para personalizá-lo",
        "por ti" to "por você",
        "soa a ti" to "tem a sua cara",
        "teu" to "seu",
        "tua" to "sua",
        "teus" to "seus",
        "tuas" to "suas",
        "tu" to "você",
        "bloqueou-te" to "bloqueou você",
        "removeu-te" to "removeu você",
        "surpreende-me" to "me surpreenda",
        "podes" to "você pode",
        "queres" to "você quer",
        "tens" to "você tem",
        "vês" to "você vê",
        "estás" to "você está",
        "ouves" to "você ouve",
        "não ouves" to "você não ouve",
        "o que ouve" to "o que você ouve",
        "manténs premido" to "mantém pressionado",
        "que manténs" to "que você mantém",
        "poderás gostar" to "você pode gostar",
        "também poderás gostar" to "você também pode gostar",
        "como te chamas" to "como você se chama",
        "levar-te diretamente" to "levar você direto",
        "mantém-se compacta" to "fica compacta",
        "e pesquisa" to "e pesquise",
        "e adiciona" to "e adicione",
        "e ativa" to "e ative",
        "e toca em" to "e toque em",
        "pesquisa uma música" to "pesquise uma música",
        "tem de estar" to "precisa estar",
        "que escolheres" to "que você escolher",
        "inicias sessão" to "você entra",
        "repor" to "redefinir",
        "volta aqui e conclui" to "volte aqui e conclua",
        "verifica a ligação e tenta novamente" to "verifique a conexão e tente novamente",
        "e tenta novamente" to "e tente novamente",
        "tenta novamente" to "tente novamente",
        "verifica a ligação" to "verifique a conexão",
        "verifica o utilizador" to "verifique o usuário",
        "alarga" to "amplie",
        "ou reduz" to "ou reduza",
        "ou abre" to "ou abra",
        "ou ouve" to "ou ouça",
        "ou escolhe" to "ou escolha",

        "à espera que" to "aguardando que",
        "à espera" to "aguardando",
        "em curso" to "em andamento",
        "demasiado" to "muito",
        "algo correu mal" to "algo deu errado",
        "de imediato" to "imediatamente",
        "mudar o nome" to "renomear",

        "em ecrã inteiro" to "em tela cheia",
        "ecrã inteiro" to "tela cheia",
        "o ecrã" to "a tela",
        "um ecrã" to "uma tela",
        "do ecrã" to "da tela",
        "no ecrã" to "na tela",
        "num ecrã" to "em uma tela",
        "este ecrã" to "esta tela",
        "qualquer ecrã" to "qualquer tela",
        "os ecrãs" to "as telas",
        "dos ecrãs" to "das telas",
        "ecrã" to "tela",
        "ecrãs" to "telas",
        "ficheiro" to "arquivo",
        "ficheiros" to "arquivos",
        "definições" to "configurações",
        "definição" to "configuração",
        "telemóvel" to "celular",
        "telefone" to "celular",
        "gerir" to "gerenciar",
        "gestão" to "gerenciamento",
        "guardar" to "salvar",
        "guarda" to "salva",
        "guardado" to "salvo",
        "guardada" to "salva",
        "guardados" to "salvos",
        "guardadas" to "salvas",
        "eliminar" to "excluir",
        "elimina" to "exclui",
        "eliminado" to "excluído",
        "eliminada" to "excluída",
        "utilizador" to "usuário",
        "utilizadores" to "usuários",
        "nome de utilizador" to "nome de usuário",
        "palavra-passe" to "senha",
        "palavras-passe" to "senhas",
        "minileitor" to "miniplayer",
        "leitor" to "player",
        "a transferência" to "o download",
        "as transferências" to "os downloads",
        "da transferência" to "do download",
        "das transferências" to "dos downloads",
        "na transferência" to "no download",
        "nas transferências" to "nos downloads",
        "uma transferência" to "um download",
        "sua transferência" to "seu download",
        "tuas transferências" to "seus downloads",
        "transferências simultâneas" to "downloads simultâneos",
        "as tuas transferências" to "seus downloads",
        "das tuas transferências" to "dos seus downloads",
        "nas tuas transferências" to "nos seus downloads",
        "transferências guardadas" to "downloads salvos",
        "válidas existentes" to "válidos existentes",
        "transferências longas" to "downloads longos",
        "transferências" to "downloads",
        "transferência" to "download",
        "transferir" to "baixar",
        "transfere" to "baixa",
        "transferida" to "baixada",
        "transferido" to "baixado",
        "transferidas" to "baixadas",
        "transferidos" to "baixados",
        "partilha de vários" to "compartilhamento de vários",
        "a partilha" to "o compartilhamento",
        "partilhar" to "compartilhar",
        "partilhado" to "compartilhado",
        "partilhada" to "compartilhada",
        "partilha" to "compartilhamento",
        "partilhar através de" to "compartilhar via",
        "ligação" to "conexão",
        "ligações" to "conexões",
        "ligar" to "conectar",
        "liga-se" to "se conecta",
        "ligá-la" to "conectá-la",
        "ligado" to "conectado",
        "ligada" to "conectada",
        "ligadas" to "conectadas",
        "ligados" to "conectados",
        "quiseres" to "quiser",
        "reproduzidas" to "tocadas",
        "mostra-as" to "as mostra",
        "desligado" to "desconectado",
        "tela desligada" to "tela desligada",
        "controlo" to "controle",
        "controlos" to "controles",
        "soma de controlo" to "soma de verificação",
        "registo" to "registro",
        "registo de alterações" to "histórico de alterações",
        "género" to "gênero",
        "géneros" to "gêneros",
        "a minha banda sonora" to "minha trilha sonora",
        "eletrónica" to "eletrônica",
        "eletrónico" to "eletrônico",
        "aplicação" to "aplicativo",
        "da aplicação" to "do aplicativo",
        "toda a app" to "todo o app",
        "a app" to "o app",
        "início de sessão" to "login",
        "iniciar sessão" to "entrar",
        "desporto" to "esportes",
        "reproduzir" to "tocar",
        "baralhar" to "aleatório",
        "anular" to "desfazer",
        "seguinte" to "próxima",
        "cópias de segurança" to "backups",
        "cópia de segurança" to "backup",
        "introduzir" to "inserir",
        "etiqueta" to "tag",
        "etiquetas" to "tags",
        "deslocação de píxeis" to "deslocamento de pixels",
        "píxeis" to "pixels",
        "deteta" to "detecta",
        "detetado" to "detectado",
        "detetada" to "detectada",
        "detetados" to "detectados",
        "detetadas" to "detectadas",
        "aspeto" to "aspecto",
        "fidedigno" to "confiável",
        "encriptação" to "criptografia",
        "encriptada" to "criptografada",
        "fornecedor" to "provedor",
        "fornecedores" to "provedores",
        "temporizador" to "timer",
        "predefinido" to "padrão",
        "predefinida" to "padrão",
        "multimédia" to "multimídia",
        "arranque automático" to "inicialização automática",
        "barra de estado" to "barra de status",
        "estado das" to "status das",
        "premida" to "pressionada",
        "conduzir" to "dirigir",
        "tabela" to "parada",
        "tabelas" to "paradas",
        "restauro" to "restauração",
        "adapta-se" to "se adapta",
        "ajusta-se" to "se ajusta",
        "aplica-se" to "se aplica",
        "num" to "em um",
        "numa" to "em uma",
        "alargar" to "ampliar",
        "altifalante" to "alto-falante",
        "secção" to "seção",
        "secções" to "seções",
        "rasto" to "rastro",
        "alterações por guardar" to "alterações não salvas",
        "anfitrião de proxy" to "host de proxy",
        "da app" to "do app",
        "reprodutor" to "player",
        "premido" to "pressionado",
        "pressão longa" to "toque longo",
        "pressão prolongada" to "toque longo",
        "poupança de dados" to "economia de dados",
        "saltar" to "pular",
        "salta" to "pula",
        "salto" to "pulo",
        "de momento" to "no momento",
        "retoma automática" to "retomada automática",
        "êxitos" to "sucessos",
        "êxito" to "sucesso"
    )

    private val keyOverrides = mapOf(
        "localWaveQuery" to "novas músicas brasileiras 2026",
        "localWaveName" to "Brasil Wave",
        "ambientModeSubtitle" to "Tela compatível com OLED para ouvir com calma",
        "updateDescription" to "Uma tela mais compacta, um histórico de alterações mais legível e conteúdo com rolagem em qualquer tela.",
        "compactHomeSubtitle" to "Reduz o espaçamento vertical e deixa a rolagem mais leve",
        "continuousRadioSubtitle" to "Adiciona músicas semelhantes quando a fila está quase no fim",
        "alternativeAudioSubtitle" to "Usa um stream de áudio externo de maior qualidade quando o Levyra consegue verificar que é exatamente a mesma faixa.",
        "automaticTranslationSubtitle" to "Usa os idiomas disponíveis nas transcrições do YouTube",
        "downloadEmbedMetadataSubtitle" to "Grava título, artista e álbum no arquivo",
        "enhanceVideoMetadataSubtitle" to "Usa títulos e miniaturas da comunidade apenas em vídeos",
        "playerGestureHorizontalSwipeSubtitle" to "Muda para a faixa anterior ou próxima",
        "restoreBackupSubtitle" to "Verifica o esquema e a soma de verificação antes de substituir os dados locais",
        "jamAwaitingApproval" to "Aguardando o anfitrião deixar você entrar",
        "listeningHistoryEmptyDetail" to "Toque uma faixa e seu histórico aparece aqui.",
        "restartRequiredBody" to "O idioma foi salvo. Reinicie o app agora para recarregar todas as telas corretamente.",
        "languageQuestion" to "Qual idioma você quer usar?",
        "tasteQuestion" to "Escolha 3 ou mais gêneros que você adora.",
        "latestInstalled" to "Você já tem a versão mais recente instalada",
        "externalLinkUnavailable" to "Link externo indisponível",
        "cannotOpenExternalLink" to "Não foi possível abrir o link externo",
        "updateLinkUnavailable" to "Link de atualização indisponível",
        "keepScreenOn" to "Manter a tela ligada",
        "offlineSavedForListening" to "Salvo para ouvir offline",
        "offlineStorageFull" to "Espaço de armazenamento insuficiente. Libere espaço e tente novamente.",
        "offlineRestoreFolderAccess" to "Restaurar acesso à pasta",
        "offlineFolderAccessMismatch" to "Escolha a mesma pasta onde este download foi salvo."
    )

    private val phrasesByFirstWord: Map<String, List<Pair<String, String>>> = buildMap<String, MutableList<Pair<String, String>>> {
        fun register(source: String, target: String) {
            getOrPut(source.substringBefore(' ')) { mutableListOf() }.add(source to target)
        }
        regionalPhrases.forEach { (source, target) ->
            register(source, target)
            register(source.capitalizeFirst(), target.capitalizeFirst())
            register(source.uppercase(Locale.ROOT), target.uppercase(Locale.ROOT))
        }
    }.mapValues { (_, phrases) -> phrases.sortedByDescending { it.first.length } }

    private val adapted = ConcurrentHashMap<String, String>()

    fun adapt(text: String): String = adapted.getOrPut(text) { replacePhrases(text) }

    fun adaptDynamic(text: String): String = replacePhrases(text)

    fun adaptEntries(entries: Map<String, String>): Map<String, String> =
        entries.mapValues { (_, value) -> adapt(value) } + keyOverrides

    fun keyOverrideNames(): Set<String> = keyOverrides.keys

    @Suppress("UNCHECKED_CAST")
    fun <T> adaptValue(value: T): T = when (value) {
        is String -> adapt(value) as T
        is List<*> -> value.map { item -> if (item is String) adapt(item) else item } as T
        is Map<*, *> -> value.mapValues { (_, item) -> if (item is String) adapt(item) else item } as T
        else -> value
    }

    private fun replacePhrases(text: String): String {
        val result = StringBuilder(text.length + 16)
        var index = 0
        while (index < text.length) {
            if (!text[index].isWordCharacter()) {
                result.append(text[index])
                index += 1
                continue
            }
            var wordEnd = index
            while (wordEnd < text.length && text[wordEnd].isWordCharacter()) wordEnd += 1
            val word = text.substring(index, wordEnd)
            val match = phrasesByFirstWord[word]?.firstOrNull { (source, _) ->
                text.startsWith(source, index) &&
                    (index + source.length == text.length || !text[index + source.length].isWordCharacter())
            } ?: sentenceStartForms[word]?.takeIf { isSentenceStart(text, index) }?.let { word to it }
            if (match == null) {
                result.append(text, index, wordEnd)
                index = wordEnd
            } else {
                result.append(match.second)
                index += match.first.length
            }
        }
        return result.toString()
    }

    private fun isSentenceStart(text: String, index: Int): Boolean {
        var cursor = index - 1
        while (cursor >= 0 && text[cursor] == ' ') cursor -= 1
        return cursor < 0 || (cursor < index - 1 && text[cursor] in sentenceTerminators) || text[cursor] == '\n'
    }

    private val sentenceTerminators = setOf('.', '!', '?', '…')

    private fun Char.isWordCharacter(): Boolean = isLetterOrDigit() || this == '-'

    private fun String.capitalizeFirst(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }
}
